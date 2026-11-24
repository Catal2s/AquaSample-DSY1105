package com.example.aquasample.model

/**
 * Roles del caso ALDEMAR:
 * - OPERADOR registra las muestras en terreno.
 * - SUPERVISOR las revisa (valida u observa).
 * - ANALISTA solo consulta: historial, dashboard y reportes. No crea ni modifica muestras.
 */
enum class Rol(val etiqueta: String) {
    OPERADOR("Operador de muestreo"),
    SUPERVISOR("Supervisor técnico"),
    ANALISTA("Analista de datos")
}
