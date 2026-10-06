package com.example.aquasample.data

import com.example.aquasample.model.Centro
import com.example.aquasample.model.Linea
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

    fun trenesDe(centroId: String): List<Tren> = trenes.filter { it.centroId == centroId }

    fun lineasDe(trenId: String): List<Linea> = lineas.filter { it.trenId == trenId }
}
