package com.example.aquasample.data

import com.example.aquasample.model.EstadoRevision
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/** Pruebas del contenido del reporte PDF de muestras validadas. */
class ReporteMuestrasSpec : StringSpec({

    val ejemplos = DatosFicticios.muestrasEjemplo // validadas: #1 (22-09) y #3 (24-09)

    "el reporte incluye solo las muestras validadas, de la más antigua a la más reciente" {
        ReporteMuestras.validadas(ejemplos).map { it.id } shouldContainExactly listOf(1, 3)
    }

    "cada fila trae las columnas pedidas en el caso, en orden" {
        val filas = ReporteMuestras.filas(ejemplos)

        filas.size shouldBe 2
        filas[0] shouldContainExactly listOf(
            "#1", "2026-09-22 09:15", "Centro Ficticio Norte", "Tren 1", "Línea 1",
            "0,2", "312", "38,5", "Muestra sin novedades", "Validado"
        )
        filas[0].size shouldBe ReporteMuestras.COLUMNAS.size
    }

    "sin calibre ni observaciones la celda muestra un guion" {
        val sinDatos = ejemplos[0].copy(observaciones = " ", conteo = ejemplos[0].conteo.copy(calibrePromedioMm = null))

        val fila = ReporteMuestras.filas(listOf(sinDatos)).single()

        fila[7] shouldBe "-"
        fila[8] shouldBe "-"
    }

    "el resumen cuenta muestras, individuos y promedio" {
        ReporteMuestras.resumen(ejemplos) shouldBe "2 muestras validadas · 713 individuos · promedio 356,5"
        ReporteMuestras.resumen(listOf(ejemplos[0])) shouldBe "1 muestra validada · 312 individuos · promedio 312"
    }

    "sin muestras validadas el reporte queda vacío" {
        val pendientes = ejemplos.map { it.copy(estado = EstadoRevision.PENDIENTE) }

        ReporteMuestras.filas(pendientes).shouldBeEmpty()
        ReporteMuestras.resumen(pendientes) shouldBe "0 muestras validadas · 0 individuos · promedio 0"
    }

    "los decimales se escriben con coma y sin ceros de más" {
        ReporteMuestras.decimal(0.2) shouldBe "0,2"
        ReporteMuestras.decimal(1.25) shouldBe "1,25"
        ReporteMuestras.decimal(40.0) shouldBe "40"
    }
})
