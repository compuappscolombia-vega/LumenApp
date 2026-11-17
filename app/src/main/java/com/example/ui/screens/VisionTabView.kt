package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.StudioIntelligenceReport
import com.example.ui.components.ProductionRadarChartCard
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.MutedStone
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioDeepPanel
import com.example.ui.theme.TealAtmos
import com.example.ui.theme.TealContainer
import com.example.ui.theme.WarmSilver

@Composable
fun VisionTabView(
    project: ProductionProject,
    sequence: ProductionSequence,
    intelligence: StudioIntelligenceReport?,
    onApplyEnhancement: (String) -> Unit,
    onRateSequence: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var copiedHex by remember { mutableStateOf<String?>(null) }
    var selectedRating by remember(sequence.id) { mutableIntStateOf(sequence.sequenceRating ?: 5) }
    var ratingNote by remember(sequence.id) { mutableStateOf("") }

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
                        color = AmberContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AmberGold.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            text = "«${sequence.logline}»",
                            style = MaterialTheme.typography.titleMedium,
                            color = IvoryPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                Text(
                    text = sequence.directorVision.ifBlank { "Sin notas de visión registradas." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmSilver
                )
            }
        }

        // Radar Chart
        item {
            ProductionRadarChartCard(project = project, activeSequence = sequence)
        }

        // Camera, Optics & LUT
        item {
            SectionCard(
                title = "Óptica, Cámara y Colorimetría",
                icon = Icons.Rounded.CameraAlt
            ) {
                SpecRow("Sistema de Cámara", sequence.cameraSystem)
                SpecRow("Paquete de Lentes", sequence.lensPackage)
                SpecRow("Emulsión / LUT", sequence.filmStockOrLut)
                SpecRow("Formato & Estilo", "${sequence.aspectRatio} · ${sequence.directorStyle}")

                if (sequence.colorPalette.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Paleta de Color (pulsa para copiar HEX)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedStone
                    )
                    if (copiedHex != null) {
                        Text(
                            text = "Copiado al portapapeles: $copiedHex",
                            style = MaterialTheme.typography.labelSmall,
                            color = TealAtmos
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        sequence.colorPalette.forEachIndexed { idx, swatch ->
                            val parsedColor = remember(swatch.hex) { parseSafeHexColor(swatch.hex) }
                            Surface(
                                color = StudioDeepPanel,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
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
                                            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${swatch.name} · ${swatch.hex}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = IvoryPrimary
                                        )
                                        if (swatch.role.isNotBlank()) {
                                            Text(
                                                text = swatch.role,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MutedStone
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Copiar color ${swatch.hex}",
                                        tint = MutedStone,
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
                SpecRow("Temperatura Kelvin", sequence.lighting.colorTemperatureKelvin)
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
                    Text("VFX / Simulaciones CGI", style = MaterialTheme.typography.labelMedium, color = AmberGold)
                    sequence.vfx.cgiElements.forEach { item ->
                        Text("• $item", style = MaterialTheme.typography.bodySmall, color = WarmSilver)
                    }
                }
                if (sequence.vfx.practicalSfx.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Efectos Prácticos en Set", style = MaterialTheme.typography.labelMedium, color = TealAtmos)
                    sequence.vfx.practicalSfx.forEach { item ->
                        Text("• $item", style = MaterialTheme.typography.bodySmall, color = WarmSilver)
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
                        color = MutedStone
                    )

                    if (intelligence.topCameraPatterns.isNotEmpty()) {
                        intelligence.topCameraPatterns.take(2).forEach { pat ->
                            Surface(
                                color = StudioDeepPanel,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(pat.label, style = MaterialTheme.typography.labelLarge, color = IvoryPrimary)
                                    Text(pat.detail, style = MaterialTheme.typography.bodySmall, color = MutedStone)
                                    Text(
                                        text = "★ ${"%.1f".format(pat.avgRating)} · ${pat.occurrences} usos",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberGold
                                    )
                                }
                            }
                        }
                    }

                    intelligence.enhancements.forEachIndexed { idx, enh ->
                        Surface(
                            color = TealContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, TealAtmos.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${enh.title} (${enh.confidenceScore}% confianza)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TealAtmos
                                )
                                Text(enh.recommendation, style = MaterialTheme.typography.bodySmall, color = IvoryPrimary)
                                Text(enh.rationale, style = MaterialTheme.typography.labelSmall, color = MutedStone)
                                OutlinedButton(
                                    onClick = { onApplyEnhancement(enh.continuitySnippet) },
                                    modifier = Modifier.testTag("apply_enhancement_$idx")
                                ) {
                                    Text("Aplicar a Biblia de Continuidad", color = IvoryPrimary)
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
                    color = MutedStone
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { selectedRating = star },
                            modifier = Modifier.testTag("rate_star_$star")
                        ) {
                            Icon(
                                imageVector = if (star <= selectedRating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = "$star estrellas",
                                tint = if (star <= selectedRating) AmberGold else MutedStone,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = ratingNote,
                    onValueChange = { ratingNote = it },
                    placeholder = {
                        Text(
                            "Nota opcional sobre óptica, luz o ritmo para futuras secuencias…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedStone
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberGold,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = IvoryPrimary,
                        unfocusedTextColor = IvoryPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rating_note_input")
                )
                Button(
                    onClick = {
                        onRateSequence(selectedRating, ratingNote)
                        ratingNote = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberGold,
                        contentColor = ObsidianBg
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
internal fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        color = StudioCard,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
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
                        tint = AmberGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AmberGold
                )
            }
            content()
        }
    }
}

@Composable
internal fun SpecRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MutedStone)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = IvoryPrimary)
    }
}

private fun parseSafeHexColor(hex: String): Color {
    val clean = hex.trim().removePrefix("#")
    return try {
        val parsed = clean.toLong(16)
        when (clean.length) {
            6 -> Color((0xFF000000L or parsed).toInt())
            8 -> Color(parsed.toInt())
            else -> AmberGold
        }
    } catch (_: Exception) {
        AmberGold
    }
}

internal fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("LumenReel", text))
}
