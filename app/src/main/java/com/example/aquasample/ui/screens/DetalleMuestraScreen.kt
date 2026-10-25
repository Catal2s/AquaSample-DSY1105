package com.example.aquasample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquasample.ui.components.EstadoVacio
import com.example.aquasample.ui.components.EtiquetaEstado
import com.example.aquasample.ui.components.FilaDato
import com.example.aquasample.viewmodel.MuestraViewModel

/**
 * Detalle de una muestra, al tocarla en Inicio, Historial o la bandeja del supervisor.
 * Recibe solo el id (viene en la ruta) y busca la muestra en la lista del ViewModel,
 * así que si el supervisor cambia el estado, el detalle se actualiza solo.
 */
@Composable
fun DetalleMuestraScreen(idMuestra: Int, muestraViewModel: MuestraViewModel) {
    val muestras by muestraViewModel.muestras.collectAsState()
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
