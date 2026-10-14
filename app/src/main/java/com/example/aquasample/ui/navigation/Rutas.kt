package com.example.aquasample.ui.navigation

/** Rutas del NavHost. Flujo: Login → Inicio → Nueva muestra → Conteo → Resumen. */
object Rutas {
    const val LOGIN = "login"
    const val INICIO = "inicio"
    const val NUEVA_MUESTRA = "nueva_muestra"
    const val CONTEO = "conteo"
    const val RESUMEN = "resumen"
    const val HISTORIAL = "historial"

    /** Pantallas que forman parte del registro de una muestra. */
    val FLUJO_MUESTRA = setOf(NUEVA_MUESTRA, CONTEO, RESUMEN)
}
