package com.example.aquasample.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquasample.data.DatosFicticios
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.Rol
import com.example.aquasample.ui.components.TarjetaMuestra
import com.example.aquasample.viewmodel.HistorialViewModel
import com.example.aquasample.viewmodel.LoginViewModel

@Composable
fun HistorialScreen(loginViewModel: LoginViewModel, historialViewModel: HistorialViewModel) {
    val loginEstado by loginViewModel.uiState.collectAsState()
    val estado by historialViewModel.uiState.collectAsState()
    val usuario = loginEstado.usuarioActual ?: return
    val esOperador = usuario.rol == Rol.OPERADOR

    // El operador solo consulta sus muestras; el supervisor consulta todas.
    LaunchedEffect(usuario.id) {
        historialViewModel.limitarAOperador(if (esOperador) usuario.id else null)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (esOperador) "Mis muestras registradas" else "Todas las muestras",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Filtra por centro, línea o estado de revisión",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            GrupoFiltros(titulo = "Centro") {
                FilterChip(
                    selected = estado.filtros.centro == null,
                    onClick = { historialViewModel.filtrarPorCentro(null) },
                    label = { Text("Todos") }
                )
                estado.centros.forEach { centro ->
                    FilterChip(
                        selected = estado.filtros.centro?.id == centro.id,
                        onClick = { historialViewModel.filtrarPorCentro(centro) },
                        label = { Text(centro.nombre) }
                    )
                }
            }
        }

        // Las líneas aparecen recién al elegir un centro, porque cada línea pertenece a un tren.
        if (estado.lineasDisponibles.isNotEmpty()) {
            item {
                GrupoFiltros(titulo = "Línea") {
                    estado.lineasDisponibles.forEach { linea ->
                        val tren = DatosFicticios.trenes.firstOrNull { it.id == linea.trenId }
                        FilterChip(
                            selected = estado.filtros.linea?.id == linea.id,
                            onClick = { historialViewModel.filtrarPorLinea(linea) },
                            label = { Text(listOfNotNull(tren?.nombre, linea.nombre).joinToString(" · ")) }
                        )
                    }
                }
            }
        }

        item {
            GrupoFiltros(titulo = "Estado") {
                EstadoRevision.entries.forEach { estadoRevision ->
                    FilterChip(
                        selected = estado.filtros.estado == estadoRevision,
                        onClick = { historialViewModel.filtrarPorEstado(estadoRevision) },
                        label = { Text(estadoRevision.etiqueta) }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${estado.muestras.size} muestras · ${estado.totalIndividuos} individuos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (estado.hayFiltros) {
                    TextButton(onClick = historialViewModel::limpiarFiltros) {
                        Text("Limpiar filtros")
                    }
                }
            }
        }

        if (estado.muestras.isEmpty()) {
            item {
                Text(
                    text = "No hay muestras que coincidan con los filtros.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(estado.muestras, key = { it.id }) { muestra ->
            TarjetaMuestra(muestra, mostrarOperador = !esOperador)
        }
    }
}

/** Título y fila de chips con desplazamiento horizontal, para que quepa en pantallas angostas. */
@Composable
private fun GrupoFiltros(titulo: String, chips: @Composable () -> Unit) {
    Column {
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chips()
        }
    }
}
