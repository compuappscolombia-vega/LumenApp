package com.example.data.util

import com.example.data.model.LearningCategory
import com.example.data.model.LearningLesson
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.SequenceVersionStatus

object LearningEngine {
    val CATEGORY_LABELS: Map<LearningCategory, String> = mapOf(
        LearningCategory.CAMERA to "Cámara y Óptica",
        LearningCategory.LIGHTING to "Iluminación",
        LearningCategory.SOUND to "Diseño Sonoro",
        LearningCategory.SCREENPLAY to "Guion",
        LearningCategory.PACING to "Ritmo y Montaje",
        LearningCategory.CONTINUITY to "Continuidad"
    )

    fun canExportPdfForSequence(sequence: ProductionSequence?): Boolean =
        sequence != null && sequence.versionStatus == SequenceVersionStatus.FINAL

    fun getFinalSequences(project: ProductionProject?): List<ProductionSequence> =
        project?.sequences?.filter { it.versionStatus == SequenceVersionStatus.FINAL }.orEmpty()

    data class DraftLesson(
        val category: LearningCategory,
        val insight: String,
        val rating: Int,
        val sourceProjectTitle: String
    )

    fun extractAutomaticLessons(
        projectTitle: String,
        sequence: ProductionSequence
    ): List<DraftLesson> {
        val result = mutableListOf<DraftLesson>()
        if (sequence.cameraSystem.isNotBlank() && sequence.lensPackage.isNotBlank()) {
            result.add(
                DraftLesson(
                    category = LearningCategory.CAMERA,
                    insight = "Combinación aprobada en versión final (${sequence.aspectRatio}): ${sequence.cameraSystem} con ${sequence.lensPackage} para estilo ${sequence.directorStyle}."
                        .take(1000),
                    rating = 5,
                    sourceProjectTitle = projectTitle.take(180)
                )
            )
        }
        if (sequence.lighting.colorTemperatureKelvin.isNotBlank()) {
            result.add(
                DraftLesson(
                    category = LearningCategory.LIGHTING,
                    insight = "Esquema cromático aprobado: ${sequence.lighting.colorTemperatureKelvin} · ${sequence.lighting.fillRatio}."
                        .take(1000),
                    rating = 5,
                    sourceProjectTitle = projectTitle.take(180)
                )
            )
        }
        if (sequence.soundDesign.subBassCue.isNotBlank()) {
            result.add(
                DraftLesson(
                    category = LearningCategory.SOUND,
                    insight = "Diseño sonoro aprobado (${sequence.soundDesign.tempoBpm} BPM): ${sequence.soundDesign.subBassCue}."
                        .take(1000),
                    rating = 5,
                    sourceProjectTitle = projectTitle.take(180)
                )
            )
        }
        return result
    }

    fun buildLearningPromptContext(lessons: List<LearningLesson>, limit: Int = 8): String {
        if (lessons.isEmpty()) return "Sin lecciones previas registradas todavía."
        val prioritized = lessons
            .sortedWith(
                compareByDescending<LearningLesson> { it.rating }
                    .thenByDescending { it.createdAtMillis }
            )
            .take(limit)

        return prioritized.mapIndexed { idx, l ->
            val label = CATEGORY_LABELS[l.category] ?: l.category.wireValue
            "${idx + 1}. [$label · Val. ${l.rating}/5 · ${l.sourceProjectTitle}]: ${l.insight}"
        }.joinToString("\n")
    }

    fun validateLessonInput(insight: String, rating: Int): String? {
        if (insight.trim().length < 5) {
            return "Escribe una observación o mejora de al menos 5 caracteres."
        }
        if (rating !in 1..5) {
            return "La valoración debe estar entre 1 y 5."
        }
        return null
    }
}
