package com.example.aquasample.data

import android.content.Context
import com.example.aquasample.data.local.AppDatabase
import com.example.aquasample.data.local.MuestraDao
import com.example.aquasample.data.local.aEntidad
import com.example.aquasample.data.local.aModelo
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.Muestra
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Repositorio de muestras respaldado por Room (SQLite).
 *
 * Mantiene la misma interfaz que la versión en memoria (muestras, siguienteId,
 * guardar, pendientes, actualizarEstado), así que los ViewModels y las
 * pantallas no cambian. Las muestras ahora se conservan al cerrar la app.
 */
class MuestraRepository(
    private val dao: MuestraDao,
    private val alcance: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    /** Lista siempre actualizada: cambia sola cuando la tabla cambia. */
    val muestras: StateFlow<List<Muestra>> =
        dao.observarTodas()
            .map { filas -> filas.map { it.aModelo() } }
            .stateIn(alcance, SharingStarted.Eagerly, emptyList())

    fun siguienteId(): Int = (muestras.value.maxOfOrNull { it.id } ?: 0) + 1

    fun guardar(muestra: Muestra) {
        alcance.launch { dao.insertar(muestra.aEntidad()) }
    }

    fun pendientes(): List<Muestra> =
        muestras.value.filter { it.estado == EstadoRevision.PENDIENTE || it.estado == EstadoRevision.CORREGIDO }

    fun actualizarEstado(id: Int, estado: EstadoRevision, comentario: String? = null) {
        alcance.launch { dao.actualizarEstado(id, estado.name, comentario) }
    }

    /** Si la base está vacía (primera vez que se abre la app), carga las muestras de ejemplo. */
    private fun sembrarSiEstaVacia() {
        alcance.launch {
            if (dao.contar() == 0) {
                dao.insertarTodas(DatosFicticios.muestrasEjemplo.map { it.aEntidad() })
            }
        }
    }

    companion object {
        private var _instancia: MuestraRepository? = null

        /** Instancia compartida. Hay que llamar a inicializar() antes (se hace en MainActivity). */
        val instancia: MuestraRepository
            get() = _instancia ?: error("MuestraRepository no inicializado: llama a inicializar(context)")

        fun inicializar(contexto: Context) {
            if (_instancia == null) {
                val repo = MuestraRepository(AppDatabase.obtener(contexto).muestraDao())
                repo.sembrarSiEstaVacia()
                _instancia = repo
            }
        }
    }
}
