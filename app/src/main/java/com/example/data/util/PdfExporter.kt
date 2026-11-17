package com.example.data.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {
    private const val PAGE_W = 595 // A4 width in pt (72 dpi)
    private const val PAGE_H = 842 // A4 height in pt
    private const val MARGIN = 44f
    private const val CONTENT_W = PAGE_W - (MARGIN * 2f)

    private fun exportsDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun safeSlug(raw: String): String =
        raw.lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(40)
            .ifBlank { "lumenreel" }

    suspend fun saveTextExport(
        context: Context,
        filename: String,
        content: String
    ): File = withContext(Dispatchers.IO) {
        val outFile = File(exportsDir(context), filename)
        outFile.writeText(content, Charsets.UTF_8)
        outFile
    }

    private class PdfWriter {
        val document = PdfDocument()
        private var currentPage: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var pageNumber = 0
        var y = MARGIN

        private val headerPaint = Paint().apply {
            color = Color.rgb(18, 18, 24)
            style = Paint.Style.FILL
        }
        private val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(229, 169, 60)
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        private val linePaint = Paint().apply {
            color = Color.rgb(215, 215, 222)
            strokeWidth = 0.8f
        }
        private val boxFillPaint = Paint().apply {
            color = Color.rgb(246, 244, 238)
            style = Paint.Style.FILL
        }
        private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(120, 120, 130)
            textSize = 8.5f
        }

        fun startNewPage() {
            finishCurrentPage()
            pageNumber += 1
            val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create()
            currentPage = document.startPage(info)
            canvas = currentPage!!.canvas
            canvas!!.drawColor(Color.WHITE)
            y = MARGIN
        }

        private fun finishCurrentPage() {
            val page = currentPage ?: return
            val c = canvas ?: return
            c.drawText("LumenReel Studio · Expediente Cinematográfico", MARGIN, PAGE_H - 22f, footerPaint)
            val rightText = "Página $pageNumber"
            val tw = footerPaint.measureText(rightText)
            c.drawText(rightText, PAGE_W - MARGIN - tw, PAGE_H - 22f, footerPaint)
            document.finishPage(page)
            currentPage = null
            canvas = null
        }

        fun ensureSpace(neededHeight: Float) {
            if (currentPage == null) {
                startNewPage()
                return
            }
            if (y + neededHeight > PAGE_H - 46f) {
                startNewPage()
            }
        }

        fun drawBanner(title: String, subtitle: String, meta: String) {
            ensureSpace(102f)
            val c = canvas!!
            c.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 92f, headerPaint)
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(245, 240, 232)
                textSize = 17f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(229, 169, 60)
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(185, 182, 174)
                textSize = 9f
            }
            c.drawText(title.take(46), MARGIN + 16f, y + 30f, titlePaint)
            c.drawText(subtitle.take(64), MARGIN + 16f, y + 52f, subPaint)
            c.drawText(meta.take(78), MARGIN + 16f, y + 72f, metaPaint)
            y += 108f
        }

        fun drawSectionHeading(title: String) {
            ensureSpace(34f)
            val c = canvas!!
            y += 6f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(168, 114, 24)
                textSize = 11.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            c.drawText(title, MARGIN, y + 12f, paint)
            y += 18f
            c.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
            y += 10f
        }

        fun drawParagraph(
            text: String,
            textSizeSp: Float = 9.5f,
            bold: Boolean = false,
            color: Int = Color.rgb(42, 44, 52),
            indent: Float = 0f,
            mono: Boolean = false,
            spacingAfter: Float = 6f
        ) {
            if (text.isBlank()) return
            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.textSize = textSizeSp
                this.color = color
                this.typeface = Typeface.create(
                    if (mono) Typeface.MONOSPACE else Typeface.SANS_SERIF,
                    if (bold) Typeface.BOLD else Typeface.NORMAL
                )
            }
            val widthInt = (CONTENT_W - indent).toInt().coerceAtLeast(100)
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, tp, widthInt)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2.5f, 1.0f)
                .build()

            if (layout.height + 12f > PAGE_H - 96f) {
                for (lineIdx in 0 until layout.lineCount) {
                    val start = layout.getLineStart(lineIdx)
                    val end = layout.getLineEnd(lineIdx)
                    val lineStr = text.substring(start, end)
                    val lineH = (layout.getLineBottom(lineIdx) - layout.getLineTop(lineIdx)).toFloat()
                    ensureSpace(lineH + 4f)
                    canvas!!.drawText(lineStr, MARGIN + indent, y + textSizeSp, tp)
                    y += lineH + 1f
                }
            } else {
                ensureSpace(layout.height + spacingAfter)
                canvas!!.save()
                canvas!!.translate(MARGIN + indent, y)
                layout.draw(canvas!!)
                canvas!!.restore()
                y += layout.height.toFloat()
            }
            y += spacingAfter
        }

        fun drawCalloutBox(header: String, lines: List<String>) {
            val needed = 28f + lines.size * 15f
            ensureSpace(needed)
            val c = canvas!!
            c.drawRect(MARGIN, y, PAGE_W - MARGIN, y + needed, boxFillPaint)
            c.drawText(header, MARGIN + 12f, y + 18f, goldPaint)
            var boxY = y + 34f
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(40, 42, 48)
                textSize = 9f
            }
            for (line in lines) {
                c.drawText(line, MARGIN + 12f, boxY, bodyPaint)
                boxY += 15f
            }
            y += needed + 10f
        }

        fun drawStoryboardImage(imagePath: String, caption: String) {
            val file = File(imagePath)
            if (!file.exists()) return
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
            val ratio = if (bitmap.width > 0) bitmap.height.toFloat() / bitmap.width.toFloat() else 9f / 16f
            val targetW = CONTENT_W * 0.85f
            val targetH = (targetW * ratio).coerceAtMost(220f)
            val drawW = targetH / ratio
            ensureSpace(targetH + 28f)
            val left = MARGIN + (CONTENT_W - drawW) / 2f
            val dst = Rect(left.toInt(), y.toInt(), (left + drawW).toInt(), (y + targetH).toInt())
            canvas!!.drawBitmap(bitmap, null, dst, null)
            bitmap.recycle()
            y += targetH + 8f
            drawParagraph(caption, textSizeSp = 8.5f, color = Color.rgb(100, 100, 110), spacingAfter = 8f)
        }

        fun writeToFile(file: File): File {
            finishCurrentPage()
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            return file
        }
    }

    suspend fun exportSequencePdf(
        context: Context,
        project: ProductionProject,
        sequence: ProductionSequence
    ): File = withContext(Dispatchers.IO) {
        check(LearningEngine.canExportPdfForSequence(sequence)) {
            "La exportación PDF solo está permitida cuando la secuencia está aprobada en Versión Final."
        }
        val writer = PdfWriter()
        writer.startNewPage()
        writer.drawBanner(
            title = project.title.uppercase(),
            subtitle = "BITÁCORA OFICIAL · ${sequence.title}",
            meta = "${sequence.directorStyle} · ${sequence.aspectRatio} · ${sequence.cameraSystem}"
        )

        writer.drawSectionHeading("1. Visión de Dirección y Óptica")
        writer.drawParagraph(sequence.logline, textSizeSp = 10.5f, bold = true)
        writer.drawParagraph(sequence.directorVision)
        writer.drawParagraph("Cámara: ${sequence.cameraSystem} | Lentes: ${sequence.lensPackage} | LUT: ${sequence.filmStockOrLut}", mono = true, textSizeSp = 8.8f)
        if (sequence.colorPalette.isNotEmpty()) {
            writer.drawParagraph(
                "Paleta: ${sequence.colorPalette.joinToString(" · ") { "${it.name} (${it.hex})" }}",
                textSizeSp = 9f
            )
        }

        writer.drawSectionHeading("2. Esquema de Iluminación y Atmósfera")
        writer.drawParagraph("• Luz principal: ${sequence.lighting.keyLight}")
        writer.drawParagraph("• Ratio de contraste: ${sequence.lighting.fillRatio}")
        writer.drawParagraph("• Contraluz / Rim: ${sequence.lighting.rimAndBacklight}")
        writer.drawParagraph("• Temperatura Kelvin: ${sequence.lighting.colorTemperatureKelvin}", mono = true)
        writer.drawParagraph("• Atmósfera: ${sequence.lighting.atmosphereAndHaze}")

        writer.drawSectionHeading("3. Lista de Planos (${sequence.shotList.size})")
        for (shot in sequence.shotList) {
            writer.drawParagraph(
                "Plano ${shot.shotNumber} [${shot.shotType}] (${shot.lensMm} · ${shot.movement} · ${shot.durationSec}s) — ${shot.framingNote} | Intención: ${shot.psychologicalIntent}",
                textSizeSp = 9f
            )
        }

        writer.drawSectionHeading("4. Diseño Sonoro Dolby Atmos (${sequence.soundDesign.tempoBpm} BPM)")
        writer.drawParagraph("• Capas Atmos: ${sequence.soundDesign.atmosLayers.joinToString(" | ")}")
        writer.drawParagraph("• Subgraves LFE: ${sequence.soundDesign.subBassCue}")
        writer.drawParagraph("• Foley: ${sequence.soundDesign.foleyDetails.joinToString(" | ")}")
        writer.drawParagraph("• Silencios dramáticos: ${sequence.soundDesign.silenceBeats}")
        writer.drawParagraph("• Score musical: ${sequence.soundDesign.musicalScore}")

        sequence.audioAnalysis?.let { aa ->
            writer.drawSectionHeading("4B. Análisis de Audio y Transcripción")
            writer.drawParagraph("• Narración: ${aa.narrationStyle}")
            writer.drawParagraph("• Arco emocional: ${aa.emotionalArc}")
            writer.drawParagraph("• Ritmo: ${aa.rhythmAndPacing}")
            writer.drawParagraph("• Transcripción: ${aa.transcription}", mono = true, textSizeSp = 8.5f)
        }

        writer.drawSectionHeading("5. VFX, SFX Prácticos y Diseño de Arte")
        writer.drawParagraph("• CGI / Simulación: ${sequence.vfx.cgiElements.joinToString(" | ")}")
        writer.drawParagraph("• Efectos Prácticos (SFX): ${sequence.vfx.practicalSfx.joinToString(" | ")}")
        writer.drawParagraph("• Locaciones y Sets: ${sequence.vfx.locationsAndSets}")
        writer.drawParagraph("• Vestuario y Maquillaje: ${sequence.vfx.wardrobeAndMakeup}")

        writer.drawSectionHeading("6. Guion Técnico-Literario")
        for (scene in sequence.screenplay) {
            writer.drawParagraph("ESCENA ${scene.sceneNumber}: ${scene.slugline}", bold = true, mono = true, textSizeSp = 9.5f)
            for (beat in scene.beats) {
                when (beat.type.lowercase()) {
                    "dialogue" -> {
                        writer.drawParagraph((beat.character ?: "PERSONAJE").uppercase(), bold = true, indent = 40f, mono = true, spacingAfter = 1f)
                        if (!beat.parenthetical.isNullOrBlank()) {
                            writer.drawParagraph("(${beat.parenthetical})", indent = 48f, mono = true, textSizeSp = 8.5f, spacingAfter = 1f)
                        }
                        writer.drawParagraph(beat.text, indent = 28f, mono = true, textSizeSp = 9f)
                    }
                    "camera" -> writer.drawParagraph("[CÁMARA] ${beat.text}", color = Color.rgb(168, 114, 24), mono = true, textSizeSp = 8.8f)
                    "sound" -> writer.drawParagraph("[SONIDO] ${beat.text}", color = Color.rgb(55, 115, 130), mono = true, textSizeSp = 8.8f)
                    else -> writer.drawParagraph(beat.text, mono = true, textSizeSp = 9f)
                }
            }
        }

        writer.drawSectionHeading("7. Storyboard y Prompts Visuales (${sequence.storyboardFrames.size})")
        for (frame in sequence.storyboardFrames) {
            writer.drawParagraph("${frame.title} (${frame.shotRef} · ${frame.cameraSpec})", bold = true)
            if (!frame.localImagePath.isNullOrBlank()) {
                writer.drawStoryboardImage(frame.localImagePath, "${frame.shotRef} — ${frame.title}")
            }
            writer.drawParagraph("Prompt Nano: ${frame.visualPrompt}", textSizeSp = 8.5f, color = Color.rgb(70, 72, 80))
            writer.drawParagraph("Movimiento: ${frame.veoMotionPrompt}", textSizeSp = 8.5f, color = Color.rgb(90, 92, 100))
        }

        val outFile = File(exportsDir(context), "${safeSlug(project.title)}_${safeSlug(sequence.title)}_Final.pdf")
        writer.writeToFile(outFile)
    }

    suspend fun exportLibraryPdf(
        context: Context,
        projects: List<ProductionProject>
    ): File = withContext(Dispatchers.IO) {
        val writer = PdfWriter()
        writer.startNewPage()
        writer.drawBanner(
            title = "LUMENREEL STUDIO",
            subtitle = "BIBLIOTECA DE VERSIONES FINALES",
            meta = "${projects.size} proyectos indexados · Exportado ${SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-ES")).format(Date())}"
        )

        var anyFinal = false
        for (proj in projects) {
            val finals = LearningEngine.getFinalSequences(proj)
            if (finals.isEmpty()) continue
            anyFinal = true
            writer.drawSectionHeading("${proj.title} (${proj.genreStyle})")
            if (proj.continuityBible.isNotBlank()) {
                writer.drawParagraph("Biblia de Continuidad: ${proj.continuityBible.take(800)}")
            }
            for (seq in finals) {
                writer.drawParagraph("• ${seq.title} [${seq.aspectRatio}] — ${seq.cameraSystem} / ${seq.lensPackage}", bold = true)
                writer.drawParagraph("  ${seq.logline}", indent = 12f)
            }
        }

        if (!anyFinal) {
            writer.drawParagraph("Aún no existen secuencias aprobadas en Versión Final. Aprueba al menos una secuencia para incluirla en la Biblioteca Oficial.")
        }

        val outFile = File(exportsDir(context), "LumenReel_Biblioteca_Finales.pdf")
        writer.writeToFile(outFile)
    }

    suspend fun exportUserManualPdf(context: Context): File = withContext(Dispatchers.IO) {
        val writer = PdfWriter()
        writer.startNewPage()
        writer.drawBanner(
            title = "LUMENREEL STUDIO",
            subtitle = "MANUAL DE USO E INSTRUCCIONES DEL SISTEMA",
            meta = "Guía rápida para producción cinematográfica con Gemini & Firestore"
        )

        writer.drawParagraph("LumenReel Studio convierte capítulos de libros, audiolibros (MP3, WAV, M4A, OGG) e historias originales en producciones cinematográficas completas directamente desde Android.")

        writer.drawCalloutBox(
            "FLUJO RÁPIDO EN 5 PASOS",
            listOf(
                "1. Inicia sesión con tu cuenta de Google para aislar tu estudio privado en la nube.",
                "2. Crea una producción e ingresa tu capítulo, PDF o archivo de audio en la Mesa de Ingesta.",
                "3. Revisa Dirección (Radar de Progreso, Paleta, Iluminación Kelvin y Studio Intelligence).",
                "4. Explora Planos y Sonido, exporta el Guion (.md / .fountain) y genera Storyboard con Nano.",
                "5. Aprueba la Versión Final para desbloquear la exportación oficial en Bitácora PDF."
            )
        )

        writer.drawSectionHeading("PASO 1. Acceso y Privacidad con Google")
        writer.drawParagraph("• Autenticación obligatoria mediante Google Sign-In (Credential Manager). Cada cuenta mantiene proyectos, secuencias, historial del agente y memoria de aprendizaje privados.")

        writer.drawSectionHeading("PASO 2. Ingesta de Material Fuente y Biblia")
        writer.drawParagraph("• Pega texto o adjunta archivos PDF, TXT, MD, Fountain o Audio (MP3, WAV, M4A, OGG).")
        writer.drawParagraph("• Analizar solo audio extrae transcripción verbatim, estilo vocal, arco emocional y efectos sonoros detectados.")

        writer.drawSectionHeading("PASO 3. Agente Autónomo Tripartito")
        writer.drawParagraph("• Conversa en modo Consejo Completo, Director de Cine, Guionista o Experto VFX/SFX para editar automáticamente la Biblia, cámara, VFX, guion o storyboard.")

        writer.drawSectionHeading("PASO 4. Dirección, Radar y Studio Intelligence")
        writer.drawParagraph("• El Gráfico Radar mide 4 ejes (Guion, Iluminación, Efectos Visuales y Sonido) y compara el promedio del proyecto contra la secuencia activa.")
        writer.drawParagraph("• Califica tus secuencias (1–5 ★) para alimentar Studio Intelligence y reutilizar tus mejores decisiones ópticas.")

        writer.drawSectionHeading("PASO 5. Versión Final y Exportación PDF")
        writer.drawParagraph("• Las secuencias aprobadas como Versión Final quedan selladas contra cambios accidentales, nutren el Autoaprendizaje y habilitan la Bitácora PDF y Biblioteca PDF.")

        val outFile = File(exportsDir(context), "LumenReel_Manual_de_Uso.pdf")
        writer.writeToFile(outFile)
    }
}
