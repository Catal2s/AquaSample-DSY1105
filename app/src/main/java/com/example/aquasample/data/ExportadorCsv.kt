package com.example.aquasample.data

import android.content.Context
import android.net.Uri
import com.example.aquasample.model.Muestra
import java.io.IOException

/**
 * Exportación simulada a planilla (RF10 del caso).
 *
 * Convierte una lista de muestras a texto CSV y lo guarda en el archivo que el usuario
 * eligió con el selector de documentos de Android (Storage Access Framework).
 * Ese selector no necesita permisos: la app solo puede escribir en el archivo elegido.
 */

private const val SEPARADOR = ";" // Excel en español separa las columnas con punto y coma
private const val BOM_UTF8 = "﻿" // para que Excel muestre bien las tildes

private val ENCABEZADOS = listOf(
    "id", "centro", "tren", "linea", "fecha", "hora", "tramo_m", "operador",
    "individuos", "calibre_mm", "estado", "observaciones", "comentario_supervisor"
)

/** Arma el texto completo del CSV: una fila de encabezados y una fila por muestra. */
fun muestrasACsv(muestras: List<Muestra>): String {
    val filas = muestras.map { m ->
        listOf(
            m.id.toString(),
            m.centro.nombre,
            m.tren.nombre,
            m.linea.nombre,
            m.fecha,
            m.hora,
            m.tramoMetros.toString(),
            m.operador.nombre,
            m.conteo.individuos.toString(),
            m.conteo.calibrePromedioMm?.toString() ?: "",
            m.estado.etiqueta,
            m.observaciones,
            m.comentarioSupervisor ?: ""
        )
    }
    return BOM_UTF8 + (listOf(ENCABEZADOS) + filas)
        .joinToString("\r\n") { fila -> fila.joinToString(SEPARADOR) { celda(it) } }
}

/** Protege una celda: comillas si trae separador, comillas o saltos de línea; y evita que Excel la lea como fórmula. */
private fun celda(valor: String): String {
    val seguro = if (valor.isNotEmpty() && valor[0] in "=+-@") "'$valor" else valor
    val necesitaComillas = seguro.any { it == ';' || it == '"' || it == '\n' || it == '\r' }
    return if (necesitaComillas) "\"" + seguro.replace("\"", "\"\"") + "\"" else seguro
}

/**
 * Escribe el CSV en el archivo elegido por el usuario.
 * Devuelve la cantidad de muestras exportadas, o -1 si no se pudo escribir.
 */
fun exportarMuestrasCsv(contexto: Context, destino: Uri, muestras: List<Muestra>): Int =
    try {
        val flujo = contexto.contentResolver.openOutputStream(destino) ?: throw IOException("Sin acceso al archivo")
        flujo.use { it.write(muestrasACsv(muestras).toByteArray(Charsets.UTF_8)) }
        muestras.size
    } catch (e: IOException) {
        -1
    }
