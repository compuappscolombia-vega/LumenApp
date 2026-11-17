package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.data.model.ProductionSequence
import com.example.data.model.StoryboardCard
import com.example.data.util.StudioLogic
import com.example.ui.theme.AmberGold
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.MutedStone
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioDeepPanel
import com.example.ui.theme.TealAtmos
import com.example.ui.theme.WarmSilver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun StoryboardTabView(
    sequence: ProductionSequence,
    generatingFrameId: String?,
    onGenerateImage: (StoryboardCard) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var copiedBanner by remember { mutableStateOf<String?>(null) }
    var lightboxFrame by remember { mutableStateOf<StoryboardCard?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        if (copiedBanner != null) {
            Surface(
                color = StudioDeepPanel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = copiedBanner!!,
                    style = MaterialTheme.typography.labelMedium,
                    color = TealAtmos,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

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
                        color = MutedStone
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
        color = StudioCard,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
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
                        color = IvoryPrimary
                    )
                    Text(
                        text = "${frame.shotRef} · ${frame.cameraSpec}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberGold
                    )
                }
            }

            // Image Frame Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioDeepPanel)
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
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
                            tint = MutedStone,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Sin fotograma renderizado aún",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedStone
                        )
                    }
                }
            }

            // Prompts
            Text(
                text = "Visual Prompt: ${frame.visualPrompt}",
                style = MaterialTheme.typography.bodySmall,
                color = WarmSilver
            )
            if (frame.veoMotionPrompt.isNotBlank()) {
                Text(
                    text = "Motion Prompt (Cámara): ${frame.veoMotionPrompt}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TealAtmos
                )
            }

            // Actions
            Button(
                onClick = onGenerate,
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberGold,
                    contentColor = ObsidianBg
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
                        color = ObsidianBg,
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
                        tint = IvoryPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text("Visual", color = IvoryPrimary, style = MaterialTheme.typography.labelMedium)
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
                            tint = TealAtmos,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text("Movimiento", color = TealAtmos, style = MaterialTheme.typography.labelMedium)
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
                .background(ObsidianBg.copy(alpha = 0.96f))
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
                    tint = IvoryPrimary
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
                Text(frame.title, style = MaterialTheme.typography.titleMedium, color = AmberGold)
                Text(frame.visualPrompt, style = MaterialTheme.typography.bodySmall, color = WarmSilver)
            }
        }
    }
}
