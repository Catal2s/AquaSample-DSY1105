package com.example.aquasample.data

import com.example.aquasample.model.Centro
import com.example.aquasample.model.Conteo
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.Linea
import com.example.aquasample.model.Muestra
import com.example.aquasample.model.Rol
import com.example.aquasample.model.Tren
import com.example.aquasample.model.Usuario

/**
 * Datos de prueba escritos directamente en el código.
 * Todo es ficticio: no hay nombres, centros ni registros reales de ALDEMAR.
 */
object DatosFicticios {

    val usuarios = listOf(
        Usuario(id = "U1", nombre = "Operador Demo 01", usuario = "operador1", clave = "1234", rol = Rol.OPERADOR),
        Usuario(id = "U2", nombre = "Operador Demo 02", usuario = "operador2", clave = "1234", rol = Rol.OPERADOR),
        Usuario(id = "U3", nombre = "Supervisor Demo", usuario = "supervisor1", clave = "1234", rol = Rol.SUPERVISOR)
    )

    val centros = listOf(
        Centro(id = "C1", nombre = "Centro Ficticio Norte"),
        Centro(id = "C2", nombre = "Centro Ficticio Sur")
    )

    val trenes = listOf(
        Tren(id = "T1", nombre = "Tren 1", centroId = "C1"),
        Tren(id = "T2", nombre = "Tren 2", centroId = "C1"),
        Tren(id = "T3", nombre = "Tren 1", centroId = "C2")
    )

    val lineas = listOf(
        Linea(id = "L1", nombre = "Línea 1", trenId = "T1"),
        Linea(id = "L2", nombre = "Línea 2", trenId = "T1"),
        Linea(id = "L3", nombre = "Línea 1", trenId = "T2"),
        Linea(id = "L4", nombre = "Línea 1", trenId = "T3"),
        Linea(id = "L5", nombre = "Línea 2", trenId = "T3")
    )

    /**
     * Muestras ya registradas para que Inicio e Historial no partan vacíos
     * en la demostración. Incluyen todos los estados de revisión.
     */
    val muestrasEjemplo: List<Muestra> by lazy {
        fun muestra(
            id: Int, lineaId: String, fecha: String, hora: String, tramo: Double,
            operadorId: String, individuos: Int, calibre: Double?, observaciones: String,
            estado: EstadoRevision, comentario: String? = null
        ): Muestra {
            val linea = lineas.first { it.id == lineaId }
            val tren = trenes.first { it.id == linea.trenId }
            return Muestra(
                id = id,
                centro = centros.first { it.id == tren.centroId },
                tren = tren,
                linea = linea,
                fecha = fecha,
                hora = hora,
                tramoMetros = tramo,
                operador = usuarios.first { it.id == operadorId },
                conteo = Conteo(individuos = individuos, calibrePromedioMm = calibre),
                observaciones = observaciones,
                estado = estado,
                comentarioSupervisor = comentario
            )
        }
        listOf(
            muestra(1, "L1", "2026-09-22", "09:15", 0.2, "U1", 312, 38.5, "Muestra sin novedades", EstadoRevision.VALIDADO),
            muestra(2, "L2", "2026-09-22", "10:40", 0.2, "U1", 287, null, "", EstadoRevision.OBSERVADO, "Revisar el conteo, parece bajo para esta línea"),
            muestra(3, "L4", "2026-09-24", "11:05", 0.3, "U2", 401, 42.0, "Alta densidad de individuos", EstadoRevision.VALIDADO),
            muestra(4, "L3", "2026-09-29", "08:50", 0.2, "U2", 265, null, "Tramo con algo de fouling", EstadoRevision.CORREGIDO),
            muestra(5, "L5", "2026-10-01", "09:30", 0.2, "U1", 298, 40.2, "", EstadoRevision.PENDIENTE)
        )
    }

    fun trenesDe(centroId: String): List<Tren> = trenes.filter { it.centroId == centroId }

    fun lineasDe(trenId: String): List<Linea> = lineas.filter { it.trenId == trenId }
}
