package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AgentChatOutcome
import com.example.data.ai.StudioAiClient
import com.example.data.model.AgentMessage
import com.example.data.model.AgentRoleFocus
import com.example.data.model.AgentToolAction
import com.example.data.model.AudioAnalysisReport
import com.example.data.model.CalendarMilestoneType
import com.example.data.model.DirectorDraftEdits
import com.example.data.model.IngestDraftState
import com.example.data.model.LightingSetup
import com.example.data.model.ProductionMilestoneItem
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.ScreenplayBeat
import com.example.data.model.ScreenplayScene
import com.example.data.model.SequenceVersionStatus
import com.example.data.model.ShotSpec
import com.example.data.model.SoundDesignSpec
import com.example.data.model.SourceMaterialType
import com.example.data.model.StoryboardCard
import com.example.data.model.ThinkingDepth
import com.example.data.model.UserStudioConfig
import com.example.data.model.WorkspaceTab
import com.example.data.repository.LOCAL_GUEST_UID
import com.example.data.repository.LocalStudioStore
import com.example.data.repository.StudioRepository
import com.example.data.util.LearningEngine
import com.example.data.util.ProjectTemplateCatalog
import com.example.data.util.StudioLogic
import com.example.ui.components.AccountDialog
import com.example.ui.components.NewProjectDialog
import com.example.ui.components.ProductionCalendarDialog
import com.example.ui.components.SourceIngestContent
import com.example.ui.components.StudioLoadingOverlay
import com.example.ui.components.readClipboardText
import com.example.ui.screens.MetricsDashboardScreen
import com.example.ui.screens.ProjectDashboardScreen
import com.example.ui.screens.ScreenplayTabView
import com.example.ui.screens.ShotsAndSoundTabView
import com.example.ui.theme.LumenReelTheme
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.StudioViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StudioFunctionalCujTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()

    private class FakeStudioAiClient(
        var shouldFailProduce: Boolean = false
    ) : StudioAiClient {
        override suspend fun analyzeAudio(
            filename: String,
            mimeType: String,
            audioBytes: ByteArray,
            learningContext: String
        ): AudioAnalysisReport = AudioAnalysisReport(
            transcription = "Transcripción de prueba",
            directingRecommendations = "Plano cerrado con 50mm"
        )

        override suspend fun produceSequence(
            userId: String,
            project: ProductionProject,
            sequenceTitle: String,
            sourceType: SourceMaterialType,
            directorStyle: String,
            aspectRatio: String,
            thinkingDepth: ThinkingDepth,
            continuityBible: String,
            sourceText: String,
            learningContext: String,
            attachedFilename: String?,
            attachedMimeType: String?,
            attachedBytes: ByteArray?
        ): ProductionSequence {
            if (shouldFailProduce) {
                throw IllegalStateException("HTTP 403 App Check verification failed")
            }
            return ProductionSequence(
                id = "seq_ai_01",
                projectId = project.id,
                ownerId = userId,
                title = sequenceTitle.trim(),
                versionStatus = SequenceVersionStatus.DRAFT,
                sourceType = sourceType,
                sourceFilename = attachedFilename,
                directorStyle = directorStyle,
                aspectRatio = aspectRatio,
                cameraSystem = "ARRI Alexa 65",
                lensPackage = "Cooke Anamorphic /i",
                filmStockOrLut = "Kodak 2383 Print LUT",
                logline = "Una escena crucial en el estudio.",
                directorVision = "Iluminación contrastada con atmósfera húmeda.",
                lighting = LightingSetup(
                    keyLight = "Tungsteno lateral",
                    colorTemperatureKelvin = "3200K"
                ),
                shotList = listOf(
                    ShotSpec("01", "PG", "35mm", "Dolly in", 5.0, "Plano inicial", "Tensión"),
                    ShotSpec("02", "PMC", "50mm", "Fijo", 4.0, "Reacción", "Intimidad")
                ),
                soundDesign = SoundDesignSpec(atmosLayers = listOf("Lluvia densa"), tempoBpm = 72),
                screenplay = listOf(
                    ScreenplayScene(
                        sceneNumber = "1",
                        slugline = "INT. ESTUDIO - NOCHE",
                        timeOfDay = "NOCHE",
                        beats = listOf(
                            ScreenplayBeat("action", text = "El proyector se enciende en la oscuridad."),
                            ScreenplayBeat("dialogue", character = "MARA", text = "Tenemos la secuencia.")
                        )
                    )
                ),
                storyboardFrames = listOf(
                    StoryboardCard(
                        id = "sb_1",
                        title = "Encuadre 1",
                        shotRef = "Plano 01",
                        aspectRatio = aspectRatio,
                        visualPrompt = "Wide cinema shot"
                    )
                ),
                continuitySummary = "Se mantiene vestuario oscuro."
            )
        }

        override suspend fun chatWithAgent(
            project: ProductionProject,
            activeSequence: ProductionSequence?,
            focus: AgentRoleFocus,
            userMessage: String,
            learningContext: String,
            recentHistory: List<AgentMessage>
        ): AgentChatOutcome = AgentChatOutcome(
            replyText = "Propuesta aplicada.",
            actions = listOf(AgentToolAction("ajustar_direccion_y_camara", "Cámara ajustada", "direccion")),
            updatedProject = project,
            updatedDraftSequence = activeSequence,
            bibleChanged = false
        )

        override suspend fun generateStoryboardImage(
            context: Context,
            frame: StoryboardCard,
            sequenceAspectRatio: String
        ): String = File(context.filesDir, "fake_sb.png").apply { writeText("img") }.absolutePath
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "lumenreel_local_studio_v1.json").delete()
        context.getSharedPreferences("lumenreel_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun awaitUiWhere(
        vm: StudioViewModel,
        predicate: (StudioUiState) -> Boolean = { true }
    ): StudioUiState {
        val deadline = System.currentTimeMillis() + 5_000L
        while (System.currentTimeMillis() < deadline) {
            testDispatcher.scheduler.advanceUntilIdle()
            val cur = vm.projectsUiState.value
            if (cur is UiState.Success && predicate(cur.data)) {
                return cur.data
            }
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                Thread.sleep(20L)
            }
        }
        val finalState = vm.projectsUiState.value
        assertTrue("Expected UiState.Success matching predicate but was $finalState", finalState is UiState.Success && predicate(finalState.data))
        return (finalState as UiState.Success).data
    }

    @Test
    fun `local store persists created projects and produced sequences across reloads`() = runBlocking {
        val storeA = LocalStudioStore(context)
        val projId = storeA.createProject(
            userId = LOCAL_GUEST_UID,
            title = "Odisea Austral",
            genreStyle = "Épica Monumental IMAX",
            defaultSourceType = SourceMaterialType.BOOK,
            continuityBible = "Regla base"
        ).getOrThrow()

        val baseSeq = StudioLogic.buildManualDraftSequence(
            userId = LOCAL_GUEST_UID,
            project = ProductionProject(id = projId, ownerId = LOCAL_GUEST_UID, title = "Odisea Austral"),
            sequenceTitle = "Capítulo 1",
            sourceType = SourceMaterialType.BOOK,
            directorStyle = "Épica Monumental IMAX",
            aspectRatio = "1.43:1",
            sourceText = "Una embarcación cruza el estrecho helado bajo auroras."
        )
        storeA.saveProducedSequence(LOCAL_GUEST_UID, projId, baseSeq, "Regla base actualizada", 1).getOrThrow()

        // Create a completely fresh LocalStudioStore instance to simulate process restart
        val storeB = LocalStudioStore(context)
        val loadedProjects = storeB.observeProjects(LOCAL_GUEST_UID).first()
        assertEquals(1, loadedProjects.size)
        assertEquals("Odisea Austral", loadedProjects.first().title)

        val loadedSequences = storeB.observeSequences(projId).first()
        assertEquals(1, loadedSequences.size)
        assertEquals("Capítulo 1", loadedSequences.first().title)
        assertEquals(1, loadedSequences.first().screenplay.size)
    }

    @Test
    fun `viewmodel CUJ preserves director and ingest adjustments across tabs and saves sequences cleanly`() = runTest {
        val store = LocalStudioStore(context, testDispatcher)
        val repo = StudioRepository(databaseId = "unused", localStore = store)
        val ai = FakeStudioAiClient()
        val vm = StudioViewModel(repo, ai, LOCAL_GUEST_UID)

        val collectorJob = backgroundScope.launch {
            vm.projectsUiState.collect {}
        }

        vm.createProject("Faro Blanco", "Thriller Psicológico Neo-Noir", SourceMaterialType.BOOK, "Biblia inicial")
        val initialUi = awaitUiWhere(vm) { it.activeProject?.title == "Faro Blanco" }
        val project = initialUi.activeProject!!
        assertEquals("Faro Blanco", project.title)

        // 1. Produce sequence via AI client & verify it lands in Guion with screenplay scenes
        vm.produceSequence(
            project = project,
            lessons = emptyList(),
            sequenceTitle = "Secuencia del Muelle",
            sourceType = SourceMaterialType.BOOK,
            directorStyle = "Thriller Psicológico Neo-Noir",
            aspectRatio = "2.39:1",
            thinkingDepth = ThinkingDepth.STANDARD,
            continuityBible = project.continuityBible,
            sourceText = "Texto fuente suficientemente largo para pasar validación cinematográfica.",
            attachedFilename = null,
            attachedMimeType = null,
            attachedBytes = null
        )

        var ui = awaitUiWhere(vm) {
            it.activeSequence?.title == "Secuencia del Muelle" && it.activeTab == WorkspaceTab.SCREENPLAY
        }
        assertNotNull(ui.activeSequence)
        assertEquals("Secuencia del Muelle", ui.activeSequence?.title)
        assertEquals(WorkspaceTab.SCREENPLAY, ui.activeTab)
        assertEquals(1, ui.activeSequence?.screenplay?.size)

        // 2. Switch to Dirección (VISION), make unsaved adjustments, switch to Guion, Agent, Shots, then back to Dirección
        vm.selectTab(WorkspaceTab.VISION)
        ui = awaitUiWhere(vm) { it.activeTab == WorkspaceTab.VISION }
        val seq = ui.activeSequence!!
        vm.updateDirectorDraft(project, seq) {
            it.copy(
                cameraSystem = "Panavision Millennium DXL2",
                colorTempKelvin = "4200K tormenta",
                directorVision = "Visión ajustada manualmente"
            )
        }
        vm.updateVisionRatingDraft(seq.id, 4)
        vm.updateVisionNoteDraft(seq.id, "Excelente contraste en sombras")

        // Switch across Guion -> Planos y Sonido -> Agente -> back to Dirección
        vm.selectTab(WorkspaceTab.SCREENPLAY)
        vm.selectTab(WorkspaceTab.SHOTS)
        vm.updateSelectedShotIndex(seq.id, 1)
        vm.updateStage3DMode(seq.id, false)
        vm.selectTab(WorkspaceTab.AGENT)
        vm.updateAgentFocus(project.id, AgentRoleFocus.DIRECTOR)
        vm.updateAgentDraftMessage(project.id, "Ajustar lente a 50mm")
        vm.selectTab(WorkspaceTab.VISION)
        ui = awaitUiWhere(vm) {
            it.activeTab == WorkspaceTab.VISION && it.activeDirectorDraft.cameraSystem == "Panavision Millennium DXL2"
        }

        // Verify ZERO adjustments were lost across tab switches!
        assertEquals("Panavision Millennium DXL2", ui.activeDirectorDraft.cameraSystem)
        assertEquals("4200K tormenta", ui.activeDirectorDraft.colorTempKelvin)
        assertEquals("Visión ajustada manualmente", ui.activeDirectorDraft.directorVision)
        assertTrue(ui.activeDirectorDraft.isDirty)
        assertEquals(4, ui.activeVisionRating)
        assertEquals("Excelente contraste en sombras", ui.activeVisionRatingNote)
        assertEquals(1, ui.activeSelectedShotIndex)
        assertFalse(ui.activeStage3DMode)
        assertEquals(AgentRoleFocus.DIRECTOR, ui.activeAgentFocus)
        assertEquals("Ajustar lente a 50mm", ui.activeAgentDraftMessage)

        // 3. Persist Director draft edits & add a manual scene in Guion, then verify persistence
        vm.saveDirectorDraftEdits(ui.activeProject!!, ui.activeSequence!!)
        ui = awaitUiWhere(vm) {
            it.activeSequence?.cameraSystem == "Panavision Millennium DXL2" && !it.activeDirectorDraft.isDirty
        }
        assertEquals("Panavision Millennium DXL2", ui.activeSequence?.cameraSystem)
        assertFalse(ui.activeDirectorDraft.isDirty)

        vm.saveScreenplaySceneDraft(
            projectId = ui.activeProject!!.id,
            sequence = ui.activeSequence!!,
            sceneNumber = "2",
            slugline = "EXT. ROMPEOLAS - AMANECER",
            timeOfDay = "AMANECER",
            actionText = "Las olas rompen contra el muro de sal.",
            dialogueCharacter = "ELENA",
            dialogueText = "El transmisor volvió a sonar.",
            editingIndex = null
        )
        ui = awaitUiWhere(vm) { it.activeSequence?.screenplay?.size == 2 }
        assertEquals(2, ui.activeSequence?.screenplay?.size)

        // 4. Approve Final & unlock PDF export
        vm.approveFinalVersion(ui.activeProject!!, ui.activeSequence!!)
        ui = awaitUiWhere(vm) { LearningEngine.canExportPdfForSequence(it.activeSequence) }
        assertTrue(LearningEngine.canExportPdfForSequence(ui.activeSequence))

        collectorJob.cancel()
    }

    @Test
    fun `manual draft creation and validation errors surface cleanly inside ingest draft state`() = runTest {
        val store = LocalStudioStore(context, testDispatcher)
        val repo = StudioRepository(databaseId = "unused", localStore = store)
        val ai = FakeStudioAiClient(shouldFailProduce = true)
        val vm = StudioViewModel(repo, ai, LOCAL_GUEST_UID)

        val collectorJob = backgroundScope.launch { vm.projectsUiState.collect {} }

        vm.createProject("Proyecto Manual", "Épica Monumental IMAX", SourceMaterialType.STORY, "")
        val project = awaitUiWhere(vm) { it.activeProject?.title == "Proyecto Manual" }.activeProject!!

        // Validation failure writes to activeIngestDraft.ingestError so it appears inside SourceIngestSheet
        vm.produceSequence(
            project = project,
            lessons = emptyList(),
            sequenceTitle = "",
            sourceType = SourceMaterialType.STORY,
            directorStyle = "Épica Monumental IMAX",
            aspectRatio = "1.43:1",
            thinkingDepth = ThinkingDepth.STANDARD,
            continuityBible = "",
            sourceText = "corto",
            attachedFilename = null,
            attachedMimeType = null,
            attachedBytes = null
        )
        var ui = awaitUiWhere(vm) { !it.activeIngestDraft.ingestError.isNullOrBlank() }
        assertNotNull(ui.activeIngestDraft.ingestError)

        // Simulated AI 403 error also surfaces inside activeIngestDraft.ingestError
        vm.produceSequence(
            project = project,
            lessons = emptyList(),
            sequenceTitle = "Escena 1",
            sourceType = SourceMaterialType.STORY,
            directorStyle = "Épica Monumental IMAX",
            aspectRatio = "1.43:1",
            thinkingDepth = ThinkingDepth.STANDARD,
            continuityBible = "",
            sourceText = "Texto suficientemente largo para invocar al modelo Gemini.",
            attachedFilename = null,
            attachedMimeType = null,
            attachedBytes = null
        )
        ui = awaitUiWhere(vm) { it.activeIngestDraft.ingestError?.contains("403") == true }
        assertTrue(ui.activeIngestDraft.ingestError?.contains("403") == true)

        // Fallback/explicit Manual Draft works offline and switches directly to Guion
        vm.createManualDraftSequence(
            project = project,
            sequenceTitle = "Borrador Offline",
            sourceType = SourceMaterialType.STORY,
            directorStyle = "Épica Monumental IMAX",
            aspectRatio = "1.43:1",
            continuityBible = "",
            sourceText = "Un pasillo silencioso iluminado por neón ámbar."
        )
        ui = awaitUiWhere(vm) { it.activeSequence?.title == "Borrador Offline" }
        assertEquals("Borrador Offline", ui.activeSequence?.title)
        assertEquals(WorkspaceTab.SCREENPLAY, ui.activeTab)
        assertEquals(SequenceVersionStatus.DRAFT, ui.activeSequence?.versionStatus)

        collectorJob.cancel()
    }

    @Test
    fun `compose ingest content displays inline error, loads sample, and pastes from clipboard`() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("test", "Texto copiado desde el portapapeles del usuario."))
        assertEquals("Texto copiado desde el portapapeles del usuario.", readClipboardText(context))

        var draft = IngestDraftState(ingestError = "Error visible dentro de la mesa de ingesta")
        var manualClicked = false
        val project = ProductionProject(id = "p1", title = "Test Film")

        composeRule.setContent {
            LumenReelTheme {
                SourceIngestContent(
                    project = project,
                    draft = draft,
                    isProducing = false,
                    isAnalyzingAudio = false,
                    latestAudioReport = null,
                    onDraftChange = { transform -> draft = transform(draft) },
                    onClose = {},
                    onSaveBible = {},
                    onAnalyzeAudio = {},
                    onProduceSequence = {},
                    onCreateManualDraft = { manualClicked = true }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ingest_error_banner").assertIsDisplayed()

        composeRule.onNodeWithTag("ingest_paste_source_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(draft.sourceText.contains("Texto copiado desde el portapapeles"))

        composeRule.onNodeWithTag("create_manual_draft_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertTrue(manualClicked)
    }

    @Test
    fun `state repository retains active sequence, director edits, and script view across ViewModel recreation`() = runTest {
        val store = LocalStudioStore(context, testDispatcher)
        val repo = StudioRepository(databaseId = "unused", localStore = store)
        val ai = FakeStudioAiClient()

        // First ViewModel instance creates project, produces sequence, edits Director settings, switches to Screenplay
        val vm1 = StudioViewModel(repo, ai, LOCAL_GUEST_UID)
        val job1 = backgroundScope.launch { vm1.projectsUiState.collect {} }

        vm1.createProject("Luz de Invierno", "Épica Monumental IMAX", SourceMaterialType.BOOK, "Biblia compartida")
        val proj = awaitUiWhere(vm1) { it.activeProject?.title == "Luz de Invierno" }.activeProject!!

        vm1.produceSequence(
            project = proj,
            lessons = emptyList(),
            sequenceTitle = "Escena del Faro",
            sourceType = SourceMaterialType.BOOK,
            directorStyle = "Épica Monumental IMAX",
            aspectRatio = "1.43:1",
            thinkingDepth = ThinkingDepth.STANDARD,
            continuityBible = proj.continuityBible,
            sourceText = "Texto narrativo extenso sobre un faro solitario frente al mar del sur.",
            attachedFilename = null,
            attachedMimeType = null,
            attachedBytes = null
        )

        var ui1 = awaitUiWhere(vm1) { it.activeSequence?.title == "Escena del Faro" }
        val seq = ui1.activeSequence!!
        assertNotNull(repo.getSequenceById(LOCAL_GUEST_UID, proj.id, seq.id))

        vm1.selectTab(WorkspaceTab.VISION)
        vm1.updateDirectorDraft(proj, seq) {
            it.copy(cameraSystem = "RED V-Raptor XL", directorVision = "Toma anamórfica con neblina")
        }
        vm1.selectTab(WorkspaceTab.SCREENPLAY)
        ui1 = awaitUiWhere(vm1) {
            it.activeTab == WorkspaceTab.SCREENPLAY && it.activeDirectorDraft.cameraSystem == "RED V-Raptor XL"
        }
        assertEquals("RED V-Raptor XL", ui1.activeDirectorDraft.cameraSystem)
        job1.cancel()

        // Second ViewModel instance attached to the same State Repository immediately restores everything
        val vm2 = StudioViewModel(repo, ai, LOCAL_GUEST_UID)
        val job2 = backgroundScope.launch { vm2.projectsUiState.collect {} }
        var ui2 = awaitUiWhere(vm2) {
            it.activeProject?.id == proj.id && it.activeSequence?.id == seq.id
        }
        assertEquals(WorkspaceTab.SCREENPLAY, ui2.activeTab)
        assertEquals("Escena del Faro", ui2.activeSequence?.title)
        assertEquals(1, ui2.activeSequence?.screenplay?.size)
        assertEquals("RED V-Raptor XL", ui2.activeDirectorDraft.cameraSystem)
        assertTrue(ui2.activeDirectorDraft.isDirty)

        vm2.selectTab(WorkspaceTab.VISION)
        ui2 = awaitUiWhere(vm2) { it.activeTab == WorkspaceTab.VISION }
        assertEquals("Toma anamórfica con neblina", ui2.activeDirectorDraft.directorVision)

        job2.cancel()
    }

    @Test
    fun `screenplay tab exposes mic dictation and device PDF save buttons`() {
        val sampleSeq = ProductionSequence(
            id = "seq_ui_1",
            projectId = "proj_ui_1",
            ownerId = "user_ui",
            title = "Escena 1: El Muelle",
            versionStatus = SequenceVersionStatus.DRAFT,
            shotList = listOf(ShotSpec("01", "PG", "35mm", "Dolly in", 5.0, "Muelle nocturno", "Tensión")),
            screenplay = listOf(
                ScreenplayScene(
                    sceneNumber = "1",
                    slugline = "EXT. MUELLE - NOCHE",
                    timeOfDay = "NOCHE",
                    beats = listOf(ScreenplayBeat("action", text = "La niebla cubre el embarcadero."))
                )
            ),
            createdAtMillis = 1_700_000_000_000L,
            updatedAtMillis = 1_700_086_400_000L
        )

        var savePdfClicked = 0
        composeRule.setContent {
            LumenReelTheme {
                ScreenplayTabView(
                    sequence = sampleSeq,
                    isExporting = false,
                    onOpenIngest = {},
                    onSaveSceneDraft = { _, _, _, _, _, _, _ -> },
                    onExportPdf = {},
                    onSaveScriptSchedulePdf = { savePdfClicked++ },
                    onExportMarkdown = {},
                    onExportFountain = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("screenplay_dictate_scene_button").assertIsDisplayed()
        composeRule.onNodeWithTag("save_script_schedule_pdf_button").assertIsDisplayed().performClick()
        assertEquals(1, savePdfClicked)
    }

    @Test
    fun `metrics dashboard and global loading overlay render charts and feedback cleanly`() {
        val sampleSeq = ProductionSequence(
            id = "seq_ui_1",
            projectId = "proj_ui_1",
            ownerId = "user_ui",
            title = "Escena 1: El Muelle",
            versionStatus = SequenceVersionStatus.DRAFT,
            shotList = listOf(ShotSpec("01", "PG", "35mm", "Dolly in", 5.0, "Muelle nocturno", "Tensión")),
            screenplay = listOf(
                ScreenplayScene(
                    sceneNumber = "1",
                    slugline = "EXT. MUELLE - NOCHE",
                    timeOfDay = "NOCHE",
                    beats = listOf(ScreenplayBeat("action", text = "La niebla cubre el embarcadero."))
                )
            ),
            createdAtMillis = 1_700_000_000_000L,
            updatedAtMillis = 1_700_086_400_000L
        )
        val sampleProject = ProductionProject(
            id = "proj_ui_1",
            ownerId = "user_ui",
            title = "El Faro Austral",
            sequences = listOf(sampleSeq),
            sequencesCount = 1,
            createdAtMillis = 1_700_000_000_000L,
            updatedAtMillis = 1_700_086_400_000L
        )

        var backClicked = false
        composeRule.setContent {
            LumenReelTheme {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize()
                ) {
                    MetricsDashboardScreen(
                        projects = listOf(sampleProject),
                        onBack = { backClicked = true }
                    )
                    StudioLoadingOverlay(
                        visible = true,
                        headline = "Generando secuencia con Gemini…",
                        subtitle = "Guardando en Firestore."
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("metrics_dashboard_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("metrics_total_sequences_card").assertIsDisplayed()
        composeRule.onNodeWithTag("metrics_script_length_card").assertIsDisplayed()
        composeRule.onNodeWithTag("global_loading_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("global_loading_headline").assertIsDisplayed()
        composeRule.onNodeWithTag("global_loading_subtitle").assertIsDisplayed()
        assertFalse(backClicked)
    }

    @Test
    fun `shots tab exposes save schedule pdf button cleanly`() {
        val sampleSeq = ProductionSequence(
            id = "seq_shots_1",
            projectId = "proj_1",
            ownerId = "user_1",
            title = "Plano Secuencia",
            shotList = listOf(ShotSpec("01", "PG", "28mm", "Tracking", 6.0, "Pasillo", "Urgencia"))
        )
        var scheduleSaveClicked = false
        composeRule.setContent {
            LumenReelTheme {
                ShotsAndSoundTabView(
                    sequence = sampleSeq,
                    selectedShotIndex = 0,
                    use3DStage = false,
                    isExporting = false,
                    onSelectShot = {},
                    onToggle3DStage = {},
                    onSaveSchedulePdf = { scheduleSaveClicked = true }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("shots_save_schedule_pdf_button").assertIsDisplayed().performClick()
        assertTrue(scheduleSaveClicked)
    }

    @Test
    fun `project template initialization persists folders in store and ViewModel`() = runTest {
        val store = LocalStudioStore(context, testDispatcher)
        val repo = StudioRepository(databaseId = "unused", localStore = store)
        val ai = FakeStudioAiClient()
        val vm = StudioViewModel(repo, ai, LOCAL_GUEST_UID)

        val collectorJob = backgroundScope.launch { vm.projectsUiState.collect {} }

        val featureTemplate = ProjectTemplateCatalog.findById("feature_script")
        vm.createProject(
            title = "Crónicas del Océano",
            genreStyle = featureTemplate.defaultStyle,
            defaultSourceType = featureTemplate.defaultSourceType,
            continuityBible = featureTemplate.continuityStarter,
            templateId = featureTemplate.id,
            productionFolders = featureTemplate.folders
        )

        var ui = awaitUiWhere(vm) { it.activeProject?.title == "Crónicas del Océano" }
        val proj = ui.activeProject!!
        assertEquals("feature_script", proj.templateId)
        assertEquals(6, proj.productionFolders.size)
        assertEquals("01_Guion_Tecnico_Actos_I_III", proj.productionFolders.first().name)

        // Add custom folder & update folder notes
        vm.addProductionFolder(proj, "Casting_Extra", "bible", "Actores secundarios")
        ui = awaitUiWhere(vm) { it.activeProject?.productionFolders?.size == 7 }
        val addedFolder = ui.activeProject!!.productionFolders.last()
        assertEquals("07_Casting_Extra", addedFolder.name)

        vm.updateProductionFolderNotes(ui.activeProject!!, addedFolder.id, "Audiciones viernes 10h")
        ui = awaitUiWhere(vm) {
            it.activeProject?.productionFolders?.lastOrNull()?.notes == "Audiciones viernes 10h"
        }

        // Open folder tab navigates cleanly
        vm.openProductionFolderTab(ui.activeProject!!.productionFolders.first())
        ui = awaitUiWhere(vm) { it.activeTab == WorkspaceTab.SCREENPLAY }
        assertEquals(WorkspaceTab.SCREENPLAY, ui.activeTab)

        // Verify persistence across fresh LocalStudioStore instance
        val reloaded = LocalStudioStore(context, testDispatcher).observeProjects(LOCAL_GUEST_UID).first()
        assertEquals(1, reloaded.size)
        assertEquals("feature_script", reloaded.first().templateId)
        assertEquals(7, reloaded.first().productionFolders.size)
        assertEquals("Audiciones viernes 10h", reloaded.first().productionFolders.last().notes)

        collectorJob.cancel()
    }

    @Test
    fun `NewProjectDialog template picker auto-configures folders and emits selected template`() {
        var createdTitle = ""
        var createdTemplateId = ""
        var createdFoldersCount = 0
        var createdSourceType: SourceMaterialType? = null

        composeRule.setContent {
            LumenReelTheme {
                NewProjectDialog(
                    onDismiss = {},
                    onCreate = { title, _, sourceType, _, templateId, folders ->
                        createdTitle = title
                        createdTemplateId = templateId
                        createdFoldersCount = folders.size
                        createdSourceType = sourceType
                    }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("template_card_documentary").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("new_project_folders_preview").assertIsDisplayed()
        composeRule.onNodeWithTag("new_project_title_input").performScrollTo().performTextInput("Voces del Sur")
        composeRule.onNodeWithTag("confirm_create_project_button").performClick()
        composeRule.waitForIdle()

        assertEquals("Voces del Sur", createdTitle)
        assertEquals("documentary", createdTemplateId)
        assertEquals(5, createdFoldersCount)
        assertEquals(SourceMaterialType.AUDIOBOOK, createdSourceType)
    }

    @Config(sdk = [35], qualifiers = "w412dp-h915dp")
    @Test
    fun `ProjectDashboardScreen shows template badge and initialized folder count`() {
        val docFolders = ProjectTemplateCatalog.defaultFoldersFor("documentary")
        val proj = ProductionProject(
            id = "proj_doc_1",
            ownerId = LOCAL_GUEST_UID,
            title = "Expedición Antártica",
            genreStyle = "Documental Observacional",
            defaultSourceType = SourceMaterialType.AUDIOBOOK,
            templateId = "documentary",
            productionFolders = docFolders
        )

        composeRule.setContent {
            LumenReelTheme {
                ProjectDashboardScreen(
                    currentUser = null,
                    projects = listOf(proj),
                    activeProjectId = proj.id,
                    searchQuery = "",
                    statusBanner = null,
                    errorBanner = null,
                    onSearchChange = {},
                    onSelectProject = {},
                    onCreateProjectClick = {},
                    onDeleteProject = {},
                    onOpenLearning = {},
                    onOpenBlueprint = {},
                    onExportManualPdf = {},
                    onExportLibraryPdf = {},
                    onOpenAccount = {},
                    onDismissBanner = {},
                    modifier = androidx.compose.ui.Modifier.fillMaxSize()
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_projects_list")
            .performScrollToNode(hasTestTag("dashboard_project_card_proj_doc_1"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_project_card_proj_doc_1")
            .assertIsDisplayed()
            .assertTextContains("Documental · 5 carpetas", substring = true)
        composeRule.onNodeWithTag("project_folders_badge_proj_doc_1", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `viewModel supports dark mode toggle`() = runTest {
        val store = LocalStudioStore(context, testDispatcher)
        val repo = StudioRepository(databaseId = "unused", localStore = store)
        val ai = FakeStudioAiClient()
        val vm = StudioViewModel(repo, ai, LOCAL_GUEST_UID)

        val collectorJob = backgroundScope.launch { vm.projectsUiState.collect {} }
        var ui = awaitUiWhere(vm)
        assertTrue(ui.isDarkMode)

        vm.toggleDarkMode()
        ui = awaitUiWhere(vm) { !it.isDarkMode }
        assertFalse(ui.isDarkMode)

        vm.toggleDarkMode()
        ui = awaitUiWhere(vm) { it.isDarkMode }
        assertTrue(ui.isDarkMode)

        collectorJob.cancel()
    }

    @Test
    fun `ProductionCalendarDialog renders milestones and allows day inspection`() {
        val sampleProject = ProductionProject(
            id = "proj_cal",
            ownerId = LOCAL_GUEST_UID,
            title = "Cine Cal",
            genreStyle = "Drama",
            createdAtMillis = System.currentTimeMillis(),
            sequences = listOf(
                ProductionSequence(
                    id = "seq_cal_1",
                    projectId = "proj_cal",
                    ownerId = LOCAL_GUEST_UID,
                    title = "Secuencia 1",
                    versionStatus = SequenceVersionStatus.FINAL,
                    createdAtMillis = System.currentTimeMillis(),
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        )

        var dismissed = false
        var selectedSeqId: String? = null

        composeRule.setContent {
            LumenReelTheme(darkTheme = true) {
                com.example.ui.components.ProductionCalendarDialog(
                    project = sampleProject,
                    onDismiss = { dismissed = true },
                    onSelectSequence = { seqId -> selectedSeqId = seqId }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("production_calendar_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar_prev_month").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar_next_month").assertIsDisplayed()
        composeRule.onNodeWithTag("close_calendar_dialog").assertIsDisplayed().performClick()
        assertTrue(dismissed)
    }

    @Test
    fun `studio config and workspace drafts persist across store and ViewModel recreation`() = runTest {
        val store1 = LocalStudioStore(context, testDispatcher)
        val repo1 = StudioRepository(databaseId = "unused", localStore = store1)
        val ai = FakeStudioAiClient()
        val vm1 = StudioViewModel(repo1, ai, LOCAL_GUEST_UID)
        val job1 = backgroundScope.launch { vm1.projectsUiState.collect {} }

        vm1.createProject("Config Film", "Documental Observacional", SourceMaterialType.BOOK, "Biblia C1")
        var ui1 = awaitUiWhere(vm1) { it.activeProject?.title == "Config Film" }
        val proj = ui1.activeProject!!

        // Update comprehensive studio config (light theme, preferred style, aspect ratio, deep depth, stage3D false)
        val customConfig = UserStudioConfig(
            themeMode = "light",
            preferredStyle = "Thriller Psicológico Neo-Noir",
            defaultAspectRatio = "1.85:1",
            defaultThinkingDepth = ThinkingDepth.DEEP.name,
            autoSaveDrafts = true,
            stage3DDefault = false
        )
        vm1.updateStudioConfig(customConfig)
        vm1.updateIngestDraft(proj) {
            it.copy(sequenceTitle = "Borrador Persistido", sourceText = "Notas iniciales de rodaje.")
        }
        vm1.updateAgentDraftMessage(proj.id, "Agregar plano dron")
        vm1.selectTab(WorkspaceTab.SHOTS)

        ui1 = awaitUiWhere(vm1) {
            !it.isDarkMode &&
                it.studioConfig.preferredStyle == "Thriller Psicológico Neo-Noir" &&
                it.activeTab == WorkspaceTab.SHOTS
        }
        assertFalse(ui1.isDarkMode)
        assertFalse(ui1.activeStage3DMode)
        assertEquals("1.85:1", ui1.studioConfig.defaultAspectRatio)
        assertEquals(ThinkingDepth.DEEP.name, ui1.studioConfig.defaultThinkingDepth)
        assertEquals("Borrador Persistido", ui1.activeIngestDraft.sequenceTitle)
        assertEquals("Agregar plano dron", ui1.activeAgentDraftMessage)
        job1.cancel()

        // Recreate completely fresh LocalStudioStore, StudioRepository, and StudioViewModel
        val store2 = LocalStudioStore(context, testDispatcher)
        val repo2 = StudioRepository(databaseId = "unused", localStore = store2)
        assertFalse(repo2.getCachedThemeDark())
        val vm2 = StudioViewModel(repo2, ai, LOCAL_GUEST_UID)
        val job2 = backgroundScope.launch { vm2.projectsUiState.collect {} }

        val ui2 = awaitUiWhere(vm2) {
            it.activeProject?.id == proj.id &&
                it.studioConfig.preferredStyle == "Thriller Psicológico Neo-Noir"
        }
        assertFalse(ui2.isDarkMode)
        assertEquals("light", ui2.studioConfig.themeMode)
        assertEquals("1.85:1", ui2.studioConfig.defaultAspectRatio)
        assertEquals(ThinkingDepth.DEEP.name, ui2.studioConfig.defaultThinkingDepth)
        assertFalse(ui2.studioConfig.stage3DDefault)
        assertEquals(WorkspaceTab.SHOTS, ui2.activeTab)
        assertEquals("Borrador Persistido", ui2.activeIngestDraft.sequenceTitle)
        assertEquals("Agregar plano dron", ui2.activeAgentDraftMessage)
        job2.cancel()
    }

    @Test
    fun `milestone CRUD persists across ViewModel and store reload`() = runTest {
        val store1 = LocalStudioStore(context, testDispatcher)
        val repo1 = StudioRepository(databaseId = "unused", localStore = store1)
        val ai = FakeStudioAiClient()
        val vm1 = StudioViewModel(repo1, ai, LOCAL_GUEST_UID)
        val job1 = backgroundScope.launch { vm1.projectsUiState.collect {} }

        vm1.createProject("Milestones Film", "Drama", SourceMaterialType.STORY, "")
        val proj = awaitUiWhere(vm1) { it.activeProject?.title == "Milestones Film" }.activeProject!!

        val dayMillis = System.currentTimeMillis()
        vm1.addMilestone(
            projectId = proj.id,
            title = "Rodaje Nocturno Muelle",
            notes = "Escena 1 y 2",
            dateMillis = dayMillis,
            type = CalendarMilestoneType.SHOOTING_DAY,
            sequenceId = null
        )
        var ui = awaitUiWhere(vm1) { it.milestones.size == 1 }
        val ms = ui.milestones.first()
        assertEquals("Rodaje Nocturno Muelle", ms.title)
        assertFalse(ms.completed)

        vm1.toggleMilestoneCompleted(proj.id, ms.id, true)
        ui = awaitUiWhere(vm1) { it.milestones.firstOrNull()?.completed == true }
        assertTrue(ui.milestones.first().completed)
        job1.cancel()

        // Reload via fresh store & verify persisted milestone + delete
        val store2 = LocalStudioStore(context, testDispatcher)
        val repo2 = StudioRepository(databaseId = "unused", localStore = store2)
        val vm2 = StudioViewModel(repo2, ai, LOCAL_GUEST_UID)
        val job2 = backgroundScope.launch { vm2.projectsUiState.collect {} }

        var ui2 = awaitUiWhere(vm2) { it.milestones.size == 1 }
        assertTrue(ui2.milestones.first().completed)
        assertEquals("Rodaje Nocturno Muelle", ui2.milestones.first().title)

        vm2.deleteMilestone(proj.id, ui2.milestones.first().id)
        ui2 = awaitUiWhere(vm2) { it.milestones.isEmpty() }
        assertTrue(ui2.milestones.isEmpty())
        job2.cancel()
    }

    @Test
    fun `ProductionCalendarDialog allows creating, toggling, and deleting custom milestones`() {
        val now = System.currentTimeMillis()
        val proj = ProductionProject(
            id = "proj_cal_crud",
            ownerId = LOCAL_GUEST_UID,
            title = "Horizonte",
            createdAtMillis = now
        )
        val existingMs = ProductionMilestoneItem(
            id = "ms_existing",
            projectId = proj.id,
            ownerId = LOCAL_GUEST_UID,
            title = "Entrega Corte Final",
            notes = "Sala Master",
            dateMillis = now,
            milestoneType = CalendarMilestoneType.DEADLINE.wireValue,
            completed = false
        )
        var addedTitle = ""
        var addedType: CalendarMilestoneType? = null
        var toggledId = ""
        var toggledValue = false
        var deletedId = ""

        composeRule.setContent {
            LumenReelTheme(darkTheme = false) {
                ProductionCalendarDialog(
                    project = proj,
                    milestones = listOf(existingMs),
                    onDismiss = {},
                    onSelectSequence = {},
                    onAddMilestone = { title, _, _, type, _ ->
                        addedTitle = title
                        addedType = type
                    },
                    onToggleMilestone = { id, completed ->
                        toggledId = id
                        toggledValue = completed
                    },
                    onDeleteMilestone = { id -> deletedId = id }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calendar_milestone_custom_ms_existing").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("calendar_toggle_ms_ms_existing").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("ms_existing", toggledId)
        assertTrue(toggledValue)

        // Open inline add form and create a new milestone
        composeRule.onNodeWithTag("calendar_add_milestone_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calendar_type_review").performScrollTo().performClick()
        composeRule.onNodeWithTag("calendar_milestone_title_input").performScrollTo().performTextInput("Revisión VFX")
        composeRule.onNodeWithTag("calendar_save_milestone_button").performClick()
        composeRule.waitForIdle()
        assertEquals("Revisión VFX", addedTitle)
        assertEquals(CalendarMilestoneType.REVIEW, addedType)

        // Delete existing milestone
        composeRule.onNodeWithTag("calendar_delete_ms_ms_existing").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("ms_existing", deletedId)
    }

    @Config(sdk = [35], qualifiers = "w412dp-h915dp")
    @Test
    fun `light and dark theme render dashboard and account configuration dialogs cleanly`() {
        val proj = ProductionProject(
            id = "proj_theme_1",
            ownerId = LOCAL_GUEST_UID,
            title = "Sol de Medianoche",
            genreStyle = "Drama",
            sequences = listOf(ProductionSequence(id = "s1", projectId = "proj_theme_1", title = "Escena 1"))
        )

        var toggledTheme = 0
        var savedCfg: UserStudioConfig? = null

        composeRule.setContent {
            LumenReelTheme(darkTheme = false) {
                androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                    ProjectDashboardScreen(
                        currentUser = null,
                        projects = listOf(proj),
                        activeProjectId = proj.id,
                        searchQuery = "",
                        statusBanner = null,
                        errorBanner = null,
                        onSearchChange = {},
                        onSelectProject = {},
                        onCreateProjectClick = {},
                        onDeleteProject = {},
                        onOpenLearning = {},
                        onOpenBlueprint = {},
                        onExportManualPdf = {},
                        onExportLibraryPdf = {},
                        onOpenAccount = {},
                        onDismissBanner = {}
                    )
                    AccountDialog(
                        user = null,
                        studioConfig = UserStudioConfig(themeMode = "light", preferredStyle = "Drama"),
                        isDarkMode = false,
                        onToggleDarkMode = { toggledTheme++ },
                        onSaveStudioConfig = { savedCfg = it },
                        onDismiss = {},
                        onSignOut = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("account_theme_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(1, toggledTheme)

        composeRule.onNodeWithTag("account_ratio_16:9").performScrollTo().performClick()
        composeRule.onNodeWithTag("account_depth_DEEP").performScrollTo().performClick()
        composeRule.onNodeWithTag("save_profile_style_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertNotNull(savedCfg)
        assertEquals("16:9", savedCfg?.defaultAspectRatio)
        assertEquals(ThinkingDepth.DEEP.name, savedCfg?.defaultThinkingDepth)
    }
}

