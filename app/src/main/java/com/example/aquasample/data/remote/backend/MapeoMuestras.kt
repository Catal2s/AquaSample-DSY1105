package com.example.aquasample.data.remote.backend

import com.example.aquasample.data.local.MuestraEntity
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.EstadoSincronizacion

/** De fila de Room al JSON que espera el backend. */
fun MuestraEntity.aRequest() = MuestraRequest(
    id = id,
    centroId = centroId,
    trenId = trenId,
    lineaId = lineaId,
    fecha = fecha,
    hora = hora,
    tramoMetros = tramoMetros,
    operadorId = operadorId,
    individuos = individuos,
    calibrePromedioMm = calibrePromedioMm,
    asistidoPorFoto = asistidoPorFoto,
    marcasRegistradas = marcasRegistradas,
    ajusteManual = ajusteManual,
    observaciones = observaciones.trim()
)

/**
 * Del JSON del backend a fila de Room ya sincronizada.
 * Devuelve null si falta un dato obligatorio o el estado no existe en la app.
 */
fun MuestraResponse.aEntidad(fotoUri: String? = null): MuestraEntity? {
    return MuestraEntity(
        id = id ?: return null,
        centroId = centroId ?: return null,
        trenId = trenId ?: return null,
        lineaId = lineaId ?: return null,
        fecha = fecha ?: return null,
        hora = hora ?: return null,
        tramoMetros = tramoMetros ?: return null,
        operadorId = operadorId ?: return null,
        individuos = individuos ?: return null,
        calibrePromedioMm = calibrePromedioMm,
        observaciones = observaciones.orEmpty(),
        estado = EstadoRevision.entries.find { it.name == estado }?.name ?: return null,
        comentarioSupervisor = comentarioSupervisor,
        fotoUri = fotoUri,
        asistidoPorFoto = asistidoPorFoto ?: false,
        marcasRegistradas = marcasRegistradas ?: 0,
        ajusteManual = ajusteManual ?: 0,
        estadoSync = EstadoSincronizacion.SINCRONIZADA.name,
        datosPendientes = false,
        fotoPendiente = false,
        enServidor = true
    )
}

/** true si el servidor ya tiene exactamente los datos que esta fila quería enviar. */
fun MuestraResponse.tieneLosDatosDe(local: MuestraEntity): Boolean =
    aEntidad()?.aRequest() == local.aRequest()
