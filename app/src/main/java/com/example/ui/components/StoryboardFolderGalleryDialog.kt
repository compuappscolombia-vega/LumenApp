package com.example.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.ProductionFolder
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.StoryboardCard
import com.example.data.util.ProjectTemplateCatalog
import com.example.data.util.StudioLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class FolderStoryboardGalleryItem(
    val sequence: ProductionSequence,
    val frame: StoryboardCard,
    val folderName: String
)

@Composable
fun StoryboardFolderGalleryGrid(
    items: List<FolderStoryboardGalleryItem>,
    generatingFrameId: String?,
    onGenerateImage: (ProductionSequence, StoryboardCard) -> Unit,
    onSelectFramePreview: (FolderStoryboardGalleryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    if (items.isEmpty()) {
        Surface(
            color = colors.surfaceVariant,
            shape = RoundedCornerShape(14.dp),
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                .testTag("gallery_empty_state")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = "Sin tarjetas de storyboard en esta carpeta",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Vincula una secuencia a esta carpeta o genera storyboards desde la Mesa de Ingesta para visualizarlos en la galería.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            modifier = modifier
                .fillMaxWidth()
                .testTag("storyboard_folder_gallery_grid"),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { idx, entry -> "${entry.sequence.id}_${entry.frame.id.ifBlank { idx.toString() }}" }
            ) { _, entry ->
                val frame = entry.frame
                val isGenerating = generatingFrameId == frame.id
                val ratio = StudioLogic.parseAspectRatio(
                    frame.aspectRatio.ifBlank { entry.sequence.aspectRatio }
                ).coerceIn(0.8f, 2.2f)
                val bitmapState by produceState<android.graphics.Bitmap?>(
                    initialValue = null,
                    frame.localImagePath
                ) {
                    val path = frame.localImagePath
                    value = if (!path.isNullOrBlank()) {
                        withContext(Dispatchers.IO) {
                            val file = File(path)
                            if (file.exists()) BitmapFactory.decodeFile(path) else null
                        }
                    } else null
                }

                Surface(
                    color = colors.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
                        .clickable { onSelectFramePreview(entry) }
                        .testTag("gallery_grid_item_${frame.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(ratio)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.outline, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val bmp = bitmapState
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = frame.title.ifBlank {
                                        stringResource(R.string.cd_storyboard_image)
                                    },
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Image,
                                        contentDescription = null,
                                        tint = colors.onSurfaceVariant.copy(alpha = 0.72f),
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Text(
                                        text = frame.shotRef.ifBlank { "Sin render" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Surface(
                                color = if (bitmapState != null) colors.secondaryContainer else colors.surfaceVariant.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = if (bitmapState != null) "Nano" else "Boceto",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (bitmapState != null) colors.onSecondaryContainer else colors.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = frame.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${entry.sequence.title} · ${frame.shotRef}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = frame.visualPrompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Button(
                            onClick = { onGenerateImage(entry.sequence, frame) },
                            enabled = !isGenerating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("gallery_generate_btn_${frame.id}")
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = colors.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.MovieCreation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    text = if (bitmapState != null) "Regenerar" else "Generar",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoryboardFolderGalleryDialog(
    project: ProductionProject,
    initialFolder: ProductionFolder,
    generatingFrameId: String?,
    onDismiss: () -> Unit,
    onGenerateImage: (ProductionSequence, StoryboardCard) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val folders = remember(project.productionFolders, project.templateId) {
        project.productionFolders.ifEmpty { ProjectTemplateCatalog.defaultFoldersFor(project.templateId) }
    }
    var selectedFolderId by remember(initialFolder.id) { mutableStateOf(initialFolder.id) }
    val activeFolder = remember(folders, selectedFolderId) {
        folders.firstOrNull { it.id == selectedFolderId } ?: initialFolder
    }
    var onlyGenerated by remember { mutableStateOf(false) }
    var previewItem by remember { mutableStateOf<FolderStoryboardGalleryItem?>(null) }

    val summary = remember(activeFolder, project.sequences) {
        ProjectTemplateCatalog.resolveFolderAssetsSummary(activeFolder, project)
    }
    val allItems = remember(summary.linkedSequences, activeFolder.name) {
        summary.linkedSequences.flatMap { seq ->
            seq.storyboardFrames.map { frame ->
                FolderStoryboardGalleryItem(
                    sequence = seq,
                    frame = frame,
                    folderName = activeFolder.name
                )
            }
        }
    }
    val visibleItems = remember(allItems, onlyGenerated) {
        if (onlyGenerated) {
            allItems.filter { !it.frame.localImagePath.isNullOrBlank() }
        } else {
            allItems
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.primary,
        textContentColor = colors.onSurface,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .widthIn(max = 820.dp)
            .fillMaxWidth(0.95f)
            .testTag("storyboard_folder_gallery_dialog"),
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
                    Icon(Icons.Rounded.GridView, contentDescription = null, tint = colors.primary)
                    Column {
                        Text(
                            text = "Galería Visual · ${activeFolder.name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${visibleItems.size} de ${allItems.size} fotogramas en la carpeta seleccionada",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("gallery_close_button")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar galería visual", tint = colors.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Folder switcher strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    folders.forEach { folder ->
                        FilterChip(
                            selected = folder.id == activeFolder.id,
                            onClick = { selectedFolderId = folder.id },
                            leadingIcon = {
                                Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            label = {
                                Text(folder.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primaryContainer,
                                selectedLabelColor = colors.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("gallery_folder_chip_${folder.id}")
                        )
                    }
                }

                // Filter bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !onlyGenerated,
                        onClick = { onlyGenerated = false },
                        label = { Text("Todos (${allItems.size})", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("gallery_filter_all")
                    )
                    FilterChip(
                        selected = onlyGenerated,
                        onClick = { onlyGenerated = true },
                        label = {
                            val generatedCount = allItems.count { !it.frame.localImagePath.isNullOrBlank() }
                            Text("Solo generados ($generatedCount)", style = MaterialTheme.typography.labelSmall)
                        },
                        modifier = Modifier.testTag("gallery_filter_generated")
                    )
                }

                StoryboardFolderGalleryGrid(
                    items = visibleItems,
                    generatingFrameId = generatingFrameId,
                    onGenerateImage = onGenerateImage,
                    onSelectFramePreview = { previewItem = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 460.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Listo", color = colors.primary, fontWeight = FontWeight.SemiBold)
            }
        }
    )

    previewItem?.let { item ->
        val bmp = remember(item.frame.localImagePath) {
            item.frame.localImagePath?.let { path ->
                if (File(path).exists()) BitmapFactory.decodeFile(path) else null
            }
        }
        Dialog(
            onDismissRequest = { previewItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background.copy(alpha = 0.96f))
                    .padding(16.dp)
                    .testTag("gallery_lightbox_dialog")
            ) {
                IconButton(
                    onClick = { previewItem = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .minimumInteractiveComponentSize()
                        .testTag("gallery_lightbox_close")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar vista previa", tint = colors.onBackground)
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = 600.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = item.frame.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        )
                    } else {
                        Surface(
                            color = colors.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Fotograma aún sin imagen renderizada",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Text(
                        text = "${item.frame.title} · ${item.frame.shotRef}",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Carpeta: ${item.folderName} · Secuencia: ${item.sequence.title}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondary
                    )
                    Text(
                        text = item.frame.visualPrompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}
