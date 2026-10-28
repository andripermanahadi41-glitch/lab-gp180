package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun FootswitchBoard(
    fs1Title: String,
    fs2Title: String,
    fs3Title: String,
    onTriggerFs: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkGunmetal, RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GP-180 3-FOOTSWITCH MAPPING",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Tap to trigger MIDI",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextTertiary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FootswitchButton(
                fsNumber = 1,
                label = "FOOTSWITCH 1",
                action = fs1Title,
                accentColor = NeonCyan,
                onClick = { onTriggerFs(1) },
                modifier = Modifier.weight(1f)
            )

            FootswitchButton(
                fsNumber = 2,
                label = "FOOTSWITCH 2",
                action = fs2Title,
                accentColor = NeonCyan,
                onClick = { onTriggerFs(2) },
                modifier = Modifier.weight(1f)
            )

            FootswitchButton(
                fsNumber = 3,
                label = "FS 3 (CTRL)",
                action = fs3Title,
                accentColor = NeonEmerald,
                onClick = { onTriggerFs(3) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FootswitchButton(
    fsNumber: Int,
    label: String,
    action: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.94f else 1.0f, label = "stompScale")

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(
                1.dp,
                if (isPressed) accentColor else SurfaceCardBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 6.dp)
            .testTag("footswitch_$fsNumber"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Footswitch LED Indicator Ring + Metallic Cap
        Box(
            modifier = Modifier.size(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2

                // Outer Glowing Ring
                drawCircle(
                    color = if (isPressed) accentColor else accentColor.copy(alpha = 0.35f),
                    radius = radius - 1.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Stomp Button Metallic Chrome Core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isPressed) Color(0xFF6B7280) else Color(0xFF4B5563),
                            Color(0xFF1F2937)
                        ),
                        center = center,
                        radius = radius * 0.7f
                    ),
                    radius = radius * 0.7f,
                    center = center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TextTertiary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = action,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
