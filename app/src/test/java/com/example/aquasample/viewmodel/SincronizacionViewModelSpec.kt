package com.example.aquasample.viewmodel

import com.example.aquasample.data.MuestraRepository
import com.example.aquasample.data.sync.ResultadoSync
import com.example.aquasample.model.Rol
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SincronizacionViewModelSpec : StringSpec({

    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    "mientras sincroniza queda en curso y no envía dos veces" {
        val respuesta = CompletableDeferred<ResultadoSync>()
        val repositorio = mockk<MuestraRepository>()
        coEvery { repositorio.sincronizarTodo() } coAnswers { respuesta.await() }
        val vm = SincronizacionViewModel(repositorio)

        vm.sincronizar()
        vm.sincronizar()
        vm.uiState.value shouldBe SincronizacionUiState(enCurso = true)
        coVerify(exactly = 1) { repositorio.sincronizarTodo() }

        respuesta.complete(ResultadoSync(enviadas = 2, bajadas = 1))
        vm.uiState.value shouldBe SincronizacionUiState(mensaje = "Enviadas: 2 · Recibidas del servidor: 1")
    }

    "sin conexión avisa que los cambios quedan guardados" {
        val repositorio = mockk<MuestraRepository>()
        coEvery { repositorio.sincronizarTodo() } returns ResultadoSync(sinConexion = true)
        val vm = SincronizacionViewModel(repositorio)
        vm.sincronizar()
        vm.uiState.value.mensaje shouldBe "Sin conexión con el servidor. Los cambios quedan guardados en el teléfono."
    }

    "el mensaje se limpia después de mostrarlo y se puede sincronizar otra vez" {
        val repositorio = mockk<MuestraRepository>()
        coEvery { repositorio.sincronizarTodo() } returns ResultadoSync()
        val vm = SincronizacionViewModel(repositorio)
        vm.sincronizar()
        vm.mensajeMostrado()
        vm.uiState.value shouldBe SincronizacionUiState()
        vm.sincronizar()
        coVerify(exactly = 2) { repositorio.sincronizarTodo() }
    }

    "solo el supervisor puede eliminar una muestra" {
        val repositorio = mockk<MuestraRepository>(relaxed = true)
        val vm = MuestraViewModel(repositorio)
        vm.eliminar(5, Rol.OPERADOR)
        verify(exactly = 0) { repositorio.eliminar(any(), any()) }
        vm.eliminar(5, Rol.SUPERVISOR)
        verify(exactly = 1) { repositorio.eliminar(5, Rol.SUPERVISOR) }
    }
})
