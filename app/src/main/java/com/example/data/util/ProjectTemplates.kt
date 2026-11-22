package com.example.data.util

import com.example.data.model.ProductionFolder
import com.example.data.model.SourceMaterialType
import com.example.data.model.WorkspaceTab

data class CinematicProjectTemplate(
    val id: String,
    val name: String,
    val englishSubtitle: String,
    val badge: String,
    val tagline: String,
    val description: String,
    val defaultStyle: String,
    val defaultSourceType: SourceMaterialType,
    val aspectRatio: String,
    val cameraRecommendation: String,
    val continuityStarter: String,
    val folders: List<ProductionFolder>
)

object ProjectTemplateCatalog {

    val ALL_TEMPLATES: List<CinematicProjectTemplate> = listOf(
        CinematicProjectTemplate(
            id = "short_film",
            name = "Cortometraje",
            englishSubtitle = "Short Film",
            badge = "CORTOMETRAJE · 10–25 MIN",
            tagline = "Estructura compacta lista para festival y rodaje ágil.",
            description = "Configura estilo intimista, guion conciso en una unidad dramática y carpetas enfocadas en rodaje de locación única o reducida.",
            defaultStyle = "Drama de Autor Atmosférico",
            defaultSourceType = SourceMaterialType.STORY,
            aspectRatio = "2.39:1",
            cameraRecommendation = "ARRI Alexa Mini LF · Zeiss Supreme Prime",
            continuityStarter = "Formato Cortometraje (2.39:1). Priorizar unidad de tiempo y locación, economía de diálogos y arco emocional contenido.",
            folders = listOf(
                ProductionFolder(
                    id = "sf_script",
                    name = "01_Guion_y_Escaleta",
                    category = "script",
                    description = "Guion literario breve, escaleta dramática y versión de lectura.",
                    notes = "Objetivo: 10-20 páginas con punto de giro en la página 5.",
                    targetTab = "screenplay"
                ),
                ProductionFolder(
                    id = "sf_bible",
                    name = "02_Biblia_y_Personajes",
                    category = "bible",
                    description = "Psicología del protagonista, vestuario y reglas visuales clave.",
                    targetTab = "vision"
                ),
                ProductionFolder(
                    id = "sf_schedule",
                    name = "03_Plan_de_Rodaje",
                    category = "schedule",
                    description = "Desglose por jornadas, citaciones y lista de planos esenciales.",
                    targetTab = "shots"
                ),
                ProductionFolder(
                    id = "sf_storyboard",
                    name = "04_Storyboard_y_Fotografia",
                    category = "visual",
                    description = "Fotogramas de referencia, paleta LUT y esquema de iluminación.",
                    targetTab = "storyboard"
                ),
                ProductionFolder(
                    id = "sf_sound",
                    name = "05_Sonido_y_Postproduccion",
                    category = "sound",
                    description = "Diseño sonoro, atmósferas, foley y mezcla final DCP.",
                    targetTab = "shots"
                )
            )
        ),
        CinematicProjectTemplate(
            id = "documentary",
            name = "Documental",
            englishSubtitle = "Documentary",
            badge = "DOCUMENTAL · NO FICCIÓN",
            tagline = "Investigación, transcripciones de entrevistas y montaje observacional.",
            description = "Optimizado para no ficción, audios reales y archivos de campo con carpetas dedicadas a investigación, testimonios y B-Roll.",
            defaultStyle = "Documental Observacional",
            defaultSourceType = SourceMaterialType.AUDIOBOOK,
            aspectRatio = "1.85:1",
            cameraRecommendation = "Sony VENICE 2 / FX6 · Cooke Panchro/i",
            continuityStarter = "Formato Documental (1.85:1). Luz natural motivada, registro veraz en locación y fidelidad verbatim en testimonios.",
            folders = listOf(
                ProductionFolder(
                    id = "doc_research",
                    name = "01_Tratamiento_e_Investigacion",
                    category = "bible",
                    description = "Tesis documental, dossiers de contexto histórico y cronología real.",
                    notes = "Registrar fuentes primarias y permisos de entrevista.",
                    targetTab = "agent"
                ),
                ProductionFolder(
                    id = "doc_interviews",
                    name = "02_Entrevistas_y_Transcripciones",
                    category = "script",
                    description = "Cuestionarios, transcripciones verbatim de audio y citas clave.",
                    targetTab = "screenplay"
                ),
                ProductionFolder(
                    id = "doc_field",
                    name = "03_Archivo_y_Material_de_Campo",
                    category = "field",
                    description = "Metraje B-Roll, archivos históricos y registro de locaciones reales.",
                    targetTab = "storyboard"
                ),
                ProductionFolder(
                    id = "doc_edit",
                    name = "04_Escaleta_de_Montaje",
                    category = "schedule",
                    description = "Estructura narrativa en isla de edición y ritmo de capítulos.",
                    targetTab = "shots"
                ),
                ProductionFolder(
                    id = "doc_sound",
                    name = "05_Voz_en_Off_y_Sonido_Directo",
                    category = "sound",
                    description = "Narración, sonido ambiente de campo y mezcla documental.",
                    targetTab = "shots"
                )
            )
        ),
        CinematicProjectTemplate(
            id = "feature_script",
            name = "Largometraje / Guion",
            englishSubtitle = "Feature Script",
            badge = "LARGOMETRAJE · 90–140 MIN",
            tagline = "Flujo completo de cine industrial con 3 actos, VFX y Dolby Atmos.",
            description = "Establece la estructura completa de estudio cinematográfico para adaptaciones literarias o guiones originales de larga duración.",
            defaultStyle = "Épica Monumental IMAX",
            defaultSourceType = SourceMaterialType.BOOK,
            aspectRatio = "2.39:1",
            cameraRecommendation = "ARRI Alexa 65 · Panavision Sphero 65",
            continuityStarter = "Largometraje de estudio (2.39:1 / IMAX). Estructura en 3 actos, continuidad estricta de vestuario, luz Kelvin coherente y supervisión VFX.",
            folders = listOf(
                ProductionFolder(
                    id = "ft_script",
                    name = "01_Guion_Tecnico_Actos_I_III",
                    category = "script",
                    description = "Secuencias maestras, diálogos revisados y paginación estándar.",
                    notes = "Acto I (0-25%), Acto II (25-75%), Acto III (75-100%).",
                    targetTab = "screenplay"
                ),
                ProductionFolder(
                    id = "ft_bible",
                    name = "02_Biblia_de_Continuidad_y_Mundo",
                    category = "bible",
                    description = "Reglas visuales, mapa de locaciones, líneas temporales y personajes.",
                    targetTab = "vision"
                ),
                ProductionFolder(
                    id = "ft_schedule",
                    name = "03_Desglose_y_Plan_de_Rodaje",
                    category = "schedule",
                    description = "Plan de rodaje por escenas, cámara/ópticas y logística de set.",
                    targetTab = "shots"
                ),
                ProductionFolder(
                    id = "ft_storyboard",
                    name = "04_Concept_Art_y_Storyboard",
                    category = "visual",
                    description = "Frames clave Nano, paletas de color y encuadres de acción.",
                    targetTab = "storyboard"
                ),
                ProductionFolder(
                    id = "ft_vfx",
                    name = "05_VFX_y_Supervision_Practica",
                    category = "vfx",
                    description = "Placas CGI, extensiones de set, miniaturas y SFX físicos en rodaje.",
                    targetTab = "vision"
                ),
                ProductionFolder(
                    id = "ft_atmos",
                    name = "06_Dolby_Atmos_Foley_y_Score",
                    category = "sound",
                    description = "Arquitectura Atmos 3D, motivos musicales y puntos de silencio.",
                    targetTab = "shots"
                )
            )
        ),
        CinematicProjectTemplate(
            id = "series_pilot",
            name = "Serie / Piloto",
            englishSubtitle = "Series Pilot",
            badge = "SERIE · EPISODIO PILOTO",
            tagline = "Biblia de temporada, sets recurrentes y guion de capítulo piloto.",
            description = "Diseñado para showrunners y salas de guionistas que construyen un universo serializado con ganchos episódicos.",
            defaultStyle = "Thriller Psicológico Neo-Noir",
            defaultSourceType = SourceMaterialType.BOOK,
            aspectRatio = "2.00:1",
            cameraRecommendation = "RED V-Raptor XL · Cooke Anamorphic /i",
            continuityStarter = "Episodio Piloto (2.00:1 Univisium). Establecer tono de serie, sets recurrentes de temporada y cliffhanger de cierre.",
            folders = listOf(
                ProductionFolder(
                    id = "sp_bible",
                    name = "01_Biblia_de_Serie_y_Temporada",
                    category = "bible",
                    description = "Biblia de showrunner, arcos de temporada y reglas del universo.",
                    targetTab = "agent"
                ),
                ProductionFolder(
                    id = "sp_script",
                    name = "02_Guion_del_Piloto",
                    category = "script",
                    description = "Teaser de apertura, actos televisivos y clímax de episodio.",
                    targetTab = "screenplay"
                ),
                ProductionFolder(
                    id = "sp_sets",
                    name = "03_Locaciones_y_Sets_Recurrentes",
                    category = "schedule",
                    description = "Espacios permanentes de rodaje y necesidades de arte reutilizables.",
                    targetTab = "shots"
                ),
                ProductionFolder(
                    id = "sp_lookbook",
                    name = "04_Look_Book_y_Storyboard",
                    category = "visual",
                    description = "Guía visual de LUT, lentes anamórficos y encuadres insignia.",
                    targetTab = "storyboard"
                ),
                ProductionFolder(
                    id = "sp_sound",
                    name = "05_Diseno_Sonoro_y_Cabecera",
                    category = "sound",
                    description = "Identidad sonora de cabecera, temas de personaje y mezcla.",
                    targetTab = "shots"
                )
            )
        ),
        CinematicProjectTemplate(
            id = "commercial_mv",
            name = "Comercial / Videoclip",
            englishSubtitle = "Commercial & Music Video",
            badge = "COMERCIAL / VIDEOCLIP",
            tagline = "Impacto visual de alta energía sincronizado a ritmo y BPM.",
            description = "Estructura veloz para piezas publicitarias o videoclips centrados en tratamiento estético, sincronización musical y VFX.",
            defaultStyle = "Sci-Fi Neón Cinético",
            defaultSourceType = SourceMaterialType.AUDIOBOOK,
            aspectRatio = "2.39:1",
            cameraRecommendation = "ARRI Alexa 35 · Atlas Orion Anamorphic",
            continuityStarter = "Pieza Cinética (2.39:1). Cortes rítmicos sincronizados al BPM de la pista, iluminación expresiva y transiciones visuales de alto impacto.",
            folders = listOf(
                ProductionFolder(
                    id = "cmv_treatment",
                    name = "01_Tratamiento_Creativo",
                    category = "bible",
                    description = "Manifiesto visual, paleta neón/contraste y concepto de campaña.",
                    targetTab = "vision"
                ),
                ProductionFolder(
                    id = "cmv_audio",
                    name = "02_Sincronizacion_Musical_BPM",
                    category = "sound",
                    description = "Marcadores de compás, análisis de pista de audio y drops de edición.",
                    targetTab = "shots"
                ),
                ProductionFolder(
                    id = "cmv_shots",
                    name = "03_Lista_de_Planos_Dinamicos",
                    category = "schedule",
                    description = "Movimientos Steadicam/FPV, alta velocidad y setups de iluminación.",
                    targetTab = "shots"
                ),
                ProductionFolder(
                    id = "cmv_vfx",
                    name = "04_Storyboard_y_VFX_Impacto",
                    category = "vfx",
                    description = "Frames clave, rotoscopia, partículas y color grading final.",
                    targetTab = "storyboard"
                ),
                ProductionFolder(
                    id = "cmv_script",
                    name = "05_Escaleta_y_Call_Sheet",
                    category = "script",
                    description = "Guion minuto a minuto por estrofa o bloque publicitario.",
                    targetTab = "screenplay"
                )
            )
        ),
        CinematicProjectTemplate(
            id = "custom",
            name = "Proyecto Personalizado",
            englishSubtitle = "Custom Blank Studio",
            badge = "ESTUDIO LIBRE",
            tagline = "Carpetas base esenciales para configurar tu flujo desde cero.",
            description = "Mantiene parámetros abiertos mientras crea una estructura limpia de guion, continuidad y plan de rodaje.",
            defaultStyle = "Épica Monumental IMAX",
            defaultSourceType = SourceMaterialType.BOOK,
            aspectRatio = "2.39:1",
            cameraRecommendation = "Configuración libre del director",
            continuityStarter = "",
            folders = listOf(
                ProductionFolder(
                    id = "cust_script",
                    name = "01_Guion_y_Notas",
                    category = "script",
                    description = "Borradores iniciales de escenas, diálogos y material fuente.",
                    targetTab = "screenplay"
                ),
                ProductionFolder(
                    id = "cust_bible",
                    name = "02_Direccion_y_Biblia",
                    category = "bible",
                    description = "Reglas de continuidad visual, tono y óptica del proyecto.",
                    targetTab = "vision"
                ),
                ProductionFolder(
                    id = "cust_schedule",
                    name = "03_Plan_de_Rodaje",
                    category = "schedule",
                    description = "Lista de planos, diseño sonoro y necesidades técnicas.",
                    targetTab = "shots"
                )
            )
        )
    )

    fun findById(templateId: String?): CinematicProjectTemplate =
        ALL_TEMPLATES.firstOrNull { it.id == templateId } ?: ALL_TEMPLATES.last()

    fun defaultFoldersFor(templateId: String?): List<ProductionFolder> =
        findById(templateId).folders.map { it.copy() }

    fun addCustomFolder(
        existing: List<ProductionFolder>,
        rawName: String,
        category: String = "script",
        description: String = "",
        targetTab: String = defaultTabForCategory(category)
    ): List<ProductionFolder> {
        val cleanName = rawName.trim()
            .replace(Regex("\\s+"), "_")
            .take(80)
        if (cleanName.isBlank() || existing.size >= 20) return existing
        val prefix = (existing.size + 1).toString().padStart(2, '0')
        val formattedName = if (cleanName.first().isDigit()) cleanName else "${prefix}_$cleanName"
        val newFolder = ProductionFolder(
            id = "fld_${System.currentTimeMillis()}_${existing.size + 1}",
            name = formattedName,
            category = category.ifBlank { "script" },
            description = description.trim().ifBlank { "Carpeta personalizada de producción." }.take(300),
            notes = "",
            targetTab = targetTab
        )
        return existing + newFolder
    }

    fun updateFolderNotes(
        existing: List<ProductionFolder>,
        folderId: String,
        notes: String
    ): List<ProductionFolder> = existing.map { folder ->
        if (folder.id == folderId) folder.copy(notes = notes.trim().take(2000)) else folder
    }

    fun resolveWorkspaceTab(folder: ProductionFolder): WorkspaceTab = when (folder.targetTab.lowercase()) {
        "vision" -> WorkspaceTab.VISION
        "shots" -> WorkspaceTab.SHOTS
        "storyboard" -> WorkspaceTab.STORYBOARD
        "agent" -> WorkspaceTab.AGENT
        else -> WorkspaceTab.SCREENPLAY
    }

    fun defaultTabForCategory(category: String): String = when (category.lowercase()) {
        "bible", "vfx" -> "vision"
        "schedule", "sound" -> "shots"
        "visual", "field" -> "storyboard"
        else -> "screenplay"
    }
}
