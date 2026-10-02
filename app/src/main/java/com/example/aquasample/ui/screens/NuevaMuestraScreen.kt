package com.example.aquasample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aquasample.ui.components.CampoTexto
import com.example.aquasample.ui.components.Selector
import com.example.aquasample.ui.navigation.Rutas
import com.example.aquasample.viewmodel.CampoMuestra
import com.example.aquasample.viewmodel.MuestraViewModel

@Composable
fun NuevaMuestraScreen(muestraViewModel: MuestraViewModel, navController: NavController) {
    val estado by muestraViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Selector(
            etiqueta = "Centro",
            opciones = estado.centros,
            seleccionado = estado.centro,
            textoDe = { it.nombre },
            onSeleccionar = muestraViewModel::seleccionarCentro,
            error = estado.errores[CampoMuestra.CENTRO],
            textoSinOpciones = "No hay centros disponibles"
        )
        Selector(
            etiqueta = "Tren",
            opciones = estado.trenesDisponibles,
            seleccionado = estado.tren,
            textoDe = { it.nombre },
            onSeleccionar = muestraViewModel::seleccionarTren,
            error = estado.errores[CampoMuestra.TREN],
            textoSinOpciones = "Primero selecciona un centro"
        )
        Selector(
            etiqueta = "Línea",
            opciones = estado.lineasDisponibles,
            seleccionado = estado.linea,
            textoDe = { it.nombre },
            onSeleccionar = muestraViewModel::seleccionarLinea,
            error = estado.errores[CampoMuestra.LINEA],
            textoSinOpciones = "Primero selecciona un tren"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoTexto(
                valor = estado.fecha,
                onCambio = muestraViewModel::onFechaChange,
                etiqueta = "Fecha",
                ejemplo = "AAAA-MM-DD",
                error = estado.errores[CampoMuestra.FECHA],
                modifier = Modifier.weight(1f)
            )
            CampoTexto(
                valor = estado.hora,
                onCambio = muestraViewModel::onHoraChange,
                etiqueta = "Hora",
                ejemplo = "HH:MM",
                error = estado.errores[CampoMuestra.HORA],
                modifier = Modifier.weight(1f)
            )
        }

        CampoTexto(
            valor = estado.tramo,
            onCambio = muestraViewModel::onTramoChange,
            etiqueta = "Tramo muestreado (m)",
            ejemplo = "Ej: 1,5",
            teclado = KeyboardType.Decimal,
            error = estado.errores[CampoMuestra.TRAMO]
        )

        CampoTexto(
            valor = estado.observaciones,
            onCambio = muestraViewModel::onObservacionesChange,
            etiqueta = "Observaciones (opcional)",
            error = null,
            unaLinea = false,
            minLineas = 3
        )

        Button(
            onClick = {
                if (muestraViewModel.validarDatosMuestra()) navController.navigate(Rutas.CONTEO)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(52.dp)
        ) {
            Text("SIGUIENTE: CONTEO", fontWeight = FontWeight.Bold)
        }
    }
}
