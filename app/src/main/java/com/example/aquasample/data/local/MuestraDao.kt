package com.example.aquasample.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Operaciones sobre la tabla "muestras". Room genera el código de cada una. */
@Dao
interface MuestraDao {

    /** Emite la lista completa cada vez que la tabla cambia. */
    @Query("SELECT * FROM muestras ORDER BY id")
    fun observarTodas(): Flow<List<MuestraEntity>>

    @Query("SELECT * FROM muestras WHERE id = :id")
    suspend fun buscarPorId(id: Int): MuestraEntity?

    @Query("SELECT COUNT(*) FROM muestras")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(muestra: MuestraEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(muestras: List<MuestraEntity>)

    @Query("UPDATE muestras SET estado = :estado, comentarioSupervisor = :comentario WHERE id = :id")
    suspend fun actualizarEstado(id: Int, estado: String, comentario: String?)
}
