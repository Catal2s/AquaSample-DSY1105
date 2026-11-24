package com.example.aquasample.data.remote.backend

/** Cuerpo de POST y PUT. El estado no va aquí: solo cambia con PATCH. */
data class MuestraRequest(
    val id: Int?,
    val centroId: String,
    val trenId: String,
    val lineaId: String,
    val fecha: String,
    val hora: String,
    val tramoMetros: Double,
    val operadorId: String,
    val individuos: Int,
    val calibrePromedioMm: Double?,
    val asistidoPorFoto: Boolean,
    val marcasRegistradas: Int,
    val ajusteManual: Int,
    val observaciones: String
)

/**
 * Muestra que responde el backend. Los campos son opcionales porque Gson
 * puede dejar null aunque el tipo de Kotlin no lo permita.
 */
data class MuestraResponse(
    val id: Int? = null,
    val centroId: String? = null,
    val trenId: String? = null,
    val lineaId: String? = null,
    val fecha: String? = null,
    val hora: String? = null,
    val tramoMetros: Double? = null,
    val operadorId: String? = null,
    val individuos: Int? = null,
    val calibrePromedioMm: Double? = null,
    val asistidoPorFoto: Boolean? = null,
    val marcasRegistradas: Int? = null,
    val ajusteManual: Int? = null,
    val observaciones: String? = null,
    val estado: String? = null,
    val comentarioSupervisor: String? = null,
    val tieneFoto: Boolean? = null,
    val fotoTipo: String? = null,
    val fotoTamanoBytes: Long? = null,
    val creadoEn: String? = null,
    val actualizadoEn: String? = null
)

/** Cuerpo de PATCH /api/muestras/{id}/estado. */
data class CambioEstadoRequest(val estado: String, val comentario: String?)

/** Indicadores de GET /api/reportes/resumen. */
data class ResumenResponse(
    val totalMuestras: Int? = null,
    val totalIndividuos: Long? = null,
    val promedioIndividuos: Double? = null,
    val muestrasPorEstado: Map<String, Int>? = null,
    val muestrasPorCentro: Map<String, Int>? = null
)

/** Formato de todos los errores del backend. */
data class ErrorResponse(
    val status: Int? = null,
    val error: String? = null,
    val mensajes: List<String>? = null,
    val fecha: String? = null
)
