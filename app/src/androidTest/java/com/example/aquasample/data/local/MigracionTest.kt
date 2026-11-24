package com.example.aquasample.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Crea a mano una base versión 1 y la abre con Room versión 2. Si la migración
 * no deja la tabla igual a MuestraEntity, Room lanza una excepción al abrirla.
 * Usa su propio archivo: no toca la base real de la app.
 */
@RunWith(AndroidJUnit4::class)
class MigracionTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val nombre = "prueba_migracion.db"

    @Before
    fun crearVersion1() {
        contexto.deleteDatabase(nombre)
        val archivo = contexto.getDatabasePath(nombre).also { it.parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(archivo, null).use { db ->
            db.execSQL(
                "CREATE TABLE muestras (id INTEGER NOT NULL, centroId TEXT NOT NULL, trenId TEXT NOT NULL, " +
                    "lineaId TEXT NOT NULL, fecha TEXT NOT NULL, hora TEXT NOT NULL, tramoMetros REAL NOT NULL, " +
                    "operadorId TEXT NOT NULL, individuos INTEGER NOT NULL, calibrePromedioMm REAL, " +
                    "observaciones TEXT NOT NULL, estado TEXT NOT NULL, comentarioSupervisor TEXT, " +
                    "fotoUri TEXT, PRIMARY KEY(id))"
            )
            fun insertar(id: Int, estado: String, comentario: String? = null, foto: String? = null) = db.execSQL(
                "INSERT INTO muestras VALUES (?, 'C1', 'T1', 'L1', '2026-10-01', '09:30', 0.2, 'U1', ?, NULL, 'obs', ?, ?, ?)",
                arrayOf<Any?>(id, id * 10, estado, comentario, foto)
            )
            insertar(1, "VALIDADO")
            insertar(2, "OBSERVADO", "Revisar")
            insertar(3, "VALIDADO")
            insertar(4, "CORREGIDO")
            insertar(5, "VALIDADO") // Aprobada en la versión 1.
            insertar(6, "CORREGIDO", "Recontar", "/foto.jpg")
            insertar(7, "PENDIENTE")
            db.version = 1
        }
    }

    @After
    fun limpiar() {
        contexto.deleteDatabase(nombre)
    }

    @Test
    fun migracion1a2_conservaTodoYMarcaLoPendiente() = runBlocking {
        val base = Room.databaseBuilder(contexto, AppDatabase::class.java, nombre)
            .addMigrations(MIGRACION_1_2).build()
        val dao = base.muestraSyncDao()
        val filas = dao.todas().associateBy { it.id }

        assertEquals((1..7).toSet(), filas.keys)
        assertTrue(filas.values.all { it.individuos == it.id * 10 && it.observaciones == "obs" })

        (1..4).forEach {
            assertEquals("SINCRONIZADA", filas[it]!!.estadoSync)
            assertTrue(filas[it]!!.enServidor && !filas[it]!!.datosPendientes)
        }
        // 5: solo falta enviar la aprobación. 6 y 7: falta todo.
        assertEquals("PENDIENTE", filas[5]!!.estadoSync)
        assertEquals(false, filas[5]!!.datosPendientes)
        assertEquals(listOf("VALIDADO"), dao.cambiosDe(5).map { it.estado })
        assertTrue(filas[6]!!.datosPendientes && filas[6]!!.fotoPendiente)
        assertEquals(
            listOf("OBSERVADO" to "SUPERVISOR", "CORREGIDO" to "OPERADOR"),
            dao.cambiosDe(6).map { it.estado to it.rol }
        )
        assertEquals("Recontar", dao.cambiosDe(6).first().comentario)
        assertTrue(filas[7]!!.datosPendientes && !filas[7]!!.fotoPendiente)
        base.close()
    }
}
