package com.example.midi

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.example.model.ConnectionProtocol
import com.example.model.MidiDirection
import com.example.model.MidiLogEntry
import com.example.service.UsbMidiService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class MidiManagerController(private val context: Context) {

    companion object {
        private const val TAG = "MidiManagerController"
        val BLE_MIDI_SERVICE_UUID: UUID = UUID.fromString("03b80e5a-ede8-4b33-a028-51000c730eab")
        val BLE_MIDI_CHAR_UUID: UUID = UUID.fromString("7772e5db-3868-4112-a1a9-f2669d106bf3")
        val CLIENT_CONFIG_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    // Connection State
    private val _connectionProtocol = MutableStateFlow(ConnectionProtocol.NONE)
    val connectionProtocol: StateFlow<ConnectionProtocol> = _connectionProtocol.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow("Belum terhubung ke GP-180")
    val connectedDeviceName: StateFlow<String> = _connectedDeviceName.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _foundDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val foundDevices: StateFlow<List<BluetoothDevice>> = _foundDevices.asStateFlow()

    // MIDI Logs
    private val _midiLogs = MutableStateFlow<List<MidiLogEntry>>(emptyList())
    val midiLogs: StateFlow<List<MidiLogEntry>> = _midiLogs.asStateFlow()

    // Last Active TX/RX for animated visual indicators
    private val _lastActivity = MutableStateFlow<MidiDirection?>(null)
    val lastActivity: StateFlow<MidiDirection?> = _lastActivity.asStateFlow()

    // Incoming Event stream (e.g. CC or PC received from pedal)
    // Triple<Status, Data1, Data2>
    private val _incomingMidi = MutableSharedFlow<Triple<Int, Int, Int>>()
    val incomingMidi: SharedFlow<Triple<Int, Int, Int>> = _incomingMidi.asSharedFlow()

    // Bluetooth Objects
    private val bluetoothManager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var bluetoothGatt: BluetoothGatt? = null
    private var midiCharacteristic: BluetoothGattCharacteristic? = null

    // USB MIDI Objects
    private val midiManager: MidiManager? = context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
    private var usbMidiDevice: MidiDevice? = null
    private var usbInputPort: MidiInputPort? = null // send to device

    // USB MIDI Background Service Objects
    private var usbService: UsbMidiService? = null
    private var isUsbServiceBound = false
    private var usbServiceJob: Job? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? UsbMidiService.UsbMidiBinder
            usbService = localBinder?.getService()
            isUsbServiceBound = true
            attachUsbServiceListeners()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            usbService = null
            isUsbServiceBound = false
            usbServiceJob?.cancel()
            usbServiceJob = null
        }
    }

    private fun attachUsbServiceListeners() {
        usbServiceJob?.cancel()
        usbServiceJob = scope.launch {
            val s = usbService ?: return@launch
            launch {
                s.isConnected.collect { connected ->
                    if (connected && _connectionProtocol.value != ConnectionProtocol.USB_MIDI) {
                        _connectionProtocol.value = ConnectionProtocol.USB_MIDI
                        _connectedDeviceName.value = s.connectedDeviceName.value
                    }
                }
            }
            launch {
                s.connectedDeviceName.collect { name ->
                    if (_connectionProtocol.value == ConnectionProtocol.USB_MIDI) {
                        _connectedDeviceName.value = name
                    }
                }
            }
            launch {
                s.incomingMidi.collect { event ->
                    _incomingMidi.emit(event)
                }
            }
            launch {
                s.incomingRawBytes.collect { raw ->
                    processRawMidi(raw)
                }
            }
        }
    }

    init {
        log(MidiDirection.RX, byteArrayOf(), "MIDI Engine ready. Siap koneksi Bluetooth BLE / USB.")
    }

    // ==========================================
    // LOGGING HELPER
    // ==========================================
    fun log(direction: MidiDirection, bytes: ByteArray, description: String) {
        val hex = if (bytes.isNotEmpty()) {
            bytes.joinToString(" ") { "%02X".format(it) }
        } else {
            "--"
        }
        val entry = MidiLogEntry(
            timestamp = timeFormat.format(Date()),
            direction = direction,
            hexBytes = hex,
            description = description
        )
        val current = _midiLogs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 100) current.removeAt(current.size - 1)
        _midiLogs.value = current
        _lastActivity.value = direction

        // Reset badge after 400ms
        scope.launch {
            delay(400)
            if (_lastActivity.value == direction) {
                _lastActivity.value = null
            }
        }
    }

    fun clearLogs() {
        _midiLogs.value = emptyList()
    }

    // ==========================================
    // 1. BLUETOOTH LE MIDI IMPLEMENTATION
    // ==========================================
    @SuppressLint("MissingPermission")
    fun startBleScan(onFailed: (String) -> Unit = {}) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            onFailed("Bluetooth tidak aktif pada perangkat ini.")
            log(MidiDirection.RX, byteArrayOf(), "Gagal scan: Bluetooth dimatikan.")
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            onFailed("BLE Scanner tidak tersedia.")
            return
        }

        _isScanning.value = true
        _foundDevices.value = emptyList()
        log(MidiDirection.TX, byteArrayOf(), "Memindai perangkat Bluetooth BLE MIDI Valeton GP-180...")

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BLE_MIDI_SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let { device ->
                    val current = _foundDevices.value.toMutableList()
                    if (current.none { it.address == device.address }) {
                        current.add(device)
                        _foundDevices.value = current
                        log(MidiDirection.RX, byteArrayOf(), "Ditemukan BLE Device: ${device.name ?: "Unknown"} (${device.address})")
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                _isScanning.value = false
                log(MidiDirection.RX, byteArrayOf(), "BLE Scan gagal dengan kode error: $errorCode")
                onFailed("Scan error: $errorCode")
            }
        }

        try {
            // Also scan generally without strict UUID filter in case pedal broadcasts custom name
            scanner.startScan(listOf(filter), settings, scanCallback)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    scanner.stopScan(scanCallback)
                    _isScanning.value = false
                    log(MidiDirection.RX, byteArrayOf(), "Scan BLE selesai. Ditemukan ${_foundDevices.value.size} perangkat.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping scan", e)
                }
            }, 8000)
        } catch (e: Exception) {
            _isScanning.value = false
            log(MidiDirection.RX, byteArrayOf(), "Error memulai scan BLE: ${e.message}")
            onFailed(e.message ?: "Error scanning")
        }
    }

    @SuppressLint("MissingPermission")
    fun connectBleDevice(device: BluetoothDevice) {
        log(MidiDirection.TX, byteArrayOf(), "Menghubungkan ke ${device.name ?: device.address}...")
        bluetoothGatt?.close()
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                log(MidiDirection.RX, byteArrayOf(), "BLE Terhubung. Mencari layanan MIDI GP-180...")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnect()
                log(MidiDirection.RX, byteArrayOf(), "Koneksi BLE Terputus.")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(BLE_MIDI_SERVICE_UUID)
                if (service != null) {
                    val characteristic = service.getCharacteristic(BLE_MIDI_CHAR_UUID)
                    if (characteristic != null) {
                        midiCharacteristic = characteristic
                        gatt.setCharacteristicNotification(characteristic, true)

                        // Enable remote notification descriptor
                        val descriptor = characteristic.getDescriptor(CLIENT_CONFIG_DESCRIPTOR_UUID)
                        if (descriptor != null) {
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                        }

                        _connectionProtocol.value = ConnectionProtocol.BLUETOOTH_LE
                        val devName = gatt.device.name ?: "Valeton GP-180 (BLE)"
                        _connectedDeviceName.value = devName
                        log(MidiDirection.RX, byteArrayOf(), "Tersambung ke $devName via BLE MIDI Nirkabel!")
                    } else {
                        log(MidiDirection.RX, byteArrayOf(), "Karakteristik BLE MIDI tidak ditemukan pada service.")
                    }
                } else {
                    log(MidiDirection.RX, byteArrayOf(), "Layanan BLE MIDI standar tidak ditemukan pada perangkat ini.")
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val bytes = characteristic.value ?: return
            handleIncomingBleBytes(bytes)
        }
    }

    private fun handleIncomingBleBytes(bytes: ByteArray) {
        if (bytes.size < 3) return
        // BLE MIDI format: Byte 0 is header (0x80..0xFF), Byte 1 is timestamp.
        // Actual MIDI starts at index 2
        val midiBytes = bytes.copyOfRange(2, bytes.size)
        processRawMidi(midiBytes)
    }

    // ==========================================
    // 2. USB MIDI IMPLEMENTATION (WITH BACKGROUND SERVICE)
    // ==========================================
    fun connectUsbMidi(onFailed: (String) -> Unit = {}) {
        // Start background USB MIDI service so sync stays active across apps & performance
        try {
            UsbMidiService.startService(context)
            val serviceIntent = Intent(context, UsbMidiService::class.java)
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            isUsbServiceBound = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start UsbMidiService: ${e.message}")
        }

        if (midiManager == null) {
            onFailed("Sistem Android MIDI tidak didukung pada perangkat ini.")
            log(MidiDirection.RX, byteArrayOf(), "Android MidiManager tidak tersedia.")
            return
        }

        val infos = midiManager.devices
        if (infos.isEmpty()) {
            val msg = "Tidak ada perangkat USB MIDI terdeteksi. Pastikan kabel OTG / USB-C tercolok ke GP-180."
            log(MidiDirection.RX, byteArrayOf(), msg)
            onFailed(msg)
            return
        }

        // Search for Valeton or GP or take first
        val targetInfo = infos.firstOrNull { info ->
            val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: ""
            name.contains("Valeton", ignoreCase = true) || name.contains("GP", ignoreCase = true)
        } ?: infos.first()

        val devName = targetInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "USB MIDI Device"
        log(MidiDirection.TX, byteArrayOf(), "Membuka port USB MIDI (Background Service Sync): $devName...")

        usbService?.connectToDevice(targetInfo, onFailed)

        midiManager.openDevice(targetInfo, { device ->
            if (device == null) {
                log(MidiDirection.RX, byteArrayOf(), "Gagal membuka port USB device.")
                onFailed("Gagal membuka USB MIDI device.")
                return@openDevice
            }

            usbMidiDevice = device
            usbInputPort = device.openInputPort(0)

            // Open output port for receiving data from pedal
            val outputPort = device.openOutputPort(0)
            outputPort?.connect(object : MidiReceiver() {
                override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
                    val raw = msg.copyOfRange(offset, offset + count)
                    processRawMidi(raw)
                }
            })

            _connectionProtocol.value = ConnectionProtocol.USB_MIDI
            _connectedDeviceName.value = devName
            log(MidiDirection.RX, byteArrayOf(), "Tersambung via USB MIDI Service ke $devName!")
        }, Handler(Looper.getMainLooper()))
    }

    // ==========================================
    // 3. VIRTUAL SIMULATION RIG MODE
    // ==========================================
    fun connectSimulation() {
        disconnect()
        _connectionProtocol.value = ConnectionProtocol.SIMULATION
        _connectedDeviceName.value = "Valeton GP-180 [Virtual Simulator]"
        log(MidiDirection.RX, byteArrayOf(0xC0.toByte(), 0x00), "Tersambung ke Simulator Valeton GP-180. Siap menerima & mengirim preset!")
    }

    // ==========================================
    // 4. UNIFIED MIDI TRANSMITTER
    // ==========================================
    fun sendBytes(bytes: ByteArray, desc: String) {
        if (_connectionProtocol.value == ConnectionProtocol.NONE) {
            log(MidiDirection.TX, bytes, "Gagal kirim: Belum tersambung ke pedal (Pilih Bluetooth, USB, atau Simulator).")
            return
        }

        log(MidiDirection.TX, bytes, desc)

        when (_connectionProtocol.value) {
            ConnectionProtocol.USB_MIDI -> {
                val sentByService = usbService?.sendMidiBytes(bytes) == true
                if (!sentByService) {
                    try {
                        usbInputPort?.send(bytes, 0, bytes.size)
                    } catch (e: Exception) {
                        log(MidiDirection.RX, byteArrayOf(), "Error USB send: ${e.message}")
                    }
                }
            }
            ConnectionProtocol.BLUETOOTH_LE -> {
                sendBleMidiPacket(bytes)
            }
            ConnectionProtocol.SIMULATION -> {
                // Realistic virtual hardware simulation response!
                simulateHardwareResponse(bytes)
            }
            ConnectionProtocol.NONE -> {}
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendBleMidiPacket(midiBytes: ByteArray) {
        val char = midiCharacteristic ?: return
        val gatt = bluetoothGatt ?: return

        // BLE MIDI Header: 0x80, Timestamp: 0x80 | (ms & 0x7F)
        val header = 0x80.toByte()
        val timestamp = (0x80 or (System.currentTimeMillis().toInt() and 0x7F)).toByte()
        val packet = byteArrayOf(header, timestamp) + midiBytes

        char.value = packet
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        gatt.writeCharacteristic(char)
    }

    private fun simulateHardwareResponse(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        val status = bytes[0].toInt() and 0xFF
        val type = status and 0xF0

        scope.launch {
            delay(120) // Realistic MIDI propagation delay
            if (type == 0xB0 && bytes.size >= 3) {
                val cc = bytes[1].toInt() and 0xFF
                val value = bytes[2].toInt() and 0xFF
                // If it was a footswitch trigger (CC 81, 82, 83), echo response back!
                if (cc in 81..83) {
                    val echoBytes = byteArrayOf(status.toByte(), cc.toByte(), 0x7F.toByte())
                    log(MidiDirection.RX, echoBytes, "Pedal Ack: Footswitch ${cc - 80} toggled")
                    _incomingMidi.emit(Triple(status, cc, value))
                }
            } else if (type == 0xC0 && bytes.size >= 2) {
                val patch = bytes[1].toInt() and 0xFF
                val echo = byteArrayOf(status.toByte(), patch.toByte())
                log(MidiDirection.RX, echo, "Pedal Ack: Memory switched to Patch #$patch")
                _incomingMidi.emit(Triple(status, patch, 0))
            }
        }
    }

    // High level helpers
    fun sendControlChange(channel: Int = 0, cc: Int, value: Int, label: String = "") {
        val status = (0xB0 or (channel and 0x0F)).toByte()
        val bytes = byteArrayOf(status, (cc and 0x7F).toByte(), (value and 0x7F).toByte())
        val desc = if (label.isNotEmpty()) "CC #$cc = $value ($label)" else "CC #$cc = $value"
        sendBytes(bytes, desc)
    }

    fun sendProgramChange(channel: Int = 0, program: Int, label: String = "") {
        val status = (0xC0 or (channel and 0x0F)).toByte()
        val bytes = byteArrayOf(status, (program and 0x7F).toByte())
        val desc = if (label.isNotEmpty()) "Program Change #$program ($label)" else "Program Change #$program"
        sendBytes(bytes, desc)
    }

    fun triggerFootswitch(fsNumber: Int) {
        val cc = 80 + fsNumber
        sendControlChange(cc = cc, value = 127, label = "Trigger FS$fsNumber")
    }

    // ==========================================
    // 5. INCOMING DATA PROCESSING (BI-DIRECTIONAL)
    // ==========================================
    private fun processRawMidi(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        var i = 0
        while (i < bytes.size) {
            val status = bytes[i].toInt() and 0xFF
            val messageType = status and 0xF0

            when (messageType) {
                0xB0 -> { // Control Change (3 bytes: status, cc, value)
                    if (i + 2 < bytes.size) {
                        val cc = bytes[i + 1].toInt() and 0xFF
                        val value = bytes[i + 2].toInt() and 0xFF
                        val chunk = byteArrayOf(bytes[i], bytes[i + 1], bytes[i + 2])
                        log(MidiDirection.RX, chunk, "RX CC #$cc = $value dari pedal GP-180")
                        scope.launch { _incomingMidi.emit(Triple(status, cc, value)) }
                        i += 3
                    } else {
                        i++
                    }
                }
                0xC0 -> { // Program Change (2 bytes: status, program)
                    if (i + 1 < bytes.size) {
                        val prog = bytes[i + 1].toInt() and 0xFF
                        val chunk = byteArrayOf(bytes[i], bytes[i + 1])
                        log(MidiDirection.RX, chunk, "RX Program Change #$prog (Pedal Patch Switch)")
                        scope.launch { _incomingMidi.emit(Triple(status, prog, 0)) }
                        i += 2
                    } else {
                        i++
                    }
                }
                else -> {
                    i++
                }
            }
        }
    }

    // ==========================================
    // 6. DISCONNECTION
    // ==========================================
    @SuppressLint("MissingPermission")
    fun disconnect() {
        val prev = _connectedDeviceName.value
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing BLE", e)
        }
        bluetoothGatt = null
        midiCharacteristic = null

        try {
            usbInputPort?.close()
            usbMidiDevice?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing USB", e)
        }
        usbInputPort = null
        usbMidiDevice = null

        if (isUsbServiceBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                // ignore
            }
            isUsbServiceBound = false
        }
        usbServiceJob?.cancel()
        usbServiceJob = null
        usbService?.disconnectDevice()
        UsbMidiService.stopService(context)
        usbService = null

        _connectionProtocol.value = ConnectionProtocol.NONE
        _connectedDeviceName.value = "Belum terhubung ke GP-180"
        log(MidiDirection.RX, byteArrayOf(), "Hardware terputus dari $prev.")
    }
}
