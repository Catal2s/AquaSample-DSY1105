package com.example.aquasample.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Base de datos SQLite de la app (archivo "aquasample.db"). */
@Database(entities = [MuestraEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun muestraDao(): MuestraDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(contexto: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext,
                    AppDatabase::class.java,
                    "aquasample.db"
                ).build().also { instancia = it }
            }
    }
}
