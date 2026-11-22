package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
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
import com.example.data.model.CalendarMilestoneType
import com.example.data.model.DirectorDraftEdits
import com.example.data.model.IngestDraftState
import com.example.data.model.LearningCategory
import com.example.data.model.LearningLesson
import com.example.data.model.ProductionFolder
import com.example.data.model.ProductionMilestoneItem
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.ScreenplayBeat
import com.example.data.model.ScreenplayScene
import com.example.data.model.SequenceVersionStatus
import com.example.data.model.SourceMaterialType
import com.example.data.model.StoryboardCard
import com.example.data.model.StudioIntelligenceReport
import com.example.data.model.ThinkingDepth
import com.example.data.model.UserStudioConfig
import com.example.data.model.WorkspaceTab
import com.example.data.repository.LocalWorkspaceDraftSnapshot
import com.example.data.repository.PersistedDirectorDraft
import com.example.data.repository.PersistedIngestDraft
import com.example.data.repository.StudioRepository
import com.example.data.repository.UserProfileState
import com.example.data.util.AgentLogic
import com.example.data.util.LearningEngine
import com.example.data.util.PdfExporter
import com.example.data.util.ProjectTemplateCatalog
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
    val userProfile: UserProfileState? = null,
    val activeProject: ProductionProject? = null,
    val activeSequence: ProductionSequence? = null,
    val agentMessages: List<AgentMessage> = emptyList(),
    val lessons: List<LearningLesson> = emptyList(),
    val intelligence: StudioIntelligenceReport? = null,
    val activeTab: WorkspaceTab = WorkspaceTab.AGENT,
    val searchQuery: String = "",
    val showDashboard: Boolean = true,
    val showMetricsDashboard: Boolean = false,
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
    val isSavingToDb: Boolean = false,
    val isExporting: Boolean = false,
    val generatingFrameId: String? = null,
    val isDarkMode: Boolean = true,
    val showCalendarDialog: Boolean = false,
    val milestones: List<ProductionMilestoneItem> = emptyList(),
    val studioConfig: UserStudioConfig = UserStudioConfig(),
    val statusBanner: String? = null,
    val errorBanner: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(
    private val repository: StudioRepository,
    private val aiService: StudioAiClient,
    private val currentUserId: String
) : ViewModel() {

    private val session = repository.sessionState

    private val _selectedProjectId get() = session.selectedProjectId
    private val _selectedSequenceId get() = session.selectedSequenceId
    private val _activeTab get() = session.activeTab
    private val _searchQuery get() = session.searchQuery
    private val _showDashboard get() = session.showDashboard
    private val _showMetricsDashboard get() = session.showMetricsDashboard
    private val _showIngestSheet get() = session.showIngestSheet

    private val _sequencesByProject get() = session.sequencesByProject
    private val _localFramePaths get() = session.localFramePaths

    private val _ingestDrafts get() = session.ingestDrafts
    private val _directorDraftsBySeq get() = session.directorDraftsBySeq
    private val _visionRatingsBySeq get() = session.visionRatingsBySeq
    private val _visionNotesBySeq get() = session.visionNotesBySeq
    private val _selectedShotIndexBySeq get() = session.selectedShotIndexBySeq
    private val _stage3DModeBySeq get() = session.stage3DModeBySeq
    private val _agentFocusByProject get() = session.agentFocusByProject
    private val _agentDraftByProject get() = session.agentDraftByProject

    private val _isProducing = MutableStateFlow(false)
    private val _isAnalyzingAudio = MutableStateFlow(false)
    private val _latestAudioReport = MutableStateFlow<AudioAnalysisReport?>(null)
    private val _isAgentBusy = MutableStateFlow(false)
    private val _isFinalizing = MutableStateFlow(false)
    private val _isSavingToDb = MutableStateFlow(false)
    private val _isExporting = MutableStateFlow(false)
    private val _generatingFrameId = MutableStateFlow<String?>(null)
    private val _studioConfig = MutableStateFlow(repository.getCachedUserConfig(currentUserId))
    private val _isDarkMode = MutableStateFlow(_studioConfig.value.isDarkMode)
    private val _showCalendarDialog = MutableStateFlow(false)
    private val _statusBanner = MutableStateFlow<String?>(null)
    private val _errorBanner = MutableStateFlow<String?>(null)
    @Volatile
    private var isSavingConfig = false

    init {
        val saved = repository.loadWorkspaceDrafts(currentUserId)
        val hasActiveSession = _selectedProjectId.value.isNotBlank() ||
            _ingestDrafts.value.isNotEmpty() ||
            _directorDraftsBySeq.value.isNotEmpty() ||
            _agentDraftByProject.value.isNotEmpty()
        if (!hasActiveSession) {
            if (saved.selectedProjectId.isNotBlank()) {
                _selectedProjectId.value = saved.selectedProjectId
            }
            if (saved.selectedSequenceId.isNotBlank()) {
                _selectedSequenceId.value = saved.selectedSequenceId
            }
            _activeTab.value = WorkspaceTab.fromRouteKey(saved.activeTab)
            _searchQuery.value = saved.searchQuery
            _showDashboard.value = saved.showDashboard
            if (saved.ingestDrafts.isNotEmpty()) {
                _ingestDrafts.value = saved.ingestDrafts.mapValues { it.value.toModel() }
            }
            if (saved.directorDraftsBySeq.isNotEmpty()) {
                _directorDraftsBySeq.value = saved.directorDraftsBySeq.mapValues { it.value.toModel() }
            }
            if (saved.visionRatingsBySeq.isNotEmpty()) {
                _visionRatingsBySeq.value = saved.visionRatingsBySeq
            }
            if (saved.visionNotesBySeq.isNotEmpty()) {
                _visionNotesBySeq.value = saved.visionNotesBySeq
            }
            if (saved.selectedShotIndexBySeq.isNotEmpty()) {
                _selectedShotIndexBySeq.value = saved.selectedShotIndexBySeq
            }
            if (saved.stage3DModeBySeq.isNotEmpty()) {
                _stage3DModeBySeq.value = saved.stage3DModeBySeq
            }
            if (saved.agentFocusByProject.isNotEmpty()) {
                _agentFocusByProject.value = saved.agentFocusByProject.mapValues { AgentRoleFocus.fromWire(it.value) }
            }
            if (saved.agentDraftByProject.isNotEmpty()) {
                _agentDraftByProject.value = saved.agentDraftByProject
            }
        }
    }

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
                    } else if (
                        _selectedSequenceId.value.isNotBlank() &&
                        seqs.none { it.id == _selectedSequenceId.value } &&
                        !_isProducing.value
                    ) {
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
                    emit(_sequencesByProject.value[pid].orEmpty())
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

    private val activeMilestonesStream: StateFlow<List<ProductionMilestoneItem>> =
        _selectedProjectId.flatMapLatest { pid ->
            if (pid.isBlank()) flowOf(emptyList())
            else repository.observeMilestones(currentUserId, pid)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing milestones", fsErr ?: error)
                    emit(emptyList())
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val activeStoryboardPathsStream: StateFlow<Map<String, String>> =
        _selectedProjectId.flatMapLatest { pid ->
            if (pid.isBlank()) flowOf(emptyMap())
            else repository.observeStoryboardAssetPaths(currentUserId, pid)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing storyboard assets", fsErr ?: error)
                    emit(emptyMap())
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    private val userProfileStream: StateFlow<UserProfileState?> =
        repository.observeUserProfile(currentUserId)
            .map { profile ->
                if (profile != null && !isSavingConfig) {
                    _studioConfig.value = profile.config
                    _isDarkMode.value = profile.config.isDarkMode
                }
                profile
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing user profile", fsErr ?: error)
                emit(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val busyFlags = combine(
        combine(_isProducing, _isAnalyzingAudio, _isAgentBusy) { p, a, g -> Triple(p, a, g) },
        combine(_isFinalizing, _isSavingToDb, _isExporting) { f, s, e -> Triple(f, s, e) }
    ) { aiFlags, ioFlags ->
        BusyState(
            producing = aiFlags.first,
            audio = aiFlags.second,
            agent = aiFlags.third,
            finalizing = ioFlags.first,
            savingToDb = ioFlags.second,
            exporting = ioFlags.third
        )
    }

    private val navFlags = combine(
        _showDashboard,
        _showMetricsDashboard,
        _showIngestSheet,
        _showCalendarDialog,
        busyFlags
    ) { dash, metrics, ingest, cal, busy ->
        ShellNavFlags(dash, metrics, ingest, cal, busy)
    }

    private val navState = combine(
        _selectedProjectId,
        _selectedSequenceId,
        _activeTab,
        _searchQuery,
        navFlags
    ) { pId, sId, tab, query, flags ->
        SelectionState(
            selectedProjectId = pId,
            selectedSequenceId = sId,
            activeTab = tab,
            searchQuery = query,
            showDashboard = flags.showDashboard,
            showMetricsDashboard = flags.showMetricsDashboard,
            showIngestSheet = flags.showIngestSheet,
            showCalendarDialog = flags.showCalendarDialog,
            busy = flags.busy
        )
    }

    private val extrasStream = combine(
        _studioConfig,
        _isDarkMode,
        activeMilestonesStream,
        activeStoryboardPathsStream
    ) { config, isDark, milestones, persistedFrames ->
        StudioExtrasState(
            config = config,
            isDarkMode = isDark,
            milestones = milestones,
            persistedFramePaths = persistedFrames
        )
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
        lessonsStream,
        userProfileStream
    ) { projResult, activeSeqs, messages, lessons, profile ->
        RepoSnapshot(projResult, activeSeqs, messages, lessons, profile)
    }

    val projectsUiState: StateFlow<UiState<StudioUiState>> = combine(
        repoDataStream,
        navState,
        bannersState,
        tabDraftsState,
        extrasStream
    ) { repoData, sel, banners, drafts, extras ->
        when (val projResult = repoData.projectsResult) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> UiState.Error(projResult.message)
            is UiState.Success -> {
                val cachedMap = _sequencesByProject.value
                val combinedFramePaths = extras.persistedFramePaths + banners.localFramePaths
                val enrichedProjects = projResult.data.map { proj ->
                    val rawSeqs = if (proj.id == sel.selectedProjectId) {
                        if (repoData.activeSeqs.isNotEmpty()) repoData.activeSeqs else (cachedMap[proj.id] ?: emptyList())
                    } else {
                        cachedMap[proj.id] ?: emptyList()
                    }
                    val mergedSeqs = rawSeqs.map { seq ->
                        seq.copy(
                            storyboardFrames = seq.storyboardFrames.map { frame ->
                                val overridePath = combinedFramePaths[frame.id]
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
                    drafts.stage3DModeBySeq[seq.id] ?: extras.config.stage3DDefault
                } ?: extras.config.stage3DDefault

                val agentFocus = activeProject?.let { proj ->
                    drafts.agentFocusByProject[proj.id] ?: AgentRoleFocus.COUNCIL
                } ?: AgentRoleFocus.COUNCIL

                val agentDraftMsg = activeProject?.let { proj ->
                    drafts.agentDraftByProject[proj.id].orEmpty()
                }.orEmpty()

                UiState.Success(
                    StudioUiState(
                        projects = enrichedProjects,
                        userProfile = repoData.userProfile,
                        activeProject = activeProject,
                        activeSequence = activeSequence,
                        agentMessages = repoData.messages,
                        lessons = repoData.lessons,
                        intelligence = intelligence,
                        activeTab = sel.activeTab,
                        searchQuery = sel.searchQuery,
                        showDashboard = sel.showDashboard,
                        showMetricsDashboard = sel.showMetricsDashboard,
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
                        isSavingToDb = sel.busy.savingToDb,
                        isExporting = sel.busy.exporting,
                        generatingFrameId = banners.generatingFrameId,
                        isDarkMode = extras.isDarkMode,
                        showCalendarDialog = sel.showCalendarDialog,
                        milestones = extras.milestones,
                        studioConfig = extras.config,
                        statusBanner = banners.statusBanner,
                        errorBanner = banners.errorBanner
                    )
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private fun defaultIngestDraft(project: ProductionProject): IngestDraftState {
        val cfg = _studioConfig.value
        val fallbackStyle = cfg.preferredStyle.takeIf { it.isNotBlank() } ?: project.genreStyle
        val preset = StudioLogic.DIRECTOR_STYLES.firstOrNull {
            project.genreStyle.contains(it.label, ignoreCase = true)
        } ?: StudioLogic.DIRECTOR_STYLES.firstOrNull {
            fallbackStyle.contains(it.label, ignoreCase = true)
        } ?: StudioLogic.DIRECTOR_STYLES.first()
        return IngestDraftState(
            sourceType = project.defaultSourceType,
            directorStyle = preset.label,
            aspectRatio = cfg.defaultAspectRatio.ifBlank { preset.defaultRatio },
            thinkingDepth = ThinkingDepth.fromWire(cfg.defaultThinkingDepth),
            continuityBible = project.continuityBible,
            bibleInitialized = true
        )
    }

    private fun persistWorkspaceState() {
        val autoSave = _studioConfig.value.autoSaveDrafts
        val snapshot = LocalWorkspaceDraftSnapshot(
            selectedProjectId = _selectedProjectId.value,
            selectedSequenceId = _selectedSequenceId.value,
            activeTab = _activeTab.value.routeKey,
            searchQuery = _searchQuery.value,
            showDashboard = _showDashboard.value,
            ingestDrafts = if (autoSave) _ingestDrafts.value.mapValues { PersistedIngestDraft.fromModel(it.value) } else emptyMap(),
            directorDraftsBySeq = if (autoSave) _directorDraftsBySeq.value.mapValues { PersistedDirectorDraft.fromModel(it.value) } else emptyMap(),
            visionRatingsBySeq = if (autoSave) _visionRatingsBySeq.value else emptyMap(),
            visionNotesBySeq = if (autoSave) _visionNotesBySeq.value else emptyMap(),
            selectedShotIndexBySeq = _selectedShotIndexBySeq.value,
            stage3DModeBySeq = _stage3DModeBySeq.value,
            agentFocusByProject = _agentFocusByProject.value.mapValues { it.value.wireValue },
            agentDraftByProject = if (autoSave) _agentDraftByProject.value else emptyMap()
        )
        viewModelScope.launch {
            repository.saveWorkspaceDrafts(currentUserId, snapshot)
        }
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
        if (show) {
            _showMetricsDashboard.value = false
        }
        persistWorkspaceState()
    }

    fun setShowMetricsDashboard(show: Boolean) {
        _showMetricsDashboard.value = show
    }

    fun setShowIngestSheet(show: Boolean) {
        _showIngestSheet.value = show
    }

    fun toggleDarkMode() {
        val nextDark = !_isDarkMode.value
        _isDarkMode.value = nextDark
        val nextConfig = _studioConfig.value.copy(
            themeMode = if (nextDark) "dark" else "light"
        )
        _studioConfig.value = nextConfig
        isSavingConfig = true
        viewModelScope.launch {
            try {
                repository.updateUserStudioConfig(currentUserId, nextConfig)
                    .onFailure { err ->
                        _errorBanner.value = friendlyFirestoreMessage(err)
                    }
            } finally {
                isSavingConfig = false
            }
        }
    }

    fun updateStudioConfig(config: UserStudioConfig) {
        val normalized = config.copy(
            themeMode = if (config.themeMode.equals("light", ignoreCase = true)) "light" else "dark",
            preferredStyle = config.preferredStyle.trim().ifBlank { StudioLogic.DIRECTOR_STYLES.first().label }
        )
        _studioConfig.value = normalized
        _isDarkMode.value = normalized.isDarkMode
        isSavingConfig = true
        viewModelScope.launch {
            _isSavingToDb.value = true
            _errorBanner.value = null
            try {
                repository.updateUserStudioConfig(currentUserId, normalized)
                    .onSuccess {
                        _statusBanner.value = "Configuración y preferencias guardadas."
                    }
                    .onFailure { err ->
                        _errorBanner.value = friendlyFirestoreMessage(err)
                    }
            } finally {
                isSavingConfig = false
                _isSavingToDb.value = false
            }
        }
    }

    fun setShowCalendarDialog(show: Boolean) {
        _showCalendarDialog.value = show
    }

    fun addMilestone(
        projectId: String,
        title: String,
        notes: String,
        dateMillis: Long,
        type: CalendarMilestoneType,
        sequenceId: String? = null
    ) {
        viewModelScope.launch {
            _isSavingToDb.value = true
            _errorBanner.value = null
            try {
                repository.createMilestone(
                    userId = currentUserId,
                    projectId = projectId,
                    title = title,
                    notes = notes,
                    dateMillis = dateMillis,
                    type = type,
                    sequenceId = sequenceId
                ).onSuccess { created ->
                    _statusBanner.value = "Hito «${created.title}» guardado en el calendario."
                }.onFailure { err ->
                    _errorBanner.value = friendlyFirestoreMessage(err)
                }
            } finally {
                _isSavingToDb.value = false
            }
        }
    }

    fun toggleMilestoneCompleted(projectId: String, milestoneId: String, completed: Boolean) {
        viewModelScope.launch {
            _errorBanner.value = null
            repository.toggleMilestoneCompleted(currentUserId, projectId, milestoneId, completed)
                .onFailure { err -> _errorBanner.value = friendlyFirestoreMessage(err) }
        }
    }

    fun deleteMilestone(projectId: String, milestoneId: String) {
        viewModelScope.launch {
            _errorBanner.value = null
            repository.deleteMilestone(currentUserId, projectId, milestoneId)
                .onSuccess { _statusBanner.value = "Hito eliminado del calendario." }
                .onFailure { err -> _errorBanner.value = friendlyFirestoreMessage(err) }
        }
    }

    fun updatePreferredStyle(style: String) {
        val nextConfig = _studioConfig.value.copy(preferredStyle = style.trim())
        _studioConfig.value = nextConfig
        isSavingConfig = true
        viewModelScope.launch {
            _isSavingToDb.value = true
            _errorBanner.value = null
            try {
                repository.updateUserPreferredStyle(currentUserId, style)
                    .onSuccess {
                        _statusBanner.value = "Perfil cinematográfico actualizado."
                    }
                    .onFailure { err ->
                        _errorBanner.value = friendlyFirestoreMessage(err)
                    }
            } finally {
                isSavingConfig = false
                _isSavingToDb.value = false
            }
        }
    }

    fun selectProject(projectId: String) {
        _selectedProjectId.value = projectId
        _selectedSequenceId.value = ""
        _latestAudioReport.value = null
        val nextConfig = _studioConfig.value.copy(lastProjectId = projectId, lastSequenceId = "")
        _studioConfig.value = nextConfig
        persistWorkspaceState()
        viewModelScope.launch { repository.updateUserStudioConfig(currentUserId, nextConfig) }
    }

    fun selectSequence(sequenceId: String) {
        _selectedSequenceId.value = sequenceId
        val nextConfig = _studioConfig.value.copy(lastSequenceId = sequenceId)
        _studioConfig.value = nextConfig
        persistWorkspaceState()
        viewModelScope.launch { repository.updateUserStudioConfig(currentUserId, nextConfig) }
    }

    fun selectTab(tab: WorkspaceTab) {
        _activeTab.value = tab
        val nextConfig = _studioConfig.value.copy(lastTab = tab.routeKey)
        _studioConfig.value = nextConfig
        persistWorkspaceState()
        viewModelScope.launch { repository.updateUserStudioConfig(currentUserId, nextConfig) }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        persistWorkspaceState()
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
        persistWorkspaceState()
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
        persistWorkspaceState()
    }

    fun saveDirectorDraftEdits(
        project: ProductionProject,
        sequence: ProductionSequence
    ) {
        val draft = _directorDraftsBySeq.value[sequence.id] ?: defaultDirectorDraft(project, sequence)
        viewModelScope.launch {
            _isSavingToDb.value = true
            _errorBanner.value = null
            try {
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
                            persistWorkspaceState()
                            _statusBanner.value = "Ajustes de dirección guardados en «${sequence.title}»."
                        }
                        .onFailure { err ->
                            _errorBanner.value = friendlyFirestoreMessage(err)
                        }
                } else {
                    _directorDraftsBySeq.update { it - sequence.id }
                    persistWorkspaceState()
                    _statusBanner.value = "Biblia de continuidad actualizada (la secuencia Final es inmutable)."
                }
            } finally {
                _isSavingToDb.value = false
            }
        }
    }

    fun updateVisionRatingDraft(sequenceId: String, rating: Int) {
        _visionRatingsBySeq.update { it + (sequenceId to rating.coerceIn(1, 5)) }
        persistWorkspaceState()
    }

    fun updateVisionNoteDraft(sequenceId: String, note: String) {
        _visionNotesBySeq.update { it + (sequenceId to note) }
        persistWorkspaceState()
    }

    fun updateSelectedShotIndex(sequenceId: String, index: Int) {
        _selectedShotIndexBySeq.update { it + (sequenceId to index.coerceAtLeast(0)) }
        persistWorkspaceState()
    }

    fun updateStage3DMode(sequenceId: String, use3D: Boolean) {
        _stage3DModeBySeq.update { it + (sequenceId to use3D) }
        persistWorkspaceState()
    }

    fun updateAgentFocus(projectId: String, focus: AgentRoleFocus) {
        _agentFocusByProject.update { it + (projectId to focus) }
        persistWorkspaceState()
    }

    fun updateAgentDraftMessage(projectId: String, text: String) {
        _agentDraftByProject.update { it + (projectId to text) }
        persistWorkspaceState()
    }

    fun createProject(
        title: String,
        genreStyle: String,
        defaultSourceType: SourceMaterialType,
        continuityBible: String,
        templateId: String = "custom",
        productionFolders: List<ProductionFolder> = ProjectTemplateCatalog.defaultFoldersFor(templateId)
    ) {
        viewModelScope.launch {
            _isSavingToDb.value = true
            _errorBanner.value = null
            try {
                val resolvedFolders = productionFolders.ifEmpty {
                    ProjectTemplateCatalog.defaultFoldersFor(templateId)
                }
                repository.createProject(
                    userId = currentUserId,
                    title = title,
                    genreStyle = genreStyle,
                    defaultSourceType = defaultSourceType,
                    continuityBible = continuityBible,
                    templateId = templateId,
                    productionFolders = resolvedFolders
                )
                    .onSuccess { newId ->
                        _selectedProjectId.value = newId
                        _selectedSequenceId.value = ""
                        _activeTab.value = WorkspaceTab.AGENT
                        _showDashboard.value = false
                        _showMetricsDashboard.value = false
                        _statusBanner.value = "Producción \"$title\" creada con ${resolvedFolders.size} carpetas de producción."
                    }
                    .onFailure { err ->
                        _errorBanner.value = friendlyFirestoreMessage(err)
                    }
            } finally {
                _isSavingToDb.value = false
            }
        }
    }

    fun addProductionFolder(
        project: ProductionProject,
        folderName: String,
        category: String,
        description: String
    ) {
        val nextFolders = ProjectTemplateCatalog.addCustomFolder(
            existing = project.productionFolders,
            rawName = folderName,
            category = category,
            description = description
        )
        if (nextFolders.size == project.productionFolders.size) {
            _errorBanner.value = "Ingresa un nombre válido (límite 20 carpetas por producción)."
            return
        }
        viewModelScope.launch {
            _isSavingToDb.value = true
            _errorBanner.value = null
            try {
                repository.updateProjectFolders(currentUserId, project.id, nextFolders)
                    .onSuccess {
                        _statusBanner.value = "Carpeta \"${nextFolders.last().name}\" añadida a la producción."
                    }
                    .onFailure { err ->
                        _errorBanner.value = friendlyFirestoreMessage(err)
                    }
            } finally {
                _isSavingToDb.value = false
            }
        }
    }

    fun updateProductionFolderNotes(
        project: ProductionProject,
        folderId: String,
        notes: String
    ) {
        val nextFolders = ProjectTemplateCatalog.updateFolderNotes(
            existing = project.productionFolders,
            folderId = folderId,
            notes = notes
        )
        viewModelScope.launch {
            _isSavingToDb.value = true
            _errorBanner.value = null
            try {
                repository.updateProjectFolders(currentUserId, project.id, nextFolders)
                    .onSuccess {
                        _statusBanner.value = "Notas de carpeta sincronizadas."
                    }
                    .onFailure { err ->
                        _errorBanner.value = friendlyFirestoreMessage(err)
                    }
            } finally {
                _isSavingToDb.value = false
            }
        }
    }

    fun openProductionFolderTab(folder: ProductionFolder) {
        _activeTab.value = ProjectTemplateCatalog.resolveWorkspaceTab(folder)
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            _isSavingToDb.value = true
            _errorBanner.value = null
            try {
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
            } finally {
                _isSavingToDb.value = false
            }
        }
    }

    fun saveContinuityBible(projectId: String, bibleText: String) {
        viewModelScope.launch {
            _isSavingToDb.value = true
            _errorBanner.value = null
            try {
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
            } finally {
                _isSavingToDb.value = false
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
                    val verified = repository.getSequenceById(currentUserId, project.id, saved.id) ?: saved
                    _sequencesByProject.update { prev ->
                        val cur = prev[project.id].orEmpty().filterNot { it.id == verified.id }
                        prev + (project.id to (listOf(verified) + cur))
                    }
                    _selectedSequenceId.value = verified.id
                    _activeTab.value = WorkspaceTab.SCREENPLAY
                    _showIngestSheet.value = false
                    _ingestDrafts.update { it - project.id }
                    _statusBanner.value = "Secuencia \"${verified.title}\" producida (${verified.screenplay.size} escenas, ${verified.shotList.size} planos)."
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
                    val verified = repository.getSequenceById(currentUserId, project.id, saved.id) ?: saved
                    _sequencesByProject.update { prev ->
                        val cur = prev[project.id].orEmpty().filterNot { it.id == verified.id }
                        prev + (project.id to (listOf(verified) + cur))
                    }
                    _selectedSequenceId.value = verified.id
                    _activeTab.value = WorkspaceTab.SCREENPLAY
                    _showIngestSheet.value = false
                    _ingestDrafts.update { it - project.id }
                    _statusBanner.value = "Borrador manual \"${verified.title}\" guardado en Guion."
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
            _isSavingToDb.value = true
            _errorBanner.value = null
            try {
                repository.updateDraftSequence(currentUserId, projectId, updatedSequence)
                    .onSuccess {
                        _statusBanner.value = "Escena $targetNumber guardada en «${sequence.title}»."
                    }
                    .onFailure { err ->
                        _errorBanner.value = friendlyFirestoreMessage(err)
                    }
            } finally {
                _isSavingToDb.value = false
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
            _isSavingToDb.value = true
            _errorBanner.value = null
            try {
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
            } finally {
                _isSavingToDb.value = false
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
            _isSavingToDb.value = true
            _errorBanner.value = null
            try {
                repository.addLearningLesson(currentUserId, category, insight, rating, sourceProjectTitle)
                    .onSuccess { _statusBanner.value = "Regla guardada en memoria de autoaprendizaje." }
                    .onFailure { err -> _errorBanner.value = friendlyFirestoreMessage(err) }
            } finally {
                _isSavingToDb.value = false
            }
        }
    }

    fun deleteLearningLesson(lessonId: String) {
        viewModelScope.launch {
            _isSavingToDb.value = true
            try {
                repository.deleteLearningLesson(currentUserId, lessonId)
                    .onFailure { err -> _errorBanner.value = friendlyFirestoreMessage(err) }
            } finally {
                _isSavingToDb.value = false
            }
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

    fun exportScriptAndSchedulePdf(
        context: Context,
        project: ProductionProject,
        sequence: ProductionSequence,
        onReadyForDeviceSave: (File) -> Unit
    ) {
        viewModelScope.launch {
            _isExporting.value = true
            _errorBanner.value = null
            try {
                val file = PdfExporter.exportScriptAndSchedulePdf(context, project, sequence)
                onReadyForDeviceSave(file)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorBanner.value = e.localizedMessage ?: "No se pudo preparar el PDF de Guion y Plan."
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun savePdfToDeviceUri(
        context: Context,
        sourceFile: File,
        destinationUri: Uri,
        displayName: String = sourceFile.name
    ) {
        viewModelScope.launch {
            _isExporting.value = true
            _errorBanner.value = null
            try {
                PdfExporter.copyFileToUri(context, sourceFile, destinationUri)
                    .onSuccess {
                        _statusBanner.value = "PDF «$displayName» guardado en el almacenamiento del dispositivo."
                    }
                    .onFailure { err ->
                        _errorBanner.value = err.localizedMessage ?: "No se pudo guardar el PDF en la ubicación seleccionada."
                    }
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
        val savingToDb: Boolean,
        val exporting: Boolean
    )

    private data class ShellNavFlags(
        val showDashboard: Boolean,
        val showMetricsDashboard: Boolean,
        val showIngestSheet: Boolean,
        val showCalendarDialog: Boolean,
        val busy: BusyState
    )

    private data class SelectionState(
        val selectedProjectId: String,
        val selectedSequenceId: String,
        val activeTab: WorkspaceTab,
        val searchQuery: String,
        val showDashboard: Boolean,
        val showMetricsDashboard: Boolean,
        val showIngestSheet: Boolean,
        val showCalendarDialog: Boolean,
        val busy: BusyState
    )

    private data class StudioExtrasState(
        val config: UserStudioConfig,
        val isDarkMode: Boolean,
        val milestones: List<ProductionMilestoneItem>,
        val persistedFramePaths: Map<String, String>
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
        val lessons: List<LearningLesson>,
        val userProfile: UserProfileState?
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
                    repository = StudioRepository.getInstance(app),
                    aiService = StudioAiService(),
                    currentUserId = currentUserId
                )
            }
        }
    }
}
