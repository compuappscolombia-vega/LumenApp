package com.example

import com.example.data.model.AgentRoleFocus
import com.example.data.model.LearningCategory
import com.example.data.model.LearningLesson
import com.example.data.model.LightingSetup
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.ScreenplayBeat
import com.example.data.model.ScreenplayScene
import com.example.data.model.ScriptVersion
import com.example.data.model.SequenceVersionStatus
import com.example.data.model.ShotSpec
import com.example.data.model.SoundDesignSpec
import com.example.data.model.SourceMaterialType
import com.example.data.model.StoryboardCard
import com.example.data.model.SystemErrorSeverity
import com.example.data.model.SystemErrorSource
import com.example.data.model.SystemErrorStatus
import com.example.data.model.UserStudioConfig
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PersistedAccountSession
import com.example.data.util.AgentLogic
import com.example.data.util.LearningEngine
import com.example.data.util.PdfExporter
import com.example.data.util.ProductionProgress
import com.example.data.util.ProjectTemplateCatalog
import com.example.data.util.StudioIntelligence
import com.example.data.util.StudioLogic
import com.example.data.util.StudioManualCatalog
import com.example.data.util.StudioMetricsCalculator
import com.example.data.util.SystemErrorDiagnostics
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
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

    @Test
    fun `production schedule, voice transcript formatting, and metrics calculator work accurately`() {
        val proj1 = sampleProject()
        val seqFinal = proj1.sequences.first().copy(
            id = "seq_2",
            title = "Clímax del Faro",
            versionStatus = SequenceVersionStatus.FINAL,
            createdAtMillis = 1_700_000_000_000L,
            updatedAtMillis = 1_700_086_400_000L
        )
        val proj2 = proj1.copy(
            id = "proj_2",
            title = "Marea Roja",
            sequences = listOf(seqFinal),
            sequencesCount = 1,
            createdAtMillis = 1_700_000_000_000L,
            updatedAtMillis = 1_700_172_800_000L
        )

        val schedule = StudioLogic.buildProductionSchedule(seqFinal)
        assertEquals(1, schedule.size)
        assertTrue(schedule.first().estimatedMinutes >= 15)
        assertTrue(schedule.first().cameraAndLens.contains("RED V-Raptor"))

        val firstAppend = StudioLogic.appendDictatedTranscript("", "la cámara avanza lento por el pasillo")
        assertEquals("La cámara avanza lento por el pasillo.", firstAppend)
        val secondAppend = StudioLogic.appendDictatedTranscript(firstAppend, "un relámpago ilumina la ventana.")
        assertEquals("La cámara avanza lento por el pasillo.\nUn relámpago ilumina la ventana.", secondAppend)

        val metrics = StudioMetricsCalculator.computeMetrics(listOf(proj1, proj2))
        assertEquals(2, metrics.totalProjects)
        assertEquals(2, metrics.totalSequences)
        assertEquals(1, metrics.draftSequences)
        assertEquals(1, metrics.finalSequences)
        assertEquals(2, metrics.totalScenes)
        assertEquals(4, metrics.totalBeats)
        assertTrue(metrics.totalScriptWords > 10)
        assertEquals(2, metrics.projectBars.size)
        assertEquals(2, metrics.scriptLengthBySequence.size)
        assertTrue(metrics.activityTimeline.isNotEmpty())
    }

    @Test
    fun `copyFileToUri writes exported bytes to destination device storage Uri cleanly`() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val source = PdfExporter.saveTextExport(context, "guion_plan_test.txt", "GUION Y PLAN DE RODAJE")
            val dest = java.io.File(context.cacheDir, "device_saved_copy.pdf")
            dest.delete()
            val result = PdfExporter.copyFileToUri(context, source, android.net.Uri.fromFile(dest))
            assertTrue(result.isSuccess)
            assertTrue(dest.exists())
            assertEquals("GUION Y PLAN DE RODAJE", dest.readText())
            source.delete()
            dest.delete()
        }
    }

    @Test
    fun `cinematic project template library provides folders and supports custom folders and notes`() {
        val all = ProjectTemplateCatalog.ALL_TEMPLATES
        assertTrue(all.any { it.id == "short_film" })
        assertTrue(all.any { it.id == "documentary" })
        assertTrue(all.any { it.id == "feature_script" })
        assertTrue(all.any { it.id == "series_pilot" })
        assertTrue(all.any { it.id == "commercial_mv" })
        assertTrue(all.all { it.folders.size >= 3 })

        val docFolders = ProjectTemplateCatalog.defaultFoldersFor("documentary")
        assertEquals(5, docFolders.size)
        assertEquals(SourceMaterialType.AUDIOBOOK, ProjectTemplateCatalog.findById("documentary").defaultSourceType)

        val featureFolders = ProjectTemplateCatalog.defaultFoldersFor("feature_script")
        assertEquals(6, featureFolders.size)
        assertTrue(featureFolders.any { it.name.contains("VFX") })

        val withCustom = ProjectTemplateCatalog.addCustomFolder(
            existing = docFolders,
            rawName = "Archivo_Prensa",
            category = "visual",
            description = "Recortes históricos"
        )
        assertEquals(6, withCustom.size)
        assertEquals("06_Archivo_Prensa", withCustom.last().name)
        assertEquals(com.example.data.model.WorkspaceTab.STORYBOARD, ProjectTemplateCatalog.resolveWorkspaceTab(withCustom.last()))

        val updated = ProjectTemplateCatalog.updateFolderNotes(
            existing = withCustom,
            folderId = docFolders.first().id,
            notes = "Confirmar entrevistas del lunes"
        )
        assertEquals("Confirmar entrevistas del lunes", updated.first().notes)
    }

    @Test
    fun `screenwriting formatting snippets and live metrics compute scenes words and cues`() {
        var text = StudioLogic.insertScreenplaySnippet("", StudioLogic.ScreenplayFormatSnippet.INT_SCENE)
        text = StudioLogic.insertScreenplaySnippet(text, StudioLogic.ScreenplayFormatSnippet.DIALOGUE)
        text = StudioLogic.insertScreenplaySnippet(text, StudioLogic.ScreenplayFormatSnippet.CAMERA)
        text = StudioLogic.insertScreenplaySnippet(text, StudioLogic.ScreenplayFormatSnippet.SOUND)

        val metrics = StudioLogic.computeScreenplayInputMetrics(text)
        assertEquals(1, metrics.sceneCount)
        assertEquals(2, metrics.cueCount)
        assertTrue(metrics.wordCount >= 12)
        assertTrue(metrics.lineCount >= 7)
        assertTrue(metrics.estimatedMinutes >= 0.5)
    }

    @Test
    fun `folder assignment deletion and asset summary organize scripts and storyboards`() {
        val baseProj = sampleProject().copy(
            sequences = listOf(
                sampleProject().sequences.first().copy(
                    storyboardFrames = listOf(
                        StoryboardCard(id = "sb_1", title = "Plano 1"),
                        StoryboardCard(id = "sb_2", title = "Plano 2")
                    )
                )
            ),
            productionFolders = ProjectTemplateCatalog.defaultFoldersFor("short_film")
        )
        val firstFolder = baseProj.productionFolders.first()
        val assigned = ProjectTemplateCatalog.assignSequenceToFolder(baseProj.productionFolders, firstFolder.id, "seq_1")
        assertTrue(assigned.first().linkedSequenceIds.contains("seq_1"))

        val summary = ProjectTemplateCatalog.resolveFolderAssetsSummary(assigned.first(), baseProj)
        assertEquals(1, summary.linkedSequences.size)
        assertEquals(1, summary.sceneCount)
        assertEquals(2, summary.storyboardFramesCount)

        val unlinked = ProjectTemplateCatalog.removeSequenceFromFolder(assigned, firstFolder.id, "seq_1")
        assertFalse(unlinked.first().linkedSequenceIds.contains("seq_1"))

        val deleted = ProjectTemplateCatalog.deleteFolder(unlinked, unlinked.last().id)
        assertEquals(baseProj.productionFolders.size - 1, deleted.size)
    }

    @Test
    fun `system error diagnostics sanitize PII audit health and manual PDF generates valid file`() {
        runBlocking {
            val entry = SystemErrorDiagnostics.buildEntry(
                source = SystemErrorSource.AI,
                rawMessage = "HTTP 403 App Check token expired for director@cine.com token AIzaSyFakeKey1234567",
                contextHint = "produceSequence"
            )
            assertEquals(SystemErrorSeverity.CRITICAL, entry.severity)
            assertEquals(SystemErrorStatus.OPEN, entry.status)
            assertFalse(entry.message.contains("director@cine.com"))
            assertFalse(entry.message.contains("AIzaSyFakeKey1234567"))
            assertTrue(entry.suggestedFix.isNotBlank())

            val brokenProj = sampleProject().copy(
                productionFolders = emptyList(),
                sequences = listOf(
                    sampleProject().sequences.first().copy(
                        storyboardFrames = listOf(
                            StoryboardCard(id = "sb_broken", localImagePath = "/nonexistent/path/frame.png")
                        )
                    )
                )
            )
            val audit = SystemErrorDiagnostics.runHealthAuditAndPlanRepairs(
                projects = listOf(brokenProj),
                selectedProjectId = "missing_proj",
                selectedSequenceId = "missing_seq",
                config = UserStudioConfig(themeMode = "invalid", defaultAspectRatio = "99:1"),
                hasSignedInUser = false,
                checkFilesOnDisk = true
            )
            assertEquals("dark", audit.sanitizedConfig.themeMode)
            assertEquals("2.39:1", audit.sanitizedConfig.defaultAspectRatio)
            assertEquals(1, audit.projectsNeedingDefaultFolders.size)
            assertEquals(listOf("sb_broken"), audit.brokenFrameIdsToClear)

            val report = SystemErrorDiagnostics.formatDiagnosticReportText(listOf(entry))
            assertTrue(report.contains("LUMENREEL STUDIO"))
            assertTrue(report.contains("App Check"))

            assertEquals(StudioManualCatalog.SECTIONS.size, StudioManualCatalog.filterSections("", "all").size)
            assertTrue(StudioManualCatalog.filterSections("App Check", "all").any { it.id == "error_repair" })

            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val manualPdf = PdfExporter.exportUserManualPdf(context)
            assertTrue(manualPdf.exists())
            assertTrue(manualPdf.length() > 500L)
            manualPdf.delete()
            Unit
        }
    }

    @Test
    fun `script version word count and persisted account session model behave accurately`() {
        val scenes = sampleProject().sequences.first().screenplay
        val words = ScriptVersion.computeWordCount(scenes)
        assertTrue(words >= 12)
        assertEquals(0, ScriptVersion.computeWordCount(emptyList()))

        val activeSession = PersistedAccountSession(
            uid = "uid_google_1",
            email = "director@lumenreel.com",
            displayName = "Elena Vance",
            lastSyncedAtMillis = 1_700_000_000_000L,
            explicitlySignedOut = false
        )
        assertTrue(activeSession.hasRememberedAccount)

        val signedOutSession = activeSession.copy(explicitlySignedOut = true)
        assertFalse(signedOutSession.hasRememberedAccount)

        val blankSession = PersistedAccountSession(uid = "", explicitlySignedOut = false)
        assertFalse(blankSession.hasRememberedAccount)
    }
}
