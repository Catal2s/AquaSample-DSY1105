package com.example.aquasample.data

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.aquasample.model.Muestra
import java.io.IOException
import java.io.OutputStream

/**
 * Dibuja el reporte de muestras validadas en un PDF usando PdfDocument (incluido en Android,
 * no necesita librerías). Hoja A4 horizontal; si las filas no caben, se agregan páginas.
 * Los datos de la tabla vienen de ReporteMuestras.
 */
object GeneradorPdf {

    // Hoja A4 horizontal, en puntos (1/72 de pulgada)
    private const val ANCHO = 842
    private const val ALTO = 595
    private const val MARGEN = 36f
    private const val ALTO_FILA = 20f

    /** Ancho relativo de cada columna (suma 1). Observaciones es la más ancha. */
    private val PESOS = listOf(0.05f, 0.11f, 0.13f, 0.07f, 0.07f, 0.07f, 0.08f, 0.08f, 0.25f, 0.09f)

    private const val COLOR_PRINCIPAL = 0xFF00695C.toInt() // Identidad visual de AquaSample
    private const val COLOR_FILA_ALTERNA = 0xFFF4FAF9.toInt()

    /**
     * Escribe el PDF en [salida]. Devuelve la cantidad de páginas generadas.
     * [generadoEn] es la fecha y hora que se imprime en el encabezado.
     */
    fun escribir(salida: OutputStream, muestras: List<Muestra>, generadoEn: String): Int {
        val filas = ReporteMuestras.filas(muestras)
        val anchoUtil = ANCHO - 2 * MARGEN
        val anchos = PESOS.map { it * anchoUtil }

        val titulo = Paint().apply { color = COLOR_PRINCIPAL; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val subtitulo = Paint().apply { color = Color.DKGRAY; textSize = 10f; isAntiAlias = true }
        val encabezado = Paint().apply { color = Color.WHITE; textSize = 9f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val texto = Paint().apply { color = 0xFF1B1B1B.toInt(); textSize = 9f; isAntiAlias = true }
        val fondo = Paint().apply { style = Paint.Style.FILL }

        val documento = PdfDocument()
        var numeroPagina = 0
        var indiceFila = 0
        try {
            do {
                numeroPagina++
                val pagina = documento.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numeroPagina).create())
                val lienzo = pagina.canvas
                var y = MARGEN + 18f

                // Título, fecha de generación y resumen
                lienzo.drawText("AquaSample · Reporte de muestras validadas", MARGEN, y, titulo)
                y += 16f
                lienzo.drawText("Generado: $generadoEn · Datos ficticios para fines académicos", MARGEN, y, subtitulo)
                y += 14f
                lienzo.drawText(ReporteMuestras.resumen(muestras), MARGEN, y, subtitulo)
                y += 14f

                // Encabezado de la tabla (fondo verde de la marca)
                fondo.color = COLOR_PRINCIPAL
                lienzo.drawRect(MARGEN, y, ANCHO - MARGEN, y + ALTO_FILA, fondo)
                dibujarFila(lienzo, ReporteMuestras.COLUMNAS, anchos, y, encabezado)
                y += ALTO_FILA

                // Filas que caben en esta página
                var alterna = false
                while (indiceFila < filas.size && y + ALTO_FILA <= ALTO - MARGEN - 14f) {
                    if (alterna) {
                        fondo.color = COLOR_FILA_ALTERNA
                        lienzo.drawRect(MARGEN, y, ANCHO - MARGEN, y + ALTO_FILA, fondo)
                    }
                    dibujarFila(lienzo, filas[indiceFila], anchos, y, texto)
                    y += ALTO_FILA
                    indiceFila++
                    alterna = !alterna
                }
                if (filas.isEmpty()) {
                    lienzo.drawText("No hay muestras validadas para informar.", MARGEN, y + 14f, texto)
                }

                // Pie de página
                lienzo.drawText("Página $numeroPagina", ANCHO - MARGEN - 50f, ALTO - MARGEN + 10f, subtitulo)
                documento.finishPage(pagina)
            } while (indiceFila < filas.size)

            documento.writeTo(salida)
        } finally {
            documento.close()
        }
        return numeroPagina
    }

    /** Dibuja una fila; si un texto no cabe en su columna se corta con "…". */
    private fun dibujarFila(lienzo: android.graphics.Canvas, celdas: List<String>, anchos: List<Float>, y: Float, pincel: Paint) {
        var x = MARGEN
        celdas.forEachIndexed { i, valor ->
            val espacio = anchos[i] - 6f
            lienzo.drawText(recortar(valor, espacio, pincel), x + 3f, y + 13f, pincel)
            x += anchos[i]
        }
    }

    private fun recortar(valor: String, ancho: Float, pincel: Paint): String {
        if (pincel.measureText(valor) <= ancho) return valor
        var corto = valor
        while (corto.isNotEmpty() && pincel.measureText("$corto…") > ancho) corto = corto.dropLast(1)
        return "$corto…"
    }

    /**
     * Guarda el reporte en el archivo elegido por el usuario (selector de documentos de Android).
     * Devuelve cuántas muestras validadas se exportaron, 0 si no había ninguna, o -1 si falló.
     */
    fun exportar(contexto: Context, destino: Uri, muestras: List<Muestra>, generadoEn: String): Int =
        try {
            val flujo = contexto.contentResolver.openOutputStream(destino) ?: throw IOException("Sin acceso al archivo")
            flujo.use { escribir(it, muestras, generadoEn) }
            ReporteMuestras.validadas(muestras).size
        } catch (e: IOException) {
            -1
        }
}
