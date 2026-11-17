package com.example.ui.screens

import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.ProductionSequence
import com.example.data.util.LearningEngine
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.MutedStone
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioDeepPanel
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TealAtmos
import com.example.ui.theme.TealContainer
import com.example.ui.theme.WarmSilver

@Composable
fun ScreenplayTabView(
    sequence: ProductionSequence,
    isExporting: Boolean,
    onExportPdf: () -> Unit,
    onExportMarkdown: () -> Unit,
    onExportFountain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canExportPdf = LearningEngine.canExportPdfForSequence(sequence)

    Column(modifier = modifier.fillMaxSize()) {
        // Export Action Bar
        Surface(
            color = StudioCard,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportPdf,
                        enabled = canExportPdf && !isExporting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor = ObsidianBg,
                            disabledContainerColor = StudioSurfaceElevated,
                            disabledContentColor = MutedStone
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_sequence_pdf_button")
                    ) {
                        Icon(
                            imageVector = if (canExportPdf) Icons.Rounded.PictureAsPdf else Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = if (canExportPdf) "Bitácora PDF" else "PDF Bloqueado (Borrador)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportMarkdown,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("export_markdown_button")
                    ) {
                        Text("Compartir .MD", color = IvoryPrimary, style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onExportFountain,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("export_fountain_button")
                    ) {
                        Text("Compartir .FOUNTAIN", color = AmberGold, style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (!canExportPdf) {
                    Text(
                        text = "Aprueba la secuencia como Versión Final en la barra superior para desbloquear la Bitácora PDF.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedStone
                    )
                }
            }
        }

        // Screenplay Sheets
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (sequence.screenplay.isEmpty()) {
                item {
                    Text(
                        text = "Esta secuencia todavía no contiene escenas de guion.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedStone
                    )
                }
            } else {
                items(sequence.screenplay, key = { "${it.sceneNumber}_${it.slugline}" }) { scene ->
                    Surface(
                        color = StudioDeepPanel,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "ESCENA ${scene.sceneNumber} · ${scene.slugline.uppercase()}",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                                color = AmberGold,
                                fontWeight = FontWeight.Bold
                            )

                            scene.beats.forEach { beat ->
                                when (beat.type.lowercase()) {
                                    "dialogue" -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = (beat.character ?: "PERSONAJE").uppercase(),
                                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                                                color = IvoryPrimary,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                            if (!beat.parenthetical.isNullOrBlank()) {
                                                Text(
                                                    text = "(${beat.parenthetical})",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontStyle = FontStyle.Italic
                                                    ),
                                                    color = MutedStone,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                            Text(
                                                text = beat.text,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                                color = WarmSilver,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                    "camera" -> {
                                        Surface(
                                            color = AmberContainer,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "[CÁMARA] ${beat.text}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                color = AmberGold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                    "sound" -> {
                                        Surface(
                                            color = TealContainer,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "[SONIDO] ${beat.text}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                color = TealAtmos,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        Text(
                                            text = beat.text,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                            color = IvoryPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
