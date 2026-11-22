package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DirectorDraftEdits
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.SequenceVersionStatus
import com.example.data.model.StudioIntelligenceReport
import com.example.data.util.StudioLogic
import com.example.ui.components.ProductionRadarChartCard
import com.example.ui.components.readClipboardText

@Composable
fun VisionTabView(
    project: ProductionProject,
    sequence: ProductionSequence,
    directorDraft: DirectorDraftEdits,
    selectedRating: Int,
    ratingNote: String,
    intelligence: StudioIntelligenceReport?,
    onDirectorDraftChange: ((DirectorDraftEdits) -> DirectorDraftEdits) -> Unit,
    onSaveDirectorDraft: () -> Unit,
    onRatingChange: (Int) -> Unit,
    onRatingNoteChange: (String) -> Unit,
    onApplyEnhancement: (String) -> Unit,
    onRateSequence: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var copiedHex by remember { mutableStateOf<String?>(null) }
    val isDraft = sequence.versionStatus == SequenceVersionStatus.DRAFT

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Logline & Director Vision Hero
        item {
            SectionCard(title = "Visión del Director & Logline") {
                if (sequence.logline.isNotBlank()) {
                    Surface(
                        color = colors.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            text = "«${sequence.logline}»",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                Text(
                    text = directorDraft.directorVision.ifBlank { sequence.directorVision }
                        .ifBlank { "Sin notas de visión registradas." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }

        // Editable Director Adjustments (Preserved across tabs in StudioViewModel)
        item {
            SectionCard(
                title = "Ajustes de Dirección y Óptica",
                icon = Icons.Rounded.Tune
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDraft) {
                            "Tus ajustes se conservan al cambiar entre pestañas y puedes guardarlos en la secuencia."
                        } else {
                            "Secuencia en Versión Final (solo lectura técnica). Puedes actualizar la Biblia de Continuidad."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.78f),
                        modifier = Modifier.weight(1f)
                    )
                    if (directorDraft.isDirty) {
                        Surface(
                            color = colors.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Editado",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("director_dirty_badge")
                            )
                        }
                    }
                }

                Text("Firma de dirección", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioLogic.DIRECTOR_STYLES.forEach { preset ->
                        FilterChip(
                            selected = directorDraft.directorStyle == preset.label,
                            enabled = isDraft,
                            onClick = {
                                onDirectorDraftChange {
                                    it.copy(
                                        directorStyle = preset.label,
                                        aspectRatio = preset.defaultRatio,
                                        cameraSystem = preset.defaultCamera,
                                        lensPackage = preset.defaultLenses
                                    )
                                }
                            },
                            label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primaryContainer,
                                selectedLabelColor = colors.onPrimaryContainer,
                                containerColor = colors.surfaceVariant,
                                labelColor = colors.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("vision_style_${preset.id}")
                        )
                    }
                }

                Text("Relación de aspecto", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioLogic.ASPECT_RATIO_OPTIONS.forEach { (ratio, label) ->
                        FilterChip(
                            selected = directorDraft.aspectRatio == ratio,
                            enabled = isDraft,
                            onClick = { onDirectorDraftChange { it.copy(aspectRatio = ratio) } },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primaryContainer,
                                selectedLabelColor = colors.onPrimaryContainer,
                                containerColor = colors.surfaceVariant,
                                labelColor = colors.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("vision_ratio_$ratio")
                        )
                    }
                }

                OutlinedTextField(
                    value = directorDraft.cameraSystem,
                    onValueChange = { value -> onDirectorDraftChange { it.copy(cameraSystem = value) } },
                    enabled = isDraft,
                    label = { Text("Cámara cinematográfica") },
                    singleLine = true,
                    colors = visionFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("director_camera_input")
                )

                OutlinedTextField(
                    value = directorDraft.lensPackage,
                    onValueChange = { value -> onDirectorDraftChange { it.copy(lensPackage = value) } },
                    enabled = isDraft,
                    label = { Text("Paquete de lentes") },
                    singleLine = true,
                    colors = visionFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("director_lenses_input")
                )

                OutlinedTextField(
                    value = directorDraft.colorTempKelvin,
                    onValueChange = { value -> onDirectorDraftChange { it.copy(colorTempKelvin = value) } },
                    enabled = isDraft,
                    label = { Text("Temperatura Kelvin / Color") },
                    singleLine = true,
                    colors = visionFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("director_kelvin_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Visión de dirección", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    if (isDraft) {
                        TextButton(
                            onClick = {
                                val clip = readClipboardText(context) ?: return@TextButton
                                onDirectorDraftChange {
                                    val next = if (it.directorVision.isBlank()) clip else "${it.directorVision.trim()}\n$clip"
                                    it.copy(directorVision = next)
                                }
                            },
                            modifier = Modifier.testTag("director_paste_vision_button")
                        ) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = colors.primary, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Pegar", color = colors.primary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                OutlinedTextField(
                    value = directorDraft.directorVision,
                    onValueChange = { value -> onDirectorDraftChange { it.copy(directorVision = value) } },
                    enabled = isDraft,
                    minLines = 2,
                    maxLines = 5,
                    colors = visionFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("director_vision_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Biblia de Continuidad", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    TextButton(
                        onClick = {
                            val clip = readClipboardText(context) ?: return@TextButton
                            onDirectorDraftChange {
                                val next = if (it.continuityBible.isBlank()) clip else "${it.continuityBible.trim()}\n$clip"
                                it.copy(continuityBible = next)
                            }
                        },
                        modifier = Modifier.testTag("director_paste_bible_button")
                    ) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = colors.primary, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Pegar", color = colors.primary, style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedTextField(
                    value = directorDraft.continuityBible,
                    onValueChange = { value -> onDirectorDraftChange { it.copy(continuityBible = value) } },
                    minLines = 2,
                    maxLines = 5,
                    colors = visionFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("director_bible_input")
                )

                Button(
                    onClick = onSaveDirectorDraft,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_director_edits_button")
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Guardar ajustes de dirección", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Radar Chart
        item {
            ProductionRadarChartCard(project = project, activeSequence = sequence)
        }

        // Camera, Optics & LUT summary + Swatches
        item {
            SectionCard(
                title = "Óptica, Cámara y Colorimetría",
                icon = Icons.Rounded.CameraAlt
            ) {
                SpecRow("Sistema de Cámara", directorDraft.cameraSystem.ifBlank { sequence.cameraSystem })
                SpecRow("Paquete de Lentes", directorDraft.lensPackage.ifBlank { sequence.lensPackage })
                SpecRow("Emulsión / LUT", directorDraft.filmStockOrLut.ifBlank { sequence.filmStockOrLut })
                SpecRow(
                    "Formato & Estilo",
                    "${directorDraft.aspectRatio.ifBlank { sequence.aspectRatio }} · ${directorDraft.directorStyle.ifBlank { sequence.directorStyle }}"
                )

                if (sequence.colorPalette.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Paleta de Color (pulsa para copiar HEX)",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant.copy(alpha = 0.78f)
                    )
                    if (copiedHex != null) {
                        Text(
                            text = "Copiado al portapapeles: $copiedHex",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondary
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val fallbackSwatchColor = colors.primary
                        sequence.colorPalette.forEachIndexed { idx, swatch ->
                            val parsedColor = remember(swatch.hex, fallbackSwatchColor) {
                                parseSafeHexColor(swatch.hex, fallbackSwatchColor)
                            }
                            Surface(
                                color = colors.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
                                    .clickable {
                                        copyToClipboard(context, swatch.hex)
                                        copiedHex = swatch.hex
                                    }
                                    .testTag("color_swatch_$idx")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(parsedColor)
                                            .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${swatch.name} · ${swatch.hex}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = colors.onSurface
                                        )
                                        if (swatch.role.isNotBlank()) {
                                            Text(
                                                text = swatch.role,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.onSurfaceVariant.copy(alpha = 0.78f)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Copiar color ${swatch.hex}",
                                        tint = colors.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Lighting Plot Kelvin
        item {
            SectionCard(
                title = "Esquema de Iluminación Kelvin",
                icon = Icons.Rounded.WbSunny
            ) {
                SpecRow("Luz Principal (Key)", sequence.lighting.keyLight)
                SpecRow("Ratio de Relleno", sequence.lighting.fillRatio)
                SpecRow("Contraluz / Rim", sequence.lighting.rimAndBacklight)
                SpecRow(
                    "Temperatura Kelvin",
                    directorDraft.colorTempKelvin.ifBlank { sequence.lighting.colorTemperatureKelvin }
                )
                SpecRow("Atmósfera & Haze", sequence.lighting.atmosphereAndHaze)
            }
        }

        // VFX & Practical SFX
        item {
            SectionCard(
                title = "VFX Digital, SFX Prácticos y Arte",
                icon = Icons.Rounded.Flare
            ) {
                if (sequence.vfx.cgiElements.isNotEmpty()) {
                    Text("VFX / Simulaciones CGI", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                    sequence.vfx.cgiElements.forEach { item ->
                        Text("• $item", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
                if (sequence.vfx.practicalSfx.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Efectos Prácticos en Set", style = MaterialTheme.typography.labelMedium, color = colors.secondary)
                    sequence.vfx.practicalSfx.forEach { item ->
                        Text("• $item", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
                SpecRow("Locaciones y Sets", sequence.vfx.locationsAndSets)
                SpecRow("Vestuario y Maquillaje", sequence.vfx.wardrobeAndMakeup)
            }
        }

        // Studio Intelligence Recommendations
        if (intelligence != null) {
            item {
                SectionCard(
                    title = "Studio Intelligence · Patrones Históricos",
                    icon = Icons.Rounded.AutoAwesome
                ) {
                    Text(
                        text = "${intelligence.finalizedSequencesCount} secuencias aprobadas · ${intelligence.totalTrackedOutcomes} aprendizajes de alta valoración",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.78f)
                    )

                    if (intelligence.topCameraPatterns.isNotEmpty()) {
                        intelligence.topCameraPatterns.take(2).forEach { pat ->
                            Surface(
                                color = colors.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(pat.label, style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
                                    Text(pat.detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant.copy(alpha = 0.78f))
                                    Text(
                                        text = "★ ${"%.1f".format(pat.avgRating)} · ${pat.occurrences} usos",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.primary
                                    )
                                }
                            }
                        }
                    }

                    intelligence.enhancements.forEachIndexed { idx, enh ->
                        Surface(
                            color = colors.secondaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.secondary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${enh.title} (${enh.confidenceScore}% confianza)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.onSecondaryContainer
                                )
                                Text(enh.recommendation, style = MaterialTheme.typography.bodySmall, color = colors.onSecondaryContainer)
                                Text(
                                    enh.rationale,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                                OutlinedButton(
                                    onClick = { onApplyEnhancement(enh.continuitySnippet) },
                                    modifier = Modifier.testTag("apply_enhancement_$idx")
                                ) {
                                    Text("Aplicar a Biblia de Continuidad", color = colors.onSecondaryContainer)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sequence Rating Form
        item {
            SectionCard(title = "Calificar Resultado y Alimentar Memoria") {
                Text(
                    text = sequence.sequenceRating?.let { "Valoración actual: $it/5 ★" }
                        ?: "Asigna una calificación de 1 a 5 estrellas para entrenar Studio Intelligence:",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.78f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { onRatingChange(star) },
                            modifier = Modifier.testTag("rate_star_$star")
                        ) {
                            Icon(
                                imageVector = if (star <= selectedRating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = "$star estrellas",
                                tint = if (star <= selectedRating) colors.primary else colors.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = ratingNote,
                    onValueChange = onRatingNoteChange,
                    placeholder = {
                        Text(
                            "Nota opcional sobre óptica, luz o ritmo para futuras secuencias…",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant.copy(alpha = 0.72f)
                        )
                    },
                    colors = visionFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rating_note_input")
                )
                Button(
                    onClick = { onRateSequence(selectedRating, ratingNote) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_rating_button")
                ) {
                    Text("Guardar Calificación ($selectedRating★)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun visionFieldColors(): androidx.compose.material3.TextFieldColors {
    val colors = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.outline,
        disabledBorderColor = colors.outline.copy(alpha = 0.5f),
        focusedTextColor = colors.onSurface,
        unfocusedTextColor = colors.onSurface,
        disabledTextColor = colors.onSurfaceVariant,
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = colors.onSurfaceVariant.copy(alpha = 0.78f),
        disabledLabelColor = colors.onSurfaceVariant.copy(alpha = 0.6f)
    )
}

@Composable
internal fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primary
                )
            }
            content()
        }
    }
}

@Composable
internal fun SpecRow(label: String, value: String) {
    if (value.isBlank()) return
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant.copy(alpha = 0.78f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
    }
}

private fun parseSafeHexColor(hex: String, fallback: Color): Color {
    val clean = hex.trim().removePrefix("#")
    return try {
        val parsed = clean.toLong(16)
        when (clean.length) {
            6 -> Color((0xFF000000L or parsed).toInt())
            8 -> Color(parsed.toInt())
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

internal fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("LumenReel", text))
}
