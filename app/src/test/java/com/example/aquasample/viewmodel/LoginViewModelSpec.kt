package com.example.aquasample.viewmodel

import com.example.aquasample.model.Rol
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Pruebas del login con usuarios ficticios.
 * Verifican las validaciones (campos vacíos, clave incorrecta) y que el rol se detecte bien.
 */
class LoginViewModelSpec : StringSpec({

    "con usuario o clave vacíos no inicia sesión y muestra un error" {
        val viewModel = LoginViewModel()

        viewModel.iniciarSesion().shouldBeFalse()

        viewModel.uiState.value.error shouldBe "Ingresa usuario y contraseña"
        viewModel.uiState.value.sesionIniciada.shouldBeFalse()
    }

    "con clave incorrecta no inicia sesión" {
        val viewModel = LoginViewModel()
        viewModel.onUsuarioChange("operador1")
        viewModel.onClaveChange("9999")

        viewModel.iniciarSesion().shouldBeFalse()

        viewModel.uiState.value.error shouldBe "Usuario o contraseña incorrectos"
        viewModel.uiState.value.usuarioActual.shouldBeNull()
    }

    "con datos correctos inicia sesión, detecta el rol y borra la clave" {
        val viewModel = LoginViewModel()
        // Mayúsculas y espacios no importan en el nombre de usuario
        viewModel.onUsuarioChange("  Supervisor1 ")
        viewModel.onClaveChange("1234")

        viewModel.iniciarSesion().shouldBeTrue()

        val estado = viewModel.uiState.value
        estado.sesionIniciada.shouldBeTrue()
        estado.usuarioActual?.rol shouldBe Rol.SUPERVISOR
        estado.clave shouldBe ""
        estado.error.shouldBeNull()
    }

    "al escribir de nuevo se borra el mensaje de error" {
        val viewModel = LoginViewModel()
        viewModel.iniciarSesion()

        viewModel.onUsuarioChange("o")
        viewModel.uiState.value.error.shouldBeNull()

        viewModel.iniciarSesion()
        viewModel.onClaveChange("1")
        viewModel.uiState.value.error.shouldBeNull()
    }

    "cerrar sesión deja el estado como al inicio" {
        val viewModel = LoginViewModel()
        viewModel.onUsuarioChange("operador1")
        viewModel.onClaveChange("1234")
        viewModel.iniciarSesion()

        viewModel.cerrarSesion()

        viewModel.uiState.value shouldBe LoginUiState()
    }
})
