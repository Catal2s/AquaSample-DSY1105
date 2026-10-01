package com.example.aquasample.model

/**
 * Estados de revisión del supervisor (RF09 del caso).
 * OBSERVADO = el supervisor pidió una corrección.
 * VALIDADO  = el supervisor aprobó la muestra.
 */
enum class EstadoRevision(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    OBSERVADO("Observado"),
    CORREGIDO("Corregido"),
    VALIDADO("Validado")
}
