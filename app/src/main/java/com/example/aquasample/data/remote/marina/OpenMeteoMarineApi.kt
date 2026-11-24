package com.example.aquasample.data.remote.marina

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API pública Open-Meteo Marine (no necesita clave).
 * Documentación: https://open-meteo.com/en/docs/marine-weather-api
 */
interface OpenMeteoMarineApi {

    /** Datos hora por hora entre dos fechas (AAAA-MM-DD). Sirve para fechas pasadas. */
    @GET("v1/marine")
    suspend fun obtenerCondiciones(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double,
        @Query("start_date") fechaInicio: String,
        @Query("end_date") fechaFin: String,
        @Query("hourly") variables: String = VARIABLES,
        @Query("timezone") zonaHoraria: String = ZONA_HORARIA
    ): RespuestaMarina

    companion object {
        /** Para mostrar otra variable hay que agregarla aquí y en [DatosHorarios]. */
        const val VARIABLES = "wave_height,wave_period,wave_direction,sea_surface_temperature"
        const val ZONA_HORARIA = "America/Santiago"
    }
}
