package com.example.aquasample.ui.navigation

import com.example.aquasample.model.Rol
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/** Cada rol parte en su pantalla principal después del login. */
class RutasSpec : StringSpec({

    "el operador parte en Inicio" {
        Rutas.inicioDe(Rol.OPERADOR) shouldBe Rutas.INICIO
    }

    "el supervisor parte en su Bandeja de revisión" {
        Rutas.inicioDe(Rol.SUPERVISOR) shouldBe Rutas.BANDEJA
    }

    "el analista parte en el Dashboard" {
        Rutas.inicioDe(Rol.ANALISTA) shouldBe Rutas.DASHBOARD
    }

    "sin sesión iniciada se usa Inicio" {
        Rutas.inicioDe(null) shouldBe Rutas.INICIO
    }

    "el detalle lleva el id de la muestra en la ruta" {
        Rutas.detalle(7) shouldBe "detalle/7"
    }
})
