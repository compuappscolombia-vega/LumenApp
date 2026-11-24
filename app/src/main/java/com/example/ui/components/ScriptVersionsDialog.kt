package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.ScriptVersion
import com.example.data.model.SequenceVersionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScriptVersionsDialog(
    project: ProductionProject,
    activeSequence: ProductionSequence,
    versions: List<ScriptVersion>,
    activeVersionId: String?,
    onDismiss: () -> Unit,
    onSaveVersion: (versionLabel: String, changeNote: String) -> Unit,
    onSwitchVersion: (ScriptVersion) -> Unit,
    onUpdateMetadata: (versionId: String, versionLabel: String, changeNote: String) -> Unit,
    onDeleteVersion: (versionId: String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val sequenceVersionsCount = remember(versions, activeSequence.id) {
        versions.count { it.sequenceId == activeSequence.id }
    }
    var versionLabel by remember(activeSequence.id, sequenceVersionsCount) {
        mutableStateOf("v${sequenceVersionsCount + 1}.0 · Borrador")
    }
    var changeNote by remember { mutableStateOf("") }
    var scopeFilter by remember { mutableStateOf("sequence") }
    var editingVersionId by remember { mutableStateOf<String?>(null) }
    var editLabelDraft by remember { mutableStateOf("") }
    var editNoteDraft by remember { mutableStateOf("") }

    val filteredVersions = remember(versions, activeSequence.id, scopeFilter) {
        if (scopeFilter == "sequence") {
            versions.filter { it.sequenceId == activeSequence.id }
        } else {
            versions
        }
    }
    val dateFormatter = remember {
        SimpleDateFormat("dd MMM HH:mm", Locale("es"))
    }
    val currentWordCount = remember(activeSequence.screenplay) {
        ScriptVersion.computeWordCount(activeSequence.screenplay)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.primary,
        textContentColor = colors.onSurface,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth(0.94f)
            .testTag("script_versions_dialog"),
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
                    Icon(Icons.Rounded.History, contentDescription = null, tint = colors.primary)
                    Column {
                        Text(
                            text = "Versiones de Guion · ${project.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Guarda puntos de control en Firestore y alterna entre versiones",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("script_versions_close_button")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar historial de versiones", tint = colors.onSurfaceVariant)
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
                // Save snapshot form card
                Surface(
                    color = colors.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Guardar versión actual de «${activeSequence.title}»",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Estado actual: ${activeSequence.screenplay.size} escenas · $currentWordCount palabras · ${if (activeSequence.versionStatus == SequenceVersionStatus.FINAL) "Versión Final" else "Borrador editable"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = versionLabel,
                            onValueChange = { versionLabel = it },
                            label = { Text("Etiqueta de versión") },
                            placeholder = { Text("Ej. v2.0 · Revisión Diálogo") },
                            singleLine = true,
                            colors = versionFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("script_version_label_input")
                        )
                        OutlinedTextField(
                            value = changeNote,
                            onValueChange = { changeNote = it },
                            label = { Text("Nota de cambios (opcional)") },
                            placeholder = { Text("Ej. Nuevo cierre en escena del muelle y ajuste de ritmo") },
                            minLines = 2,
                            maxLines = 3,
                            colors = versionFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("script_version_note_input")
                        )
                        Button(
                            onClick = {
                                val safeLabel = versionLabel.trim().ifBlank { "v${sequenceVersionsCount + 1}.0" }
                                onSaveVersion(safeLabel, changeNote.trim())
                                changeNote = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("script_save_version_button")
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Guardar instantánea en Firestore", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Scope filter chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = scopeFilter == "sequence",
                        onClick = { scopeFilter = "sequence" },
                        label = {
                            Text(
                                "Esta secuencia ($sequenceVersionsCount)",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primaryContainer,
                            selectedLabelColor = colors.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("script_versions_filter_sequence")
                    )
                    FilterChip(
                        selected = scopeFilter == "all",
                        onClick = { scopeFilter = "all" },
                        label = {
                            Text(
                                "Todo el proyecto (${versions.size})",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primaryContainer,
                            selectedLabelColor = colors.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("script_versions_filter_all")
                    )
                }

                if (filteredVersions.isEmpty()) {
                    Surface(
                        color = colors.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("script_versions_empty_state")
                    ) {
                        Text(
                            text = "No hay versiones guardadas todavía. Guarda la primera instantánea arriba para comparar o restaurar revisiones en cualquier momento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    filteredVersions.forEach { ver ->
                        val isActive = ver.id == activeVersionId
                        val isEditing = editingVersionId == ver.id
                        Surface(
                            color = if (isActive) colors.primaryContainer.copy(alpha = 0.45f) else colors.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isActive) 1.5.dp else 1.dp,
                                    color = if (isActive) colors.primary else colors.outline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("script_version_card_${ver.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            color = colors.primary,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "#${ver.versionNumber}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.onPrimary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = ver.versionLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isActive) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = "Versión cargada",
                                                tint = colors.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (isEditing) {
                                                    editingVersionId = null
                                                } else {
                                                    editingVersionId = ver.id
                                                    editLabelDraft = ver.versionLabel
                                                    editNoteDraft = ver.changeNote
                                                }
                                            },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("script_edit_version_${ver.id}")
                                        ) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "Renombrar versión", tint = colors.secondary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { onDeleteVersion(ver.id) },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("script_delete_version_${ver.id}")
                                        ) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar versión", tint = colors.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                Text(
                                    text = "${ver.sequenceTitle} · ${ver.sceneCount} escenas · ${ver.wordCount} palabras · ${dateFormatter.format(Date(ver.createdAtMillis.coerceAtLeast(1L)))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )

                                if (ver.changeNote.isNotBlank() && !isEditing) {
                                    Text(
                                        text = ver.changeNote,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurface
                                    )
                                }

                                val firstSlug = ver.scenes.firstOrNull()?.slugline.orEmpty()
                                if (firstSlug.isNotBlank() && !isEditing) {
                                    Text(
                                        text = "Inicio: $firstSlug",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.secondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (isEditing) {
                                    OutlinedTextField(
                                        value = editLabelDraft,
                                        onValueChange = { editLabelDraft = it },
                                        label = { Text("Nueva etiqueta") },
                                        singleLine = true,
                                        colors = versionFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = editNoteDraft,
                                        onValueChange = { editNoteDraft = it },
                                        label = { Text("Nota") },
                                        minLines = 1,
                                        maxLines = 2,
                                        colors = versionFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                if (editLabelDraft.isNotBlank()) {
                                                    onUpdateMetadata(ver.id, editLabelDraft, editNoteDraft)
                                                    editingVersionId = null
                                                }
                                            },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Actualizar")
                                        }
                                        TextButton(onClick = { editingVersionId = null }) {
                                            Text("Cancelar")
                                        }
                                    }
                                } else {
                                    val targetSequence = project.sequences.firstOrNull { it.id == ver.sequenceId }
                                    val willClone = targetSequence == null || targetSequence.versionStatus == SequenceVersionStatus.FINAL
                                    OutlinedButton(
                                        onClick = {
                                            onSwitchVersion(ver)
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("script_switch_version_${ver.id}")
                                    ) {
                                        Icon(Icons.Rounded.Restore, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(8.dp))
                                        Text(
                                            text = if (willClone) {
                                                "Cargar como nuevo borrador"
                                            } else {
                                                "Cambiar a esta versión"
                                            },
                                            color = colors.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Listo", color = colors.primary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun versionFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
)
