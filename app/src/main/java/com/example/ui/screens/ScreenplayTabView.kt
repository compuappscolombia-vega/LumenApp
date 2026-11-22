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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ProductionSequence
import com.example.data.model.ScreenplayScene
import com.example.data.model.SequenceVersionStatus
import com.example.data.util.LearningEngine
import com.example.data.util.StudioLogic
import com.example.ui.components.readClipboardText
import com.example.ui.components.rememberVoiceDictationController
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ErrorCrimson
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
    onOpenIngest: () -> Unit,
    onSaveSceneDraft: (
        sceneNumber: String,
        slugline: String,
        timeOfDay: String,
        actionText: String,
        dialogueCharacter: String,
        dialogueText: String,
        editingIndex: Int?
    ) -> Unit,
    onExportPdf: () -> Unit,
    onSaveScriptSchedulePdf: () -> Unit = {},
    onExportMarkdown: () -> Unit,
    onExportFountain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canExportPdf = LearningEngine.canExportPdfForSequence(sequence)
    val isDraft = sequence.versionStatus == SequenceVersionStatus.DRAFT
    var editingSceneIndex by remember(sequence.id) { mutableStateOf<Int?>(null) }
    var showSceneEditor by remember(sequence.id) { mutableStateOf(false) }
    var autoStartDictation by remember(sequence.id) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Export & Authoring Action Bar
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
                    OutlinedButton(
                        onClick = onOpenIngest,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("screenplay_new_sequence_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Nueva secuencia", color = AmberGold, style = MaterialTheme.typography.labelMedium)
                    }
                    if (isDraft) {
                        OutlinedButton(
                            onClick = {
                                editingSceneIndex = null
                                autoStartDictation = false
                                showSceneEditor = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("screenplay_add_scene_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = TealAtmos, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Añadir escena", color = TealAtmos, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (isDraft) {
                    OutlinedButton(
                        onClick = {
                            editingSceneIndex = null
                            autoStartDictation = true
                            showSceneEditor = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("screenplay_dictate_scene_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = stringResource(R.string.cd_voice_dictate_scene),
                            tint = AmberGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text("Dictar nueva escena por micrófono", color = AmberGold, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSaveScriptSchedulePdf,
                        enabled = !isExporting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealAtmos,
                            contentColor = ObsidianBg,
                            disabledContainerColor = StudioSurfaceElevated,
                            disabledContentColor = MutedStone
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_script_schedule_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = stringResource(R.string.action_save_script_schedule_pdf),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

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
                            text = if (canExportPdf) "Bitácora PDF" else "Bitácora Final Bloqueada",
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
                        text = "Puedes guardar el Guion y Plan de Rodaje en tu dispositivo en cualquier momento. Aprueba la Versión Final para desbloquear la Bitácora Completa PDF.",
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
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Esta secuencia todavía no contiene escenas de guion.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedStone
                        )
                        if (isDraft) {
                            Button(
                                onClick = {
                                    editingSceneIndex = null
                                    autoStartDictation = false
                                    showSceneEditor = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberGold,
                                    contentColor = ObsidianBg
                                ),
                                modifier = Modifier.testTag("empty_add_scene_button")
                            ) {
                                Text("Escribir primera escena", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    sequence.screenplay,
                    key = { idx, it -> "${idx}_${it.sceneNumber}_${it.slugline}" }
                ) { idx, scene ->
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ESCENA ${scene.sceneNumber} · ${scene.slugline.uppercase()}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                                    color = AmberGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isDraft) {
                                    IconButton(
                                        onClick = {
                                            editingSceneIndex = idx
                                            autoStartDictation = false
                                            showSceneEditor = true
                                        },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("edit_scene_button_$idx")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Editar escena ${scene.sceneNumber}",
                                            tint = TealAtmos,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

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

    if (showSceneEditor) {
        val existing: ScreenplayScene? = editingSceneIndex?.let { sequence.screenplay.getOrNull(it) }
        ScreenplaySceneEditorDialog(
            initialScene = existing,
            defaultSceneNumber = (sequence.screenplay.size + 1).toString(),
            autoStartDictation = autoStartDictation,
            onDismiss = {
                autoStartDictation = false
                showSceneEditor = false
            },
            onSave = { number, slugline, timeOfDay, action, character, dialogue ->
                onSaveSceneDraft(
                    number,
                    slugline,
                    timeOfDay,
                    action,
                    character,
                    dialogue,
                    editingSceneIndex
                )
                autoStartDictation = false
                showSceneEditor = false
            }
        )
    }
}

@Composable
private fun ScreenplaySceneEditorDialog(
    initialScene: ScreenplayScene?,
    defaultSceneNumber: String,
    autoStartDictation: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (
        sceneNumber: String,
        slugline: String,
        timeOfDay: String,
        actionText: String,
        dialogueCharacter: String,
        dialogueText: String
    ) -> Unit
) {
    val context = LocalContext.current
    val existingAction = remember(initialScene) {
        initialScene?.beats
            ?.filter { it.type.equals("action", ignoreCase = true) }
            ?.joinToString("\n\n") { it.text }
            .orEmpty()
    }
    val firstDialogue = remember(initialScene) {
        initialScene?.beats?.firstOrNull { it.type.equals("dialogue", ignoreCase = true) }
    }

    var sceneNumber by remember { mutableStateOf(initialScene?.sceneNumber ?: defaultSceneNumber) }
    var slugline by remember { mutableStateOf(initialScene?.slugline ?: "INT. SET PRINCIPAL - NOCHE") }
    var timeOfDay by remember { mutableStateOf(initialScene?.timeOfDay ?: "NOCHE") }
    var actionText by remember { mutableStateOf(existingAction) }
    var dialogueCharacter by remember { mutableStateOf(firstDialogue?.character.orEmpty()) }
    var dialogueText by remember { mutableStateOf(firstDialogue?.text.orEmpty()) }

    val voiceController = rememberVoiceDictationController { spoken ->
        actionText = StudioLogic.appendDictatedTranscript(actionText, spoken)
    }

    var triggeredAutoStart by remember { mutableStateOf(false) }
    LaunchedEffect(autoStartDictation) {
        if (autoStartDictation && !triggeredAutoStart) {
            triggeredAutoStart = true
            voiceController.startOrStop()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioCard,
        titleContentColor = AmberGold,
        textContentColor = IvoryPrimary,
        title = { Text(if (initialScene == null) "Nueva Escena de Guion" else "Editar Escena ${initialScene.sceneNumber}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sceneNumber,
                        onValueChange = { sceneNumber = it },
                        label = { Text("Nº") },
                        singleLine = true,
                        colors = sceneFieldColors(),
                        modifier = Modifier.weight(0.35f)
                    )
                    OutlinedTextField(
                        value = timeOfDay,
                        onValueChange = { timeOfDay = it },
                        label = { Text("Momento") },
                        singleLine = true,
                        colors = sceneFieldColors(),
                        modifier = Modifier.weight(0.65f)
                    )
                }
                OutlinedTextField(
                    value = slugline,
                    onValueChange = { slugline = it },
                    label = { Text("Encabezado (Slugline)") },
                    singleLine = true,
                    colors = sceneFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scene_slugline_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Acción visual", style = MaterialTheme.typography.labelMedium, color = WarmSilver)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { voiceController.startOrStop() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("scene_action_voice_button")
                        ) {
                            Icon(
                                imageVector = if (voiceController.isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = stringResource(R.string.cd_voice_dictate_scene),
                                tint = if (voiceController.isListening) ErrorCrimson else TealAtmos,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = if (voiceController.isListening) "Detener" else "Dictar",
                                color = if (voiceController.isListening) ErrorCrimson else TealAtmos,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = {
                                val clip = readClipboardText(context) ?: return@TextButton
                                actionText = if (actionText.isBlank()) clip else "${actionText.trim()}\n$clip"
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = AmberGold, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Pegar", color = AmberGold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (voiceController.isListening) {
                    Surface(
                        color = TealContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TealAtmos, RoundedCornerShape(8.dp))
                            .testTag("scene_voice_partial_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = TealAtmos, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (voiceController.partialTranscript.isNotBlank()) {
                                    "Escuchando: «${voiceController.partialTranscript}»"
                                } else {
                                    "Escuchando tu narración… habla cerca del micrófono."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = IvoryPrimary
                            )
                        }
                    }
                }

                if (!voiceController.errorMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scene_voice_error_text")
                    ) {
                        Text(
                            text = voiceController.errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = actionText,
                    onValueChange = { actionText = it },
                    placeholder = { Text("Describe lo que ve y escucha la cámara, o usa el micrófono…", color = MutedStone) },
                    minLines = 3,
                    maxLines = 6,
                    colors = sceneFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scene_action_input")
                )
                OutlinedTextField(
                    value = dialogueCharacter,
                    onValueChange = { dialogueCharacter = it },
                    label = { Text("Personaje (opcional)") },
                    singleLine = true,
                    colors = sceneFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dialogueText,
                    onValueChange = { dialogueText = it },
                    label = { Text("Diálogo clave (opcional)") },
                    minLines = 2,
                    maxLines = 4,
                    colors = sceneFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(sceneNumber, slugline, timeOfDay, actionText, dialogueCharacter, dialogueText) },
                enabled = actionText.isNotBlank() || dialogueText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianBg),
                modifier = Modifier.testTag("save_scene_dialog_button")
            ) {
                Text("Guardar escena", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MutedStone)
            }
        }
    )
}

@Composable
private fun sceneFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AmberGold,
    unfocusedBorderColor = StudioBorder,
    focusedTextColor = IvoryPrimary,
    unfocusedTextColor = IvoryPrimary,
    focusedLabelColor = AmberGold,
    unfocusedLabelColor = MutedStone
)
