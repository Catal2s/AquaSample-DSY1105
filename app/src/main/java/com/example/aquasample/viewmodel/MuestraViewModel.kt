package com.example.aquasample.viewmodel

import androidx.lifecycle.ViewModel
import com.example.aquasample.data.DatosFicticios
import com.example.aquasample.data.MuestraRepository
import com.example.aquasample.model.Centro
import com.example.aquasample.model.Conteo
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.Linea
import com.example.aquasample.model.Muestra
import com.example.aquasample.model.Tren
import com.example.aquasample.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Campos que se validan, para mostrar el error debajo del campo correcto. */
enum class CampoMuestra { CENTRO, TREN, LINEA, FECHA, HORA, TRAMO, INDIVIDUOS, CALIBRE }

/**
 * Estado compartido por las pantallas Nueva muestra → Conteo → Resumen.
 * Los campos numéricos se guardan como texto tal cual los escribe el usuario
 * y se convierten recién al validar.
 */
data class MuestraUiState(
    val centros: List<Centro> = DatosFicticios.centros,
    val trenesDisponibles: List<Tren> = emptyList(),
    val lineasDisponibles: List<Linea> = emptyList(),
    val centro: Centro? = null,
    val tren: Tren? = null,
    val linea: Linea? = null,
    val fecha: String = "",
    val hora: String = "",
    val tramo: String = "",
    val individuos: Int = 0,
    val calibre: String = "",
    val observaciones: String = "",
    val errores: Map<CampoMuestra, String> = emptyMap(),
    val muestraEnviada: Muestra? = null
) {
    val tramoMetros: Double? get() = tramo.aNumero()
    val calibreMm: Double? get() = calibre.aNumero()
}

private fun String.aNumero(): Double? = trim().replace(',', '.').toDoubleOrNull()

class MuestraViewModel(
    private val repositorio: MuestraRepository = MuestraRepository.instancia,
    private val reloj: () -> Date = { Date() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(estadoInicial())
    val uiState: StateFlow<MuestraUiState> = _uiState.asStateFlow()

    /** Todas las muestras guardadas (para Inicio o la bandeja del supervisor). */
    val muestras: StateFlow<List<Muestra>> = repositorio.muestras

    // ---------- Pantalla: Nueva muestra ----------

    fun seleccionarCentro(centro: Centro) {
        _uiState.update {
            it.copy(
                centro = centro,
                trenesDisponibles = DatosFicticios.trenesDe(centro.id),
                tren = null,
                linea = null,
                lineasDisponibles = emptyList(),
                errores = it.errores - CampoMuestra.CENTRO - CampoMuestra.TREN - CampoMuestra.LINEA
            )
        }
    }

    fun seleccionarTren(tren: Tren) {
        _uiState.update {
            it.copy(
                tren = tren,
                linea = null,
                lineasDisponibles = DatosFicticios.lineasDe(tren.id),
                errores = it.errores - CampoMuestra.TREN - CampoMuestra.LINEA
            )
        }
    }

    fun seleccionarLinea(linea: Linea) {
        _uiState.update { it.copy(linea = linea, errores = it.errores - CampoMuestra.LINEA) }
    }

    fun onFechaChange(valor: String) {
        _uiState.update { it.copy(fecha = valor, errores = it.errores - CampoMuestra.FECHA) }
    }

    fun onHoraChange(valor: String) {
        _uiState.update { it.copy(hora = valor, errores = it.errores - CampoMuestra.HORA) }
    }

    fun onTramoChange(valor: String) {
        _uiState.update { it.copy(tramo = valor, errores = it.errores - CampoMuestra.TRAMO) }
    }

    fun onObservacionesChange(valor: String) {
        _uiState.update { it.copy(observaciones = valor) }
    }

    /**
     * Se llama al apretar "Siguiente" en Nueva muestra.
     * Si devuelve true, la pantalla puede navegar a Conteo.
     */
    fun validarDatosMuestra(): Boolean {
        val estado = _uiState.value
        val errores = mutableMapOf<CampoMuestra, String>()

        if (estado.centro == null) errores[CampoMuestra.CENTRO] = "Selecciona un centro"
        if (estado.tren == null) errores[CampoMuestra.TREN] = "Selecciona un tren"
        if (estado.linea == null) errores[CampoMuestra.LINEA] = "Selecciona una línea"
        if (!fechaValida(estado.fecha)) errores[CampoMuestra.FECHA] = "Usa el formato AAAA-MM-DD"
        if (!horaValida(estado.hora)) errores[CampoMuestra.HORA] = "Usa el formato HH:MM"

        val tramo = estado.tramoMetros
        if (tramo == null || tramo <= 0) errores[CampoMuestra.TRAMO] = "Ingresa un largo de tramo mayor a 0"

        _uiState.update { it.copy(errores = it.errores - DATOS_MUESTRA + errores) }
        return errores.isEmpty()
    }

    // ---------- Pantalla: Conteo ----------

    fun sumarIndividuo() {
        _uiState.update {
            it.copy(individuos = it.individuos + 1, errores = it.errores - CampoMuestra.INDIVIDUOS)
        }
    }

    fun restarIndividuo() {
        _uiState.update { it.copy(individuos = (it.individuos - 1).coerceAtLeast(0)) }
    }

    /** Para cuando el operador escribe el número directo en vez de usar +/-. */
    fun onIndividuosChange(valor: String) {
        val limpio = valor.filter { it.isDigit() }
        val numero = if (limpio.isEmpty()) 0 else limpio.take(6).toInt()
        _uiState.update { it.copy(individuos = numero, errores = it.errores - CampoMuestra.INDIVIDUOS) }
    }

    fun onCalibreChange(valor: String) {
        _uiState.update { it.copy(calibre = valor, errores = it.errores - CampoMuestra.CALIBRE) }
    }

    /** Se llama al apretar "Ver resumen". Si devuelve true, navegar a Resumen. */
    fun validarConteo(): Boolean {
        val estado = _uiState.value
        val errores = mutableMapOf<CampoMuestra, String>()

        if (estado.individuos <= 0) errores[CampoMuestra.INDIVIDUOS] = "El conteo debe ser mayor a 0"
        if (estado.calibre.isNotBlank()) {
            val calibre = estado.calibreMm
            if (calibre == null || calibre <= 0) errores[CampoMuestra.CALIBRE] = "El calibre debe ser un número mayor a 0"
        }

        _uiState.update { it.copy(errores = it.errores - DATOS_CONTEO + errores) }
        return errores.isEmpty()
    }

    // ---------- Pantalla: Resumen ----------

    /**
     * Guarda la muestra con estado PENDIENTE para que la revise el supervisor.
     * Recibe el usuario que inició sesión (viene del LoginViewModel).
     */
    fun enviarARevision(operador: Usuario?): Boolean {
        if (operador == null) return false
        val datosOk = validarDatosMuestra()
        val conteoOk = validarConteo()
        if (!datosOk || !conteoOk) return false

        val estado = _uiState.value
        val muestra = Muestra(
            id = repositorio.siguienteId(),
            centro = estado.centro!!,
            tren = estado.tren!!,
            linea = estado.linea!!,
            fecha = estado.fecha.trim(),
            hora = estado.hora.trim(),
            tramoMetros = estado.tramoMetros!!,
            operador = operador,
            conteo = Conteo(individuos = estado.individuos, calibrePromedioMm = estado.calibreMm),
            observaciones = estado.observaciones.trim()
        )
        repositorio.guardar(muestra)
        _uiState.update { it.copy(muestraEnviada = muestra) }
        return true
    }

    /** Limpia el formulario para registrar otra muestra. */
    fun nuevaMuestra() {
        _uiState.value = estadoInicial()
    }

    // ---------- Supervisor (para la bandeja, siguiente etapa) ----------

    fun aprobar(idMuestra: Int) {
        repositorio.actualizarEstado(idMuestra, EstadoRevision.VALIDADO)
    }

    fun solicitarCorreccion(idMuestra: Int, comentario: String) {
        repositorio.actualizarEstado(idMuestra, EstadoRevision.OBSERVADO, comentario.trim())
    }

    // ---------- Apoyo ----------

    private fun estadoInicial(): MuestraUiState {
        val ahora = reloj()
        return MuestraUiState(
            fecha = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(ahora),
            hora = SimpleDateFormat("HH:mm", Locale.US).format(ahora)
        )
    }

    private fun fechaValida(fecha: String): Boolean {
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        return Regex("""\d{4}-\d{2}-\d{2}""").matches(fecha.trim()) &&
            runCatching { formato.parse(fecha.trim()) }.getOrNull() != null
    }

    private fun horaValida(hora: String): Boolean {
        val partes = hora.trim().split(":")
        if (partes.size != 2 || partes.any { it.length != 2 || !it.all(Char::isDigit) }) return false
        return partes[0].toInt() in 0..23 && partes[1].toInt() in 0..59
    }

    private companion object {
        val DATOS_MUESTRA = setOf(
            CampoMuestra.CENTRO, CampoMuestra.TREN, CampoMuestra.LINEA,
            CampoMuestra.FECHA, CampoMuestra.HORA, CampoMuestra.TRAMO
        )
        val DATOS_CONTEO = setOf(CampoMuestra.INDIVIDUOS, CampoMuestra.CALIBRE)
    }
}
