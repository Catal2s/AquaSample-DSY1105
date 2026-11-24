package com.example.aquasample

import com.example.aquasample.data.DatosFicticios
import com.example.aquasample.data.GeneradorPdf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Prueba del PDF en un dispositivo: PdfDocument solo funciona dentro de Android,
 * por eso esta prueba no es unitaria sino instrumentada (corre en el emulador).
 */
class GeneradorPdfTest {

    @Test
    fun generaUnPdfValidoConLasMuestrasValidadas() {
        val salida = ByteArrayOutputStream()

        val paginas = GeneradorPdf.escribir(salida, DatosFicticios.muestrasEjemplo, "2026-11-24 10:00")

        assertEquals(1, paginas)
        val inicio = String(salida.toByteArray().copyOfRange(0, 5), Charsets.US_ASCII)
        assertEquals("%PDF-", inicio) // todo archivo PDF parte así

        // Copia para revisar el resultado a mano (evidencia):
        // adb pull /sdcard/Android/data/com.example.aquasample/files/reporte_prueba.pdf
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        contexto.getExternalFilesDir(null)?.let { File(it, "reporte_prueba.pdf").writeBytes(salida.toByteArray()) }
    }

    @Test
    fun siHayMuchasMuestrasAgregaMasPaginas() {
        val base = DatosFicticios.muestrasEjemplo.first()
        val muchas = (1..60).map { base.copy(id = it) }
        val salida = ByteArrayOutputStream()

        val paginas = GeneradorPdf.escribir(salida, muchas, "2026-11-24 10:00")

        assertTrue("Se esperaban varias páginas y hubo $paginas", paginas > 1)
    }
}
