package com.example.aquasample.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.Muestra

/**
 * Tarjeta con el resumen de una muestra. Se usa en Inicio y en Historial
 * (antes estaba como función privada dentro de InicioScreen).
 */
@Composable
fun TarjetaMuestra(muestra: Muestra, mostrarOperador: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Muestra #${muestra.id}", fontWeight = FontWeight.Bold)
                EtiquetaEstado(muestra.estado)
            }
            Text("${muestra.centro.nombre} · ${muestra.tren.nombre} · ${muestra.linea.nombre}")
            Text(
                text = "${muestra.fecha} ${muestra.hora} · ${muestra.conteo.individuos} individuos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (mostrarOperador) {
                Text(
                    text = "Operador: ${muestra.operador.nombre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            muestra.comentarioSupervisor?.let {
                Text("Observación: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Etiqueta de color según el estado de revisión de la muestra. */
@Composable
fun EtiquetaEstado(estado: EstadoRevision) {
    val (fondo, texto) = when (estado) {
        EstadoRevision.PENDIENTE, EstadoRevision.CORREGIDO -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        EstadoRevision.OBSERVADO -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        EstadoRevision.VALIDADO -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
    }
    Surface(color = fondo, contentColor = texto, shape = RoundedCornerShape(50)) {
        Text(
            text = estado.etiqueta,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
