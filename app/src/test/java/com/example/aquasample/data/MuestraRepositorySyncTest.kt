package com.example.aquasample.data

import com.example.aquasample.data.local.MuestraDao
import com.example.aquasample.data.local.MuestraEntity
import com.example.aquasample.data.local.aEntidad
import com.example.aquasample.data.local.aEntidadDeEjemplo
import com.example.aquasample.data.remote.backend.MuestraResponse
import com.example.aquasample.data.sync.FakeAquaSampleApi
import com.example.aquasample.data.sync.FakeLectorFotos
import com.example.aquasample.data.sync.FakeMuestraSyncDao
import com.example.aquasample.data.sync.ResultadoSync
import com.example.aquasample.data.sync.SincronizadorMuestras
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.EstadoSincronizacion
import com.example.aquasample.model.Rol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** El repositorio guarda primero en Room y después sincroniza con el backend (ambos falsos). */
@OptIn(ExperimentalCoroutinesApi::class)
class MuestraRepositorySyncTest {

    private val api = FakeAquaSampleApi()
    private val ejemplo = DatosFicticios.muestrasEjemplo.first { it.id == 5 }

    /** MuestraDao que comparte las filas con el DAO de sincronización. */
    private class DaoCompartido(private val sync: FakeMuestraSyncDao) : MuestraDao {
        val flujo = MutableStateFlow(sync.filas.values.toList())
        fun refrescar() { flujo.value = sync.filas.values.toList() }
        override fun observarTodas() = flujo
        override suspend fun buscarPorId(id: Int) = sync.filas[id]
        override suspend fun contar() = sync.filas.size
        override suspend fun insertar(muestra: MuestraEntity) { sync.filas[muestra.id] = muestra; refrescar() }
        override suspend fun insertarTodas(muestras: List<MuestraEntity>) = muestras.forEach { insertar(it) }
        override suspend fun actualizarEstado(id: Int, estado: String, comentario: String?) = error("no se usa con sincronizador")
    }

    private inner class Caso(alcance: CoroutineScope, vararg filas: MuestraEntity) {
        val sync = FakeMuestraSyncDao(*filas)
        val dao = DaoCompartido(sync)
        val repositorio = MuestraRepository(dao, alcance, SincronizadorMuestras(sync, api, FakeLectorFotos()))
    }

    private fun TestScope.caso(vararg filas: MuestraEntity) =
        Caso(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)), *filas)

    @Test
    fun guardar_dejaLaMuestraEnRoomYLaEnvia() = runTest {
        val c = caso()
        c.repositorio.guardar(ejemplo.copy(id = 6))
        advanceUntilIdle()
        assertEquals(listOf("PUT 6 OPERADOR"), api.llamadas)
        assertEquals("SINCRONIZADA", c.sync.filas[6]!!.estadoSync)
    }

    @Test
    fun guardarSinInternet_quedaPendienteYSincronizarPendientesLaEnvia() = runTest {
        val c = caso()
        api.sinInternet = true
        c.repositorio.guardar(ejemplo.copy(id = 6))
        advanceUntilIdle()
        assertEquals(EstadoSincronizacion.PENDIENTE, c.repositorio.muestras.value.single().estadoSync)
        assertFalse(c.repositorio.sincronizarPendientes())

        api.sinInternet = false
        assertTrue(c.repositorio.sincronizarPendientes())
        assertEquals(setOf(6), api.guardadas.keys)
    }

    @Test
    fun actualizarEstado_enviaElRolDeQuienLoHizo() = runTest {
        val fila = ejemplo.aEntidadDeEjemplo()
        val c = caso(fila)
        api.guardadas[5] = MuestraResponse(id = 5, estado = "PENDIENTE")
        c.repositorio.actualizarEstado(5, EstadoRevision.OBSERVADO, "Revisar")
        c.repositorio.actualizarEstado(5, EstadoRevision.CORREGIDO)
        c.repositorio.actualizarEstado(5, EstadoRevision.VALIDADO, rol = Rol.SUPERVISOR)
        advanceUntilIdle()
        assertEquals(
            listOf("PATCH 5 OBSERVADO SUPERVISOR", "PATCH 5 CORREGIDO OPERADOR", "PATCH 5 VALIDADO SUPERVISOR"),
            api.llamadas
        )
    }

    @Test
    fun eliminarSinInternet_ocultaLaMuestraYNoReutilizaSuId() = runTest {
        val c = caso(ejemplo.aEntidadDeEjemplo())
        api.sinInternet = true
        c.repositorio.eliminar(5, Rol.SUPERVISOR)
        advanceUntilIdle()
        c.dao.refrescar()
        advanceUntilIdle()
        assertTrue(c.repositorio.muestras.value.isEmpty())
        assertEquals(6, c.repositorio.siguienteId())
        assertEquals("SUPERVISOR", c.sync.filas[5]!!.eliminadoPor)
    }

    @Test
    fun sincronizarTodo_subeYDespuesBaja() = runTest {
        val c = caso(ejemplo.copy(id = 6).aEntidad())
        val resultado = c.repositorio.sincronizarTodo()
        assertEquals(1, resultado.enviadas)
        assertEquals(listOf("PUT 6 OPERADOR", "GET"), api.llamadas)
    }

    @Test
    fun descargarDelServidor_soloBaja() = runTest {
        val c = caso(ejemplo.copy(id = 6).aEntidad())
        c.repositorio.descargarDelServidor()
        assertEquals(listOf("GET"), api.llamadas)
        assertEquals("PENDIENTE", c.sync.filas[6]!!.estadoSync)
    }

    @Test
    fun sinSincronizador_noHayNadaQueEnviar() = runTest {
        val repositorio = MuestraRepository(DaoCompartido(FakeMuestraSyncDao()), backgroundScope)
        assertTrue(repositorio.sincronizarPendientes())
        assertEquals(ResultadoSync(), repositorio.sincronizarTodo())
        assertEquals(ResultadoSync(), repositorio.descargarDelServidor())
        assertNull(repositorio.muestras.value.firstOrNull())
    }
}
