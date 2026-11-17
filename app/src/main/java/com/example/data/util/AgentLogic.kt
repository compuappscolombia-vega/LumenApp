package com.example.data.util

import com.example.data.model.AgentRoleFocus
import com.example.data.model.AgentToolAction
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.ScreenplayBeat
import com.example.data.model.ScreenplayScene
import com.example.data.model.SequenceVersionStatus
import com.example.data.model.StoryboardCard

data class AgentRoleProfile(
    val id: AgentRoleFocus,
    val label: String,
    val badge: String,
    val description: String
)

data class AgentQuickCommand(
    val id: String,
    val focus: AgentRoleFocus,
    val title: String,
    val prompt: String
)

data class AgentToolOutcome(
    val updatedProject: ProductionProject,
    val updatedSequence: ProductionSequence?,
    val bibleChanged: Boolean,
    val action: AgentToolAction,
    val resultMessage: String
)

object AgentLogic {
    const val TRIPARTITE_AGENT_INSTRUCTION = """Eres LumenReel, un Agente Cinematográfico Autónomo de élite que integra tres disciplinas maestras en una sola mente creativa:

1. DIRECTOR DE CINE GALARDONADO: Dominas la puesta en escena, el lenguaje de cámara, la selección de cuerpos reales (ARRI Alexa 65, Panavision DXL2, Sony VENICE 2), ópticas anamórficas/esféricas, iluminación por temperaturas Kelvin, colorimetría y montaje rítmico.
2. GUIONISTA PROFESIONAL DE HOLLYWOOD: Escribes y reescribes escenas con estructura dramática sólida, subtexto emocional, economía visual ("Show, don't tell"), sluglines estándar (INT./EXT.), acotaciones precisas y diálogos cinematográficos memorables.
3. EXPERTO EN EFECTOS ESPECIALES (VFX Y SFX PRÁCTICOS): Diseñas efectos visuales CGI (simulaciones de fluidos/fuego/destrucción, criaturas, extensiones de set, matte painting, tracking/rotoscopia) combinados con efectos prácticos reales en set (rigs mecánicos, lluvia artificial, atmósfera de humo/hazer, pirotecnia controlada, maquillaje protésico y seguridad en rodaje).

COMPORTAMIENTO COMO AGENTE:
- Responde siempre en español con criterio técnico de alto nivel, claridad ejecutable y tono de liderazgo cinematográfico.
- Cuando el usuario te pida modificar, expandir, mejorar o corregir algo del proyecto o secuencia activa (biblia de continuidad, dirección/cámara/iluminación, guion/diálogos, paquete VFX/SFX o tarjetas de storyboard), UTILIZA las herramientas disponibles para aplicar los cambios reales al expediente del proyecto antes de responder.
- Si no hay ninguna secuencia producida todavía, ayuda activamente a estructurar la Biblia de Continuidad, el guion base, la visión de dirección y el diseño VFX/SFX desde cero, usando "actualizar_biblia_continuidad" cuando el usuario defina reglas o premisas clave.
- Integra explícitamente las tres perspectivas (**Dirección**, **Guion** y **VFX/SFX**) cuando estés en modo Consejo Completo, o profundiza en la especialidad solicitada cuando el usuario seleccione un rol específico."""

    val AGENT_ROLE_PROFILES: List<AgentRoleProfile> = listOf(
        AgentRoleProfile(
            id = AgentRoleFocus.COUNCIL,
            label = "Consejo Completo",
            badge = "Director · Guionista · VFX",
            description = "Las tres especialidades colaboran en simultáneo sobre puesta en escena, guion y efectos especiales."
        ),
        AgentRoleProfile(
            id = AgentRoleFocus.DIRECTOR,
            label = "Director de Cine",
            badge = "Puesta en escena y Cámara",
            description = "Enfoque en narrativa visual, lentes, movimientos de cámara, iluminación Kelvin y ritmo."
        ),
        AgentRoleProfile(
            id = AgentRoleFocus.SCREENWRITER,
            label = "Guionista",
            badge = "Estructura y Diálogos",
            description = "Enfoque en subtexto, arco dramático, sluglines, ritmo de escena y reescritura de diálogos."
        ),
        AgentRoleProfile(
            id = AgentRoleFocus.VFX,
            label = "Experto VFX / SFX",
            badge = "CGI y Efectos Prácticos",
            description = "Enfoque en simulaciones digitales, composición, rigs mecánicos en set, atmósfera y viabilidad técnica."
        )
    )

    val AGENT_QUICK_COMMANDS: List<AgentQuickCommand> = listOf(
        AgentQuickCommand(
            id = "cmd-council-audit",
            focus = AgentRoleFocus.COUNCIL,
            title = "Auditoría integral (Director · Guionista · VFX)",
            prompt = "Analiza el estado actual del proyecto como Director, Guionista y Experto en Efectos Especiales. Indica mejoras concretas de puesta en escena, subtexto del guion y sinergia entre SFX prácticos y CGI."
        ),
        AgentQuickCommand(
            id = "cmd-director-upgrade",
            focus = AgentRoleFocus.DIRECTOR,
            title = "Elevar dirección, óptica e iluminación",
            prompt = "Como Director de Cine, optimiza el lenguaje visual, la iluminación Kelvin y el movimiento de cámara de la secuencia activa, aplicando los ajustes directamente en el expediente si ya existe una secuencia."
        ),
        AgentQuickCommand(
            id = "cmd-writer-rewrite",
            focus = AgentRoleFocus.SCREENWRITER,
            title = "Reescribir escena con mayor subtexto",
            prompt = "Como Guionista, revisa o propone una escena con mayor tensión dramática, diálogos con subtexto y beats visuales precisos (\"Show, don't tell\"), actualizando el guion o la Biblia del proyecto."
        ),
        AgentQuickCommand(
            id = "cmd-vfx-blueprint",
            focus = AgentRoleFocus.VFX,
            title = "Plan maestro de VFX (CGI + SFX prácticos)",
            prompt = "Como Experto en Efectos Especiales, diseña el desglose técnico de simulaciones CGI, extensiones de set y efectos prácticos en cámara, y guarda el paquete actualizado en el proyecto."
        )
    )

    fun buildAgentTurnContext(
        project: ProductionProject,
        activeSequence: ProductionSequence?,
        focus: AgentRoleFocus,
        learningContext: String
    ): String {
        val profile = AGENT_ROLE_PROFILES.firstOrNull { it.id == focus } ?: AGENT_ROLE_PROFILES.first()
        val seqSummary = if (activeSequence != null) {
            val statusLabel = if (activeSequence.versionStatus == SequenceVersionStatus.FINAL) "Versión Final" else "Borrador"
            val scenesList = activeSequence.screenplay.joinToString(" / ") { "${it.sceneNumber}. ${it.slugline}" }
                .ifBlank { "Sin escenas" }
            """SECUENCIA ACTIVA ($statusLabel):
- ID: ${activeSequence.id}
- Título: ${activeSequence.title}
- Logline: ${activeSequence.logline}
- Visión de dirección: ${activeSequence.directorVision}
- Cámara / Óptica / LUT: ${activeSequence.cameraSystem} | ${activeSequence.lensPackage} | ${activeSequence.filmStockOrLut}
- Iluminación: Key=${activeSequence.lighting.keyLight.ifBlank { "N/D" }} | Kelvin=${activeSequence.lighting.colorTemperatureKelvin.ifBlank { "N/D" }} | Atmósfera=${activeSequence.lighting.atmosphereAndHaze.ifBlank { "N/D" }}
- VFX CGI actuales: ${activeSequence.vfx.cgiElements.joinToString("; ").ifBlank { "Ninguno" }}
- SFX Prácticos actuales: ${activeSequence.vfx.practicalSfx.joinToString("; ").ifBlank { "Ninguno" }}
- Escenas en guion: ${activeSequence.screenplay.size} ($scenesList)
- Storyboard frames: ${activeSequence.storyboardFrames.size}"""
        } else {
            "SECUENCIA ACTIVA: Ninguna secuencia generada todavía en este proyecto (etapa de desarrollo / preproducción)."
        }

        return """ENFOQUE SOLICITADO POR EL USUARIO: ${profile.label} (${profile.badge})
${profile.description}

PROYECTO ACTIVO:
- Título: ${project.title}
- Estilo / Género: ${project.genreStyle}
- Tipo fuente base: ${project.defaultSourceType.wireValue}
- Biblia de continuidad actual:
${project.continuityBible.ifBlank { "[Vacía — lista para ser construida]" }}

$seqSummary

MEMORIA DE AUTOAPRENDIZAJE DEL ESTUDIO:
$learningContext"""
    }

    private fun asStringList(value: Any?): List<String> {
        val list = value as? List<*> ?: return emptyList()
        return list.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
    }

    fun applyAgentToolCall(
        project: ProductionProject,
        activeSequenceId: String?,
        toolName: String,
        args: Map<String, Any?>
    ): AgentToolOutcome {
        val targetIdx = if (!activeSequenceId.isNullOrBlank()) {
            project.sequences.indexOfFirst { it.id == activeSequenceId }
        } else if (project.sequences.isNotEmpty()) 0 else -1

        if (toolName == "actualizar_biblia_continuidad") {
            val newRules = (args["reglasContinuidad"] ?: args["content"] ?: "").toString().trim()
            val mode = (args["modo"] ?: "append").toString().lowercase()
            if (newRules.isEmpty()) {
                return AgentToolOutcome(
                    updatedProject = project,
                    updatedSequence = null,
                    bibleChanged = false,
                    action = AgentToolAction(toolName, "Sin cambios: no se recibieron reglas nuevas.", "biblia"),
                    resultMessage = "No se recibieron reglas válidas para actualizar la Biblia."
                )
            }
            val existing = project.continuityBible.trim()
            val nextBible = when {
                mode == "replace" || existing.isEmpty() -> newRules.take(20000)
                existing.contains(newRules) -> existing.take(20000)
                else -> "$existing\n\n$newRules".take(20000)
            }
            val updatedProject = project.copy(continuityBible = nextBible)
            return AgentToolOutcome(
                updatedProject = updatedProject,
                updatedSequence = null,
                bibleChanged = true,
                action = AgentToolAction(toolName, "Biblia de continuidad actualizada por el Agente.", "biblia"),
                resultMessage = "Biblia de continuidad actualizada correctamente en el proyecto."
            )
        }

        if (targetIdx == -1) {
            return AgentToolOutcome(
                updatedProject = project,
                updatedSequence = null,
                bibleChanged = false,
                action = AgentToolAction(toolName, "No hay secuencia activa aún; usa la Biblia o genera una secuencia primero.", "biblia"),
                resultMessage = "El proyecto todavía no tiene una secuencia producida para editar. Propón cambios en la Biblia de continuidad o pide al usuario dirigir la primera secuencia."
            )
        }

        val seq = project.sequences[targetIdx]
        if (seq.versionStatus == SequenceVersionStatus.FINAL) {
            return AgentToolOutcome(
                updatedProject = project,
                updatedSequence = null,
                bibleChanged = false,
                action = AgentToolAction(toolName, "La secuencia está sellada como Versión Final y no puede modificarse.", "direccion"),
                resultMessage = "La secuencia \"${seq.title}\" ya está aprobada como Versión Final. Solo puedes añadir notas en la Biblia de continuidad."
            )
        }

        when (toolName) {
            "ajustar_direccion_y_camara" -> {
                val updatedSeq = seq.copy(
                    directorVision = args["directorVision"]?.toString()?.trim()?.takeIf(String::isNotEmpty) ?: seq.directorVision,
                    cameraSystem = args["cameraSystem"]?.toString()?.trim()?.takeIf(String::isNotEmpty) ?: seq.cameraSystem,
                    lensPackage = args["lensPackage"]?.toString()?.trim()?.takeIf(String::isNotEmpty) ?: seq.lensPackage,
                    lighting = seq.lighting.copy(
                        keyLight = args["keyLight"]?.toString()?.trim()?.takeIf(String::isNotEmpty) ?: seq.lighting.keyLight,
                        colorTemperatureKelvin = args["colorTemperatureKelvin"]?.toString()?.trim()?.takeIf(String::isNotEmpty)
                            ?: seq.lighting.colorTemperatureKelvin,
                        atmosphereAndHaze = args["atmosphereAndHaze"]?.toString()?.trim()?.takeIf(String::isNotEmpty)
                            ?: seq.lighting.atmosphereAndHaze
                    )
                )
                val nextSeqs = project.sequences.toMutableList().apply { set(targetIdx, updatedSeq) }
                return AgentToolOutcome(
                    updatedProject = project.copy(sequences = nextSeqs),
                    updatedSequence = updatedSeq,
                    bibleChanged = false,
                    action = AgentToolAction(toolName, "Dirección, óptica e iluminación actualizadas en \"${seq.title}\".", "direccion"),
                    resultMessage = "Dirección, cámara (${updatedSeq.cameraSystem}) e iluminación actualizadas en la secuencia \"${seq.title}\"."
                )
            }

            "disenar_paquete_vfx_sfx" -> {
                val cgi = asStringList(args["cgiElements"])
                val sfx = asStringList(args["practicalSfx"])
                val replace = args["modo"]?.toString()?.equals("replace", ignoreCase = true) == true
                val nextCgi = if (replace) cgi else (seq.vfx.cgiElements + cgi).distinct()
                val nextSfx = if (replace) sfx else (seq.vfx.practicalSfx + sfx).distinct()
                val updatedSeq = seq.copy(
                    vfx = seq.vfx.copy(
                        cgiElements = nextCgi.ifEmpty { seq.vfx.cgiElements },
                        practicalSfx = nextSfx.ifEmpty { seq.vfx.practicalSfx },
                        locationsAndSets = args["locationsAndSets"]?.toString()?.trim()?.takeIf(String::isNotEmpty)
                            ?: seq.vfx.locationsAndSets,
                        wardrobeAndMakeup = args["wardrobeAndMakeup"]?.toString()?.trim()?.takeIf(String::isNotEmpty)
                            ?: seq.vfx.wardrobeAndMakeup
                    )
                )
                val nextSeqs = project.sequences.toMutableList().apply { set(targetIdx, updatedSeq) }
                return AgentToolOutcome(
                    updatedProject = project.copy(sequences = nextSeqs),
                    updatedSequence = updatedSeq,
                    bibleChanged = false,
                    action = AgentToolAction(
                        toolName,
                        "Plan VFX/SFX actualizado (${updatedSeq.vfx.cgiElements.size} CGI · ${updatedSeq.vfx.practicalSfx.size} SFX prácticos).",
                        "vfx"
                    ),
                    resultMessage = "Paquete de efectos especiales VFX y SFX prácticos actualizado en \"${seq.title}\"."
                )
            }

            "refinar_guion_escena" -> {
                val sceneNumber = args["sceneNumber"]?.toString()?.trim()?.takeIf(String::isNotEmpty)
                    ?: (seq.screenplay.size + 1).toString()
                val slugline = args["slugline"]?.toString()?.trim()?.takeIf(String::isNotEmpty)
                    ?: "EXT. NUEVA ESCENA - NOCHE"
                val timeOfDay = args["timeOfDay"]?.toString()?.trim()?.takeIf(String::isNotEmpty)
                    ?: "NOCHE"
                val rawBeats = args["beats"] as? List<*> ?: emptyList<Any?>()
                val beats = rawBeats.mapNotNull { item ->
                    val map = item as? Map<*, *> ?: return@mapNotNull null
                    val rawType = map["type"]?.toString()?.lowercase() ?: "action"
                    val beatType = if (rawType in listOf("dialogue", "camera", "sound")) rawType else "action"
                    val text = map["text"]?.toString()?.trim().orEmpty()
                    if (text.isEmpty()) return@mapNotNull null
                    ScreenplayBeat(
                        type = beatType,
                        character = map["character"]?.toString()?.trim()?.takeIf(String::isNotEmpty),
                        parenthetical = map["parenthetical"]?.toString()?.trim()?.takeIf(String::isNotEmpty),
                        text = text
                    )
                }
                if (beats.isEmpty()) {
                    return AgentToolOutcome(
                        updatedProject = project,
                        updatedSequence = null,
                        bibleChanged = false,
                        action = AgentToolAction(toolName, "No se agregaron beats vacíos al guion.", "guion"),
                        resultMessage = "La escena propuesta no contenía beats de guion válidos."
                    )
                }
                val nextScene = ScreenplayScene(sceneNumber, slugline, timeOfDay, beats)
                val existingIdx = seq.screenplay.indexOfFirst { it.sceneNumber == sceneNumber }
                val nextScreenplay = if (existingIdx != -1) {
                    seq.screenplay.toMutableList().apply { set(existingIdx, nextScene) }
                } else {
                    seq.screenplay + nextScene
                }
                val updatedSeq = seq.copy(screenplay = nextScreenplay)
                val nextSeqs = project.sequences.toMutableList().apply { set(targetIdx, updatedSeq) }
                return AgentToolOutcome(
                    updatedProject = project.copy(sequences = nextSeqs),
                    updatedSequence = updatedSeq,
                    bibleChanged = false,
                    action = AgentToolAction(
                        toolName,
                        "Escena $sceneNumber ($slugline) ${if (existingIdx != -1) "reescrita" else "añadida"} en el guion.",
                        "guion"
                    ),
                    resultMessage = "Guion actualizado con la escena $sceneNumber: $slugline (${beats.size} beats)."
                )
            }

            "agregar_plano_storyboard" -> {
                val visualPrompt = args["visualPrompt"]?.toString()?.trim().orEmpty()
                if (visualPrompt.isEmpty()) {
                    return AgentToolOutcome(
                        updatedProject = project,
                        updatedSequence = null,
                        bibleChanged = false,
                        action = AgentToolAction(toolName, "Falta visualPrompt para añadir la tarjeta de storyboard.", "storyboard"),
                        resultMessage = "Se requiere visualPrompt en inglés para crear el fotograma."
                    )
                }
                val newCard = StoryboardCard(
                    id = "sb-${System.currentTimeMillis().toString(36)}-${seq.storyboardFrames.size + 1}",
                    title = args["title"]?.toString()?.trim()?.takeIf(String::isNotEmpty)
                        ?: "Plano Adicional ${seq.storyboardFrames.size + 1}",
                    shotRef = args["shotRef"]?.toString()?.trim()?.takeIf(String::isNotEmpty)
                        ?: "Plano ${seq.storyboardFrames.size + 1}",
                    aspectRatio = seq.aspectRatio,
                    cameraSpec = args["cameraSpec"]?.toString()?.trim()?.takeIf(String::isNotEmpty)
                        ?: seq.cameraSystem,
                    visualPrompt = visualPrompt,
                    veoMotionPrompt = args["veoMotionPrompt"]?.toString()?.trim().orEmpty()
                )
                val updatedSeq = seq.copy(storyboardFrames = seq.storyboardFrames + newCard)
                val nextSeqs = project.sequences.toMutableList().apply { set(targetIdx, updatedSeq) }
                return AgentToolOutcome(
                    updatedProject = project.copy(sequences = nextSeqs),
                    updatedSequence = updatedSeq,
                    bibleChanged = false,
                    action = AgentToolAction(toolName, "Nuevo fotograma añadido al storyboard: \"${newCard.title}\".", "storyboard"),
                    resultMessage = "Tarjeta de storyboard \"${newCard.title}\" añadida al expediente."
                )
            }

            else -> return AgentToolOutcome(
                updatedProject = project,
                updatedSequence = null,
                bibleChanged = false,
                action = AgentToolAction(toolName, "Herramienta desconocida: $toolName", "direccion"),
                resultMessage = "Herramienta $toolName no reconocida."
            )
        }
    }

    fun formatAiError(error: Throwable): String {
        val raw = error.localizedMessage ?: error.toString()
        val norm = raw.lowercase()
        return when {
            norm.contains("403") || norm.contains("app check") || norm.contains("attestation") ->
                "Error de verificación de App Check o permisos (HTTP 403). Asegúrate de que la app esté ejecutándose con el token de App Check configurado."
            norm.contains("429") || norm.contains("quota") || norm.contains("resource_exhausted") ->
                "Límite de cuota de Gemini alcanzado (HTTP 429). Inténtalo nuevamente en unos segundos."
            norm.contains("413") || norm.contains("too large") ->
                "El archivo adjunto excede el límite permitido (máx. 20 MB en línea)."
            else -> raw
        }
    }
}
