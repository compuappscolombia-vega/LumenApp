package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ProductionSequence
import com.example.data.model.ShotSpec
import com.example.data.util.StudioLogic
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.MutedStone
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioDeepPanel
import com.example.ui.theme.TealAtmos
import com.example.ui.theme.TealContainer
import com.example.ui.theme.WarmSilver
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private data class Vec3(val x: Float, val y: Float, val z: Float)

private data class Face3D(
    val vertices: List<Vec3>,
    val fill: Color,
    val stroke: Color? = null,
    val strokeWidthPx: Float = 1.2f
)

private enum class StageCameraPreset(
    val label: String,
    val yawDeg: Float,
    val pitchDeg: Float,
    val zoom: Float
) {
    DIRECTOR("Director", -38f, 24f, 1.0f),
    TOP("Cenital", 0f, 74f, 0.92f),
    POV("Cámara POV", 4f, 12f, 1.22f),
    RIM("Contraluz", 142f, 22f, 1.05f)
}

@Composable
fun CinemaSet3DStageCard(
    sequence: ProductionSequence,
    activeShot: ShotSpec?,
    onOpenFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF07080D),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
    ) {
        CinemaSet3DViewport(
            sequence = sequence,
            activeShot = activeShot,
            isFullscreen = false,
            onToggleFullscreen = onOpenFullscreen,
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
        )
    }
}

@Composable
fun CinemaSet3DFullscreenDialog(
    sequence: ProductionSequence,
    activeShot: ShotSpec?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg)
        ) {
            CinemaSet3DViewport(
                sequence = sequence,
                activeShot = activeShot,
                isFullscreen = true,
                onToggleFullscreen = onDismiss,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CinemaSet3DViewport(
    sequence: ProductionSequence,
    activeShot: ShotSpec?,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var targetYaw by remember { mutableFloatStateOf(StageCameraPreset.DIRECTOR.yawDeg) }
    var targetPitch by remember { mutableFloatStateOf(StageCameraPreset.DIRECTOR.pitchDeg) }
    var targetZoom by remember { mutableFloatStateOf(StageCameraPreset.DIRECTOR.zoom) }

    var currentYaw by remember { mutableFloatStateOf(targetYaw) }
    var currentPitch by remember { mutableFloatStateOf(targetPitch) }
    var currentZoom by remember { mutableFloatStateOf(targetZoom) }

    var autoOrbit by remember { mutableStateOf(false) }
    var dockExpanded by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(StageCameraPreset.DIRECTOR) }

    // Frame-synchronized exponential damping + optional idle orbit
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    if (autoOrbit) {
                        targetYaw += 14f * dt
                    }
                    val alpha = 1f - exp(-9.5f * dt)
                    currentYaw += (targetYaw - currentYaw) * alpha
                    currentPitch += (targetPitch - currentPitch) * alpha
                    currentZoom += (targetZoom - currentZoom) * alpha
                }
                lastNanos = now
            }
        }
    }

    val focalLengthMm = remember(activeShot?.lensMm) {
        parseFocalLengthMm(activeShot?.lensMm)
    }
    val aspectRatioVal = remember(sequence.aspectRatio) {
        StudioLogic.parseAspectRatio(sequence.aspectRatio)
    }
    val keyKelvinColor = remember(sequence.lighting.colorTemperatureKelvin) {
        kelvinToAccentColor(sequence.lighting.colorTemperatureKelvin)
    }
    val setAccentColor = remember(sequence.colorPalette) {
        parsePaletteHex(sequence.colorPalette.firstOrNull()?.hex, TealAtmos)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isFullscreen) 0.dp else 14.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF151824), Color(0xFF0A0B10), Color(0xFF050608))
                )
            )
            .semantics {
                contentDescription = "Escenario 3D interactivo de cámara e iluminación para ${sequence.title}"
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("stage_3d_canvas")
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        autoOrbit = false
                        targetYaw += dragAmount.x * 0.42f
                        targetPitch = (targetPitch + dragAmount.y * 0.35f).coerceIn(6f, 84f)
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) {
                            targetZoom = (targetZoom * zoom).coerceIn(0.65f, 1.85f)
                        }
                    }
                }
        ) {
            drawCinemaStage3D(
                yawDeg = currentYaw,
                pitchDeg = currentPitch,
                zoom = currentZoom,
                focalLengthMm = focalLengthMm,
                aspectRatio = aspectRatioVal,
                keyLightColor = keyKelvinColor,
                setAccentColor = setAccentColor
            )
        }

        // Top-Left HUD Badge
        Surface(
            color = ObsidianBg.copy(alpha = 0.78f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ViewInAr,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = activeShot?.let { "Plano ${it.shotNumber} · ${focalLengthMm.toInt()}mm" }
                            ?: "Pre-Vis 3D · ${focalLengthMm.toInt()}mm",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${sequence.aspectRatio} · ${sequence.lighting.colorTemperatureKelvin.ifBlank { "3200K" }} · Arrastra para orbitar",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmSilver
                )
            }
        }

        // Top-Right Fullscreen / Close Button
        Surface(
            color = ObsidianBg.copy(alpha = 0.78f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
        ) {
            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("stage_3d_fullscreen_button")
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Rounded.Close else Icons.Rounded.Fullscreen,
                    contentDescription = if (isFullscreen) "Salir de pantalla completa 3D" else "Expandir escenario 3D",
                    tint = IvoryPrimary
                )
            }
        }

        // Bottom-Right Collapsible Control Dock
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(
                visible = dockExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = StudioCard.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StageCameraPreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = selectedPreset == preset,
                                    onClick = {
                                        autoOrbit = false
                                        selectedPreset = preset
                                        targetYaw = preset.yawDeg
                                        targetPitch = preset.pitchDeg
                                        targetZoom = preset.zoom
                                    },
                                    label = {
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberContainer,
                                        selectedLabelColor = AmberGold,
                                        containerColor = StudioDeepPanel,
                                        labelColor = WarmSilver
                                    )
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { autoOrbit = !autoOrbit },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (autoOrbit) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (autoOrbit) "Pausar órbita automática" else "Activar órbita automática",
                                    tint = if (autoOrbit) TealAtmos else IvoryPrimary
                                )
                            }
                            IconButton(
                                onClick = { targetZoom = (targetZoom - 0.15f).coerceIn(0.65f, 1.85f) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Alejar cámara 3D", tint = IvoryPrimary)
                            }
                            Text(
                                text = "${(currentZoom * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedStone
                            )
                            IconButton(
                                onClick = { targetZoom = (targetZoom + 0.15f).coerceIn(0.65f, 1.85f) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Acercar cámara 3D", tint = IvoryPrimary)
                            }
                            IconButton(
                                onClick = {
                                    autoOrbit = false
                                    selectedPreset = StageCameraPreset.DIRECTOR
                                    targetYaw = StageCameraPreset.DIRECTOR.yawDeg
                                    targetPitch = StageCameraPreset.DIRECTOR.pitchDeg
                                    targetZoom = StageCameraPreset.DIRECTOR.zoom
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Restablecer cámara 3D", tint = AmberGold)
                            }
                        }
                    }
                }
            }

            Surface(
                color = if (dockExpanded) AmberContainer else ObsidianBg.copy(alpha = 0.85f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .border(1.dp, if (dockExpanded) AmberGold else StudioBorder, RoundedCornerShape(24.dp))
            ) {
                IconButton(
                    onClick = { dockExpanded = !dockExpanded },
                    modifier = Modifier
                        .height(48.dp)
                        .width(126.dp)
                        .testTag("stage_3d_dock_toggle")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (dockExpanded) Icons.Rounded.Cameraswitch else Icons.Rounded.Tune,
                            contentDescription = "Alternar controles de cámara 3D",
                            tint = AmberGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (dockExpanded) "Ocultar 3D" else "Controles 3D",
                            style = MaterialTheme.typography.labelSmall,
                            color = IvoryPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCinemaStage3D(
    yawDeg: Float,
    pitchDeg: Float,
    zoom: Float,
    focalLengthMm: Float,
    aspectRatio: Float,
    keyLightColor: Color,
    setAccentColor: Color
) {
    val yaw = (yawDeg * PI / 180.0).toFloat()
    val pitch = (pitchDeg * PI / 180.0).toFloat()
    val cosY = cos(yaw)
    val sinY = sin(yaw)
    val cosP = cos(pitch)
    val sinP = sin(pitch)

    val cx = size.width / 2f
    val cy = size.height * 0.56f
    val camDistance = 7.6f / zoom
    val projScale = min(size.width, size.height) * 0.92f

    fun toView(v: Vec3): Vec3 {
        val rx = v.x * cosY - v.z * sinY
        val rz = v.x * sinY + v.z * cosY
        val ry = v.y * cosP - rz * sinP
        val tz = v.y * sinP + rz * cosP + camDistance
        return Vec3(rx, ry, tz)
    }

    fun project(viewPt: Vec3): Offset {
        val z = max(0.35f, viewPt.z)
        val factor = projScale / z
        return Offset(cx + viewPt.x * factor, cy - viewPt.y * factor)
    }

    // 1. Floor Cyclorama Grid & Stage Distance Rings (Y = 0)
    val gridColor = Color.White.copy(alpha = 0.08f)
    for (step in -4..4) {
        val p1 = project(toView(Vec3(step * 0.7f, 0f, -2.8f)))
        val p2 = project(toView(Vec3(step * 0.7f, 0f, 2.8f)))
        drawLine(gridColor, p1, p2, strokeWidth = 1f)

        val h1 = project(toView(Vec3(-2.8f, 0f, step * 0.7f)))
        val h2 = project(toView(Vec3(2.8f, 0f, step * 0.7f)))
        drawLine(gridColor, h1, h2, strokeWidth = 1f)
    }

    listOf(1.1f, 2.2f).forEach { r ->
        val ring = Path()
        for (i in 0..36) {
            val a = (i * 2.0 * PI / 36.0).toFloat()
            val pt = project(toView(Vec3(cos(a) * r, 0.01f, sin(a) * r)))
            if (i == 0) ring.moveTo(pt.x, pt.y) else ring.lineTo(pt.x, pt.y)
        }
        drawPath(ring, color = setAccentColor.copy(alpha = 0.18f), style = Stroke(width = 1.2.dp.toPx()))
    }

    // 2. Build Multi-Part 3D Mesh Faces for Depth-Sorted Painter's Rendering
    val faces = mutableListOf<Face3D>()

    fun addBox(cxB: Float, cyB: Float, czB: Float, w: Float, h: Float, d: Float, baseColor: Color, stroke: Color? = null) {
        val hw = w / 2f
        val hh = h / 2f
        val hd = d / 2f
        val p = arrayOf(
            Vec3(cxB - hw, cyB - hh, czB - hd),
            Vec3(cxB + hw, cyB - hh, czB - hd),
            Vec3(cxB + hw, cyB + hh, czB - hd),
            Vec3(cxB - hw, cyB + hh, czB - hd),
            Vec3(cxB - hw, cyB - hh, czB + hd),
            Vec3(cxB + hw, cyB - hh, czB + hd),
            Vec3(cxB + hw, cyB + hh, czB + hd),
            Vec3(cxB - hw, cyB + hh, czB + hd)
        )
        faces += Face3D(listOf(p[3], p[2], p[6], p[7]), baseColor.copy(alpha = min(1f, baseColor.alpha + 0.08f)), stroke) // Top
        faces += Face3D(listOf(p[0], p[1], p[2], p[3]), baseColor, stroke) // Front
        faces += Face3D(listOf(p[5], p[4], p[7], p[6]), baseColor.copy(alpha = baseColor.alpha * 0.85f), stroke) // Back
        faces += Face3D(listOf(p[1], p[5], p[6], p[2]), baseColor.copy(alpha = baseColor.alpha * 0.9f), stroke) // Right
        faces += Face3D(listOf(p[4], p[0], p[3], p[7]), baseColor.copy(alpha = baseColor.alpha * 0.82f), stroke) // Left
    }

    // Contact Ground Shadow Disc under Subject & Camera Dolly
    addBox(0f, 0.01f, -0.35f, 0.85f, 0.01f, 0.85f, Color.Black.copy(alpha = 0.45f))
    addBox(0f, 0.01f, 1.55f, 0.95f, 0.01f, 1.65f, Color.Black.copy(alpha = 0.42f))

    // Set Backdrop Pillars (tinted from sequence color palette)
    addBox(-1.45f, 0.85f, -1.55f, 0.36f, 1.70f, 0.24f, setAccentColor.copy(alpha = 0.28f), setAccentColor.copy(alpha = 0.65f))
    addBox(1.45f, 1.0f, -1.65f, 0.42f, 2.0f, 0.24f, setAccentColor.copy(alpha = 0.24f), setAccentColor.copy(alpha = 0.60f))

    // Subject / Actor Blocking Silhouette (Legs + Torso + Head at Z = -0.35)
    addBox(0f, 0.35f, -0.35f, 0.24f, 0.70f, 0.18f, Color(0xFF3E4559), IvoryPrimary.copy(alpha = 0.35f))
    addBox(0f, 0.90f, -0.35f, 0.34f, 0.44f, 0.20f, Color(0xFF4D5670), AmberGold.copy(alpha = 0.55f))
    addBox(0f, 1.24f, -0.35f, 0.18f, 0.20f, 0.18f, Color(0xFF7D859B), AmberGold.copy(alpha = 0.75f))

    // Dolly Track Rails (along Z = 0.9 .. 2.35)
    addBox(-0.34f, 0.03f, 1.62f, 0.06f, 0.05f, 1.55f, Color(0xFF757E94), Color.White.copy(alpha = 0.25f))
    addBox(0.34f, 0.03f, 1.62f, 0.06f, 0.05f, 1.55f, Color(0xFF757E94), Color.White.copy(alpha = 0.25f))
    for (tieZ in listOf(0.95f, 1.40f, 1.85f, 2.30f)) {
        addBox(0f, 0.02f, tieZ, 0.76f, 0.03f, 0.06f, Color(0xFF4B5263))
    }

    // Cinema Camera Dolly Cart + Pedestal + ARRI Camera Rig + Lens Barrel at Z = 1.55
    addBox(0f, 0.11f, 1.55f, 0.62f, 0.10f, 0.52f, Color(0xFF2B3140), AmberGold.copy(alpha = 0.5f))
    addBox(0f, 0.48f, 1.55f, 0.12f, 0.66f, 0.12f, Color(0xFF565E73))
    addBox(0f, 0.87f, 1.55f, 0.24f, 0.22f, 0.34f, Color(0xFF1F2430), AmberGold) // Camera Body
    addBox(0f, 1.03f, 1.62f, 0.18f, 0.14f, 0.16f, Color(0xFF2B303E), WarmSilver.copy(alpha = 0.4f)) // Magazine
    addBox(0f, 0.87f, 1.29f, 0.13f, 0.13f, 0.22f, Color(0xFF12161E), TealAtmos) // Lens + Matte Box

    // Key Light Tripod Stand + Barn-door Fixture (Front-Right of Subject)
    addBox(1.55f, 0.55f, 0.65f, 0.06f, 1.10f, 0.06f, Color(0xFF505768))
    addBox(1.55f, 1.18f, 0.65f, 0.22f, 0.22f, 0.18f, keyLightColor.copy(alpha = 0.88f), keyLightColor)

    // Fill Light Scrim Stand (Front-Left)
    addBox(-1.45f, 0.50f, 0.55f, 0.05f, 1.0f, 0.05f, Color(0xFF4A5060))
    addBox(-1.45f, 1.06f, 0.55f, 0.36f, 0.30f, 0.06f, Color(0xFFAEC6DF).copy(alpha = 0.38f), Color.White.copy(alpha = 0.6f))

    // Rim / Backlight Stand (Rear-Right)
    addBox(0.95f, 0.68f, -1.25f, 0.05f, 1.36f, 0.05f, Color(0xFF4A5060))
    addBox(0.95f, 1.42f, -1.25f, 0.18f, 0.18f, 0.14f, TealAtmos.copy(alpha = 0.85f), TealAtmos)

    // Key Light Volumetric Beam Cone toward Subject
    val keySrc = Vec3(1.48f, 1.16f, 0.58f)
    faces += Face3D(
        vertices = listOf(keySrc, Vec3(-0.35f, 0.05f, -0.75f), Vec3(0.35f, 0.05f, -0.05f)),
        fill = keyLightColor.copy(alpha = 0.12f),
        stroke = keyLightColor.copy(alpha = 0.28f),
        strokeWidthPx = 1f
    )

    // Dynamic Camera Optical Frustum Cone driven by focalLengthMm + aspectRatio
    val fovHalfTan = (18f / focalLengthMm.coerceIn(18f, 135f)).coerceIn(0.12f, 0.72f)
    val lensTip = Vec3(0f, 0.87f, 1.18f)
    val frustumDepth = 1.65f
    val halfW = frustumDepth * fovHalfTan
    val halfH = halfW / aspectRatio.coerceIn(1.2f, 2.5f)
    val fTL = Vec3(-halfW, 0.87f + halfH, lensTip.z - frustumDepth)
    val fTR = Vec3(halfW, 0.87f + halfH, lensTip.z - frustumDepth)
    val fBR = Vec3(halfW, 0.87f - halfH, lensTip.z - frustumDepth)
    val fBL = Vec3(-halfW, 0.87f - halfH, lensTip.z - frustumDepth)

    val frustumFill = AmberGold.copy(alpha = 0.10f)
    val frustumEdge = AmberGold.copy(alpha = 0.62f)
    faces += Face3D(listOf(lensTip, fTL, fTR), frustumFill, frustumEdge, 1.4f)
    faces += Face3D(listOf(lensTip, fTR, fBR), frustumFill, frustumEdge, 1.4f)
    faces += Face3D(listOf(lensTip, fBR, fBL), frustumFill, frustumEdge, 1.4f)
    faces += Face3D(listOf(lensTip, fBL, fTL), frustumFill, frustumEdge, 1.4f)
    faces += Face3D(listOf(fTL, fTR, fBR, fBL), AmberGold.copy(alpha = 0.06f), AmberGold.copy(alpha = 0.85f), 1.8f)

    // Transform & Depth Sort Faces Back-to-Front
    val sorted = faces.map { face ->
        val viewVerts = face.vertices.map(::toView)
        val avgZ = viewVerts.map { it.z }.average().toFloat()
        Triple(face, viewVerts.map(::project), avgZ)
    }.sortedByDescending { it.third }

    for ((face, screenPts, _) in sorted) {
        if (screenPts.size < 3) continue
        val path = Path().apply {
            moveTo(screenPts[0].x, screenPts[0].y)
            for (i in 1 until screenPts.size) {
                lineTo(screenPts[i].x, screenPts[i].y)
            }
            close()
        }
        drawPath(path = path, color = face.fill)
        face.stroke?.let { strokeCol ->
            drawPath(
                path = path,
                color = strokeCol,
                style = Stroke(width = face.strokeWidthPx)
            )
        }
    }
}

private fun parseFocalLengthMm(rawLens: String?): Float {
    if (rawLens.isNullOrBlank()) return 35f
    val match = Regex("(\\d{2,3})\\s*mm", RegexOption.IGNORE_CASE).find(rawLens)
        ?: Regex("(\\d{2,3})").find(rawLens)
    val mm = match?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 35f
    return mm.coerceIn(18f, 135f)
}

private fun kelvinToAccentColor(kelvinStr: String): Color {
    val match = Regex("(\\d{4})").find(kelvinStr)
    val k = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 3200
    return when {
        k <= 3200 -> Color(0xFFFFB454) // Warm Tungsten
        k <= 4600 -> Color(0xFFFFDDA0) // Neutral Warm
        k <= 5800 -> Color(0xFFEAF4FF) // Daylight
        else -> Color(0xFF6DD3FF)      // Cool Moonlight / Cyan
    }
}

private fun parsePaletteHex(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    val cleaned = hex.trim().removePrefix("#")
    return try {
        val parsed = cleaned.toLong(16)
        when (cleaned.length) {
            6 -> Color(0xFF000000L or parsed)
            8 -> Color(parsed)
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
