package com.example.aquasample.data.remote.marina

import com.google.gson.annotations.SerializedName

/**
 * JSON que devuelve Open-Meteo Marine (solo lo que usa la app).
 * Todo es nullable porque Gson deja en null lo que no viene en la respuesta.
 */
data class RespuestaMarina(
    @SerializedName("hourly") val horario: DatosHorarios? = null
)

/**
 * Listas paralelas: el dato de la posición i corresponde a horas[i] ("2026-09-22T09:00").
 * Un valor puede venir en null si el modelo no tiene dato para esa hora.
 */
data class DatosHorarios(
    @SerializedName("time") val horas: List<String>? = null,
    @SerializedName("wave_height") val alturaOla: List<Double?>? = null,
    @SerializedName("wave_period") val periodoOla: List<Double?>? = null,
    @SerializedName("wave_direction") val direccionOla: List<Double?>? = null,
    @SerializedName("sea_surface_temperature") val temperaturaMar: List<Double?>? = null
)
