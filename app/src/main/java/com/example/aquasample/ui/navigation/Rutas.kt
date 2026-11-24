package com.example.aquasample.ui.navigation

import com.example.aquasample.model.Rol

/**
 * Rutas del NavHost.
 * Operador: Login → Inicio → Nueva muestra → Conteo → Resumen.
 * Supervisor: Login → Bandeja de revisión.
 * Ambos: Historial y Detalle de una muestra.
 */
object Rutas {
    const val LOGIN = "login"
    const val INICIO = "inicio"
    const val NUEVA_MUESTRA = "nueva_muestra"
    const val CONTEO = "conteo"
    const val RESUMEN = "resumen"
    const val HISTORIAL = "historial"
    const val BANDEJA = "bandeja"

    /** El detalle recibe el id de la muestra en la ruta, por ejemplo "detalle/3". */
    const val ARG_ID_MUESTRA = "idMuestra"
    const val DETALLE = "detalle/{$ARG_ID_MUESTRA}"

    fun detalle(idMuestra: Int) = "detalle/$idMuestra"

    /** Pantalla de inicio según el rol: el supervisor parte en su bandeja de revisión. */
    fun inicioDe(rol: Rol?) = if (rol == Rol.SUPERVISOR) BANDEJA else INICIO

    /** Pantallas que forman parte del registro de una muestra. */
    val FLUJO_MUESTRA = setOf(NUEVA_MUESTRA, CONTEO, RESUMEN)
}
