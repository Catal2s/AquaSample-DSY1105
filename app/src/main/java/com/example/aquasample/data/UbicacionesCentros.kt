package com.example.aquasample.data

data class Coordenadas(val latitud: Double, val longitud: Double)

/**
 * Ubicación de cada centro para consultar las condiciones marinas.
 *
 * Son coordenadas APROXIMADAS y solo de referencia: puntos en el mar del sur de Chile
 * (sector de Calbuco y Chiloé). Los centros son ficticios y no corresponden
 * a centros de cultivo reales de ninguna empresa.
 */
object UbicacionesCentros {

    private val porCentro = mapOf(
        "C1" to Coordenadas(latitud = -41.95, longitud = -73.05),
        "C2" to Coordenadas(latitud = -42.60, longitud = -73.50)
    )

    fun de(centroId: String): Coordenadas? = porCentro[centroId]
}
