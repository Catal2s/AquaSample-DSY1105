package com.example.aquasample.data.remote.backend

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Microservicio propio (ver docs/API_CONTRATO.md).
 * Devuelve Response para poder revisar el código HTTP (409, 404, etc.).
 */
interface AquaSampleApi {

    @GET("api/muestras")
    suspend fun listar(
        @Query("centroId") centroId: String? = null,
        @Query("trenId") trenId: String? = null,
        @Query("lineaId") lineaId: String? = null,
        @Query("estado") estado: String? = null,
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null
    ): Response<List<MuestraResponse>>

    @GET("api/muestras/{id}")
    suspend fun obtener(@Path("id") id: Int): Response<MuestraResponse>

    @POST("api/muestras")
    suspend fun crear(@Header("X-Rol") rol: String, @Body datos: MuestraRequest): Response<MuestraResponse>

    /** Crea o actualiza con el id de la app: reintentar no duplica. */
    @PUT("api/muestras/{id}")
    suspend fun guardar(
        @Path("id") id: Int,
        @Header("X-Rol") rol: String,
        @Body datos: MuestraRequest
    ): Response<MuestraResponse>

    @PATCH("api/muestras/{id}/estado")
    suspend fun cambiarEstado(
        @Path("id") id: Int,
        @Header("X-Rol") rol: String,
        @Body cambio: CambioEstadoRequest
    ): Response<MuestraResponse>

    @DELETE("api/muestras/{id}")
    suspend fun eliminar(@Path("id") id: Int, @Header("X-Rol") rol: String): Response<Unit>

    /** La parte debe llamarse "archivo". */
    @Multipart
    @POST("api/muestras/{id}/foto")
    suspend fun subirFoto(
        @Path("id") id: Int,
        @Header("X-Rol") rol: String,
        @Part archivo: MultipartBody.Part
    ): Response<MuestraResponse>

    @GET("api/reportes/resumen")
    suspend fun resumen(): Response<ResumenResponse>
}
