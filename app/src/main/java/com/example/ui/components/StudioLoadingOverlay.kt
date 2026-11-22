package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StudioLoadingOverlay(
    visible: Boolean,
    headline: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(180)),
        exit = fadeOut(animationSpec = tween(180)),
        modifier = modifier
    ) {
        val colors = MaterialTheme.colorScheme
        val primaryColor = colors.primary
        val secondaryColor = colors.secondary
        val onSurfaceColor = colors.onSurface

        val transition = rememberInfiniteTransition(label = "lumenreel_global_loader")
        val reelAngle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "reel_rotation"
        )
        val counterAngle by transition.animateFloat(
            initialValue = 360f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "aperture_rotation"
        )
        val pulseAlpha by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
                .testTag("global_loading_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 360.dp)
                    .border(1.dp, colors.outline, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Canvas(modifier = Modifier.size(86.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val outerRadius = size.minDimension * 0.44f
                        val innerRadius = size.minDimension * 0.25f

                        drawArc(
                            color = primaryColor.copy(alpha = 0.22f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                            size = Size(outerRadius * 2f, outerRadius * 2f),
                            style = Stroke(width = 3.5.dp.toPx())
                        )
                        drawArc(
                            color = primaryColor,
                            startAngle = reelAngle,
                            sweepAngle = 110f,
                            useCenter = false,
                            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                            size = Size(outerRadius * 2f, outerRadius * 2f),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = secondaryColor.copy(alpha = pulseAlpha),
                            startAngle = counterAngle,
                            sweepAngle = 75f,
                            useCenter = false,
                            topLeft = Offset(center.x - (outerRadius * 0.82f), center.y - (outerRadius * 0.82f)),
                            size = Size(outerRadius * 1.64f, outerRadius * 1.64f),
                            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                        )

                        rotate(degrees = reelAngle, pivot = center) {
                            drawCircle(
                                color = onSurfaceColor.copy(alpha = 0.85f),
                                radius = innerRadius,
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            val holeOrbit = innerRadius * 0.56f
                            for (i in 0 until 5) {
                                val rad = Math.toRadians((i * 72).toDouble())
                                val hx = center.x + (cos(rad) * holeOrbit).toFloat()
                                val hy = center.y + (sin(rad) * holeOrbit).toFloat()
                                drawCircle(
                                    color = primaryColor.copy(alpha = 0.9f),
                                    radius = 3.2.dp.toPx(),
                                    center = Offset(hx, hy)
                                )
                            }
                            drawCircle(
                                color = secondaryColor.copy(alpha = pulseAlpha),
                                radius = 3.8.dp.toPx(),
                                center = center
                            )
                        }
                    }

                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("global_loading_headline")
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("global_loading_subtitle")
                    )
                }
            }
        }
    }
}
