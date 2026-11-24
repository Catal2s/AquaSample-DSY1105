package com.example.aquasample.data.sync

import com.example.aquasample.data.DatosFicticios
import com.example.aquasample.data.local.aEntidad
import com.example.aquasample.data.local.aEntidadDeEjemplo
import com.example.aquasample.data.local.aModelo
import com.example.aquasample.data.remote.backend.MuestraResponse
import com.example.aquasample.data.remote.backend.ResumenResponse
import com.example.aquasample.data.remote.backend.aEntidad
import com.example.aquasample.data.remote.backend.aRequest
import com.example.aquasample.model.Conteo
import com.example.aquasample.model.EstadoSincronizacion
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapeoMuestrasTest {

    private val muestra = DatosFicticios.muestrasEjemplo.first()
        .copy(id = 7, conteo = Conteo(312, 38.5, true, 300, 12), fotoUri = "/fotos/a.jpg")

    /** Ejemplo de salida de docs/API_CONTRATO.md. */
    private val json = """
        {"id":7,"centroId":"C1","trenId":"T1","lineaId":"L1","fecha":"2026-11-24","hora":"10:30",
         "tramoMetros":0.2,"operadorId":"U1","individuos":312,"calibrePromedioMm":38.5,"asistidoPorFoto":true,
         "marcasRegistradas":300,"ajusteManual":12,"observaciones":"Muestra sin novedades","estado":"OBSERVADO",
         "comentarioSupervisor":"Revisar","tieneFoto":false,"fotoTipo":null,"fotoTamanoBytes":null,
         "creadoEn":"2026-11-24T10:31:02","actualizadoEn":"2026-11-24T10:31:02"}
    """

    private fun respuesta() = Gson().fromJson(json, MuestraResponse::class.java)

    @Test
    fun entidadARequest_incluyeElConteoAsistido() {
        val request = muestra.aEntidad().aRequest()
        assertEquals(7, request.id)
        assertEquals("L1", request.lineaId)
        assertEquals("U1", request.operadorId)
        assertEquals(0.2, request.tramoMetros, 0.0)
        assertTrue(request.asistidoPorFoto)
        assertEquals(300, request.marcasRegistradas)
        assertEquals(12, request.ajusteManual)
    }

    @Test
    fun respuestaAEntidad_quedaSincronizada() {
        val entidad = respuesta().aEntidad()!!
        assertEquals("OBSERVADO", entidad.estado)
        assertEquals("Revisar", entidad.comentarioSupervisor)
        assertEquals("SINCRONIZADA", entidad.estadoSync)
        assertFalse(entidad.datosPendientes)
        assertTrue(entidad.enServidor)
        assertEquals(312, entidad.aModelo().conteo.individuos)
    }

    @Test
    fun respuestaIncompletaODeEstadoDesconocidoSeDescarta() {
        assertNull(MuestraResponse(id = 1).aEntidad())
        assertNull(respuesta().copy(estado = "OTRO").aEntidad())
    }

    @Test
    fun idsFueraDelCatalogoNoBotanLaApp() {
        val modelo = respuesta().copy(centroId = "C9", trenId = "T9", lineaId = "L9", operadorId = "U9").aEntidad()!!.aModelo()
        assertEquals("C9", modelo.centro.nombre)
        assertEquals("U9", modelo.operador.nombre)
    }

    @Test
    fun guardarYLeerDeRoomConservaLaMuestra() {
        assertEquals(muestra, muestra.aEntidad().aModelo())
    }

    @Test
    fun muestraNuevaPartePendienteYLaDeEjemploSincronizada() {
        val nueva = muestra.aEntidad()
        assertEquals("PENDIENTE", nueva.estadoSync)
        assertTrue(nueva.datosPendientes && nueva.fotoPendiente && !nueva.enServidor)
        assertEquals(EstadoSincronizacion.SINCRONIZADA, muestra.aEntidadDeEjemplo().aModelo().estadoSync)
    }

    @Test
    fun reconoceJpegYPngPorSusPrimerosBytes() {
        fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }
        assertEquals("image/jpeg", tipoDeImagen(bytes(0xFF, 0xD8, 0xFF, 0xE0)))
        assertEquals("image/png", tipoDeImagen(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)))
        assertNull(tipoDeImagen("RIFF".toByteArray()))
        assertNull(tipoDeImagen(ByteArray(0)))
    }

    @Test
    fun leeElResumenDelContrato() {
        val json = """{"totalMuestras":5,"totalIndividuos":1563,"promedioIndividuos":312.6,
            "muestrasPorEstado":{"CORREGIDO":1,"VALIDADO":2},"muestrasPorCentro":{"C1":3,"C2":2}}"""
        val resumen = Gson().fromJson(json, ResumenResponse::class.java)
        assertEquals(5, resumen.totalMuestras)
        assertEquals(1563L, resumen.totalIndividuos)
        assertEquals(312.6, resumen.promedioIndividuos!!, 0.0)
        assertEquals(2, resumen.muestrasPorEstado!!["VALIDADO"])
        assertEquals(3, resumen.muestrasPorCentro!!["C1"])
    }
}
