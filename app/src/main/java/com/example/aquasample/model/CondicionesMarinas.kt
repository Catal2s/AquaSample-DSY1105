package com.example.aquasample.model

/**
 * Condiciones marinas de referencia para una muestra.
 * Son estimaciones de un modelo (Open-Meteo Marine), no mediciones del centro.
 * Un valor en null significa que el modelo no tiene ese dato.
 */
data class CondicionesMarinas(
    val fecha: String,
    /** Hora del dato usado ("09:00"). */
    val hora: String,
    val latitud: Double,
    val longitud: Double,
    val alturaOlaM: Double?,
    val periodoOlaS: Double?,
    /** Dirección DESDE la que vienen las olas: 0 = norte, 90 = este. */
    val direccionOlaGrados: Double?,
    val temperaturaMarC: Double?
)

/** Convierte grados a punto cardinal en español. Ejemplo: 315 es "NO". */
fun puntoCardinal(grados: Double): String {
    val puntos = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    val normalizado = ((grados % 360) + 360) % 360
    return puntos[((normalizado + 22.5) / 45).toInt() % puntos.size]
}
