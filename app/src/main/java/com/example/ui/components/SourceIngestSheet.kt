package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AudioAnalysisReport
import com.example.data.model.ProductionProject
import com.example.data.model.SourceMaterialType
import com.example.data.model.ThinkingDepth
import com.example.data.util.StudioLogic
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ErrorContainerDark
import com.example.ui.theme.ErrorLightText
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.MutedStone
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioDeepPanel
import com.example.ui.theme.TealAtmos
import com.example.ui.theme.TealContainer
import com.example.ui.theme.WarmSilver

private const val MAX_FILE_BYTES = 19 * 1024 * 1024

data class AttachedSourceFile(
    val filename: String,
    val mimeType: String,
    val bytes: ByteArray
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceIngestModalSheet(
    project: ProductionProject,
    isProducing: Boolean,
    isAnalyzingAudio: Boolean,
    latestAudioReport: AudioAnalysisReport?,
    onDismiss: () -> Unit,
    onSaveBible: (String) -> Unit,
    onAnalyzeAudio: (AttachedSourceFile) -> Unit,
    onProduceSequence: (
        sequenceTitle: String,
        sourceType: SourceMaterialType,
        directorStyle: String,
        aspectRatio: String,
        thinkingDepth: ThinkingDepth,
        continuityBible: String,
        sourceText: String,
        attachedFile: AttachedSourceFile?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioCard,
        contentColor = IvoryPrimary
    ) {
        SourceIngestContent(
            project = project,
            isProducing = isProducing,
            isAnalyzingAudio = isAnalyzingAudio,
            latestAudioReport = latestAudioReport,
            onClose = onDismiss,
            onSaveBible = onSaveBible,
            onAnalyzeAudio = onAnalyzeAudio,
            onProduceSequence = onProduceSequence
        )
    }
}

@Composable
fun SourceIngestContent(
    project: ProductionProject,
    isProducing: Boolean,
    isAnalyzingAudio: Boolean,
    latestAudioReport: AudioAnalysisReport?,
    onClose: (() -> Unit)?,
    onSaveBible: (String) -> Unit,
    onAnalyzeAudio: (AttachedSourceFile) -> Unit,
    onProduceSequence: (
        sequenceTitle: String,
        sourceType: SourceMaterialType,
        directorStyle: String,
        aspectRatio: String,
        thinkingDepth: ThinkingDepth,
        continuityBible: String,
        sourceText: String,
        attachedFile: AttachedSourceFile?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initialPreset = remember(project.id) {
        StudioLogic.DIRECTOR_STYLES.firstOrNull {
            project.genreStyle.contains(it.label, ignoreCase = true)
        } ?: StudioLogic.DIRECTOR_STYLES.first()
    }

    var sequenceTitle by remember(project.id) { mutableStateOf("") }
    var sourceType by remember(project.id) { mutableStateOf(project.defaultSourceType) }
    var selectedStyle by remember(project.id) { mutableStateOf(initialPreset.label) }
    var aspectRatio by remember(project.id) { mutableStateOf(initialPreset.defaultRatio) }
    var thinkingDepth by remember(project.id) { mutableStateOf(ThinkingDepth.STANDARD) }
    var continuityBible by remember(project.id, project.continuityBible) { mutableStateOf(project.continuityBible) }
    var sourceText by remember(project.id) { mutableStateOf("") }
    var attachedFile by remember(project.id) { mutableStateOf<AttachedSourceFile?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            readSourceUri(context, uri)
                .onSuccess { file ->
                    attachedFile = file
                    fileError = null
                    if (file.mimeType.startsWith("audio/")) {
                        sourceType = SourceMaterialType.AUDIOBOOK
                    }
                    if (sequenceTitle.isBlank()) {
                        sequenceTitle = file.filename.substringBeforeLast('.').take(80)
                    }
                }
                .onFailure { err ->
                    fileError = err.localizedMessage ?: "No se pudo leer el archivo."
                }
        }
    }

    val isAudioAttached = attachedFile?.let { f ->
        f.mimeType.startsWith("audio/") || listOf(".mp3", ".wav", ".m4a", ".ogg", ".aac")
            .any { f.filename.lowercase().endsWith(it) }
    } == true

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.ingest_title),
                style = MaterialTheme.typography.titleMedium,
                color = AmberGold
            )
            if (onClose != null) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("ingest_close_button")) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = MutedStone
                    )
                }
            }
        }

        // Sequence Title
        OutlinedTextField(
            value = sequenceTitle,
            onValueChange = { sequenceTitle = it },
            label = { Text(stringResource(R.string.ingest_seq_title_label)) },
            placeholder = { Text(stringResource(R.string.ingest_seq_title_hint), color = MutedStone) },
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
            color = WarmSilver
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SourceMaterialType.entries.forEach { type ->
                val selected = sourceType == type
                FilterChip(
                    selected = selected,
                    onClick = { sourceType = type },
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
            color = WarmSilver
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudioLogic.DIRECTOR_STYLES.forEach { preset ->
                val selected = selectedStyle == preset.label
                FilterChip(
                    selected = selected,
                    onClick = {
                        selectedStyle = preset.label
                        aspectRatio = preset.defaultRatio
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
            color = WarmSilver
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudioLogic.ASPECT_RATIO_OPTIONS.forEach { (ratio, label) ->
                FilterChip(
                    selected = aspectRatio == ratio,
                    onClick = { aspectRatio = ratio },
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
            color = WarmSilver
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = thinkingDepth == ThinkingDepth.STANDARD,
                onClick = { thinkingDepth = ThinkingDepth.STANDARD },
                label = { Text(stringResource(R.string.ingest_depth_standard)) },
                colors = chipColors(),
                modifier = Modifier.testTag("depth_standard")
            )
            FilterChip(
                selected = thinkingDepth == ThinkingDepth.DEEP,
                onClick = { thinkingDepth = ThinkingDepth.DEEP },
                label = { Text(stringResource(R.string.ingest_depth_deep)) },
                colors = chipColors(),
                modifier = Modifier.testTag("depth_deep")
            )
        }

        // File Attachment (SAF OpenDocument)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    openDocumentLauncher.launch(
                        arrayOf("application/pdf", "text/*", "audio/*")
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("pick_source_file_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.AttachFile,
                    contentDescription = null,
                    tint = AmberGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = attachedFile?.filename ?: stringResource(R.string.ingest_pick_file),
                    color = IvoryPrimary,
                    maxLines = 1
                )
            }
            if (attachedFile != null) {
                IconButton(
                    onClick = { attachedFile = null },
                    modifier = Modifier.testTag("clear_source_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.ingest_clear_file),
                        tint = MutedStone
                    )
                }
            }
        }

        if (fileError != null) {
            Surface(
                color = ErrorContainerDark,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = fileError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorLightText,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        if (isAudioAttached && attachedFile != null) {
            OutlinedButton(
                onClick = { onAnalyzeAudio(attachedFile!!) },
                enabled = !isAnalyzingAudio && !isProducing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("analyze_audio_only_button")
            ) {
                if (isAnalyzingAudio) {
                    CircularProgressIndicator(strokeWidth = 2.dp, color = TealAtmos, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.ingest_analyzing_audio), color = TealAtmos)
                } else {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = TealAtmos, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.ingest_analyze_audio), color = TealAtmos)
                }
            }
        }

        if (latestAudioReport != null) {
            Surface(
                color = TealContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TealAtmos.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Resumen Audio Analizado", style = MaterialTheme.typography.labelLarge, color = TealAtmos)
                    Text(
                        text = latestAudioReport.directingRecommendations.ifBlank { latestAudioReport.emotionalArc },
                        style = MaterialTheme.typography.bodySmall,
                        color = IvoryPrimary
                    )
                    if (latestAudioReport.transcription.isNotBlank()) {
                        Text(
                            text = "Transcripción: ${latestAudioReport.transcription.take(260)}…",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmSilver
                        )
                    }
                }
            }
        }

        // Source Text Input
        OutlinedTextField(
            value = sourceText,
            onValueChange = { sourceText = it },
            label = { Text(stringResource(R.string.ingest_text_label)) },
            placeholder = { Text(stringResource(R.string.ingest_text_hint), color = MutedStone) },
            minLines = 4,
            maxLines = 8,
            colors = fieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ingest_source_text_input")
        )

        // Continuity Bible Editor
        Surface(
            color = StudioDeepPanel,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
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
                        color = IvoryPrimary
                    )
                    OutlinedButton(
                        onClick = { onSaveBible(continuityBible) },
                        modifier = Modifier.testTag("save_bible_button")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.bible_save_cta), color = AmberGold, style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedTextField(
                    value = continuityBible,
                    onValueChange = { continuityBible = it },
                    placeholder = { Text(stringResource(R.string.bible_hint), color = MutedStone) },
                    minLines = 3,
                    maxLines = 6,
                    colors = fieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("continuity_bible_input")
                )
            }
        }

        // Produce Sequence CTA
        Button(
            onClick = {
                onProduceSequence(
                    sequenceTitle,
                    sourceType,
                    selectedStyle,
                    aspectRatio,
                    thinkingDepth,
                    continuityBible,
                    sourceText,
                    attachedFile
                )
            },
            enabled = !isProducing && !isAnalyzingAudio,
            colors = ButtonDefaults.buttonColors(
                containerColor = AmberGold,
                contentColor = ObsidianBg
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("produce_sequence_button")
        ) {
            if (isProducing) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = ObsidianBg, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.ingest_producing), fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.ingest_produce_cta), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AmberGold,
    unfocusedBorderColor = StudioBorder,
    focusedTextColor = IvoryPrimary,
    unfocusedTextColor = IvoryPrimary,
    focusedLabelColor = AmberGold,
    unfocusedLabelColor = MutedStone
)

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = AmberContainer,
    selectedLabelColor = AmberGold,
    containerColor = StudioDeepPanel,
    labelColor = WarmSilver
)

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
