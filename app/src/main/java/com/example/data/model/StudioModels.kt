package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.Serializable

enum class SourceMaterialType(val wireValue: String) {
    BOOK("book"),
    AUDIOBOOK("audiobook"),
    STORY("story");

    companion object {
        fun fromWire(raw: String?): SourceMaterialType =
            entries.firstOrNull { it.wireValue == raw?.lowercase() } ?: BOOK
    }
}

enum class WorkspaceTab(val routeKey: String) {
    AGENT("agent"),
    VISION("vision"),
    SHOTS("shots"),
    SCREENPLAY("screenplay"),
    STORYBOARD("storyboard");

    companion object {
        fun fromRouteKey(raw: String?): WorkspaceTab =
            entries.firstOrNull { it.routeKey == raw?.lowercase() } ?: AGENT
    }
}

enum class AgentRoleFocus(val wireValue: String) {
    COUNCIL("council"),
    DIRECTOR("director"),
    SCREENWRITER("screenwriter"),
    VFX("vfx");

    companion object {
        fun fromWire(raw: String?): AgentRoleFocus =
            entries.firstOrNull { it.wireValue == raw?.lowercase() } ?: COUNCIL
    }
}

enum class SequenceVersionStatus(val wireValue: String) {
    DRAFT("draft"),
    FINAL("final");

    companion object {
        fun fromWire(raw: String?): SequenceVersionStatus =
            entries.firstOrNull { it.wireValue == raw?.lowercase() } ?: DRAFT
    }
}

enum class ThinkingDepth(val label: String) {
    STANDARD("Estándar"),
    DEEP("Profundo");

    companion object {
        fun fromWire(raw: String?): ThinkingDepth =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: STANDARD
    }
}

enum class LearningCategory(val wireValue: String) {
    CAMERA("camera"),
    LIGHTING("lighting"),
    SOUND("sound"),
    SCREENPLAY("screenplay"),
    PACING("pacing"),
    CONTINUITY("continuity");

    companion object {
        fun fromWire(raw: String?): LearningCategory =
            entries.firstOrNull { it.wireValue == raw?.lowercase() } ?: CAMERA
    }
}

@Keep
@IgnoreExtraProperties
@Serializable
data class AgentToolAction(
    val toolName: String = "",
    val summary: String = "",
    val affectedArea: String = "direccion"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "toolName" to toolName.take(120),
        "summary" to summary.take(500),
        "affectedArea" to affectedArea.take(40)
    )
}

@Keep
@IgnoreExtraProperties
data class AgentMessageDoc(
    @DocumentId val id: String = "",
    val projectId: String = "",
    val ownerId: String = "",
    val role: String = "agent",
    val focus: String = "council",
    val content: String = "",
    val actions: List<AgentToolAction>? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class AgentMessage(
    val id: String = "",
    val role: String = "agent",
    val focus: AgentRoleFocus = AgentRoleFocus.COUNCIL,
    val content: String = "",
    val actions: List<AgentToolAction> = emptyList(),
    val createdAtMillis: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class LearningLessonDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val category: String = "camera",
    val insight: String = "",
    val rating: Int = 5,
    val sourceProjectTitle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class LearningLesson(
    val id: String = "",
    val category: LearningCategory = LearningCategory.CAMERA,
    val insight: String = "",
    val rating: Int = 5,
    val sourceProjectTitle: String = "",
    val createdAtMillis: Long = 0L
)

@Keep
@IgnoreExtraProperties
@Serializable
data class AudioAnalysisReport(
    val transcription: String = "",
    val narrationStyle: String = "",
    val emotionalArc: String = "",
    val rhythmAndPacing: String = "",
    val detectedSoundEffects: List<String> = emptyList(),
    val directingRecommendations: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "transcription" to transcription,
        "narrationStyle" to narrationStyle,
        "emotionalArc" to emotionalArc,
        "rhythmAndPacing" to rhythmAndPacing,
        "detectedSoundEffects" to detectedSoundEffects,
        "directingRecommendations" to directingRecommendations
    )

    companion object {
        fun fromMap(map: Map<*, *>?): AudioAnalysisReport? {
            if (map == null) return null
            val effectsRaw = map["detectedSoundEffects"] as? List<*> ?: emptyList<Any?>()
            return AudioAnalysisReport(
                transcription = map["transcription"] as? String ?: "",
                narrationStyle = map["narrationStyle"] as? String ?: "",
                emotionalArc = map["emotionalArc"] as? String ?: "",
                rhythmAndPacing = map["rhythmAndPacing"] as? String ?: "",
                detectedSoundEffects = effectsRaw.mapNotNull { it?.toString() },
                directingRecommendations = map["directingRecommendations"] as? String ?: ""
            )
        }
    }
}

@Keep
@IgnoreExtraProperties
@Serializable
data class ColorSwatch(
    val name: String = "",
    val hex: String = "#E5A93C",
    val role: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "hex" to hex,
        "role" to role
    )

    companion object {
        fun fromMap(map: Map<*, *>): ColorSwatch = ColorSwatch(
            name = map["name"] as? String ?: "",
            hex = map["hex"] as? String ?: "#E5A93C",
            role = map["role"] as? String ?: ""
        )
    }
}

@Keep
@IgnoreExtraProperties
@Serializable
data class ShotSpec(
    val shotNumber: String = "",
    val shotType: String = "",
    val lensMm: String = "",
    val movement: String = "",
    val durationSec: Double = 4.0,
    val framingNote: String = "",
    val psychologicalIntent: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "shotNumber" to shotNumber,
        "shotType" to shotType,
        "lensMm" to lensMm,
        "movement" to movement,
        "durationSec" to durationSec,
        "framingNote" to framingNote,
        "psychologicalIntent" to psychologicalIntent
    )

    companion object {
        fun fromMap(map: Map<*, *>): ShotSpec = ShotSpec(
            shotNumber = map["shotNumber"] as? String ?: "",
            shotType = map["shotType"] as? String ?: "",
            lensMm = map["lensMm"] as? String ?: "",
            movement = map["movement"] as? String ?: "",
            durationSec = (map["durationSec"] as? Number)?.toDouble() ?: 4.0,
            framingNote = map["framingNote"] as? String ?: "",
            psychologicalIntent = map["psychologicalIntent"] as? String ?: ""
        )
    }
}

@Keep
@IgnoreExtraProperties
@Serializable
data class LightingSetup(
    val keyLight: String = "",
    val fillRatio: String = "",
    val rimAndBacklight: String = "",
    val colorTemperatureKelvin: String = "",
    val atmosphereAndHaze: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "keyLight" to keyLight,
        "fillRatio" to fillRatio,
        "rimAndBacklight" to rimAndBacklight,
        "colorTemperatureKelvin" to colorTemperatureKelvin,
        "atmosphereAndHaze" to atmosphereAndHaze
    )

    companion object {
        fun fromMap(map: Map<*, *>?): LightingSetup = LightingSetup(
            keyLight = map?.get("keyLight") as? String ?: "",
            fillRatio = map?.get("fillRatio") as? String ?: "",
            rimAndBacklight = map?.get("rimAndBacklight") as? String ?: "",
            colorTemperatureKelvin = map?.get("colorTemperatureKelvin") as? String ?: "",
            atmosphereAndHaze = map?.get("atmosphereAndHaze") as? String ?: ""
        )
    }
}

@Keep
@IgnoreExtraProperties
@Serializable
data class SoundDesignSpec(
    val atmosLayers: List<String> = emptyList(),
    val subBassCue: String = "",
    val foleyDetails: List<String> = emptyList(),
    val silenceBeats: String = "",
    val musicalScore: String = "",
    val tempoBpm: Int = 72
) {
    fun toMap(): Map<String, Any> = mapOf(
        "atmosLayers" to atmosLayers,
        "subBassCue" to subBassCue,
        "foleyDetails" to foleyDetails,
        "silenceBeats" to silenceBeats,
        "musicalScore" to musicalScore,
        "tempoBpm" to tempoBpm
    )

    companion object {
        fun fromMap(map: Map<*, *>?): SoundDesignSpec {
            val atmos = (map?.get("atmosLayers") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val foley = (map?.get("foleyDetails") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            return SoundDesignSpec(
                atmosLayers = atmos,
                subBassCue = map?.get("subBassCue") as? String ?: "",
                foleyDetails = foley,
                silenceBeats = map?.get("silenceBeats") as? String ?: "",
                musicalScore = map?.get("musicalScore") as? String ?: "",
                tempoBpm = (map?.get("tempoBpm") as? Number)?.toInt() ?: 72
            )
        }
    }
}

@Keep
@IgnoreExtraProperties
@Serializable
data class VfxBreakdown(
    val cgiElements: List<String> = emptyList(),
    val practicalSfx: List<String> = emptyList(),
    val locationsAndSets: String = "",
    val wardrobeAndMakeup: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "cgiElements" to cgiElements,
        "practicalSfx" to practicalSfx,
        "locationsAndSets" to locationsAndSets,
        "wardrobeAndMakeup" to wardrobeAndMakeup
    )

    companion object {
        fun fromMap(map: Map<*, *>?): VfxBreakdown {
            val cgi = (map?.get("cgiElements") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val sfx = (map?.get("practicalSfx") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            return VfxBreakdown(
                cgiElements = cgi,
                practicalSfx = sfx,
                locationsAndSets = map?.get("locationsAndSets") as? String ?: "",
                wardrobeAndMakeup = map?.get("wardrobeAndMakeup") as? String ?: ""
            )
        }
    }
}

@Keep
@IgnoreExtraProperties
@Serializable
data class ScreenplayBeat(
    val type: String = "action", // action | dialogue | camera | sound
    val character: String? = null,
    val parenthetical: String? = null,
    val text: String = ""
) {
    fun toMap(): Map<String, Any> {
        val out = mutableMapOf<String, Any>(
            "type" to type,
            "text" to text
        )
        if (!character.isNullOrBlank()) out["character"] = character
        if (!parenthetical.isNullOrBlank()) out["parenthetical"] = parenthetical
        return out
    }

    companion object {
        fun fromMap(map: Map<*, *>): ScreenplayBeat = ScreenplayBeat(
            type = map["type"] as? String ?: "action",
            character = map["character"] as? String,
            parenthetical = map["parenthetical"] as? String,
            text = map["text"] as? String ?: ""
        )
    }
}

@Keep
@IgnoreExtraProperties
@Serializable
data class ScreenplayScene(
    val sceneNumber: String = "1",
    val slugline: String = "",
    val timeOfDay: String = "NOCHE",
    val beats: List<ScreenplayBeat> = emptyList()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "sceneNumber" to sceneNumber,
        "slugline" to slugline,
        "timeOfDay" to timeOfDay,
        "beats" to beats.map { it.toMap() }
    )

    companion object {
        fun fromMap(map: Map<*, *>): ScreenplayScene {
            val rawBeats = map["beats"] as? List<*> ?: emptyList<Any?>()
            return ScreenplayScene(
                sceneNumber = map["sceneNumber"] as? String ?: "1",
                slugline = map["slugline"] as? String ?: "",
                timeOfDay = map["timeOfDay"] as? String ?: "NOCHE",
                beats = rawBeats.mapNotNull { item ->
                    (item as? Map<*, *>)?.let { ScreenplayBeat.fromMap(it) }
                }
            )
        }
    }
}

@Keep
@IgnoreExtraProperties
@Serializable
data class StoryboardCard(
    val id: String = "",
    val title: String = "",
    val shotRef: String = "",
    val aspectRatio: String = "2.39:1",
    val cameraSpec: String = "",
    val localImagePath: String? = null,
    val visualPrompt: String = "",
    val veoMotionPrompt: String = ""
) {
    fun toMap(): Map<String, Any> {
        val out = mutableMapOf<String, Any>(
            "id" to id,
            "title" to title,
            "shotRef" to shotRef,
            "aspectRatio" to aspectRatio,
            "cameraSpec" to cameraSpec,
            "visualPrompt" to visualPrompt,
            "veoMotionPrompt" to veoMotionPrompt
        )
        if (!localImagePath.isNullOrBlank()) out["localImagePath"] = localImagePath
        return out
    }

    companion object {
        fun fromMap(map: Map<*, *>): StoryboardCard = StoryboardCard(
            id = map["id"] as? String ?: "",
            title = map["title"] as? String ?: "",
            shotRef = map["shotRef"] as? String ?: "",
            aspectRatio = map["aspectRatio"] as? String ?: "2.39:1",
            cameraSpec = map["cameraSpec"] as? String ?: "",
            localImagePath = map["localImagePath"] as? String,
            visualPrompt = map["visualPrompt"] as? String ?: "",
            veoMotionPrompt = map["veoMotionPrompt"] as? String ?: ""
        )
    }
}

data class ProductionSequence(
    val id: String = "",
    val projectId: String = "",
    val ownerId: String = "",
    val title: String = "",
    val versionStatus: SequenceVersionStatus = SequenceVersionStatus.DRAFT,
    val sourceType: SourceMaterialType = SourceMaterialType.BOOK,
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
    fun buildSequenceDataMap(): Map<String, Any> {
        val data = mutableMapOf<String, Any>(
            "directorStyle" to directorStyle,
            "aspectRatio" to aspectRatio,
            "cameraSystem" to cameraSystem,
            "lensPackage" to lensPackage,
            "filmStockOrLut" to filmStockOrLut,
            "colorPalette" to colorPalette.map { it.toMap() },
            "lighting" to lighting.toMap(),
            "shotList" to shotList.map { it.toMap() },
            "soundDesign" to soundDesign.toMap(),
            "vfx" to vfx.toMap(),
            "screenplay" to screenplay.map { it.toMap() },
            "storyboardFrames" to storyboardFrames.map { it.toMap() }
        )
        if (!sourceFilename.isNullOrBlank()) data["sourceFilename"] = sourceFilename
        if (audioAnalysis != null) data["audioAnalysis"] = audioAnalysis.toMap()
        return data
    }
}

@Keep
@IgnoreExtraProperties
@Serializable
data class ProductionFolder(
    val id: String = "",
    val name: String = "",
    val category: String = "script", // script | bible | schedule | visual | sound | vfx | field
    val description: String = "",
    val notes: String = "",
    val targetTab: String = "screenplay", // screenplay | vision | shots | storyboard | agent
    val linkedSequenceIds: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id.take(64),
        "name" to name.take(120),
        "category" to category.take(40),
        "description" to description.take(300),
        "notes" to notes.take(2000),
        "targetTab" to targetTab.take(40),
        "linkedSequenceIds" to linkedSequenceIds.take(50)
    )

    companion object {
        fun fromMap(map: Map<*, *>): ProductionFolder = ProductionFolder(
            id = map["id"] as? String ?: "",
            name = map["name"] as? String ?: "",
            category = map["category"] as? String ?: "script",
            description = map["description"] as? String ?: "",
            notes = map["notes"] as? String ?: "",
            targetTab = map["targetTab"] as? String ?: "screenplay",
            linkedSequenceIds = (map["linkedSequenceIds"] as? List<*>)
                ?.mapNotNull { it as? String }
                ?.take(50)
                ?: emptyList()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class StudioProjectDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val genreStyle: String = "",
    val defaultSourceType: String = "book",
    val continuityBible: String = "",
    val templateId: String = "custom",
    val productionFolders: List<ProductionFolder>? = null,
    val sequencesCount: Long = 0L,
    val assetsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ProductionProject(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val genreStyle: String = "",
    val defaultSourceType: SourceMaterialType = SourceMaterialType.BOOK,
    val continuityBible: String = "",
    val templateId: String = "custom",
    val productionFolders: List<ProductionFolder> = emptyList(),
    val sequencesCount: Int = 0,
    val assetsCount: Int = 0,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val sequences: List<ProductionSequence> = emptyList(),
    val agentMessages: List<AgentMessage> = emptyList()
)

data class DirectorStylePreset(
    val id: String,
    val label: String,
    val references: String,
    val defaultCamera: String,
    val defaultLenses: String,
    val defaultRatio: String,
    val description: String
)

data class PatternMetric(
    val label: String,
    val detail: String,
    val occurrences: Int,
    val avgRating: Double,
    val sourceProjects: List<String>
)

data class DirectorialEnhancement(
    val id: String,
    val category: String, // "camera" | "lighting" | "sound"
    val title: String,
    val recommendation: String,
    val rationale: String,
    val sourceProjectTitle: String,
    val confidenceScore: Int,
    val continuitySnippet: String
)

data class StudioIntelligenceReport(
    val totalTrackedOutcomes: Int,
    val finalizedSequencesCount: Int,
    val topCameraPatterns: List<PatternMetric>,
    val topLightingSetups: List<PatternMetric>,
    val topSoundscapes: List<PatternMetric>,
    val enhancements: List<DirectorialEnhancement>
)

data class AttachedSourceFile(
    val filename: String,
    val mimeType: String,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AttachedSourceFile) return false
        return filename == other.filename && mimeType == other.mimeType && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

data class IngestDraftState(
    val sequenceTitle: String = "",
    val sourceType: SourceMaterialType = SourceMaterialType.BOOK,
    val directorStyle: String = "Épica Monumental IMAX",
    val aspectRatio: String = "2.39:1",
    val thinkingDepth: ThinkingDepth = ThinkingDepth.STANDARD,
    val continuityBible: String = "",
    val bibleInitialized: Boolean = false,
    val sourceText: String = "",
    val attachedFile: AttachedSourceFile? = null,
    val fileError: String? = null,
    val ingestError: String? = null
)

data class DirectorDraftEdits(
    val directorStyle: String = "",
    val aspectRatio: String = "2.39:1",
    val cameraSystem: String = "",
    val lensPackage: String = "",
    val filmStockOrLut: String = "",
    val colorTempKelvin: String = "",
    val directorVision: String = "",
    val continuityBible: String = "",
    val isDirty: Boolean = false
)

enum class CalendarMilestoneType(val wireValue: String, val displayLabel: String) {
    SHOOTING_DAY("shooting_day", "Día de rodaje"),
    DEADLINE("deadline", "Fecha límite"),
    REVIEW("review", "Revisión técnica"),
    PREMIERE("premiere", "Entrega / Estreno");

    companion object {
        fun fromWire(raw: String?): CalendarMilestoneType =
            entries.firstOrNull { it.wireValue == raw?.lowercase() } ?: SHOOTING_DAY
    }
}

@Keep
@IgnoreExtraProperties
@Serializable
data class ProductionMilestoneItem(
    val id: String = "",
    val projectId: String = "",
    val ownerId: String = "",
    val title: String = "",
    val notes: String = "",
    val dateMillis: Long = 0L,
    val milestoneType: String = CalendarMilestoneType.SHOOTING_DAY.wireValue,
    val sequenceId: String? = null,
    val completed: Boolean = false,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun resolvedType(): CalendarMilestoneType = CalendarMilestoneType.fromWire(milestoneType)
}

@Keep
@IgnoreExtraProperties
@Serializable
data class StoryboardAssetOverride(
    val id: String = "",
    val projectId: String = "",
    val sequenceId: String = "",
    val frameId: String = "",
    val localImagePath: String = "",
    val updatedAtMillis: Long = 0L
)

@Serializable
data class UserStudioConfig(
    val themeMode: String = "dark",
    val preferredStyle: String = "Épica Monumental IMAX",
    val defaultAspectRatio: String = "2.39:1",
    val defaultThinkingDepth: String = "STANDARD",
    val autoSaveDrafts: Boolean = true,
    val stage3DDefault: Boolean = true,
    val lastProjectId: String = "",
    val lastSequenceId: String = "",
    val lastTab: String = "agent",
    val gflowEndpointUrl: String = "https://api.gflow.dev/v1",
    val gflowApiKey: String = "",
    val gflowEnabled: Boolean = false
) {
    val isDarkMode: Boolean get() = themeMode != "light"
}

enum class SystemErrorSource(val wireValue: String, val label: String) {
    AI("ai", "Motor IA Gemini"),
    FIRESTORE("firestore", "Cloud Firestore"),
    AUTH("auth", "Sesión Google"),
    STORAGE("storage", "Archivos y PDF"),
    AUDIO("audio", "Audio y Micrófono"),
    SYSTEM("system", "Sistema y Validación")
}

enum class SystemErrorSeverity(val wireValue: String, val label: String) {
    CRITICAL("critical", "Crítico"),
    WARNING("warning", "Advertencia"),
    INFO("info", "Diagnóstico")
}

enum class SystemErrorStatus(val wireValue: String, val label: String) {
    OPEN("open", "Pendiente"),
    REPAIRED("repaired", "Reparado")
}

enum class SystemRepairAction(val wireValue: String, val label: String) {
    REPAIR_SYNC_AND_CACHE("repair_sync_cache", "Reparar caché y sincronización"),
    RESET_CORRUPT_DRAFTS("reset_corrupt_drafts", "Sanear borradores y selección"),
    VERIFY_AUTH_AND_AI("verify_auth_ai", "Verificar sesión y App Check"),
    CLEAR_INVALID_MEDIA("clear_invalid_media", "Reparar adjuntos / storyboards"),
    RUN_FULL_SELF_CHECK("run_full_self_check", "Auto-reparación integral")
}

@Serializable
data class SystemErrorEntry(
    val id: String = "",
    val timestampMillis: Long = 0L,
    val source: SystemErrorSource = SystemErrorSource.SYSTEM,
    val severity: SystemErrorSeverity = SystemErrorSeverity.WARNING,
    val title: String = "",
    val message: String = "",
    val technicalDetail: String = "",
    val rootCauseHint: String = "",
    val suggestedFix: String = "",
    val status: SystemErrorStatus = SystemErrorStatus.OPEN,
    val repairAction: SystemRepairAction = SystemRepairAction.RUN_FULL_SELF_CHECK,
    val repairNote: String = "",
    val repairedAtMillis: Long = 0L,
    val projectId: String = ""
)

@Keep
@IgnoreExtraProperties
data class ScriptVersionDoc(
    @DocumentId val id: String = "",
    val projectId: String = "",
    val ownerId: String = "",
    val sequenceId: String = "",
    val sequenceTitle: String = "",
    val versionNumber: Long = 1L,
    val versionLabel: String = "",
    val changeNote: String = "",
    val sceneCount: Long = 0L,
    val wordCount: Long = 0L,
    val scenes: List<ScreenplayScene>? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
@Serializable
data class ScriptVersion(
    val id: String = "",
    val projectId: String = "",
    val ownerId: String = "",
    val sequenceId: String = "",
    val sequenceTitle: String = "",
    val versionNumber: Int = 1,
    val versionLabel: String = "",
    val changeNote: String = "",
    val sceneCount: Int = 0,
    val wordCount: Int = 0,
    val scenes: List<ScreenplayScene> = emptyList(),
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    companion object {
        fun computeWordCount(scenes: List<ScreenplayScene>): Int =
            scenes.sumOf { scene ->
                val slugWords = scene.slugline.trim().split(Regex("\\s+")).count { it.isNotBlank() }
                val beatWords = scene.beats.sumOf { beat ->
                    beat.text.trim().split(Regex("\\s+")).count { it.isNotBlank() } +
                        (beat.character?.trim()?.split(Regex("\\s+"))?.count { it.isNotBlank() } ?: 0)
                }
                slugWords + beatWords
            }
    }
}



