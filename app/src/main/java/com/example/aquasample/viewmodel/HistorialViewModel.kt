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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Filtros elegidos por el usuario en Historial. null o texto vacío = sin filtro.
 * Las fechas se escriben como AAAA-MM-DD, igual que en el formulario de la muestra.
 */
data class FiltrosHistorial(
    val operadorId: String? = null,
    val texto: String = "",
    val fechaDesde: String = "",
    val fechaHasta: String = "",
    val centro: Centro? = null,
    val linea: Linea? = null,
    val estado: EstadoRevision? = null
)

/** Estado de la pantalla Historial (RF07 y RF08 del caso). */
data class HistorialUiState(
    val centros: List<Centro> = DatosFicticios.centros,
    val lineasDisponibles: List<Linea> = emptyList(),
    val filtros: FiltrosHistorial = FiltrosHistorial(),
    val muestras: List<Muestra> = emptyList(),
    val errorFechaDesde: String? = null,
    val errorFechaHasta: String? = null
) {
    val hayFiltros: Boolean
        get() = filtros.texto.isNotBlank() || filtros.fechaDesde.isNotBlank() || filtros.fechaHasta.isNotBlank() ||
            filtros.centro != null || filtros.linea != null || filtros.estado != null

    val totalIndividuos: Int get() = muestras.sumOf { it.conteo.individuos }
}

/**
 * Historial de muestras con búsqueda de texto y filtros por fecha, centro, línea y estado de revisión.
 * La lista se recalcula sola cada vez que cambian las muestras o los filtros.
 */
class HistorialViewModel(
    repositorio: MuestraRepository = MuestraRepository.instancia
) : ViewModel() {

    private val _filtros = MutableStateFlow(FiltrosHistorial())

    /**
     * Filtros tal cual los escribe el usuario. Los campos de texto (búsqueda y fechas)
     * leen de aquí y no de uiState, porque uiState se recalcula de forma asíncrona
     * y un OutlinedTextField con valor atrasado pierde letras al escribir rápido.
     */
    val filtros: StateFlow<FiltrosHistorial> = _filtros.asStateFlow()

    val uiState: StateFlow<HistorialUiState> =
        combine(repositorio.muestras, _filtros) { todas, actuales -> construirEstado(todas, actuales) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = construirEstado(repositorio.muestras.value, _filtros.value)
            )

    /**
     * El operador solo ve sus propias muestras; el supervisor ve todas.
     * La pantalla lo llama con el usuario que inició sesión.
     */
    fun limitarAOperador(operadorId: String?) {
        _filtros.update { it.copy(operadorId = operadorId) }
    }

    /** Texto de la barra de búsqueda. Se filtra en cada letra que escribe el usuario. */
    fun buscar(texto: String) {
        _filtros.update { it.copy(texto = texto) }
    }

    fun onFechaDesdeChange(valor: String) {
        _filtros.update { it.copy(fechaDesde = valor) }
    }

    fun onFechaHastaChange(valor: String) {
        _filtros.update { it.copy(fechaHasta = valor) }
    }

    /** Tocar el centro ya elegido lo quita. Al cambiar de centro se borra la línea. */
    fun filtrarPorCentro(centro: Centro?) {
        _filtros.update {
            val nuevo = if (centro?.id == it.centro?.id) null else centro
            it.copy(centro = nuevo, linea = null)
        }
    }

    fun filtrarPorLinea(linea: Linea?) {
        _filtros.update { it.copy(linea = if (linea?.id == it.linea?.id) null else linea) }
    }

    fun filtrarPorEstado(estado: EstadoRevision?) {
        _filtros.update { it.copy(estado = if (estado == it.estado) null else estado) }
    }

    /** Quita los filtros elegidos, pero mantiene el límite por operador. */
    fun limpiarFiltros() {
        _filtros.update { FiltrosHistorial(operadorId = it.operadorId) }
    }

    private fun construirEstado(todas: List<Muestra>, actuales: FiltrosHistorial): HistorialUiState {
        val errorDesde = errorDeFecha(actuales.fechaDesde)
        var errorHasta = errorDeFecha(actuales.fechaHasta)
        // Una fecha se usa como filtro solo cuando está completa y es válida.
        val desde = actuales.fechaDesde.trim().takeIf { it.length == 10 && errorDesde == null }
        var hasta = actuales.fechaHasta.trim().takeIf { it.length == 10 && errorHasta == null }
        if (desde != null && hasta != null && hasta < desde) {
            errorHasta = "Debe ser igual o posterior a Desde"
            hasta = null
        }
        val busqueda = actuales.texto.trim().sinTildes()

        val muestras = todas
            .filter { actuales.operadorId == null || it.operador.id == actuales.operadorId }
            .filter { busqueda.isEmpty() || it.textoBuscable().contains(busqueda, ignoreCase = true) }
            // Las fechas en formato AAAA-MM-DD se pueden comparar directo como texto.
            .filter { desde == null || it.fecha >= desde }
            .filter { hasta == null || it.fecha <= hasta }
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
            muestras = muestras,
            errorFechaDesde = errorDesde,
            errorFechaHasta = errorHasta
        )
    }

    /** Todo el texto de la muestra en que se puede buscar: número, ubicación, operador y comentarios. */
    private fun Muestra.textoBuscable(): String =
        listOf(
            "#$id", centro.nombre, tren.nombre, linea.nombre, operador.nombre,
            estado.etiqueta, observaciones, comentarioSupervisor.orEmpty()
        ).joinToString(" ").sinTildes()

    /**
     * Mientras el usuario escribe no se marca error (la fecha está incompleta).
     * Con los 10 caracteres de AAAA-MM-DD se revisa que la fecha exista.
     */
    private fun errorDeFecha(fecha: String): String? {
        val limpia = fecha.trim()
        if (limpia.length < 10 && limpia.all { it.isDigit() || it == '-' }) return null
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val valida = Regex("""\d{4}-\d{2}-\d{2}""").matches(limpia) &&
            runCatching { formato.parse(limpia) }.getOrNull() != null
        return if (valida) null else "Usa el formato AAAA-MM-DD"
    }

    /** Quita tildes para que "linea" encuentre "Línea". */
    private fun String.sinTildes(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
}
