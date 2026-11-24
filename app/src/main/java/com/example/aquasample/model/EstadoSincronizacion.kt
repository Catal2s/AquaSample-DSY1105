package com.example.aquasample.model

/** Indica si los cambios locales de una muestra ya llegaron al backend. */
enum class EstadoSincronizacion(val etiqueta: String) {
    SINCRONIZADA("Sincronizada"),
    PENDIENTE("Pendiente de sincronización"),
    /** El servidor rechazó el envío: reintentar igual no sirve. */
    ERROR("Error de sincronización")
}
