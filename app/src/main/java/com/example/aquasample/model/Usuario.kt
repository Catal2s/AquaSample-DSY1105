package com.example.aquasample.model

/**
 * Usuario ficticio de la app. La clave va en texto plano porque
 * en esta etapa no se pide autenticación real (solo datos de prueba).
 */
data class Usuario(
    val id: String,
    val nombre: String,
    val usuario: String,
    val clave: String,
    val rol: Rol
)
