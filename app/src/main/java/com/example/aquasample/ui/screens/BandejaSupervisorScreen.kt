package com.example.aquasample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.ui.components.EstadoVacio
import com.example.aquasample.ui.components.TarjetaMuestra
import com.example.aquasample.ui.navigation.Rutas
import com.example.aquasample.viewmodel.LoginViewModel
import com.example.aquasample.viewmodel.MuestraViewModel

/**
 * Home del supervisor (wireframe 3): bandeja con las muestras que esperan revisión,
 * es decir, las Pendientes y las Corregidas por el operador.
 * Van de la más antigua a la más nueva, para revisarlas en el orden en que llegaron.
 */
@Composable
fun BandejaSupervisorScreen(
    loginViewModel: LoginViewModel,
    muestraViewModel: MuestraViewModel,
    navController: NavController
) {
    val loginEstado by loginViewModel.uiState.collectAsState()
    val todas by muestraViewModel.muestras.collectAsState()
    val usuario = loginEstado.usuarioActual ?: return

    val porRevisar = todas
        .filter { it.estado == EstadoRevision.PENDIENTE || it.estado == EstadoRevision.CORREGIDO }
        .sortedBy { it.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Hola, ${usuario.nombre}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when (porRevisar.size) {
                    0 -> "No tienes muestras por revisar"
                    1 -> "Tienes 1 muestra por revisar"
                    else -> "Tienes ${porRevisar.size} muestras por revisar"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (porRevisar.isEmpty()) {
            item {
                EstadoVacio(
                    titulo = "Bandeja al día",
                    mensaje = "Cuando un operador envíe o corrija una muestra, aparecerá aquí.",
                    textoAccion = "Ver historial",
                    onAccion = {
                        navController.navigate(Rutas.HISTORIAL) {
                            popUpTo(Rutas.BANDEJA)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        items(porRevisar, key = { it.id }) { muestra ->
            TarjetaMuestra(
                muestra = muestra,
                mostrarOperador = true,
                onClick = { navController.navigate(Rutas.detalle(muestra.id)) }
            )
        }
    }
}
