package com.example.aquasample.data

import com.example.aquasample.data.remote.marina.ClienteOpenMeteo
import com.example.aquasample.data.remote.marina.OpenMeteoMarineApi
import com.example.aquasample.data.remote.marina.RespuestaMarina
import com.example.aquasample.model.CondicionesMarinas
import com.example.aquasample.model.Muestra
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import retrofit2.HttpException
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.math.abs

/** Resultado de consultar la API externa. */
sealed interface ResultadoCondiciones {
    data class Exito(val condiciones: CondicionesMarinas) : ResultadoCondiciones
    data class Error(val mensaje: String) : ResultadoCondiciones
    data object SinConexion : ResultadoCondiciones
}

/** Es una interfaz para poder usar un repositorio falso en las pruebas. */
interface CondicionesMarinasRepository {
    suspend fun obtener(muestra: Muestra): ResultadoCondiciones
}

/** Obtiene las condiciones marinas desde Open-Meteo y convierte las fallas en resultados. */
class CondicionesMarinasRepositoryOpenMeteo(
    private val api: OpenMeteoMarineApi = ClienteOpenMeteo.api
) : CondicionesMarinasRepository {

    override suspend fun obtener(muestra: Muestra): ResultadoCondiciones {
        val lugar = UbicacionesCentros.de(muestra.centro.id)
            ?: return ResultadoCondiciones.Error("Este centro no tiene una ubicación de referencia.")

        return try {
            val respuesta = api.obtenerCondiciones(
                latitud = lugar.latitud,
                longitud = lugar.longitud,
                fechaInicio = muestra.fecha,
                fechaFin = muestra.fecha
            )
            respuesta.aCondiciones(muestra.fecha, muestra.hora, lugar)
                ?.let { ResultadoCondiciones.Exito(it) }
                ?: ResultadoCondiciones.Error("Open-Meteo no tiene datos para esta fecha.")
        } catch (e: HttpException) {
            ResultadoCondiciones.Error(mensajeHttp(e))
        } catch (e: JsonParseException) {
            ResultadoCondiciones.Error(RESPUESTA_INVALIDA)
        } catch (e: MalformedJsonException) {
            ResultadoCondiciones.Error(RESPUESTA_INVALIDA)
        } catch (e: EOFException) {
            // Respuesta sin contenido.
            ResultadoCondiciones.Error(RESPUESTA_INVALIDA)
        } catch (e: InterruptedIOException) {
            // Se cumplió el tiempo máximo. Va antes de IOException porque es una subclase.
            ResultadoCondiciones.Error("El servicio tardó demasiado en responder.")
        } catch (e: IOException) {
            ResultadoCondiciones.SinConexion
        }
    }

    /** La API responde 400 con este texto cuando la fecha está fuera de su rango. */
    private fun mensajeHttp(e: HttpException): String {
        val cuerpo = runCatching { e.response()?.errorBody()?.string() }.getOrNull().orEmpty()
        return if (e.code() == 400 && cuerpo.contains("out of allowed range")) {
            "La fecha está fuera del rango disponible en Open-Meteo."
        } else {
            "Open-Meteo respondió con un error (${e.code()})."
        }
    }

    private companion object {
        const val RESPUESTA_INVALIDA = "La respuesta de Open-Meteo no es válida."
    }
}

/**
 * Elige una hora de la respuesta y la convierte al modelo de la app.
 *
 * - Con hora válida en la muestra: la hora más cercana del mismo día que tenga datos.
 *   Si hay empate (10:30), se usa la anterior (10:00).
 * - Sin hora válida: la hora de mayor oleaje del día (máximo diario de altura de ola).
 *
 * Devuelve null si la respuesta viene vacía o todos los valores son null.
 */
fun RespuestaMarina.aCondiciones(fecha: String, horaMuestra: String, lugar: Coordenadas): CondicionesMarinas? {
    val datos = horario ?: return null
    val candidatas = datos.horas.orEmpty().mapIndexed { i, fechaHora ->
        CondicionesMarinas(
            fecha = fecha,
            hora = fechaHora.substringAfter('T', ""),
            latitud = lugar.latitud,
            longitud = lugar.longitud,
            alturaOlaM = datos.alturaOla?.getOrNull(i),
            periodoOlaS = datos.periodoOla?.getOrNull(i),
            direccionOlaGrados = datos.direccionOla?.getOrNull(i),
            temperaturaMarC = datos.temperaturaMar?.getOrNull(i)
        )
    }.filter {
        it.alturaOlaM != null || it.periodoOlaS != null ||
            it.direccionOlaGrados != null || it.temperaturaMarC != null
    }

    val buscada = minutosDelDia(horaMuestra)
        ?: return candidatas.maxByOrNull { it.alturaOlaM ?: -1.0 }
    return candidatas
        .filter { minutosDelDia(it.hora) != null }
        .minByOrNull { abs(minutosDelDia(it.hora)!! - buscada) }
}

/** "09:15" son 555 minutos. Devuelve null si no es una hora válida. */
private fun minutosDelDia(hora: String): Int? {
    val partes = hora.trim().split(":")
    if (partes.size != 2) return null
    val horas = partes[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
    val minutos = partes[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
    return horas * 60 + minutos
}
