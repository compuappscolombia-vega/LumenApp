package com.example

import com.example.data.model.AgentRoleFocus
import com.example.data.model.LearningCategory
import com.example.data.model.LearningLesson
import com.example.data.model.LightingSetup
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.ScreenplayBeat
import com.example.data.model.ScreenplayScene
import com.example.data.model.SequenceVersionStatus
import com.example.data.model.ShotSpec
import com.example.data.model.SoundDesignSpec
import com.example.data.model.SourceMaterialType
import com.example.data.util.AgentLogic
import com.example.data.util.LearningEngine
import com.example.data.util.ProductionProgress
import com.example.data.util.StudioIntelligence
import com.example.data.util.StudioLogic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioDomainLogicTest {

    private fun sampleProject(): ProductionProject {
        val seq = ProductionSequence(
            id = "seq_1",
            projectId = "proj_1",
            ownerId = "user_1",
            title = "Prólogo Nocturno",
            versionStatus = SequenceVersionStatus.DRAFT,
            directorStyle = "Thriller Psicológico Neo-Noir",
            aspectRatio = "2.39:1",
            cameraSystem = "RED V-Raptor XL 8K VV",
            lensPackage = "Cooke Anamorphic /i SF",
            filmStockOrLut = "Kodak 2383 Print LUT",
            logline = "Una señal imposible interrumpe la guardia nocturna.",
            directorVision = "Tensión contenida en penumbra azul y sodio.",
            lighting = LightingSetup(
                keyLight = "Tungsteno rasante 45°",
                fillRatio = "6:1",
                colorTemperatureKelvin = "3200K / 5600K"
            ),
            shotList = listOf(
                ShotSpec("01", "PG", "35mm", "Dolly in", 6.0, "Faro en tormenta", "Aislamiento")
            ),
            soundDesign = SoundDesignSpec(
                atmosLayers = listOf("Oleaje grave"),
                subBassCue = "35 Hz continuo",
                tempoBpm = 70
            ),
            screenplay = listOf(
                ScreenplayScene(
                    sceneNumber = "1",
                    slugline = "INT. FARO - NOCHE",
                    timeOfDay = "NOCHE",
                    beats = listOf(
                        ScreenplayBeat(type = "action", text = "El haz gira sobre el océano negro."),
                        ScreenplayBeat(type = "dialogue", character = "ELENA", text = "¿Quién transmite en esta banda?")
                    )
                )
            )
        )
        return ProductionProject(
            id = "proj_1",
            ownerId = "user_1",
            title = "El Faro de Sal",
            genreStyle = "Thriller Psicológico Neo-Noir",
            continuityBible = "Regla 1: Iluminación contrastada.",
            sequences = listOf(seq)
        )
    }

    @Test
    fun `validateSequenceInput rejects blank title or short text without file`() {
        val missingTitle = StudioLogic.validateSequenceInput("", "Texto suficientemente largo para pasar.", false)
        assertTrue(missingTitle.any { it.contains("título", ignoreCase = true) })

        val shortText = StudioLogic.validateSequenceInput("Cap 1", "Muy corto", false)
        assertTrue(shortText.any { it.contains("20 caracteres", ignoreCase = true) })

        val okWithFile = StudioLogic.validateSequenceInput("Cap 1", "", true)
        assertTrue(okWithFile.isEmpty())
    }

    @Test
    fun `isSupportedSourceFile accepts pdf txt md fountain and audio even with octet-stream mime`() {
        assertTrue(StudioLogic.isSupportedSourceFile("guion.fountain", "application/octet-stream"))
        assertTrue(StudioLogic.isSupportedSourceFile("notas.md", "application/octet-stream"))
        assertTrue(StudioLogic.isSupportedSourceFile("capitulo.txt", "text/plain"))
        assertTrue(StudioLogic.isSupportedSourceFile("libro.pdf", "application/pdf"))
        assertTrue(StudioLogic.isSupportedSourceFile("voz.m4a", "audio/mp4"))
        assertFalse(StudioLogic.isSupportedSourceFile("foto.png", "image/png"))
    }

    @Test
    fun `buildManualDraftSequence populates editable screenplay scene and camera specs`() {
        val proj = sampleProject().copy(sequences = emptyList())
        val seq = StudioLogic.buildManualDraftSequence(
            userId = "guest",
            project = proj,
            sequenceTitle = "Acto I: Tormenta",
            sourceType = SourceMaterialType.STORY,
            directorStyle = "Épica Monumental IMAX",
            aspectRatio = "1.43:1",
            sourceText = "Primer párrafo de acción.\n\nSegundo párrafo de suspenso."
        )
        assertEquals("Acto I: Tormenta", seq.title)
        assertEquals(SequenceVersionStatus.DRAFT, seq.versionStatus)
        assertEquals("1.43:1", seq.aspectRatio)
        assertEquals(1, seq.screenplay.size)
        assertEquals(2, seq.screenplay.first().beats.size)
        assertTrue(seq.shotList.isNotEmpty())
        assertTrue(seq.storyboardFrames.isNotEmpty())
    }

    @Test
    fun `agent tools mutate bible, vision, vfx, and screenplay safely`() {
        val proj = sampleProject()

        val bibleOutcome = AgentLogic.applyAgentToolCall(
            project = proj,
            activeSequenceId = "seq_1",
            toolName = "actualizar_biblia_continuidad",
            args = mapOf("reglasContinuidad" to "Regla 2: Lentes anamórficos en interiores.", "modo" to "append")
        )
        assertTrue(bibleOutcome.bibleChanged)
        assertTrue(bibleOutcome.updatedProject.continuityBible.contains("Regla 2"))

        val directorOutcome = AgentLogic.applyAgentToolCall(
            project = proj,
            activeSequenceId = "seq_1",
            toolName = "ajustar_direccion_y_camara",
            args = mapOf("cameraSystem" to "ARRI Alexa 65", "colorTemperatureKelvin" to "4000K neutro")
        )
        assertEquals("ARRI Alexa 65", directorOutcome.updatedSequence?.cameraSystem)
        assertEquals("4000K neutro", directorOutcome.updatedSequence?.lighting?.colorTemperatureKelvin)

        val scriptOutcome = AgentLogic.applyAgentToolCall(
            project = proj,
            activeSequenceId = "seq_1",
            toolName = "refinar_guion_escena",
            args = mapOf(
                "sceneNumber" to "2",
                "slugline" to "EXT. MUELLE - MADRUGADA",
                "beats" to listOf(
                    mapOf("type" to "action", "text" to "La niebla cubre las boyas metálicas.")
                )
            )
        )
        assertEquals(2, scriptOutcome.updatedSequence?.screenplay?.size)
    }

    @Test
    fun `formatAiError formats timeout and app check errors clearly`() = runBlocking {
        var captured: Throwable? = null
        try {
            withTimeout(1L) {
                CompletableDeferred<Unit>().await()
            }
        } catch (t: TimeoutCancellationException) {
            captured = t
        }
        val msg = AgentLogic.formatAiError(captured!!)
        assertTrue(msg.contains("60s") || msg.contains("espera"))

        val forbidden = AgentLogic.formatAiError(RuntimeException("HTTP 403 App Check token invalid"))
        assertTrue(forbidden.contains("403"))
    }

    @Test
    fun `learning engine, progress, and intelligence compute expected metrics`() {
        val proj = sampleProject()
        val finalSeq = proj.sequences.first().copy(versionStatus = SequenceVersionStatus.FINAL, sequenceRating = 5)
        val finalizedProj = proj.copy(sequences = listOf(finalSeq))

        assertFalse(LearningEngine.canExportPdfForSequence(proj.sequences.first()))
        assertTrue(LearningEngine.canExportPdfForSequence(finalSeq))

        val autoLessons = LearningEngine.extractAutomaticLessons(proj.title, finalSeq)
        assertTrue(autoLessons.size >= 3)

        val radar = ProductionProgress.computeProductionRadarMetrics(finalizedProj, finalSeq)
        assertEquals(4, radar.axes.size)
        assertTrue(radar.hasContent)
        assertTrue(radar.axes.all { it.sequenceScore in 0..100 })

        val lessons = listOf(
            LearningLesson(
                id = "l1",
                category = LearningCategory.CAMERA,
                insight = "ARRI Alexa 65 con Cooke anamórficos",
                rating = 5,
                sourceProjectTitle = "Prev"
            )
        )
        val report = StudioIntelligence.buildStudioIntelligence(listOf(finalizedProj), lessons, finalizedProj, finalSeq)
        assertEquals(1, report.finalizedSequencesCount)
        assertTrue(report.totalTrackedOutcomes >= 1)
        assertEquals(AgentRoleFocus.COUNCIL, AgentRoleFocus.fromWire("unknown"))
    }
}
