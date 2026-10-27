package com.example.aquasample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.Rol
import com.example.aquasample.ui.components.EstadoVacio
import com.example.aquasample.ui.components.EtiquetaEstado
import com.example.aquasample.ui.components.FilaDato
import com.example.aquasample.viewmodel.LoginViewModel
import com.example.aquasample.viewmodel.MuestraViewModel
import com.example.aquasample.ui.components.FotoMuestra

/**
 * Detalle de una muestra, al tocarla en Inicio, Historial o la bandeja del supervisor.
 * Recibe solo el id (viene en la ruta) y busca la muestra en la lista del ViewModel,
 * así que si el supervisor cambia el estado, el detalle se actualiza solo.
 */
@Composable
fun DetalleMuestraScreen(
    idMuestra: Int,
    muestraViewModel: MuestraViewModel,
    loginViewModel: LoginViewModel
) {
    val muestras by muestraViewModel.muestras.collectAsState()
    val loginEstado by loginViewModel.uiState.collectAsState()
    val usuario = loginEstado.usuarioActual
    val muestra = muestras.find { it.id == idMuestra }

    if (muestra == null) {
        EstadoVacio(
            titulo = "Muestra no encontrada",
            mensaje = "No existe una muestra con el número #$idMuestra."
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Muestra #${muestra.id}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            EtiquetaEstado(muestra.estado)
        }

        FotoMuestra(muestra.fotoUri)

        SeccionDetalle("Ubicación y fecha") {
            FilaDato("Centro", muestra.centro.nombre)
            FilaDato("Tren", muestra.tren.nombre)
            FilaDato("Línea", muestra.linea.nombre)
            FilaDato("Fecha", muestra.fecha)
            FilaDato("Hora", muestra.hora)
            FilaDato("Tramo", "${muestra.tramoMetros} m")
        }

        SeccionDetalle("Conteo manual") {
            FilaDato("Individuos", muestra.conteo.individuos.toString())
            FilaDato("Calibre promedio", muestra.conteo.calibrePromedioMm?.let { "$it mm" } ?: "No registrado")
            FilaDato("Observaciones", muestra.observaciones.ifBlank { "Sin observaciones" })
        }

        SeccionDetalle("Registro y revisión") {
            FilaDato("Operador", muestra.operador.nombre)
            FilaDato("Estado", muestra.estado.etiqueta)
            FilaDato("Comentario", muestra.comentarioSupervisor?.ifBlank { null } ?: "Sin comentarios del supervisor")
        }

        // Supervisor: aprueba o pide corrección de las muestras que esperan revisión.
        val porRevisar = muestra.estado == EstadoRevision.PENDIENTE || muestra.estado == EstadoRevision.CORREGIDO
        if (usuario?.rol == Rol.SUPERVISOR && porRevisar) {
            PanelRevision(
                onAprobar = { muestraViewModel.aprobar(muestra.id) },
                onSolicitarCorreccion = { comentario ->
                    muestraViewModel.solicitarCorreccion(muestra.id, comentario)
                }
            )
        }

        // Operador: avisa que corrigió una de sus muestras observadas.
        if (usuario?.rol == Rol.OPERADOR &&
            muestra.operador.id == usuario.id &&
            muestra.estado == EstadoRevision.OBSERVADO
        ) {
            Button(
                onClick = { muestraViewModel.marcarCorregida(muestra.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Marcar como corregida")
            }
        }
    }
}

/**
 * Acciones del supervisor (wireframe 4): aprobar la muestra o pedir una corrección.
 * Para pedir una corrección el comentario es obligatorio, y se muestra un mensaje si está vacío.
 */
@Composable
private fun PanelRevision(
    onAprobar: () -> Unit,
    onSolicitarCorreccion: (String) -> Unit
) {
    var comentario by rememberSaveable { mutableStateOf("") }
    var mostrarError by rememberSaveable { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Revisión del supervisor", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            OutlinedTextField(
                value = comentario,
                onValueChange = {
                    comentario = it
                    mostrarError = false
                },
                label = { Text("Comentario para el operador") },
                isError = mostrarError,
                supportingText = {
                    if (mostrarError) Text("Escribe qué hay que corregir para solicitar la corrección")
                },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = onAprobar, modifier = Modifier.fillMaxWidth()) {
                Text("Aprobar muestra")
            }
            OutlinedButton(
                onClick = {
                    if (comentario.isBlank()) {
                        mostrarError = true
                    } else {
                        onSolicitarCorreccion(comentario)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Solicitar corrección")
            }
        }
    }
}

/** Card con un título y sus filas de datos. */
@Composable
private fun SeccionDetalle(titulo: String, contenido: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            contenido()
        }
    }
}
