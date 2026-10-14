package com.example.aquasample.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquasample.data.DatosFicticios
import com.example.aquasample.data.MuestraRepository
import com.example.aquasample.model.Centro
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.Linea
import com.example.aquasample.model.Muestra
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Filtros elegidos por el usuario en Historial. null = sin filtro. */
data class FiltrosHistorial(
    val operadorId: String? = null,
    val centro: Centro? = null,
    val linea: Linea? = null,
    val estado: EstadoRevision? = null
)

/** Estado de la pantalla Historial (RF07 y RF08 del caso). */
data class HistorialUiState(
    val centros: List<Centro> = DatosFicticios.centros,
    val lineasDisponibles: List<Linea> = emptyList(),
    val filtros: FiltrosHistorial = FiltrosHistorial(),
    val muestras: List<Muestra> = emptyList()
) {
    val hayFiltros: Boolean
        get() = filtros.centro != null || filtros.linea != null || filtros.estado != null

    val totalIndividuos: Int get() = muestras.sumOf { it.conteo.individuos }
}

/**
 * Historial de muestras con filtros por centro, línea y estado de revisión.
 * La lista se recalcula sola cada vez que cambian las muestras o los filtros.
 */
class HistorialViewModel(
    repositorio: MuestraRepository = MuestraRepository.instancia
) : ViewModel() {

    private val filtros = MutableStateFlow(FiltrosHistorial())

    val uiState: StateFlow<HistorialUiState> =
        combine(repositorio.muestras, filtros) { todas, actuales -> construirEstado(todas, actuales) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = construirEstado(repositorio.muestras.value, filtros.value)
            )

    /**
     * El operador solo ve sus propias muestras; el supervisor ve todas.
     * La pantalla lo llama con el usuario que inició sesión.
     */
    fun limitarAOperador(operadorId: String?) {
        filtros.update { it.copy(operadorId = operadorId) }
    }

    /** Tocar el centro ya elegido lo quita. Al cambiar de centro se borra la línea. */
    fun filtrarPorCentro(centro: Centro?) {
        filtros.update {
            val nuevo = if (centro?.id == it.centro?.id) null else centro
            it.copy(centro = nuevo, linea = null)
        }
    }

    fun filtrarPorLinea(linea: Linea?) {
        filtros.update { it.copy(linea = if (linea?.id == it.linea?.id) null else linea) }
    }

    fun filtrarPorEstado(estado: EstadoRevision?) {
        filtros.update { it.copy(estado = if (estado == it.estado) null else estado) }
    }

    /** Quita los filtros elegidos, pero mantiene el límite por operador. */
    fun limpiarFiltros() {
        filtros.update { FiltrosHistorial(operadorId = it.operadorId) }
    }

    private fun construirEstado(todas: List<Muestra>, actuales: FiltrosHistorial): HistorialUiState {
        val muestras = todas
            .filter { actuales.operadorId == null || it.operador.id == actuales.operadorId }
            .filter { actuales.centro == null || it.centro.id == actuales.centro.id }
            .filter { actuales.linea == null || it.linea.id == actuales.linea.id }
            .filter { actuales.estado == null || it.estado == actuales.estado }
            // Más recientes primero (fecha y hora van en formato AAAA-MM-DD y HH:MM).
            .sortedWith(compareByDescending<Muestra> { it.fecha }.thenByDescending { it.hora })

        val lineas = actuales.centro
            ?.let { centro -> DatosFicticios.trenesDe(centro.id).flatMap { DatosFicticios.lineasDe(it.id) } }
            .orEmpty()

        return HistorialUiState(
            lineasDisponibles = lineas,
            filtros = actuales,
            muestras = muestras
        )
    }
}
