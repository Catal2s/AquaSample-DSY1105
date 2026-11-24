package com.example.aquasample.viewmodel

import com.example.aquasample.data.CondicionesMarinasRepository
import com.example.aquasample.data.DatosFicticios
import com.example.aquasample.data.ResultadoCondiciones
import com.example.aquasample.model.CondicionesMarinas
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.Muestra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Pruebas del ViewModel con un repositorio falso (sin internet). */
@OptIn(ExperimentalCoroutinesApi::class)
class CondicionesMarinasViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val muestra = DatosFicticios.muestrasEjemplo.first()
    private val condiciones = CondicionesMarinas("2026-09-22", "09:00", -41.95, -73.05, 0.78, 3.1, 9.0, 10.3)

    private class RepositorioFalso(var resultado: ResultadoCondiciones) : CondicionesMarinasRepository {
        var llamadas = 0
        override suspend fun obtener(muestra: Muestra): ResultadoCondiciones {
            llamadas++
            return resultado
        }
    }

    @Before
    fun preparar() = Dispatchers.setMain(dispatcher)

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun exito_pasaDeCargandoAExito() = runTest(dispatcher) {
        val vm = CondicionesMarinasViewModel(RepositorioFalso(ResultadoCondiciones.Exito(condiciones)))
        vm.cargar(muestra)
        assertEquals(CondicionesMarinasUiState.Cargando, vm.uiState.value)
        advanceUntilIdle()
        assertEquals(CondicionesMarinasUiState.Exito(condiciones), vm.uiState.value)
    }

    @Test
    fun error_muestraElMensaje() = runTest(dispatcher) {
        val vm = CondicionesMarinasViewModel(RepositorioFalso(ResultadoCondiciones.Error("falla")))
        vm.cargar(muestra)
        advanceUntilIdle()
        assertEquals(CondicionesMarinasUiState.Error("falla"), vm.uiState.value)
    }

    @Test
    fun sinConexion_yReintentarConExito() = runTest(dispatcher) {
        val repositorio = RepositorioFalso(ResultadoCondiciones.SinConexion)
        val vm = CondicionesMarinasViewModel(repositorio)
        vm.cargar(muestra)
        advanceUntilIdle()
        assertEquals(CondicionesMarinasUiState.SinConexion, vm.uiState.value)

        repositorio.resultado = ResultadoCondiciones.Exito(condiciones)
        vm.reintentar()
        assertEquals(CondicionesMarinasUiState.Cargando, vm.uiState.value)
        advanceUntilIdle()
        assertEquals(CondicionesMarinasUiState.Exito(condiciones), vm.uiState.value)
        assertEquals(2, repositorio.llamadas)
    }

    @Test
    fun noRepiteLaConsultaSiSoloCambiaElEstadoDeRevision() = runTest(dispatcher) {
        val repositorio = RepositorioFalso(ResultadoCondiciones.Exito(condiciones))
        val vm = CondicionesMarinasViewModel(repositorio)
        vm.cargar(muestra)
        advanceUntilIdle()
        vm.cargar(muestra.copy(estado = EstadoRevision.OBSERVADO))
        advanceUntilIdle()
        assertEquals(1, repositorio.llamadas)
    }

    @Test
    fun vuelveAConsultarSiCambiaLaFecha() = runTest(dispatcher) {
        val repositorio = RepositorioFalso(ResultadoCondiciones.Exito(condiciones))
        val vm = CondicionesMarinasViewModel(repositorio)
        vm.cargar(muestra)
        advanceUntilIdle()
        vm.cargar(muestra.copy(fecha = "2026-10-01"))
        advanceUntilIdle()
        assertEquals(2, repositorio.llamadas)
    }

    @Test
    fun reintentarSinMuestraNoHaceNada() = runTest(dispatcher) {
        val repositorio = RepositorioFalso(ResultadoCondiciones.SinConexion)
        CondicionesMarinasViewModel(repositorio).reintentar()
        advanceUntilIdle()
        assertEquals(0, repositorio.llamadas)
    }
}
