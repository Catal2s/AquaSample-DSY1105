package com.example.aquasample.model

/**
 * Resultado del conteo manual o asistido de la muestra.
 * El calibre es opcional (RF06), por eso puede ir en null.
 */
data class Conteo(
    val individuos: Int,
    val calibrePromedioMm: Double? = null,
    val asistidoPorFoto: Boolean = false,
    val marcasRegistradas: Int = 0,
    val ajusteManual: Int = 0
)

