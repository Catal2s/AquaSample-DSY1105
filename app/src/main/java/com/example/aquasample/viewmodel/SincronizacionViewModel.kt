package com.example.aquasample.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquasample.data.MuestraRepository
import com.example.aquasample.data.sync.ResultadoSync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado del botón Sincronizar. El mensaje se muestra una vez y se limpia. */
data class SincronizacionUiState(val enCurso: Boolean = false, val mensaje: String? = null)

class SincronizacionViewModel(
    private val repositorio: MuestraRepository = MuestraRepository.instancia
) : ViewModel() {

    private val _uiState = MutableStateFlow(SincronizacionUiState())
    val uiState: StateFlow<SincronizacionUiState> = _uiState.asStateFlow()

    fun sincronizar() {
        if (_uiState.value.enCurso) return
        _uiState.value = SincronizacionUiState(enCurso = true)
        viewModelScope.launch {
            val resultado = repositorio.sincronizarTodo()
            _uiState.value = SincronizacionUiState(mensaje = resultado.aMensaje())
        }
    }

    fun mensajeMostrado() = _uiState.update { it.copy(mensaje = null) }
}

/** Texto para el usuario: enviadas, errores o sin conexión. */
fun ResultadoSync.aMensaje(): String = when {
    sinConexion -> "Sin conexión con el servidor. Los cambios quedan guardados en el teléfono."
    errores.isNotEmpty() -> "Enviadas: $enviadas · Con error: ${errores.size}\n" + errores.joinToString("\n")
    porReintentar > 0 -> "El servidor falló. Enviadas: $enviadas. Intenta más tarde."
    enviadas == 0 && bajadas == 0 -> "Todo está sincronizado"
    else -> "Enviadas: $enviadas · Recibidas del servidor: $bajadas"
}
