package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ProductionProject
import com.example.data.util.ProjectActivityPoint
import com.example.data.util.ProjectSequenceBarItem
import com.example.data.util.SequenceScriptMetric
import com.example.data.util.StudioMetricsCalculator
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsDashboardScreen(
    projects: List<ProductionProject>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val colors = MaterialTheme.colorScheme
    val report = remember(projects) { StudioMetricsCalculator.computeMetrics(projects) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("metrics_dashboard_screen"),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.onSurface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("metrics_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver a Producciones",
                            tint = colors.onSurface
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_lumenreel_logo),
                            contentDescription = "Logo LumenReel",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .border(1.dp, colors.primary.copy(alpha = 0.65f), RoundedCornerShape(9.dp))
                        )
                        Column {
                            Text(
                                text = "Analítica del Estudio",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Secuencias, extensión de guion y actividad",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(colors.surfaceVariant.copy(alpha = 0.45f), colors.background, colors.background)
                    )
                )
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricsKpiCard(
                            title = "Secuencias",
                            primaryValue = report.totalSequences.toString(),
                            subtitle = "${report.finalSequences} final · ${report.draftSequences} borrador",
                            accent = colors.primary,
                            icon = Icons.Rounded.Movie,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("metrics_total_sequences_card")
                        )
                        MetricsKpiCard(
                            title = "Guion Total",
                            primaryValue = "${report.totalScriptWords}",
                            subtitle = "${report.totalScenes} esc · ${report.totalBeats} beats · ~${report.estimatedMinutes}m",
                            accent = colors.secondary,
                            icon = Icons.Rounded.Description,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("metrics_script_length_card")
                        )
                    }
                }

                item {
                    MetricsKpiWideBanner(
                        totalProjects = report.totalProjects,
                        avgWords = report.averageWordsPerSequence,
                        activityPointsCount = report.activityTimeline.sumOf { it.totalEvents },
                        modifier = Modifier.testTag("metrics_activity_kpi_card")
                    )
                }

                item {
                    SequenceDistributionChartCard(
                        items = report.projectBars,
                        totalSequences = report.totalSequences
                    )
                }

                item {
                    ScriptLengthChartCard(
                        items = report.scriptLengthBySequence,
                        totalWords = report.totalScriptWords,
                        totalScenes = report.totalScenes
                    )
                }

                item {
                    ActivityTimelineChartCard(
                        points = report.activityTimeline,
                        modifier = Modifier.testTag("metrics_activity_chart")
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricsKpiCard(
    title: String,
    primaryValue: String,
    subtitle: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, colors.outline, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.headlineMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricsKpiWideBanner(
    totalProjects: Int,
    avgWords: Int,
    activityPointsCount: Int,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Insights, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Column {
                    Text(
                        text = "$totalProjects producciones activas · $activityPointsCount hitos registrados",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Promedio de guion: $avgWords palabras por secuencia",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SequenceDistributionChartCard(
    items: List<ProjectSequenceBarItem>,
    totalSequences: Int
) {
    val colors = MaterialTheme.colorScheme
    val primaryColor = colors.primary
    val secondaryColor = colors.secondary
    val onSurfaceColor = colors.onSurface
    val shown = remember(items) { items.take(6) }
    val maxTotal = remember(shown) { (shown.maxOfOrNull { it.totalCount } ?: 1).coerceAtLeast(1) }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.AutoGraph, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Secuencias Generadas por Producción",
                        style = MaterialTheme.typography.titleSmall,
                        color = primaryColor
                    )
                }
                Text(
                    text = "$totalSequences total",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }

            if (shown.isEmpty()) {
                MetricsEmptyState(
                    message = "Aún no hay producciones con secuencias para graficar."
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendDot(color = primaryColor, label = "Borrador")
                    LegendDot(color = secondaryColor, label = "Versión Final")
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .semantics {
                            contentDescription = "Gráfico de barras de secuencias generadas en borrador y versión final por producción"
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barCount = shown.size
                        val slotW = size.width / barCount.coerceAtLeast(1)
                        val barW = (slotW * 0.48f).coerceAtMost(44.dp.toPx())
                        val chartBottom = size.height - 8.dp.toPx()
                        val chartTop = 10.dp.toPx()
                        val usableH = (chartBottom - chartTop).coerceAtLeast(24f)

                        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        drawLine(
                            color = onSurfaceColor.copy(alpha = 0.10f),
                            start = Offset(0f, chartTop + usableH * 0.5f),
                            end = Offset(size.width, chartTop + usableH * 0.5f),
                            strokeWidth = 1f,
                            pathEffect = dash
                        )
                        drawLine(
                            color = onSurfaceColor.copy(alpha = 0.20f),
                            start = Offset(0f, chartBottom),
                            end = Offset(size.width, chartBottom),
                            strokeWidth = 1.2f
                        )

                        shown.forEachIndexed { idx, item ->
                            val cx = slotW * idx + (slotW - barW) / 2f
                            val totalH = (item.totalCount.toFloat() / maxTotal.toFloat()) * usableH
                            val finalH = if (item.totalCount > 0) {
                                totalH * (item.finalCount.toFloat() / item.totalCount.toFloat())
                            } else 0f
                            val draftH = (totalH - finalH).coerceAtLeast(0f)

                            if (totalH <= 0f) {
                                drawRoundRect(
                                    color = onSurfaceColor.copy(alpha = 0.10f),
                                    topLeft = Offset(cx, chartBottom - 4.dp.toPx()),
                                    size = Size(barW, 4.dp.toPx()),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            } else {
                                if (draftH > 0f) {
                                    drawRoundRect(
                                        color = primaryColor,
                                        topLeft = Offset(cx, chartBottom - totalH),
                                        size = Size(barW, draftH),
                                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )
                                }
                                if (finalH > 0f) {
                                    drawRoundRect(
                                        color = secondaryColor,
                                        topLeft = Offset(cx, chartBottom - finalH),
                                        size = Size(barW, finalH),
                                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    shown.forEach { item ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = item.totalCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.projectTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScriptLengthChartCard(
    items: List<SequenceScriptMetric>,
    totalWords: Int,
    totalScenes: Int
) {
    val colors = MaterialTheme.colorScheme
    val primaryColor = colors.primary
    val secondaryColor = colors.secondary
    val onSurfaceColor = colors.onSurface
    val topItems = remember(items) { items.take(6) }
    val maxScale = remember(topItems) {
        (topItems.maxOfOrNull { max(it.wordsCount, it.beatsCount * 12) } ?: 1).coerceAtLeast(1)
    }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Description, contentDescription = null, tint = secondaryColor, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Extensión de Guion por Secuencia",
                        style = MaterialTheme.typography.titleSmall,
                        color = secondaryColor
                    )
                }
                Text(
                    text = "$totalWords pal · $totalScenes esc",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }

            if (topItems.isEmpty()) {
                MetricsEmptyState(
                    message = "Produce o redacta una secuencia para analizar extensión en palabras, escenas y beats."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    topItems.forEach { metric ->
                        val ratio = (max(metric.wordsCount, metric.beatsCount * 12).toFloat() / maxScale.toFloat())
                            .coerceIn(0.06f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${metric.sequenceTitle} · ${metric.projectTitle}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${metric.wordsCount} pal · ${metric.scenesCount} esc · ${metric.beatsCount} beats",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryColor
                                )
                            }
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                            ) {
                                drawRoundRect(
                                    color = onSurfaceColor.copy(alpha = 0.10f),
                                    size = size,
                                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                                )
                                drawRoundRect(
                                    brush = Brush.horizontalGradient(listOf(secondaryColor, primaryColor)),
                                    size = Size(size.width * ratio, size.height),
                                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityTimelineChartCard(
    points: List<ProjectActivityPoint>,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val primaryColor = colors.primary
    val secondaryColor = colors.secondary
    val onSurfaceColor = colors.onSurface
    val surfaceColor = colors.surface
    val maxEvents = remember(points) { (points.maxOfOrNull { it.totalEvents } ?: 1).coerceAtLeast(1) }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Timeline, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Actividad de Proyectos en el Tiempo",
                        style = MaterialTheme.typography.titleSmall,
                        color = primaryColor
                    )
                }
                Text(
                    text = "${points.sumOf { it.totalEvents }} eventos",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }

            if (points.isEmpty()) {
                MetricsEmptyState(
                    message = "La actividad cronológica aparecerá conforme crees proyectos y guardes secuencias."
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendDot(color = primaryColor, label = "Creaciones + ediciones")
                    LegendDot(color = secondaryColor, label = "Hitos de guardado")
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .semantics {
                            contentDescription = "Gráfico lineal temporal de actividad de proyectos y secuencias"
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val padX = 16.dp.toPx()
                        val padTop = 14.dp.toPx()
                        val padBottom = 14.dp.toPx()
                        val plotW = (size.width - padX * 2f).coerceAtLeast(24f)
                        val plotH = (size.height - padTop - padBottom).coerceAtLeast(24f)

                        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        listOf(0f, 0.5f, 1f).forEach { frac ->
                            val gy = padTop + plotH * frac
                            drawLine(
                                color = onSurfaceColor.copy(alpha = 0.12f),
                                start = Offset(padX, gy),
                                end = Offset(padX + plotW, gy),
                                strokeWidth = 1f,
                                pathEffect = dash
                            )
                        }

                        val coords = points.mapIndexed { idx, pt ->
                            val x = if (points.size == 1) {
                                padX + plotW / 2f
                            } else {
                                padX + (idx.toFloat() / (points.size - 1).toFloat()) * plotW
                            }
                            val yRatio = (pt.totalEvents.toFloat() / maxEvents.toFloat()).coerceIn(0f, 1f)
                            val y = padTop + plotH * (1f - yRatio)
                            Offset(x, y)
                        }

                        if (coords.size >= 2) {
                            val areaPath = Path().apply {
                                moveTo(coords.first().x, padTop + plotH)
                                coords.forEach { pt -> lineTo(pt.x, pt.y) }
                                lineTo(coords.last().x, padTop + plotH)
                                close()
                            }
                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    listOf(primaryColor.copy(alpha = 0.30f), Color.Transparent)
                                )
                            )

                            val linePath = Path().apply {
                                coords.forEachIndexed { idx, pt ->
                                    if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                                }
                            }
                            drawPath(
                                path = linePath,
                                color = primaryColor,
                                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        coords.forEach { pt ->
                            drawCircle(
                                color = surfaceColor,
                                radius = 6.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = secondaryColor,
                                radius = 4.2.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    points.forEach { pt ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${pt.totalEvents}",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = pt.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MetricsEmptyState(message: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(14.dp)
        )
    }
}
