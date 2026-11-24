package com.example.aquasample.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.aquasample.data.DatosFicticios
import com.example.aquasample.model.Centro
import com.example.aquasample.model.Conteo
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.EstadoSincronizacion
import com.example.aquasample.model.Linea
import com.example.aquasample.model.Muestra
import com.example.aquasample.model.Rol
import com.example.aquasample.model.Tren
import com.example.aquasample.model.Usuario

/**
 * Fila de la tabla "muestras" en SQLite.
 *
 * Room no guarda objetos anidados (Centro, Tren, Linea, Usuario), así que
 * aquí se guardan solo sus ids. Al leer, se reconstruyen con el catálogo
 * de DatosFicticios. El estado se guarda como texto (el nombre del enum).
 */
@Entity(tableName = "muestras")
data class MuestraEntity(
    @PrimaryKey val id: Int,
    val centroId: String,
    val trenId: String,
    val lineaId: String,
    val fecha: String,
    val hora: String,
    val tramoMetros: Double,
    val operadorId: String,
    val individuos: Int,
    val calibrePromedioMm: Double?,
    val observaciones: String,
    val estado: String,
    val comentarioSupervisor: String?,
    val fotoUri: String?,

    // Versión 2: conteo asistido.
    @ColumnInfo(defaultValue = "0") val asistidoPorFoto: Boolean = false,
    @ColumnInfo(defaultValue = "0") val marcasRegistradas: Int = 0,
    @ColumnInfo(defaultValue = "0") val ajusteManual: Int = 0,

    // Versión 2: sincronización.
    @ColumnInfo(defaultValue = "PENDIENTE") val estadoSync: String = EstadoSincronizacion.PENDIENTE.name,
    /** Falta enviar los datos (PUT). */
    @ColumnInfo(defaultValue = "1") val datosPendientes: Boolean = true,
    /** Falta subir la foto. */
    @ColumnInfo(defaultValue = "0") val fotoPendiente: Boolean = false,
    /** El servidor ya la conoce: para eliminarla hay que enviar un DELETE. */
    @ColumnInfo(defaultValue = "0") val enServidor: Boolean = false,
    /** Rol de quien la eliminó. Si no es null, está oculta y falta enviar el DELETE. */
    val eliminadoPor: String? = null,
    /** Motivo del último error de sincronización. */
    val mensajeSync: String? = null
)

/** De modelo de la app a fila de la base de datos. */
fun Muestra.aEntidad() = MuestraEntity(
    id = id,
    centroId = centro.id,
    trenId = tren.id,
    lineaId = linea.id,
    fecha = fecha,
    hora = hora,
    tramoMetros = tramoMetros,
    operadorId = operador.id,
    individuos = conteo.individuos,
    calibrePromedioMm = conteo.calibrePromedioMm,
    observaciones = observaciones,
    estado = estado.name,
    comentarioSupervisor = comentarioSupervisor,
    fotoUri = fotoUri,
    asistidoPorFoto = conteo.asistidoPorFoto,
    marcasRegistradas = conteo.marcasRegistradas,
    ajusteManual = conteo.ajusteManual,
    fotoPendiente = fotoUri != null
)

/** Fila de una muestra de ejemplo: el servidor ya la tiene, no hay nada que enviar. */
fun Muestra.aEntidadDeEjemplo() = aEntidad().copy(
    estadoSync = EstadoSincronizacion.SINCRONIZADA.name,
    datosPendientes = false,
    fotoPendiente = false,
    enServidor = true
)

/**
 * De fila de la base de datos a modelo de la app. El servidor no valida los ids contra
 * el catálogo, así que si llega uno desconocido se muestra el id (la app no se cae).
 */
fun MuestraEntity.aModelo() = Muestra(
    id = id,
    centro = DatosFicticios.centros.find { it.id == centroId } ?: Centro(centroId, centroId),
    tren = DatosFicticios.trenes.find { it.id == trenId } ?: Tren(trenId, trenId, centroId),
    linea = DatosFicticios.lineas.find { it.id == lineaId } ?: Linea(lineaId, lineaId, trenId),
    fecha = fecha,
    hora = hora,
    tramoMetros = tramoMetros,
    operador = DatosFicticios.usuarios.find { it.id == operadorId }
        ?: Usuario(operadorId, operadorId, usuario = "", clave = "", rol = Rol.OPERADOR),
    conteo = Conteo(
        individuos = individuos,
        calibrePromedioMm = calibrePromedioMm,
        asistidoPorFoto = asistidoPorFoto,
        marcasRegistradas = marcasRegistradas,
        ajusteManual = ajusteManual
    ),
    observaciones = observaciones,
    estado = EstadoRevision.valueOf(estado),
    comentarioSupervisor = comentarioSupervisor,
    fotoUri = fotoUri,
    estadoSync = EstadoSincronizacion.valueOf(estadoSync),
    mensajeSync = mensajeSync
)
