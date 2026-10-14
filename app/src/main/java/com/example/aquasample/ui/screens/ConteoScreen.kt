package com.example.aquasample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aquasample.R
import com.example.aquasample.ui.components.CampoTexto
import com.example.aquasample.ui.navigation.Rutas
import com.example.aquasample.viewmodel.CampoMuestra
import com.example.aquasample.viewmodel.MuestraViewModel

@Composable
fun ConteoScreen(muestraViewModel: MuestraViewModel, navController: NavController) {
    val estado by muestraViewModel.uiState.collectAsState()
    val errorIndividuos = estado.errores[CampoMuestra.INDIVIDUOS]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Recordatorio de qué muestra se está contando.
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = listOfNotNull(estado.centro?.nombre, estado.tren?.nombre, estado.linea?.nombre)
                        .joinToString(" · "),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${estado.fecha} ${estado.hora} · tramo ${estado.tramo.trim()} m",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text(
            text = "Individuos contados",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = muestraViewModel::restarIndividuo,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(painterResource(R.drawable.ic_restar), contentDescription = "Restar uno")
            }
            OutlinedTextField(
                value = estado.individuos.toString(),
                onValueChange = muestraViewModel::onIndividuosChange,
                isError = errorIndividuos != null,
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = muestraViewModel::sumarIndividuo,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(painterResource(R.drawable.ic_agregar), contentDescription = "Sumar uno")
            }
        }
        errorIndividuos?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        CampoTexto(
            valor = estado.calibre,
            onCambio = muestraViewModel::onCalibreChange,
            etiqueta = "Calibre promedio en mm (opcional)",
            ejemplo = "Ej: 45",
            teclado = KeyboardType.Decimal,
            error = estado.errores[CampoMuestra.CALIBRE]
        )

        Button(
            onClick = {
                if (muestraViewModel.validarConteo()) navController.navigate(Rutas.RESUMEN)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("VER RESUMEN", fontWeight = FontWeight.Bold)
        }
    }
}
