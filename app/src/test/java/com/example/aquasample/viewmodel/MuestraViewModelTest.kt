package com.example.aquasample.viewmodel

import com.example.aquasample.data.DatosFicticios
import com.example.aquasample.data.MuestraRepository
import com.example.aquasample.data.local.MuestraDao
import com.example.aquasample.data.local.MuestraEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

private class FakeMuestraDao : MuestraDao {
    private val lista = MutableStateFlow<List<MuestraEntity>>(emptyList())
    override fun observarTodas(): Flow<List<MuestraEntity>> = lista
    override suspend fun buscarPorId(id: Int): MuestraEntity? = lista.value.find { it.id == id }
    override suspend fun contar(): Int = lista.value.size
    override suspend fun insertar(muestra: MuestraEntity) { lista.value = lista.value + muestra }
    override suspend fun insertarTodas(muestras: List<MuestraEntity>) { lista.value = lista.value + muestras }
    override suspend fun actualizarEstado(id: Int, estado: String, comentario: String?) {}
}

class MuestraViewModelTest {

    private lateinit var viewModel: MuestraViewModel

    @Before
    fun setUp() {
        val repo = MuestraRepository(FakeMuestraDao())
        viewModel = MuestraViewModel(repositorio = repo, reloj = { Date(1730000000000L) })
    }

    @Test
    fun conteoAsistido_agregarMarcas_incrementaIndividuosYListaDeMarcas() {
        assertEquals(0, viewModel.uiState.value.individuos)
        assertEquals(0, viewModel.uiState.value.totalMarcas)

        viewModel.agregarMarca(0.2f, 0.3f)
        viewModel.agregarMarca(0.4f, 0.5f)

        val estado = viewModel.uiState.value
        assertEquals(2, estado.individuos)
        assertEquals(2, estado.totalMarcas)
        assertEquals(0.2f, estado.marcasPuntos[0].xRel, 0.001f)
        assertEquals(0.5f, estado.marcasPuntos[1].yRel, 0.001f)
    }

    @Test
    fun conteoAsistido_deshacerUltimaMarca_decrementaContador() {
        viewModel.agregarMarca(0.1f, 0.1f)
        viewModel.agregarMarca(0.2f, 0.2f)
        viewModel.agregarMarca(0.3f, 0.3f)

        assertEquals(3, viewModel.uiState.value.individuos)

        viewModel.deshacerUltimaMarca()

        val estado = viewModel.uiState.value
        assertEquals(2, estado.individuos)
        assertEquals(2, estado.totalMarcas)
        assertEquals(2, estado.marcasPuntos.last().id)
    }

    @Test
    fun conteoAsistido_ajusteManual_calculaDiferenciaCorrectamente() {
        viewModel.agregarMarca(0.1f, 0.1f)
        viewModel.agregarMarca(0.2f, 0.2f)

        assertEquals(2, viewModel.uiState.value.totalMarcas)
        assertEquals(2, viewModel.uiState.value.individuos)
        assertEquals(0, viewModel.uiState.value.ajusteManual)

        // El usuario ajusta manualmente a 5 individuos
        viewModel.onIndividuosChange("5")

        val estado = viewModel.uiState.value
        assertEquals(5, estado.individuos)
        assertEquals(2, estado.totalMarcas)
        assertEquals(3, estado.ajusteManual)
    }

    @Test
    fun conteoAsistido_limpiarMarcas_reiniciaMarcasEIndividuos() {
        viewModel.agregarMarca(0.5f, 0.5f)
        viewModel.limpiarMarcas()

        assertEquals(0, viewModel.uiState.value.totalMarcas)
        assertEquals(0, viewModel.uiState.value.individuos)
        assertTrue(viewModel.uiState.value.marcasPuntos.isEmpty())
    }

    @Test
    fun alternarModoMarcado_cambiaEstadoModoMarcado() {
        assertTrue(viewModel.uiState.value.modoMarcadoActivo)
        viewModel.alternarModoMarcado()
        assertFalse(viewModel.uiState.value.modoMarcadoActivo)
        viewModel.alternarModoMarcado()
        assertTrue(viewModel.uiState.value.modoMarcadoActivo)
    }

    /** Deja válido el resto de Nueva muestra, para probar solo las observaciones. */
    private fun completarDatos(observaciones: String) {
        val centro = DatosFicticios.centros.first()
        val tren = DatosFicticios.trenesDe(centro.id).first()
        viewModel.seleccionarCentro(centro)
        viewModel.seleccionarTren(tren)
        viewModel.seleccionarLinea(DatosFicticios.lineasDe(tren.id).first())
        viewModel.onTramoChange("0.2")
        viewModel.onObservacionesChange(observaciones)
    }

    private fun errorObservaciones() = viewModel.uiState.value.errores[CampoMuestra.OBSERVACIONES]

    @Test
    fun observaciones_con499Caracteres_sonValidas() {
        completarDatos("a".repeat(499))

        assertTrue(viewModel.validarDatosMuestra())
        assertNull(errorObservaciones())
    }

    @Test
    fun observaciones_con500Caracteres_sonValidas() {
        completarDatos("a".repeat(500))

        assertTrue(viewModel.validarDatosMuestra())
        assertNull(errorObservaciones())
    }

    @Test
    fun observaciones_con501Caracteres_muestranErrorYNoAvanzan() {
        completarDatos("a".repeat(501))

        assertFalse(viewModel.validarDatosMuestra())
        assertEquals(
            mapOf(CampoMuestra.OBSERVACIONES to "Usa como máximo 500 caracteres"),
            viewModel.uiState.value.errores
        )
    }

    @Test
    fun observaciones_losEspaciosDeLosExtremosNoCuentan() {
        completarDatos("  " + "a".repeat(500) + "  ")

        assertEquals(500, viewModel.uiState.value.largoObservaciones)
        assertTrue(viewModel.validarDatosMuestra())
    }

    @Test
    fun observaciones_alEditarSeBorraElError() {
        completarDatos("a".repeat(501))
        viewModel.validarDatosMuestra()

        viewModel.onObservacionesChange("a".repeat(500))

        assertNull(errorObservaciones())
        assertTrue(viewModel.validarDatosMuestra())
    }
}
