package com.example.aquasample.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/** Operaciones de Room que usa la sincronización. Va aparte de MuestraDao para no cambiar sus pruebas. */
@Dao
interface MuestraSyncDao {

    @Query("SELECT * FROM muestras ORDER BY id")
    suspend fun todas(): List<MuestraEntity>

    @Query("SELECT * FROM muestras WHERE id = :id")
    suspend fun buscar(id: Int): MuestraEntity?

    @Query("SELECT * FROM muestras WHERE estadoSync = 'PENDIENTE' ORDER BY id")
    suspend fun pendientes(): List<MuestraEntity>

    @Query("SELECT * FROM cambios_estado WHERE muestraId = :muestraId ORDER BY id")
    suspend fun cambiosDe(muestraId: Int): List<CambioEstadoEntity>

    // ---------- Cambios locales ----------

    @Query(
        "UPDATE muestras SET estado = :estado, comentarioSupervisor = :comentario, " +
            "estadoSync = 'PENDIENTE', mensajeSync = NULL WHERE id = :id"
    )
    suspend fun actualizarEstadoPendiente(id: Int, estado: String, comentario: String?)

    @Insert
    suspend fun insertarCambio(cambio: CambioEstadoEntity)

    /** Todo o nada: la muestra y el cambio por enviar se guardan juntos. */
    @Transaction
    suspend fun cambiarEstadoLocal(cambio: CambioEstadoEntity) {
        actualizarEstadoPendiente(cambio.muestraId, cambio.estado, cambio.comentario)
        insertarCambio(cambio)
    }

    @Query("UPDATE muestras SET eliminadoPor = :rol, estadoSync = 'PENDIENTE', mensajeSync = NULL WHERE id = :id")
    suspend fun marcarEliminada(id: Int, rol: String)

    // ---------- Resultado del envío ----------

    @Query("UPDATE muestras SET datosPendientes = 0, enServidor = 1 WHERE id = :id")
    suspend fun datosEnviados(id: Int)

    @Query("UPDATE muestras SET fotoPendiente = 0 WHERE id = :id")
    suspend fun fotoProcesada(id: Int)

    @Query("DELETE FROM cambios_estado WHERE id = :id")
    suspend fun borrarCambio(id: Long)

    @Query("DELETE FROM cambios_estado WHERE muestraId = :muestraId")
    suspend fun borrarCambiosDe(muestraId: Int)

    @Query("UPDATE muestras SET estadoSync = 'ERROR', mensajeSync = :mensaje, eliminadoPor = NULL WHERE id = :id")
    suspend fun marcarError(id: Int, mensaje: String)

    /** Solo si no quedó nada por enviar (pudo haber un cambio nuevo durante el envío). */
    @Query(
        "UPDATE muestras SET estadoSync = 'SINCRONIZADA', mensajeSync = NULL WHERE id = :id " +
            "AND datosPendientes = 0 AND fotoPendiente = 0 AND eliminadoPor IS NULL " +
            "AND NOT EXISTS (SELECT 1 FROM cambios_estado WHERE muestraId = :id)"
    )
    suspend fun marcarSincronizada(id: Int)

    @Query("DELETE FROM muestras WHERE id = :id")
    suspend fun borrarFila(id: Int)

    @Transaction
    suspend fun borrar(id: Int) {
        borrarCambiosDe(id)
        borrarFila(id)
    }

    // ---------- Bajada ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarDelServidor(muestra: MuestraEntity)

    /**
     * Actualiza Room con lo que tiene el servidor, sin tocar lo que está por enviarse.
     * Las muestras ya sincronizadas que no vienen en [idsServidor] se borran,
     * salvo que el servidor no tenga ninguna (protege los datos si la base está vacía).
     */
    @Transaction
    suspend fun aplicarServidor(delServidor: List<MuestraEntity>, idsServidor: Set<Int>): CambiosBajados {
        val locales = todas().associateBy { it.id }
        var actualizadas = 0
        delServidor.forEach { remota ->
            val local = locales[remota.id]
            if (local != null && local.tieneCambiosLocales()) return@forEach
            // La foto vive en este teléfono: se conserva si es la misma muestra.
            val nueva = remota.copy(fotoUri = local?.takeIf { it.esLaMismaQue(remota) }?.fotoUri)
            if (nueva != local) {
                guardarDelServidor(nueva)
                actualizadas++
            }
        }
        val borradas = if (idsServidor.isEmpty()) emptyList() else locales.values.filter {
            it.id !in idsServidor && it.estadoSync == "SINCRONIZADA" && !it.tieneCambiosLocales()
        }
        borradas.forEach { borrar(it.id) }
        return CambiosBajados(actualizadas, borradas)
    }
}

/** Lo que cambió en Room al bajar del servidor. */
data class CambiosBajados(val actualizadas: Int, val borradas: List<MuestraEntity>)

private fun MuestraEntity.tieneCambiosLocales() =
    estadoSync == "PENDIENTE" || datosPendientes || eliminadoPor != null

private fun MuestraEntity.esLaMismaQue(otra: MuestraEntity) =
    operadorId == otra.operadorId && lineaId == otra.lineaId && fecha == otra.fecha && hora == otra.hora
