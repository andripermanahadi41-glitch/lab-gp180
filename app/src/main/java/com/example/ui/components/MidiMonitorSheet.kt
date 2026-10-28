package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MidiDirection
import com.example.model.MidiLogEntry
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MidiMonitorSheet(
    logs: List<MidiLogEntry>,
    onClearLogs: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<MidiDirection?>(null) }

    val filteredLogs = remember(logs, selectedFilter) {
        if (selectedFilter == null) logs else logs.filter { it.direction == selectedFilter }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkGunmetal,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SurfaceCardBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LIVE MIDI TRAFFIC MONITOR",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Bi-directional hardware stream inspection",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClearLogs, modifier = Modifier.testTag("clear_logs_btn")) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Logs",
                            tint = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_monitor_btn")) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("ALL (${logs.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SurfaceCardActive,
                        selectedLabelColor = NeonCyan
                    )
                )

                FilterChip(
                    selected = selectedFilter == MidiDirection.TX,
                    onClick = { selectedFilter = MidiDirection.TX },
                    label = { Text("TX SENT", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyanDim,
                        selectedLabelColor = NeonCyan
                    )
                )

                FilterChip(
                    selected = selectedFilter == MidiDirection.RX,
                    onClick = { selectedFilter = MidiDirection.RX },
                    label = { Text("RX RECV", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonEmeraldDim,
                        selectedLabelColor = NeonEmerald
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada lalu lintas MIDI tercatat",
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        MidiLogItem(log)
                    }
                }
            }
        }
    }
}

@Composable
fun MidiLogItem(log: MidiLogEntry) {
    val isTx = log.direction == MidiDirection.TX
    val badgeColor = if (isTx) NeonCyan else NeonEmerald
    val badgeBg = if (isTx) NeonCyanDim else NeonEmeraldDim

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(8.dp))
            .border(0.5.dp, SurfaceCardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Direction Badge
        Box(
            modifier = Modifier
                .background(badgeBg, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isTx) "TX" else "RX",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = badgeColor
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Time
        Text(
            text = log.timestamp,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = TextTertiary
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Hex Bytes
        Text(
            text = log.hexBytes,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.width(90.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Human Description
        Text(
            text = log.description,
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}
