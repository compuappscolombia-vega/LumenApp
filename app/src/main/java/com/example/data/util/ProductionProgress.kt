package com.example.data.util

import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.SequenceVersionStatus
import kotlin.math.min
import kotlin.math.roundToInt

enum class ProductionAxisKey {
    SCREENPLAY,
    LIGHTING,
    VFX,
    SOUND
}

data class ProductionRadarAxis(
    val key: ProductionAxisKey,
    val axis: String,
    val projectScore: Int,
    val sequenceScore: Int,
    val detailSummary: String
)

data class ProductionRadarReport(
    val axes: List<ProductionRadarAxis>,
    val overallProjectScore: Int,
    val sequencesCount: Int,
    val finalCount: Int,
    val hasContent: Boolean
)

object ProductionProgress {
    private fun clampScore(value: Double): Int {
        if (!value.isFinite()) return 0
        return value.roundToInt().coerceIn(0, 100)
    }

    private fun scoreNonEmpty(text: String?, minRichLength: Int = 18): Double {
        val clean = (text ?: "").trim()
        if (clean.isEmpty()) return 0.0
        return if (clean.length >= minRichLength) 1.0 else 0.6
    }

    private data class ScoredSequence(
        val id: String,
        val screenplay: Int,
        val lighting: Int,
        val vfx: Int,
        val sound: Int,
        val scenes: Int,
        val beats: Int,
        val paletteCount: Int,
        val vfxItems: Int,
        val renderedFrames: Int,
        val soundItems: Int
    )

    private fun scoreSequenceAxes(seq: ProductionSequence): ScoredSequence {
        val scenes = seq.screenplay
        val totalBeats = scenes.sumOf { it.beats.size }
        val dialogueBeats = scenes.sumOf { sc -> sc.beats.count { it.type == "dialogue" } }
        val technicalBeats = scenes.sumOf { sc -> sc.beats.count { it.type == "camera" || it.type == "sound" } }

        val screenplayRaw =
            min(35.0, scenes.size * 14.0) +
                min(35.0, totalBeats * 3.5) +
                min(15.0, dialogueBeats * 5.0) +
                min(15.0, technicalBeats * 5.0)

        val lightingFields = listOf(
            scoreNonEmpty(seq.lighting.keyLight, 15),
            scoreNonEmpty(seq.lighting.fillRatio, 8),
            scoreNonEmpty(seq.lighting.rimAndBacklight, 12),
            scoreNonEmpty(seq.lighting.colorTemperatureKelvin, 4),
            scoreNonEmpty(seq.lighting.atmosphereAndHaze, 12)
        )
        val lightingFieldScore = (lightingFields.sum() / 5.0) * 60.0
        val paletteScore = min(25.0, seq.colorPalette.size * 5.0)
        val lutScore = scoreNonEmpty(seq.filmStockOrLut, 6) * 15.0
        val lightingRaw = lightingFieldScore + paletteScore + lutScore

        val cgiCount = seq.vfx.cgiElements.size
        val sfxCount = seq.vfx.practicalSfx.size
        val frames = seq.storyboardFrames
        val promptFrames = frames.count { it.visualPrompt.trim().length > 10 }
        val renderedFrames = frames.count { !it.localImagePath.isNullOrBlank() }

        val vfxRaw =
            min(24.0, cgiCount * 8.0) +
                min(20.0, sfxCount * 7.0) +
                scoreNonEmpty(seq.vfx.locationsAndSets, 15) * 14.0 +
                scoreNonEmpty(seq.vfx.wardrobeAndMakeup, 15) * 12.0 +
                min(15.0, promptFrames * 5.0) +
                min(15.0, renderedFrames * 7.5)

        val atmosCount = seq.soundDesign.atmosLayers.size
        val foleyCount = seq.soundDesign.foleyDetails.size
        val audioBonus = if (seq.audioAnalysis != null) {
            10.0 + min(5.0, seq.audioAnalysis.detectedSoundEffects.size * 2.0)
        } else {
            0.0
        }
        val soundRaw =
            min(25.0, atmosCount * 8.5) +
                min(20.0, foleyCount * 7.0) +
                scoreNonEmpty(seq.soundDesign.subBassCue, 10) * 15.0 +
                scoreNonEmpty(seq.soundDesign.silenceBeats, 10) * 12.0 +
                scoreNonEmpty(seq.soundDesign.musicalScore, 10) * 13.0 +
                (if (seq.soundDesign.tempoBpm > 0) 5.0 else 0.0) +
                audioBonus

        return ScoredSequence(
            id = seq.id,
            screenplay = clampScore(screenplayRaw),
            lighting = clampScore(lightingRaw),
            vfx = clampScore(vfxRaw),
            sound = clampScore(soundRaw),
            scenes = scenes.size,
            beats = totalBeats,
            paletteCount = seq.colorPalette.size,
            vfxItems = cgiCount + sfxCount,
            renderedFrames = renderedFrames,
            soundItems = atmosCount + foleyCount
        )
    }

    fun computeProductionRadarMetrics(
        project: ProductionProject?,
        activeSequence: ProductionSequence? = null
    ): ProductionRadarReport {
        if (project == null || project.sequences.isEmpty()) {
            return ProductionRadarReport(
                axes = listOf(
                    ProductionRadarAxis(ProductionAxisKey.SCREENPLAY, "Guion", 0, 0, "0 escenas · 0 beats"),
                    ProductionRadarAxis(ProductionAxisKey.LIGHTING, "Diseño de iluminación", 0, 0, "Sin esquema Kelvin ni paleta"),
                    ProductionRadarAxis(ProductionAxisKey.VFX, "Efectos visuales", 0, 0, "0 VFX/SFX · 0 frames"),
                    ProductionRadarAxis(ProductionAxisKey.SOUND, "Diseño sonoro", 0, 0, "0 capas Atmos/Foley")
                ),
                overallProjectScore = 0,
                sequencesCount = 0,
                finalCount = 0,
                hasContent = false
            )
        }

        val scored = project.sequences.map(::scoreSequenceAxes)
        val count = scored.size.toDouble()
        val targetSeqScore = activeSequence?.let { seq -> scored.firstOrNull { it.id == seq.id } } ?: scored.first()

        val totalScenes = scored.sumOf { it.scenes }
        val totalBeats = scored.sumOf { it.beats }
        val totalVfx = scored.sumOf { it.vfxItems }
        val totalFrames = scored.sumOf { it.renderedFrames }
        val totalSound = scored.sumOf { it.soundItems }
        val avgPalette = (scored.sumOf { it.paletteCount } / count).roundToInt()
        val bibleBonus = if (project.continuityBible.trim().length >= 40) 4.0 else 0.0

        val projectScreenplay = clampScore((scored.sumOf { it.screenplay } / count) + bibleBonus)
        val projectLighting = clampScore(scored.sumOf { it.lighting } / count)
        val projectVfx = clampScore(scored.sumOf { it.vfx } / count)
        val projectSound = clampScore(scored.sumOf { it.sound } / count)

        val axes = listOf(
            ProductionRadarAxis(
                key = ProductionAxisKey.SCREENPLAY,
                axis = "Guion",
                projectScore = projectScreenplay,
                sequenceScore = targetSeqScore.screenplay,
                detailSummary = "$totalScenes ${if (totalScenes == 1) "escena" else "escenas"} · $totalBeats beats"
            ),
            ProductionRadarAxis(
                key = ProductionAxisKey.LIGHTING,
                axis = "Diseño de iluminación",
                projectScore = projectLighting,
                sequenceScore = targetSeqScore.lighting,
                detailSummary = "$avgPalette tonos · esquema Kelvin y contraste"
            ),
            ProductionRadarAxis(
                key = ProductionAxisKey.VFX,
                axis = "Efectos visuales",
                projectScore = projectVfx,
                sequenceScore = targetSeqScore.vfx,
                detailSummary = "$totalVfx CGI/SFX · $totalFrames visuales generados"
            ),
            ProductionRadarAxis(
                key = ProductionAxisKey.SOUND,
                axis = "Diseño sonoro",
                projectScore = projectSound,
                sequenceScore = targetSeqScore.sound,
                detailSummary = "$totalSound capas Atmos/Foley · score"
            )
        )

        val overall = clampScore((projectScreenplay + projectLighting + projectVfx + projectSound) / 4.0)
        return ProductionRadarReport(
            axes = axes,
            overallProjectScore = overall,
            sequencesCount = scored.size,
            finalCount = project.sequences.count { it.versionStatus == SequenceVersionStatus.FINAL },
            hasContent = true
        )
    }
}
