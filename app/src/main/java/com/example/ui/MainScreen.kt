package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionProtocol
import com.example.model.MidiDirection
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.Gp180ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: Gp180ViewModel) {
    val context = LocalContext.current
    val currentRig by viewModel.currentRig.collectAsState()
    val activeIndex by viewModel.activeModuleIndex.collectAsState()
    val connectionProtocol by viewModel.connectionProtocol.collectAsState()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val foundDevices by viewModel.foundDevices.collectAsState()
    val lastActivity by viewModel.lastActivity.collectAsState()
    val midiLogs by viewModel.midiLogs.collectAsState()
    val isGeneratingAi by viewModel.isGeneratingAi.collectAsState()
    val presetList by viewModel.presetList.collectAsState()

    val songInput by viewModel.songInput.collectAsState()
    val promptInput by viewModel.promptInput.collectAsState()

    val showDevicePicker by viewModel.showDevicePicker.collectAsState()
    val showPresetLibrary by viewModel.showPresetLibrary.collectAsState()
    val showMidiMonitor by viewModel.showMidiMonitor.collectAsState()
    val showSaveDialog by viewModel.showSaveDialog.collectAsState()

    Scaffold(
        containerColor = CyberBlack,
        bottomBar = {
            // Persistent Live MIDI Log Bar
            MidiBottomBar(
                latestLog = midiLogs.firstOrNull()?.description ?: "Menunggu inisialisasi koneksi...",
                lastActivity = lastActivity,
                onClick = { viewModel.showMidiMonitor.value = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ==========================================
            // TOP BAR & HARDWARE DUAL CONNECTION
            // ==========================================
            HeaderConnectionCard(
                protocol = connectionProtocol,
                deviceName = connectedDeviceName,
                onConnectBt = { viewModel.connectBluetooth() },
                onConnectUsb = { viewModel.connectUsb() },
                onConnectSim = { viewModel.connectSimulation() },
                onDisconnect = { viewModel.disconnect() }
            )

            // ==========================================
            // SECTION 01: TONE CONCEPT & AI GENERATOR
            // ==========================================
            ToneConceptCard(
                songInput = songInput,
                onSongChange = { viewModel.songInput.value = it },
                promptInput = promptInput,
                onPromptChange = { viewModel.promptInput.value = it },
                isGenerating = isGeneratingAi,
                onGenerate = { viewModel.generatePresetWithAi() },
                onQuickStyle = { song, prompt ->
                    viewModel.songInput.value = song
                    viewModel.promptInput.value = prompt
                }
            )

            // ==========================================
            // SECTION 02: SIGNAL CHAIN & MODULE EDITOR
            // ==========================================
            SignalChainCard(
                rig = currentRig,
                activeIndex = activeIndex,
                onSelectModule = { viewModel.selectModule(it) },
                onToggleModule = { viewModel.toggleCurrentModule() },
                onParamChange = { key, value -> viewModel.updateParam(key, value) },
                onOpenPresets = { viewModel.showPresetLibrary.value = true }
            )

            // ==========================================
            // FOOTSWITCH BOARD
            // ==========================================
            FootswitchBoard(
                fs1Title = currentRig.fs1Title,
                fs2Title = currentRig.fs2Title,
                fs3Title = currentRig.fs3Title,
                onTriggerFs = { viewModel.triggerFootswitch(it) }
            )

            // ==========================================
            // ACTION BUTTONS (SYNC, BACKUP, SAVE)
            // ==========================================
            ActionButtonsRow(
                onSendToPedal = { viewModel.sendRigToGp180() },
                onSave = { viewModel.showSaveDialog.value = true },
                onBackup = {
                    val json = viewModel.getExportJson()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("GP180_Preset", json))
                    Toast.makeText(context, "Preset .prst disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                },
                onOpenMonitor = { viewModel.showMidiMonitor.value = true }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    // ==========================================
    // DIALOGS & BOTTOM SHEETS
    // ==========================================
    if (showDevicePicker) {
        DevicePickerDialog(
            isScanning = isScanning,
            devices = foundDevices,
            onScanAgain = { viewModel.connectBluetooth() },
            onSelectDevice = { viewModel.connectBleDevice(it) },
            onStartSimulation = { viewModel.connectSimulation() },
            onDismiss = { viewModel.showDevicePicker.value = false }
        )
    }

    if (showPresetLibrary) {
        PresetLibraryDialog(
            presets = presetList,
            currentPresetId = currentRig.id,
            onSelectPreset = { viewModel.loadPreset(it) },
            onImportJson = { viewModel.importPresetJson(it) },
            onExportCurrent = {
                val json = viewModel.getExportJson()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("GP180_Preset", json))
                Toast.makeText(context, "Preset .prst disalin ke clipboard!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { viewModel.showPresetLibrary.value = false }
        )
    }

    if (showSaveDialog) {
        SavePresetDialog(
            initialName = currentRig.name,
            onSave = { viewModel.saveCurrentPreset(it) },
            onDismiss = { viewModel.showSaveDialog.value = false }
        )
    }

    if (showMidiMonitor) {
        MidiMonitorSheet(
            logs = midiLogs,
            onClearLogs = { viewModel.midiController.clearLogs() },
            onDismiss = { viewModel.showMidiMonitor.value = false }
        )
    }
}

// ==========================================
// SUB-COMPONENTS
// ==========================================

@Composable
fun HeaderConnectionCard(
    protocol: ConnectionProtocol,
    deviceName: String,
    onConnectBt: () -> Unit,
    onConnectUsb: () -> Unit,
    onConnectSim: () -> Unit,
    onDisconnect: () -> Unit
) {
    val isConnected = protocol != ConnectionProtocol.NONE

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkGunmetal, RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // App Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "GP-180 ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "PRESET LAB",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF042F2E), RoundedCornerShape(4.dp))
                            .border(0.5.dp, Color(0xFF0F766E), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "BT + USB DUAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan
                        )
                    }
                }
                Text(
                    text = "Hardware Rig Sync & AI Tone Generator for Valeton GP-180",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }

        // Connection Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bluetooth Button
            Button(
                onClick = onConnectBt,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (protocol == ConnectionProtocol.BLUETOOTH_LE) NeonBlueDim else SurfaceCard,
                    contentColor = if (protocol == ConnectionProtocol.BLUETOOTH_LE) NeonBlue else TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (protocol == ConnectionProtocol.BLUETOOTH_LE) NeonBlue else SurfaceCardBorder),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_bluetooth_connect")
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (protocol == ConnectionProtocol.BLUETOOTH_LE) "Connected BLE" else "Bluetooth",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // USB MIDI Button
            Button(
                onClick = onConnectUsb,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (protocol == ConnectionProtocol.USB_MIDI) NeonEmeraldDim else SurfaceCard,
                    contentColor = if (protocol == ConnectionProtocol.USB_MIDI) NeonEmerald else TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (protocol == ConnectionProtocol.USB_MIDI) NeonEmerald else SurfaceCardBorder),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_usb_connect")
            ) {
                Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (protocol == ConnectionProtocol.USB_MIDI) "Connected USB" else "USB MIDI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Simulation / Virtual Rig Button
            Button(
                onClick = onConnectSim,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (protocol == ConnectionProtocol.SIMULATION) NeonCyanDim else SurfaceCard,
                    contentColor = if (protocol == ConnectionProtocol.SIMULATION) NeonCyan else TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (protocol == ConnectionProtocol.SIMULATION) NeonCyan else SurfaceCardBorder),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.testTag("btn_sim_connect")
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(15.dp))
            }

            // Disconnect button
            if (isConnected) {
                IconButton(
                    onClick = onDisconnect,
                    modifier = Modifier
                        .background(Color(0x33EF4444), RoundedCornerShape(10.dp))
                        .size(38.dp)
                        .testTag("btn_disconnect")
                ) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = "Disconnect",
                        tint = NeonCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Hardware Status Indicator Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberBlack, RoundedCornerShape(10.dp))
                .border(0.5.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dotColor = when (protocol) {
                    ConnectionProtocol.BLUETOOTH_LE -> NeonBlue
                    ConnectionProtocol.USB_MIDI -> NeonEmerald
                    ConnectionProtocol.SIMULATION -> NeonCyan
                    ConnectionProtocol.NONE -> NeonCrimson
                }

                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = deviceName,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        when (protocol) {
                            ConnectionProtocol.BLUETOOTH_LE -> NeonBlueDim
                            ConnectionProtocol.USB_MIDI -> NeonEmeraldDim
                            ConnectionProtocol.SIMULATION -> NeonCyanDim
                            ConnectionProtocol.NONE -> SurfaceCard
                        },
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = protocol.badgeText,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = when (protocol) {
                        ConnectionProtocol.BLUETOOTH_LE -> NeonBlue
                        ConnectionProtocol.USB_MIDI -> NeonEmerald
                        ConnectionProtocol.SIMULATION -> NeonCyan
                        ConnectionProtocol.NONE -> TextTertiary
                    }
                )
            }
        }
    }
}

@Composable
fun ToneConceptCard(
    songInput: String,
    onSongChange: (String) -> Unit,
    promptInput: String,
    onPromptChange: (String) -> Unit,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    onQuickStyle: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkGunmetal, RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(NeonCyan, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "01",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberBlack
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TONE CONCEPT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = "Gemini 3.8 Flash AI",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
        }

        // Quick suggestions row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val suggestions = listOf(
                "Metallica - St. Anger" to "Raw aggressive high gain, mid scoop, tight gate chug",
                "Pink Floyd - Comfortably Numb" to "Singing sustained lead, stereo tape echo, lush hall reverb",
                "Polyphia - Playing God" to "Modern nylon spanky clean, compressor, acoustic simulator",
                "Van Halen - Brown Sound" to "1968 Variac Plexi crunch, punchy midrange, Phaser 90",
                "SRV - Texas Blues" to "Overdriven Texas strat blues, TS boost into warm tube combo"
            )
            items(suggestions) { (sSong, sPrompt) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceCard)
                        .border(0.5.dp, SurfaceCardBorder, RoundedCornerShape(6.dp))
                        .clickable { onQuickStyle(sSong, sPrompt) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = sSong.substringBefore(" -"),
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Inputs
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = songInput,
                onValueChange = onSongChange,
                label = { Text("Lagu / Artis Referensi", fontSize = 11.sp, color = TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = SurfaceCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CyberBlack,
                    unfocusedContainerColor = CyberBlack
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("song_input")
            )

            OutlinedTextField(
                value = promptInput,
                onValueChange = onPromptChange,
                label = { Text("Karakter Sound / Prompt", fontSize = 11.sp, color = TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = SurfaceCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CyberBlack,
                    unfocusedContainerColor = CyberBlack
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prompt_input")
            )
        }

        // Generate Rig AI Button
        Button(
            onClick = onGenerate,
            enabled = !isGenerating,
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan,
                contentColor = CyberBlack
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("btn_generate_ai")
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CyberBlack, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gemini 3.8 Flash Meracik Rig...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Rig dengan Gemini AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SignalChainCard(
    rig: com.example.model.PresetRig,
    activeIndex: Int,
    onSelectModule: (Int) -> Unit,
    onToggleModule: () -> Unit,
    onParamChange: (String, Int) -> Unit,
    onOpenPresets: () -> Unit
) {
    val activeMod = rig.modules.getOrNull(activeIndex) ?: rig.modules.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkGunmetal, RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header: Step 02 & Patch selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(NeonCyan, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("02", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CyberBlack)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SIGNAL CHAIN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
            }

            // Patch selector button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCardActive)
                    .border(1.dp, NeonCyanDim, RoundedCornerShape(8.dp))
                    .clickable { onOpenPresets() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("patch_badge_btn")
            ) {
                Text(
                    text = "Patch: ${rig.patchDisplay}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan
                )
            }
        }

        // Horizontal Chain Carousel
        FxChainBar(
            modules = rig.modules,
            activeIndex = activeIndex,
            onSelectModule = onSelectModule
        )

        // Active Module Editor Card
        if (activeMod != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBlack, RoundedCornerShape(14.dp))
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Module Title & Bypass Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${activeMod.id}: ${activeMod.name}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${activeMod.category})",
                                fontSize = 10.sp,
                                color = TextTertiary
                            )
                        }
                        Text(
                            text = "MIDI CC #${activeMod.cc}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan
                        )
                    }

                    // Bypass Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (activeMod.isEnabled) "ACTIVE" else "BYPASS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (activeMod.isEnabled) NeonCyan else TextTertiary
                        )
                        Switch(
                            checked = activeMod.isEnabled,
                            onCheckedChange = { onToggleModule() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberBlack,
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = SliderTrack
                            ),
                            modifier = Modifier.testTag("module_toggle_switch")
                        )
                    }
                }

                // Knobs Grid
                val paramList = activeMod.params.entries.toList()
                val chunkedParams = paramList.chunked(2)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunkedParams.forEach { rowParams ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowParams.forEach { (key, value) ->
                                KnobControl(
                                    title = key,
                                    value = value,
                                    onValueChange = { onParamChange(key, it) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowParams.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButtonsRow(
    onSendToPedal: () -> Unit,
    onSave: () -> Unit,
    onBackup: () -> Unit,
    onOpenMonitor: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Send to GP-180 Primary Action
        Button(
            onClick = onSendToPedal,
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonEmerald,
                contentColor = CyberBlack
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("btn_send_rig_to_gp180")
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Preset to GP-180 Memory", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        // Secondary actions: Save, Backup, Live Monitor
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onSave,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Rig", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onBackup,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Backup", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onOpenMonitor,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyanDim),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Monitor", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun MidiBottomBar(
    latestLog: String,
    lastActivity: MidiDirection?,
    onClick: () -> Unit
) {
    val isTx = lastActivity == MidiDirection.TX
    val isRx = lastActivity == MidiDirection.RX

    Surface(
        color = CyberBlack,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = SurfaceCardBorder)
            .clickable { onClick() }
            .testTag("midi_bottom_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "MIDI LOG: ",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = NeonCyan
                )
                Text(
                    text = latestLog,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // TX/RX animated badge
            Box(
                modifier = Modifier
                    .background(
                        when {
                            isTx -> NeonCyanDim
                            isRx -> NeonEmeraldDim
                            else -> SurfaceCard
                        },
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = when {
                        isTx -> "TX"
                        isRx -> "RX"
                        else -> "IDLE"
                    },
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isTx -> NeonCyan
                        isRx -> NeonEmerald
                        else -> TextTertiary
                    }
                )
            }
        }
    }
}
