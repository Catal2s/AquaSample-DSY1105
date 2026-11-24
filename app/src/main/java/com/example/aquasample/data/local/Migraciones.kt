package com.example.aquasample.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Versión 1 → 2: agrega los campos de sincronización y del conteo asistido, y la tabla
 * de cambios de estado por enviar. Solo agrega: ninguna muestra se borra.
 */
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Columnas nuevas. Las muestras que ya existían parten como pendientes.
        listOf(
            "asistidoPorFoto INTEGER NOT NULL DEFAULT 0",
            "marcasRegistradas INTEGER NOT NULL DEFAULT 0",
            "ajusteManual INTEGER NOT NULL DEFAULT 0",
            "estadoSync TEXT NOT NULL DEFAULT 'PENDIENTE'",
            "datosPendientes INTEGER NOT NULL DEFAULT 1",
            "fotoPendiente INTEGER NOT NULL DEFAULT 0",
            "enServidor INTEGER NOT NULL DEFAULT 0",
            "eliminadoPor TEXT",
            "mensajeSync TEXT"
        ).forEach { db.execSQL("ALTER TABLE muestras ADD COLUMN $it") }

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS cambios_estado (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "muestraId INTEGER NOT NULL, estado TEXT NOT NULL, comentario TEXT, rol TEXT NOT NULL)"
        )

        // 2. Las muestras de ejemplo (1 a 5) ya están en el servidor. El resto debe subir con su foto.
        db.execSQL(
            "UPDATE muestras SET estadoSync = 'SINCRONIZADA', datosPendientes = 0, enServidor = 1 WHERE id BETWEEN 1 AND 5"
        )
        db.execSQL("UPDATE muestras SET fotoPendiente = 1 WHERE id > 5 AND fotoUri IS NOT NULL")

        // 3. Revisiones hechas en la versión 1: se encolan en el orden que exige el servidor.
        //    Estado inicial en el servidor: 1 y 3 VALIDADO, 2 OBSERVADO, 4 CORREGIDO, 5 y nuevas PENDIENTE.
        val comentario = "COALESCE(NULLIF(TRIM(comentarioSupervisor), ''), 'Corrección solicitada')"
        fun encolar(estado: String, comentarioSql: String, rol: String, condicion: String) = db.execSQL(
            "INSERT INTO cambios_estado (muestraId, estado, comentario, rol) " +
                "SELECT id, '$estado', $comentarioSql, '$rol' FROM muestras WHERE $condicion ORDER BY id"
        )
        encolar("OBSERVADO", comentario, "SUPERVISOR", "id >= 5 AND estado = 'CORREGIDO'")
        encolar(
            "CORREGIDO", "NULL", "OPERADOR",
            "(id >= 5 AND estado = 'CORREGIDO') OR (id = 2 AND estado IN ('CORREGIDO', 'VALIDADO'))"
        )
        encolar("VALIDADO", "comentarioSupervisor", "SUPERVISOR", "id NOT IN (1, 3) AND estado = 'VALIDADO'")
        encolar("OBSERVADO", comentario, "SUPERVISOR", "id >= 4 AND estado = 'OBSERVADO'")
        db.execSQL(
            "UPDATE muestras SET estadoSync = 'PENDIENTE' WHERE id IN (SELECT muestraId FROM cambios_estado)"
        )
    }
}
