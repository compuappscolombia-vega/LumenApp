package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.AttachedSourceFile
import com.example.data.model.AudioAnalysisReport
import com.example.data.model.IngestDraftState
import com.example.data.model.ProductionProject
import com.example.data.model.SourceMaterialType
import com.example.data.model.ThinkingDepth
import com.example.data.util.StudioLogic

private const val MAX_FILE_BYTES = 19 * 1024 * 1024

@Composable
fun SourceIngestModalSheet(
    project: ProductionProject,
    draft: IngestDraftState,
    isProducing: Boolean,
    isAnalyzingAudio: Boolean,
    latestAudioReport: AudioAnalysisReport?,
    onDraftChange: ((IngestDraftState) -> IngestDraftState) -> Unit,
    onDismiss: () -> Unit,
    onSaveBible: (String) -> Unit,
    onAnalyzeAudio: (AttachedSourceFile) -> Unit,
    onProduceSequence: () -> Unit,
    onCreateManualDraft: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .statusBarsPadding()
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            val colors = MaterialTheme.colorScheme
            Surface(
                color = colors.surface,
                contentColor = colors.onSurface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .navigationBarsPadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                SourceIngestContent(
                    project = project,
                    draft = draft,
                    isProducing = isProducing,
                    isAnalyzingAudio = isAnalyzingAudio,
                    latestAudioReport = latestAudioReport,
                    onDraftChange = onDraftChange,
                    onClose = onDismiss,
                    onSaveBible = onSaveBible,
                    onAnalyzeAudio = onAnalyzeAudio,
                    onProduceSequence = onProduceSequence,
                    onCreateManualDraft = onCreateManualDraft
                )
            }
        }
    }
}

@Composable
fun SourceIngestContent(
    project: ProductionProject,
    draft: IngestDraftState,
    isProducing: Boolean,
    isAnalyzingAudio: Boolean,
    latestAudioReport: AudioAnalysisReport?,
    onDraftChange: ((IngestDraftState) -> IngestDraftState) -> Unit,
    onClose: (() -> Unit)?,
    onSaveBible: (String) -> Unit,
    onAnalyzeAudio: (AttachedSourceFile) -> Unit,
    onProduceSequence: () -> Unit,
    onCreateManualDraft: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var clipboardNotice by remember { mutableStateOf<String?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            readSourceUri(context, uri)
                .onSuccess { file ->
                    clipboardNotice = null
                    val nextType = if (isAudioFile(file)) SourceMaterialType.AUDIOBOOK else draft.sourceType
                    val nextTitle = if (draft.sequenceTitle.isBlank()) {
                        file.filename.substringBeforeLast('.').take(80)
                    } else draft.sequenceTitle
                    onDraftChange {
                        it.copy(
                            attachedFile = file,
                            fileError = null,
                            ingestError = null,
                            sourceType = nextType,
                            sequenceTitle = nextTitle
                        )
                    }
                }
                .onFailure { err ->
                    onDraftChange {
                        it.copy(fileError = err.localizedMessage ?: "No se pudo leer el archivo.")
                    }
                }
        }
    }

    val attachedFile = draft.attachedFile
    val isAudioAttached = attachedFile != null && isAudioFile(attachedFile)
    val isTextFileAttached = attachedFile != null && !isAudioAttached &&
        !attachedFile.filename.lowercase().endsWith(".pdf") &&
        attachedFile.mimeType != "application/pdf"

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ingest_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primary
                )
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.78f)
                )
            }
            if (onClose != null) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("ingest_close_button")) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = colors.onSurfaceVariant
                    )
                }
            }
        }
        HorizontalDivider(color = colors.outline)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

        // Sequence Title
        OutlinedTextField(
            value = draft.sequenceTitle,
            onValueChange = { value ->
                onDraftChange { it.copy(sequenceTitle = value, ingestError = null) }
            },
            label = { Text(stringResource(R.string.ingest_seq_title_label)) },
            placeholder = {
                Text(
                    stringResource(R.string.ingest_seq_title_hint),
                    color = colors.onSurfaceVariant.copy(alpha = 0.72f)
                )
            },
            singleLine = true,
            colors = fieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ingest_sequence_title_input")
        )

        // Source Material Type
        Text(
            text = stringResource(R.string.ingest_source_type_label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SourceMaterialType.entries.forEach { type ->
                val selected = draft.sourceType == type
                FilterChip(
                    selected = selected,
                    onClick = { onDraftChange { it.copy(sourceType = type) } },
                    label = {
                        Text(
                            text = when (type) {
                                SourceMaterialType.BOOK -> "Libro"
                                SourceMaterialType.AUDIOBOOK -> "Audiolibro"
                                SourceMaterialType.STORY -> "Historia"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = chipColors(),
                    modifier = Modifier.testTag("source_type_${type.wireValue}")
                )
            }
        }

        // Director Style Presets
        Text(
            text = stringResource(R.string.ingest_style_label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudioLogic.DIRECTOR_STYLES.forEach { preset ->
                val selected = draft.directorStyle == preset.label
                FilterChip(
                    selected = selected,
                    onClick = {
                        onDraftChange {
                            it.copy(
                                directorStyle = preset.label,
                                aspectRatio = preset.defaultRatio
                            )
                        }
                    },
                    label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                    colors = chipColors(),
                    modifier = Modifier.testTag("style_preset_${preset.id}")
                )
            }
        }

        // Aspect Ratio Options
        Text(
            text = stringResource(R.string.ingest_ratio_label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudioLogic.ASPECT_RATIO_OPTIONS.forEach { (ratio, label) ->
                FilterChip(
                    selected = draft.aspectRatio == ratio,
                    onClick = { onDraftChange { it.copy(aspectRatio = ratio) } },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = chipColors(),
                    modifier = Modifier.testTag("aspect_ratio_$ratio")
                )
            }
        }

        // Thinking Depth
        Text(
            text = stringResource(R.string.ingest_depth_label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = draft.thinkingDepth == ThinkingDepth.STANDARD,
                onClick = { onDraftChange { it.copy(thinkingDepth = ThinkingDepth.STANDARD) } },
                label = { Text(stringResource(R.string.ingest_depth_standard)) },
                colors = chipColors(),
                modifier = Modifier.testTag("depth_standard")
            )
            FilterChip(
                selected = draft.thinkingDepth == ThinkingDepth.DEEP,
                onClick = { onDraftChange { it.copy(thinkingDepth = ThinkingDepth.DEEP) } },
                label = { Text(stringResource(R.string.ingest_depth_deep)) },
                colors = chipColors(),
                modifier = Modifier.testTag("depth_deep")
            )
        }

        // File Attachment (SAF OpenDocument with */* so PC-transferred .txt/.md/.fountain are never greyed out)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    openDocumentLauncher.launch(arrayOf("*/*"))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("pick_source_file_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.AttachFile,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = attachedFile?.filename ?: stringResource(R.string.ingest_pick_file),
                    color = colors.onSurface,
                    maxLines = 1
                )
            }
            if (attachedFile != null) {
                IconButton(
                    onClick = { onDraftChange { it.copy(attachedFile = null, fileError = null) } },
                    modifier = Modifier.testTag("clear_source_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.ingest_clear_file),
                        tint = colors.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "Formatos: PDF, TXT, MD, Fountain, SRT o Audio (MP3/WAV/M4A). Desde tu PC, arrastra el archivo sobre la ventana del emulador para guardarlo en Descargas y selecciónalo aquí.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant.copy(alpha = 0.78f)
        )

        if (isTextFileAttached) {
            OutlinedButton(
                onClick = {
                    val decoded = attachedFile.bytes.toString(Charsets.UTF_8).take(120_000)
                    onDraftChange {
                        val merged = if (it.sourceText.isBlank()) decoded else "${it.sourceText.trim()}\n\n$decoded"
                        it.copy(sourceText = merged, ingestError = null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("import_attached_text_button")
            ) {
                Text(
                    text = "Copiar contenido de ${attachedFile.filename} al campo de texto",
                    color = colors.secondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        if (draft.fileError != null) {
            Surface(
                color = colors.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ingest_file_error_banner")
            ) {
                Text(
                    text = draft.fileError,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onErrorContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        if (isAudioAttached) {
            OutlinedButton(
                onClick = { onAnalyzeAudio(attachedFile) },
                enabled = !isAnalyzingAudio && !isProducing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("analyze_audio_only_button")
            ) {
                if (isAnalyzingAudio) {
                    CircularProgressIndicator(strokeWidth = 2.dp, color = colors.secondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.ingest_analyzing_audio), color = colors.secondary)
                } else {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.ingest_analyze_audio), color = colors.secondary)
                }
            }
        }

        if (latestAudioReport != null) {
            Surface(
                color = colors.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.secondary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Resumen Audio Analizado", style = MaterialTheme.typography.labelLarge, color = colors.onSecondaryContainer)
                    Text(
                        text = latestAudioReport.directingRecommendations.ifBlank { latestAudioReport.emotionalArc },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSecondaryContainer
                    )
                    if (latestAudioReport.transcription.isNotBlank()) {
                        Text(
                            text = "Transcripción: ${latestAudioReport.transcription.take(260)}…",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Source Text Input + Explicit Paste & Sample controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.ingest_text_label),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        val clip = readClipboardText(context)
                        if (clip.isNullOrBlank()) {
                            clipboardNotice = "El portapapeles está vacío. Copia primero un texto o toca «Ejemplo»."
                        } else {
                            clipboardNotice = "Texto pegado (${clip.length} caracteres)."
                            onDraftChange {
                                val next = if (it.sourceText.isBlank()) clip else "${it.sourceText.trim()}\n\n$clip"
                                it.copy(sourceText = next, ingestError = null)
                            }
                        }
                    },
                    modifier = Modifier.testTag("ingest_paste_source_button")
                ) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Pegar", color = colors.primary, style = MaterialTheme.typography.labelSmall)
                }
                TextButton(
                    onClick = {
                        clipboardNotice = null
                        onDraftChange {
                            it.copy(
                                sequenceTitle = it.sequenceTitle.ifBlank { StudioLogic.SAMPLE_SEQUENCE_TITLE },
                                sourceText = StudioLogic.SAMPLE_SOURCE_TEXT,
                                ingestError = null
                            )
                        }
                    },
                    modifier = Modifier.testTag("ingest_load_sample_button")
                ) {
                    Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Ejemplo", color = colors.secondary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (clipboardNotice != null) {
            Text(
                text = clipboardNotice!!,
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondary
            )
        }

        OutlinedTextField(
            value = draft.sourceText,
            onValueChange = { value ->
                onDraftChange { it.copy(sourceText = value, ingestError = null) }
            },
            placeholder = {
                Text(
                    stringResource(R.string.ingest_text_hint),
                    color = colors.onSurfaceVariant.copy(alpha = 0.72f)
                )
            },
            minLines = 4,
            maxLines = 8,
            colors = fieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ingest_source_text_input")
        )

        // Continuity Bible Editor
        Surface(
            color = colors.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
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
                    Text(
                        text = stringResource(R.string.bible_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            val clip = readClipboardText(context)
                            if (clip.isNullOrBlank()) {
                                clipboardNotice = "El portapapeles está vacío."
                            } else {
                                onDraftChange {
                                    val next = if (it.continuityBible.isBlank()) clip else "${it.continuityBible.trim()}\n\n$clip"
                                    it.copy(continuityBible = next)
                                }
                            }
                        },
                        modifier = Modifier.testTag("ingest_paste_bible_button")
                    ) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = "Pegar en Biblia", tint = colors.onSurfaceVariant, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Pegar", color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { onSaveBible(draft.continuityBible) },
                        modifier = Modifier.testTag("save_bible_button")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.bible_save_cta), color = colors.primary, style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedTextField(
                    value = draft.continuityBible,
                    onValueChange = { value ->
                        onDraftChange { it.copy(continuityBible = value) }
                    },
                    placeholder = {
                        Text(
                            stringResource(R.string.bible_hint),
                            color = colors.onSurfaceVariant.copy(alpha = 0.72f)
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    colors = fieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("continuity_bible_input")
                )
            }
        }
        }

        // Pinned Footer: Error Banner + Primary AI Produce + Secondary Manual Draft CTA
        HorizontalDivider(color = colors.outline)
        Surface(
            color = colors.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!draft.ingestError.isNullOrBlank()) {
                    Surface(
                        color = colors.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ingest_error_banner")
                    ) {
                        Text(
                            text = draft.ingestError,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = onProduceSequence,
                    enabled = !isProducing && !isAnalyzingAudio,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("produce_sequence_button")
                ) {
                    if (isProducing) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = colors.onPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.ingest_producing), fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.ingest_produce_cta), fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onCreateManualDraft,
                    enabled = !isProducing && !isAnalyzingAudio,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("create_manual_draft_button")
                ) {
                    Icon(Icons.Rounded.EditNote, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Crear borrador manual (sin IA) en Guion", color = colors.secondary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun fieldColors(): androidx.compose.material3.TextFieldColors {
    val colors = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.outline,
        focusedTextColor = colors.onSurface,
        unfocusedTextColor = colors.onSurface,
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = colors.onSurfaceVariant.copy(alpha = 0.78f)
    )
}

@Composable
private fun chipColors(): androidx.compose.material3.SelectableChipColors {
    val colors = MaterialTheme.colorScheme
    return FilterChipDefaults.filterChipColors(
        selectedContainerColor = colors.primaryContainer,
        selectedLabelColor = colors.onPrimaryContainer,
        containerColor = colors.surfaceVariant,
        labelColor = colors.onSurfaceVariant
    )
}

internal fun readClipboardText(context: Context): String? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = cm.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
}

private fun isAudioFile(file: AttachedSourceFile): Boolean {
    val lower = file.filename.lowercase()
    return file.mimeType.startsWith("audio/") ||
        listOf(".mp3", ".wav", ".m4a", ".ogg", ".aac", ".flac", ".webm").any { lower.endsWith(it) }
}

private fun readSourceUri(context: Context, uri: Uri): Result<AttachedSourceFile> {
    return try {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        var name = "archivo_fuente"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    name = cursor.getString(idx) ?: name
                }
            }
        }
        if (!StudioLogic.isSupportedSourceFile(name, mime)) {
            return Result.failure(
                IllegalArgumentException(
                    "Archivo «$name» no compatible. Usa PDF, TXT, MD, Fountain, SRT o audio MP3/WAV/M4A."
                )
            )
        }
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return Result.failure(IllegalStateException("No se pudo abrir el stream del archivo."))
        if (bytes.size > MAX_FILE_BYTES) {
            return Result.failure(IllegalArgumentException("El archivo supera el límite máximo de 19 MB."))
        }
        Result.success(AttachedSourceFile(filename = name, mimeType = mime, bytes = bytes))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
