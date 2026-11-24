package com.example.aquasample.data

import android.content.Context
import com.example.aquasample.data.local.AppDatabase
import com.example.aquasample.data.local.MuestraDao
import com.example.aquasample.data.local.MuestraEntity
import com.example.aquasample.data.local.aEntidad
import com.example.aquasample.data.local.aEntidadDeEjemplo
import com.example.aquasample.data.local.aModelo
import com.example.aquasample.data.remote.backend.ClienteBackend
import com.example.aquasample.data.sync.LectorFotosAndroid
import com.example.aquasample.data.sync.ResultadoSync
import com.example.aquasample.data.sync.SincronizadorMuestras
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.Muestra
import com.example.aquasample.model.Rol
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
 *
 * Room es la fuente de verdad: cada cambio se guarda primero aquí como PENDIENTE y
 * después se intenta enviar al backend. Si no hay conexión, se envía más tarde.
 */
class MuestraRepository(
    private val dao: MuestraDao,
    private val alcance: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    /** null en pruebas que no usan el backend. */
    private val sincronizador: SincronizadorMuestras? = null
) {

    /** Todas las filas, incluidas las que esperan ser eliminadas en el servidor. */
    private val filas: StateFlow<List<MuestraEntity>> =
        dao.observarTodas().stateIn(alcance, SharingStarted.Eagerly, emptyList())

    /** Lista siempre actualizada: cambia sola cuando la tabla cambia. Oculta las eliminadas. */
    val muestras: StateFlow<List<Muestra>> =
        dao.observarTodas()
            .map { todas -> todas.filter { it.eliminadoPor == null }.map { it.aModelo() } }
            .stateIn(alcance, SharingStarted.Eagerly, emptyList())

    /** Cuenta también las eliminadas sin enviar, para no reutilizar su id. */
    fun siguienteId(): Int = (filas.value.maxOfOrNull { it.id } ?: 0) + 1

    fun guardar(muestra: Muestra) {
        alcance.launch {
            dao.insertar(muestra.aEntidad())
            sincronizador?.subir()
        }
    }

    fun pendientes(): List<Muestra> =
        muestras.value.filter { it.estado == EstadoRevision.PENDIENTE || it.estado == EstadoRevision.CORREGIDO }

    /** [rol] es el de quien hizo el cambio: viaja en el header X-Rol. */
    fun actualizarEstado(
        id: Int,
        estado: EstadoRevision,
        comentario: String? = null,
        rol: Rol = if (estado == EstadoRevision.CORREGIDO) Rol.OPERADOR else Rol.SUPERVISOR
    ) {
        alcance.launch {
            if (sincronizador == null) {
                dao.actualizarEstado(id, estado.name, comentario)
            } else {
                sincronizador.cambiarEstadoLocal(id, estado.name, comentario, rol)
                sincronizador.subir()
            }
        }
    }

    /** La muestra desaparece de la app de inmediato; el DELETE se envía cuando haya conexión. */
    fun eliminar(id: Int, rol: Rol) {
        alcance.launch {
            sincronizador?.eliminarLocal(id, rol)
            sincronizador?.subir()
        }
    }

    /**
     * Envía al backend todo lo que está PENDIENTE. Es la función que debe llamar el Worker
     * de WorkManager, después de `MuestraRepository.inicializar(applicationContext)`.
     *
     * Por cada muestra envía, en orden: PUT de los datos, POST de la foto (si tiene) y PATCH de
     * cada cambio de estado con el rol de quien lo hizo; o el DELETE si fue eliminada.
     * Se puede llamar varias veces sin duplicar, porque el PUT usa el id de la app, y
     * dos llamadas simultáneas se atienden una después de la otra.
     *
     * No lanza excepciones por falta de red ni por errores HTTP.
     *
     * @return `true` si no queda nada que reintentar (`Result.success()`). Incluye las muestras que
     * el servidor rechazó (400, 403, 404, 409): quedan en ERROR, porque repetir el envío daría lo mismo.
     * `false` si no hubo conexión o el servidor falló (5xx): lo no enviado sigue PENDIENTE (`Result.retry()`).
     */
    suspend fun sincronizarPendientes(): Boolean = sincronizador?.subir()?.completa ?: true

    /**
     * Trae GET /api/muestras y actualiza Room, sin pisar los cambios locales pendientes.
     * Borra las muestras sincronizadas que ya no existen en el servidor.
     */
    suspend fun descargarDelServidor(): ResultadoSync = sincronizador?.bajar() ?: ResultadoSync()

    /** Botón "Sincronizar": sube lo pendiente y después baja lo del servidor. */
    suspend fun sincronizarTodo(): ResultadoSync = sincronizador?.subirYBajar() ?: ResultadoSync()

    /** Si la base está vacía (primera vez que se abre la app), carga las muestras de ejemplo. */
    private fun sembrarSiEstaVacia() {
        alcance.launch {
            if (dao.contar() == 0) {
                dao.insertarTodas(DatosFicticios.muestrasEjemplo.map { it.aEntidadDeEjemplo() })
            }
        }
    }

    companion object {
        private var _instancia: MuestraRepository? = null

        /** Instancia compartida. Hay que llamar a inicializar() antes (se hace en MainActivity). */
        val instancia: MuestraRepository
            get() = _instancia ?: error("MuestraRepository no inicializado: llama a inicializar(context)")

        @Synchronized
        fun inicializar(contexto: Context) {
            if (_instancia == null) {
                val base = AppDatabase.obtener(contexto)
                val repo = MuestraRepository(
                    dao = base.muestraDao(),
                    sincronizador = SincronizadorMuestras(
                        dao = base.muestraSyncDao(),
                        api = ClienteBackend.api,
                        fotos = LectorFotosAndroid(contexto.applicationContext)
                    )
                )
                repo.sembrarSiEstaVacia()
                _instancia = repo
            }
        }
    }
}
