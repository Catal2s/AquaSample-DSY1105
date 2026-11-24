package com.example.aquasample.data

import com.example.aquasample.data.remote.marina.OpenMeteoMarineApi
import com.example.aquasample.data.remote.marina.RespuestaMarina
import com.example.aquasample.model.Centro
import com.example.aquasample.model.Muestra
import com.example.aquasample.model.puntoCardinal
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import com.google.gson.stream.MalformedJsonException
import java.io.EOFException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Pruebas del mapeo del JSON y del manejo de errores (API falsa, sin internet). */
class CondicionesMarinasRepositoryTest {

    private val lugar = Coordenadas(-41.95, -73.05)
    private val muestra = DatosFicticios.muestrasEjemplo.first() // C1, 2026-09-22, 09:15

    /** Mismo formato de la respuesta real de Open-Meteo. */
    private val json = """
        {"latitude":-41.958336,"longitude":-73.04166,"timezone":"America/Santiago",
         "hourly":{"time":["2026-09-22T08:00","2026-09-22T09:00","2026-09-22T10:00","2026-09-22T11:00"],
         "wave_height":[0.70,0.78,null,0.95],
         "wave_period":[3.00,3.10,null,3.40],
         "wave_direction":[5,9,null,20],
         "sea_surface_temperature":[10.2,10.3,null,10.4]}}
    """.trimIndent()

    private fun respuesta(texto: String = json): RespuestaMarina =
        Gson().fromJson(texto, RespuestaMarina::class.java)

    private class ApiFalsa(private val accion: () -> RespuestaMarina) : OpenMeteoMarineApi {
        var latitud = 0.0
        var fecha = ""
        override suspend fun obtenerCondiciones(
            latitud: Double, longitud: Double, fechaInicio: String, fechaFin: String,
            variables: String, zonaHoraria: String
        ): RespuestaMarina {
            this.latitud = latitud
            fecha = fechaInicio
            return accion()
        }
    }

    private suspend fun obtenerCon(m: Muestra = muestra, accion: () -> RespuestaMarina) =
        CondicionesMarinasRepositoryOpenMeteo(ApiFalsa(accion)).obtener(m)

    private fun mensaje(r: ResultadoCondiciones) = (r as ResultadoCondiciones.Error).mensaje

    @Test
    fun eligeLaHoraMasCercana() {
        val c = respuesta().aCondiciones("2026-09-22", "09:15", lugar)!!
        assertEquals("09:00", c.hora)
        assertEquals(0.78, c.alturaOlaM!!, 0.0)
        assertEquals(3.1, c.periodoOlaS!!, 0.0)
        assertEquals(9.0, c.direccionOlaGrados!!, 0.0)
        assertEquals(10.3, c.temperaturaMarC!!, 0.0)
        assertEquals(-41.95, c.latitud, 0.0)
    }

    @Test
    fun enUnEmpateUsaLaHoraAnterior() {
        assertEquals("08:00", respuesta().aCondiciones("2026-09-22", "08:30", lugar)!!.hora)
    }

    @Test
    fun saltaLasHorasSinNingunDato() {
        // Las 10:00 vienen completas en null: la más cercana con datos a las 10:10 es 11:00.
        assertEquals("11:00", respuesta().aCondiciones("2026-09-22", "10:10", lugar)!!.hora)
    }

    @Test
    fun sinHoraValidaUsaElMayorOleajeDelDia() {
        listOf("", "99:99", "hola").forEach {
            assertEquals("11:00", respuesta().aCondiciones("2026-09-22", it, lugar)!!.hora)
        }
    }

    @Test
    fun conservaLosValoresParcialmenteNulos() {
        val parcial = """{"hourly":{"time":["2026-09-22T09:00"],"wave_height":[null],"sea_surface_temperature":[10.5]}}"""
        val c = respuesta(parcial).aCondiciones("2026-09-22", "09:00", lugar)!!
        assertNull(c.alturaOlaM)
        assertNull(c.direccionOlaGrados)
        assertEquals(10.5, c.temperaturaMarC!!, 0.0)
    }

    @Test
    fun respuestaVaciaOTodaNulaDevuelveNull() {
        val vacias = listOf(
            "{}",
            """{"hourly":{}}""",
            """{"hourly":{"time":["2026-09-22T09:00"],"wave_height":[null]}}"""
        )
        vacias.forEach { assertNull(respuesta(it).aCondiciones("2026-09-22", "09:00", lugar)) }
    }

    @Test
    fun exitoConsultaLaUbicacionYFechaDeLaMuestra() = runTest {
        val api = ApiFalsa { respuesta() }
        val r = CondicionesMarinasRepositoryOpenMeteo(api).obtener(muestra)
        assertTrue(r is ResultadoCondiciones.Exito)
        assertEquals(UbicacionesCentros.de("C1")!!.latitud, api.latitud, 0.0)
        assertEquals("2026-09-22", api.fecha)
    }

    @Test
    fun sinInternet() = runTest {
        assertEquals(ResultadoCondiciones.SinConexion, obtenerCon { throw UnknownHostException() })
    }

    @Test
    fun timeoutEsErrorYNoSinConexion() = runTest {
        assertTrue(mensaje(obtenerCon { throw SocketTimeoutException() }).contains("tardó"))
    }

    @Test
    fun fechaFueraDeRango() = runTest {
        val cuerpo = """{"error":true,"reason":"Parameter 'start_date' is out of allowed range from 1940-01-01 to 2026-12-09"}"""
        val r = obtenerCon { throw HttpException(Response.error<RespuestaMarina>(400, cuerpo.toResponseBody())) }
        assertTrue(mensaje(r).contains("fuera del rango"))
    }

    @Test
    fun errorDelServidor() = runTest {
        val r = obtenerCon { throw HttpException(Response.error<RespuestaMarina>(500, "".toResponseBody())) }
        assertTrue(mensaje(r).contains("500"))
    }

    @Test
    fun jsonInvalidoYRespuestaSinDatosSonError() = runTest {
        assertTrue(mensaje(obtenerCon { throw JsonSyntaxException("x") }).contains("no es válida"))
        assertTrue(mensaje(obtenerCon { RespuestaMarina() }).contains("no tiene datos"))
    }

    @Test
    fun centroSinUbicacionEsError() = runTest {
        val r = obtenerCon(muestra.copy(centro = Centro("C9", "Otro"))) { respuesta() }
        assertTrue(mensaje(r).contains("ubicación"))
    }

    @Test
    fun todosLosCentrosTienenUbicacion() {
        DatosFicticios.centros.forEach { assertTrue(UbicacionesCentros.de(it.id) != null) }
    }

    @Test
    fun puntosCardinales() {
        assertEquals("N", puntoCardinal(0.0))
        assertEquals("N", puntoCardinal(350.0))
        assertEquals("N", puntoCardinal(360.0))
        assertEquals("NE", puntoCardinal(45.0))
        assertEquals("SO", puntoCardinal(225.0))
        assertEquals("NO", puntoCardinal(315.0))
    }

    @Test
    fun respuestaCortadaOMalFormadaEsErrorYNoSinConexion() = runTest {
        assertTrue(mensaje(obtenerCon { throw MalformedJsonException("x") }).contains("no es válida"))
        assertTrue(mensaje(obtenerCon { throw EOFException() }).contains("no es válida"))
    }
}
