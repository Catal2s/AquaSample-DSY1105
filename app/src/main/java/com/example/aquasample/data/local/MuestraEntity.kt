package com.example.aquasample.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.aquasample.data.DatosFicticios
import com.example.aquasample.model.Conteo
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.Muestra

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
    val fotoUri: String?
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
    fotoUri = fotoUri
)

/** De fila de la base de datos a modelo de la app. */
fun MuestraEntity.aModelo() = Muestra(
    id = id,
    centro = DatosFicticios.centros.first { it.id == centroId },
    tren = DatosFicticios.trenes.first { it.id == trenId },
    linea = DatosFicticios.lineas.first { it.id == lineaId },
    fecha = fecha,
    hora = hora,
    tramoMetros = tramoMetros,
    operador = DatosFicticios.usuarios.first { it.id == operadorId },
    conteo = Conteo(individuos = individuos, calibrePromedioMm = calibrePromedioMm),
    observaciones = observaciones,
    estado = EstadoRevision.valueOf(estado),
    comentarioSupervisor = comentarioSupervisor,
    fotoUri = fotoUri
)
