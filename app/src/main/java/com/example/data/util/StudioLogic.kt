package com.example.data.util

import com.example.data.model.DirectorStylePreset
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.SourceMaterialType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.round

object StudioLogic {
    val DIRECTOR_STYLES: List<DirectorStylePreset> = listOf(
        DirectorStylePreset(
            id = "epic-imax",
            label = "Épica Monumental IMAX",
            references = "Denis Villeneuve · Ridley Scott",
            defaultCamera = "ARRI Alexa 65 IMAX",
            defaultLenses = "Panavision Sphero 65 & Hasselblad Prime DNA",
            defaultRatio = "1.43:1",
            description = "Escala colosal, arquitectura brutalista o sagrada, planos sostenidos y frecuencias subgraves."
        ),
        DirectorStylePreset(
            id = "neo-noir",
            label = "Thriller Psicológico Neo-Noir",
            references = "David Fincher · Christopher Nolan",
            defaultCamera = "RED V-Raptor XL 8K VV",
            defaultLenses = "Cooke Anamorphic /i SF",
            defaultRatio = "2.39:1",
            description = "Precisión quirúrgica de cámara, iluminación de sodio y sombras densas, montaje tenso."
        ),
        DirectorStylePreset(
            id = "elevated-horror",
            label = "Terror Atmosférico Elevado",
            references = "Robert Eggers · Ari Aster",
            defaultCamera = "ARRI Arricam LT 35mm Film",
            defaultLenses = "Cooke Panchro/i Classic & Baltar",
            defaultRatio = "1.85:1",
            description = "Luz natural o de vela, simetría inquietante, foley visceral y silencios opresivos."
        ),
        DirectorStylePreset(
            id = "scifi-odyssey",
            label = "Ciencia Ficción Visionaria",
            references = "Stanley Kubrick · Alex Garland",
            defaultCamera = "Sony VENICE 2 8.6K",
            defaultLenses = "Zeiss Supreme Prime Radiance",
            defaultRatio = "2.39:1",
            description = "Simetría fría, reflejos anamórficos controlados, diseño sonoro sintético y aislamiento."
        ),
        DirectorStylePreset(
            id = "auteur-oner",
            label = "Drama Autoral en Plano Secuencia",
            references = "Alfonso Cuarón · Emmanuel Lubezki",
            defaultCamera = "ARRI Alexa Mini LF (Steadicam / Trinity)",
            defaultLenses = "Leitz Summilux-C 18mm–35mm",
            defaultRatio = "1.85:1",
            description = "Inmersión continua en tiempo real, luz ambiente orgánica, coreografía milimétrica de actores."
        ),
        DirectorStylePreset(
            id = "classic-adventure",
            label = "Aventura Blockbuster Clásica",
            references = "Steven Spielberg · James Cameron",
            defaultCamera = "Panavision Millennium DXL2",
            defaultLenses = "Panavision C-Series Anamorphic",
            defaultRatio = "2.39:1",
            description = "Contraluces dorados, barridos de Technocrane, ritmo trepidante y partitura sinfónica."
        )
    )

    val ASPECT_RATIO_OPTIONS: List<Pair<String, String>> = listOf(
        "2.39:1" to "2.39:1 Anamórfico Scope",
        "1.43:1" to "1.43:1 IMAX 65mm",
        "1.85:1" to "1.85:1 Flat Cinematográfico",
        "16:9" to "16:9 Widescreen Digital"
    )

    private val RATIO_REGEX = Regex("""^(\d+(?:\.\d+)?)\s*:\s*(\d+(?:\.\d+)?)$""")

    fun parseAspectRatio(ratioStr: String): Float {
        val match = RATIO_REGEX.matchEntire(ratioStr.trim()) ?: return 16f / 9f
        val w = match.groupValues[1].toFloatOrNull() ?: return 16f / 9f
        val h = match.groupValues[2].toFloatOrNull() ?: return 16f / 9f
        return if (w > 0f && h > 0f && w.isFinite() && h.isFinite()) w / h else 16f / 9f
    }

    fun getSourceTypeLabel(type: SourceMaterialType): String = when (type) {
        SourceMaterialType.BOOK -> "Libro / Manuscrito"
        SourceMaterialType.AUDIOBOOK -> "Audiolibro"
        SourceMaterialType.STORY -> "Historia Original"
    }

    fun validateSequenceInput(
        sequenceTitle: String,
        sourceText: String,
        hasFile: Boolean
    ): List<String> {
        val errors = mutableListOf<String>()
        if (sequenceTitle.trim().isEmpty()) {
            errors.add("Indica un título para la secuencia o capítulo.")
        }
        if (!hasFile && sourceText.trim().length < 20) {
            errors.add(
                "Proporciona al menos 20 caracteres del texto fuente o adjunta un archivo (PDF, TXT o audio MP3/WAV/M4A)."
            )
        }
        return errors
    }

    fun filterProjects(projects: List<ProductionProject>, query: String): List<ProductionProject> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return projects
        return projects.filter { p ->
            p.title.lowercase().contains(q) ||
                p.genreStyle.lowercase().contains(q) ||
                p.sequences.any { it.title.lowercase().contains(q) }
        }
    }

    data class ProjectStats(
        val sequenceCount: Int,
        val shotCount: Int,
        val estimatedMinutes: Double
    )

    fun computeProjectStats(project: ProductionProject): ProjectStats {
        val sequenceCount = project.sequences.size
        var shotCount = 0
        var totalSec = 0.0
        for (seq in project.sequences) {
            shotCount += seq.shotList.size
            for (shot in seq.shotList) {
                if (shot.durationSec.isFinite()) {
                    totalSec += shot.durationSec
                }
            }
        }
        val minutes = max(0.0, round((totalSec / 60.0) * 10.0) / 10.0)
        return ProjectStats(sequenceCount, shotCount, minutes)
    }

    fun buildContinuationContext(
        existingBible: String,
        sequenceTitle: String,
        continuitySummary: String
    ): String {
        val base = existingBible.trim()
        val addition = "[${sequenceTitle.trim()}]: ${continuitySummary.trim()}"
        if (continuitySummary.trim().isEmpty()) return base
        if (base.isEmpty()) return addition.take(20000)
        if (base.contains(addition)) return base.take(20000)
        return "$base\n\n$addition".take(20000)
    }

    fun formatScreenplayMarkdown(
        projectTitle: String,
        sequence: ProductionSequence
    ): String {
        val lines = mutableListOf<String>()
        lines.add("# ${projectTitle.uppercase()} — ${sequence.title}")
        lines.add(
            "**Estilo:** ${sequence.directorStyle} · **Formato:** ${sequence.aspectRatio} · **Cámara:** ${sequence.cameraSystem} · **Ópticas:** ${sequence.lensPackage}"
        )
        lines.add("")
        lines.add("> ${sequence.logline}")
        lines.add("")
        lines.add("## 1. Visión de Dirección y Look & Feel")
        lines.add(sequence.directorVision)
        lines.add("- **LUT / Emulsión:** ${sequence.filmStockOrLut}")
        lines.add(
            "- **Paleta:** ${sequence.colorPalette.joinToString(" · ") { "${it.name} (${it.hex})" }}"
        )
        lines.add("")
        lines.add("## 2. Diseño de Iluminación")
        lines.add("- **Luz Principal:** ${sequence.lighting.keyLight}")
        lines.add("- **Ratio de Contraste / Relleno:** ${sequence.lighting.fillRatio}")
        lines.add("- **Contraluz / Rim:** ${sequence.lighting.rimAndBacklight}")
        lines.add("- **Temperatura:** ${sequence.lighting.colorTemperatureKelvin}")
        lines.add("- **Atmósfera:** ${sequence.lighting.atmosphereAndHaze}")
        lines.add("")
        lines.add("## 3. Lista de Planos (Shot List)")
        for (shot in sequence.shotList) {
            lines.add(
                "- **Plano ${shot.shotNumber} [${shot.shotType}]** (${shot.lensMm} · ${shot.movement} · ${shot.durationSec}s): ${shot.framingNote} — *Intención:* ${shot.psychologicalIntent}"
            )
        }
        lines.add("")
        lines.add("## 4. Diseño Sonoro, Foley y Score")
        lines.add("- **Atmósferas Atmos:** ${sequence.soundDesign.atmosLayers.joinToString(" | ")}")
        lines.add("- **Subgraves:** ${sequence.soundDesign.subBassCue}")
        lines.add("- **Foley:** ${sequence.soundDesign.foleyDetails.joinToString(" | ")}")
        lines.add("- **Silencios:** ${sequence.soundDesign.silenceBeats}")
        lines.add(
            "- **Score Musical (${sequence.soundDesign.tempoBpm} BPM):** ${sequence.soundDesign.musicalScore}"
        )
        lines.add("")
        lines.add("## 5. VFX, SFX y Arte")
        lines.add("- **CGI / VFX:** ${sequence.vfx.cgiElements.joinToString(" | ")}")
        lines.add("- **Efectos Prácticos:** ${sequence.vfx.practicalSfx.joinToString(" | ")}")
        lines.add("- **Locaciones y Sets:** ${sequence.vfx.locationsAndSets}")
        lines.add("- **Vestuario y Maquillaje:** ${sequence.vfx.wardrobeAndMakeup}")
        lines.add("")
        sequence.audioAnalysis?.let { aa ->
            lines.add("## 5B. Informe de Dirección y Transcripción del Audio Fuente")
            lines.add("- **Narración:** ${aa.narrationStyle}")
            lines.add("- **Arco Emocional:** ${aa.emotionalArc}")
            lines.add("- **Ritmo:** ${aa.rhythmAndPacing}")
            lines.add("- **Efectos Detectados:** ${aa.detectedSoundEffects.joinToString(" | ")}")
            lines.add("- **Recomendaciones de Dirección:** ${aa.directingRecommendations}")
            lines.add("")
            lines.add("### Transcripción Verbatim")
            lines.add(aa.transcription)
            lines.add("")
        }
        lines.add("## 6. Guion Cinematográfico")
        for (scene in sequence.screenplay) {
            lines.add("")
            lines.add("### ESCENA ${scene.sceneNumber}: ${scene.slugline}")
            for (beat in scene.beats) {
                when (beat.type.lowercase()) {
                    "dialogue" -> {
                        lines.add("**${(beat.character ?: "PERSONAJE").uppercase()}**")
                        if (!beat.parenthetical.isNullOrBlank()) {
                            lines.add("*(${beat.parenthetical})*")
                        }
                        lines.add(beat.text)
                        lines.add("")
                    }
                    "camera" -> {
                        lines.add("**[CÁMARA]** ${beat.text}")
                        lines.add("")
                    }
                    "sound" -> {
                        lines.add("**[SONIDO]** ${beat.text}")
                        lines.add("")
                    }
                    else -> {
                        lines.add(beat.text)
                        lines.add("")
                    }
                }
            }
        }
        return lines.joinToString("\n")
    }

    fun formatFountainScreenplay(
        projectTitle: String,
        sequence: ProductionSequence
    ): String {
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-ES"))
            .format(Date(if (sequence.createdAtMillis > 0L) sequence.createdAtMillis else System.currentTimeMillis()))
        val lines = mutableListOf<String>()
        lines.add("Title: $projectTitle")
        lines.add("Credit: Adaptación cinematográfica por LumenReel Studio")
        lines.add("Draft date: $dateStr")
        lines.add("Notes: ${sequence.title} (${sequence.aspectRatio})")
        lines.add("")
        lines.add("= ${sequence.logline}")
        lines.add("")
        for (scene in sequence.screenplay) {
            lines.add(scene.slugline.uppercase())
            lines.add("")
            for (beat in scene.beats) {
                when (beat.type.lowercase()) {
                    "dialogue" -> {
                        lines.add((beat.character ?: "PERSONAJE").uppercase())
                        if (!beat.parenthetical.isNullOrBlank()) {
                            lines.add("(${beat.parenthetical})")
                        }
                        lines.add(beat.text)
                        lines.add("")
                    }
                    "camera" -> {
                        lines.add("[[CÁMARA: ${beat.text}]]")
                        lines.add("")
                    }
                    "sound" -> {
                        lines.add("[[SONIDO: ${beat.text}]]")
                        lines.add("")
                    }
                    else -> {
                        lines.add(beat.text)
                        lines.add("")
                    }
                }
            }
        }
        return lines.joinToString("\n")
    }
}
