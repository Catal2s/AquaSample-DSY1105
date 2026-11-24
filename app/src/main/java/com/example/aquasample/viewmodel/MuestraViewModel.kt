package com.example.aquasample.viewmodel

import androidx.lifecycle.ViewModel
import com.example.aquasample.data.DatosFicticios
import com.example.aquasample.data.MuestraRepository
import com.example.aquasample.model.Centro
import com.example.aquasample.model.Conteo
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.Linea
import com.example.aquasample.model.Muestra
import com.example.aquasample.model.Rol
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
enum class CampoMuestra { CENTRO, TREN, LINEA, TRAMO, INDIVIDUOS, CALIBRE, FOTO }

/** Marca táctil sobre la fotografía para el conteo asistido. Coordenadas normalizadas (0.0 a 1.0). */
data class PuntoMarca(
    val id: Int,
    val xRel: Float,
    val yRel: Float
)

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
    val marcasPuntos: List<PuntoMarca> = emptyList(),
    val modoMarcadoActivo: Boolean = true,
    val errores: Map<CampoMuestra, String> = emptyMap(),
    val muestraEnviada: Muestra? = null
) {
    val tramoMetros: Double? get() = tramo.aNumero()
    val calibreMm: Double? get() = calibre.aNumero()
    val totalMarcas: Int get() = marcasPuntos.size
    val ajusteManual: Int get() = (individuos - marcasPuntos.size).coerceAtLeast(0)
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

    /**
     * La fecha y la hora no se escriben: se marcan solas con el reloj del teléfono.
     * Se llama al abrir Nueva muestra y otra vez al enviar, así la muestra queda
     * con el momento exacto en que se registró y el operador no puede equivocarse.
     */
    fun marcarFechaHoraActual() {
        _uiState.update { it.copy(fecha = hoy(), hora = horaActual()) }
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
        // La fecha y la hora no se validan: las pone el reloj del teléfono (marcarFechaHoraActual).

        val tramo = estado.tramoMetros
        if (tramo == null || tramo <= 0) errores[CampoMuestra.TRAMO] = "Ingresa un largo de tramo mayor a 0"

        _uiState.update { it.copy(errores = it.errores - DATOS_MUESTRA + errores) }
        return errores.isEmpty()
    }

    // ---------- Pantalla: Conteo Asistido y Manual ----------

    /** Agrega una marca táctil sobre la fotografía e incrementa el total de individuos. */
    fun agregarMarca(xRel: Float, yRel: Float) {
        _uiState.update { estado ->
            val nuevoId = (estado.marcasPuntos.maxOfOrNull { it.id } ?: 0) + 1
            val punto = PuntoMarca(nuevoId, xRel.coerceIn(0f, 1f), yRel.coerceIn(0f, 1f))
            val nuevas = estado.marcasPuntos + punto
            estado.copy(
                marcasPuntos = nuevas,
                individuos = nuevas.size,
                errores = estado.errores - CampoMuestra.INDIVIDUOS
            )
        }
    }

    /** Deshace la última marca realizada en la foto y sincroniza el contador. */
    fun deshacerUltimaMarca() {
        _uiState.update { estado ->
            if (estado.marcasPuntos.isEmpty()) return@update estado
            val nuevas = estado.marcasPuntos.dropLast(1)
            estado.copy(
                marcasPuntos = nuevas,
                individuos = nuevas.size
            )
        }
    }

    /** Limpia todas las marcas registradas en la fotografía. */
    fun limpiarMarcas() {
        _uiState.update { it.copy(marcasPuntos = emptyList(), individuos = 0) }
    }

    /** Activa o desactiva el modo de marcado táctil sobre la fotografía. */
    fun alternarModoMarcado() {
        _uiState.update { it.copy(modoMarcadoActivo = !it.modoMarcadoActivo) }
    }

    fun sumarIndividuo() {
        _uiState.update {
            it.copy(individuos = it.individuos + 1, errores = it.errores - CampoMuestra.INDIVIDUOS)
        }
    }

    fun restarIndividuo() {
        _uiState.update { it.copy(individuos = (it.individuos - 1).coerceAtLeast(0)) }
    }

    /** Para cuando el operador escribe el número directo en vez de usar +/- o después de marcar. */
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

        if (estado.fotoUri == null) {
            errores[CampoMuestra.FOTO] = "Adjunta o toma una foto de la muestra"
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

        // Se registra el momento exacto del envío.
        marcarFechaHoraActual()
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
            conteo = Conteo(
                individuos = estado.individuos,
                calibrePromedioMm = estado.calibreMm,
                asistidoPorFoto = estado.marcasPuntos.isNotEmpty(),
                marcasRegistradas = estado.marcasPuntos.size,
                ajusteManual = (estado.individuos - estado.marcasPuntos.size).coerceAtLeast(0)
            ),
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

    fun aprobar(idMuestra: Int, rol: Rol = Rol.SUPERVISOR) {
        repositorio.actualizarEstado(idMuestra, EstadoRevision.VALIDADO, rol = rol)
    }

    fun solicitarCorreccion(idMuestra: Int, comentario: String, rol: Rol = Rol.SUPERVISOR) {
        repositorio.actualizarEstado(idMuestra, EstadoRevision.OBSERVADO, comentario.trim(), rol)
    }

    /** El operador avisa que corrigió una muestra observada; vuelve a la bandeja del supervisor. */
    fun marcarCorregida(idMuestra: Int, rol: Rol = Rol.OPERADOR) {
        val actual = muestras.value.find { it.id == idMuestra } ?: return
        repositorio.actualizarEstado(idMuestra, EstadoRevision.CORREGIDO, actual.comentarioSupervisor, rol)
    }

    /** Solo el supervisor puede eliminar (el backend también lo exige). */
    fun eliminar(idMuestra: Int, rol: Rol) {
        if (rol == Rol.SUPERVISOR) repositorio.eliminar(idMuestra, rol)
    }

    // ---------- Apoyo ----------

    /** La fecha y la hora parten con el momento actual del teléfono. */
    private fun estadoInicial(): MuestraUiState = MuestraUiState(fecha = hoy(), hora = horaActual())

    /** Fecha del teléfono en formato AAAA-MM-DD (el mismo que usa el backend). */
    private fun hoy(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(reloj())

    /** Hora del teléfono en formato HH:mm (24 horas). */
    private fun horaActual(): String = SimpleDateFormat("HH:mm", Locale.US).format(reloj())

    private companion object {
        val DATOS_MUESTRA = setOf(CampoMuestra.CENTRO, CampoMuestra.TREN, CampoMuestra.LINEA, CampoMuestra.TRAMO)
        val DATOS_CONTEO = setOf(CampoMuestra.INDIVIDUOS, CampoMuestra.CALIBRE, CampoMuestra.FOTO)
    }
}
