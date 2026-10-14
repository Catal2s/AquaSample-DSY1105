package com.example.aquasample.model

/**
 * Resultado del conteo manual de la muestra.
 * El calibre es opcional (RF06), por eso puede ir en null.
 */
data class Conteo(
    val individuos: Int,
    val calibrePromedioMm: Double? = null
)
