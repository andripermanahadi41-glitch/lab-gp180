package com.example.ui.components

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@SuppressLint("MissingPermission")
@Composable
fun DevicePickerDialog(
    isScanning: Boolean,
    devices: List<BluetoothDevice>,
    onScanAgain: () -> Unit,
    onSelectDevice: (BluetoothDevice) -> Unit,
    onStartSimulation: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkGunmetal)
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = NeonBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "BLUETOOTH BLE SCAN",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = "Mencari Valeton GP-180...",
                                fontSize = 10.sp,
                                color = TextTertiary
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = onScanAgain, enabled = !isScanning) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = NeonCyan
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = TextSecondary)
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Discovered Devices List
                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(SurfaceCard, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isScanning) "Sedang mencari perangkat Bluetooth di sekitar..." else "Tidak ditemukan perangkat GP-180.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pastikan Bluetooth pedal GP-180 menyala",
                                fontSize = 10.sp,
                                color = TextTertiary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(devices) { dev ->
                            val name = dev.name ?: "Unknown Device"
                            val isValeton = name.contains("Valeton", ignoreCase = true) || name.contains("GP", ignoreCase = true)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isValeton) SurfaceCardActive else SurfaceCard)
                                    .border(1.dp, if (isValeton) NeonCyan else SurfaceCardBorder, RoundedCornerShape(10.dp))
                                    .clickable { onSelectDevice(dev) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isValeton) NeonCyan else TextPrimary
                                    )
                                    Text(
                                        text = dev.address,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextTertiary
                                    )
                                }

                                Text(
                                    text = "Connect",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Virtual Rig / Simulation fallback button
                Button(
                    onClick = {
                        onStartSimulation()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceCardActive,
                        contentColor = NeonCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_simulation_btn")
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gunakan Virtual Simulator GP-180",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
