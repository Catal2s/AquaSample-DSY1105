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
import java.util.TimeZone

/** Campos que se validan, para mostrar el error debajo del campo correcto. */
enum class CampoMuestra { CENTRO, TREN, LINEA, FECHA, HORA, TRAMO, INDIVIDUOS, CALIBRE, FOTO }

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
    val fotoUri: String? = null,
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

    /** Recibe el día elegido en el calendario (DatePicker), que viene en milisegundos UTC. */
    fun onFechaElegida(millisUtc: Long) {
        onFechaChange(formatoFecha(utc = true).format(Date(millisUtc)))
    }

    /** Recibe la hora elegida en el reloj (TimePicker). */
    fun onHoraElegida(hora: Int, minuto: Int) {
        onHoraChange(String.format(Locale.US, "%02d:%02d", hora, minuto))
    }

    /** Fecha del formulario en milisegundos UTC, para que el calendario parta en ese día. */
    fun fechaEnMillis(): Long? = fechaAMillisUtc(_uiState.value.fecha)

    /** Hoy en milisegundos UTC: el calendario no deja elegir días posteriores. */
    fun hoyEnMillis(): Long = fechaAMillisUtc(hoy()) ?: Long.MAX_VALUE

    /** Hora y minuto del formulario, para que el reloj parta en esa hora. */
    fun horaYMinuto(): Pair<Int, Int> {
        val hora = _uiState.value.hora.trim()
        if (!horaValida(hora)) return 0 to 0
        val partes = hora.split(":")
        return partes[0].toInt() to partes[1].toInt()
    }

    fun onTramoChange(valor: String) {
        _uiState.update { it.copy(tramo = valor, errores = it.errores - CampoMuestra.TRAMO) }
    }

    fun onObservacionesChange(valor: String) {
        _uiState.update { it.copy(observaciones = valor) }
    }

    /* Guarda la ruta de la foto elegida (o null si se quitó) y borra el error de foto.*/
    fun onFotoChange(ruta: String?){
        _uiState.update { it.copy(fotoUri = ruta, errores = it.errores - CampoMuestra.FOTO) }
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
        // La muestra se registra en terreno, así que no puede tener fecha ni hora futuras.
        // Como el formato es AAAA-MM-DD y HH:MM, se pueden comparar como texto.
        val hoy = hoy()
        val fecha = estado.fecha.trim()
        val hora = estado.hora.trim()
        if (!fechaValida(fecha)) errores[CampoMuestra.FECHA] = "Usa el formato AAAA-MM-DD"
        else if (fecha > hoy) errores[CampoMuestra.FECHA] = "La fecha no puede ser posterior a hoy"
        if (!horaValida(hora)) errores[CampoMuestra.HORA] = "Usa el formato HH:MM"
        else if (fecha == hoy && hora > horaActual()) errores[CampoMuestra.HORA] = "La hora no puede ser posterior a la actual"

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
            observaciones = estado.observaciones.trim(),
            fotoUri = estado.fotoUri
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

    /** El operador avisa que corrigió una muestra observada; vuelve a la bandeja del supervisor. */
    fun marcarCorregida(idMuestra: Int) {
        val actual = muestras.value.find { it.id == idMuestra } ?: return
        repositorio.actualizarEstado(idMuestra, EstadoRevision.CORREGIDO, actual.comentarioSupervisor)
    }

    // ---------- Apoyo ----------

    /** La fecha y la hora parten con el momento actual del teléfono. */
    private fun estadoInicial(): MuestraUiState = MuestraUiState(fecha = hoy(), hora = horaActual())

    private fun hoy(): String = formatoFecha().format(reloj())

    private fun horaActual(): String = SimpleDateFormat("HH:mm", Locale.US).format(reloj())

    /** El DatePicker trabaja en UTC; el resto de la app en la zona horaria del teléfono. */
    private fun formatoFecha(utc: Boolean = false) = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
        if (utc) timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun fechaAMillisUtc(fecha: String): Long? =
        if (fechaValida(fecha)) runCatching { formatoFecha(utc = true).parse(fecha.trim())?.time }.getOrNull()
        else null

    private fun fechaValida(fecha: String): Boolean =
        Regex("""\d{4}-\d{2}-\d{2}""").matches(fecha.trim()) &&
            runCatching { formatoFecha().parse(fecha.trim()) }.getOrNull() != null

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
