package com.example.aquasample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aquasample.ui.navigation.Rutas
import com.example.aquasample.viewmodel.LoginViewModel
import com.example.aquasample.viewmodel.MuestraViewModel

@Composable
fun ResumenScreen(
    loginViewModel: LoginViewModel,
    muestraViewModel: MuestraViewModel,
    navController: NavController
) {
    val loginEstado by loginViewModel.uiState.collectAsState()
    val estado by muestraViewModel.uiState.collectAsState()
    var errorEnvio by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Revisa los datos antes de enviar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilaDato("Centro", estado.centro?.nombre ?: "-")
                FilaDato("Tren", estado.tren?.nombre ?: "-")
                FilaDato("Línea", estado.linea?.nombre ?: "-")
                FilaDato("Fecha", estado.fecha)
                FilaDato("Hora", estado.hora)
                FilaDato("Tramo", "${estado.tramo.trim()} m")
                HorizontalDivider()
                FilaDato("Individuos", estado.individuos.toString())
                FilaDato("Calibre promedio", estado.calibre.trim().ifBlank { null }?.let { "$it mm" } ?: "No registrado")
                FilaDato("Observaciones", estado.observaciones.trim().ifBlank { "Sin observaciones" })
                HorizontalDivider()
                FilaDato("Operador", loginEstado.usuarioActual?.nombre ?: "-")
            }
        }

        errorEnvio?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                if (muestraViewModel.enviarARevision(loginEstado.usuarioActual)) {
                    // Vuelve a Inicio y saca Nueva muestra, Conteo y Resumen de la pila.
                    navController.navigate(Rutas.INICIO) {
                        popUpTo(Rutas.INICIO)
                        launchSingleTop = true
                    }
                } else {
                    errorEnvio = "No se pudo enviar. Vuelve atrás y corrige los campos marcados."
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("ENVIAR A REVISIÓN", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = { navController.popBackStack(Rutas.NUEVA_MUESTRA, inclusive = false) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("EDITAR DATOS")
        }
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = etiqueta,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp)
        )
        Text(text = valor, fontWeight = FontWeight.Medium)
    }
}
