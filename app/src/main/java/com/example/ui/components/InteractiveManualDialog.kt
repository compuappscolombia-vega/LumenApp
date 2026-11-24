package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.util.ManualQuickAction
import com.example.data.util.StudioManualCatalog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveManualDialog(
    onDismiss: () -> Unit,
    onQuickAction: (ManualQuickAction) -> Unit,
    onSaveManualPdf: () -> Unit,
    onShareManualPdf: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    var expandedSectionIds by remember { mutableStateOf(setOf(StudioManualCatalog.SECTIONS.first().id)) }
    var selectedAspectRatio by remember { mutableStateOf(StudioManualCatalog.ASPECT_GUIDE_SPECS.first().ratio) }
    var checkedSteps by remember { mutableStateOf(setOf<Int>()) }

    val filteredSections = remember(searchQuery, selectedCategory) {
        StudioManualCatalog.filterSections(searchQuery, selectedCategory)
    }
    val activeAspectSpec = remember(selectedAspectRatio) {
        StudioManualCatalog.ASPECT_GUIDE_SPECS.firstOrNull { it.ratio == selectedAspectRatio }
            ?: StudioManualCatalog.ASPECT_GUIDE_SPECS.first()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.primary,
        textContentColor = colors.onSurface,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth(0.95f)
            .testTag("interactive_manual_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.AutoStories, contentDescription = null, tint = colors.primary)
                    Column {
                        Text(
                            text = "Manual Interactivo y Guía PDF",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primary
                        )
                        Text(
                            text = "Instrucciones paso a paso, guía de encuadre y exportación PDF",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("manual_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cerrar manual interactivo",
                        tint = colors.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "Buscar en el manual (Gemini, carpetas, PDF, errores…)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.onSurfaceVariant) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline,
                                focusedTextColor = colors.onSurface,
                                unfocusedTextColor = colors.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_search_input")
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StudioManualCatalog.TOPIC_FILTERS.forEach { (id, label) ->
                                FilterChip(
                                    selected = selectedCategory == id,
                                    onClick = { selectedCategory = id },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primaryContainer,
                                        selectedLabelColor = colors.onPrimaryContainer,
                                        containerColor = colors.surfaceVariant,
                                        labelColor = colors.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("manual_topic_$id")
                                )
                            }
                        }

                        // Interactive Aspect Ratio Framing Visual Guide
                        Surface(
                            color = colors.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                                .testTag("manual_aspect_guide")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Guía Visual de Encuadre y Relación de Aspecto",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    StudioManualCatalog.ASPECT_GUIDE_SPECS.forEach { spec ->
                                        FilterChip(
                                            selected = spec.ratio == selectedAspectRatio,
                                            onClick = { selectedAspectRatio = spec.ratio },
                                            label = { Text(spec.ratio, style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = colors.primaryContainer,
                                                selectedLabelColor = colors.onPrimaryContainer,
                                                containerColor = colors.surface,
                                                labelColor = colors.onSurfaceVariant
                                            ),
                                            modifier = Modifier.testTag("manual_aspect_chip_${spec.ratio}")
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .background(colors.background, RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .aspectRatio(activeAspectSpec.numericRatio)
                                            .background(colors.primaryContainer.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                                            .border(2.dp, colors.primary, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = activeAspectSpec.ratio,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = colors.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "${activeAspectSpec.title}: ${activeAspectSpec.usageNote}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurface,
                                    modifier = Modifier.testTag("manual_aspect_note")
                                )
                            }
                        }

                        // Step-by-Step Interactive Guide Sections
                        if (filteredSections.isEmpty()) {
                            Surface(
                                color = colors.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No se encontraron capítulos del manual para «$searchQuery».",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            filteredSections.forEach { section ->
                                val isExpanded = section.id in expandedSectionIds ||
                                    searchQuery.isNotBlank() ||
                                    selectedCategory != "all"

                                Surface(
                                    color = colors.surfaceVariant,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                                        .clickable {
                                            expandedSectionIds = if (section.id in expandedSectionIds) {
                                                expandedSectionIds - section.id
                                            } else {
                                                expandedSectionIds + section.id
                                            }
                                        }
                                        .testTag("manual_section_${section.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Surface(
                                                    color = colors.secondaryContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = section.badge,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = colors.onSecondaryContainer,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = section.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = colors.onSurface,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                contentDescription = if (isExpanded) "Contraer sección" else "Expandir sección",
                                                tint = colors.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = section.summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant
                                        )

                                        if (isExpanded) {
                                            HorizontalDivider(color = colors.outline.copy(alpha = 0.5f))
                                            section.steps.forEachIndexed { idx, step ->
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Surface(
                                                        color = colors.primaryContainer,
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "${idx + 1}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = colors.onPrimaryContainer,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = step,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = colors.onSurface,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            if (section.proTip.isNotBlank()) {
                                                Surface(
                                                    color = colors.surface,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Lightbulb,
                                                            contentDescription = null,
                                                            tint = colors.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = section.proTip,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = colors.onSurface
                                                        )
                                                    }
                                                }
                                            }

                                            if (section.quickAction != ManualQuickAction.NONE && section.actionLabel.isNotBlank()) {
                                                OutlinedButton(
                                                    onClick = { onQuickAction(section.quickAction) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                        .testTag("manual_jump_${section.id}")
                                                ) {
                                                    Text(section.actionLabel, style = MaterialTheme.typography.labelMedium)
                                                    Spacer(Modifier.size(6.dp))
                                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Interactive Production Readiness Checklist
                        Surface(
                            color = colors.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Checklist Interactivo de Producción (${checkedSteps.size}/${StudioManualCatalog.READINESS_CHECKLIST_STEPS.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                StudioManualCatalog.READINESS_CHECKLIST_STEPS.forEachIndexed { idx, label ->
                                    val checked = idx in checkedSteps
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                checkedSteps = if (checked) checkedSteps - idx else checkedSteps + idx
                                            }
                                            .testTag("manual_checklist_$idx"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = { isChecked ->
                                                checkedSteps = if (isChecked) checkedSteps + idx else checkedSteps - idx
                                            }
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (checked) colors.secondary else colors.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
        confirmButton = {
            Button(
                onClick = onSaveManualPdf,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("manual_save_pdf_button")
            ) {
                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text("Guardar Manual PDF", fontWeight = FontWeight.Bold, maxLines = 1)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onShareManualPdf,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("manual_share_pdf_button")
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text("Compartir PDF", fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    )
}
