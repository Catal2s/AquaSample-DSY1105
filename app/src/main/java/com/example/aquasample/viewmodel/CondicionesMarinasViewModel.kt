package com.example.aquasample.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquasample.data.CondicionesMarinasRepository
import com.example.aquasample.data.CondicionesMarinasRepositoryOpenMeteo
import com.example.aquasample.data.ResultadoCondiciones
import com.example.aquasample.model.CondicionesMarinas
import com.example.aquasample.model.Muestra
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado de la tarjeta de condiciones marinas. */
sealed interface CondicionesMarinasUiState {
    data object Cargando : CondicionesMarinasUiState
    data class Exito(val condiciones: CondicionesMarinas) : CondicionesMarinasUiState
    data class Error(val mensaje: String) : CondicionesMarinasUiState
    data object SinConexion : CondicionesMarinasUiState
}

/** Pide las condiciones marinas de una muestra a la API externa. */
class CondicionesMarinasViewModel(
    private val repositorio: CondicionesMarinasRepository = CondicionesMarinasRepositoryOpenMeteo()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CondicionesMarinasUiState>(CondicionesMarinasUiState.Cargando)
    val uiState: StateFlow<CondicionesMarinasUiState> = _uiState.asStateFlow()

    private var muestraActual: Muestra? = null
    private var consulta: Job? = null

    /** No repite la consulta si centro, fecha y hora son los mismos (por ejemplo, al girar la pantalla). */
    fun cargar(muestra: Muestra) {
        val anterior = muestraActual
        muestraActual = muestra
        val mismosDatos = anterior != null && anterior.centro.id == muestra.centro.id &&
            anterior.fecha == muestra.fecha && anterior.hora == muestra.hora
        if (!mismosDatos) consultar(muestra)
    }

    fun reintentar() {
        muestraActual?.let { consultar(it) }
    }

    private fun consultar(muestra: Muestra) {
        consulta?.cancel()
        _uiState.value = CondicionesMarinasUiState.Cargando
        consulta = viewModelScope.launch {
            _uiState.value = when (val resultado = repositorio.obtener(muestra)) {
                is ResultadoCondiciones.Exito -> CondicionesMarinasUiState.Exito(resultado.condiciones)
                is ResultadoCondiciones.Error -> CondicionesMarinasUiState.Error(resultado.mensaje)
                ResultadoCondiciones.SinConexion -> CondicionesMarinasUiState.SinConexion
            }
        }
    }
}
