package com.example.aquasample.model

/**
 * Muestra de choritos registrada en terreno.
 *
 * Fecha y hora van como texto ("2026-09-30" y "10:30") para no depender
 * de java.time, que en versiones antiguas de Android requiere configuración extra.
 */
data class Muestra(
    val id: Int,
    val centro: Centro,
    val tren: Tren,
    val linea: Linea,
    val fecha: String,
    val hora: String,
    val tramoMetros: Double,
    val operador: Usuario,
    val conteo: Conteo,
    val observaciones: String = "",
    val estado: EstadoRevision = EstadoRevision.PENDIENTE,
    val comentarioSupervisor: String? = null
)
