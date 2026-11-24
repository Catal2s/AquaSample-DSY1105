package com.example.aquasample.viewmodel

import com.example.aquasample.data.DatosFicticios
import com.example.aquasample.data.MuestraRepository
import com.example.aquasample.model.EstadoRevision
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Pruebas de los filtros del Historial.
 * Las muestras de ejemplo (DatosFicticios) son:
 * #1 L1 22-09 U1 VALIDADO · #2 L2 22-09 U1 OBSERVADO · #3 L4 24-09 U2 VALIDADO
 * #4 L3 29-09 U2 CORREGIDO · #5 L5 01-10 U1 PENDIENTE
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistorialViewModelSpec : StringSpec({

    lateinit var muestras: MutableStateFlow<List<com.example.aquasample.model.Muestra>>
    lateinit var viewModel: HistorialViewModel

    beforeTest {
        // viewModelScope usa Dispatchers.Main, que no existe fuera de Android: se reemplaza.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        muestras = MutableStateFlow(DatosFicticios.muestrasEjemplo)
        val repositorio = mockk<MuestraRepository>()
        every { repositorio.muestras } returns muestras
        viewModel = HistorialViewModel(repositorio)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    /** uiState solo se recalcula mientras alguien lo observa (como la pantalla). */
    fun TestScope.observarPantalla() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    fun idsVisibles() = viewModel.uiState.value.muestras.map { it.id }

    "sin filtros muestra todas, de la más reciente a la más antigua" {
        runTest {
            observarPantalla()

            idsVisibles() shouldContainExactly listOf(5, 4, 3, 2, 1)
            viewModel.uiState.value.hayFiltros.shouldBeFalse()
            viewModel.uiState.value.totalIndividuos shouldBe 312 + 287 + 401 + 265 + 298
        }
    }

    "el operador solo ve sus propias muestras" {
        runTest {
            observarPantalla()

            viewModel.limitarAOperador("U2")

            idsVisibles() shouldContainExactly listOf(4, 3)
            viewModel.filtros.value.operadorId shouldBe "U2"
        }
    }

    "la búsqueda no distingue tildes ni mayúsculas" {
        runTest {
            observarPantalla()

            viewModel.buscar("LINEA 2")

            idsVisibles() shouldContainExactly listOf(5, 2)
            viewModel.uiState.value.hayFiltros.shouldBeTrue()
        }
    }

    "se puede buscar por número de muestra y por comentario del supervisor" {
        runTest {
            observarPantalla()

            viewModel.buscar("#3")
            idsVisibles() shouldContainExactly listOf(3)

            viewModel.buscar("parece bajo")
            idsVisibles() shouldContainExactly listOf(2)
        }
    }

    "filtra por rango de fechas" {
        runTest {
            observarPantalla()

            viewModel.onFechaDesdeChange("2026-09-24")
            viewModel.onFechaHastaChange("2026-09-30")

            idsVisibles() shouldContainExactly listOf(4, 3)
        }
    }

    "mientras se escribe la fecha no marca error ni filtra" {
        runTest {
            observarPantalla()

            viewModel.onFechaDesdeChange("2026-0")

            viewModel.uiState.value.errorFechaDesde.shouldBeNull()
            idsVisibles().size shouldBe 5
        }
    }

    "una fecha inválida muestra error y no se usa como filtro" {
        runTest {
            observarPantalla()

            viewModel.onFechaDesdeChange("2026-13-40")
            viewModel.onFechaHastaChange("ayer")

            viewModel.uiState.value.errorFechaDesde shouldBe "Usa el formato AAAA-MM-DD"
            viewModel.uiState.value.errorFechaHasta shouldBe "Usa el formato AAAA-MM-DD"
            idsVisibles().size shouldBe 5
        }
    }

    "Hasta no puede ser anterior a Desde" {
        runTest {
            observarPantalla()

            viewModel.onFechaDesdeChange("2026-10-01")
            viewModel.onFechaHastaChange("2026-09-01")

            viewModel.uiState.value.errorFechaHasta shouldBe "Debe ser igual o posterior a Desde"
            idsVisibles() shouldContainExactly listOf(5)
        }
    }

    "elegir un centro filtra y carga sus líneas; tocarlo de nuevo lo quita" {
        runTest {
            observarPantalla()
            val sur = DatosFicticios.centros.first { it.id == "C2" }

            viewModel.filtrarPorCentro(sur)
            idsVisibles() shouldContainExactly listOf(5, 3)
            viewModel.uiState.value.lineasDisponibles.map { it.id } shouldContainExactly listOf("L4", "L5")

            viewModel.filtrarPorCentro(sur)
            viewModel.uiState.value.filtros.centro.shouldBeNull()
            viewModel.uiState.value.lineasDisponibles.shouldBeEmpty()
        }
    }

    "filtra por línea y por estado, y se pueden desmarcar" {
        runTest {
            observarPantalla()
            val linea4 = DatosFicticios.lineas.first { it.id == "L4" }

            viewModel.filtrarPorLinea(linea4)
            idsVisibles() shouldContainExactly listOf(3)
            viewModel.filtrarPorLinea(linea4)
            viewModel.uiState.value.filtros.linea.shouldBeNull()

            viewModel.filtrarPorEstado(EstadoRevision.VALIDADO)
            idsVisibles() shouldContainExactly listOf(3, 1)
            viewModel.filtrarPorEstado(EstadoRevision.VALIDADO)
            viewModel.uiState.value.filtros.estado.shouldBeNull()
        }
    }

    "limpiar filtros quita todo menos el límite por operador" {
        runTest {
            observarPantalla()
            viewModel.limitarAOperador("U1")
            viewModel.buscar("norte")
            viewModel.filtrarPorEstado(EstadoRevision.OBSERVADO)

            viewModel.limpiarFiltros()

            viewModel.uiState.value.hayFiltros.shouldBeFalse()
            viewModel.filtros.value.operadorId shouldBe "U1"
            idsVisibles() shouldContainExactly listOf(5, 2, 1)
        }
    }

    "la lista se actualiza sola cuando llega una muestra nueva" {
        runTest {
            observarPantalla()

            muestras.value = DatosFicticios.muestrasEjemplo.take(2)

            idsVisibles() shouldContainExactly listOf(2, 1)
        }
    }
})
