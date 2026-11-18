package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.StudioAiClient
import com.example.data.ai.StudioAiService
import com.example.data.model.AgentMessage
import com.example.data.model.AgentRoleFocus
import com.example.data.model.AudioAnalysisReport
import com.example.data.model.DirectorDraftEdits
import com.example.data.model.IngestDraftState
import com.example.data.model.LearningCategory
import com.example.data.model.LearningLesson
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.ScreenplayBeat
import com.example.data.model.ScreenplayScene
import com.example.data.model.SequenceVersionStatus
import com.example.data.model.SourceMaterialType
import com.example.data.model.StoryboardCard
import com.example.data.model.StudioIntelligenceReport
import com.example.data.model.ThinkingDepth
import com.example.data.model.WorkspaceTab
import com.example.data.repository.StudioRepository
import com.example.data.util.AgentLogic
import com.example.data.util.LearningEngine
import com.example.data.util.PdfExporter
import com.example.data.util.StudioIntelligence
import com.example.data.util.StudioLogic
import com.example.data.util.friendlyFirestoreMessage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "StudioViewModel"

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class StudioUiState(
    val projects: List<ProductionProject> = emptyList(),
    val activeProject: ProductionProject? = null,
    val activeSequence: ProductionSequence? = null,
    val agentMessages: List<AgentMessage> = emptyList(),
    val lessons: List<LearningLesson> = emptyList(),
    val intelligence: StudioIntelligenceReport? = null,
    val activeTab: WorkspaceTab = WorkspaceTab.AGENT,
    val searchQuery: String = "",
    val showDashboard: Boolean = true,
    val showIngestSheet: Boolean = false,
    val activeIngestDraft: IngestDraftState = IngestDraftState(),
    val activeDirectorDraft: DirectorDraftEdits = DirectorDraftEdits(),
    val activeVisionRating: Int = 5,
    val activeVisionRatingNote: String = "",
    val activeSelectedShotIndex: Int = 0,
    val activeStage3DMode: Boolean = true,
    val activeAgentFocus: AgentRoleFocus = AgentRoleFocus.COUNCIL,
    val activeAgentDraftMessage: String = "",
    val isProducing: Boolean = false,
    val isAnalyzingAudio: Boolean = false,
    val latestAudioReport: AudioAnalysisReport? = null,
    val isAgentBusy: Boolean = false,
    val isFinalizing: Boolean = false,
    val isExporting: Boolean = false,
    val generatingFrameId: String? = null,
    val statusBanner: String? = null,
    val errorBanner: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(
    private val repository: StudioRepository,
    private val aiService: StudioAiClient,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedProjectId = MutableStateFlow("")
    private val _selectedSequenceId = MutableStateFlow("")
    private val _activeTab = MutableStateFlow(WorkspaceTab.AGENT)
    private val _searchQuery = MutableStateFlow("")
    private val _showDashboard = MutableStateFlow(true)
    private val _showIngestSheet = MutableStateFlow(false)

    private val _sequencesByProject = MutableStateFlow<Map<String, List<ProductionSequence>>>(emptyMap())
    private val _localFramePaths = MutableStateFlow<Map<String, String>>(emptyMap())

    private val _ingestDrafts = MutableStateFlow<Map<String, IngestDraftState>>(emptyMap())
    private val _directorDraftsBySeq = MutableStateFlow<Map<String, DirectorDraftEdits>>(emptyMap())
    private val _visionRatingsBySeq = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _visionNotesBySeq = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _selectedShotIndexBySeq = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _stage3DModeBySeq = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val _agentFocusByProject = MutableStateFlow<Map<String, AgentRoleFocus>>(emptyMap())
    private val _agentDraftByProject = MutableStateFlow<Map<String, String>>(emptyMap())

    private val _isProducing = MutableStateFlow(false)
    private val _isAnalyzingAudio = MutableStateFlow(false)
    private val _latestAudioReport = MutableStateFlow<AudioAnalysisReport?>(null)
    private val _isAgentBusy = MutableStateFlow(false)
    private val _isFinalizing = MutableStateFlow(false)
    private val _isExporting = MutableStateFlow(false)
    private val _generatingFrameId = MutableStateFlow<String?>(null)
    private val _statusBanner = MutableStateFlow<String?>(null)
    private val _errorBanner = MutableStateFlow<String?>(null)

    private val projectsStream: StateFlow<UiState<List<ProductionProject>>> =
        repository.observeProjects(currentUserId)
            .map<List<ProductionProject>, UiState<List<ProductionProject>>> { list ->
                if (_selectedProjectId.value.isBlank() && list.isNotEmpty()) {
                    _selectedProjectId.value = list.first().id
                } else if (_selectedProjectId.value.isNotBlank() && list.none { it.id == _selectedProjectId.value }) {
                    _selectedProjectId.value = list.firstOrNull()?.id.orEmpty()
                }
                UiState.Success(list)
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                val root = fsErr ?: error
                Log.w(TAG, "Error observing projects", root)
                emit(UiState.Error(friendlyFirestoreMessage(root)))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val activeSequencesStream: StateFlow<List<ProductionSequence>> =
        _selectedProjectId.flatMapLatest { pid ->
            if (pid.isBlank()) flowOf(emptyList())
            else repository.observeSequences(currentUserId, pid)
                .map { seqs ->
                    _sequencesByProject.update { prev -> prev + (pid to seqs) }
                    if (_selectedSequenceId.value.isBlank() && seqs.isNotEmpty()) {
                        _selectedSequenceId.value = seqs.first().id
                    } else if (_selectedSequenceId.value.isNotBlank() && seqs.none { it.id == _selectedSequenceId.value }) {
                        _selectedSequenceId.value = seqs.firstOrNull()?.id.orEmpty()
                    }
                    seqs
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing sequences", fsErr ?: error)
                    _errorBanner.value = friendlyFirestoreMessage(fsErr ?: error)
                    emit(emptyList())
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val activeMessagesStream: StateFlow<List<AgentMessage>> =
        _selectedProjectId.flatMapLatest { pid ->
            if (pid.isBlank()) flowOf(emptyList())
            else repository.observeAgentMessages(currentUserId, pid)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing agent messages", fsErr ?: error)
                    emit(emptyList())
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val lessonsStream: StateFlow<List<LearningLesson>> =
        repository.observeLearningLessons(currentUserId)
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing lessons", fsErr ?: error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val busyFlags = combine(
        _isProducing,
        _isAnalyzingAudio,
        _isAgentBusy,
        _isFinalizing,
        _isExporting
    ) { producing, audio, agent, finalizing, exporting ->
        BusyState(producing, audio, agent, finalizing, exporting)
    }

    private val navState = combine(
        _selectedProjectId,
        _selectedSequenceId,
        _activeTab,
        _searchQuery,
        combine(_showDashboard, _showIngestSheet, busyFlags) { dash, ingest, busy ->
            Triple(dash, ingest, busy)
        }
    ) { pId, sId, tab, query, shell ->
        SelectionState(pId, sId, tab, query, shell.first, shell.second, shell.third)
    }

    private val bannersState = combine(
        _generatingFrameId,
        _statusBanner,
        _errorBanner,
        _latestAudioReport,
        _localFramePaths
    ) { genFrame, status, err, audioReport, framePaths ->
        BannerState(genFrame, status, err, audioReport, framePaths)
    }

    private val tabDraftsState = combine(
        combine(_ingestDrafts, _directorDraftsBySeq) { ingest, dir -> ingest to dir },
        combine(_visionRatingsBySeq, _visionNotesBySeq) { ratings, notes -> ratings to notes },
        combine(_selectedShotIndexBySeq, _stage3DModeBySeq) { shotIdx, stage3d -> shotIdx to stage3d },
        combine(_agentFocusByProject, _agentDraftByProject) { focus, msg -> focus to msg }
    ) { ingestDir, vision, shots, agent ->
        TabDraftsSnapshot(
            ingestDrafts = ingestDir.first,
            directorDraftsBySeq = ingestDir.second,
            visionRatingsBySeq = vision.first,
            visionNotesBySeq = vision.second,
            selectedShotIndexBySeq = shots.first,
            stage3DModeBySeq = shots.second,
            agentFocusByProject = agent.first,
            agentDraftByProject = agent.second
        )
    }

    private val repoDataStream = combine(
        projectsStream,
        activeSequencesStream,
        activeMessagesStream,
        lessonsStream
    ) { projResult, activeSeqs, messages, lessons ->
        RepoSnapshot(projResult, activeSeqs, messages, lessons)
    }

    val projectsUiState: StateFlow<UiState<StudioUiState>> = combine(
        repoDataStream,
        navState,
        bannersState,
        tabDraftsState
    ) { repoData, sel, banners, drafts ->
        when (val projResult = repoData.projectsResult) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> UiState.Error(projResult.message)
            is UiState.Success -> {
                val cachedMap = _sequencesByProject.value
                val enrichedProjects = projResult.data.map { proj ->
                    val rawSeqs = if (proj.id == sel.selectedProjectId) repoData.activeSeqs else (cachedMap[proj.id] ?: emptyList())
                    val mergedSeqs = rawSeqs.map { seq ->
                        seq.copy(
                            storyboardFrames = seq.storyboardFrames.map { frame ->
                                val overridePath = banners.localFramePaths[frame.id]
                                if (!overridePath.isNullOrBlank()) frame.copy(localImagePath = overridePath) else frame
                            }
                        )
                    }
                    proj.copy(
                        sequences = mergedSeqs,
                        agentMessages = if (proj.id == sel.selectedProjectId) repoData.messages else emptyList()
                    )
                }
                val activeProject = enrichedProjects.firstOrNull { it.id == sel.selectedProjectId }
                    ?: enrichedProjects.firstOrNull()
                val activeSequence = activeProject?.sequences?.firstOrNull { it.id == sel.selectedSequenceId }
                    ?: activeProject?.sequences?.firstOrNull()

                val intelligence = activeSequence?.let { seq ->
                    StudioIntelligence.buildStudioIntelligence(
                        projects = enrichedProjects,
                        lessons = repoData.lessons,
                        activeProject = activeProject,
                        activeSequence = seq
                    )
                }

                val ingestDraft = activeProject?.let { proj ->
                    drafts.ingestDrafts[proj.id] ?: defaultIngestDraft(proj)
                } ?: IngestDraftState()

                val directorDraft = if (activeSequence != null) {
                    drafts.directorDraftsBySeq[activeSequence.id]
                        ?: defaultDirectorDraft(activeProject, activeSequence)
                } else {
                    DirectorDraftEdits()
                }

                val visionRating = activeSequence?.let { seq ->
                    drafts.visionRatingsBySeq[seq.id] ?: seq.sequenceRating ?: 5
                } ?: 5

                val visionNote = activeSequence?.let { seq ->
                    drafts.visionNotesBySeq[seq.id].orEmpty()
                }.orEmpty()

                val shotIdx = activeSequence?.let { seq ->
                    val raw = drafts.selectedShotIndexBySeq[seq.id] ?: 0
                    if (seq.shotList.isEmpty()) 0 else raw.coerceIn(0, seq.shotList.lastIndex)
                } ?: 0

                val stage3D = activeSequence?.let { seq ->
                    drafts.stage3DModeBySeq[seq.id] ?: true
                } ?: true

                val agentFocus = activeProject?.let { proj ->
                    drafts.agentFocusByProject[proj.id] ?: AgentRoleFocus.COUNCIL
                } ?: AgentRoleFocus.COUNCIL

                val agentDraftMsg = activeProject?.let { proj ->
                    drafts.agentDraftByProject[proj.id].orEmpty()
                }.orEmpty()

                UiState.Success(
                    StudioUiState(
                        projects = enrichedProjects,
                        activeProject = activeProject,
                        activeSequence = activeSequence,
                        agentMessages = repoData.messages,
                        lessons = repoData.lessons,
                        intelligence = intelligence,
                        activeTab = sel.activeTab,
                        searchQuery = sel.searchQuery,
                        showDashboard = sel.showDashboard,
                        showIngestSheet = sel.showIngestSheet,
                        activeIngestDraft = ingestDraft,
                        activeDirectorDraft = directorDraft,
                        activeVisionRating = visionRating,
                        activeVisionRatingNote = visionNote,
                        activeSelectedShotIndex = shotIdx,
                        activeStage3DMode = stage3D,
                        activeAgentFocus = agentFocus,
                        activeAgentDraftMessage = agentDraftMsg,
                        isProducing = sel.busy.producing,
                        isAnalyzingAudio = sel.busy.audio,
                        latestAudioReport = banners.audioReport,
                        isAgentBusy = sel.busy.agent,
                        isFinalizing = sel.busy.finalizing,
                        isExporting = sel.busy.exporting,
                        generatingFrameId = banners.generatingFrameId,
                        statusBanner = banners.statusBanner,
                        errorBanner = banners.errorBanner
                    )
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private fun defaultIngestDraft(project: ProductionProject): IngestDraftState {
        val preset = StudioLogic.DIRECTOR_STYLES.firstOrNull {
            project.genreStyle.contains(it.label, ignoreCase = true)
        } ?: StudioLogic.DIRECTOR_STYLES.first()
        return IngestDraftState(
            sourceType = project.defaultSourceType,
            directorStyle = preset.label,
            aspectRatio = preset.defaultRatio,
            continuityBible = project.continuityBible,
            bibleInitialized = true
        )
    }

    private fun defaultDirectorDraft(
        project: ProductionProject?,
        seq: ProductionSequence
    ): DirectorDraftEdits = DirectorDraftEdits(
        directorStyle = seq.directorStyle,
        aspectRatio = seq.aspectRatio,
        cameraSystem = seq.cameraSystem,
        lensPackage = seq.lensPackage,
        filmStockOrLut = seq.filmStockOrLut,
        colorTempKelvin = seq.lighting.colorTemperatureKelvin,
        directorVision = seq.directorVision,
        continuityBible = project?.continuityBible.orEmpty(),
        isDirty = false
    )

    fun setShowDashboard(show: Boolean) {
        _showDashboard.value = show
    }

    fun setShowIngestSheet(show: Boolean) {
        _showIngestSheet.value = show
    }

    fun selectProject(projectId: String) {
        _selectedProjectId.value = projectId
        _selectedSequenceId.value = ""
        _latestAudioReport.value = null
    }

    fun selectSequence(sequenceId: String) {
        _selectedSequenceId.value = sequenceId
    }

    fun selectTab(tab: WorkspaceTab) {
        _activeTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearBanners() {
        _statusBanner.value = null
        _errorBanner.value = null
    }

    fun updateIngestDraft(
        project: ProductionProject,
        transform: (IngestDraftState) -> IngestDraftState
    ) {
        _ingestDrafts.update { map ->
            val current = map[project.id] ?: defaultIngestDraft(project)
            map + (project.id to transform(current))
        }
    }

    fun updateDirectorDraft(
        project: ProductionProject,
        sequence: ProductionSequence,
        transform: (DirectorDraftEdits) -> DirectorDraftEdits
    ) {
        _directorDraftsBySeq.update { map ->
            val current = map[sequence.id] ?: defaultDirectorDraft(project, sequence)
            map + (sequence.id to transform(current).copy(isDirty = true))
        }
    }

    fun saveDirectorDraftEdits(
        project: ProductionProject,
        sequence: ProductionSequence
    ) {
        val draft = _directorDraftsBySeq.value[sequence.id] ?: defaultDirectorDraft(project, sequence)
        viewModelScope.launch {
            _errorBanner.value = null
            if (draft.continuityBible != project.continuityBible) {
                repository.updateProjectContinuity(currentUserId, project.id, draft.continuityBible)
                    .onFailure { err ->
                        _errorBanner.value = friendlyFirestoreMessage(err)
                        return@launch
                    }
            }
            if (sequence.versionStatus == SequenceVersionStatus.DRAFT) {
                val updatedSeq = sequence.copy(
                    directorStyle = draft.directorStyle.trim().ifBlank { sequence.directorStyle },
                    aspectRatio = draft.aspectRatio.trim().ifBlank { sequence.aspectRatio },
                    cameraSystem = draft.cameraSystem.trim().ifBlank { sequence.cameraSystem },
                    lensPackage = draft.lensPackage.trim().ifBlank { sequence.lensPackage },
                    filmStockOrLut = draft.filmStockOrLut.trim().ifBlank { sequence.filmStockOrLut },
                    lighting = sequence.lighting.copy(
                        colorTemperatureKelvin = draft.colorTempKelvin.trim()
                    ),
                    directorVision = draft.directorVision.trim()
                )
                repository.updateDraftSequence(currentUserId, project.id, updatedSeq)
                    .onSuccess {
                        _directorDraftsBySeq.update { it - sequence.id }
                        _statusBanner.value = "Ajustes de dirección guardados en «${sequence.title}»."
                    }
                    .onFailure { err ->
                        _errorBanner.value = friendlyFirestoreMessage(err)
                    }
            } else {
                _directorDraftsBySeq.update { it - sequence.id }
                _statusBanner.value = "Biblia de continuidad actualizada (la secuencia Final es inmutable)."
            }
        }
    }

    fun updateVisionRatingDraft(sequenceId: String, rating: Int) {
        _visionRatingsBySeq.update { it + (sequenceId to rating.coerceIn(1, 5)) }
    }

    fun updateVisionNoteDraft(sequenceId: String, note: String) {
        _visionNotesBySeq.update { it + (sequenceId to note) }
    }

    fun updateSelectedShotIndex(sequenceId: String, index: Int) {
        _selectedShotIndexBySeq.update { it + (sequenceId to index.coerceAtLeast(0)) }
    }

    fun updateStage3DMode(sequenceId: String, use3D: Boolean) {
        _stage3DModeBySeq.update { it + (sequenceId to use3D) }
    }

    fun updateAgentFocus(projectId: String, focus: AgentRoleFocus) {
        _agentFocusByProject.update { it + (projectId to focus) }
    }

    fun updateAgentDraftMessage(projectId: String, text: String) {
        _agentDraftByProject.update { it + (projectId to text) }
    }

    fun createProject(
        title: String,
        genreStyle: String,
        defaultSourceType: SourceMaterialType,
        continuityBible: String
    ) {
        viewModelScope.launch {
            _errorBanner.value = null
            repository.createProject(currentUserId, title, genreStyle, defaultSourceType, continuityBible)
                .onSuccess { newId ->
                    _selectedProjectId.value = newId
                    _selectedSequenceId.value = ""
                    _activeTab.value = WorkspaceTab.AGENT
                    _showDashboard.value = false
                    _statusBanner.value = "Producción \"$title\" creada en el estudio."
                }
                .onFailure { err ->
                    _errorBanner.value = friendlyFirestoreMessage(err)
                }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            _errorBanner.value = null
            repository.deleteProject(currentUserId, projectId)
                .onSuccess {
                    _ingestDrafts.update { it - projectId }
                    _agentFocusByProject.update { it - projectId }
                    _agentDraftByProject.update { it - projectId }
                    _statusBanner.value = "Producción eliminada."
                }
                .onFailure { err ->
                    _errorBanner.value = friendlyFirestoreMessage(err)
                }
        }
    }

    fun saveContinuityBible(projectId: String, bibleText: String) {
        viewModelScope.launch {
            _errorBanner.value = null
            repository.updateProjectContinuity(currentUserId, projectId, bibleText)
                .onSuccess {
                    _ingestDrafts.update { map ->
                        val existing = map[projectId]
                        if (existing != null) map + (projectId to existing.copy(continuityBible = bibleText)) else map
                    }
                    _statusBanner.value = "Biblia de continuidad actualizada."
                }
                .onFailure { err ->
                    _errorBanner.value = friendlyFirestoreMessage(err)
                }
        }
    }

    fun analyzeAudioSource(
        projectId: String,
        filename: String,
        mimeType: String,
        audioBytes: ByteArray,
        lessons: List<LearningLesson>
    ) {
        viewModelScope.launch {
            _isAnalyzingAudio.value = true
            _errorBanner.value = null
            _statusBanner.value = null
            _ingestDrafts.update { map ->
                val cur = map[projectId]
                if (cur != null) map + (projectId to cur.copy(ingestError = null)) else map
            }
            try {
                val learningContext = LearningEngine.buildLearningPromptContext(lessons, 6)
                val report = aiService.analyzeAudio(filename, mimeType, audioBytes, learningContext)
                _latestAudioReport.value = report
                _statusBanner.value = "Análisis y transcripción verbatim listos para \"$filename\"."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Audio analysis failed", e)
                val msg = AgentLogic.formatAiError(e)
                _errorBanner.value = msg
                _ingestDrafts.update { map ->
                    val cur = map[projectId]
                    if (cur != null) map + (projectId to cur.copy(ingestError = msg)) else map
                }
            } finally {
                _isAnalyzingAudio.value = false
            }
        }
    }

    fun produceSequence(
        project: ProductionProject,
        lessons: List<LearningLesson>,
        sequenceTitle: String,
        sourceType: SourceMaterialType,
        directorStyle: String,
        aspectRatio: String,
        thinkingDepth: ThinkingDepth,
        continuityBible: String,
        sourceText: String,
        attachedFilename: String?,
        attachedMimeType: String?,
        attachedBytes: ByteArray?,
        onDone: () -> Unit = {}
    ) {
        val validationErrors = StudioLogic.validateSequenceInput(
            sequenceTitle = sequenceTitle,
            sourceText = sourceText,
            hasFile = attachedBytes != null
        )
        if (validationErrors.isNotEmpty()) {
            val msg = validationErrors.first()
            _errorBanner.value = msg
            updateIngestDraft(project) { it.copy(ingestError = msg) }
            return
        }

        viewModelScope.launch {
            _isProducing.value = true
            _errorBanner.value = null
            _statusBanner.value = null
            updateIngestDraft(project) { it.copy(ingestError = null) }
            try {
                val learningContext = LearningEngine.buildLearningPromptContext(lessons, 8)
                val produced = aiService.produceSequence(
                    userId = currentUserId,
                    project = project,
                    sequenceTitle = sequenceTitle,
                    sourceType = sourceType,
                    directorStyle = directorStyle,
                    aspectRatio = aspectRatio,
                    thinkingDepth = thinkingDepth,
                    continuityBible = continuityBible,
                    sourceText = sourceText,
                    learningContext = learningContext,
                    attachedFilename = attachedFilename,
                    attachedMimeType = attachedMimeType,
                    attachedBytes = attachedBytes
                )

                val nextBible = StudioLogic.buildContinuationContext(
                    continuityBible.ifBlank { project.continuityBible },
                    sequenceTitle,
                    produced.continuitySummary
                )

                repository.saveProducedSequence(
                    userId = currentUserId,
                    projectId = project.id,
                    sequence = produced,
                    nextContinuityBible = nextBible,
                    nextSequencesCount = project.sequences.size + 1
                ).onSuccess { saved ->
                    _selectedSequenceId.value = saved.id
                    _activeTab.value = WorkspaceTab.SCREENPLAY
                    _showIngestSheet.value = false
                    _ingestDrafts.update { it - project.id }
                    _statusBanner.value = "Secuencia \"${saved.title}\" producida (${saved.screenplay.size} escenas, ${saved.shotList.size} planos)."
                    onDone()
                }.onFailure { err ->
                    val msg = friendlyFirestoreMessage(err)
                    _errorBanner.value = msg
                    updateIngestDraft(project) { it.copy(ingestError = msg) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Sequence production failed", e)
                val msg = AgentLogic.formatAiError(e)
                _errorBanner.value = msg
                updateIngestDraft(project) { it.copy(ingestError = msg) }
            } finally {
                _isProducing.value = false
            }
        }
    }

    fun createManualDraftSequence(
        project: ProductionProject,
        sequenceTitle: String,
        sourceType: SourceMaterialType,
        directorStyle: String,
        aspectRatio: String,
        continuityBible: String,
        sourceText: String,
        attachedFilename: String? = null,
        attachedMimeType: String? = null,
        attachedBytes: ByteArray? = null,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isProducing.value = true
            _errorBanner.value = null
            updateIngestDraft(project) { it.copy(ingestError = null) }
            try {
                val textPreview = if (
                    attachedBytes != null &&
                    StudioLogic.isSupportedSourceFile(attachedFilename, attachedMimeType) &&
                    !attachedMimeType.orEmpty().startsWith("audio/") &&
                    attachedFilename?.lowercase()?.endsWith(".pdf") != true
                ) {
                    attachedBytes.toString(Charsets.UTF_8).take(12_000)
                } else null

                val manualSeq = StudioLogic.buildManualDraftSequence(
                    userId = currentUserId,
                    project = project,
                    sequenceTitle = sequenceTitle,
                    sourceType = sourceType,
                    directorStyle = directorStyle,
                    aspectRatio = aspectRatio,
                    sourceText = sourceText,
                    attachedFilename = attachedFilename,
                    attachedTextPreview = textPreview
                )
                val nextBible = continuityBible.trim().ifBlank { project.continuityBible }
                repository.saveProducedSequence(
                    userId = currentUserId,
                    projectId = project.id,
                    sequence = manualSeq,
                    nextContinuityBible = nextBible,
                    nextSequencesCount = project.sequences.size + 1
                ).onSuccess { saved ->
                    _selectedSequenceId.value = saved.id
                    _activeTab.value = WorkspaceTab.SCREENPLAY
                    _showIngestSheet.value = false
                    _ingestDrafts.update { it - project.id }
                    _statusBanner.value = "Borrador manual \"${saved.title}\" guardado en Guion."
                    onDone()
                }.onFailure { err ->
                    val msg = friendlyFirestoreMessage(err)
                    _errorBanner.value = msg
                    updateIngestDraft(project) { it.copy(ingestError = msg) }
                }
            } finally {
                _isProducing.value = false
            }
        }
    }

    fun saveScreenplaySceneDraft(
        projectId: String,
        sequence: ProductionSequence,
        sceneNumber: String,
        slugline: String,
        timeOfDay: String,
        actionText: String,
        dialogueCharacter: String,
        dialogueText: String,
        editingIndex: Int?
    ) {
        if (sequence.versionStatus != SequenceVersionStatus.DRAFT) {
            _errorBanner.value = "La secuencia está aprobada como Versión Final y es de solo lectura."
            return
        }
        val cleanSlug = slugline.trim().ifBlank { "INT. ESCENA - NOCHE" }.uppercase()
        val beats = mutableListOf<ScreenplayBeat>()
        if (actionText.isNotBlank()) {
            beats.add(ScreenplayBeat(type = "action", text = actionText.trim()))
        }
        if (dialogueText.isNotBlank()) {
            beats.add(
                ScreenplayBeat(
                    type = "dialogue",
                    character = dialogueCharacter.trim().ifBlank { "PERSONAJE" }.uppercase(),
                    text = dialogueText.trim()
                )
            )
        }
        if (beats.isEmpty()) {
            _errorBanner.value = "Escribe al menos una acción o diálogo para la escena."
            return
        }
        val targetNumber = sceneNumber.trim().ifBlank {
            if (editingIndex != null && editingIndex in sequence.screenplay.indices) {
                sequence.screenplay[editingIndex].sceneNumber
            } else {
                (sequence.screenplay.size + 1).toString()
            }
        }
        val nextScene = ScreenplayScene(
            sceneNumber = targetNumber,
            slugline = cleanSlug,
            timeOfDay = timeOfDay.trim().ifBlank { "NOCHE" }.uppercase(),
            beats = beats
        )
        val updatedList = sequence.screenplay.toMutableList()
        if (editingIndex != null && editingIndex in updatedList.indices) {
            updatedList[editingIndex] = nextScene
        } else {
            updatedList.add(nextScene)
        }
        val updatedSequence = sequence.copy(screenplay = updatedList)
        viewModelScope.launch {
            _errorBanner.value = null
            repository.updateDraftSequence(currentUserId, projectId, updatedSequence)
                .onSuccess {
                    _statusBanner.value = "Escena $targetNumber guardada en «${sequence.title}»."
                }
                .onFailure { err ->
                    _errorBanner.value = friendlyFirestoreMessage(err)
                }
        }
    }

    fun sendAgentMessage(
        project: ProductionProject,
        activeSequence: ProductionSequence?,
        recentMessages: List<AgentMessage>,
        lessons: List<LearningLesson>,
        focus: AgentRoleFocus,
        userText: String
    ) {
        val clean = userText.trim()
        if (clean.isEmpty() || _isAgentBusy.value) return

        viewModelScope.launch {
            _isAgentBusy.value = true
            _errorBanner.value = null
            try {
                val learningContext = LearningEngine.buildLearningPromptContext(lessons, 6)
                val outcome = aiService.chatWithAgent(
                    project = project,
                    activeSequence = activeSequence,
                    focus = focus,
                    userMessage = clean,
                    learningContext = learningContext,
                    recentHistory = recentMessages
                )

                if (outcome.bibleChanged) {
                    repository.updateProjectContinuity(
                        userId = currentUserId,
                        projectId = project.id,
                        continuityBible = outcome.updatedProject.continuityBible
                    )
                }
                if (outcome.updatedDraftSequence != null &&
                    outcome.updatedDraftSequence.versionStatus == SequenceVersionStatus.DRAFT
                ) {
                    repository.updateDraftSequence(
                        userId = currentUserId,
                        projectId = project.id,
                        sequence = outcome.updatedDraftSequence
                    )
                }

                repository.saveAgentMessagePair(
                    userId = currentUserId,
                    projectId = project.id,
                    focus = focus,
                    userContent = clean,
                    agentContent = outcome.replyText,
                    actions = outcome.actions
                ).onFailure { err ->
                    _errorBanner.value = friendlyFirestoreMessage(err)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Agent chat failed", e)
                _errorBanner.value = AgentLogic.formatAiError(e)
            } finally {
                _isAgentBusy.value = false
            }
        }
    }

    fun clearAgentHistory(projectId: String) {
        viewModelScope.launch {
            repository.clearAgentMessages(currentUserId, projectId)
                .onSuccess { _statusBanner.value = "Historial del Agente reiniciado." }
                .onFailure { err -> _errorBanner.value = friendlyFirestoreMessage(err) }
        }
    }

    fun approveFinalVersion(project: ProductionProject, sequence: ProductionSequence) {
        if (sequence.versionStatus == SequenceVersionStatus.FINAL) return
        viewModelScope.launch {
            _isFinalizing.value = true
            _errorBanner.value = null
            repository.finalizeSequence(currentUserId, project.id, project.title, sequence)
                .onSuccess {
                    _statusBanner.value = "Versión Final aprobada. Exportación Bitácora PDF desbloqueada."
                }
                .onFailure { err ->
                    _errorBanner.value = friendlyFirestoreMessage(err)
                }
            _isFinalizing.value = false
        }
    }

    fun rateSequence(
        project: ProductionProject,
        sequence: ProductionSequence,
        rating: Int,
        note: String
    ) {
        viewModelScope.launch {
            _errorBanner.value = null
            repository.rateSequence(currentUserId, project.id, project.title, sequence, rating, note)
                .onSuccess {
                    _visionNotesBySeq.update { it + (sequence.id to "") }
                    _statusBanner.value = if (rating >= 4) {
                        "Valoración registrada en Studio Intelligence ($rating/5 ★)."
                    } else {
                        "Valoración guardada ($rating/5 ★)."
                    }
                }
                .onFailure { err ->
                    _errorBanner.value = friendlyFirestoreMessage(err)
                }
        }
    }

    fun applyDirectorialEnhancement(project: ProductionProject, snippet: String) {
        val nextBible = if (project.continuityBible.trim().isEmpty()) {
            snippet
        } else if (project.continuityBible.contains(snippet)) {
            project.continuityBible
        } else {
            "${project.continuityBible.trim()}\n\n$snippet"
        }
        saveContinuityBible(project.id, nextBible)
    }

    fun generateStoryboardImage(
        context: Context,
        projectId: String,
        sequence: ProductionSequence,
        frame: StoryboardCard
    ) {
        viewModelScope.launch {
            _generatingFrameId.value = frame.id
            _errorBanner.value = null
            try {
                val imagePath = aiService.generateStoryboardImage(context, frame, sequence.aspectRatio)
                _localFramePaths.update { it + (frame.id to imagePath) }
                repository.saveStoryboardFrameImagePath(
                    userId = currentUserId,
                    projectId = projectId,
                    sequence = sequence,
                    frameId = frame.id,
                    localImagePath = imagePath
                )
                _statusBanner.value = "Fotograma generado con Nano para \"${frame.title}\"."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Nano image generation failed", e)
                _errorBanner.value = AgentLogic.formatAiError(e)
            } finally {
                _generatingFrameId.value = null
            }
        }
    }

    fun addLearningLesson(
        category: LearningCategory,
        insight: String,
        rating: Int,
        sourceProjectTitle: String
    ) {
        val error = LearningEngine.validateLessonInput(insight, rating)
        if (error != null) {
            _errorBanner.value = error
            return
        }
        viewModelScope.launch {
            repository.addLearningLesson(currentUserId, category, insight, rating, sourceProjectTitle)
                .onSuccess { _statusBanner.value = "Regla guardada en memoria de autoaprendizaje." }
                .onFailure { err -> _errorBanner.value = friendlyFirestoreMessage(err) }
        }
    }

    fun deleteLearningLesson(lessonId: String) {
        viewModelScope.launch {
            repository.deleteLearningLesson(currentUserId, lessonId)
                .onFailure { err -> _errorBanner.value = friendlyFirestoreMessage(err) }
        }
    }

    fun exportSequencePdf(
        context: Context,
        project: ProductionProject,
        sequence: ProductionSequence,
        onReadyToShare: (File, String) -> Unit
    ) {
        viewModelScope.launch {
            _isExporting.value = true
            _errorBanner.value = null
            try {
                val file = PdfExporter.exportSequencePdf(context, project, sequence)
                onReadyToShare(file, "application/pdf")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorBanner.value = e.localizedMessage ?: "No se pudo generar la Bitácora PDF."
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportLibraryPdf(
        context: Context,
        projects: List<ProductionProject>,
        onReadyToShare: (File, String) -> Unit
    ) {
        viewModelScope.launch {
            _isExporting.value = true
            _errorBanner.value = null
            try {
                val file = PdfExporter.exportLibraryPdf(context, projects)
                onReadyToShare(file, "application/pdf")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorBanner.value = e.localizedMessage ?: "No se pudo exportar la Biblioteca PDF."
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportUserManualPdf(
        context: Context,
        onReadyToShare: (File, String) -> Unit
    ) {
        viewModelScope.launch {
            _isExporting.value = true
            _errorBanner.value = null
            try {
                val file = PdfExporter.exportUserManualPdf(context)
                onReadyToShare(file, "application/pdf")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorBanner.value = e.localizedMessage ?: "No se pudo generar el Manual PDF."
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportScreenplayText(
        context: Context,
        projectTitle: String,
        sequence: ProductionSequence,
        fountain: Boolean,
        onReadyToShare: (File, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val slug = sequence.title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "guion" }
                val text = if (fountain) {
                    StudioLogic.formatFountainScreenplay(projectTitle, sequence)
                } else {
                    StudioLogic.formatScreenplayMarkdown(projectTitle, sequence)
                }
                val filename = if (fountain) "$slug.fountain" else "$slug.md"
                val mime = if (fountain) "text/plain" else "text/markdown"
                val file = PdfExporter.saveTextExport(context, filename, text)
                onReadyToShare(file, mime)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorBanner.value = e.localizedMessage ?: "No se pudo exportar el guion."
            }
        }
    }

    private data class BusyState(
        val producing: Boolean,
        val audio: Boolean,
        val agent: Boolean,
        val finalizing: Boolean,
        val exporting: Boolean
    )

    private data class SelectionState(
        val selectedProjectId: String,
        val selectedSequenceId: String,
        val activeTab: WorkspaceTab,
        val searchQuery: String,
        val showDashboard: Boolean,
        val showIngestSheet: Boolean,
        val busy: BusyState
    )

    private data class BannerState(
        val generatingFrameId: String?,
        val statusBanner: String?,
        val errorBanner: String?,
        val audioReport: AudioAnalysisReport?,
        val localFramePaths: Map<String, String>
    )

    private data class RepoSnapshot(
        val projectsResult: UiState<List<ProductionProject>>,
        val activeSeqs: List<ProductionSequence>,
        val messages: List<AgentMessage>,
        val lessons: List<LearningLesson>
    )

    private data class TabDraftsSnapshot(
        val ingestDrafts: Map<String, IngestDraftState>,
        val directorDraftsBySeq: Map<String, DirectorDraftEdits>,
        val visionRatingsBySeq: Map<String, Int>,
        val visionNotesBySeq: Map<String, String>,
        val selectedShotIndexBySeq: Map<String, Int>,
        val stage3DModeBySeq: Map<String, Boolean>,
        val agentFocusByProject: Map<String, AgentRoleFocus>,
        val agentDraftByProject: Map<String, String>
    )

    companion object {
        fun provideFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                StudioViewModel(
                    repository = StudioRepository(app),
                    aiService = StudioAiService(),
                    currentUserId = currentUserId
                )
            }
        }
    }
}
