package com.example.aquasample.viewmodel

import com.example.aquasample.data.DatosFicticios
import com.example.aquasample.data.MuestraRepository
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.Muestra
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldContainKeys
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Calendar
import java.util.Date

/** Fecha y hora en la zona horaria del equipo (como el reloj del teléfono). */
private fun momento(anio: Int, mes: Int, dia: Int, hora: Int, minuto: Int): Date =
    Calendar.getInstance().apply { clear(); set(anio, mes - 1, dia, hora, minuto) }.time

/** "Ahora" al iniciar cada prueba: 15 de octubre de 2026 a las 12:00. */
private val AHORA = momento(2026, 10, 15, 12, 0)

/**
 * Pruebas del formulario Nueva muestra → Conteo → Resumen y de la revisión del supervisor.
 * El repositorio se simula con MockK: así se prueba solo la lógica del ViewModel,
 * sin base de datos, y se puede verificar qué le pide el ViewModel al repositorio.
 */
class MuestraViewModelSpec : StringSpec({

    lateinit var repositorio: MuestraRepository
    lateinit var viewModel: MuestraViewModel
    // Reloj simulado: cada prueba puede "adelantar el tiempo" cambiando esta variable.
    var ahora = AHORA

    beforeTest {
        ahora = AHORA
        repositorio = mockk(relaxed = true)
        every { repositorio.muestras } returns MutableStateFlow(DatosFicticios.muestrasEjemplo)
        every { repositorio.siguienteId() } returns 6
        viewModel = MuestraViewModel(repositorio = repositorio, reloj = { ahora })
    }

    /** Completa la primera pantalla con datos válidos (la fecha y la hora se marcan solas). */
    fun llenarDatosValidos() {
        viewModel.seleccionarCentro(DatosFicticios.centros[0])
        viewModel.seleccionarTren(DatosFicticios.trenesDe("C1")[0])
        viewModel.seleccionarLinea(DatosFicticios.lineasDe("T1")[0])
        viewModel.onTramoChange("0,2")
    }

    /** Completa la pantalla de conteo con datos válidos. */
    fun llenarConteoValido() {
        viewModel.onIndividuosChange("310")
        viewModel.onCalibreChange("38.5")
        viewModel.onFotoChange("/fotos/muestra_1.jpg")
    }

    // ---------- Estado inicial y selección de centro / tren / línea ----------

    "el formulario parte con la fecha y la hora actuales" {
        viewModel.uiState.value.fecha shouldBe "2026-10-15"
        viewModel.uiState.value.hora shouldBe "12:00"
        viewModel.muestras.value shouldBe DatosFicticios.muestrasEjemplo
    }

    "elegir un centro carga sus trenes y borra el tren y la línea elegidos" {
        viewModel.seleccionarCentro(DatosFicticios.centros[0])
        viewModel.seleccionarTren(DatosFicticios.trenesDe("C1")[0])
        viewModel.seleccionarLinea(DatosFicticios.lineasDe("T1")[0])

        viewModel.seleccionarCentro(DatosFicticios.centros[1])

        val estado = viewModel.uiState.value
        estado.centro?.id shouldBe "C2"
        estado.trenesDisponibles shouldBe DatosFicticios.trenesDe("C2")
        estado.tren.shouldBeNull()
        estado.linea.shouldBeNull()
        estado.lineasDisponibles.shouldBeEmpty()
    }

    "elegir un tren carga solo sus líneas" {
        viewModel.seleccionarCentro(DatosFicticios.centros[0])
        viewModel.seleccionarTren(DatosFicticios.trenesDe("C1")[0])

        viewModel.uiState.value.lineasDisponibles shouldBe DatosFicticios.lineasDe("T1")
    }

    // ---------- Validaciones de la primera pantalla ----------

    "con el formulario vacío marca error en centro, tren, línea y tramo" {
        viewModel.validarDatosMuestra().shouldBeFalse()

        // La fecha y la hora nunca dan error: las pone el reloj del teléfono.
        viewModel.uiState.value.errores.keys shouldBe setOf(
            CampoMuestra.CENTRO, CampoMuestra.TREN, CampoMuestra.LINEA, CampoMuestra.TRAMO
        )
    }

    "con todos los datos correctos no hay errores" {
        llenarDatosValidos()

        viewModel.validarDatosMuestra().shouldBeTrue()

        viewModel.uiState.value.errores.shouldBeEmpty()
        viewModel.uiState.value.tramoMetros shouldBe 0.2 // acepta coma decimal
    }

    "el tramo debe ser un número mayor a cero" {
        llenarDatosValidos()

        for (tramoInvalido in listOf("0", "-1", "abc")) {
            viewModel.onTramoChange(tramoInvalido)
            viewModel.validarDatosMuestra().shouldBeFalse()
            viewModel.uiState.value.errores shouldContainKey CampoMuestra.TRAMO
        }
    }

    "al corregir un campo se borra su error" {
        viewModel.validarDatosMuestra()
        viewModel.uiState.value.errores shouldContainKey CampoMuestra.TRAMO

        viewModel.onTramoChange("0.3")

        viewModel.uiState.value.errores shouldNotContainKey CampoMuestra.TRAMO
    }

    // ---------- Fecha y hora automáticas ----------

    "marcar la fecha y hora toma el momento actual del reloj, con dos dígitos" {
        ahora = momento(2026, 11, 24, 8, 5)

        viewModel.marcarFechaHoraActual()

        viewModel.uiState.value.fecha shouldBe "2026-11-24"
        viewModel.uiState.value.hora shouldBe "08:05"
    }

    "al enviar, la muestra queda con la fecha y hora exactas del envío" {
        llenarDatosValidos()
        llenarConteoValido()
        val guardada = slot<Muestra>()
        every { repositorio.guardar(capture(guardada)) } returns Unit
        // Pasan unos minutos mientras el operador cuenta los choritos
        ahora = momento(2026, 10, 15, 12, 47)

        viewModel.enviarARevision(DatosFicticios.usuarios[0]).shouldBeTrue()

        guardada.captured.fecha shouldBe "2026-10-15"
        guardada.captured.hora shouldBe "12:47"
    }

    // ---------- Pantalla de conteo ----------

    "los botones + y - cambian el conteo y nunca baja de cero" {
        viewModel.sumarIndividuo()
        viewModel.sumarIndividuo()
        viewModel.uiState.value.individuos shouldBe 2

        viewModel.restarIndividuo()
        viewModel.restarIndividuo()
        viewModel.restarIndividuo()
        viewModel.uiState.value.individuos shouldBe 0
    }

    "al escribir el conteo se ignoran letras y se limita a 6 dígitos" {
        viewModel.onIndividuosChange("12a3")
        viewModel.uiState.value.individuos shouldBe 123

        viewModel.onIndividuosChange("12345678")
        viewModel.uiState.value.individuos shouldBe 123456

        viewModel.onIndividuosChange("")
        viewModel.uiState.value.individuos shouldBe 0
    }

    "el conteo exige individuos, calibre válido y una foto" {
        viewModel.onCalibreChange("-3")

        viewModel.validarConteo().shouldBeFalse()

        viewModel.uiState.value.errores.shouldContainKeys(
            CampoMuestra.INDIVIDUOS, CampoMuestra.CALIBRE, CampoMuestra.FOTO
        )
    }

    "el calibre es opcional y la foto se puede quitar" {
        llenarConteoValido()
        viewModel.onCalibreChange("")
        viewModel.validarConteo().shouldBeTrue()
        viewModel.uiState.value.calibreMm.shouldBeNull()

        viewModel.onFotoChange(null)
        viewModel.validarConteo().shouldBeFalse()
        viewModel.uiState.value.errores shouldContainKey CampoMuestra.FOTO
    }

    // ---------- Envío a revisión ----------

    "sin un operador con sesión iniciada no se envía la muestra" {
        llenarDatosValidos()
        llenarConteoValido()

        viewModel.enviarARevision(null).shouldBeFalse()

        verify(exactly = 0) { repositorio.guardar(any()) }
    }

    "con datos inválidos no se guarda nada en el repositorio" {
        viewModel.enviarARevision(DatosFicticios.usuarios[0]).shouldBeFalse()

        verify(exactly = 0) { repositorio.guardar(any()) }
    }

    "con datos válidos guarda la muestra PENDIENTE con todos sus datos" {
        llenarDatosValidos()
        llenarConteoValido()
        viewModel.onObservacionesChange("  Sin novedades  ")
        val guardada = slot<Muestra>()
        every { repositorio.guardar(capture(guardada)) } returns Unit

        viewModel.enviarARevision(DatosFicticios.usuarios[0]).shouldBeTrue()

        with(guardada.captured) {
            id shouldBe 6
            centro.id shouldBe "C1"
            tren.id shouldBe "T1"
            linea.id shouldBe "L1"
            fecha shouldBe "2026-10-15"
            hora shouldBe "12:00"
            tramoMetros shouldBe 0.2
            operador.id shouldBe "U1"
            conteo.individuos shouldBe 310
            conteo.calibrePromedioMm shouldBe 38.5
            observaciones shouldBe "Sin novedades"
            estado shouldBe EstadoRevision.PENDIENTE
            fotoUri shouldBe "/fotos/muestra_1.jpg"
        }
        viewModel.uiState.value.muestraEnviada.shouldNotBeNull()
    }

    "nueva muestra limpia el formulario" {
        llenarDatosValidos()
        llenarConteoValido()

        viewModel.nuevaMuestra()

        viewModel.uiState.value shouldBe MuestraUiState(fecha = "2026-10-15", hora = "12:00")
    }

    // ---------- Revisión del supervisor ----------

    "aprobar deja la muestra VALIDADO" {
        viewModel.aprobar(5)

        verify { repositorio.actualizarEstado(5, EstadoRevision.VALIDADO, null) }
    }

    "solicitar corrección deja la muestra OBSERVADO con el comentario sin espacios extra" {
        viewModel.solicitarCorreccion(5, "  Recontar la muestra  ")

        verify { repositorio.actualizarEstado(5, EstadoRevision.OBSERVADO, "Recontar la muestra") }
    }

    "marcar corregida conserva el comentario del supervisor" {
        viewModel.marcarCorregida(2)

        verify {
            repositorio.actualizarEstado(2, EstadoRevision.CORREGIDO, "Revisar el conteo, parece bajo para esta línea")
        }
    }

    "marcar corregida una muestra que no existe no hace nada" {
        viewModel.marcarCorregida(99)

        verify(exactly = 0) { repositorio.actualizarEstado(any(), any(), any()) }
    }
})
