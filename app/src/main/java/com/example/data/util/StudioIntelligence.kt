package com.example.data.util

import com.example.data.model.DirectorialEnhancement
import com.example.data.model.LearningCategory
import com.example.data.model.LearningLesson
import com.example.data.model.PatternMetric
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.SequenceVersionStatus
import com.example.data.model.StudioIntelligenceReport
import kotlin.math.min
import kotlin.math.roundToInt

object StudioIntelligence {
    private data class PatternAccumulator(
        val detail: String,
        val ratings: MutableList<Int> = mutableListOf(),
        val projects: MutableSet<String> = linkedSetOf()
    )

    private fun pushPattern(
        map: MutableMap<String, PatternAccumulator>,
        label: String,
        detail: String,
        rating: Int,
        projectTitle: String
    ) {
        val key = label.trim()
        if (key.isEmpty()) return
        val existing = map[key]
        if (existing != null) {
            existing.ratings.add(rating)
            existing.projects.add(projectTitle)
        } else {
            map[key] = PatternAccumulator(
                detail = detail,
                ratings = mutableListOf(rating),
                projects = linkedSetOf(projectTitle)
            )
        }
    }

    private fun finalizePatterns(
        map: Map<String, PatternAccumulator>,
        limit: Int = 3
    ): List<PatternMetric> {
        return map.entries.map { (label, entry) ->
            val avg = entry.ratings.sum().toDouble() / entry.ratings.size.coerceAtLeast(1)
            PatternMetric(
                label = label,
                detail = entry.detail,
                occurrences = entry.ratings.size,
                avgRating = (avg * 10.0).roundToInt() / 10.0,
                sourceProjects = entry.projects.toList()
            )
        }.sortedWith(
            compareByDescending<PatternMetric> { it.avgRating }
                .thenByDescending { it.occurrences }
        ).take(limit)
    }

    fun buildStudioIntelligence(
        projects: List<ProductionProject>,
        lessons: List<LearningLesson>,
        activeProject: ProductionProject?,
        activeSequence: ProductionSequence
    ): StudioIntelligenceReport {
        val cameraMap = linkedMapOf<String, PatternAccumulator>()
        val lightingMap = linkedMapOf<String, PatternAccumulator>()
        val soundMap = linkedMapOf<String, PatternAccumulator>()
        var finalizedSequencesCount = 0

        for (proj in projects) {
            for (seq in proj.sequences) {
                val seqScore = seq.sequenceRating
                    ?: if (seq.versionStatus == SequenceVersionStatus.FINAL) 5 else 3
                if (seq.versionStatus == SequenceVersionStatus.FINAL) {
                    finalizedSequencesCount += 1
                }
                if (seq.versionStatus == SequenceVersionStatus.FINAL || (seq.sequenceRating ?: 0) >= 4) {
                    pushPattern(
                        cameraMap,
                        seq.cameraSystem,
                        "${seq.lensPackage} · ${seq.aspectRatio}",
                        seqScore,
                        proj.title
                    )
                    if (seq.lighting.colorTemperatureKelvin.isNotBlank()) {
                        pushPattern(
                            lightingMap,
                            seq.lighting.colorTemperatureKelvin,
                            "${seq.lighting.fillRatio} · ${seq.lighting.keyLight}",
                            seqScore,
                            proj.title
                        )
                    }
                    if (seq.soundDesign.subBassCue.isNotBlank()) {
                        pushPattern(
                            soundMap,
                            "${seq.soundDesign.tempoBpm} BPM · LFE",
                            seq.soundDesign.subBassCue,
                            seqScore,
                            proj.title
                        )
                    }
                }
            }
        }

        for (lesson in lessons) {
            if (lesson.rating < 4) continue
            when (lesson.category) {
                LearningCategory.CAMERA, LearningCategory.PACING -> pushPattern(
                    cameraMap,
                    lesson.insight.take(68),
                    lesson.insight,
                    lesson.rating,
                    lesson.sourceProjectTitle
                )
                LearningCategory.LIGHTING -> pushPattern(
                    lightingMap,
                    lesson.insight.take(68),
                    lesson.insight,
                    lesson.rating,
                    lesson.sourceProjectTitle
                )
                LearningCategory.SOUND -> pushPattern(
                    soundMap,
                    lesson.insight.take(68),
                    lesson.insight,
                    lesson.rating,
                    lesson.sourceProjectTitle
                )
                else -> Unit
            }
        }

        val topCameraPatterns = finalizePatterns(cameraMap, 3)
        val topLightingSetups = finalizePatterns(lightingMap, 3)
        val topSoundscapes = finalizePatterns(soundMap, 3)

        val enhancements = mutableListOf<DirectorialEnhancement>()
        val activeTitle = activeProject?.title.orEmpty()

        val externalCam = lessons.firstOrNull {
            (it.category == LearningCategory.CAMERA || it.category == LearningCategory.PACING) &&
                it.rating >= 4
        }
        val bestCamPattern = topCameraPatterns.firstOrNull { !it.sourceProjects.contains(activeTitle) }
            ?: topCameraPatterns.firstOrNull()

        if (externalCam != null) {
            enhancements.add(
                DirectorialEnhancement(
                    id = "enh-cam-${externalCam.id}",
                    category = "camera",
                    title = "Refinamiento de Óptica y Cadencia de Cámara",
                    recommendation = externalCam.insight,
                    rationale = "Validado con ${externalCam.rating}/5 ★ en \"${externalCam.sourceProjectTitle}\".",
                    sourceProjectTitle = externalCam.sourceProjectTitle,
                    confidenceScore = min(98, 70 + externalCam.rating * 5),
                    continuitySnippet = "[Mejora de Cámara · Studio Intelligence]: ${externalCam.insight}"
                )
            )
        } else if (bestCamPattern != null) {
            enhancements.add(
                DirectorialEnhancement(
                    id = "enh-cam-pattern",
                    category = "camera",
                    title = "Adoptar referencia óptica (${bestCamPattern.label})",
                    recommendation = "Combinar ${activeSequence.cameraSystem} con ${bestCamPattern.detail} para reforzar el peso visual de la escena.",
                    rationale = "Promedio ${bestCamPattern.avgRating}/5 ★ en ${bestCamPattern.occurrences} producciones aprobadas.",
                    sourceProjectTitle = bestCamPattern.sourceProjects.firstOrNull() ?: "Archivo del Estudio",
                    confidenceScore = min(96, (bestCamPattern.avgRating * 18.0).roundToInt()),
                    continuitySnippet = "[Mejora de Cámara · Studio Intelligence]: Referencia ${bestCamPattern.label} (${bestCamPattern.detail})."
                )
            )
        }

        val externalLight = lessons.firstOrNull {
            it.category == LearningCategory.LIGHTING && it.rating >= 4
        }
        val bestLightPattern = topLightingSetups.firstOrNull { !it.sourceProjects.contains(activeTitle) }
            ?: topLightingSetups.firstOrNull()

        if (externalLight != null) {
            enhancements.add(
                DirectorialEnhancement(
                    id = "enh-light-${externalLight.id}",
                    category = "lighting",
                    title = "Contraste Térmico e Iluminación Motivada",
                    recommendation = externalLight.insight,
                    rationale = "Validado con ${externalLight.rating}/5 ★ en \"${externalLight.sourceProjectTitle}\".",
                    sourceProjectTitle = externalLight.sourceProjectTitle,
                    confidenceScore = min(97, 68 + externalLight.rating * 5),
                    continuitySnippet = "[Mejora de Iluminación · Studio Intelligence]: ${externalLight.insight}"
                )
            )
        } else if (bestLightPattern != null) {
            enhancements.add(
                DirectorialEnhancement(
                    id = "enh-light-pattern",
                    category = "lighting",
                    title = "Contraste probado (${bestLightPattern.label})",
                    recommendation = "Aplicar separación térmica (${bestLightPattern.detail}) frente a la luz principal actual (${activeSequence.lighting.colorTemperatureKelvin}).",
                    rationale = "Promedio ${bestLightPattern.avgRating}/5 ★ en \"${bestLightPattern.sourceProjects.firstOrNull() ?: "Archivo del Estudio"}\".",
                    sourceProjectTitle = bestLightPattern.sourceProjects.firstOrNull() ?: "Archivo del Estudio",
                    confidenceScore = min(95, (bestLightPattern.avgRating * 18.0).roundToInt()),
                    continuitySnippet = "[Mejora de Iluminación · Studio Intelligence]: ${bestLightPattern.label} — ${bestLightPattern.detail}."
                )
            )
        }

        val externalSound = lessons.firstOrNull {
            it.category == LearningCategory.SOUND && it.rating >= 4
        }
        val bestSoundPattern = topSoundscapes.firstOrNull { !it.sourceProjects.contains(activeTitle) }
            ?: topSoundscapes.firstOrNull()

        if (externalSound != null) {
            enhancements.add(
                DirectorialEnhancement(
                    id = "enh-sound-${externalSound.id}",
                    category = "sound",
                    title = "Arquitectura Atmos y Subgraves Dramáticos",
                    recommendation = externalSound.insight,
                    rationale = "Validado con ${externalSound.rating}/5 ★ en \"${externalSound.sourceProjectTitle}\".",
                    sourceProjectTitle = externalSound.sourceProjectTitle,
                    confidenceScore = min(98, 70 + externalSound.rating * 5),
                    continuitySnippet = "[Mejora Sonora · Studio Intelligence]: ${externalSound.insight}"
                )
            )
        } else if (bestSoundPattern != null) {
            enhancements.add(
                DirectorialEnhancement(
                    id = "enh-sound-pattern",
                    category = "sound",
                    title = "Diseño Subgrave y Silencio (${bestSoundPattern.label})",
                    recommendation = "Integrar textura LFE de referencia (${bestSoundPattern.detail}) manteniendo ${activeSequence.soundDesign.tempoBpm} BPM.",
                    rationale = "Promedio ${bestSoundPattern.avgRating}/5 ★ en \"${bestSoundPattern.sourceProjects.firstOrNull() ?: "Archivo del Estudio"}\".",
                    sourceProjectTitle = bestSoundPattern.sourceProjects.firstOrNull() ?: "Archivo del Estudio",
                    confidenceScore = min(94, (bestSoundPattern.avgRating * 18.0).roundToInt()),
                    continuitySnippet = "[Mejora Sonora · Studio Intelligence]: ${bestSoundPattern.label} (${bestSoundPattern.detail})."
                )
            )
        }

        return StudioIntelligenceReport(
            totalTrackedOutcomes = finalizedSequencesCount + lessons.count { it.rating >= 4 },
            finalizedSequencesCount = finalizedSequencesCount,
            topCameraPatterns = topCameraPatterns,
            topLightingSetups = topLightingSetups,
            topSoundscapes = topSoundscapes,
            enhancements = enhancements
        )
    }

    fun buildRatedChoiceLessons(
        projectTitle: String,
        sequence: ProductionSequence,
        rating: Int,
        note: String
    ): List<LearningEngine.DraftLesson> {
        val safeRating = rating.coerceIn(1, 5)
        val noteSuffix = if (note.trim().isNotEmpty()) " Nota: ${note.trim()}" else ""
        val out = mutableListOf(
            LearningEngine.DraftLesson(
                category = LearningCategory.CAMERA,
                insight = "${sequence.cameraSystem} + ${sequence.lensPackage} (${sequence.aspectRatio}).$noteSuffix".take(1000),
                rating = safeRating,
                sourceProjectTitle = projectTitle.take(180)
            )
        )
        if (sequence.lighting.colorTemperatureKelvin.isNotBlank()) {
            out.add(
                LearningEngine.DraftLesson(
                    category = LearningCategory.LIGHTING,
                    insight = "${sequence.lighting.colorTemperatureKelvin} · ${sequence.lighting.fillRatio}.$noteSuffix".take(1000),
                    rating = safeRating,
                    sourceProjectTitle = projectTitle.take(180)
                )
            )
        }
        if (sequence.soundDesign.subBassCue.isNotBlank()) {
            out.add(
                LearningEngine.DraftLesson(
                    category = LearningCategory.SOUND,
                    insight = "${sequence.soundDesign.tempoBpm} BPM · ${sequence.soundDesign.subBassCue}.$noteSuffix".take(1000),
                    rating = safeRating,
                    sourceProjectTitle = projectTitle.take(180)
                )
            )
        }
        return out
    }
}
