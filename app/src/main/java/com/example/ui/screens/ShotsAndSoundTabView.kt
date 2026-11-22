package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ProductionSequence
import com.example.ui.components.CinemaSet3DFullscreenDialog
import com.example.ui.components.CinemaSet3DStageCard
import com.example.ui.components.OpticalViewfinderCanvas

@Composable
fun ShotsAndSoundTabView(
    sequence: ProductionSequence,
    selectedShotIndex: Int,
    use3DStage: Boolean,
    isExporting: Boolean = false,
    onSelectShot: (Int) -> Unit,
    onToggle3DStage: (Boolean) -> Unit,
    onSaveSchedulePdf: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var fullscreen3DOpen by remember { mutableStateOf(false) }
    val activeShot = sequence.shotList.getOrNull(selectedShotIndex)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = use3DStage,
                    onClick = { onToggle3DStage(true) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.ViewInAr,
                            contentDescription = null
                        )
                    },
                    label = { Text("Escenario 3D", style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primaryContainer,
                        selectedLabelColor = colors.onPrimaryContainer,
                        selectedLeadingIconColor = colors.onPrimaryContainer,
                        containerColor = colors.surfaceVariant,
                        labelColor = colors.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("stage_mode_3d")
                )
                FilterChip(
                    selected = !use3DStage,
                    onClick = { onToggle3DStage(false) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.CropFree,
                            contentDescription = null
                        )
                    },
                    label = { Text("Visor Óptico 2D", style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.secondaryContainer,
                        selectedLabelColor = colors.onSecondaryContainer,
                        selectedLeadingIconColor = colors.onSecondaryContainer,
                        containerColor = colors.surfaceVariant,
                        labelColor = colors.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("stage_mode_2d")
                )
            }
        }

        item {
            if (use3DStage) {
                CinemaSet3DStageCard(
                    sequence = sequence,
                    activeShot = activeShot,
                    onOpenFullscreen = { fullscreen3DOpen = true }
                )
            } else {
                OpticalViewfinderCanvas(
                    aspectRatioStr = sequence.aspectRatio,
                    activeShot = activeShot
                )
            }
        }

        item {
            Button(
                onClick = onSaveSchedulePdf,
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.secondary,
                    contentColor = colors.onSecondary,
                    disabledContainerColor = colors.surfaceVariant,
                    disabledContentColor = colors.onSurfaceVariant
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("shots_save_schedule_pdf_button")
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Guardar Plan de Rodaje y Guion (PDF)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            SectionCard(
                title = "Lista de Planos (${sequence.shotList.size})",
                icon = Icons.Rounded.Videocam
            ) {
                if (sequence.shotList.isEmpty()) {
                    Text(
                        text = "No se generaron planos para esta secuencia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.78f)
                    )
                }
            }
        }

        itemsIndexed(sequence.shotList, key = { idx, s -> "${idx}_${s.shotNumber}" }) { idx, shot ->
            val selected = idx == selectedShotIndex
            Surface(
                color = if (selected) colors.primaryContainer else colors.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (selected) colors.primary else colors.outline,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectShot(idx) }
                    .testTag("shot_item_$idx")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Plano ${shot.shotNumber} · ${shot.shotType}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${shot.lensMm} · ${shot.durationSec}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) colors.onPrimaryContainer else colors.secondary
                        )
                    }
                    Text(
                        text = "Movimiento: ${shot.movement}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant.copy(alpha = 0.78f)
                    )
                    if (shot.framingNote.isNotBlank()) {
                        Text(
                            shot.framingNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
                        )
                    }
                    if (shot.psychologicalIntent.isNotBlank()) {
                        Text(
                            text = "Intención psicológica: ${shot.psychologicalIntent}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant.copy(alpha = 0.78f)
                        )
                    }
                }
            }
        }

        // Dolby Atmos & Sound Design
        item {
            SectionCard(
                title = "Diseño Sonoro Dolby Atmos & Score (${sequence.soundDesign.tempoBpm} BPM)",
                icon = Icons.Rounded.GraphicEq
            ) {
                if (sequence.soundDesign.atmosLayers.isNotEmpty()) {
                    Text("Capas Atmos", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                    sequence.soundDesign.atmosLayers.forEach { layer ->
                        Text("• $layer", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
                SpecRow("Subgraves (LFE)", sequence.soundDesign.subBassCue)
                if (sequence.soundDesign.foleyDetails.isNotEmpty()) {
                    Text("Detalles Foley", style = MaterialTheme.typography.labelSmall, color = colors.secondary)
                    sequence.soundDesign.foleyDetails.forEach { foley ->
                        Text("• $foley", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
                SpecRow("Silencios Dramáticos", sequence.soundDesign.silenceBeats)
                SpecRow("Score Musical", sequence.soundDesign.musicalScore)
            }
        }

        // Audio Analysis + Verbatim Transcription (when available)
        sequence.audioAnalysis?.let { aa ->
            item {
                SectionCard(
                    title = "Análisis del Audio Fuente y Transcripción Verbatim",
                    icon = Icons.Rounded.Mic
                ) {
                    SpecRow("Estilo de Narración", aa.narrationStyle)
                    SpecRow("Arco Emocional", aa.emotionalArc)
                    SpecRow("Ritmo y Pacing", aa.rhythmAndPacing)
                    if (aa.detectedSoundEffects.isNotEmpty()) {
                        SpecRow("Efectos Detectados", aa.detectedSoundEffects.joinToString(" · "))
                    }
                    SpecRow("Recomendaciones de Dirección", aa.directingRecommendations)
                    if (aa.transcription.isNotBlank()) {
                        Surface(
                            color = colors.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Transcripción Verbatim", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                                Text(aa.transcription, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (fullscreen3DOpen) {
        CinemaSet3DFullscreenDialog(
            sequence = sequence,
            activeShot = activeShot,
            onDismiss = { fullscreen3DOpen = false }
        )
    }
}
