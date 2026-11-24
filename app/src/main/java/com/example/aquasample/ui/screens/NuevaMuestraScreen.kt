package com.example.aquasample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aquasample.R
import com.example.aquasample.ui.components.CampoTexto
import com.example.aquasample.ui.components.Selector
import com.example.aquasample.ui.navigation.Rutas
import com.example.aquasample.viewmodel.CampoMuestra
import com.example.aquasample.viewmodel.MAX_OBSERVACIONES
import com.example.aquasample.viewmodel.MuestraViewModel

@Composable
fun NuevaMuestraScreen(muestraViewModel: MuestraViewModel, navController: NavController) {
    val estado by muestraViewModel.uiState.collectAsState()

    // Al abrir la pantalla se marca la fecha y hora actuales del teléfono.
    LaunchedEffect(Unit) { muestraViewModel.marcarFechaHoraActual() }

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

        // Fecha y hora: se marcan solas con el reloj del teléfono (no se escriben).
        FechaHoraAutomatica(fecha = estado.fecha, hora = estado.hora)

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
            error = estado.errores[CampoMuestra.OBSERVACIONES],
            unaLinea = false,
            minLineas = 3,
            ayuda = "${estado.largoObservaciones}/$MAX_OBSERVACIONES"
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

/**
 * Muestra la fecha y la hora del registro como información fija (no editable).
 * Se marcan automáticamente al abrir el formulario y se actualizan al enviar la muestra.
 */
@Composable
private fun FechaHoraAutomatica(fecha: String, hora: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_reloj),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text("Fecha y hora del registro", style = MaterialTheme.typography.labelMedium)
                Text("$fecha · $hora", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Se marcan automáticamente con el reloj del teléfono",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
