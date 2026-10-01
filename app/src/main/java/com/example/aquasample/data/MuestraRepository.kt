package com.example.aquasample.data

import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.Muestra
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Repositorio en memoria. Las muestras viven mientras la app está abierta.
 * Más adelante se puede cambiar por Room sin tocar las pantallas.
 */
class MuestraRepository {

    private val _muestras = MutableStateFlow<List<Muestra>>(emptyList())
    val muestras: StateFlow<List<Muestra>> = _muestras.asStateFlow()

    private var ultimoId = 0

    fun siguienteId(): Int = ++ultimoId

    fun guardar(muestra: Muestra) {
        _muestras.update { it + muestra }
    }

    fun pendientes(): List<Muestra> =
        _muestras.value.filter { it.estado == EstadoRevision.PENDIENTE || it.estado == EstadoRevision.CORREGIDO }

    fun actualizarEstado(id: Int, estado: EstadoRevision, comentario: String? = null) {
        _muestras.update { lista ->
            lista.map { if (it.id == id) it.copy(estado = estado, comentarioSupervisor = comentario) else it }
        }
    }

    companion object {
        /** Instancia compartida para que todas las pantallas vean las mismas muestras. */
        val instancia = MuestraRepository()
    }
}
