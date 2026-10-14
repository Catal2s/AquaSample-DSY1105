package com.example.aquasample.model

/**
 * Roles de la app. Para el MVP basta con distinguir
 * a quien registra la muestra de quien la revisa.
 */
enum class Rol(val etiqueta: String) {
    OPERADOR("Operador de muestreo"),
    SUPERVISOR("Supervisor técnico")
}
