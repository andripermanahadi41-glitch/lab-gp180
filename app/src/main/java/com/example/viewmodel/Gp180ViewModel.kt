package com.example.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ToneGeneratorEngine
import com.example.data.PresetRepository
import com.example.midi.MidiManagerController
import com.example.model.ConnectionProtocol
import com.example.model.FxModule
import com.example.model.MidiDirection
import com.example.model.PresetRig
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

class Gp180ViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PresetRepository(application)
    val midiController = MidiManagerController(application)
    private val aiEngine = ToneGeneratorEngine()

    // UI State: Current Rig
    private val _currentRig = MutableStateFlow(PresetRepository.FACTORY_PRESETS[0])
    val currentRig: StateFlow<PresetRig> = _currentRig.asStateFlow()

    // Active selected module index in chain
    private val _activeModuleIndex = MutableStateFlow(2) // Default AMP
    val activeModuleIndex: StateFlow<Int> = _activeModuleIndex.asStateFlow()

    // Presets List
    private val _presetList = MutableStateFlow(repository.getAllPresets())
    val presetList: StateFlow<List<PresetRig>> = _presetList.asStateFlow()

    // AI inputs
    val songInput = MutableStateFlow("Metallica - Saint Anger")
    val promptInput = MutableStateFlow("Raw aggressive high gain, mid scoop, tight gate chug")
    val isGeneratingAi = MutableStateFlow(false)

    // Connection & Log states forwarded from controller
    val connectionProtocol = midiController.connectionProtocol
    val connectedDeviceName = midiController.connectedDeviceName
    val isScanning = midiController.isScanning
    val foundDevices = midiController.foundDevices
    val midiLogs = midiController.midiLogs
    val lastActivity = midiController.lastActivity

    // Dialog & Sheet states
    val showDevicePicker = MutableStateFlow(false)
    val showPresetLibrary = MutableStateFlow(false)
    val showMidiMonitor = MutableStateFlow(false)
    val showSaveDialog = MutableStateFlow(false)

    init {
        // Listen to incoming MIDI from hardware (Bi-directional sync!)
        viewModelScope.launch {
            midiController.incomingMidi.collect { (status, data1, data2) ->
                val type = status and 0xF0
                if (type == 0xB0) { // Control Change
                    handleIncomingControlChange(cc = data1, value = data2)
                } else if (type == 0xC0) { // Program Change
                    handleIncomingProgramChange(patch = data1)
                }
            }
        }
    }

    // ==========================================
    // CHAIN & MODULE CONTROLS
    // ==========================================
    fun selectModule(index: Int) {
        val count = _currentRig.value.modules.size
        if (index in 0 until count) {
            _activeModuleIndex.value = index
        }
    }

    fun toggleCurrentModule() {
        val rig = _currentRig.value
        val index = _activeModuleIndex.value
        if (index !in rig.modules.indices) return

        val mod = rig.modules[index]
        val newEnabled = !mod.isEnabled
        val updatedModules = rig.modules.toMutableList()
        updatedModules[index] = mod.copy(isEnabled = newEnabled)

        _currentRig.value = rig.copy(modules = updatedModules)

        // Send CC for bypass (127 = active, 0 = bypassed)
        midiController.sendControlChange(
            cc = mod.cc,
            value = if (newEnabled) 127 else 0,
            label = "${mod.id} ${if (newEnabled) "ON" else "BYPASS"}"
        )
    }

    fun updateParam(paramKey: String, newValue: Int) {
        val rig = _currentRig.value
        val index = _activeModuleIndex.value
        if (index !in rig.modules.indices) return

        val mod = rig.modules[index]
        val updatedParams = LinkedHashMap(mod.params)
        updatedParams[paramKey] = newValue

        val updatedModules = rig.modules.toMutableList()
        updatedModules[index] = mod.copy(params = updatedParams)

        _currentRig.value = rig.copy(modules = updatedModules)

        // Transmit real-time CC (MIDI value 0..127 scaled from 0..100)
        val midiVal = ((newValue / 100f) * 127).toInt().coerceIn(0, 127)
        midiController.sendControlChange(
            cc = mod.cc,
            value = midiVal,
            label = "${mod.id} $paramKey"
        )
    }

    // ==========================================
    // FOOTSWITCH CONTROLS
    // ==========================================
    fun triggerFootswitch(fsNumber: Int) {
        midiController.triggerFootswitch(fsNumber)
        // If FS1 or FS2 correspond to Boost or Delay, toggle their UI state
        val rig = _currentRig.value
        when (fsNumber) {
            1 -> {
                val bstIdx = rig.modules.indexOfFirst { it.id == "BST" }
                if (bstIdx >= 0) {
                    val updated = rig.modules.toMutableList()
                    val mod = updated[bstIdx]
                    updated[bstIdx] = mod.copy(isEnabled = !mod.isEnabled)
                    _currentRig.value = rig.copy(modules = updated)
                }
            }
            2 -> {
                val dlyIdx = rig.modules.indexOfFirst { it.id == "DLY" }
                if (dlyIdx >= 0) {
                    val updated = rig.modules.toMutableList()
                    val mod = updated[dlyIdx]
                    updated[dlyIdx] = mod.copy(isEnabled = !mod.isEnabled)
                    _currentRig.value = rig.copy(modules = updated)
                }
            }
        }
    }

    // ==========================================
    // BI-DIRECTIONAL INCOMING SYNC
    // ==========================================
    private fun handleIncomingControlChange(cc: Int, value: Int) {
        val rig = _currentRig.value
        val modIdx = rig.modules.indexOfFirst { it.cc == cc }
        if (modIdx >= 0) {
            val mod = rig.modules[modIdx]
            val isEnabled = value > 63
            val updated = rig.modules.toMutableList()
            updated[modIdx] = mod.copy(isEnabled = isEnabled)
            _currentRig.value = rig.copy(modules = updated)
        }
    }

    private fun handleIncomingProgramChange(patch: Int) {
        _currentRig.value = _currentRig.value.copy(patchNumber = patch)
    }

    // ==========================================
    // PRESET ACTIONS & SYNC
    // ==========================================
    fun sendRigToGp180() {
        val rig = _currentRig.value
        // 1. Program Change to select patch slot
        midiController.sendProgramChange(
            program = rig.patchNumber,
            label = "Patch ${rig.patchDisplay} - ${rig.name}"
        )

        // 2. Transmit module bypass states and initial parameters
        rig.modules.forEach { mod ->
            midiController.sendControlChange(
                cc = mod.cc,
                value = if (mod.isEnabled) 127 else 0,
                label = "${mod.id} Sync"
            )
        }

        Toast.makeText(
            getApplication(),
            "Seluruh preset \"${rig.name}\" tersinkron ke memori GP-180!",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun generatePresetWithAi() {
        val song = songInput.value.trim()
        val prompt = promptInput.value.trim()

        if (song.isBlank() && prompt.isBlank()) {
            Toast.makeText(getApplication(), "Masukkan judul lagu atau karakter suara", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            isGeneratingAi.value = true
            midiController.log(
                MidiDirection.TX,
                byteArrayOf(),
                "AI Tone Architect sedang meracik rig GP-180 untuk \"$song\"..."
            )

            try {
                val newRig = aiEngine.generatePreset(song, prompt, _currentRig.value.patchNumber)
                _currentRig.value = newRig
                _activeModuleIndex.value = newRig.modules.indexOfFirst { it.id == "AMP" }.coerceAtLeast(0)

                // Auto-transmit new preset to GP-180
                sendRigToGp180()

                midiController.log(
                    MidiDirection.RX,
                    byteArrayOf(),
                    "Rig baru untuk \"${newRig.name}\" berhasil diracik dan ditransfer ke pedal!"
                )
            } catch (e: Exception) {
                midiController.log(MidiDirection.RX, byteArrayOf(), "Gagal meracik rig: ${e.message}")
            } finally {
                isGeneratingAi.value = false
            }
        }
    }

    fun loadPreset(preset: PresetRig) {
        _currentRig.value = preset
        songInput.value = preset.songReference.ifBlank { preset.name }
        promptInput.value = preset.soundPrompt
        _activeModuleIndex.value = preset.modules.indexOfFirst { it.id == "AMP" }.coerceAtLeast(0)

        // Switch hardware patch
        midiController.sendProgramChange(program = preset.patchNumber, label = preset.name)
        preset.modules.forEach { mod ->
            midiController.sendControlChange(cc = mod.cc, value = if (mod.isEnabled) 127 else 0)
        }
        showPresetLibrary.value = false
    }

    fun saveCurrentPreset(name: String) {
        if (name.isBlank()) return
        val rig = _currentRig.value.copy(name = name)
        _currentRig.value = rig
        repository.saveUserPreset(rig)
        _presetList.value = repository.getAllPresets()
        showSaveDialog.value = false
        Toast.makeText(getApplication(), "Preset \"$name\" berhasil disimpan!", Toast.LENGTH_SHORT).show()
    }

    fun importPresetJson(jsonStr: String): Boolean {
        return try {
            val json = JSONObject(jsonStr)
            val imported = repository.presetFromJson(json)
            loadPreset(imported)
            repository.saveUserPreset(imported)
            _presetList.value = repository.getAllPresets()
            Toast.makeText(getApplication(), "Preset \"${imported.name}\" berhasil diimpor!", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(getApplication(), "Format preset JSON tidak valid: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    fun getExportJson(): String {
        return repository.presetToJson(_currentRig.value).toString(2)
    }

    // ==========================================
    // HARDWARE CONNECTION ACTIONS
    // ==========================================
    fun connectBluetooth() {
        showDevicePicker.value = true
        midiController.startBleScan { error ->
            Toast.makeText(getApplication(), error, Toast.LENGTH_SHORT).show()
        }
    }

    fun connectBleDevice(device: BluetoothDevice) {
        showDevicePicker.value = false
        midiController.connectBleDevice(device)
    }

    fun connectUsb() {
        midiController.connectUsbMidi { error ->
            Toast.makeText(getApplication(), error, Toast.LENGTH_LONG).show()
        }
    }

    fun connectSimulation() {
        midiController.connectSimulation()
        Toast.makeText(getApplication(), "Tersambung ke GP-180 Simulator", Toast.LENGTH_SHORT).show()
    }

    fun disconnect() {
        midiController.disconnect()
    }
}
