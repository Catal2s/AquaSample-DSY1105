package com.example.aquasample.data.sync

import com.example.aquasample.data.DatosFicticios
import com.example.aquasample.data.local.MuestraEntity
import com.example.aquasample.data.local.aEntidad
import com.example.aquasample.data.local.aEntidadDeEjemplo
import com.example.aquasample.data.remote.backend.aRequest
import com.example.aquasample.model.Rol
import com.example.aquasample.viewmodel.aMensaje
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sincronización con Room y backend falsos, sin internet. */
class SincronizadorMuestrasTest {

    private val api = FakeAquaSampleApi()
    private val fotos = FakeLectorFotos()

    /** Muestra #id del operador U1, creada en el teléfono y sin enviar. */
    private fun nueva(id: Int, foto: String? = null): MuestraEntity =
        DatosFicticios.muestrasEjemplo.first { it.id == 5 }.copy(id = id, fotoUri = foto).aEntidad()

    private fun sincronizada(id: Int): MuestraEntity =
        DatosFicticios.muestrasEjemplo.first { it.id == 5 }.copy(id = id).aEntidadDeEjemplo()

    private fun enServidor(entidad: MuestraEntity, estado: String = "PENDIENTE") {
        api.guardadas[entidad.id] = entidad.aRequest().aRespuesta(estado)
    }

    private fun sincronizador(dao: FakeMuestraSyncDao) = SincronizadorMuestras(dao, api, fotos)

    // ---------- Éxito ----------

    @Test
    fun exito_enviaDatosLuegoFotoYLuegoEstadosConSuRol() = runTest {
        val dao = FakeMuestraSyncDao(nueva(6, foto = "/f.jpg"))
        val sync = sincronizador(dao)
        sync.cambiarEstadoLocal(6, "OBSERVADO", "Revisar", Rol.SUPERVISOR)
        sync.cambiarEstadoLocal(6, "CORREGIDO", "Revisar", Rol.OPERADOR)
        sync.cambiarEstadoLocal(6, "VALIDADO", null, Rol.SUPERVISOR)

        val resultado = sync.subir()

        assertEquals(
            listOf(
                "PUT 6 OPERADOR", "FOTO 6 OPERADOR", "PATCH 6 OBSERVADO SUPERVISOR",
                "PATCH 6 CORREGIDO OPERADOR", "PATCH 6 VALIDADO SUPERVISOR"
            ),
            api.llamadas
        )
        assertEquals(ResultadoSync(enviadas = 1), resultado)
        assertTrue(resultado.completa)
        assertEquals("SINCRONIZADA", dao.filas[6]!!.estadoSync)
        assertTrue(dao.cambios.isEmpty())
        assertEquals("VALIDADO", api.guardadas[6]!!.estado)
    }

    @Test
    fun cambioDeEstadoDeUnaMuestraYaEnviada_soloHacePatch() = runTest {
        enServidor(sincronizada(5))
        val dao = FakeMuestraSyncDao(sincronizada(5))
        val sync = sincronizador(dao)
        sync.cambiarEstadoLocal(5, "VALIDADO", null, Rol.SUPERVISOR)
        assertEquals("PENDIENTE", dao.filas[5]!!.estadoSync)
        sync.subir()
        assertEquals(listOf("PATCH 5 VALIDADO SUPERVISOR"), api.llamadas)
    }

    @Test
    fun sinNadaPendienteNoLlamaAlServidor() = runTest {
        assertTrue(sincronizador(FakeMuestraSyncDao(sincronizada(1))).subir().completa)
        assertTrue(api.llamadas.isEmpty())
    }

    // ---------- Sin conexión y reintentos ----------

    @Test
    fun sinConexion_quedaPendienteYDespuesSubeUnaSolaVez() = runTest {
        val dao = FakeMuestraSyncDao(nueva(6), nueva(7))
        val sync = sincronizador(dao)
        api.sinInternet = true

        val fallido = sync.subir()
        assertTrue(fallido.sinConexion)
        assertFalse(fallido.completa)
        assertEquals("PENDIENTE", dao.filas[6]!!.estadoSync)
        assertEquals(1, api.llamadas.size) // No insiste con la segunda.

        api.sinInternet = false
        assertEquals(2, sync.subir().enviadas)
        sync.subir()
        assertEquals(setOf(6, 7), api.guardadas.keys)
        assertEquals(1, api.llamadas.count { it.startsWith("PUT 7") })
    }

    @Test
    fun siSePierdeLaRespuestaDelPut_reintentarNoDuplica() = runTest {
        val dao = FakeMuestraSyncDao(nueva(6))
        val sync = sincronizador(dao)
        api.perderRespuestaDelPut = true
        assertTrue(sync.subir().sinConexion)
        assertEquals(1, api.guardadas.size)

        api.perderRespuestaDelPut = false
        assertEquals(1, sync.subir().enviadas)
        assertEquals(1, api.guardadas.size)
        assertEquals("SINCRONIZADA", dao.filas[6]!!.estadoSync)
    }

    @Test
    fun dosSincronizacionesALaVezEnvianUnaSolaVez() = runTest {
        val sync = sincronizador(FakeMuestraSyncDao(nueva(6)))
        listOf(async { sync.subir() }, async { sync.subir() }).awaitAll()
        assertEquals(listOf("PUT 6 OPERADOR"), api.llamadas)
    }

    @Test
    fun error500_sigueConLasDemasYQuedaParaReintentar() = runTest {
        val dao = FakeMuestraSyncDao(nueva(6), nueva(7))
        api.idConError500 = 6
        val resultado = sincronizador(dao).subir()
        assertFalse(resultado.completa)
        assertEquals(1, resultado.porReintentar)
        assertEquals(1, resultado.enviadas)
        assertEquals("PENDIENTE", dao.filas[6]!!.estadoSync)
        assertEquals("SINCRONIZADA", dao.filas[7]!!.estadoSync)
    }

    // ---------- Los 409 ----------

    @Test
    fun conflicto409_muestraValidadaConOtrosDatos_quedaEnErrorYNoSeReintenta() = runTest {
        enServidor(nueva(6).copy(individuos = 999), "VALIDADO")
        val dao = FakeMuestraSyncDao(nueva(6))
        val sync = sincronizador(dao)

        val resultado = sync.subir()
        assertTrue(resultado.completa)
        assertEquals(listOf("#6: La muestra 6 ya fue validada y no se puede editar"), resultado.errores)
        assertEquals("ERROR", dao.filas[6]!!.estadoSync)

        val llamadas = api.llamadas.size
        sync.subir()
        assertEquals(llamadas, api.llamadas.size)
    }

    @Test
    fun conflicto409_validadaConLosMismosDatos_yaHabiaLlegado() = runTest {
        enServidor(nueva(6), "VALIDADO")
        val dao = FakeMuestraSyncDao(nueva(6))
        assertEquals(ResultadoSync(enviadas = 1), sincronizador(dao).subir())
        assertEquals("SINCRONIZADA", dao.filas[6]!!.estadoSync)
    }

    @Test
    fun conflicto409_deOtroOperador_conservaAmbasMuestras() = runTest {
        enServidor(nueva(6).copy(operadorId = "U2", individuos = 111))
        val dao = FakeMuestraSyncDao(nueva(6))
        val resultado = sincronizador(dao).subirYBajar()

        assertTrue(resultado.errores.single().contains("otro operador"))
        assertEquals("ERROR", dao.filas[6]!!.estadoSync)
        assertEquals("U1", dao.filas[6]!!.operadorId) // La bajada no pisa la local.
        assertEquals("U2", api.guardadas[6]!!.operadorId)
    }

    @Test
    fun conflicto409_deEstado_mandaElServidor() = runTest {
        enServidor(sincronizada(5), "VALIDADO")
        val dao = FakeMuestraSyncDao(sincronizada(5))
        val sync = sincronizador(dao)
        sync.cambiarEstadoLocal(5, "OBSERVADO", "Tarde", Rol.SUPERVISOR)

        val resultado = sync.subirYBajar()
        assertEquals(1, resultado.errores.size)
        assertTrue(dao.cambios.isEmpty())
        assertEquals("VALIDADO", dao.filas[5]!!.estado)
        assertEquals("SINCRONIZADA", dao.filas[5]!!.estadoSync)
    }

    @Test
    fun error400_quedaEnErrorConElMensajeDelServidor() = runTest {
        api.codigoForzado = 400
        val dao = FakeMuestraSyncDao(nueva(6))
        assertTrue(sincronizador(dao).subir().completa)
        assertEquals("forzado", dao.filas[6]!!.mensajeSync)
    }

    // ---------- Foto ----------

    @Test
    fun fotoIlegible_igualEnviaLosDatosYElEstado() = runTest {
        val dao = FakeMuestraSyncDao(nueva(6, foto = "rota"))
        val sync = sincronizador(dao)
        sync.cambiarEstadoLocal(6, "VALIDADO", null, Rol.SUPERVISOR)
        val resultado = sync.subir()
        assertEquals(listOf("#6: No se pudo leer la foto"), resultado.errores)
        assertEquals("VALIDADO", api.guardadas[6]!!.estado)
        assertFalse(dao.filas[6]!!.datosPendientes)
    }

    // ---------- Eliminar ----------

    @Test
    fun eliminarSinConexion_seOcultaYSeBorraDespues() = runTest {
        enServidor(sincronizada(5))
        val dao = FakeMuestraSyncDao(sincronizada(5).copy(fotoUri = "/f.jpg"))
        val sync = sincronizador(dao)
        api.sinInternet = true
        sync.eliminarLocal(5, Rol.SUPERVISOR)
        sync.subir()
        assertEquals("SUPERVISOR", dao.filas[5]!!.eliminadoPor)

        api.sinInternet = false
        sync.subirYBajar()
        assertTrue(api.llamadas.contains("DELETE 5 SUPERVISOR"))
        assertNull(dao.filas[5])
        assertTrue(api.guardadas.isEmpty())
        assertEquals(listOf("/f.jpg"), fotos.borradas)
    }

    @Test
    fun eliminarAlgoQueYaNoExisteEnElServidorNoEsError() = runTest {
        val dao = FakeMuestraSyncDao(sincronizada(5))
        val sync = sincronizador(dao)
        sync.eliminarLocal(5, Rol.SUPERVISOR)
        assertEquals(ResultadoSync(enviadas = 1), sync.subir())
        assertNull(dao.filas[5])
    }

    @Test
    fun eliminarUnaQueNuncaLlegoAlServidor_noEnviaDelete() = runTest {
        val dao = FakeMuestraSyncDao(nueva(6))
        val sync = sincronizador(dao)
        sync.eliminarLocal(6, Rol.SUPERVISOR)
        sync.subir()
        assertNull(dao.filas[6])
        assertTrue(api.llamadas.isEmpty())
    }

    // ---------- Bajada ----------

    @Test
    fun bajar_agregaYActualizaSinPisarPendientes() = runTest {
        enServidor(nueva(5).copy(individuos = 500), "VALIDADO")
        enServidor(nueva(6).copy(individuos = 600))
        enServidor(nueva(7))
        val dao = FakeMuestraSyncDao(sincronizada(5).copy(fotoUri = "/f.jpg"), nueva(6))

        val resultado = sincronizador(dao).bajar()

        assertEquals(2, resultado.bajadas)
        assertEquals(500, dao.filas[5]!!.individuos)
        assertEquals("/f.jpg", dao.filas[5]!!.fotoUri)
        assertEquals(298, dao.filas[6]!!.individuos)
        assertEquals("SINCRONIZADA", dao.filas[7]!!.estadoSync)
    }

    @Test
    fun bajar_borraLasSincronizadasQueYaNoExistenPeroNoLasPendientes() = runTest {
        enServidor(sincronizada(1))
        val dao = FakeMuestraSyncDao(
            sincronizada(1), sincronizada(2), nueva(6), nueva(7).copy(estadoSync = "ERROR")
        )
        sincronizador(dao).bajar()
        assertEquals(setOf(1, 6, 7), dao.filas.keys)
    }

    @Test
    fun bajar_conServidorVacioNoBorraNada() = runTest {
        val dao = FakeMuestraSyncDao(sincronizada(1))
        sincronizador(dao).bajar()
        assertEquals(setOf(1), dao.filas.keys)
    }

    @Test
    fun mensajesParaElUsuario() {
        assertTrue(ResultadoSync(sinConexion = true).aMensaje().startsWith("Sin conexión"))
        assertTrue(ResultadoSync(errores = listOf("#6: x")).aMensaje().contains("#6: x"))
        assertTrue(ResultadoSync(porReintentar = 1).aMensaje().contains("falló"))
        assertEquals("Todo está sincronizado", ResultadoSync().aMensaje())
        assertEquals("Enviadas: 1 · Recibidas del servidor: 2", ResultadoSync(1, bajadas = 2).aMensaje())
    }

    // ---------- Fallas en cada paso del envío ----------

    @Test
    fun fotoCon500_conservaLaFotoPendienteYNoRepiteElPut() = runTest {
        val dao = FakeMuestraSyncDao(nueva(6, foto = "/f.jpg"))
        val sync = sincronizador(dao)
        api.fallar = { if (it.startsWith("FOTO")) 500 else null }
        assertFalse(sync.subir().completa)
        assertFalse(dao.filas[6]!!.datosPendientes)
        assertTrue(dao.filas[6]!!.fotoPendiente)

        api.fallar = { null }
        assertEquals(1, sync.subir().enviadas)
        assertEquals(1, api.llamadas.count { it.startsWith("PUT") })
        assertEquals(true, api.guardadas[6]!!.tieneFoto)
    }

    @Test
    fun fotoRechazada_informaElMotivo() = runTest {
        val dao = FakeMuestraSyncDao(nueva(6, foto = "/f.jpg"))
        api.fallar = { if (it.startsWith("FOTO")) 413 else null }
        assertEquals(listOf("#6: Foto no subida: falla 413"), sincronizador(dao).subir().errores)
        assertEquals("ERROR", dao.filas[6]!!.estadoSync)
    }

    @Test
    fun foto409_siElServidorYaLaTieneNoEsError() = runTest {
        enServidor(nueva(6), "VALIDADO")
        api.guardadas[6] = api.guardadas[6]!!.copy(tieneFoto = true)
        val dao = FakeMuestraSyncDao(nueva(6, foto = "/f.jpg"))
        api.fallar = { if (it.startsWith("FOTO")) 409 else null }
        assertEquals(ResultadoSync(enviadas = 1), sincronizador(dao).subir())
    }

    @Test
    fun patchCon500_conservaElCambioParaReintentar() = runTest {
        enServidor(sincronizada(5))
        val dao = FakeMuestraSyncDao(sincronizada(5))
        val sync = sincronizador(dao)
        sync.cambiarEstadoLocal(5, "VALIDADO", null, Rol.SUPERVISOR)
        api.fallar = { if (it.startsWith("PATCH")) 503 else null }
        assertEquals(1, sync.subir().porReintentar)
        assertEquals(1, dao.cambios.size)
        assertEquals("PENDIENTE", dao.filas[5]!!.estadoSync)
    }

    @Test
    fun conflictoCuandoElServidorNoPuedeResponder_seReintenta() = runTest {
        enServidor(nueva(6).copy(individuos = 999), "VALIDADO")
        val dao = FakeMuestraSyncDao(nueva(6))
        api.fallar = { if (it == "GET 6") 500 else null }
        assertEquals(1, sincronizador(dao).subir().porReintentar)
        assertEquals("PENDIENTE", dao.filas[6]!!.estadoSync)
    }

    @Test
    fun eliminarCon500_sigueOculta_yCon403VuelveAMostrarse() = runTest {
        enServidor(sincronizada(5))
        val dao = FakeMuestraSyncDao(sincronizada(5))
        val sync = sincronizador(dao)
        sync.eliminarLocal(5, Rol.SUPERVISOR)

        api.fallar = { 500 }
        assertEquals(1, sync.subir().porReintentar)
        assertEquals("SUPERVISOR", dao.filas[5]!!.eliminadoPor)

        api.fallar = { 403 }
        assertEquals(1, sync.subir().errores.size)
        assertNull(dao.filas[5]!!.eliminadoPor)
        assertEquals("ERROR", dao.filas[5]!!.estadoSync)
    }

    @Test
    fun eliminarUnIdQueNoExisteNoHaceNada() = runTest {
        sincronizador(FakeMuestraSyncDao()).eliminarLocal(99, Rol.SUPERVISOR)
        assertTrue(api.llamadas.isEmpty())
    }

    @Test
    fun respuestaQueNoEsJson_quedaParaReintentar() = runTest {
        val dao = FakeMuestraSyncDao(nueva(6))
        api.excepcion = JsonSyntaxException("<html>")
        val resultado = sincronizador(dao).subirYBajar()
        assertEquals(2, resultado.porReintentar) // La subida y la bajada.
        assertEquals("PENDIENTE", dao.filas[6]!!.estadoSync)
    }

    @Test
    fun bajar_conError500OSinInternetNoTocaRoom() = runTest {
        val dao = FakeMuestraSyncDao(sincronizada(1))
        val sync = sincronizador(dao)
        api.fallar = { 500 }
        assertEquals(1, sync.bajar().porReintentar)
        api.sinInternet = true
        assertTrue(sync.bajar().sinConexion)
        assertEquals(setOf(1), dao.filas.keys)
    }
}
