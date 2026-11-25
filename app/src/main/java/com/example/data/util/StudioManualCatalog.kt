package com.example.data.util

enum class ManualQuickAction {
    OPEN_SCRIPT_EDITOR,
    OPEN_FOLDER_MANAGER,
    OPEN_CALENDAR,
    OPEN_THEME_SETTINGS,
    OPEN_ERROR_CENTER,
    NONE
}

data class ManualSection(
    val id: String,
    val category: String, // "guion" | "carpetas" | "direccion" | "sistema" | "errores" | "pdf"
    val badge: String,
    val title: String,
    val summary: String,
    val steps: List<String>,
    val proTip: String,
    val actionLabel: String = "",
    val quickAction: ManualQuickAction = ManualQuickAction.NONE
)

data class AspectRatioGuideSpec(
    val ratio: String,
    val numericRatio: Float,
    val title: String,
    val usageNote: String
)

object StudioManualCatalog {

    val TOPIC_FILTERS: List<Pair<String, String>> = listOf(
        "all" to "Todo",
        "guion" to "Guion e IA",
        "carpetas" to "Carpetas",
        "direccion" to "Aspecto y 3D",
        "sistema" to "Calendario y Tema",
        "errores" to "Errores y Reparación",
        "pdf" to "Exportación PDF"
    )

    val ASPECT_GUIDE_SPECS: List<AspectRatioGuideSpec> = listOf(
        AspectRatioGuideSpec(
            ratio = "2.39:1",
            numericRatio = 2.39f,
            title = "Anamórfico Scope (Cine Épico)",
            usageNote = "Ideal para paisajes amplios, acción cinematográfica y thrillers neo-noir."
        ),
        AspectRatioGuideSpec(
            ratio = "1.85:1",
            numericRatio = 1.85f,
            title = "Flat Cinematográfico",
            usageNote = "Estándar versátil para drama íntimo, documental y planos secuencia."
        ),
        AspectRatioGuideSpec(
            ratio = "16:9",
            numericRatio = 1.777f,
            title = "Widescreen Digital",
            usageNote = "Formato nativo de series, pantallas modernas y pilotos televisivos."
        ),
        AspectRatioGuideSpec(
            ratio = "1.43:1",
            numericRatio = 1.43f,
            title = "IMAX 65mm Vertical",
            usageNote = "Máxima altura arquitectónica y sensación monumental en pantalla grande."
        )
    )

    val READINESS_CHECKLIST_STEPS: List<String> = listOf(
        "Crear un proyecto desde plantilla cinematográfica (Corto, Documental, Largo…)",
        "Organizar carpetas para Guiones y Storyboards en el Gestor de Carpetas",
        "Redactar o dictar el guion con fichas INT./EXT. y analizarlo con Gemini",
        "Revisar Categorías Técnicas: Cámara, Luz Kelvin, Planos 3D, Atmos y VFX",
        "Programar hitos y fechas límite en el Calendario de Producción",
        "Exportar el Guion y Plan de Rodaje o el Manual PDF al dispositivo"
    )

    val SECTIONS: List<ManualSection> = listOf(
        ManualSection(
            id = "agent_lumen",
            category = "guion",
            badge = "CONTROL DEL AGENTE · LUMEN",
            title = "Comando y Control del Agente Lumen (Agente Tripartito)",
            summary = "Domina el Agente Tripartito Lumen (Director, Guionista Hollywood y Experto VFX/SFX) para modificar expedientes, aplicar herramientas y autoaprender reglas de rodaje.",
            steps = listOf(
                "Paso 1: Selecciona la pestaña «Agente» en cualquier proyecto activo para iniciar sesión con el Agente Lumen.",
                "Paso 2: Elige el rol de especialización (Consejo Tripartito, Director de Cine, Guionista o Experto VFX) según el objetivo de tu consulta.",
                "Paso 3: Escribe tu solicitud en lenguaje natural (ej. «Ajusta la óptica a lentes anamórficos Cooke y añade una capa de lluvia al diseño sonoro»).",
                "Paso 4: El Agente Lumen evaluará tu solicitud y ejecutará de forma autónoma las 5 herramientas de modificación de expediente: actualizar biblia de continuidad, ajustar cámara/Kelvin, diseñar paquete VFX/SFX, refinar guion o añadir storyboards.",
                "Paso 5: Consulta el menú «Autoaprendizaje» para guardar reglas técnicas aprobadas que el Agente inyectará automáticamente en futuras producciones."
            ),
            proTip = "Puedes alternar entre los 3 roles en cualquier momento; el Agente recuerda el contexto de tu Biblia de Continuidad y secuencias.",
            actionLabel = "Abrir Panel del Agente",
            quickAction = ManualQuickAction.NONE
        ),
        ManualSection(
            id = "script_ai",
            category = "guion",
            badge = "PASO 1 · GUION & GEMINI",
            title = "Editor de Guion y Análisis Técnico con Gemini",
            summary = "Redacta escenas directamente en el editor cinematográfico, dicta por voz o adjunta PDF/TXT/Audio para que Gemini desglose todas las categorías técnicas.",
            steps = listOf(
                "Abre «Editor de Guion IA» desde el Panel de Producciones o el botón superior del estudio.",
                "Usa las fichas rápidas INT., EXT., DIÁLOGO, [CÁMARA] y [SONIDO] para estructurar tu texto sin esfuerzo.",
                "Toca «Dictar» si prefieres narrar escenas por micrófono o «Adjuntar» para subir archivos PDF, Fountain o MP3/WAV.",
                "Pulsa «Analizar guion con Gemini» para generar Cámara/Óptica, Iluminación Kelvin, Lista de Planos, Dolby Atmos, VFX, Guion y Storyboard.",
                "¿Sin conexión o prefieres escribir a mano? Toca «Crear borrador manual» para guardar al instante sin esperar a la nube."
            ),
            proTip = "El contador en vivo calcula escenas INT./EXT., palabras y duración estimada antes de enviar tu guion a Gemini.",
            actionLabel = "Abrir Editor de Guion IA",
            quickAction = ManualQuickAction.OPEN_SCRIPT_EDITOR
        ),
        ManualSection(
            id = "project_folders",
            category = "carpetas",
            badge = "PASO 2 · ORGANIZACIÓN",
            title = "Gestor de Proyectos y Carpetas Cinematográficas",
            summary = "Organiza tus guiones, secuencias y storyboards en carpetas temáticas por proyecto y plantilla cinematográfica.",
            steps = listOf(
                "Al crear un proyecto, elige una plantilla (Cortometraje, Documental, Largometraje, Serie o Comercial) para generar carpetas iniciales.",
                "En el Panel de Producciones, pulsa «Carpetas» en cualquier tarjeta o «Carpetas del Proyecto» en la barra rápida.",
                "Crea nuevas carpetas indicando área (Guion/Scripts, Storyboard/Arte, Biblia, Rodaje, Sonido o VFX).",
                "Vincula secuencias específicas a cada carpeta y salta directo a su pestaña de Guion o Storyboard con un toque."
            ),
            proTip = "Puedes guardar notas de entrega y pendientes técnicos dentro de cada carpeta; se sincronizan con Cloud Firestore.",
            actionLabel = "Abrir Gestor de Carpetas",
            quickAction = ManualQuickAction.OPEN_FOLDER_MANAGER
        ),
        ManualSection(
            id = "direction_3d",
            category = "direccion",
            badge = "PASO 3 · DIRECCIÓN Y 3D",
            title = "Dirección, Óptica, Radar y Escenario 3D",
            summary = "Ajusta cámara, lentes, temperatura de color Kelvin y explora la puesta en escena en vista isométrica 3D.",
            steps = listOf(
                "En la pestaña «Dirección», inspecciona el Gráfico Radar (Guion, Luz, VFX y Sonido) y edita la Biblia de Continuidad.",
                "En «Planos y Sonido», alterna entre el Escenario 3D interactivo y la Lista de Planos con arquitectura Dolby Atmos.",
                "En «Storyboard», genera fotogramas conceptuales con Nano y visualiza el encuadre exacto según la relación de aspecto."
            ),
            proTip = "Califica tus secuencias (1–5 ★) para entrenar a Studio Intelligence y reutilizar tus mejores configuraciones de cámara y luz.",
            actionLabel = "Abrir Dirección y 3D",
            quickAction = ManualQuickAction.NONE
        ),
        ManualSection(
            id = "calendar_theme",
            category = "sistema",
            badge = "PASO 4 · AGENDA Y TEMA",
            title = "Calendario de Rodaje, Tema Claro/Oscuro y Persistencia",
            summary = "Planifica fechas límite, días de rodaje y personaliza la apariencia visual con sincronización automática.",
            steps = listOf(
                "Toca el ícono de Calendario para mapear días de rodaje, entregas, revisiones técnicas y estrenos sobre el calendario mensual.",
                "Pulsa cualquier fecha para ver las escenas programadas o agregar un nuevo hito vinculado a una secuencia.",
                "Usa el botón de brillo en la barra superior o en «Perfil y Configuración» para alternar entre Tema Oscuro (Obsidiana) y Tema Claro (Papel Guion).",
                "Todas tus preferencias, borradores de guion e ingesta se auto-guardan localmente y se sincronizan con tu cuenta de Google."
            ),
            proTip = "El tema Claro está calibrado con alto contraste para lectura en exteriores o rodaje diurno bajo sol.",
            actionLabel = "Abrir Calendario",
            quickAction = ManualQuickAction.OPEN_CALENDAR
        ),
        ManualSection(
            id = "error_repair",
            category = "errores",
            badge = "PASO 5 · DIAGNÓSTICO",
            title = "Registro de Errores del Sistema y Auto-Reparación",
            summary = "Cada fallo de IA, red, Firestore, archivos o validación queda registrado con su causa probable y reparación guiada en 1 toque.",
            steps = listOf(
                "Abre «Errores y Reparación» desde la barra del Panel de Producciones o el menú superior del estudio.",
                "Revisa el estado (Pendiente / Reparado), el módulo afectado, la causa probable y la solución recomendada.",
                "Pulsa «Auto-reparar ahora» en un error o «Ejecutar auto-reparación integral» para sanear carpetas faltantes, rutas rotas de storyboard y caché.",
                "Exporta o copia el reporte de diagnóstico si necesitas documentar una incidencia técnica sin exponer correos ni credenciales."
            ),
            proTip = "El registro de errores funciona incluso sin conexión a internet para diagnosticar fallos de red, App Check o cuota.",
            actionLabel = "Abrir Centro de Errores",
            quickAction = ManualQuickAction.OPEN_ERROR_CENTER
        ),
        ManualSection(
            id = "pdf_exports",
            category = "pdf",
            badge = "PASO 6 · ENTREGABLES",
            title = "Exportación PDF, Fountain (.fountain) y Markdown (.md)",
            summary = "Genera documentos listos para set y archivo del equipo directamente en el almacenamiento del dispositivo.",
            steps = listOf(
                "En Guion o Planos, usa «Guardar Guion y Plan (PDF)» en cualquier momento — incluso en estado Borrador.",
                "Aprueba una secuencia como «Versión Final» para sellarla contra ediciones accidentales y desbloquear la «Bitácora Oficial PDF» con storyboards.",
                "Exporta el guion estándar en formato .FOUNTAIN o .MD para abrirlo en software de guion profesional.",
                "Guarda o comparte este mismo Manual de Uso en PDF desde el pie del manual interactivo."
            ),
            proTip = "Los nombres de archivo PDF se generan automáticamente con el título del proyecto y secuencia.",
            actionLabel = "Exportar Manual PDF",
            quickAction = ManualQuickAction.NONE
        )
    )

    fun filterSections(query: String, category: String): List<ManualSection> {
        val q = query.trim().lowercase()
        return SECTIONS.filter { section ->
            val matchesCategory = category == "all" || section.category == category
            val matchesQuery = q.isEmpty() ||
                section.title.lowercase().contains(q) ||
                section.summary.lowercase().contains(q) ||
                section.proTip.lowercase().contains(q) ||
                section.steps.any { it.lowercase().contains(q) }
            matchesCategory && matchesQuery
        }
    }
}
