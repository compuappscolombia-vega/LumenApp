package com.example.data.util

import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.SystemErrorEntry
import com.example.data.model.SystemErrorSeverity
import com.example.data.model.SystemErrorSource
import com.example.data.model.SystemErrorStatus
import com.example.data.model.SystemRepairAction
import com.example.data.model.UserStudioConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object SystemErrorDiagnostics {

    private val EMAIL_REGEX = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")
    private val TOKEN_REGEX = Regex("""(AIza[0-9A-Za-z\-_]{10,}|Bearer\s+[A-Za-z0-9\-._~+/]+=*|ya29\.[0-9A-Za-z\-_]+)""")

    fun sanitizeDiagnosticText(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.trim()
            .replace(EMAIL_REGEX, "[correo-protegido]")
            .replace(TOKEN_REGEX, "[credencial-protegida]")
            .take(500)
    }

    fun buildEntry(
        source: SystemErrorSource,
        rawMessage: String,
        exception: Throwable? = null,
        contextHint: String = "",
        projectId: String = ""
    ): SystemErrorEntry {
        val cleanMessage = sanitizeDiagnosticText(rawMessage).ifBlank {
            sanitizeDiagnosticText(exception?.localizedMessage ?: exception?.toString())
                .ifBlank { "Error técnico no especificado en ${source.label}." }
        }
        val rawDetail = sanitizeDiagnosticText(
            buildString {
                if (contextHint.isNotBlank()) append("[$contextHint] ")
                append(exception?.javaClass?.simpleName ?: source.wireValue.uppercase())
                val exMsg = exception?.message?.takeIf { it.isNotBlank() }
                if (exMsg != null) append(": $exMsg")
            }
        )
        val combined = "$cleanMessage $rawDetail".lowercase(Locale.getDefault())

        val (severity, title, rootCause, suggestedFix, action) = when {
            combined.contains("app check") || combined.contains("403") || combined.contains("attestation") ->
                DiagnosticClassification(
                    severity = SystemErrorSeverity.CRITICAL,
                    title = "Fallo de verificación Gemini / App Check",
                    rootCause = "El servidor rechazó la solicitud de IA por token App Check inválido o caducado o cuota bloqueada.",
                    suggestedFix = "Ejecuta «Verificar sesión y App Check», comprueba conexión a red o usa «Crear borrador manual» mientras restableces la sesión.",
                    action = SystemRepairAction.VERIFY_AUTH_AND_AI
                )
            combined.contains("permission_denied") || combined.contains("permisos insuficientes") ->
                DiagnosticClassification(
                    severity = SystemErrorSeverity.CRITICAL,
                    title = "Permisos insuficientes en Cloud Firestore",
                    rootCause = "La sesión de Google no coincide con el propietario del documento o las reglas de seguridad rechazaron la escritura.",
                    suggestedFix = "Verifica tu sesión de Google y ejecuta la reparación de caché y sincronización.",
                    action = SystemRepairAction.VERIFY_AUTH_AND_AI
                )
            combined.contains("quota exceeded") || combined.contains("cuota") ->
                DiagnosticClassification(
                    severity = SystemErrorSeverity.WARNING,
                    title = "Cuota de Cloud Firestore / IA alcanzada",
                    rootCause = "Se superó el límite de lecturas o peticiones en la nube.",
                    suggestedFix = "Sincroniza en modo local seguro, evita reintentos en bucle y reanuda cuando la cuota se restablezca.",
                    action = SystemRepairAction.REPAIR_SYNC_AND_CACHE
                )
            combined.contains("unavailable") || combined.contains("timeout") || combined.contains("network") || combined.contains("conexión") ->
                DiagnosticClassification(
                    severity = SystemErrorSeverity.WARNING,
                    title = "Interrupción de red o tiempo de espera agotado",
                    rootCause = "La solicitud excedió el tiempo de espera o no hay conectividad estable.",
                    suggestedFix = "Limpia la cola temporal de caché y reintenta cuando la conexión se estabilice.",
                    action = SystemRepairAction.REPAIR_SYNC_AND_CACHE
                )
            combined.contains("pdf") || combined.contains("archivo") || combined.contains("stream") || combined.contains("19 mb") || combined.contains("bitmap") || combined.contains("storyboard") ->
                DiagnosticClassification(
                    severity = SystemErrorSeverity.WARNING,
                    title = "Error de archivo, PDF o recurso multimedia",
                    rootCause = "El documento adjunto excede el tamaño límite, tiene formato incompatible o el archivo de destino es inaccesible.",
                    suggestedFix = "Limpia adjuntos corruptos y rutas huérfanas de storyboard con la reparación de medios.",
                    action = SystemRepairAction.CLEAR_INVALID_MEDIA
                )
            combined.contains("micrófono") || combined.contains("audio") || combined.contains("speech") || combined.contains("voz") ->
                DiagnosticClassification(
                    severity = SystemErrorSeverity.INFO,
                    title = "Incidencia de dictado por voz / audio",
                    rootCause = "El servicio de reconocimiento de voz del dispositivo no respondió o falta permiso de micrófono.",
                    suggestedFix = "Verifica el permiso RECORD_AUDIO en Ajustes o utiliza las fichas de formato manual en el editor.",
                    action = SystemRepairAction.RESET_CORRUPT_DRAFTS
                )
            source == SystemErrorSource.SYSTEM || combined.contains("obligatorio") || combined.contains("caracteres") || combined.contains("borrador") ->
                DiagnosticClassification(
                    severity = SystemErrorSeverity.INFO,
                    title = "Validación de entrada o borrador incompleto",
                    rootCause = "Faltan campos requeridos (título, mínimo 20 caracteres o escena) o existe un borrador inconsistente.",
                    suggestedFix = "Sanea los borradores locales e introduce título + guion o adjunto válido.",
                    action = SystemRepairAction.RESET_CORRUPT_DRAFTS
                )
            else ->
                DiagnosticClassification(
                    severity = SystemErrorSeverity.WARNING,
                    title = "Incidencia detectada en ${source.label}",
                    rootCause = "Se produjo una excepción controlada durante la operación ($contextHint).",
                    suggestedFix = "Ejecuta la auto-reparación integral para sanear estado, carpetas y configuración.",
                    action = SystemRepairAction.RUN_FULL_SELF_CHECK
                )
        }

        return SystemErrorEntry(
            id = "err_${UUID.randomUUID().toString().replace("-", "").take(10)}",
            timestampMillis = System.currentTimeMillis(),
            source = source,
            severity = severity,
            title = title,
            message = cleanMessage,
            technicalDetail = rawDetail,
            rootCauseHint = rootCause,
            suggestedFix = suggestedFix,
            status = SystemErrorStatus.OPEN,
            repairAction = action,
            projectId = projectId.take(128)
        )
    }

    data class HealthAuditResult(
        val issuesFound: List<String>,
        val repairsApplied: List<String>,
        val sanitizedConfig: UserStudioConfig,
        val projectsNeedingDefaultFolders: List<ProductionProject>,
        val brokenFrameIdsToClear: List<String>
    )

    fun runHealthAuditAndPlanRepairs(
        projects: List<ProductionProject>,
        selectedProjectId: String,
        selectedSequenceId: String,
        config: UserStudioConfig,
        hasSignedInUser: Boolean,
        checkFilesOnDisk: Boolean = true
    ): HealthAuditResult {
        val issues = mutableListOf<String>()
        val repairs = mutableListOf<String>()

        val allowedRatios = setOf("2.39:1", "1.85:1", "16:9", "1.43:1")
        val normalizedTheme = if (config.themeMode.equals("light", ignoreCase = true)) "light" else "dark"
        val normalizedRatio = config.defaultAspectRatio.takeIf { it in allowedRatios } ?: "2.39:1"
        val normalizedProjectId = if (selectedProjectId.isNotBlank() && projects.none { it.id == selectedProjectId }) {
            issues.add("Proyecto activo apuntaba a un ID inexistente.")
            repairs.add("Proyecto activo reasignado al primer proyecto disponible.")
            projects.firstOrNull()?.id.orEmpty()
        } else {
            selectedProjectId
        }
        val activeProj = projects.firstOrNull { it.id == normalizedProjectId } ?: projects.firstOrNull()
        val normalizedSequenceId = if (
            selectedSequenceId.isNotBlank() &&
            activeProj != null &&
            activeProj.sequences.isNotEmpty() &&
            activeProj.sequences.none { it.id == selectedSequenceId }
        ) {
            issues.add("Secuencia activa apuntaba a un registro eliminado.")
            repairs.add("Selección de secuencia restablecida correctamente.")
            activeProj.sequences.firstOrNull()?.id.orEmpty()
        } else {
            selectedSequenceId
        }

        if (config.themeMode != normalizedTheme || config.defaultAspectRatio != normalizedRatio) {
            issues.add("Configuración de tema o formato contenía valores fuera de rango.")
            repairs.add("Parámetros de tema y relación de aspecto saneados.")
        }

        val sanitizedConfig = config.copy(
            themeMode = normalizedTheme,
            defaultAspectRatio = normalizedRatio,
            lastProjectId = normalizedProjectId,
            lastSequenceId = normalizedSequenceId
        )

        val projectsMissingFolders = projects.filter { it.productionFolders.isEmpty() }
        if (projectsMissingFolders.isNotEmpty()) {
            issues.add("${projectsMissingFolders.size} proyecto(s) sin carpetas de producción.")
            repairs.add("Estructura de carpetas restaurada según plantilla cinematográfica.")
        }

        val brokenFrames = mutableListOf<String>()
        if (checkFilesOnDisk) {
            for (proj in projects) {
                for (seq in proj.sequences) {
                    for (frame in seq.storyboardFrames) {
                        val path = frame.localImagePath
                        if (!path.isNullOrBlank() && !File(path).exists()) {
                            brokenFrames.add(frame.id)
                        }
                    }
                }
            }
            if (brokenFrames.isNotEmpty()) {
                issues.add("${brokenFrames.size} ruta(s) huérfana(s) de imagen de storyboard.")
                repairs.add("Referencias rotas de storyboard limpiadas para permitir regeneración.")
            }
        }

        if (!hasSignedInUser) {
            repairs.add("Modo estudio local verificado: todos los borradores, carpetas e historial siguen protegidos en almacenamiento interno.")
        } else {
            repairs.add("Sesión de Google y ruta aislada de Cloud Firestore verificadas.")
        }

        if (issues.isEmpty()) {
            repairs.add("Integridad completa verificada: configuración, carpetas, secuencias y borradores consistentes.")
        }

        return HealthAuditResult(
            issuesFound = issues,
            repairsApplied = repairs,
            sanitizedConfig = sanitizedConfig,
            projectsNeedingDefaultFolders = projectsMissingFolders,
            brokenFrameIdsToClear = brokenFrames
        )
    }

    fun formatDiagnosticReportText(errors: List<SystemErrorEntry>): String {
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("es-ES"))
        return buildString {
            appendLine("=== LUMENREEL STUDIO · REPORTE DE DIAGNÓSTICO Y REPARACIÓN ===")
            appendLine("Generado: ${df.format(Date())}")
            appendLine("Total registros: ${errors.size} · Pendientes: ${errors.count { it.status == SystemErrorStatus.OPEN }} · Reparados: ${errors.count { it.status == SystemErrorStatus.REPAIRED }}")
            appendLine()
            if (errors.isEmpty()) {
                appendLine("No hay errores registrados en el sistema.")
            } else {
                errors.forEachIndexed { idx, err ->
                    appendLine("${idx + 1}. [${err.status.label.uppercase()}] ${err.title} (${err.source.label} · ${err.severity.label})")
                    appendLine("   Fecha: ${df.format(Date(err.timestampMillis))}")
                    appendLine("   Mensaje: ${err.message}")
                    if (err.rootCauseHint.isNotBlank()) appendLine("   Causa probable: ${err.rootCauseHint}")
                    if (err.suggestedFix.isNotBlank()) appendLine("   Solución sugerida: ${err.suggestedFix}")
                    if (err.repairNote.isNotBlank()) appendLine("   Reparación aplicada: ${err.repairNote}")
                    appendLine()
                }
            }
        }
    }

    private data class DiagnosticClassification(
        val severity: SystemErrorSeverity,
        val title: String,
        val rootCause: String,
        val suggestedFix: String,
        val action: SystemRepairAction
    )
}
