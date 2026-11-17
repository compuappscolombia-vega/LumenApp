package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.ai.StudioAiService
import com.example.data.model.AgentMessage
import com.example.data.model.AgentRoleFocus
import com.example.data.model.AudioAnalysisReport
import com.example.data.model.LearningCategory
import com.example.data.model.LearningLesson
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val aiService: StudioAiService,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedProjectId = MutableStateFlow("")
    private val _selectedSequenceId = MutableStateFlow("")
    private val _activeTab = MutableStateFlow(WorkspaceTab.AGENT)
    private val _searchQuery = MutableStateFlow("")
    private val _sequencesByProject = MutableStateFlow<Map<String, List<ProductionSequence>>>(emptyMap())
    private val _localFramePaths = MutableStateFlow<Map<String, String>>(emptyMap())

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
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

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

    private val bannersState = combine(
        _generatingFrameId,
        _statusBanner,
        _errorBanner,
        _latestAudioReport,
        _localFramePaths
    ) { genFrame, status, err, audioReport, framePaths ->
        BannerState(genFrame, status, err, audioReport, framePaths)
    }

    val projectsUiState: StateFlow<UiState<StudioUiState>> = combine(
        projectsStream,
        activeSequencesStream,
        activeMessagesStream,
        lessonsStream,
        combine(_selectedProjectId, _selectedSequenceId, _activeTab, _searchQuery, busyFlags) { pId, sId, tab, query, busy ->
            SelectionState(pId, sId, tab, query, busy)
        },
        bannersState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val projResult = values[0] as UiState<List<ProductionProject>>
        @Suppress("UNCHECKED_CAST")
        val activeSeqs = values[1] as List<ProductionSequence>
        @Suppress("UNCHECKED_CAST")
        val messages = values[2] as List<AgentMessage>
        @Suppress("UNCHECKED_CAST")
        val lessons = values[3] as List<LearningLesson>
        val sel = values[4] as SelectionState
        val banners = values[5] as BannerState

        when (projResult) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> UiState.Error(projResult.message)
            is UiState.Success -> {
                val cachedMap = _sequencesByProject.value
                val enrichedProjects = projResult.data.map { proj ->
                    val rawSeqs = if (proj.id == sel.selectedProjectId) activeSeqs else (cachedMap[proj.id] ?: emptyList())
                    val mergedSeqs = rawSeqs.map { seq ->
                        seq.copy(
                            storyboardFrames = seq.storyboardFrames.map { frame ->
                                val overridePath = banners.localFramePaths[frame.id]
                                if (!overridePath.isNullOrBlank()) frame.copy(localImagePath = overridePath) else frame
                            }
                        )
                    }
                    proj.copy(sequences = mergedSeqs, agentMessages = if (proj.id == sel.selectedProjectId) messages else emptyList())
                }
                val activeProject = enrichedProjects.firstOrNull { it.id == sel.selectedProjectId }
                    ?: enrichedProjects.firstOrNull()
                val activeSequence = activeProject?.sequences?.firstOrNull { it.id == sel.selectedSequenceId }
                    ?: activeProject?.sequences?.firstOrNull()

                val intelligence = activeSequence?.let { seq ->
                    StudioIntelligence.buildStudioIntelligence(
                        projects = enrichedProjects,
                        lessons = lessons,
                        activeProject = activeProject,
                        activeSequence = seq
                    )
                }

                UiState.Success(
                    StudioUiState(
                        projects = enrichedProjects,
                        activeProject = activeProject,
                        activeSequence = activeSequence,
                        agentMessages = messages,
                        lessons = lessons,
                        intelligence = intelligence,
                        activeTab = sel.activeTab,
                        searchQuery = sel.searchQuery,
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
                    _statusBanner.value = "Biblia de continuidad actualizada."
                }
                .onFailure { err ->
                    _errorBanner.value = friendlyFirestoreMessage(err)
                }
        }
    }

    fun analyzeAudioSource(
        filename: String,
        mimeType: String,
        audioBytes: ByteArray,
        lessons: List<LearningLesson>
    ) {
        viewModelScope.launch {
            _isAnalyzingAudio.value = true
            _errorBanner.value = null
            _statusBanner.value = null
            try {
                val learningContext = LearningEngine.buildLearningPromptContext(lessons, 6)
                val report = aiService.analyzeAudio(filename, mimeType, audioBytes, learningContext)
                _latestAudioReport.value = report
                _statusBanner.value = "Análisis y transcripción verbatim listos para \"$filename\"."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Audio analysis failed", e)
                _errorBanner.value = AgentLogic.formatAiError(e)
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
        onDone: () -> Unit
    ) {
        val validationErrors = StudioLogic.validateSequenceInput(
            sequenceTitle = sequenceTitle,
            sourceText = sourceText,
            hasFile = attachedBytes != null
        )
        if (validationErrors.isNotEmpty()) {
            _errorBanner.value = validationErrors.first()
            return
        }

        viewModelScope.launch {
            _isProducing.value = true
            _errorBanner.value = null
            _statusBanner.value = null
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
                    _activeTab.value = WorkspaceTab.VISION
                    _statusBanner.value = "Secuencia \"${saved.title}\" producida en borrador."
                    onDone()
                }.onFailure { err ->
                    _errorBanner.value = friendlyFirestoreMessage(err)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Sequence production failed", e)
                _errorBanner.value = AgentLogic.formatAiError(e)
            } finally {
                _isProducing.value = false
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
                if (sequence.versionStatus == SequenceVersionStatus.DRAFT) {
                    val updatedFrames = sequence.storyboardFrames.map {
                        if (it.id == frame.id) it.copy(localImagePath = imagePath) else it
                    }
                    repository.updateDraftSequence(
                        userId = currentUserId,
                        projectId = projectId,
                        sequence = sequence.copy(storyboardFrames = updatedFrames)
                    )
                }
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
        val busy: BusyState
    )

    private data class BannerState(
        val generatingFrameId: String?,
        val statusBanner: String?,
        val errorBanner: String?,
        val audioReport: AudioAnalysisReport?,
        val localFramePaths: Map<String, String>
    )

    companion object {
        fun provideFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                StudioViewModel(
                    repository = StudioRepository(databaseId),
                    aiService = StudioAiService(),
                    currentUserId = currentUserId
                )
            }
        }
    }
}
