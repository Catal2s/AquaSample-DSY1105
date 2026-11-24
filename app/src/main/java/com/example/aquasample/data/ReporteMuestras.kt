package com.example.aquasample.data

import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.Muestra
import java.util.Locale

/**
 * Datos del reporte de muestras validadas (exportación a PDF).
 * Aquí solo se decide QUÉ va en el reporte; cómo se dibuja está en GeneradorPdf.
 * Al ser lógica pura (sin Android), se prueba con pruebas unitarias.
 */
object ReporteMuestras {

    /** Columnas pedidas en el caso: ID, fecha, centro, tren, línea, longitud, conteo, observaciones y estado. */
    val COLUMNAS = listOf(
        "ID", "Fecha", "Centro", "Tren", "Línea", "Tramo (m)",
        "Individuos", "Calibre (mm)", "Observaciones", "Estado"
    )

    /** Solo las muestras validadas por el supervisor, de la más antigua a la más reciente. */
    fun validadas(muestras: List<Muestra>): List<Muestra> =
        muestras
            .filter { it.estado == EstadoRevision.VALIDADO }
            .sortedWith(compareBy<Muestra> { it.fecha }.thenBy { it.hora }.thenBy { it.id })

    /** Una fila de texto por muestra validada, en el mismo orden que COLUMNAS. */
    fun filas(muestras: List<Muestra>): List<List<String>> =
        validadas(muestras).map { m ->
            listOf(
                "#${m.id}",
                "${m.fecha} ${m.hora}",
                m.centro.nombre,
                m.tren.nombre,
                m.linea.nombre,
                decimal(m.tramoMetros),
                m.conteo.individuos.toString(),
                m.conteo.calibrePromedioMm?.let { decimal(it) } ?: "-",
                m.observaciones.ifBlank { "-" },
                m.estado.etiqueta
            )
        }

    /** Línea de resumen bajo el título, por ejemplo: "2 muestras validadas · 713 individuos · promedio 356,5". */
    fun resumen(muestras: List<Muestra>): String {
        val lista = validadas(muestras)
        val total = lista.sumOf { it.conteo.individuos }
        val promedio = if (lista.isEmpty()) 0.0 else total.toDouble() / lista.size
        val palabra = if (lista.size == 1) "muestra validada" else "muestras validadas"
        return "${lista.size} $palabra · $total individuos · promedio ${decimal(promedio)}"
    }

    /** Número con coma decimal, como se escribe en Chile (0,2 en vez de 0.2). */
    fun decimal(valor: Double): String {
        val texto = String.format(Locale.US, "%.2f", valor).trimEnd('0').trimEnd('.')
        return texto.replace('.', ',')
    }
}
