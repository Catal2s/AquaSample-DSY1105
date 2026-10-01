package com.example.aquasample.viewmodel

import androidx.lifecycle.ViewModel
import com.example.aquasample.data.DatosFicticios
import com.example.aquasample.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Estado de la pantalla de Login. */
data class LoginUiState(
    val usuario: String = "",
    val clave: String = "",
    val error: String? = null,
    val usuarioActual: Usuario? = null
) {
    val sesionIniciada: Boolean get() = usuarioActual != null
}

/**
 * Login con usuarios ficticios (no es autenticación real).
 * Usuarios de prueba: operador1 / 1234, operador2 / 1234, supervisor1 / 1234.
 */
class LoginViewModel(
    private val usuarios: List<Usuario> = DatosFicticios.usuarios
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsuarioChange(valor: String) {
        _uiState.update { it.copy(usuario = valor, error = null) }
    }

    fun onClaveChange(valor: String) {
        _uiState.update { it.copy(clave = valor, error = null) }
    }

    /**
     * Devuelve true si el login fue correcto, para que la pantalla
     * pueda navegar a Inicio justo después.
     */
    fun iniciarSesion(): Boolean {
        val estado = _uiState.value
        if (estado.usuario.isBlank() || estado.clave.isBlank()) {
            _uiState.update { it.copy(error = "Ingresa usuario y contraseña") }
            return false
        }
        val encontrado = usuarios.find {
            it.usuario.equals(estado.usuario.trim(), ignoreCase = true) && it.clave == estado.clave
        }
        if (encontrado == null) {
            _uiState.update { it.copy(error = "Usuario o contraseña incorrectos") }
            return false
        }
        _uiState.update { it.copy(usuarioActual = encontrado, clave = "", error = null) }
        return true
    }

    fun cerrarSesion() {
        _uiState.value = LoginUiState()
    }
}
