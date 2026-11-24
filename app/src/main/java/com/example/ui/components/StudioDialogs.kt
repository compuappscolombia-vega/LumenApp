package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Sync
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LearningCategory
import com.example.data.model.LearningLesson
import com.example.data.model.ProductionFolder
import com.example.data.model.SourceMaterialType
import com.example.data.model.ThinkingDepth
import com.example.data.model.UserStudioConfig
import com.example.data.repository.PersistedAccountSession
import com.example.data.util.LearningEngine
import com.example.data.util.ProjectTemplateCatalog
import com.example.data.util.StudioLogic
import com.example.ui.screens.copyToClipboard
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        genreStyle: String,
        defaultSourceType: SourceMaterialType,
        continuityBible: String,
        templateId: String,
        productionFolders: List<ProductionFolder>
    ) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val templates = remember { ProjectTemplateCatalog.ALL_TEMPLATES }
    var selectedTemplateId by remember { mutableStateOf(templates.first().id) }
    val selectedTemplate = remember(selectedTemplateId) {
        ProjectTemplateCatalog.findById(selectedTemplateId)
    }

    var title by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(selectedTemplate.defaultStyle) }
    var sourceType by remember { mutableStateOf(selectedTemplate.defaultSourceType) }
    var bible by remember { mutableStateOf(selectedTemplate.continuityStarter) }
    var userEditedBible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.primary,
        textContentColor = colors.onSurface,
        title = { Text("Nueva Producción Cinematográfica") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesomeMotion,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Biblioteca de plantillas de proyecto",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.forEach { template ->
                        val selected = template.id == selectedTemplateId
                        Surface(
                            color = if (selected) colors.primaryContainer else colors.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(196.dp)
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) colors.primary else colors.outline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedTemplateId = template.id
                                    selectedStyle = template.defaultStyle
                                    sourceType = template.defaultSourceType
                                    if (!userEditedBible || bible.isBlank()) {
                                        bible = template.continuityStarter
                                    }
                                }
                                .testTag("template_card_${template.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = template.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) colors.onPrimaryContainer else colors.secondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Plantilla seleccionada",
                                            tint = colors.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${template.folders.size} carpetas · ${template.aspectRatio}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
                                )
                                Text(
                                    text = template.tagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) colors.onPrimaryContainer.copy(alpha = 0.85f) else colors.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = colors.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.secondary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .testTag("new_project_folders_preview")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Carpetas de producción automáticas (${selectedTemplate.folders.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${selectedTemplate.cameraRecommendation} · ${selectedTemplate.description}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                        selectedTemplate.folders.forEach { folder ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = folder.description,
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

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la producción") },
                    placeholder = { Text("Ej. Crónicas de Marea Negra", color = colors.onSurfaceVariant) },
                    singleLine = true,
                    colors = dialogFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_title_input")
                )

                Text("Estilo cinematográfico base", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioLogic.DIRECTOR_STYLES.forEach { preset ->
                        FilterChip(
                            selected = selectedStyle == preset.label,
                            onClick = { selectedStyle = preset.label },
                            label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                            colors = dialogChipColors()
                        )
                    }
                }

                Text("Material fuente principal", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SourceMaterialType.entries.forEach { st ->
                        FilterChip(
                            selected = sourceType == st,
                            onClick = { sourceType = st },
                            label = {
                                Text(
                                    when (st) {
                                        SourceMaterialType.BOOK -> "Libro"
                                        SourceMaterialType.AUDIOBOOK -> "Audio"
                                        SourceMaterialType.STORY -> "Historia"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = dialogChipColors()
                        )
                    }
                }

                OutlinedTextField(
                    value = bible,
                    onValueChange = {
                        bible = it
                        userEditedBible = true
                    },
                    label = { Text("Reglas iniciales de continuidad (opcional)") },
                    minLines = 3,
                    maxLines = 5,
                    colors = dialogFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_bible_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().isNotEmpty()) {
                        onCreate(
                            title.trim(),
                            selectedStyle,
                            sourceType,
                            bible.trim(),
                            selectedTemplate.id,
                            selectedTemplate.folders.map { it.copy() }
                        )
                        onDismiss()
                    }
                },
                enabled = title.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                modifier = Modifier.testTag("confirm_create_project_button")
            ) {
                Text("Crear producción", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = colors.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun ProductionFolderDetailDialog(
    folder: ProductionFolder,
    onDismiss: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onOpenWorkspaceTab: () -> Unit,
    onOpenGallery: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    var notesDraft by remember(folder.id, folder.notes) { mutableStateOf(folder.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.primary,
        textContentColor = colors.onSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Folder, contentDescription = null, tint = colors.primary)
                Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(folder.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                OutlinedTextField(
                    value = notesDraft,
                    onValueChange = { notesDraft = it },
                    label = { Text("Notas y entregables de la carpeta") },
                    placeholder = { Text("Añade pendientes, archivos de referencia o checklist…", color = colors.onSurfaceVariant) },
                    minLines = 3,
                    maxLines = 6,
                    colors = dialogFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_notes_input")
                )
                OutlinedButton(
                    onClick = {
                        onOpenGallery()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("open_folder_gallery_button")
                ) {
                    Icon(Icons.Rounded.GridView, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Abrir Galería Visual (Grid)", color = colors.primary)
                }
                OutlinedButton(
                    onClick = {
                        onOpenWorkspaceTab()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("open_folder_tab_button")
                ) {
                    Text("Abrir sección vinculada", color = colors.secondary)
                    Spacer(Modifier.size(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(16.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveNotes(notesDraft)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                modifier = Modifier.testTag("save_folder_notes_button")
            ) {
                Text("Guardar notas", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), color = colors.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun AddProductionFolderDialog(
    onDismiss: () -> Unit,
    onAddFolder: (name: String, category: String, description: String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var folderName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("script") }
    var description by remember { mutableStateOf("") }
    val categories = remember {
        listOf(
            "script" to "Guion",
            "bible" to "Biblia",
            "schedule" to "Rodaje",
            "visual" to "Arte / Storyboard",
            "sound" to "Sonido",
            "vfx" to "VFX"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.primary,
        textContentColor = colors.onSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, tint = colors.primary)
                Text("Nueva carpeta de producción")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Nombre de la carpeta") },
                    placeholder = { Text("Ej. Casting_y_Vestuario", color = colors.onSurfaceVariant) },
                    singleLine = true,
                    colors = dialogFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_folder_name_input")
                )
                Text("Área vinculada", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedCategory == key,
                            onClick = { selectedCategory = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = dialogChipColors()
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)") },
                    singleLine = true,
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.trim().isNotEmpty()) {
                        onAddFolder(folderName.trim(), selectedCategory, description.trim())
                        onDismiss()
                    }
                },
                enabled = folderName.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                modifier = Modifier.testTag("add_custom_folder_button")
            ) {
                Text("Crear carpeta", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = colors.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun LearningMemoryDialog(
    lessons: List<LearningLesson>,
    activeProjectTitle: String,
    onDismiss: () -> Unit,
    onAddLesson: (LearningCategory, String, Int, String) -> Unit,
    onDeleteLesson: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var selectedCategory by remember { mutableStateOf(LearningCategory.CAMERA) }
    var insightText by remember { mutableStateOf("") }
    var rating by remember { mutableIntStateOf(5) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.primary,
        textContentColor = colors.onSurface,
        title = { Text("Autoaprendizaje del Estudio (${lessons.size})") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Las reglas guardadas se inyectan automáticamente en Gemini y en el Agente Tripartito para futuras producciones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LearningCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    LearningEngine.CATEGORY_LABELS[cat] ?: cat.wireValue,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = dialogChipColors()
                        )
                    }
                }

                OutlinedTextField(
                    value = insightText,
                    onValueChange = { insightText = it },
                    label = { Text("Nueva regla o aprendizaje técnico") },
                    minLines = 2,
                    maxLines = 4,
                    colors = dialogFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("learning_insight_input")
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Prioridad:", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = "$star estrellas",
                                tint = if (star <= rating) colors.primary else colors.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Text(validationError!!, style = MaterialTheme.typography.labelSmall, color = colors.error)
                }

                Button(
                    onClick = {
                        val err = LearningEngine.validateLessonInput(insightText, rating)
                        if (err != null) {
                            validationError = err
                        } else {
                            validationError = null
                            onAddLesson(
                                selectedCategory,
                                insightText.trim(),
                                rating,
                                activeProjectTitle.ifBlank { "Estudio LumenReel" }
                            )
                            insightText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_learning_lesson_button")
                ) {
                    Text("Guardar regla en memoria", fontWeight = FontWeight.Bold)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(lessons, key = { it.id }) { lesson ->
                        Surface(
                            color = colors.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "${LearningEngine.CATEGORY_LABELS[lesson.category] ?: lesson.category.wireValue} · ${lesson.rating}★",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.primary
                                    )
                                    Text(lesson.insight, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
                                    Text(lesson.sourceProjectTitle, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = { onDeleteLesson(lesson.id) },
                                    modifier = Modifier.testTag("delete_lesson_${lesson.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Eliminar aprendizaje",
                                        tint = colors.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), color = colors.onSurface)
            }
        }
    )
}

@Composable
fun AgentBlueprintDialog(
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    val blueprintJson = remember {
        """{
  "agentName": "LumenReel Autonomous Tripartite Film Agent",
  "runtime": "Firebase AI Logic + Gemini 3.8 Flash",
  "roles": ["Director de Cine", "Guionista Hollywood", "Experto VFX y SFX Prácticos"],
  "imageGenerator": "gemini-nano-banana-2.1",
  "tools": [
    "actualizar_biblia_continuidad",
    "ajustar_direccion_y_camara",
    "disenar_paquete_vfx_sfx",
    "refinar_guion_escena",
    "agregar_plano_storyboard"
  ],
  "security": "Firebase App Check + Google Sign-In + Firestore Zero-Trust Rules"
}"""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.primary,
        textContentColor = colors.onSurface,
        title = { Text("Plantilla Agent Studio (JSON)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Especificación declarativa del Agente Tripartito y sus 5 herramientas de modificación de expediente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Surface(
                    color = colors.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = blueprintJson,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                if (copied) {
                    Text("Plantilla JSON copiada al portapapeles.", style = MaterialTheme.typography.labelSmall, color = colors.secondary)
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = {
                    copyToClipboard(context, blueprintJson)
                    copied = true
                },
                modifier = Modifier.testTag("copy_blueprint_json_button")
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("Copiar JSON", color = colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), color = colors.onSurface)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountDialog(
    user: FirebaseUser?,
    preferredStyle: String = "",
    onSavePreferredStyle: (String) -> Unit = {},
    studioConfig: UserStudioConfig = UserStudioConfig(),
    isDarkMode: Boolean = studioConfig.isDarkMode,
    onToggleDarkMode: () -> Unit = {},
    onSaveStudioConfig: (UserStudioConfig) -> Unit = {},
    persistedSession: PersistedAccountSession = PersistedAccountSession(),
    onResyncGoogleSession: () -> Unit = {},
    authStatusMessage: String? = null,
    onClearAuthStatus: () -> Unit = {},
    onOptionalGoogleSignIn: () -> Unit = {},
    onDismiss: () -> Unit,
    onSignOut: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val syncFormatter = remember {
        SimpleDateFormat("dd MMM HH:mm", Locale("es"))
    }
    var styleDraft by remember(preferredStyle, studioConfig.preferredStyle) {
        mutableStateOf(
            studioConfig.preferredStyle.ifBlank {
                preferredStyle.ifBlank { StudioLogic.DIRECTOR_STYLES.first().label }
            }
        )
    }
    var darkDraft by remember(isDarkMode, studioConfig.themeMode) {
        mutableStateOf(isDarkMode)
    }
    var ratioDraft by remember(studioConfig.defaultAspectRatio) {
        mutableStateOf(studioConfig.defaultAspectRatio.ifBlank { "2.39:1" })
    }
    var depthDraft by remember(studioConfig.defaultThinkingDepth) {
        mutableStateOf(ThinkingDepth.fromWire(studioConfig.defaultThinkingDepth))
    }
    var stage3DDraft by remember(studioConfig.stage3DDefault) {
        mutableStateOf(studioConfig.stage3DDefault)
    }
    var autoSaveDraft by remember(studioConfig.autoSaveDrafts) {
        mutableStateOf(studioConfig.autoSaveDrafts)
    }

    AlertDialog(
        onDismissRequest = {
            onClearAuthStatus()
            onDismiss()
        },
        containerColor = colors.surface,
        titleContentColor = colors.primary,
        textContentColor = colors.onSurface,
        title = { Text(if (user == null) "Configuración y Acceso" else "Perfil y Configuración") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (user == null) {
                    Text(
                        text = "Conecta tu cuenta de Google para sincronizar tu perfil, proyectos, hitos y configuraciones en Cloud Firestore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    if (persistedSession.hasRememberedAccount) {
                        Surface(
                            color = colors.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                                .testTag("account_persistence_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Sesión Google recordada: ${persistedSession.email.ifBlank { persistedSession.displayName }}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                OutlinedButton(
                                    onClick = onResyncGoogleSession,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("account_resync_button")
                                ) {
                                    Icon(Icons.Rounded.Sync, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text(stringResource(R.string.account_resync_cta), color = colors.primary)
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = user.displayName ?: "Director LumenReel",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface
                    )
                    Text(
                        text = user.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Surface(
                        color = colors.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                            .testTag("account_persistence_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Sesión persistente Google activa",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            val syncedLabel = if (persistedSession.lastSyncedAtMillis > 0L) {
                                "Última sincronización: ${syncFormatter.format(Date(persistedSession.lastSyncedAtMillis))}"
                            } else {
                                "Sincronización continua con Cloud Firestore habilitada"
                            }
                            Text(
                                text = "$syncedLabel · Respeta reinicios y reinstalaciones del emulador",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                            Text(
                                text = "UID privado: ${user.uid}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = colors.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = onResyncGoogleSession,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("account_resync_button")
                            ) {
                                Icon(Icons.Rounded.Sync, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(R.string.account_resync_cta), color = colors.primary)
                            }
                        }
                    }
                }

                // Global Theme & Studio Configuration
                Surface(
                    color = colors.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (darkDraft) "Tema Oscuro (Obsidiana)" else "Tema Claro (Papel Guion)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.onSurface
                                )
                                Text(
                                    text = "Se aplica y persiste en toda la aplicación",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = darkDraft,
                                onCheckedChange = { checked ->
                                    darkDraft = checked
                                    if (checked != isDarkMode) {
                                        onToggleDarkMode()
                                    }
                                },
                                modifier = Modifier.testTag("account_theme_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Escenario 3D por defecto", style = MaterialTheme.typography.labelMedium, color = colors.onSurface)
                                Text("Vista isométrica al abrir Planos y Sonido", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            }
                            Switch(
                                checked = stage3DDraft,
                                onCheckedChange = { stage3DDraft = it },
                                modifier = Modifier.testTag("account_stage3d_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-guardar borradores", style = MaterialTheme.typography.labelMedium, color = colors.onSurface)
                                Text("Restaura guion, ingesta y chat tras reiniciar", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            }
                            Switch(
                                checked = autoSaveDraft,
                                onCheckedChange = { autoSaveDraft = it },
                                modifier = Modifier.testTag("account_autosave_switch")
                            )
                        }
                    }
                }

                Text(
                    text = "Estilo cinematográfico predeterminado",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StudioLogic.DIRECTOR_STYLES.forEach { preset ->
                        FilterChip(
                            selected = styleDraft == preset.label,
                            onClick = { styleDraft = preset.label },
                            label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                            colors = dialogChipColors()
                        )
                    }
                }

                Text(
                    text = "Relación de aspecto por defecto",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("2.39:1", "1.85:1", "16:9", "1.43:1").forEach { ratio ->
                        FilterChip(
                            selected = ratioDraft == ratio,
                            onClick = { ratioDraft = ratio },
                            label = { Text(ratio, style = MaterialTheme.typography.labelSmall) },
                            colors = dialogChipColors(),
                            modifier = Modifier.testTag("account_ratio_$ratio")
                        )
                    }
                }

                Text(
                    text = "Profundidad de análisis Gemini",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThinkingDepth.entries.forEach { depth ->
                        FilterChip(
                            selected = depthDraft == depth,
                            onClick = { depthDraft = depth },
                            label = { Text(depth.label, style = MaterialTheme.typography.labelSmall) },
                            colors = dialogChipColors(),
                            modifier = Modifier.testTag("account_depth_${depth.name}")
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        val updatedConfig = studioConfig.copy(
                            themeMode = if (darkDraft) "dark" else "light",
                            preferredStyle = styleDraft,
                            defaultAspectRatio = ratioDraft,
                            defaultThinkingDepth = depthDraft.name,
                            autoSaveDrafts = autoSaveDraft,
                            stage3DDefault = stage3DDraft
                        )
                        onSaveStudioConfig(updatedConfig)
                        onSavePreferredStyle(styleDraft)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("save_profile_style_button")
                ) {
                    Text("Guardar configuración del estudio", color = colors.primary, fontWeight = FontWeight.SemiBold)
                }

                if (!authStatusMessage.isNullOrBlank()) {
                    Surface(
                        color = colors.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = authStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (user == null) {
                Button(
                    onClick = onOptionalGoogleSignIn,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                    modifier = Modifier.testTag("optional_google_sign_in_button")
                ) {
                    Text("Iniciar sesión con Google", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        onDismiss()
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError),
                    modifier = Modifier.testTag("account_sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.action_sign_out))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onClearAuthStatus()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.action_close), color = colors.onSurface)
            }
        }
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
private fun dialogChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
)
