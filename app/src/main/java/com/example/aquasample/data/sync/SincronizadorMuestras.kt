package com.example.aquasample.data.sync

import com.example.aquasample.data.local.CambioEstadoEntity
import com.example.aquasample.data.local.MuestraEntity
import com.example.aquasample.data.local.MuestraSyncDao
import com.example.aquasample.data.remote.backend.AquaSampleApi
import com.example.aquasample.data.remote.backend.CambioEstadoRequest
import com.example.aquasample.data.remote.backend.ErrorResponse
import com.example.aquasample.data.remote.backend.aEntidad
import com.example.aquasample.data.remote.backend.aRequest
import com.example.aquasample.data.remote.backend.tieneLosDatosDe
import com.example.aquasample.model.Rol
import com.google.gson.Gson
import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException

/** Resumen de una sincronización, para mostrarlo al usuario. */
data class ResultadoSync(
    val enviadas: Int = 0,
    /** Muestras que el servidor rechazó (no sirve reintentar). */
    val errores: List<String> = emptyList(),
    /** Envíos que fallaron por algo pasajero, como un error 500. */
    val porReintentar: Int = 0,
    val sinConexion: Boolean = false,
    /** Muestras que cambiaron en el teléfono al bajar del servidor. */
    val bajadas: Int = 0
) {
    /** true si no queda nada que valga la pena reintentar. */
    val completa: Boolean get() = !sinConexion && porReintentar == 0
}

/** Lleva los cambios de Room al backend y trae los del backend a Room. */
class SincronizadorMuestras(
    private val dao: MuestraSyncDao,
    private val api: AquaSampleApi,
    private val fotos: LectorFotos
) {
    /** Una sincronización a la vez: el botón y el Worker no envían lo mismo en paralelo. */
    private val candado = Mutex()

    suspend fun subir(): ResultadoSync = candado.withLock { subirPendientes() }

    suspend fun bajar(): ResultadoSync = candado.withLock { bajarTodo(ResultadoSync()) }

    /** Primero sube lo local y después baja, para no traer datos más antiguos que los del teléfono. */
    suspend fun subirYBajar(): ResultadoSync = candado.withLock {
        val subida = subirPendientes()
        if (subida.sinConexion) subida else bajarTodo(subida)
    }

    // ---------- Cambios locales ----------

    suspend fun cambiarEstadoLocal(id: Int, estado: String, comentario: String?, rol: Rol) {
        dao.cambiarEstadoLocal(
            CambioEstadoEntity(muestraId = id, estado = estado, comentario = comentario, rol = rol.name)
        )
    }

    /**
     * Si la muestra nunca llegó al servidor se borra de inmediato. Si llegó, se oculta y
     * queda como borrado pendiente hasta que el servidor confirme el DELETE.
     */
    suspend fun eliminarLocal(id: Int, rol: Rol) {
        val muestra = dao.buscar(id) ?: return
        if (muestra.enServidor) {
            dao.marcarEliminada(id, rol.name)
        } else {
            dao.borrar(id)
            fotos.borrar(muestra.fotoUri)
        }
    }

    // ---------- Subida ----------

    private sealed interface Envio {
        data object Listo : Envio
        data object Reintentar : Envio
        data class Rechazado(val mensaje: String) : Envio
    }

    private suspend fun subirPendientes(): ResultadoSync {
        var resultado = ResultadoSync()
        for (pendiente in dao.pendientes()) {
            // Se lee de nuevo: pudo cambiar mientras se enviaban las anteriores.
            val muestra = dao.buscar(pendiente.id) ?: continue
            val envio = try {
                enviar(muestra)
            } catch (e: JsonParseException) {
                Envio.Reintentar // Respondió algo que no es nuestro backend.
            } catch (e: IOException) {
                return resultado.copy(sinConexion = true) // Lo demás queda pendiente.
            }
            resultado = when (envio) {
                Envio.Listo -> resultado.copy(enviadas = resultado.enviadas + 1)
                Envio.Reintentar -> resultado.copy(porReintentar = resultado.porReintentar + 1)
                is Envio.Rechazado -> resultado.copy(errores = resultado.errores + "#${muestra.id}: ${envio.mensaje}")
            }
        }
        return resultado
    }

    /** Orden: DELETE, o bien PUT de los datos, POST de la foto y PATCH de cada cambio de estado. */
    private suspend fun enviar(muestra: MuestraEntity): Envio {
        if (muestra.eliminadoPor != null) return enviarEliminacion(muestra, muestra.eliminadoPor)

        if (muestra.datosPendientes) {
            val envio = enviarDatos(muestra)
            if (envio != Envio.Listo) return envio
        }
        var errorFoto: String? = null
        if (muestra.fotoPendiente) {
            when (val envio = enviarFoto(muestra)) {
                Envio.Listo -> Unit
                Envio.Reintentar -> return envio
                // La muestra sirve aunque falle su foto: se sigue con los cambios de estado.
                is Envio.Rechazado -> errorFoto = envio.mensaje
            }
        }
        val envio = enviarCambiosDeEstado(muestra.id)
        if (envio != Envio.Listo) return envio
        if (errorFoto != null) return rechazar(muestra.id, errorFoto)

        dao.marcarSincronizada(muestra.id)
        return Envio.Listo
    }

    private suspend fun enviarDatos(muestra: MuestraEntity): Envio {
        val respuesta = api.guardar(muestra.id, Rol.OPERADOR.name, muestra.aRequest())
        return when {
            respuesta.isSuccessful -> {
                dao.datosEnviados(muestra.id)
                Envio.Listo
            }
            respuesta.esPasajera() -> Envio.Reintentar
            respuesta.code() == 409 -> resolverConflicto(muestra, respuesta.mensaje())
            else -> rechazar(muestra.id, respuesta.mensaje())
        }
    }

    /**
     * El PUT responde 409 en dos casos: la muestra ya fue validada, o el id es de otro operador.
     * - Si el servidor ya tiene estos mismos datos (un envío anterior llegó, pero se perdió
     *   la respuesta, y luego la validaron), no hay conflicto: está sincronizada.
     * - Si son datos distintos, queda en ERROR y se conserva la copia local. No se reintenta.
     */
    private suspend fun resolverConflicto(muestra: MuestraEntity, mensaje: String): Envio {
        val enServidor = api.obtener(muestra.id)
        if (enServidor.esPasajera()) return Envio.Reintentar
        if (enServidor.body()?.tieneLosDatosDe(muestra) == true) {
            dao.datosEnviados(muestra.id)
            return Envio.Listo
        }
        return rechazar(muestra.id, mensaje)
    }

    private suspend fun enviarFoto(muestra: MuestraEntity): Envio {
        val foto = muestra.fotoUri?.let { withContext(Dispatchers.IO) { fotos.leer(it) } }
        if (foto == null) {
            dao.fotoProcesada(muestra.id)
            return Envio.Rechazado("No se pudo leer la foto")
        }
        val extension = if (foto.tipo == "image/png") "png" else "jpg"
        val parte = MultipartBody.Part.createFormData(
            "archivo",
            "muestra_${muestra.id}.$extension",
            foto.bytes.toRequestBody(foto.tipo.toMediaType())
        )
        val respuesta = api.subirFoto(muestra.id, Rol.OPERADOR.name, parte)
        if (respuesta.esPasajera()) return Envio.Reintentar
        // 409: ya fue validada. Si el servidor tiene foto, es porque un envío anterior llegó.
        val yaEstaba = respuesta.code() == 409 && api.obtener(muestra.id).body()?.tieneFoto == true
        dao.fotoProcesada(muestra.id)
        return if (respuesta.isSuccessful || yaEstaba) Envio.Listo
        else Envio.Rechazado("Foto no subida: ${respuesta.mensaje()}")
    }

    private suspend fun enviarCambiosDeEstado(id: Int): Envio {
        for (cambio in dao.cambiosDe(id)) {
            val respuesta = api.cambiarEstado(id, cambio.rol, CambioEstadoRequest(cambio.estado, cambio.comentario))
            if (respuesta.esPasajera()) return Envio.Reintentar
            if (!respuesta.isSuccessful) {
                // Manda el servidor: se descarta el cambio y al bajar se verá su estado real.
                dao.borrarCambiosDe(id)
                return rechazar(id, respuesta.mensaje())
            }
            dao.borrarCambio(cambio.id)
        }
        return Envio.Listo
    }

    private suspend fun enviarEliminacion(muestra: MuestraEntity, rol: String): Envio {
        val respuesta = api.eliminar(muestra.id, rol)
        return when {
            // 404: ya no existía, que es lo que se quería.
            respuesta.isSuccessful || respuesta.code() == 404 -> {
                dao.borrar(muestra.id)
                fotos.borrar(muestra.fotoUri)
                Envio.Listo
            }
            respuesta.esPasajera() -> Envio.Reintentar
            else -> rechazar(muestra.id, respuesta.mensaje())
        }
    }

    private suspend fun rechazar(id: Int, mensaje: String): Envio {
        dao.marcarError(id, mensaje)
        return Envio.Rechazado(mensaje)
    }

    // ---------- Bajada ----------

    private suspend fun bajarTodo(previo: ResultadoSync): ResultadoSync = try {
        val respuesta = api.listar()
        val lista = respuesta.body()
        if (!respuesta.isSuccessful || lista == null) {
            previo.copy(porReintentar = previo.porReintentar + 1)
        } else {
            val cambios = dao.aplicarServidor(
                delServidor = lista.mapNotNull { it.aEntidad() },
                idsServidor = lista.mapNotNull { it.id }.toSet()
            )
            cambios.borradas.forEach { fotos.borrar(it.fotoUri) }
            previo.copy(bajadas = cambios.actualizadas + cambios.borradas.size)
        }
    } catch (e: JsonParseException) {
        previo.copy(porReintentar = previo.porReintentar + 1)
    } catch (e: IOException) {
        previo.copy(sinConexion = true)
    }

    // ---------- Apoyo ----------

    /** Fallas que se pueden arreglar solas: vale la pena reintentar más tarde. */
    private fun Response<*>.esPasajera() = code() >= 500 || code() == 408 || code() == 429

    private fun Response<*>.mensaje(): String {
        val error = runCatching { Gson().fromJson(errorBody()?.string(), ErrorResponse::class.java) }.getOrNull()
        return error?.mensajes?.joinToString(". ")?.ifBlank { null } ?: "Error ${code()} del servidor"
    }
}
