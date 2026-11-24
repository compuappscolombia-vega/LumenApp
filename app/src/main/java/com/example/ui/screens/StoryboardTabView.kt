package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.StoryboardCard
import com.example.data.util.ProjectTemplateCatalog
import com.example.data.util.StudioLogic
import com.example.ui.components.FolderStoryboardGalleryItem
import com.example.ui.components.StoryboardFolderGalleryGrid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun StoryboardTabView(
    sequence: ProductionSequence,
    generatingFrameId: String?,
    onGenerateImage: (StoryboardCard) -> Unit,
    project: ProductionProject? = null,
    onGenerateFolderFrame: ((ProductionSequence, StoryboardCard) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var copiedBanner by remember { mutableStateOf<String?>(null) }
    var lightboxFrame by remember { mutableStateOf<StoryboardCard?>(null) }
    var isGridView by remember { mutableStateOf(false) }
    var selectedFolderId by remember(project?.id) { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        if (copiedBanner != null) {
            Surface(
                color = colors.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = copiedBanner!!,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.secondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Surface(
            color = colors.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.outline)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !isGridView,
                        onClick = { isGridView = false },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Rounded.ViewList, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Lista detallada", style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primaryContainer,
                            selectedLabelColor = colors.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("storyboard_view_list")
                    )
                    FilterChip(
                        selected = isGridView,
                        onClick = { isGridView = true },
                        leadingIcon = {
                            Icon(Icons.Rounded.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Galería Grid", style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primaryContainer,
                            selectedLabelColor = colors.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("storyboard_view_grid")
                    )
                }

                if (isGridView && project != null) {
                    val folders = remember(project.productionFolders, project.templateId) {
                        project.productionFolders.ifEmpty { ProjectTemplateCatalog.defaultFoldersFor(project.templateId) }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFolderId == null,
                            onClick = { selectedFolderId = null },
                            label = { Text("Secuencia actual", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("storyboard_grid_scope_sequence")
                        )
                        folders.forEach { folder ->
                            FilterChip(
                                selected = selectedFolderId == folder.id,
                                onClick = { selectedFolderId = folder.id },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                label = { Text(folder.name, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("storyboard_grid_folder_${folder.id}")
                            )
                        }
                    }
                }
            }
        }

        if (isGridView) {
            val gridItems = remember(sequence, project, selectedFolderId) {
                val activeFolder = if (project != null && selectedFolderId != null) {
                    val folders = project.productionFolders.ifEmpty {
                        ProjectTemplateCatalog.defaultFoldersFor(project.templateId)
                    }
                    folders.firstOrNull { it.id == selectedFolderId }
                } else null

                if (project != null && activeFolder != null) {
                    val summary = ProjectTemplateCatalog.resolveFolderAssetsSummary(activeFolder, project)
                    summary.linkedSequences.flatMap { seq ->
                        seq.storyboardFrames.map { card ->
                            FolderStoryboardGalleryItem(
                                sequence = seq,
                                frame = card,
                                folderName = activeFolder.name
                            )
                        }
                    }
                } else {
                    sequence.storyboardFrames.map { card ->
                        FolderStoryboardGalleryItem(
                            sequence = sequence,
                            frame = card,
                            folderName = sequence.title
                        )
                    }
                }
            }

            StoryboardFolderGalleryGrid(
                items = gridItems,
                generatingFrameId = generatingFrameId,
                onGenerateImage = { seq, card ->
                    onGenerateFolderFrame?.invoke(seq, card) ?: onGenerateImage(card)
                },
                onSelectFramePreview = { entry -> lightboxFrame = entry.frame },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (sequence.storyboardFrames.isEmpty()) {
                    item {
                        Text(
                            text = "No hay tarjetas de storyboard generadas todavía.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant.copy(alpha = 0.78f)
                        )
                    }
                } else {
                    itemsIndexed(sequence.storyboardFrames, key = { idx, f -> f.id.ifBlank { "sb_$idx" } }) { idx, frame ->
                        StoryboardFrameItem(
                            index = idx,
                            frame = frame,
                            sequenceAspectRatio = sequence.aspectRatio,
                            isGenerating = generatingFrameId == frame.id,
                            onGenerate = { onGenerateImage(frame) },
                            onOpenLightbox = { lightboxFrame = frame },
                            onCopyVisual = {
                                copyToClipboard(context, frame.visualPrompt)
                                copiedBanner = "Prompt visual de \"${frame.title}\" copiado."
                            },
                            onCopyMotion = {
                                copyToClipboard(context, frame.veoMotionPrompt)
                                copiedBanner = "Prompt de movimiento de cámara copiado."
                            }
                        )
                    }
                }
            }
        }
    }

    lightboxFrame?.let { active ->
        LightboxImageDialog(
            frame = active,
            onDismiss = { lightboxFrame = null }
        )
    }
}

@Composable
private fun StoryboardFrameItem(
    index: Int,
    frame: StoryboardCard,
    sequenceAspectRatio: String,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    onOpenLightbox: () -> Unit,
    onCopyVisual: () -> Unit,
    onCopyMotion: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val ratio = StudioLogic.parseAspectRatio(frame.aspectRatio.ifBlank { sequenceAspectRatio })
        .coerceIn(0.6f, 2.5f)
    val bitmapState by produceState<android.graphics.Bitmap?>(initialValue = null, frame.localImagePath) {
        val path = frame.localImagePath
        value = if (!path.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                val file = File(path)
                if (file.exists()) BitmapFactory.decodeFile(path) else null
            }
        } else null
    }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = frame.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface
                    )
                    Text(
                        text = "${frame.shotRef} · ${frame.cameraSpec}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary
                    )
                }
            }

            // Image Frame Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceVariant)
                    .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
                    .clickable(enabled = bitmapState != null) { onOpenLightbox() },
                contentAlignment = Alignment.Center
            ) {
                val bmp = bitmapState
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = stringResource(R.string.cd_storyboard_image),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant.copy(alpha = 0.72f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Sin fotograma renderizado aún",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant.copy(alpha = 0.78f)
                        )
                    }
                }
            }

            // Prompts
            Text(
                text = "Visual Prompt: ${frame.visualPrompt}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            if (frame.veoMotionPrompt.isNotBlank()) {
                Text(
                    text = "Motion Prompt (Cámara): ${frame.veoMotionPrompt}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondary
                )
            }

            // Actions
            Button(
                onClick = onGenerate,
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("generate_storyboard_btn_$index")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = colors.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Generando con Nano…", fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MovieCreation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = if (bitmapState != null) "Regenerar imagen (Nano)" else "Generar imagen (Nano)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyVisual,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("copy_visual_prompt_$index")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        tint = colors.onSurface,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text("Visual", color = colors.onSurface, style = MaterialTheme.typography.labelMedium)
                }
                if (frame.veoMotionPrompt.isNotBlank()) {
                    OutlinedButton(
                        onClick = onCopyMotion,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("copy_motion_prompt_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            tint = colors.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text("Movimiento", color = colors.secondary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun LightboxImageDialog(
    frame: StoryboardCard,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val bmp = remember(frame.localImagePath) {
        frame.localImagePath?.let { path ->
            if (File(path).exists()) BitmapFactory.decodeFile(path) else null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background.copy(alpha = 0.97f))
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .testTag("close_lightbox_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Cerrar vista ampliada",
                    tint = colors.onBackground
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = frame.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    )
                }
                Text(frame.title, style = MaterialTheme.typography.titleMedium, color = colors.primary)
                Text(frame.visualPrompt, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}
