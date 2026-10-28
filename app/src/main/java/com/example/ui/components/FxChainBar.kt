package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FxModule
import com.example.ui.theme.*

/**
 * Compact DSP signal chain bar for GP-180 multi-effects.
 * Provides a sleek, space-saving visual representation of the 10-module guitar signal path.
 */
@Composable
fun FxChainBar(
    modules: List<FxModule>,
    activeIndex: Int,
    onSelectModule: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modules.forEachIndexed { index, mod ->
            val isSelected = index == activeIndex
            val categoryColor = getCategoryColor(mod.id, mod.category)

            val borderModifier = if (isSelected) {
                Modifier.border(1.5.dp, NeonCyan, RoundedCornerShape(8.dp))
            } else {
                Modifier.border(0.8.dp, if (mod.isEnabled) SurfaceCardBorder else Color(0xFF22262E), RoundedCornerShape(8.dp))
            }

            Box(
                modifier = Modifier
                    .width(62.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .then(borderModifier)
                    .background(if (isSelected) SurfaceCardActive else if (mod.isEnabled) SurfaceCard else Color(0xFF13161C))
                    .clickable { onSelectModule(index) }
                    .padding(horizontal = 5.dp, vertical = 3.dp)
                    .alpha(if (mod.isEnabled) 1.0f else 0.42f)
                    .testTag("fx_module_${mod.id}"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top row: Module ID badge & mini LED dot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mod.id,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = categoryColor,
                            letterSpacing = (-0.2).sp
                        )

                        // Compact LED indicator
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (mod.isEnabled) categoryColor else Color(0xFF374151))
                        )
                    }

                    // Middle: Gear Model Name (truncated)
                    Text(
                        text = mod.name,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) TextPrimary else if (mod.isEnabled) Color(0xFFE2E8F0) else TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Bottom: Status & CC #
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (mod.isEnabled) "ON" else "BYP",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (mod.isEnabled) categoryColor else TextTertiary
                        )
                        Text(
                            text = "CC${mod.cc}",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextTertiary
                        )
                    }
                }
            }

            // Subtle signal flow separator between blocks (except last block)
            if (index < modules.size - 1) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(if (mod.isEnabled && modules[index + 1].isEnabled) NeonCyanDim else Color(0xFF262C36))
                )
            }
        }
    }
}

fun getCategoryColor(id: String, category: String): Color {
    return when (id) {
        "NR" -> ColorGate
        "WAH" -> Color(0xFFFFD600)
        "BST" -> ColorDrive
        "AMP" -> ColorAmp
        "CAB" -> ColorCab
        "EQ" -> ColorEq
        "MOD" -> ColorMod
        "DLY" -> ColorDelay
        "RVB" -> ColorReverb
        "VOL" -> Color(0xFF64B5F6)
        else -> NeonCyan
    }
}
