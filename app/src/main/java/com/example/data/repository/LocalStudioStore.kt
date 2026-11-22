package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.AgentMessage
import com.example.data.model.AgentRoleFocus
import com.example.data.model.AgentToolAction
import com.example.data.model.AudioAnalysisReport
import com.example.data.model.ColorSwatch
import com.example.data.model.LearningCategory
import com.example.data.model.LearningLesson
import com.example.data.model.LightingSetup
import com.example.data.model.ProductionFolder
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.ScreenplayScene
import com.example.data.model.SequenceVersionStatus
import com.example.data.model.ShotSpec
import com.example.data.model.SoundDesignSpec
import com.example.data.model.SourceMaterialType
import com.example.data.model.StoryboardCard
import com.example.data.model.VfxBreakdown
import com.example.data.util.LearningEngine
import com.example.data.util.ProjectTemplateCatalog
import com.example.data.util.StudioIntelligence
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

const val LOCAL_GUEST_UID = "local_studio_guest"
private const val TAG = "LocalStudioStore"
private const val LOCAL_STORE_FILENAME = "lumenreel_local_studio_v1.json"

@Serializable
private data class LocalSequenceEntry(
    val id: String,
    val projectId: String,
    val ownerId: String = LOCAL_GUEST_UID,
    val title: String = "",
    val versionStatus: String = "draft",
    val sourceType: String = "book",
    val sourceFilename: String? = null,
    val directorStyle: String = "",
    val aspectRatio: String = "2.39:1",
    val cameraSystem: String = "",
    val lensPackage: String = "",
    val filmStockOrLut: String = "",
    val logline: String = "",
    val directorVision: String = "",
    val colorPalette: List<ColorSwatch> = emptyList(),
    val lighting: LightingSetup = LightingSetup(),
    val shotList: List<ShotSpec> = emptyList(),
    val soundDesign: SoundDesignSpec = SoundDesignSpec(),
    val audioAnalysis: AudioAnalysisReport? = null,
    val vfx: VfxBreakdown = VfxBreakdown(),
    val screenplay: List<ScreenplayScene> = emptyList(),
    val storyboardFrames: List<StoryboardCard> = emptyList(),
    val continuitySummary: String = "",
    val sequenceRating: Int? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun toModel(): ProductionSequence = ProductionSequence(
        id = id,
        projectId = projectId,
        ownerId = ownerId,
        title = title,
        versionStatus = SequenceVersionStatus.fromWire(versionStatus),
        sourceType = SourceMaterialType.fromWire(sourceType),
        sourceFilename = sourceFilename,
        directorStyle = directorStyle,
        aspectRatio = aspectRatio,
        cameraSystem = cameraSystem,
        lensPackage = lensPackage,
        filmStockOrLut = filmStockOrLut,
        logline = logline,
        directorVision = directorVision,
        colorPalette = colorPalette,
        lighting = lighting,
        shotList = shotList,
        soundDesign = soundDesign,
        audioAnalysis = audioAnalysis,
        vfx = vfx,
        screenplay = screenplay,
        storyboardFrames = storyboardFrames,
        continuitySummary = continuitySummary,
        sequenceRating = sequenceRating,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )

    companion object {
        fun fromModel(seq: ProductionSequence): LocalSequenceEntry = LocalSequenceEntry(
            id = seq.id,
            projectId = seq.projectId,
            ownerId = seq.ownerId.ifBlank { LOCAL_GUEST_UID },
            title = seq.title,
            versionStatus = seq.versionStatus.wireValue,
            sourceType = seq.sourceType.wireValue,
            sourceFilename = seq.sourceFilename,
            directorStyle = seq.directorStyle,
            aspectRatio = seq.aspectRatio,
            cameraSystem = seq.cameraSystem,
            lensPackage = seq.lensPackage,
            filmStockOrLut = seq.filmStockOrLut,
            logline = seq.logline,
            directorVision = seq.directorVision,
            colorPalette = seq.colorPalette,
            lighting = seq.lighting,
            shotList = seq.shotList,
            soundDesign = seq.soundDesign,
            audioAnalysis = seq.audioAnalysis,
            vfx = seq.vfx,
            screenplay = seq.screenplay,
            storyboardFrames = seq.storyboardFrames,
            continuitySummary = seq.continuitySummary,
            sequenceRating = seq.sequenceRating,
            createdAtMillis = seq.createdAtMillis,
            updatedAtMillis = seq.updatedAtMillis
        )
    }
}

@Serializable
private data class LocalAgentMessageEntry(
    val id: String,
    val projectId: String,
    val role: String,
    val focus: String,
    val content: String,
    val actions: List<AgentToolAction> = emptyList(),
    val createdAtMillis: Long = 0L
) {
    fun toModel(): AgentMessage = AgentMessage(
        id = id,
        role = role,
        focus = AgentRoleFocus.fromWire(focus),
        content = content,
        actions = actions,
        createdAtMillis = createdAtMillis
    )
}

@Serializable
private data class LocalLessonEntry(
    val id: String,
    val category: String,
    val insight: String,
    val rating: Int,
    val sourceProjectTitle: String,
    val createdAtMillis: Long = 0L
) {
    fun toModel(): LearningLesson = LearningLesson(
        id = id,
        category = LearningCategory.fromWire(category),
        insight = insight,
        rating = rating.coerceIn(1, 5),
        sourceProjectTitle = sourceProjectTitle,
        createdAtMillis = createdAtMillis
    )
}

@Serializable
private data class LocalProjectEntry(
    val id: String,
    val ownerId: String = LOCAL_GUEST_UID,
    val title: String,
    val genreStyle: String,
    val defaultSourceType: String = "book",
    val continuityBible: String = "",
    val templateId: String = "custom",
    val productionFolders: List<ProductionFolder> = emptyList(),
    val sequencesCount: Int = 0,
    val assetsCount: Int = 0,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun toModel(): ProductionProject = ProductionProject(
        id = id,
        ownerId = ownerId,
        title = title,
        genreStyle = genreStyle,
        defaultSourceType = SourceMaterialType.fromWire(defaultSourceType),
        continuityBible = continuityBible,
        templateId = templateId.ifBlank { "custom" },
        productionFolders = productionFolders.ifEmpty { ProjectTemplateCatalog.defaultFoldersFor(templateId) },
        sequencesCount = sequencesCount,
        assetsCount = assetsCount,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Serializable
private data class LocalStudioSnapshot(
    val projects: List<LocalProjectEntry> = emptyList(),
    val sequences: List<LocalSequenceEntry> = emptyList(),
    val messages: List<LocalAgentMessageEntry> = emptyList(),
    val lessons: List<LocalLessonEntry> = emptyList()
)

class LocalStudioStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val storeFile = File(context.applicationContext.filesDir, LOCAL_STORE_FILENAME)
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val snapshotFlow: MutableStateFlow<LocalStudioSnapshot> by lazy {
        MutableStateFlow(readSnapshotFromDisk())
    }

    private fun newId(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(12)}"

    private fun readSnapshotFromDisk(): LocalStudioSnapshot {
        if (!storeFile.exists()) return LocalStudioSnapshot()
        return try {
            val raw = storeFile.readText()
            if (raw.isBlank()) LocalStudioSnapshot()
            else json.decodeFromString(LocalStudioSnapshot.serializer(), raw)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode local studio store; resetting cleanly", e)
            LocalStudioSnapshot()
        }
    }

    private suspend fun mutate(transform: (LocalStudioSnapshot) -> LocalStudioSnapshot) {
        withContext(ioDispatcher) {
            mutex.withLock {
                val updated = transform(snapshotFlow.value)
                snapshotFlow.value = updated
                try {
                    val tmp = File(storeFile.parentFile, "$LOCAL_STORE_FILENAME.tmp")
                    tmp.writeText(json.encodeToString(LocalStudioSnapshot.serializer(), updated))
                    if (!tmp.renameTo(storeFile)) {
                        storeFile.writeText(json.encodeToString(LocalStudioSnapshot.serializer(), updated))
                        tmp.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist local studio snapshot", e)
                }
            }
        }
    }

    fun observeProjects(userId: String): Flow<List<ProductionProject>> =
        snapshotFlow.map { snap ->
            snap.projects
                .sortedByDescending { it.updatedAtMillis }
                .map { it.toModel().copy(ownerId = userId) }
        }

    fun observeSequences(projectId: String): Flow<List<ProductionSequence>> =
        snapshotFlow.map { snap ->
            snap.sequences
                .filter { it.projectId == projectId }
                .sortedByDescending { it.createdAtMillis }
                .map { it.toModel() }
        }

    fun observeAgentMessages(projectId: String): Flow<List<AgentMessage>> =
        snapshotFlow.map { snap ->
            snap.messages
                .filter { it.projectId == projectId }
                .sortedBy { it.createdAtMillis }
                .map { it.toModel() }
        }

    fun observeLearningLessons(): Flow<List<LearningLesson>> =
        snapshotFlow.map { snap ->
            snap.lessons
                .sortedByDescending { it.createdAtMillis }
                .map { it.toModel() }
        }

    suspend fun createProject(
        userId: String,
        title: String,
        genreStyle: String,
        defaultSourceType: SourceMaterialType,
        continuityBible: String,
        templateId: String = "custom",
        productionFolders: List<ProductionFolder> = ProjectTemplateCatalog.defaultFoldersFor(templateId)
    ): Result<String> {
        val cleanTitle = title.trim().take(180)
        if (cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("El título de la producción es obligatorio."))
        }
        val safeTemplateId = templateId.trim().ifBlank { "custom" }.take(64)
        val safeFolders = productionFolders.ifEmpty { ProjectTemplateCatalog.defaultFoldersFor(safeTemplateId) }.take(20)
        val projectId = newId("proj")
        val now = System.currentTimeMillis()
        mutate { snap ->
            val entry = LocalProjectEntry(
                id = projectId,
                ownerId = userId,
                title = cleanTitle,
                genreStyle = genreStyle.trim().ifBlank { "Épica Monumental IMAX" }.take(180),
                defaultSourceType = defaultSourceType.wireValue,
                continuityBible = continuityBible.trim().take(20000),
                templateId = safeTemplateId,
                productionFolders = safeFolders,
                sequencesCount = 0,
                assetsCount = 0,
                createdAtMillis = now,
                updatedAtMillis = now
            )
            snap.copy(projects = snap.projects + entry)
        }
        return Result.success(projectId)
    }

    suspend fun updateProjectContinuity(projectId: String, continuityBible: String): Result<Unit> {
        val now = System.currentTimeMillis()
        mutate { snap ->
            snap.copy(
                projects = snap.projects.map { proj ->
                    if (proj.id == projectId) {
                        proj.copy(
                            continuityBible = continuityBible.trim().take(20000),
                            updatedAtMillis = now
                        )
                    } else proj
                }
            )
        }
        return Result.success(Unit)
    }

    suspend fun updateProjectFolders(projectId: String, folders: List<ProductionFolder>): Result<Unit> {
        val now = System.currentTimeMillis()
        val safeFolders = folders.take(20)
        mutate { snap ->
            snap.copy(
                projects = snap.projects.map { proj ->
                    if (proj.id == projectId) {
                        proj.copy(
                            productionFolders = safeFolders,
                            updatedAtMillis = now
                        )
                    } else proj
                }
            )
        }
        return Result.success(Unit)
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        mutate { snap ->
            snap.copy(
                projects = snap.projects.filterNot { it.id == projectId },
                sequences = snap.sequences.filterNot { it.projectId == projectId },
                messages = snap.messages.filterNot { it.projectId == projectId }
            )
        }
        return Result.success(Unit)
    }

    suspend fun saveProducedSequence(
        userId: String,
        projectId: String,
        sequence: ProductionSequence,
        nextContinuityBible: String,
        nextSequencesCount: Int
    ): Result<ProductionSequence> {
        val seqId = sequence.id.ifBlank { newId("seq") }
        val now = System.currentTimeMillis()
        val cleanSeq = sequence.copy(
            id = seqId,
            projectId = projectId,
            ownerId = userId,
            title = sequence.title.trim().ifBlank { "Secuencia" }.take(200),
            directorStyle = sequence.directorStyle.trim().take(180),
            aspectRatio = sequence.aspectRatio.trim().take(24),
            logline = sequence.logline.trim().take(1200),
            directorVision = sequence.directorVision.trim().take(6000),
            continuitySummary = sequence.continuitySummary.trim().take(3000),
            createdAtMillis = if (sequence.createdAtMillis > 0L) sequence.createdAtMillis else now,
            updatedAtMillis = now
        )
        mutate { snap ->
            val seqEntry = LocalSequenceEntry.fromModel(cleanSeq)
            val updatedSequences = snap.sequences.filterNot { it.id == seqId } + seqEntry
            val updatedProjects = snap.projects.map { proj ->
                if (proj.id == projectId) {
                    proj.copy(
                        sequencesCount = nextSequencesCount.coerceIn(0, 1000),
                        continuityBible = nextContinuityBible.trim().take(20000),
                        updatedAtMillis = now
                    )
                } else proj
            }
            snap.copy(projects = updatedProjects, sequences = updatedSequences)
        }
        return Result.success(cleanSeq)
    }

    suspend fun updateDraftSequence(sequence: ProductionSequence): Result<Unit> {
        if (sequence.versionStatus != SequenceVersionStatus.DRAFT) {
            return Result.failure(IllegalArgumentException("Solo las secuencias en borrador pueden editarse."))
        }
        val now = System.currentTimeMillis()
        mutate { snap ->
            val updated = LocalSequenceEntry.fromModel(sequence.copy(updatedAtMillis = now))
            snap.copy(
                sequences = snap.sequences.map { existing ->
                    if (existing.id == sequence.id) updated else existing
                }
            )
        }
        return Result.success(Unit)
    }

    suspend fun updateStoryboardFramePath(
        sequenceId: String,
        frameId: String,
        localImagePath: String
    ): Result<Unit> {
        val now = System.currentTimeMillis()
        mutate { snap ->
            snap.copy(
                sequences = snap.sequences.map { seq ->
                    if (seq.id == sequenceId) {
                        seq.copy(
                            storyboardFrames = seq.storyboardFrames.map { frame ->
                                if (frame.id == frameId) frame.copy(localImagePath = localImagePath) else frame
                            },
                            updatedAtMillis = now
                        )
                    } else seq
                }
            )
        }
        return Result.success(Unit)
    }

    suspend fun finalizeSequence(projectTitle: String, sequence: ProductionSequence): Result<Unit> {
        val now = System.currentTimeMillis()
        mutate { snap ->
            val updatedSequences = snap.sequences.map { seq ->
                if (seq.id == sequence.id) {
                    seq.copy(versionStatus = SequenceVersionStatus.FINAL.wireValue, updatedAtMillis = now)
                } else seq
            }
            val newLessons = if (sequence.versionStatus != SequenceVersionStatus.FINAL) {
                LearningEngine.extractAutomaticLessons(projectTitle, sequence).map { lesson ->
                    LocalLessonEntry(
                        id = newId("lesson"),
                        category = lesson.category.wireValue,
                        insight = lesson.insight.trim().take(1000),
                        rating = lesson.rating.coerceIn(1, 5),
                        sourceProjectTitle = lesson.sourceProjectTitle.trim().ifBlank { "LumenReel Studio" }.take(180),
                        createdAtMillis = now
                    )
                }
            } else emptyList()
            snap.copy(
                sequences = updatedSequences,
                lessons = snap.lessons + newLessons
            )
        }
        return Result.success(Unit)
    }

    suspend fun rateSequence(
        projectTitle: String,
        sequence: ProductionSequence,
        rating: Int,
        note: String
    ): Result<Unit> {
        val safeRating = rating.coerceIn(1, 5)
        val now = System.currentTimeMillis()
        mutate { snap ->
            val updatedSequences = snap.sequences.map { seq ->
                if (seq.id == sequence.id) {
                    seq.copy(sequenceRating = safeRating, updatedAtMillis = now)
                } else seq
            }
            val newLessons = if (safeRating >= 4) {
                StudioIntelligence.buildRatedChoiceLessons(
                    projectTitle = projectTitle,
                    sequence = sequence,
                    rating = safeRating,
                    note = note
                ).map { lesson ->
                    LocalLessonEntry(
                        id = newId("lesson"),
                        category = lesson.category.wireValue,
                        insight = lesson.insight.trim().take(1000),
                        rating = lesson.rating.coerceIn(1, 5),
                        sourceProjectTitle = lesson.sourceProjectTitle.trim().ifBlank { "LumenReel Studio" }.take(180),
                        createdAtMillis = now
                    )
                }
            } else emptyList()
            snap.copy(
                sequences = updatedSequences,
                lessons = snap.lessons + newLessons
            )
        }
        return Result.success(Unit)
    }

    suspend fun saveAgentMessagePair(
        projectId: String,
        focus: AgentRoleFocus,
        userContent: String,
        agentContent: String,
        actions: List<AgentToolAction>
    ): Result<Unit> {
        val now = System.currentTimeMillis()
        mutate { snap ->
            val uMsg = LocalAgentMessageEntry(
                id = newId("msg_u"),
                projectId = projectId,
                role = "user",
                focus = focus.wireValue,
                content = userContent.trim().take(12000),
                actions = emptyList(),
                createdAtMillis = now
            )
            val aMsg = LocalAgentMessageEntry(
                id = newId("msg_a"),
                projectId = projectId,
                role = "agent",
                focus = focus.wireValue,
                content = agentContent.trim().take(12000),
                actions = actions.take(10),
                createdAtMillis = now + 1L
            )
            snap.copy(messages = snap.messages + uMsg + aMsg)
        }
        return Result.success(Unit)
    }

    suspend fun clearAgentMessages(projectId: String): Result<Unit> {
        mutate { snap ->
            snap.copy(messages = snap.messages.filterNot { it.projectId == projectId })
        }
        return Result.success(Unit)
    }

    suspend fun addLearningLesson(
        category: LearningCategory,
        insight: String,
        rating: Int,
        sourceProjectTitle: String
    ): Result<Unit> {
        val cleanInsight = insight.trim().take(1000)
        if (cleanInsight.length < 5) {
            return Result.failure(IllegalArgumentException("La lección debe tener al menos 5 caracteres."))
        }
        val now = System.currentTimeMillis()
        mutate { snap ->
            val entry = LocalLessonEntry(
                id = newId("lesson"),
                category = category.wireValue,
                insight = cleanInsight,
                rating = rating.coerceIn(1, 5),
                sourceProjectTitle = sourceProjectTitle.trim().ifBlank { "LumenReel Studio" }.take(180),
                createdAtMillis = now
            )
            snap.copy(lessons = snap.lessons + entry)
        }
        return Result.success(Unit)
    }

    suspend fun deleteLearningLesson(lessonId: String): Result<Unit> {
        mutate { snap ->
            snap.copy(lessons = snap.lessons.filterNot { it.id == lessonId })
        }
        return Result.success(Unit)
    }

    companion object {
        @Volatile
        private var instance: LocalStudioStore? = null

        fun getInstance(context: Context): LocalStudioStore =
            instance ?: synchronized(this) {
                instance ?: LocalStudioStore(context.applicationContext).also { instance = it }
            }
    }
}
