package com.example.aquasample.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.example.aquasample.model.EstadoRevision
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayOutputStream
import java.io.IOException

/** Pruebas de la exportación a CSV (RF10). */
class ExportadorCsvSpec : StringSpec({

    val ejemplos = DatosFicticios.muestrasEjemplo

    /** Contexto de Android simulado: escribe en memoria en vez de un archivo real. */
    fun contextoQueEscribeEn(salida: ByteArrayOutputStream?): Pair<Context, Uri> {
        val resolver = mockk<ContentResolver>()
        val contexto = mockk<Context>()
        val destino = mockk<Uri>()
        every { contexto.contentResolver } returns resolver
        every { resolver.openOutputStream(destino) } returns salida
        return contexto to destino
    }

    "el CSV parte con BOM y encabezados separados por punto y coma" {
        val csv = muestrasACsv(emptyList())

        csv shouldStartWith "﻿id;centro;tren;linea;fecha;hora;tramo_m;operador"
    }

    "agrega una fila por muestra con sus datos" {
        val lineas = muestrasACsv(ejemplos).split("\r\n")

        lineas.size shouldBe 1 + ejemplos.size
        lineas[1] shouldBe "1;Centro Ficticio Norte;Tren 1;Línea 1;2026-09-22;09:15;0.2;Operador Demo 01;312;38.5;Validado;Muestra sin novedades;"
        // Sin calibre la celda queda vacía
        lineas[2] shouldContain ";287;;Observado;"
    }

    "protege las celdas con punto y coma, comillas o fórmulas" {
        val muestra = ejemplos[0].copy(
            observaciones = "dice \"hola\"; chao",
            comentarioSupervisor = "=SUMA(A1)",
            estado = EstadoRevision.OBSERVADO
        )

        val fila = muestrasACsv(listOf(muestra)).split("\r\n")[1]

        fila shouldContain "\"dice \"\"hola\"\"; chao\""
        fila shouldContain "'=SUMA(A1)"
    }

    "exportar escribe el archivo elegido y devuelve cuántas muestras exportó" {
        val salida = ByteArrayOutputStream()
        val (contexto, destino) = contextoQueEscribeEn(salida)

        exportarMuestrasCsv(contexto, destino, ejemplos) shouldBe 5

        salida.toString(Charsets.UTF_8.name()) shouldBe muestrasACsv(ejemplos)
    }

    "si no se puede abrir o escribir el archivo devuelve -1" {
        val (sinAcceso, destino1) = contextoQueEscribeEn(null)
        exportarMuestrasCsv(sinAcceso, destino1, ejemplos) shouldBe -1

        val resolver = mockk<ContentResolver>()
        val contexto = mockk<Context>()
        val destino2 = mockk<Uri>()
        every { contexto.contentResolver } returns resolver
        every { resolver.openOutputStream(destino2) } throws IOException("disco lleno")
        exportarMuestrasCsv(contexto, destino2, ejemplos) shouldBe -1
    }
})
