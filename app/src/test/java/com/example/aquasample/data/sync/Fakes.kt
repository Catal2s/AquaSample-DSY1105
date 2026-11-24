package com.example.aquasample.data.sync

import com.example.aquasample.data.local.CambioEstadoEntity
import com.example.aquasample.data.local.MuestraEntity
import com.example.aquasample.data.local.MuestraSyncDao
import com.example.aquasample.data.remote.backend.AquaSampleApi
import com.example.aquasample.data.remote.backend.CambioEstadoRequest
import com.example.aquasample.data.remote.backend.MuestraRequest
import com.example.aquasample.data.remote.backend.MuestraResponse
import com.example.aquasample.data.remote.backend.ResumenResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException

/** Room en memoria. */
class FakeMuestraSyncDao(vararg iniciales: MuestraEntity) : MuestraSyncDao {
    val filas = iniciales.associateBy { it.id }.toMutableMap()
    val cambios = mutableListOf<CambioEstadoEntity>()
    private var siguienteCambio = 1L

    private fun cambiar(id: Int, f: (MuestraEntity) -> MuestraEntity) { filas[id]?.let { filas[id] = f(it) } }

    override suspend fun todas() = filas.values.sortedBy { it.id }
    override suspend fun buscar(id: Int) = filas[id]
    override suspend fun pendientes() = todas().filter { it.estadoSync == "PENDIENTE" }
    override suspend fun cambiosDe(muestraId: Int) = cambios.filter { it.muestraId == muestraId }
    override suspend fun actualizarEstadoPendiente(id: Int, estado: String, comentario: String?) =
        cambiar(id) { it.copy(estado = estado, comentarioSupervisor = comentario, estadoSync = "PENDIENTE", mensajeSync = null) }
    override suspend fun insertarCambio(cambio: CambioEstadoEntity) { cambios += cambio.copy(id = siguienteCambio++) }
    override suspend fun marcarEliminada(id: Int, rol: String) =
        cambiar(id) { it.copy(eliminadoPor = rol, estadoSync = "PENDIENTE", mensajeSync = null) }
    override suspend fun datosEnviados(id: Int) = cambiar(id) { it.copy(datosPendientes = false, enServidor = true) }
    override suspend fun fotoProcesada(id: Int) = cambiar(id) { it.copy(fotoPendiente = false) }
    override suspend fun borrarCambio(id: Long) { cambios.removeAll { it.id == id } }
    override suspend fun borrarCambiosDe(muestraId: Int) { cambios.removeAll { it.muestraId == muestraId } }
    override suspend fun marcarError(id: Int, mensaje: String) =
        cambiar(id) { it.copy(estadoSync = "ERROR", mensajeSync = mensaje, eliminadoPor = null) }
    override suspend fun marcarSincronizada(id: Int) = cambiar(id) {
        val limpia = !it.datosPendientes && !it.fotoPendiente && it.eliminadoPor == null && cambios.none { c -> c.muestraId == id }
        if (limpia) it.copy(estadoSync = "SINCRONIZADA", mensajeSync = null) else it
    }
    override suspend fun borrarFila(id: Int) { filas.remove(id) }
    override suspend fun guardarDelServidor(muestra: MuestraEntity) { filas[muestra.id] = muestra }
}

/** Servidor en memoria con las reglas del contrato que importan para sincronizar. */
class FakeAquaSampleApi : AquaSampleApi {
    val guardadas = linkedMapOf<Int, MuestraResponse>()
    val llamadas = mutableListOf<String>()
    var sinInternet = false
    /** El servidor guarda, pero la respuesta del PUT se pierde. */
    var perderRespuestaDelPut = false
    var codigoForzado: Int? = null
    /** Id de muestra cuyo PUT responde 500. */
    var idConError500: Int? = null

    private fun <T> error(codigo: Int, mensaje: String): Response<T> = Response.error(
        codigo, """{"status":$codigo,"error":"x","mensajes":["$mensaje"],"fecha":"2026-11-24T10:00:00"}""".toResponseBody()
    )

    private fun <T> responder(llamada: String, accion: () -> Response<T>): Response<T> {
        llamadas += llamada
        if (sinInternet) throw IOException("sin internet")
        codigoForzado?.let { return error(it, "forzado") }
        return accion()
    }

    override suspend fun listar(centroId: String?, trenId: String?, lineaId: String?, estado: String?, desde: String?, hasta: String?) =
        responder("GET") { Response.success(guardadas.values.toList()) }

    override suspend fun obtener(id: Int) = responder("GET $id") {
        guardadas[id]?.let { Response.success(it) } ?: error(404, "No existe la muestra $id")
    }

    override suspend fun crear(rol: String, datos: MuestraRequest) = error<MuestraResponse>(409, "no se usa")

    override suspend fun guardar(id: Int, rol: String, datos: MuestraRequest): Response<MuestraResponse> =
        responder("PUT $id $rol") {
            val actual = guardadas[id]
            when {
                id == idConError500 -> error(500, "Error interno")
                actual != null && actual.operadorId != datos.operadorId ->
                    error(409, "La muestra $id ya pertenece a otro operador")
                actual?.estado == "VALIDADO" -> error(409, "La muestra $id ya fue validada y no se puede editar")
                else -> {
                    val nueva = datos.aRespuesta(actual?.estado ?: "PENDIENTE").copy(tieneFoto = actual?.tieneFoto ?: false)
                    guardadas[id] = nueva
                    if (perderRespuestaDelPut) throw IOException("se cortó la conexión")
                    Response.success(nueva)
                }
            }
        }

    override suspend fun cambiarEstado(id: Int, rol: String, cambio: CambioEstadoRequest) =
        responder("PATCH $id ${cambio.estado} $rol") {
            val actual = guardadas[id]
            val permitido = when (cambio.estado) {
                "CORREGIDO" -> actual?.estado == "OBSERVADO"
                else -> actual?.estado == "PENDIENTE" || actual?.estado == "CORREGIDO"
            }
            when {
                actual == null -> error(404, "No existe la muestra $id")
                actual.estado == cambio.estado -> Response.success(actual)
                !permitido -> error(409, "Cambio de estado no válido")
                else -> {
                    val comentario = if (rol == "SUPERVISOR") cambio.comentario else actual.comentarioSupervisor
                    guardadas[id] = actual.copy(estado = cambio.estado, comentarioSupervisor = comentario)
                    Response.success(guardadas[id]!!)
                }
            }
        }

    override suspend fun eliminar(id: Int, rol: String): Response<Unit> = responder("DELETE $id $rol") {
        if (guardadas.remove(id) != null) Response.success(Unit) else error(404, "No existe la muestra $id")
    }

    override suspend fun subirFoto(id: Int, rol: String, archivo: MultipartBody.Part) = responder("FOTO $id $rol") {
        guardadas[id] = guardadas.getValue(id).copy(tieneFoto = true)
        Response.success(guardadas.getValue(id))
    }

    override suspend fun resumen() = Response.success(ResumenResponse())
}

class FakeLectorFotos : LectorFotos {
    val borradas = mutableListOf<String>()
    override fun leer(fotoUri: String) = if (fotoUri == "rota") null else FotoParaSubir(byteArrayOf(1), "image/jpeg")
    override fun borrar(fotoUri: String?) { fotoUri?.let { borradas += it } }
}

fun MuestraRequest.aRespuesta(estado: String) = MuestraResponse(
    id, centroId, trenId, lineaId, fecha, hora, tramoMetros, operadorId, individuos, calibrePromedioMm,
    asistidoPorFoto, marcasRegistradas, ajusteManual, observaciones, estado
)
