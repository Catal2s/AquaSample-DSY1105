package com.example.aquasample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aquasample.model.Rol
import com.example.aquasample.ui.components.TarjetaMuestra
import com.example.aquasample.ui.navigation.abrirNuevaMuestra
import com.example.aquasample.viewmodel.LoginViewModel
import com.example.aquasample.viewmodel.MuestraViewModel

@Composable
fun InicioScreen(
    loginViewModel: LoginViewModel,
    muestraViewModel: MuestraViewModel,
    navController: NavController
) {
    val loginEstado by loginViewModel.uiState.collectAsState()
    val todas by muestraViewModel.muestras.collectAsState()
    val usuario = loginEstado.usuarioActual ?: return
    val esOperador = usuario.rol == Rol.OPERADOR

    // El operador ve solo sus envíos; el supervisor ve todas las muestras recibidas.
    val muestras = todas
        .filter { !esOperador || it.operador.id == usuario.id }
        .sortedByDescending { it.id }

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
                text = usuario.rol.etiqueta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (esOperador) {
            item {
                Button(
                    onClick = { navController.abrirNuevaMuestra(muestraViewModel) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("REGISTRAR NUEVA MUESTRA", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text(
                text = if (esOperador) "Mis envíos" else "Muestras recibidas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (muestras.isEmpty()) {
            item {
                Text(
                    text = if (esOperador) "Todavía no has enviado muestras." else "No hay muestras para revisar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(muestras, key = { it.id }) { muestra ->
            TarjetaMuestra(muestra, mostrarOperador = !esOperador)
        }
    }
}
