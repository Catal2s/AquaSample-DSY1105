package com.example.aquasample.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cambio de estado que falta enviar (PATCH). Es una lista y no un solo campo porque
 * sin internet pueden acumularse varios, y el servidor los exige en orden
 * (por ejemplo OBSERVADO y luego CORREGIDO), cada uno con el rol de quien lo hizo.
 */
@Entity(tableName = "cambios_estado")
data class CambioEstadoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val muestraId: Int,
    val estado: String,
    val comentario: String?,
    val rol: String
)
