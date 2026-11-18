package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LearningCategory
import com.example.data.model.LearningLesson
import com.example.data.model.SourceMaterialType
import com.example.data.util.LearningEngine
import com.example.data.util.StudioLogic
import com.example.ui.screens.copyToClipboard
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.MutedStone
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioDeepPanel
import com.example.ui.theme.TealAtmos
import com.example.ui.theme.WarmSilver
import com.google.firebase.auth.FirebaseUser

@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        genreStyle: String,
        defaultSourceType: SourceMaterialType,
        continuityBible: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(StudioLogic.DIRECTOR_STYLES.first().label) }
    var sourceType by remember { mutableStateOf(SourceMaterialType.BOOK) }
    var bible by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioCard,
        titleContentColor = AmberGold,
        textContentColor = IvoryPrimary,
        title = { Text("Nueva Producción Cinematográfica") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la producción") },
                    placeholder = { Text("Ej. Crónicas de Marea Negra", color = MutedStone) },
                    singleLine = true,
                    colors = dialogFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_title_input")
                )

                Text("Estilo cinematográfico base", style = MaterialTheme.typography.labelMedium, color = WarmSilver)
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

                Text("Material fuente principal", style = MaterialTheme.typography.labelMedium, color = WarmSilver)
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
                    onValueChange = { bible = it },
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
                        onCreate(title.trim(), selectedStyle, sourceType, bible.trim())
                        onDismiss()
                    }
                },
                enabled = title.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianBg),
                modifier = Modifier.testTag("confirm_create_project_button")
            ) {
                Text("Crear producción", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = MutedStone)
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
    var selectedCategory by remember { mutableStateOf(LearningCategory.CAMERA) }
    var insightText by remember { mutableStateOf("") }
    var rating by remember { mutableIntStateOf(5) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioCard,
        titleContentColor = AmberGold,
        textContentColor = IvoryPrimary,
        title = { Text("Autoaprendizaje del Estudio (${lessons.size})") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Las reglas guardadas se inyectan automáticamente en Gemini y en el Agente Tripartito para futuras producciones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedStone
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
                    Text("Prioridad:", style = MaterialTheme.typography.labelSmall, color = MutedStone)
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = "$star estrellas",
                                tint = if (star <= rating) AmberGold else MutedStone,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Text(validationError!!, style = MaterialTheme.typography.labelSmall, color = ErrorCrimson)
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
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianBg),
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
                            color = StudioDeepPanel,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
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
                                        color = AmberGold
                                    )
                                    Text(lesson.insight, style = MaterialTheme.typography.bodySmall, color = IvoryPrimary)
                                    Text(lesson.sourceProjectTitle, style = MaterialTheme.typography.labelSmall, color = MutedStone)
                                }
                                IconButton(
                                    onClick = { onDeleteLesson(lesson.id) },
                                    modifier = Modifier.testTag("delete_lesson_${lesson.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Eliminar aprendizaje",
                                        tint = MutedStone
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
                Text(stringResource(R.string.action_close), color = IvoryPrimary)
            }
        }
    )
}

@Composable
fun AgentBlueprintDialog(
    onDismiss: () -> Unit
) {
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
        containerColor = StudioCard,
        titleContentColor = AmberGold,
        textContentColor = IvoryPrimary,
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
                    color = MutedStone
                )
                Surface(
                    color = StudioDeepPanel,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = blueprintJson,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = WarmSilver,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                if (copied) {
                    Text("Plantilla JSON copiada al portapapeles.", style = MaterialTheme.typography.labelSmall, color = TealAtmos)
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
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("Copiar JSON", color = AmberGold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), color = IvoryPrimary)
            }
        }
    )
}

@Composable
fun AccountDialog(
    user: FirebaseUser?,
    authStatusMessage: String? = null,
    onClearAuthStatus: () -> Unit = {},
    onOptionalGoogleSignIn: () -> Unit = {},
    onDismiss: () -> Unit,
    onSignOut: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            onClearAuthStatus()
            onDismiss()
        },
        containerColor = StudioCard,
        titleContentColor = AmberGold,
        textContentColor = IvoryPrimary,
        title = { Text(if (user == null) "Modo Estudio Local" else "Cuenta de Estudio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (user == null) {
                    Text(
                        text = "Estudio sin inicio de sesión",
                        style = MaterialTheme.typography.titleMedium,
                        color = IvoryPrimary
                    )
                    Text(
                        text = "El inicio de sesión está omitido temporalmente. Todas tus producciones, secuencias, guiones y memoria de aprendizaje se guardan en este dispositivo y el Agente Gemini funciona directamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmSilver
                    )
                } else {
                    Text(
                        text = user.displayName ?: "Director LumenReel",
                        style = MaterialTheme.typography.titleMedium,
                        color = IvoryPrimary
                    )
                    Text(
                        text = user.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmSilver
                    )
                    Surface(
                        color = StudioDeepPanel,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "UID privado: ${user.uid}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MutedStone,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                if (!authStatusMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = authStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (user == null) {
                OutlinedButton(
                    onClick = onOptionalGoogleSignIn,
                    modifier = Modifier.testTag("optional_google_sign_in_button")
                ) {
                    Text("Conectar con Google (Opcional)", color = AmberGold)
                }
            } else {
                Button(
                    onClick = {
                        onDismiss()
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson, contentColor = IvoryPrimary),
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
                Text(stringResource(R.string.action_close), color = IvoryPrimary)
            }
        }
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AmberGold,
    unfocusedBorderColor = StudioBorder,
    focusedTextColor = IvoryPrimary,
    unfocusedTextColor = IvoryPrimary,
    focusedLabelColor = AmberGold,
    unfocusedLabelColor = MutedStone
)

@Composable
private fun dialogChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = AmberContainer,
    selectedLabelColor = AmberGold,
    containerColor = StudioDeepPanel,
    labelColor = WarmSilver
)
