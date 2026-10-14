package com.example.aquasample.model

/** Línea de cultivo. Cada línea pertenece a un tren. */
data class Linea(
    val id: String,
    val nombre: String,
    val trenId: String
)
