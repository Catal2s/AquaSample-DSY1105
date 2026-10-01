package com.example.aquasample.model

/** Tren de cultivo. Cada tren pertenece a un centro. */
data class Tren(
    val id: String,
    val nombre: String,
    val centroId: String
)
