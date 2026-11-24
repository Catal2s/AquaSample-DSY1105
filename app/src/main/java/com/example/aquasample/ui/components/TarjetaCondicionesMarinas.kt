package com.example.aquasample.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aquasample.R
import com.example.aquasample.model.CondicionesMarinas
import com.example.aquasample.model.Muestra
import com.example.aquasample.model.puntoCardinal
import com.example.aquasample.viewmodel.CondicionesMarinasUiState
import com.example.aquasample.viewmodel.CondicionesMarinasViewModel
import java.util.Locale

/**
 * Condiciones marinas de referencia de la muestra (API externa Open-Meteo Marine).
 * Maneja sus propios estados: si la API falla, el resto del detalle no se ve afectado.
 */
@Composable
fun TarjetaCondicionesMarinas(
    muestra: Muestra,
    viewModel: CondicionesMarinasViewModel = viewModel(key = "marina-${muestra.id}")
) {
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(muestra.centro.id, muestra.fecha, muestra.hora) {
        viewModel.cargar(muestra)
    }

    ContenidoCondicionesMarinas(estado, onReintentar = viewModel::reintentar)
}

@Composable
private fun ContenidoCondicionesMarinas(
    estado: CondicionesMarinasUiState,
    onReintentar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Condiciones marinas de referencia",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            when (estado) {
                CondicionesMarinasUiState.Cargando -> Cargando()
                is CondicionesMarinasUiState.Exito -> Datos(estado.condiciones)
                is CondicionesMarinasUiState.Error -> Falla(estado.mensaje, onReintentar)
                CondicionesMarinasUiState.SinConexion -> Falla(
                    "Sin conexión. Conéctate a internet para ver las condiciones marinas.",
                    onReintentar
                )
            }
            Text(
                text = "Datos de referencia estimados por Open-Meteo Marine · no son mediciones del centro",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Cargando() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        Text("Consultando condiciones marinas…")
    }
}

@Composable
private fun Falla(mensaje: String, onReintentar: () -> Unit) {
    Text(text = mensaje, color = MaterialTheme.colorScheme.error)
    OutlinedButton(onClick = onReintentar, modifier = Modifier.fillMaxWidth()) {
        Text("Reintentar")
    }
}

@Composable
private fun Datos(condiciones: CondicionesMarinas) {
    FilaCondicion(R.drawable.ic_marina_ola, "Altura de ola", condiciones.alturaOlaM.conUnidad(2, "m"))
    FilaCondicion(R.drawable.ic_reloj, "Período", condiciones.periodoOlaS.conUnidad(1, "s"))
    FilaCondicion(
        R.drawable.ic_marina_direccion,
        "Olas desde",
        condiciones.direccionOlaGrados
            ?.let { "${puntoCardinal(it)} (${it.conUnidad(0, "°", separador = "")})" }
            ?: SIN_DATO
    )
    FilaCondicion(
        R.drawable.ic_marina_temperatura,
        "Temp. del mar",
        condiciones.temperaturaMarC.conUnidad(1, "°C")
    )
    HorizontalDivider()
    Text(
        text = "Fecha consultada: ${condiciones.fecha}, ${condiciones.hora} h",
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = String.format(
            Locale.US,
            "Ubicación aproximada: lat %.2f, lon %.2f",
            condiciones.latitud,
            condiciones.longitud
        ),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun FilaCondicion(@DrawableRes icono: Int, etiqueta: String, valor: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(icono),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        FilaDato(etiqueta, valor)
    }
}

private const val SIN_DATO = "Sin dato"

private fun Double?.conUnidad(decimales: Int, unidad: String, separador: String = " "): String =
    if (this == null) SIN_DATO else String.format(Locale.US, "%.${decimales}f", this) + separador + unidad
