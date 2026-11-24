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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ProductionFolder
import com.example.data.model.ProductionProject
import com.example.data.model.WorkspaceTab
import com.example.data.util.ProjectTemplateCatalog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectFolderManagerDialog(
    project: ProductionProject,
    onDismiss: () -> Unit,
    onCreateFolder: (name: String, category: String, description: String) -> Unit,
    onUpdateFolderNotes: (folderId: String, notes: String) -> Unit,
    onDeleteFolder: (folderId: String) -> Unit,
    onAssignSequenceToFolder: (folderId: String, sequenceId: String) -> Unit,
    onRemoveSequenceFromFolder: (folderId: String, sequenceId: String) -> Unit,
    onOpenWorkspaceTab: (folder: ProductionFolder, sequenceId: String?, tabOverride: WorkspaceTab?) -> Unit,
    onOpenScriptEditor: () -> Unit,
    onOpenFolderGallery: (ProductionFolder) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val folders = remember(project.productionFolders, project.templateId) {
        project.productionFolders.ifEmpty { ProjectTemplateCatalog.defaultFoldersFor(project.templateId) }
    }
    var selectedFilter by remember { mutableStateOf("all") }
    var showCreateForm by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var newFolderCategory by remember { mutableStateOf("script") }
    var newFolderDesc by remember { mutableStateOf("") }
    var editingNotesFolderId by remember { mutableStateOf<String?>(null) }
    var notesDraft by remember { mutableStateOf("") }

    val filteredFolders = remember(folders, selectedFilter) {
        when (selectedFilter) {
            "script" -> folders.filter { it.category.equals("script", ignoreCase = true) }
            "visual" -> folders.filter {
                it.category.equals("visual", ignoreCase = true) ||
                    it.category.equals("field", ignoreCase = true)
            }
            "tech" -> folders.filter {
                it.category.lowercase() !in listOf("script", "visual", "field")
            }
            else -> folders
        }
    }

    val totalScenes = remember(project.sequences) {
        project.sequences.sumOf { it.screenplay.size }
    }
    val totalFrames = remember(project.sequences) {
        project.sequences.sumOf { it.storyboardFrames.size }
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
            .testTag("project_folder_manager_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gestor de Carpetas · ${project.title}",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Organiza guiones, secuencias y storyboards por área de producción",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pm_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cerrar gestor de carpetas",
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
                        // Summary + Quick Actions Card
                        Surface(
                            color = colors.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "${folders.size} carpetas · ${project.sequences.size} secuencias · $totalScenes escenas de guion · $totalFrames fotogramas storyboard",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showCreateForm = !showCreateForm },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.primary,
                                            contentColor = colors.onPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("pm_toggle_create_folder_button")
                                    ) {
                                        Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(6.dp))
                                        Text(
                                            text = if (showCreateForm) "Ocultar formulario" else "Nueva carpeta",
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = onOpenScriptEditor,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("pm_open_script_editor_button")
                                    ) {
                                        Icon(Icons.Rounded.EditNote, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(6.dp))
                                        Text(
                                            text = "Escribir guion IA",
                                            color = colors.secondary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Create New Folder Inline Form
                        if (showCreateForm) {
                            Surface(
                                color = colors.surface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.5.dp, colors.primary.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Crear nueva carpeta cinematográfica",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.primary
                                    )
                                    OutlinedTextField(
                                        value = newFolderName,
                                        onValueChange = { newFolderName = it },
                                        label = { Text("Nombre de la carpeta") },
                                        placeholder = { Text("Ej. Guiones_Acto_III o Storyboards_Climax") },
                                        singleLine = true,
                                        colors = folderFieldColors(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("pm_new_folder_name_input")
                                    )
                                    Text(
                                        text = "Departamento / Tipo de contenido",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.onSurfaceVariant
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            "script" to "Guion / Scripts",
                                            "visual" to "Storyboard / Arte",
                                            "bible" to "Biblia y Dirección",
                                            "schedule" to "Plan de Rodaje",
                                            "sound" to "Sonido Atmos",
                                            "vfx" to "VFX / SFX"
                                        ).forEach { (catId, catLabel) ->
                                            FilterChip(
                                                selected = newFolderCategory == catId,
                                                onClick = { newFolderCategory = catId },
                                                label = { Text(catLabel, style = MaterialTheme.typography.labelSmall) },
                                                colors = folderChipColors(),
                                                modifier = Modifier.testTag("pm_folder_category_$catId")
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = newFolderDesc,
                                        onValueChange = { newFolderDesc = it },
                                        label = { Text("Descripción y alcance") },
                                        placeholder = { Text("Ej. Versiones revisadas de guion y anotaciones de cámara.") },
                                        minLines = 2,
                                        maxLines = 3,
                                        colors = folderFieldColors(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("pm_new_folder_desc_input")
                                    )
                                    Button(
                                        onClick = {
                                            if (newFolderName.isNotBlank()) {
                                                onCreateFolder(newFolderName, newFolderCategory, newFolderDesc)
                                                newFolderName = ""
                                                newFolderDesc = ""
                                                showCreateForm = false
                                            }
                                        },
                                        enabled = newFolderName.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.primary,
                                            contentColor = colors.onPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("pm_confirm_create_folder_button")
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(6.dp))
                                        Text("Crear carpeta en el proyecto", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Filter Chips
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "all" to "Todas (${folders.size})",
                                "script" to "Guiones",
                                "visual" to "Storyboards",
                                "tech" to "Técnicas"
                            ).forEach { (id, label) ->
                                FilterChip(
                                    selected = selectedFilter == id,
                                    onClick = { selectedFilter = id },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    colors = folderChipColors(),
                                    modifier = Modifier.testTag("pm_filter_$id")
                                )
                            }
                        }

                        if (filteredFolders.isEmpty()) {
                            Surface(
                                color = colors.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No hay carpetas en esta categoría. Pulsa «Nueva carpeta» para crear una.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            filteredFolders.forEach { folder ->
                                val summary = remember(folder, project.sequences) {
                                    ProjectTemplateCatalog.resolveFolderAssetsSummary(folder, project)
                                }
                                val unassignedSequences = remember(folder.linkedSequenceIds, project.sequences) {
                                    project.sequences.filter { it.id !in folder.linkedSequenceIds }
                                }
                                val isEditingNotes = editingNotesFolderId == folder.id

                                Surface(
                                    color = colors.surfaceVariant,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                                        .testTag("pm_folder_card_${folder.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Surface(
                                                    color = colors.primaryContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = ProjectTemplateCatalog.categoryDisplayLabel(folder.category),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = colors.onPrimaryContainer,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Folder,
                                                        contentDescription = null,
                                                        tint = colors.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = folder.name,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = colors.onSurface,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { onDeleteFolder(folder.id) },
                                                enabled = folders.size > 1,
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("pm_delete_folder_${folder.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = "Eliminar carpeta ${folder.name}",
                                                    tint = if (folders.size > 1) colors.onSurfaceVariant else colors.outline
                                                )
                                            }
                                        }

                                        Text(
                                            text = folder.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant
                                        )

                                        Surface(
                                            color = colors.surface,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                                        ) {
                                            Text(
                                                text = "Contenido: ${summary.linkedSequences.size} secuencia(s) · ${summary.sceneCount} escenas de guion · ${summary.storyboardFramesCount} tarjetas de storyboard",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.onSurface,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }

                                        // Explicit sequence links inside this folder
                                        if (project.sequences.isNotEmpty()) {
                                            val linkedExplicitly = project.sequences.filter { it.id in folder.linkedSequenceIds }
                                            if (linkedExplicitly.isNotEmpty()) {
                                                Text(
                                                    text = "Secuencias organizadas en esta carpeta:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.secondary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    linkedExplicitly.forEach { seq ->
                                                        Surface(
                                                            color = colors.secondaryContainer,
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Text(
                                                                    text = "${seq.title} (${seq.screenplay.size} esc · ${seq.storyboardFrames.size} sb)",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = colors.onSecondaryContainer,
                                                                    modifier = Modifier.clickable {
                                                                        onOpenWorkspaceTab(folder, seq.id, null)
                                                                    }
                                                                )
                                                                Icon(
                                                                    imageVector = Icons.Rounded.Close,
                                                                    contentDescription = "Quitar ${seq.title} de la carpeta",
                                                                    tint = colors.onSecondaryContainer,
                                                                    modifier = Modifier
                                                                        .size(16.dp)
                                                                        .clickable { onRemoveSequenceFromFolder(folder.id, seq.id) }
                                                                        .testTag("pm_unlink_seq_${folder.id}_${seq.id}")
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            if (unassignedSequences.isNotEmpty()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Link,
                                                        contentDescription = null,
                                                        tint = colors.onSurfaceVariant,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "Vincular:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = colors.onSurfaceVariant
                                                    )
                                                    unassignedSequences.forEach { seq ->
                                                        OutlinedButton(
                                                            onClick = { onAssignSequenceToFolder(folder.id, seq.id) },
                                                            shape = RoundedCornerShape(8.dp),
                                                            modifier = Modifier.testTag("pm_assign_seq_${folder.id}_${seq.id}")
                                                        ) {
                                                            Text(
                                                                text = "+ ${seq.title}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Folder Notes
                                        if (folder.notes.isNotBlank() && !isEditingNotes) {
                                            Text(
                                                text = "Notas: ${folder.notes}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.onSurfaceVariant
                                            )
                                        }
                                        if (isEditingNotes) {
                                            OutlinedTextField(
                                                value = notesDraft,
                                                onValueChange = { notesDraft = it },
                                                label = { Text("Notas de carpeta") },
                                                minLines = 2,
                                                maxLines = 4,
                                                colors = folderFieldColors(),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("pm_notes_input_${folder.id}")
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        onUpdateFolderNotes(folder.id, notesDraft)
                                                        editingNotesFolderId = null
                                                    },
                                                    modifier = Modifier.testTag("pm_save_notes_${folder.id}")
                                                ) {
                                                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.size(4.dp))
                                                    Text("Guardar notas")
                                                }
                                                TextButton(onClick = { editingNotesFolderId = null }) {
                                                    Text("Cancelar")
                                                }
                                            }
                                        }

                                        // Direct Workspace Jump Controls
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val targetSeqId = summary.linkedSequences.firstOrNull()?.id
                                            OutlinedButton(
                                                onClick = { onOpenWorkspaceTab(folder, targetSeqId, WorkspaceTab.SCREENPLAY) },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("pm_open_script_${folder.id}")
                                            ) {
                                                Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.size(4.dp))
                                                Text("Guion", style = MaterialTheme.typography.labelSmall)
                                            }
                                            OutlinedButton(
                                                onClick = { onOpenFolderGallery(folder) },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .heightIn(min = 48.dp)
                                                    .testTag("pm_open_gallery_${folder.id}")
                                            ) {
                                                Icon(Icons.Rounded.GridView, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.size(4.dp))
                                                Text("Galería Grid", style = MaterialTheme.typography.labelSmall)
                                            }
                                            OutlinedButton(
                                                onClick = { onOpenWorkspaceTab(folder, targetSeqId, WorkspaceTab.STORYBOARD) },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .heightIn(min = 48.dp)
                                                    .testTag("pm_open_storyboard_${folder.id}")
                                            ) {
                                                Icon(Icons.Rounded.ViewCarousel, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.size(4.dp))
                                                Text("Storyboard", style = MaterialTheme.typography.labelSmall)
                                            }
                                            OutlinedButton(
                                                onClick = { onOpenWorkspaceTab(folder, targetSeqId, null) },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .heightIn(min = 48.dp)
                                                    .testTag("pm_open_area_${folder.id}")
                                            ) {
                                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.size(4.dp))
                                                Text("Abrir área", style = MaterialTheme.typography.labelSmall)
                                            }
                                            TextButton(
                                                onClick = {
                                                    notesDraft = folder.notes
                                                    editingNotesFolderId = if (isEditingNotes) null else folder.id
                                                },
                                                modifier = Modifier
                                                    .heightIn(min = 48.dp)
                                                    .testTag("pm_edit_notes_${folder.id}")
                                            ) {
                                                Text(
                                                    text = if (isEditingNotes) "Cerrar notas" else "Editar notas",
                                                    style = MaterialTheme.typography.labelSmall
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
private fun folderFieldColors(): androidx.compose.material3.TextFieldColors {
    val colors = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.outline,
        focusedTextColor = colors.onSurface,
        unfocusedTextColor = colors.onSurface,
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = colors.onSurfaceVariant
    )
}

@Composable
private fun folderChipColors(): androidx.compose.material3.SelectableChipColors {
    val colors = MaterialTheme.colorScheme
    return FilterChipDefaults.filterChipColors(
        selectedContainerColor = colors.primaryContainer,
        selectedLabelColor = colors.onPrimaryContainer,
        containerColor = colors.surface,
        labelColor = colors.onSurfaceVariant
    )
}
