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

data class ProjectSequenceBarItem(
    val projectId: String,
    val projectTitle: String,
    val draftCount: Int,
    val finalCount: Int,
    val totalCount: Int
)

data class SequenceScriptMetric(
    val sequenceId: String,
    val sequenceTitle: String,
    val projectTitle: String,
    val scenesCount: Int,
    val beatsCount: Int,
    val wordsCount: Int
)

data class ProjectActivityPoint(
    val dayKeyMillis: Long,
    val label: String,
    val createdCount: Int,
    val updatedCount: Int,
    val totalEvents: Int
)

data class StudioMetricsReport(
    val totalProjects: Int,
    val totalSequences: Int,
    val draftSequences: Int,
    val finalSequences: Int,
    val totalScenes: Int,
    val totalBeats: Int,
    val totalScriptWords: Int,
    val averageWordsPerSequence: Int,
    val estimatedMinutes: Int,
    val projectBars: List<ProjectSequenceBarItem>,
    val scriptLengthBySequence: List<SequenceScriptMetric>,
    val activityTimeline: List<ProjectActivityPoint>
)

object StudioMetricsCalculator {
    private const val ONE_DAY_MS = 86_400_000L

    fun countWords(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split(Regex("\\s+")).count { it.isNotBlank() }
    }

    fun computeMetrics(projects: List<ProductionProject>): StudioMetricsReport {
        val allSequences = projects.flatMap { proj ->
            proj.sequences.map { seq -> proj.title to seq }
        }

        val finalSequences = allSequences.count { (_, seq) ->
            seq.versionStatus == SequenceVersionStatus.FINAL
        }
        val draftSequences = allSequences.count { (_, seq) ->
            seq.versionStatus == SequenceVersionStatus.DRAFT
        }
        val reportedTotalSequences = maxOf(
            allSequences.size,
            projects.sumOf { maxOf(it.sequencesCount, it.sequences.size) }
        )

        val scriptMetrics = allSequences.map { (projectTitle, seq) ->
            val scenes = seq.screenplay
            val beats = scenes.sumOf { it.beats.size }
            val words = scenes.sumOf { sc ->
                countWords(sc.slugline) + sc.beats.sumOf { beat ->
                    countWords(beat.character.orEmpty()) +
                        countWords(beat.parenthetical.orEmpty()) +
                        countWords(beat.text)
                }
            }
            SequenceScriptMetric(
                sequenceId = seq.id,
                sequenceTitle = seq.title.ifBlank { "Secuencia sin título" },
                projectTitle = projectTitle,
                scenesCount = scenes.size,
                beatsCount = beats,
                wordsCount = words
            )
        }.sortedByDescending { it.wordsCount.coerceAtLeast(it.beatsCount * 10) }

        val totalScenes = scriptMetrics.sumOf { it.scenesCount }
        val totalBeats = scriptMetrics.sumOf { it.beatsCount }
        val totalWords = scriptMetrics.sumOf { it.wordsCount }
        val avgWords = if (scriptMetrics.isNotEmpty()) totalWords / scriptMetrics.size else 0
        val estimatedMinutes = allSequences.sumOf { (_, seq) ->
            val sec = seq.shotList.sumOf { it.durationSec }
            if (sec > 0.0) kotlin.math.ceil(sec / 60.0).toInt() else seq.screenplay.size
        }

        val projectBars = projects.map { proj ->
            val finals = proj.sequences.count { it.versionStatus == SequenceVersionStatus.FINAL }
            val drafts = proj.sequences.count { it.versionStatus == SequenceVersionStatus.DRAFT }
            val total = maxOf(proj.sequences.size, proj.sequencesCount)
            val normalizedDrafts = if (total > finals + drafts) total - finals else drafts
            ProjectSequenceBarItem(
                projectId = proj.id,
                projectTitle = proj.title.ifBlank { "Sin título" },
                draftCount = normalizedDrafts.coerceAtLeast(0),
                finalCount = finals.coerceAtLeast(0),
                totalCount = total.coerceAtLeast(finals + normalizedDrafts)
            )
        }.sortedByDescending { it.totalCount }

        val dayBuckets = linkedMapOf<Long, Pair<Int, Int>>()
        fun bucketKey(millis: Long): Long = if (millis > 0L) (millis / ONE_DAY_MS) * ONE_DAY_MS else 0L

        projects.forEach { proj ->
            val cKey = bucketKey(proj.createdAtMillis)
            if (cKey > 0L) {
                val cur = dayBuckets[cKey] ?: (0 to 0)
                dayBuckets[cKey] = (cur.first + 1) to cur.second
            }
            val uKey = bucketKey(proj.updatedAtMillis)
            if (uKey > 0L && uKey != cKey) {
                val cur = dayBuckets[uKey] ?: (0 to 0)
                dayBuckets[uKey] = cur.first to (cur.second + 1)
            }
            proj.sequences.forEach { seq ->
                val scKey = bucketKey(seq.createdAtMillis)
                if (scKey > 0L) {
                    val cur = dayBuckets[scKey] ?: (0 to 0)
                    dayBuckets[scKey] = (cur.first + 1) to cur.second
                }
                val suKey = bucketKey(seq.updatedAtMillis)
                if (suKey > 0L && suKey != scKey) {
                    val cur = dayBuckets[suKey] ?: (0 to 0)
                    dayBuckets[suKey] = cur.first to (cur.second + 1)
                }
            }
        }

        val formatter = java.text.SimpleDateFormat("dd MMM", java.util.Locale.forLanguageTag("es-ES"))
        val activityTimeline = dayBuckets.entries
            .sortedBy { it.key }
            .takeLast(10)
            .map { (dayMillis, counts) ->
                ProjectActivityPoint(
                    dayKeyMillis = dayMillis,
                    label = formatter.format(java.util.Date(dayMillis)),
                    createdCount = counts.first,
                    updatedCount = counts.second,
                    totalEvents = counts.first + counts.second
                )
            }

        return StudioMetricsReport(
            totalProjects = projects.size,
            totalSequences = reportedTotalSequences,
            draftSequences = draftSequences,
            finalSequences = finalSequences,
            totalScenes = totalScenes,
            totalBeats = totalBeats,
            totalScriptWords = totalWords,
            averageWordsPerSequence = avgWords,
            estimatedMinutes = estimatedMinutes,
            projectBars = projectBars,
            scriptLengthBySequence = scriptMetrics,
            activityTimeline = activityTimeline
        )
    }
}

