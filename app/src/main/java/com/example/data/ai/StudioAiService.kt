package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AgentMessage
import com.example.data.model.AgentRoleFocus
import com.example.data.model.AgentToolAction
import com.example.data.model.AudioAnalysisReport
import com.example.data.model.ColorSwatch
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
import com.example.data.model.VfxBreakdown
import com.example.data.util.AgentLogic
import com.example.data.util.StudioLogic
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private const val TAG = "StudioAiService"
private const val MODEL_GEMINI = "gemini-3.8-flash"
private const val MODEL_IMAGE = "gemini-nano-banana-2.1"
private const val MAX_INLINE_BYTES = 19 * 1024 * 1024

private const val PRODUCER_SYSTEM_INSTRUCTION = """Eres LumenReel, un Agente Cinematográfico de élite en Hollywood que combina tres maestrías galardonadas: Director de Cine, Guionista Profesional y Experto en Efectos Especiales (VFX & SFX Prácticos).
Tu sello personal es convertir cualquier material fuente —capítulos de libros, audiolibros narrados, relatos cortos o sinopsis— en una producción cinematográfica monumental de clase mundial.

REGLAS DE ORO DEL AGENTE (DIRECTOR · GUIONISTA · EXPERTO VFX/SFX):
1. FIDELIDAD ABSOLUTA AL MATERIAL FUENTE: Respeta los eventos, nombres propios, tono emocional, diálogos clave y atmósfera de la obra proporcionada. Si recibes audio, escucha narración, tono emocional, ritmo, pausas y efectos sonoros para elaborar el informe de dirección y transcripción.
2. DIRECCIÓN DE FOTOGRAFÍA Y PUESTA EN ESCENA: Especifica cuerpos de cámara reales (ARRI Alexa 65, Panavision Millennium DXL2, RED V-Raptor XL, Sony VENICE 2), ópticas concretas, distancias focales en mm, iluminación por temperaturas Kelvin y movimientos motivados.
3. INGENIERÍA DE EFECTOS ESPECIALES: Detalla simulaciones CGI, composición digital e integración directa con efectos prácticos en set (rigs, lluvia controlada, hazer/humo, vestuario y caracterización).
4. GUION CINEMATOGRÁFICO PROFESIONAL: Redacta escenas completas en español con sluglines estándar, subtexto dramático, beats de acción visual ("Show, don't tell"), indicaciones técnicas de cámara y sonido, y diálogos con acotaciones precisas.
5. INGENIERÍA SONORA DOLBY ATMOS: Diseña capas ambientales, frecuencias subgraves concretas (Hz), foley hiperrealista, silencios dramáticos y score musical con tempo en BPM.
6. STORYBOARD PROMPTS: Para cada tarjeta de storyboard, escribe el prompt visual detallado en inglés y el prompt de movimiento de cámara en inglés.
7. AUTOAPRENDIZAJE CONTINUO: Aplica activamente las lecciones acumuladas por el estudio que recibes en el contexto."""

data class AgentChatOutcome(
    val replyText: String,
    val actions: List<AgentToolAction>,
    val updatedProject: ProductionProject,
    val updatedDraftSequence: ProductionSequence?,
    val bibleChanged: Boolean
)

class StudioAiService {

    private val audioAnalysisSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "transcription" to Schema.string("Transcripción fiel y completa del audio"),
                "narrationStyle" to Schema.string("Cadencia vocal, dicción, timbre e intención actoral"),
                "emotionalArc" to Schema.string("Evolución emocional y subtexto dramático"),
                "rhythmAndPacing" to Schema.string("Estructura rítmica, pausas y cortes de montaje"),
                "detectedSoundEffects" to Schema.array(Schema.string(), "Efectos sonoros o ambientes detectados"),
                "directingRecommendations" to Schema.string("Recomendaciones de cámara, luz y Atmos")
            )
        )
    }

    private val productionSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "cameraSystem" to Schema.string("Cámara cinematográfica"),
                "lensPackage" to Schema.string("Paquete de lentes"),
                "filmStockOrLut" to Schema.string("Emulsión o LUT maestro"),
                "logline" to Schema.string("Logline dramático de la secuencia"),
                "directorVision" to Schema.string("Visión completa de dirección y estética"),
                "colorPalette" to Schema.array(
                    Schema.obj(
                        properties = mapOf(
                            "name" to Schema.string(),
                            "hex" to Schema.string("Color hexadecimal #RRGGBB"),
                            "role" to Schema.string()
                        )
                    )
                ),
                "lighting" to Schema.obj(
                    properties = mapOf(
                        "keyLight" to Schema.string(),
                        "fillRatio" to Schema.string(),
                        "rimAndBacklight" to Schema.string(),
                        "colorTemperatureKelvin" to Schema.string(),
                        "atmosphereAndHaze" to Schema.string()
                    )
                ),
                "shotList" to Schema.array(
                    Schema.obj(
                        properties = mapOf(
                            "shotNumber" to Schema.string(),
                            "shotType" to Schema.string(),
                            "lensMm" to Schema.string(),
                            "movement" to Schema.string(),
                            "durationSec" to Schema.double(),
                            "framingNote" to Schema.string(),
                            "psychologicalIntent" to Schema.string()
                        )
                    )
                ),
                "soundDesign" to Schema.obj(
                    properties = mapOf(
                        "atmosLayers" to Schema.array(Schema.string()),
                        "subBassCue" to Schema.string(),
                        "foleyDetails" to Schema.array(Schema.string()),
                        "silenceBeats" to Schema.string(),
                        "musicalScore" to Schema.string(),
                        "tempoBpm" to Schema.integer()
                    )
                ),
                "audioAnalysis" to audioAnalysisSchema,
                "vfx" to Schema.obj(
                    properties = mapOf(
                        "cgiElements" to Schema.array(Schema.string()),
                        "practicalSfx" to Schema.array(Schema.string()),
                        "locationsAndSets" to Schema.string(),
                        "wardrobeAndMakeup" to Schema.string()
                    )
                ),
                "screenplay" to Schema.array(
                    Schema.obj(
                        properties = mapOf(
                            "sceneNumber" to Schema.string(),
                            "slugline" to Schema.string(),
                            "timeOfDay" to Schema.string(),
                            "beats" to Schema.array(
                                Schema.obj(
                                    properties = mapOf(
                                        "type" to Schema.enumeration(listOf("action", "dialogue", "camera", "sound")),
                                        "character" to Schema.string(),
                                        "parenthetical" to Schema.string(),
                                        "text" to Schema.string()
                                    ),
                                    optionalProperties = listOf("character", "parenthetical")
                                )
                            )
                        )
                    )
                ),
                "storyboardFrames" to Schema.array(
                    Schema.obj(
                        properties = mapOf(
                            "title" to Schema.string(),
                            "shotRef" to Schema.string(),
                            "cameraSpec" to Schema.string(),
                            "visualPrompt" to Schema.string("Detailed visual prompt in English"),
                            "veoMotionPrompt" to Schema.string("Detailed camera motion prompt in English")
                        )
                    )
                ),
                "continuitySummary" to Schema.string("Resumen de continuidad narrativa y visual")
            ),
            optionalProperties = listOf("audioAnalysis")
        )
    }

    private val productionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_GEMINI,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = productionSchema
            },
            systemInstruction = content { text(PRODUCER_SYSTEM_INSTRUCTION) }
        )
    }

    private val audioAnalysisModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_GEMINI,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = audioAnalysisSchema
            },
            systemInstruction = content { text(PRODUCER_SYSTEM_INSTRUCTION) }
        )
    }

    private val agentToolDeclarations: List<FunctionDeclaration> by lazy {
        listOf(
            FunctionDeclaration(
                name = "actualizar_biblia_continuidad",
                description = "Guarda o amplía reglas narrativas, visuales, de estilo o de efectos especiales en la Biblia de Continuidad del proyecto activo.",
                parameters = mapOf(
                    "reglasContinuidad" to Schema.string("Texto claro con las reglas o notas de continuidad a registrar."),
                    "modo" to Schema.string("\"append\" para añadir al historial existente o \"replace\" para reescribir.")
                ),
                optionalParameters = listOf("modo")
            ),
            FunctionDeclaration(
                name = "ajustar_direccion_y_camara",
                description = "Actualiza la visión de dirección, sistema de cámara, lentes o esquema de iluminación Kelvin de la secuencia activa (rol Director de Cine).",
                parameters = mapOf(
                    "directorVision" to Schema.string(),
                    "cameraSystem" to Schema.string(),
                    "lensPackage" to Schema.string(),
                    "keyLight" to Schema.string(),
                    "colorTemperatureKelvin" to Schema.string(),
                    "atmosphereAndHaze" to Schema.string()
                ),
                optionalParameters = listOf(
                    "directorVision",
                    "cameraSystem",
                    "lensPackage",
                    "keyLight",
                    "colorTemperatureKelvin",
                    "atmosphereAndHaze"
                )
            ),
            FunctionDeclaration(
                name = "disenar_paquete_vfx_sfx",
                description = "Agrega o actualiza elementos CGI, efectos prácticos en set (SFX), locaciones o caracterización en la secuencia activa.",
                parameters = mapOf(
                    "cgiElements" to Schema.array(Schema.string()),
                    "practicalSfx" to Schema.array(Schema.string()),
                    "locationsAndSets" to Schema.string(),
                    "wardrobeAndMakeup" to Schema.string(),
                    "modo" to Schema.string("\"append\" o \"replace\"")
                ),
                optionalParameters = listOf(
                    "cgiElements",
                    "practicalSfx",
                    "locationsAndSets",
                    "wardrobeAndMakeup",
                    "modo"
                )
            ),
            FunctionDeclaration(
                name = "refinar_guion_escena",
                description = "Reescribe una escena existente por sceneNumber o añade una nueva escena al guion de la secuencia activa.",
                parameters = mapOf(
                    "sceneNumber" to Schema.string(),
                    "slugline" to Schema.string(),
                    "timeOfDay" to Schema.string(),
                    "beats" to Schema.array(
                        Schema.obj(
                            properties = mapOf(
                                "type" to Schema.enumeration(listOf("action", "dialogue", "camera", "sound")),
                                "character" to Schema.string(),
                                "parenthetical" to Schema.string(),
                                "text" to Schema.string()
                            ),
                            optionalProperties = listOf("character", "parenthetical")
                        )
                    )
                ),
                optionalParameters = listOf("timeOfDay")
            ),
            FunctionDeclaration(
                name = "agregar_plano_storyboard",
                description = "Añade una nueva tarjeta de storyboard a la secuencia activa con prompts en inglés.",
                parameters = mapOf(
                    "title" to Schema.string(),
                    "shotRef" to Schema.string(),
                    "cameraSpec" to Schema.string(),
                    "visualPrompt" to Schema.string(),
                    "veoMotionPrompt" to Schema.string()
                ),
                optionalParameters = listOf("shotRef", "cameraSpec")
            )
        )
    }

    private val agentModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_GEMINI,
            tools = listOf(Tool.functionDeclarations(agentToolDeclarations)),
            systemInstruction = content { text(AgentLogic.TRIPARTITE_AGENT_INSTRUCTION) }
        )
    }

    suspend fun analyzeAudio(
        filename: String,
        mimeType: String,
        audioBytes: ByteArray,
        learningContext: String
    ): AudioAnalysisReport {
        require(audioBytes.size <= MAX_INLINE_BYTES) {
            "El archivo de audio excede el límite de 19 MB para envío directo."
        }
        val prompt = """Analiza este archivo de audio cinematográfico ($filename).
MEMORIA DE AUTOAPRENDIZAJE DEL ESTUDIO:
$learningContext

Genera un informe JSON profesional en español con:
1) transcription: transcripción verbatim completa en su idioma original.
2) narrationStyle: cadencia vocal, dicción, timbre e intención actoral.
3) emotionalArc: evolución emocional y subtexto dramático.
4) rhythmAndPacing: estructura rítmica, pausas y cortes de montaje recomendados.
5) detectedSoundEffects: lista detallada de efectos sonoros o ambientes detectados.
6) directingRecommendations: indicaciones de cámara, luz y mezcla Dolby Atmos."""

        val response = withTimeout(60_000L) {
            audioAnalysisModel.generateContent(
                content {
                    inlineData(audioBytes, mimeType)
                    text(prompt)
                }
            )
        }
        val jsonText = response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Gemini no devolvió análisis de audio.")
        return parseAudioAnalysis(JSONObject(jsonText))
    }

    suspend fun produceSequence(
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
        val isBinaryAttachment = attachedBytes != null && attachedMimeType != null &&
            !attachedMimeType.startsWith("text/")
        if (attachedBytes != null && isBinaryAttachment) {
            require(attachedBytes.size <= MAX_INLINE_BYTES) {
                "El archivo adjunto excede el máximo de 19 MB en línea."
            }
        }
        val isAudio = isAudioMimeOrName(attachedMimeType, attachedFilename)
        val decodedTextAttachment = if (attachedBytes != null && !isBinaryAttachment) {
            attachedBytes.toString(Charsets.UTF_8)
        } else null

        val combinedText = buildString {
            if (sourceText.isNotBlank()) append(sourceText.trim())
            if (!decodedTextAttachment.isNullOrBlank()) {
                if (isNotEmpty()) append("\n\n")
                append("--- CONTENIDO DEL ARCHIVO (${attachedFilename ?: "documento"}) ---\n")
                append(decodedTextAttachment)
            }
        }

        val depthNote = if (thinkingDepth == ThinkingDepth.DEEP) {
            "\nPROFUNDIDAD MÁXIMA: Aplica análisis estructural profundo, subtexto detallado, especificaciones ópticas exactas y foley exhaustivo."
        } else ""

        val audioNote = if (isAudio) {
            "\nMATERIAL DE AUDIO DETECTADO (${attachedFilename ?: "audio"}): Escucha directamente el archivo adjunto, transcribe íntegramente en 'transcription' y completa todo el bloque 'audioAnalysis'."
        } else ""

        val prompt = """PRODUCCIÓN ACTIVA: ${project.title}
SECUENCIA / CAPÍTULO: $sequenceTitle
TIPO DE MATERIAL FUENTE: ${StudioLogic.getSourceTypeLabel(sourceType)}
ESTILO CINEMATOGRÁFICO OBJETIVO: $directorStyle
RELACIÓN DE ASPECTO OBJETIVO: $aspectRatio$depthNote

MEMORIA DE AUTOAPRENDIZAJE Y MEJORA DEL ESTUDIO:
$learningContext

BIBLIA DE CONTINUIDAD DEL PROYECTO:
${continuityBible.ifBlank { project.continuityBible }.ifBlank { "Establecer el estándar visual y sonoro desde cero manteniendo coherencia de Hollywood." }}
$audioNote

MATERIAL FUENTE ESCRITO / NOTAS DEL CLIENTE:
${combinedText.ifBlank { "[El material fuente principal está adjunto como archivo binario PDF o Audio. Procésalo íntegramente.]" }}

Genera el expediente completo de dirección en JSON estructurado siguiendo el esquema solicitado. Incluye al menos 4 planos en shotList, al menos 1 escena detallada con múltiples beats (action, camera, sound, dialogue) en screenplay, y 3 tarjetas de storyboardFrames."""

        val response = withTimeout(60_000L) {
            productionModel.generateContent(
                content {
                    if (isBinaryAttachment) {
                        inlineData(attachedBytes!!, attachedMimeType!!)
                    }
                    text(prompt)
                }
            )
        }

        val jsonText = response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("El motor cinematográfico devolvió una respuesta vacía.")

        return parseProducedSequence(
            json = JSONObject(jsonText),
            userId = userId,
            projectId = project.id,
            sequenceTitle = sequenceTitle,
            sourceType = sourceType,
            sourceFilename = attachedFilename,
            directorStyle = directorStyle,
            aspectRatio = aspectRatio
        )
    }

    suspend fun chatWithAgent(
        project: ProductionProject,
        activeSequence: ProductionSequence?,
        focus: AgentRoleFocus,
        userMessage: String,
        learningContext: String,
        recentHistory: List<AgentMessage>
    ): AgentChatOutcome {
        val turnContext = AgentLogic.buildAgentTurnContext(
            project = project,
            activeSequence = activeSequence,
            focus = focus,
            learningContext = learningContext
        )

        val historyContents = recentHistory.takeLast(10).map { msg ->
            content(role = if (msg.role == "user") "user" else "model") {
                text(msg.content)
            }
        }

        val chat = agentModel.startChat(history = historyContents)
        var workingProject = project
        var workingDraftSeq: ProductionSequence? = null
        var bibleChanged = false
        val executedActions = mutableListOf<AgentToolAction>()

        var response = withTimeout(60_000L) {
            chat.sendMessage("$turnContext\n\nINSTRUCCIÓN DEL USUARIO:\n$userMessage")
        }

        var round = 0
        while (response.functionCalls.isNotEmpty() && round < 3) {
            round += 1
            val calls = response.functionCalls
            val responseParts = mutableListOf<FunctionResponsePart>()

            for (fnCall in calls) {
                val argsMap = fnCall.args.mapValues { (_, element) -> jsonElementToKotlin(element) }
                val outcome = AgentLogic.applyAgentToolCall(
                    project = workingProject,
                    activeSequenceId = activeSequence?.id,
                    toolName = fnCall.name,
                    args = argsMap
                )
                workingProject = outcome.updatedProject
                if (outcome.updatedSequence != null) {
                    workingDraftSeq = outcome.updatedSequence
                }
                if (outcome.bibleChanged) {
                    bibleChanged = true
                }
                executedActions.add(outcome.action)

                responseParts.add(
                    FunctionResponsePart(
                        name = fnCall.name,
                        response = JsonObject(mapOf("result" to JsonPrimitive(outcome.resultMessage)))
                    )
                )
            }

            response = withTimeout(60_000L) {
                chat.sendMessage(
                    content(role = "function") {
                        responseParts.forEach { part(it) }
                    }
                )
            }
        }

        val replyText = response.text?.trim()?.takeIf { it.isNotEmpty() }
            ?: if (executedActions.isNotEmpty()) {
                "He aplicado los ajustes solicitados en el expediente de tu producción."
            } else {
                "No se recibió respuesta del Agente."
            }

        return AgentChatOutcome(
            replyText = replyText,
            actions = executedActions,
            updatedProject = workingProject,
            updatedDraftSequence = workingDraftSeq,
            bibleChanged = bibleChanged
        )
    }

    suspend fun generateStoryboardImage(
        context: Context,
        frame: StoryboardCard,
        sequenceAspectRatio: String
    ): String {
        val ratioEnum = when ((frame.aspectRatio.ifBlank { sequenceAspectRatio }).trim()) {
            "9:16" -> AspectRatio.PORTRAIT_9x16
            "3:4" -> AspectRatio.PORTRAIT_3x4
            "1:1" -> AspectRatio.SQUARE_1x1
            "4:3", "1.43:1" -> AspectRatio.LANDSCAPE_4x3
            else -> AspectRatio.LANDSCAPE_16x9
        }

        val imageModel = Firebase.ai.generativeModel(
            modelName = MODEL_IMAGE,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(ratioEnum)
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
        )

        val promptText = "${frame.visualPrompt}\n\nPhotorealistic 65mm film still, cinematic color grading, no text overlays or watermarks."
        val response = withTimeout(60_000L) {
            imageModel.generateContent(promptText)
        }
        val bitmap = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()?.image
            ?: throw IllegalStateException("Nano no devolvió ninguna imagen para el fotograma.")

        return withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "storyboard")
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, "${frame.id.ifBlank { UUID.randomUUID().toString() }}.png")
            FileOutputStream(outFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)
            }
            outFile.absolutePath
        }
    }

    private fun jsonElementToKotlin(element: JsonElement): Any? = when (element) {
        is JsonNull -> null
        is JsonPrimitive -> {
            if (element.isString) element.content
            else element.booleanOrNull ?: element.longOrNull ?: element.doubleOrNull ?: element.content
        }
        is JsonArray -> element.map { jsonElementToKotlin(it) }
        is JsonObject -> element.mapValues { (_, v) -> jsonElementToKotlin(v) }
    }

    private fun isAudioMimeOrName(mime: String?, filename: String?): Boolean {
        if (mime != null && mime.startsWith("audio/")) return true
        val lower = filename?.lowercase().orEmpty()
        return listOf(".mp3", ".wav", ".m4a", ".ogg", ".aac", ".flac", ".webm").any { lower.endsWith(it) }
    }

    private fun parseAudioAnalysis(obj: JSONObject): AudioAnalysisReport {
        val effectsArr = obj.optJSONArray("detectedSoundEffects") ?: JSONArray()
        val effects = (0 until effectsArr.length()).map { effectsArr.optString(it) }.filter { it.isNotBlank() }
        return AudioAnalysisReport(
            transcription = obj.optString("transcription"),
            narrationStyle = obj.optString("narrationStyle"),
            emotionalArc = obj.optString("emotionalArc"),
            rhythmAndPacing = obj.optString("rhythmAndPacing"),
            detectedSoundEffects = effects,
            directingRecommendations = obj.optString("directingRecommendations")
        )
    }

    private fun parseProducedSequence(
        json: JSONObject,
        userId: String,
        projectId: String,
        sequenceTitle: String,
        sourceType: SourceMaterialType,
        sourceFilename: String?,
        directorStyle: String,
        aspectRatio: String
    ): ProductionSequence {
        val paletteArr = json.optJSONArray("colorPalette") ?: JSONArray()
        val palette = (0 until paletteArr.length()).mapNotNull { i ->
            val o = paletteArr.optJSONObject(i) ?: return@mapNotNull null
            ColorSwatch(
                name = o.optString("name", "Tono ${i + 1}"),
                hex = o.optString("hex", "#E5A93C"),
                role = o.optString("role", "")
            )
        }

        val lightingObj = json.optJSONObject("lighting")
        val lighting = LightingSetup(
            keyLight = lightingObj?.optString("keyLight").orEmpty(),
            fillRatio = lightingObj?.optString("fillRatio").orEmpty(),
            rimAndBacklight = lightingObj?.optString("rimAndBacklight").orEmpty(),
            colorTemperatureKelvin = lightingObj?.optString("colorTemperatureKelvin").orEmpty(),
            atmosphereAndHaze = lightingObj?.optString("atmosphereAndHaze").orEmpty()
        )

        val shotsArr = json.optJSONArray("shotList") ?: JSONArray()
        val shots = (0 until shotsArr.length()).mapNotNull { i ->
            val o = shotsArr.optJSONObject(i) ?: return@mapNotNull null
            ShotSpec(
                shotNumber = o.optString("shotNumber", "${i + 1}"),
                shotType = o.optString("shotType", "PG"),
                lensMm = o.optString("lensMm", "35mm"),
                movement = o.optString("movement", "Fijo"),
                durationSec = o.optDouble("durationSec", 4.0).takeIf { it.isFinite() } ?: 4.0,
                framingNote = o.optString("framingNote"),
                psychologicalIntent = o.optString("psychologicalIntent")
            )
        }

        val soundObj = json.optJSONObject("soundDesign")
        val atmosArr = soundObj?.optJSONArray("atmosLayers") ?: JSONArray()
        val foleyArr = soundObj?.optJSONArray("foleyDetails") ?: JSONArray()
        val soundDesign = SoundDesignSpec(
            atmosLayers = (0 until atmosArr.length()).map { atmosArr.optString(it) }.filter { it.isNotBlank() },
            subBassCue = soundObj?.optString("subBassCue").orEmpty(),
            foleyDetails = (0 until foleyArr.length()).map { foleyArr.optString(it) }.filter { it.isNotBlank() },
            silenceBeats = soundObj?.optString("silenceBeats").orEmpty(),
            musicalScore = soundObj?.optString("musicalScore").orEmpty(),
            tempoBpm = soundObj?.optInt("tempoBpm", 72) ?: 72
        )

        val vfxObj = json.optJSONObject("vfx")
        val cgiArr = vfxObj?.optJSONArray("cgiElements") ?: JSONArray()
        val sfxArr = vfxObj?.optJSONArray("practicalSfx") ?: JSONArray()
        val vfx = VfxBreakdown(
            cgiElements = (0 until cgiArr.length()).map { cgiArr.optString(it) }.filter { it.isNotBlank() },
            practicalSfx = (0 until sfxArr.length()).map { sfxArr.optString(it) }.filter { it.isNotBlank() },
            locationsAndSets = vfxObj?.optString("locationsAndSets").orEmpty(),
            wardrobeAndMakeup = vfxObj?.optString("wardrobeAndMakeup").orEmpty()
        )

        val scenesArr = json.optJSONArray("screenplay") ?: JSONArray()
        val screenplay = (0 until scenesArr.length()).mapNotNull { i ->
            val o = scenesArr.optJSONObject(i) ?: return@mapNotNull null
            val beatsArr = o.optJSONArray("beats") ?: JSONArray()
            val beats = (0 until beatsArr.length()).mapNotNull { bIdx ->
                val b = beatsArr.optJSONObject(bIdx) ?: return@mapNotNull null
                val rawType = b.optString("type", "action").lowercase()
                val type = if (rawType in listOf("dialogue", "camera", "sound")) rawType else "action"
                ScreenplayBeat(
                    type = type,
                    character = b.optString("character").takeIf { it.isNotBlank() && it != "null" },
                    parenthetical = b.optString("parenthetical").takeIf { it.isNotBlank() && it != "null" },
                    text = b.optString("text")
                )
            }
            ScreenplayScene(
                sceneNumber = o.optString("sceneNumber", "${i + 1}"),
                slugline = o.optString("slugline", "INT. ESCENA - DÍA"),
                timeOfDay = o.optString("timeOfDay", "NOCHE"),
                beats = beats
            )
        }

        val framesArr = json.optJSONArray("storyboardFrames") ?: JSONArray()
        val storyboard = (0 until framesArr.length()).mapNotNull { i ->
            val o = framesArr.optJSONObject(i) ?: return@mapNotNull null
            StoryboardCard(
                id = "sb_${UUID.randomUUID().toString().replace("-", "").take(8)}_$i",
                title = o.optString("title", "Fotograma ${i + 1}"),
                shotRef = o.optString("shotRef", "Plano 0${i + 1}"),
                aspectRatio = aspectRatio,
                cameraSpec = o.optString("cameraSpec", json.optString("cameraSystem", "65mm IMAX")),
                visualPrompt = o.optString("visualPrompt"),
                veoMotionPrompt = o.optString("veoMotionPrompt")
            )
        }

        val audioObj = json.optJSONObject("audioAnalysis")
        val audioAnalysis = audioObj?.let(::parseAudioAnalysis)

        val seqId = "seq_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        return ProductionSequence(
            id = seqId,
            projectId = projectId,
            ownerId = userId,
            title = sequenceTitle.trim(),
            versionStatus = SequenceVersionStatus.DRAFT,
            sourceType = sourceType,
            sourceFilename = sourceFilename,
            directorStyle = directorStyle,
            aspectRatio = aspectRatio,
            cameraSystem = json.optString("cameraSystem", "ARRI Alexa 65"),
            lensPackage = json.optString("lensPackage", "Cooke Anamorphic /i"),
            filmStockOrLut = json.optString("filmStockOrLut", "Kodak 2383 Print LUT"),
            logline = json.optString("logline"),
            directorVision = json.optString("directorVision"),
            colorPalette = palette,
            lighting = lighting,
            shotList = shots,
            soundDesign = soundDesign,
            audioAnalysis = audioAnalysis,
            vfx = vfx,
            screenplay = screenplay,
            storyboardFrames = storyboard,
            continuitySummary = json.optString("continuitySummary")
        )
    }
}
