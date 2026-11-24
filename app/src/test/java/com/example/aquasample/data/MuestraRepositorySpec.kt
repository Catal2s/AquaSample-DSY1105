package com.example.aquasample.data

import com.example.aquasample.data.local.MuestraDao
import com.example.aquasample.data.local.aEntidad
import com.example.aquasample.data.local.aModelo
import com.example.aquasample.model.EstadoRevision
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

/**
 * Pruebas del repositorio. El DAO de Room se simula con MockK (no hay base de datos real),
 * igual que en la Guía 15 se simula el ApiService. Se verifica que el repositorio
 * convierta bien las filas y que llame al DAO correcto en cada operación.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MuestraRepositorySpec : StringSpec({

    val ejemplos = DatosFicticios.muestrasEjemplo

    /** Repositorio con un DAO simulado que tiene las filas indicadas. */
    fun crearRepositorio(dao: MuestraDao, filas: List<com.example.aquasample.data.local.MuestraEntity>) : MuestraRepository {
        every { dao.observarTodas() } returns MutableStateFlow(filas)
        // UnconfinedTestDispatcher: las corrutinas del repositorio corren al instante en la prueba
        return MuestraRepository(dao, CoroutineScope(UnconfinedTestDispatcher()))
    }

    "convierte las filas de Room en muestras de la app" {
        runTest {
            val dao = mockk<MuestraDao>(relaxed = true)

            val repositorio = crearRepositorio(dao, ejemplos.map { it.aEntidad() })

            repositorio.muestras.value shouldBe ejemplos
        }
    }

    "la conversión ida y vuelta entre Muestra y fila de Room no pierde datos" {
        ejemplos.forEach { muestra -> muestra.aEntidad().aModelo() shouldBe muestra }
    }

    "el siguiente id es el mayor más uno, o 1 si no hay muestras" {
        runTest {
            val conDatos = crearRepositorio(mockk(relaxed = true), ejemplos.map { it.aEntidad() })
            conDatos.siguienteId() shouldBe 6

            val vacio = crearRepositorio(mockk(relaxed = true), emptyList())
            vacio.siguienteId() shouldBe 1
        }
    }

    "guardar inserta la muestra en la base de datos" {
        runTest {
            val dao = mockk<MuestraDao>(relaxed = true)
            val repositorio = crearRepositorio(dao, emptyList())

            repositorio.guardar(ejemplos[0])

            coVerify(exactly = 1) { dao.insertar(ejemplos[0].aEntidad()) }
        }
    }

    "actualizar el estado guarda el nombre del estado y el comentario" {
        runTest {
            val dao = mockk<MuestraDao>(relaxed = true)
            val repositorio = crearRepositorio(dao, emptyList())

            repositorio.actualizarEstado(2, EstadoRevision.OBSERVADO, "Recontar")
            repositorio.actualizarEstado(3, EstadoRevision.VALIDADO)

            coVerify { dao.actualizarEstado(2, "OBSERVADO", "Recontar") }
            coVerify { dao.actualizarEstado(3, "VALIDADO", null) }
        }
    }

    "pendientes devuelve las muestras que el supervisor debe revisar" {
        runTest {
            val repositorio = crearRepositorio(mockk(relaxed = true), ejemplos.map { it.aEntidad() })

            // #4 está CORREGIDO y #5 PENDIENTE; las validadas y observadas no se revisan
            repositorio.pendientes().map { it.id } shouldContainExactly listOf(4, 5)
        }
    }

    "usar el repositorio compartido sin inicializarlo da un error claro" {
        val error = shouldThrow<IllegalStateException> { MuestraRepository.instancia }

        error.message shouldContain "no inicializado"
    }
})
