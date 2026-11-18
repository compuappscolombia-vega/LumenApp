package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AgentChatOutcome
import com.example.data.ai.StudioAiClient
import com.example.data.model.AgentMessage
import com.example.data.model.AgentRoleFocus
import com.example.data.model.AgentToolAction
import com.example.data.model.AudioAnalysisReport
import com.example.data.model.DirectorDraftEdits
import com.example.data.model.IngestDraftState
import com.example.data.model.LightingSetup
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
import com.example.data.model.WorkspaceTab
import com.example.data.repository.LOCAL_GUEST_UID
import com.example.data.repository.LocalStudioStore
import com.example.data.repository.StudioRepository
import com.example.data.util.LearningEngine
import com.example.data.util.StudioLogic
import com.example.ui.components.SourceIngestContent
import com.example.ui.components.readClipboardText
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
        val store = LocalStudioStore(context)
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
        val store = LocalStudioStore(context)
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
        composeRule.onNodeWithTag("ingest_error_banner").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("ingest_paste_source_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(draft.sourceText.contains("Texto copiado desde el portapapeles"))

        composeRule.onNodeWithTag("create_manual_draft_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(manualClicked)
    }
}
