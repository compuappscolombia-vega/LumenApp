package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AgentMessage
import com.example.data.model.AgentMessageDoc
import com.example.data.model.AgentRoleFocus
import com.example.data.model.AgentToolAction
import com.example.data.model.AudioAnalysisReport
import com.example.data.model.ColorSwatch
import com.example.data.model.LearningCategory
import com.example.data.model.LearningLesson
import com.example.data.model.LearningLessonDoc
import com.example.data.model.LightingSetup
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.ScreenplayScene
import com.example.data.model.SequenceVersionStatus
import com.example.data.model.ShotSpec
import com.example.data.model.SoundDesignSpec
import com.example.data.model.SourceMaterialType
import com.example.data.model.StoryboardCard
import com.example.data.model.StudioProjectDoc
import com.example.data.model.VfxBreakdown
import com.example.data.util.LearningEngine
import com.example.data.util.OperationType
import com.example.data.util.StudioIntelligence
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class UserProfileState(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val preferredStyle: String = ""
)

class StudioRepository(
    private val firestoreProvider: () -> FirebaseFirestore,
    private val authProvider: () -> FirebaseAuth = { Firebase.auth },
    private val localStore: LocalStudioStore? = null
) {
    constructor(db: FirebaseFirestore, auth: FirebaseAuth = Firebase.auth) : this(
        firestoreProvider = { db },
        authProvider = { auth },
        localStore = null
    )

    constructor(databaseId: String, localStore: LocalStudioStore? = null) : this(
        firestoreProvider = { FirebaseFirestore.getInstance(databaseId) },
        localStore = localStore
    )

    constructor(context: Context) : this(
        databaseId = context.applicationContext.getString(R.string.firestore_database_id),
        localStore = LocalStudioStore.getInstance(context)
    )

    private val db: FirebaseFirestore by lazy { firestoreProvider() }
    private val auth: FirebaseAuth by lazy { authProvider() }

    val sessionState = StudioSessionStateHolder()
    private val _firestoreSequenceCache = MutableStateFlow<Map<String, List<ProductionSequence>>>(emptyMap())

    fun clearSessionCache() {
        sessionState.reset()
        _firestoreSequenceCache.value = emptyMap()
    }

    private fun cacheKey(userId: String, projectId: String): String = "$userId::$projectId"

    private fun upsertCachedSequence(userId: String, projectId: String, sequence: ProductionSequence) {
        val key = cacheKey(userId, projectId)
        _firestoreSequenceCache.update { map ->
            val current = map[key].orEmpty().filterNot { it.id == sequence.id }
            val next = (listOf(sequence) + current).sortedByDescending { it.createdAtMillis }
            map + (key to next)
        }
        sessionState.sequencesByProject.update { map ->
            val current = map[projectId].orEmpty().filterNot { it.id == sequence.id }
            val next = (listOf(sequence) + current).sortedByDescending { it.createdAtMillis }
            map + (projectId to next)
        }
    }

    private fun isLocalMode(userId: String): Boolean =
        localStore != null && (userId == LOCAL_GUEST_UID || auth.currentUser == null)

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: error("Debes iniciar sesión con Google antes de modificar datos.")
        check(uid == expectedUserId) { "La sesión activa cambió; operación bloqueada." }
        return uid
    }

    private fun newSafeDocId(prefix: String): String {
        val shortUuid = UUID.randomUUID().toString().replace("-", "").take(12)
        return "${prefix}_${shortUuid}"
    }

    fun observeProjects(userId: String): Flow<List<ProductionProject>> {
        if (isLocalMode(userId)) {
            return localStore!!.observeProjects(userId)
        }
        return db.collection("users").document(userId)
            .collection("projects")
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val dto = doc.toObject(
                        StudioProjectDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: return@mapNotNull null
                    ProductionProject(
                        id = dto.id,
                        ownerId = dto.ownerId,
                        title = dto.title,
                        genreStyle = dto.genreStyle,
                        defaultSourceType = SourceMaterialType.fromWire(dto.defaultSourceType),
                        continuityBible = dto.continuityBible,
                        sequencesCount = dto.sequencesCount.toInt(),
                        assetsCount = dto.assetsCount.toInt(),
                        createdAtMillis = (dto.createdAt ?: Timestamp.now()).toDate().time,
                        updatedAtMillis = (dto.updatedAt ?: dto.createdAt ?: Timestamp.now()).toDate().time
                    )
                }.sortedByDescending { it.updatedAtMillis }
            }
    }

    fun observeSequences(userId: String, projectId: String): Flow<List<ProductionSequence>> {
        if (isLocalMode(userId)) {
            return localStore!!.observeSequences(projectId)
        }
        val key = cacheKey(userId, projectId)
        val remoteFlow = db.collection("users").document(userId)
            .collection("projects").document(projectId)
            .collection("sequences")
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc -> parseSequenceDocument(doc) }
            }
            .onStart {
                val initialCached = _firestoreSequenceCache.value[key].orEmpty()
                if (initialCached.isNotEmpty()) {
                    emit(initialCached)
                }
            }
        return combine(remoteFlow, _firestoreSequenceCache) { remote, cacheMap ->
            val cached = cacheMap[key].orEmpty()
            val merged = mergeSequenceLists(remote = remote, cached = cached)
            sessionState.sequencesByProject.update { prev -> prev + (projectId to merged) }
            merged
        }
    }

    private fun mergeSequenceLists(
        remote: List<ProductionSequence>,
        cached: List<ProductionSequence>
    ): List<ProductionSequence> {
        if (cached.isEmpty()) return remote.sortedByDescending { it.createdAtMillis }
        val byId = LinkedHashMap<String, ProductionSequence>()
        for (seq in remote) {
            byId[seq.id] = seq
        }
        for (cachedSeq in cached) {
            val remoteSeq = byId[cachedSeq.id]
            if (remoteSeq == null || cachedSeq.updatedAtMillis > remoteSeq.updatedAtMillis) {
                byId[cachedSeq.id] = cachedSeq
            }
        }
        return byId.values.sortedByDescending { it.createdAtMillis }
    }

    suspend fun getSequenceById(
        userId: String,
        projectId: String,
        sequenceId: String
    ): ProductionSequence? {
        if (sequenceId.isBlank()) return null
        if (isLocalMode(userId)) {
            return localStore!!.observeSequences(projectId).first().firstOrNull { it.id == sequenceId }
        }
        val cached = _firestoreSequenceCache.value[cacheKey(userId, projectId)]
            ?.firstOrNull { it.id == sequenceId }
        if (cached != null) return cached
        return try {
            val doc = db.collection("users").document(userId)
                .collection("projects").document(projectId)
                .collection("sequences").document(sequenceId)
                .get()
                .await()
            parseSequenceDocument(doc)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    fun observeAgentMessages(userId: String, projectId: String): Flow<List<AgentMessage>> {
        if (isLocalMode(userId)) {
            return localStore!!.observeAgentMessages(projectId)
        }
        return db.collection("users").document(userId)
            .collection("projects").document(projectId)
            .collection("agent_messages")
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val dto = doc.toObject(
                        AgentMessageDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: return@mapNotNull null
                    AgentMessage(
                        id = dto.id,
                        role = dto.role,
                        focus = AgentRoleFocus.fromWire(dto.focus),
                        content = dto.content,
                        actions = dto.actions.orEmpty(),
                        createdAtMillis = (dto.createdAt ?: Timestamp.now()).toDate().time
                    )
                }.sortedBy { it.createdAtMillis }
            }
    }

    fun observeLearningLessons(userId: String): Flow<List<LearningLesson>> {
        if (isLocalMode(userId)) {
            return localStore!!.observeLearningLessons()
        }
        return db.collection("users").document(userId)
            .collection("learning_memory")
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val dto = doc.toObject(
                        LearningLessonDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: return@mapNotNull null
                    LearningLesson(
                        id = dto.id,
                        category = LearningCategory.fromWire(dto.category),
                        insight = dto.insight,
                        rating = dto.rating.coerceIn(1, 5),
                        sourceProjectTitle = dto.sourceProjectTitle,
                        createdAtMillis = (dto.createdAt ?: Timestamp.now()).toDate().time
                    )
                }.sortedByDescending { it.createdAtMillis }
            }
    }

    private fun parseSequenceDocument(doc: DocumentSnapshot): ProductionSequence? {
        val data = doc.data ?: return null
        val seqMap = data["sequenceData"] as? Map<*, *> ?: emptyMap<String, Any>()
        val rawPalette = seqMap["colorPalette"] as? List<*> ?: emptyList<Any?>()
        val rawShots = seqMap["shotList"] as? List<*> ?: emptyList<Any?>()
        val rawScenes = seqMap["screenplay"] as? List<*> ?: emptyList<Any?>()
        val rawFrames = seqMap["storyboardFrames"] as? List<*> ?: emptyList<Any?>()

        val createdTs = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        val updatedTs = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

        return ProductionSequence(
            id = doc.id,
            projectId = data["projectId"] as? String ?: "",
            ownerId = data["ownerId"] as? String ?: "",
            title = data["title"] as? String ?: "",
            versionStatus = SequenceVersionStatus.fromWire(data["versionStatus"] as? String),
            sourceType = SourceMaterialType.fromWire(data["sourceType"] as? String),
            sourceFilename = seqMap["sourceFilename"] as? String,
            directorStyle = (seqMap["directorStyle"] as? String)?.takeIf { it.isNotBlank() }
                ?: (data["directorStyle"] as? String ?: ""),
            aspectRatio = (seqMap["aspectRatio"] as? String)?.takeIf { it.isNotBlank() }
                ?: (data["aspectRatio"] as? String ?: "2.39:1"),
            cameraSystem = seqMap["cameraSystem"] as? String ?: "ARRI Alexa 65",
            lensPackage = seqMap["lensPackage"] as? String ?: "Cooke Anamorphic /i",
            filmStockOrLut = seqMap["filmStockOrLut"] as? String ?: "Kodak 2383 Print LUT",
            logline = data["logline"] as? String ?: "",
            directorVision = data["directorVision"] as? String ?: "",
            colorPalette = rawPalette.mapNotNull { (it as? Map<*, *>)?.let(ColorSwatch::fromMap) },
            lighting = LightingSetup.fromMap(seqMap["lighting"] as? Map<*, *>),
            shotList = rawShots.mapNotNull { (it as? Map<*, *>)?.let(ShotSpec::fromMap) },
            soundDesign = SoundDesignSpec.fromMap(seqMap["soundDesign"] as? Map<*, *>),
            audioAnalysis = AudioAnalysisReport.fromMap(seqMap["audioAnalysis"] as? Map<*, *>),
            vfx = VfxBreakdown.fromMap(seqMap["vfx"] as? Map<*, *>),
            screenplay = rawScenes.mapNotNull { (it as? Map<*, *>)?.let(ScreenplayScene::fromMap) },
            storyboardFrames = rawFrames.mapNotNull { (it as? Map<*, *>)?.let(StoryboardCard::fromMap) },
            continuitySummary = data["continuitySummary"] as? String ?: "",
            sequenceRating = (data["sequenceRating"] as? Number)?.toInt(),
            createdAtMillis = (createdTs ?: Timestamp.now()).toDate().time,
            updatedAtMillis = (updatedTs ?: createdTs ?: Timestamp.now()).toDate().time
        )
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireUserId(userId)
            val safeName = displayName.trim().ifBlank { "Director LumenReel" }.take(120)
            val safeEmail = email.trim().ifBlank { "${userId}@lumenreel.local" }.take(200)
            val docRef = db.document(path)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "uid" to userId,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "role" to "director",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfileState?> {
        if (isLocalMode(userId)) {
            return flowOf(
                UserProfileState(
                    uid = userId,
                    displayName = "Director Local",
                    email = "",
                    preferredStyle = ""
                )
            )
        }
        return db.collection("users").document(userId)
            .snapshots(MetadataChanges.INCLUDE)
            .map { doc ->
                if (!doc.exists()) null
                else UserProfileState(
                    uid = doc.getString("uid") ?: userId,
                    displayName = doc.getString("displayName") ?: "",
                    email = doc.getString("email") ?: "",
                    preferredStyle = doc.getString("preferredStyle") ?: ""
                )
            }
    }

    suspend fun updateUserPreferredStyle(
        userId: String,
        preferredStyle: String
    ): Result<Unit> {
        if (isLocalMode(userId)) {
            return Result.success(Unit)
        }
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val safeStyle = preferredStyle.trim().take(160)
            val docRef = db.document("users/$uid")
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val user = auth.currentUser
                ensureUserProfile(
                    userId = uid,
                    displayName = user?.displayName ?: "Director LumenReel",
                    email = user?.email ?: "${uid}@lumenreel.local"
                ).getOrThrow()
            }
            docRef.update(
                mapOf(
                    "preferredStyle" to safeStyle,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createProject(
        userId: String,
        title: String,
        genreStyle: String,
        defaultSourceType: SourceMaterialType,
        continuityBible: String
    ): Result<String> {
        if (isLocalMode(userId)) {
            return localStore!!.createProject(userId, title, genreStyle, defaultSourceType, continuityBible)
        }
        val projectId = newSafeDocId("proj")
        val path = "users/$userId/projects/$projectId"
        return try {
            requireUserId(userId)
            val cleanTitle = title.trim().take(180)
            require(cleanTitle.isNotEmpty()) { "El título de la producción es obligatorio." }
            val cleanStyle = genreStyle.trim().ifBlank { "Épica Monumental IMAX" }.take(180)
            val cleanBible = continuityBible.trim().take(20000)

            val payload = mapOf(
                "ownerId" to userId,
                "title" to cleanTitle,
                "genreStyle" to cleanStyle,
                "defaultSourceType" to defaultSourceType.wireValue,
                "continuityBible" to cleanBible,
                "sequencesCount" to 0L,
                "assetsCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.document(path).set(payload).await()
            Result.success(projectId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProjectContinuity(
        userId: String,
        projectId: String,
        continuityBible: String
    ): Result<Unit> {
        if (isLocalMode(userId)) {
            return localStore!!.updateProjectContinuity(projectId, continuityBible)
        }
        val path = "users/$userId/projects/$projectId"
        return try {
            requireUserId(userId)
            db.document(path).update(
                mapOf(
                    "continuityBible" to continuityBible.trim().take(20000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(userId: String, projectId: String): Result<Unit> {
        if (isLocalMode(userId)) {
            return localStore!!.deleteProject(projectId)
        }
        val path = "users/$userId/projects/$projectId"
        return try {
            requireUserId(userId)
            val projRef = db.document(path)
            val seqDocs = projRef.collection("sequences").get().await()
            for (seqDoc in seqDocs.documents) {
                if (seqDoc.getString("versionStatus") == "draft") {
                    seqDoc.reference.delete().await()
                }
            }
            val msgDocs = projRef.collection("agent_messages").get().await()
            for (msgDoc in msgDocs.documents) {
                msgDoc.reference.delete().await()
            }
            projRef.delete().await()
            _firestoreSequenceCache.update { it - cacheKey(userId, projectId) }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveProducedSequence(
        userId: String,
        projectId: String,
        sequence: ProductionSequence,
        nextContinuityBible: String,
        nextSequencesCount: Int
    ): Result<ProductionSequence> {
        if (isLocalMode(userId)) {
            return localStore!!.saveProducedSequence(
                userId,
                projectId,
                sequence,
                nextContinuityBible,
                nextSequencesCount
            ).onSuccess { saved ->
                upsertCachedSequence(userId, projectId, saved)
            }
        }
        val seqId = sequence.id.ifBlank { newSafeDocId("seq") }
        val seqPath = "users/$userId/projects/$projectId/sequences/$seqId"
        return try {
            requireUserId(userId)
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
            val seqPayload = mutableMapOf<String, Any>(
                "projectId" to projectId,
                "ownerId" to userId,
                "title" to cleanSeq.title,
                "versionStatus" to cleanSeq.versionStatus.wireValue,
                "sourceType" to cleanSeq.sourceType.wireValue,
                "directorStyle" to cleanSeq.directorStyle,
                "aspectRatio" to cleanSeq.aspectRatio,
                "logline" to cleanSeq.logline,
                "directorVision" to cleanSeq.directorVision,
                "continuitySummary" to cleanSeq.continuitySummary,
                "sequenceData" to cleanSeq.buildSequenceDataMap(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val projPath = "users/$userId/projects/$projectId"
            val projUpdate = mapOf(
                "sequencesCount" to nextSequencesCount.coerceIn(0, 1000).toLong(),
                "assetsCount" to 0L,
                "continuityBible" to nextContinuityBible.trim().take(20000),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.runBatch { batch ->
                batch.set(db.document(seqPath), seqPayload)
                batch.update(db.document(projPath), projUpdate)
            }.await()

            upsertCachedSequence(userId, projectId, cleanSeq)
            Result.success(cleanSeq)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, seqPath)
            Result.failure(e)
        }
    }

    suspend fun updateDraftSequence(
        userId: String,
        projectId: String,
        sequence: ProductionSequence
    ): Result<Unit> {
        if (isLocalMode(userId)) {
            return localStore!!.updateDraftSequence(sequence).onSuccess {
                upsertCachedSequence(userId, projectId, sequence)
            }
        }
        val path = "users/$userId/projects/$projectId/sequences/${sequence.id}"
        return try {
            requireUserId(userId)
            require(sequence.versionStatus == SequenceVersionStatus.DRAFT) {
                "Solo las secuencias en borrador pueden editarse."
            }
            val now = System.currentTimeMillis()
            val cleanSeq = sequence.copy(
                title = sequence.title.trim().ifBlank { "Secuencia" }.take(200),
                logline = sequence.logline.trim().take(1200),
                directorVision = sequence.directorVision.trim().take(6000),
                continuitySummary = sequence.continuitySummary.trim().take(3000),
                updatedAtMillis = now
            )
            db.document(path).update(
                mapOf(
                    "title" to cleanSeq.title,
                    "logline" to cleanSeq.logline,
                    "directorVision" to cleanSeq.directorVision,
                    "sequenceData" to cleanSeq.buildSequenceDataMap(),
                    "continuitySummary" to cleanSeq.continuitySummary,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            upsertCachedSequence(userId, projectId, cleanSeq)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveStoryboardFrameImagePath(
        userId: String,
        projectId: String,
        sequence: ProductionSequence,
        frameId: String,
        localImagePath: String
    ): Result<Unit> {
        if (isLocalMode(userId)) {
            return localStore!!.updateStoryboardFramePath(sequence.id, frameId, localImagePath)
        }
        if (sequence.versionStatus == SequenceVersionStatus.DRAFT) {
            val updatedFrames = sequence.storyboardFrames.map {
                if (it.id == frameId) it.copy(localImagePath = localImagePath) else it
            }
            return updateDraftSequence(
                userId = userId,
                projectId = projectId,
                sequence = sequence.copy(storyboardFrames = updatedFrames)
            )
        }
        return Result.success(Unit)
    }

    suspend fun finalizeSequence(
        userId: String,
        projectId: String,
        projectTitle: String,
        sequence: ProductionSequence
    ): Result<Unit> {
        if (isLocalMode(userId)) {
            return localStore!!.finalizeSequence(projectTitle, sequence)
        }
        val path = "users/$userId/projects/$projectId/sequences/${sequence.id}"
        return try {
            requireUserId(userId)
            db.document(path).update(
                mapOf(
                    "versionStatus" to SequenceVersionStatus.FINAL.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            upsertCachedSequence(
                userId,
                projectId,
                sequence.copy(
                    versionStatus = SequenceVersionStatus.FINAL,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )

            if (sequence.versionStatus != SequenceVersionStatus.FINAL) {
                val autoLessons = LearningEngine.extractAutomaticLessons(projectTitle, sequence)
                for (lesson in autoLessons) {
                    addLearningLesson(
                        userId = userId,
                        category = lesson.category,
                        insight = lesson.insight,
                        rating = lesson.rating,
                        sourceProjectTitle = lesson.sourceProjectTitle
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun rateSequence(
        userId: String,
        projectId: String,
        projectTitle: String,
        sequence: ProductionSequence,
        rating: Int,
        note: String
    ): Result<Unit> {
        if (isLocalMode(userId)) {
            return localStore!!.rateSequence(projectTitle, sequence, rating, note)
        }
        val path = "users/$userId/projects/$projectId/sequences/${sequence.id}"
        return try {
            requireUserId(userId)
            val safeRating = rating.coerceIn(1, 5)
            db.document(path).update(
                mapOf(
                    "sequenceRating" to safeRating,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            upsertCachedSequence(
                userId,
                projectId,
                sequence.copy(
                    sequenceRating = safeRating,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )

            if (safeRating >= 4) {
                val lessons = StudioIntelligence.buildRatedChoiceLessons(
                    projectTitle = projectTitle,
                    sequence = sequence,
                    rating = safeRating,
                    note = note
                )
                for (lesson in lessons) {
                    addLearningLesson(
                        userId = userId,
                        category = lesson.category,
                        insight = lesson.insight,
                        rating = lesson.rating,
                        sourceProjectTitle = lesson.sourceProjectTitle
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAgentMessagePair(
        userId: String,
        projectId: String,
        focus: AgentRoleFocus,
        userContent: String,
        agentContent: String,
        actions: List<AgentToolAction>
    ): Result<Unit> {
        if (isLocalMode(userId)) {
            return localStore!!.saveAgentMessagePair(projectId, focus, userContent, agentContent, actions)
        }
        val colPath = "users/$userId/projects/$projectId/agent_messages"
        return try {
            requireUserId(userId)
            val userMsgId = newSafeDocId("msg_u")
            val agentMsgId = newSafeDocId("msg_a")
            val userPayload = mapOf(
                "projectId" to projectId,
                "ownerId" to userId,
                "role" to "user",
                "focus" to focus.wireValue,
                "content" to userContent.trim().take(12000),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.document("$colPath/$userMsgId").set(userPayload).await()

            val agentPayload = mutableMapOf<String, Any>(
                "projectId" to projectId,
                "ownerId" to userId,
                "role" to "agent",
                "focus" to focus.wireValue,
                "content" to agentContent.trim().take(12000),
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (actions.isNotEmpty()) {
                agentPayload["actions"] = actions.take(10).map { it.toMap() }
            }
            db.document("$colPath/$agentMsgId").set(agentPayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colPath)
            Result.failure(e)
        }
    }

    suspend fun clearAgentMessages(userId: String, projectId: String): Result<Unit> {
        if (isLocalMode(userId)) {
            return localStore!!.clearAgentMessages(projectId)
        }
        val colPath = "users/$userId/projects/$projectId/agent_messages"
        return try {
            requireUserId(userId)
            val docs = db.collection(colPath).get().await()
            for (d in docs.documents) {
                d.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colPath)
            Result.failure(e)
        }
    }

    suspend fun addLearningLesson(
        userId: String,
        category: LearningCategory,
        insight: String,
        rating: Int,
        sourceProjectTitle: String
    ): Result<Unit> {
        if (isLocalMode(userId)) {
            return localStore!!.addLearningLesson(category, insight, rating, sourceProjectTitle)
        }
        val lessonId = newSafeDocId("lesson")
        val path = "users/$userId/learning_memory/$lessonId"
        return try {
            requireUserId(userId)
            val cleanInsight = insight.trim().take(1000)
            require(cleanInsight.length >= 5) { "La lección debe tener al menos 5 caracteres." }
            val payload = mapOf(
                "ownerId" to userId,
                "category" to category.wireValue,
                "insight" to cleanInsight,
                "rating" to rating.coerceIn(1, 5),
                "sourceProjectTitle" to sourceProjectTitle.trim().ifBlank { "LumenReel Studio" }.take(180),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.document(path).set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteLearningLesson(userId: String, lessonId: String): Result<Unit> {
        if (isLocalMode(userId)) {
            return localStore!!.deleteLearningLesson(lessonId)
        }
        val path = "users/$userId/learning_memory/$lessonId"
        return try {
            requireUserId(userId)
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var instance: StudioRepository? = null

        fun getInstance(context: Context): StudioRepository =
            instance ?: synchronized(this) {
                instance ?: StudioRepository(context.applicationContext).also { instance = it }
            }
    }
}
