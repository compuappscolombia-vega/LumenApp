package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.ShotSpec
import com.example.data.util.ProductionProgress
import com.example.data.util.StudioLogic
import com.example.ui.theme.AmberGold
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.MutedStone
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioDeepPanel
import com.example.ui.theme.TealAtmos
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ProductionRadarChartCard(
    project: ProductionProject,
    activeSequence: ProductionSequence
) {
    val report = ProductionProgress.computeProductionRadarMetrics(project, activeSequence)

    Surface(
        color = StudioCard,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Radar de Progreso de Producción",
                        style = MaterialTheme.typography.titleMedium,
                        color = AmberGold
                    )
                    Text(
                        text = "${report.sequencesCount} secuencias · ${report.finalCount} versión final",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedStone
                    )
                }
                Surface(
                    color = StudioDeepPanel,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "${report.overallProjectScore}% Global",
                        style = MaterialTheme.typography.labelMedium,
                        color = IvoryPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Custom Canvas 4-Axis Radar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .semantics {
                        contentDescription = "Gráfico radar comparando Guion, Iluminación, VFX y Diseño Sonoro"
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = min(size.width, size.height) * 0.38f
                    val axesCount = report.axes.size.coerceAtLeast(4)

                    // Rings at 25%, 50%, 75%, 100%
                    listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { level ->
                        val ringPath = Path()
                        for (i in 0 until axesCount) {
                            val angle = (-PI / 2.0 + i * (2.0 * PI / axesCount)).toFloat()
                            val px = cx + cos(angle) * radius * level
                            val py = cy + sin(angle) * radius * level
                            if (i == 0) ringPath.moveTo(px, py) else ringPath.lineTo(px, py)
                        }
                        ringPath.close()
                        drawPath(
                            path = ringPath,
                            color = Color.White.copy(alpha = 0.10f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Axis spokes
                    for (i in 0 until axesCount) {
                        val angle = (-PI / 2.0 + i * (2.0 * PI / axesCount)).toFloat()
                        drawLine(
                            color = Color.White.copy(alpha = 0.14f),
                            start = Offset(cx, cy),
                            end = Offset(cx + cos(angle) * radius, cy + sin(angle) * radius),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Project Average Polygon (Amber Gold)
                    val projectPath = Path()
                    report.axes.forEachIndexed { i, axis ->
                        val angle = (-PI / 2.0 + i * (2.0 * PI / axesCount)).toFloat()
                        val scale = (axis.projectScore / 100f).coerceIn(0f, 1f)
                        val px = cx + cos(angle) * radius * scale
                        val py = cy + sin(angle) * radius * scale
                        if (i == 0) projectPath.moveTo(px, py) else projectPath.lineTo(px, py)
                    }
                    projectPath.close()
                    drawPath(projectPath, color = AmberGold.copy(alpha = 0.22f))
                    drawPath(projectPath, color = AmberGold, style = Stroke(width = 2.dp.toPx()))

                    // Active Sequence Polygon (Teal Atmos)
                    val seqPath = Path()
                    report.axes.forEachIndexed { i, axis ->
                        val angle = (-PI / 2.0 + i * (2.0 * PI / axesCount)).toFloat()
                        val scale = (axis.sequenceScore / 100f).coerceIn(0f, 1f)
                        val px = cx + cos(angle) * radius * scale
                        val py = cy + sin(angle) * radius * scale
                        if (i == 0) seqPath.moveTo(px, py) else seqPath.lineTo(px, py)
                    }
                    seqPath.close()
                    drawPath(seqPath, color = TealAtmos.copy(alpha = 0.18f))
                    drawPath(seqPath, color = TealAtmos, style = Stroke(width = 2.dp.toPx()))
                }

                // Axis Labels Positioned Cleanly at Corners
                Column(
                    modifier = Modifier.fillMaxWidth().height(210.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Guion ${report.axes.getOrNull(0)?.sequenceScore ?: 0}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = IvoryPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sonido\n${report.axes.getOrNull(3)?.sequenceScore ?: 0}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = IvoryPrimary
                        )
                        Text(
                            text = "Iluminación\n${report.axes.getOrNull(1)?.sequenceScore ?: 0}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = IvoryPrimary
                        )
                    }
                    Text(
                        text = "VFX ${report.axes.getOrNull(2)?.sequenceScore ?: 0}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = IvoryPrimary
                    )
                }
            }

            // Legend + Progress Breakdown
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.height(8.dp).fillMaxWidth(0.05f).clip(RoundedCornerShape(4.dp)).background(AmberGold))
                    Text("Proyecto", style = MaterialTheme.typography.labelSmall, color = AmberGold)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.height(8.dp).fillMaxWidth(0.05f).clip(RoundedCornerShape(4.dp)).background(TealAtmos))
                    Text("Secuencia activa", style = MaterialTheme.typography.labelSmall, color = TealAtmos)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                report.axes.forEach { axis ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(axis.axis, style = MaterialTheme.typography.labelLarge, color = IvoryPrimary)
                            Text(
                                text = "${axis.sequenceScore}% sec · ${axis.projectScore}% proy",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedStone
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (axis.sequenceScore / 100f).coerceIn(0f, 1f) },
                            color = AmberGold,
                            trackColor = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp))
                        )
                        Text(axis.detailSummary, style = MaterialTheme.typography.bodySmall, color = MutedStone)
                    }
                }
            }
        }
    }
}

@Composable
fun OpticalViewfinderCanvas(
    aspectRatioStr: String,
    activeShot: ShotSpec?
) {
    val ratioValue = StudioLogic.parseAspectRatio(aspectRatioStr)

    Surface(
        color = Color(0xFF07070A),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .padding(12.dp)
                .semantics {
                    contentDescription = activeShot?.let {
                        "Diagrama óptico de encuadre para plano ${it.shotNumber} formato $aspectRatioStr"
                    } ?: "Visor de encuadre cinematográfico"
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(166.dp)) {
                val maxW = size.width * 0.92f
                val maxH = size.height * 0.92f
                var frameW = maxW
                var frameH = frameW / ratioValue
                if (frameH > maxH) {
                    frameH = maxH
                    frameW = frameH * ratioValue
                }
                val fx = (size.width - frameW) / 2f
                val fy = (size.height - frameH) / 2f

                // Frame box
                drawRect(
                    color = Color(0xFF12141B),
                    topLeft = Offset(fx, fy),
                    size = Size(frameW, frameH)
                )
                drawRect(
                    color = AmberGold.copy(alpha = 0.85f),
                    topLeft = Offset(fx, fy),
                    size = Size(frameW, frameH),
                    style = Stroke(width = 1.4.dp.toPx())
                )

                // Rule of thirds dashed lines
                val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                val gridColor = Color.White.copy(alpha = 0.16f)
                for (i in 1..2) {
                    val vx = fx + frameW * (i / 3f)
                    drawLine(gridColor, Offset(vx, fy), Offset(vx, fy + frameH), strokeWidth = 1f, pathEffect = dash)
                    val hy = fy + frameH * (i / 3f)
                    drawLine(gridColor, Offset(fx, hy), Offset(fx + frameW, hy), strokeWidth = 1f, pathEffect = dash)
                }

                // Center crosshairs
                val cx = size.width / 2f
                val cy = size.height / 2f
                val arm = 10.dp.toPx()
                drawLine(AmberGold.copy(alpha = 0.65f), Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = 1.dp.toPx())
                drawLine(AmberGold.copy(alpha = 0.65f), Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = 1.dp.toPx())
            }

            if (activeShot != null) {
                Text(
                    text = "${activeShot.shotType} · ${activeShot.lensMm}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberGold,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                )
            }
            Text(
                text = aspectRatioStr,
                style = MaterialTheme.typography.labelSmall,
                color = MutedStone,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
            )
        }
    }
}
