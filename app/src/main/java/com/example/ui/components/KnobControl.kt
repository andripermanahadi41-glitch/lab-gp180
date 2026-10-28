package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun KnobControl(
    title: String,
    value: Int,
    range: ClosedFloatingPointRange<Float> = 0f..100f,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .background(SurfaceCard, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("knob_$title"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Text(
                text = value.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = NeonCyan
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Rotary Dial Canvas with Drag Support
        val animatedValue by animateFloatAsState(targetValue = value.toFloat(), label = "knobAngle")

        Box(
            modifier = Modifier
                .size(68.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragAccumulator = value.toFloat() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Vertical drag: upward increases, downward decreases
                            dragAccumulator -= dragAmount.y * 0.6f
                            val clamped = dragAccumulator.coerceIn(range.start, range.endInclusive)
                            onValueChange(clamped.toInt())
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 5.dp.toPx()
                val radius = (size.minDimension - strokeWidth * 2) / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Background track arc (240 degrees sweep from 150 deg to 390 deg)
                val startAngle = 135f
                val totalSweep = 270f

                drawArc(
                    color = SliderTrack,
                    startAngle = startAngle,
                    sweepAngle = totalSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active glowing sweep
                val fraction = (animatedValue - range.start) / (range.endInclusive - range.start)
                val currentSweep = totalSweep * fraction.coerceIn(0f, 1f)

                if (currentSweep > 0) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(NeonBlue, NeonCyan),
                            center = center
                        ),
                        startAngle = startAngle,
                        sweepAngle = currentSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Inner Knob Circle
                val knobRadius = radius * 0.72f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF263544), Color(0xFF111822)),
                        center = center,
                        radius = knobRadius
                    ),
                    radius = knobRadius,
                    center = center
                )
                drawCircle(
                    color = SurfaceCardBorder,
                    radius = knobRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Knob Indicator Dot / Needle
                val needleAngleRad = Math.toRadians((startAngle + currentSweep).toDouble())
                val needleLength = knobRadius * 0.75f
                val needleEnd = Offset(
                    (center.x + needleLength * cos(needleAngleRad)).toFloat(),
                    (center.y + needleLength * sin(needleAngleRad)).toFloat()
                )
                val needleStart = Offset(
                    (center.x + (knobRadius * 0.25f) * cos(needleAngleRad)).toFloat(),
                    (center.y + (knobRadius * 0.25f) * sin(needleAngleRad)).toFloat()
                )

                drawLine(
                    color = NeonCyan,
                    start = needleStart,
                    end = needleEnd,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Linear fine-adjustment slider
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = SliderTrack
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        )
    }
}
