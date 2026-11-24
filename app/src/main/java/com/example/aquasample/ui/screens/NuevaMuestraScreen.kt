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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aquasample.R
import com.example.aquasample.ui.components.CampoSeleccion
import com.example.aquasample.ui.components.CampoTexto
import com.example.aquasample.ui.components.DialogoFecha
import com.example.aquasample.ui.components.DialogoHora
import com.example.aquasample.ui.components.Selector
import com.example.aquasample.ui.navigation.Rutas
import com.example.aquasample.viewmodel.CampoMuestra
import com.example.aquasample.viewmodel.MuestraViewModel

@Composable
fun NuevaMuestraScreen(muestraViewModel: MuestraViewModel, navController: NavController) {
    val estado by muestraViewModel.uiState.collectAsState()
    // Solo indican si el diálogo está abierto (estado de la vista, no del formulario).
    var mostrarCalendario by rememberSaveable { mutableStateOf(false) }
    var mostrarReloj by rememberSaveable { mutableStateOf(false) }

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

        // Fecha y hora parten con las del teléfono; al tocarlas se abre el calendario o el reloj.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoSeleccion(
                valor = estado.fecha,
                etiqueta = "Fecha",
                icono = R.drawable.ic_calendario,
                onClick = { mostrarCalendario = true },
                error = estado.errores[CampoMuestra.FECHA],
                modifier = Modifier.weight(1f)
            )
            CampoSeleccion(
                valor = estado.hora,
                etiqueta = "Hora",
                icono = R.drawable.ic_reloj,
                onClick = { mostrarReloj = true },
                error = estado.errores[CampoMuestra.HORA],
                modifier = Modifier.weight(1f)
            )
        }

        if (mostrarCalendario) {
            DialogoFecha(
                fechaInicialMillis = muestraViewModel.fechaEnMillis(),
                hoyMillis = muestraViewModel.hoyEnMillis(),
                onConfirmar = muestraViewModel::onFechaElegida,
                onCerrar = { mostrarCalendario = false }
            )
        }
        if (mostrarReloj) {
            val (hora, minuto) = muestraViewModel.horaYMinuto()
            DialogoHora(
                horaInicial = hora,
                minutoInicial = minuto,
                onConfirmar = muestraViewModel::onHoraElegida,
                onCerrar = { mostrarReloj = false }
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
