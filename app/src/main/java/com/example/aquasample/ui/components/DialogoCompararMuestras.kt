package com.example.aquasample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.aquasample.R
import com.example.aquasample.model.Muestra
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Diálogo interactivo para comparar dos muestreos lado a lado,
 * permitiendo analizar la evolución de una misma línea de cultivo
 * o la variación entre períodos de tiempo.
 */
@Composable
fun DialogoCompararMuestras(
    muestraA: Muestra,
    muestraB: Muestra,
    onCerrar: () -> Unit
) {
    // Orden cronológico: A como más antigua y B como más reciente
    val (antigua, reciente) = if (muestraA.fecha <= muestraB.fecha) muestraA to muestraB else muestraB to muestraA
    val mismaLinea = antigua.linea.id == reciente.linea.id

    val difIndividuos = reciente.conteo.individuos - antigua.conteo.individuos
    val pctIndividuos = if (antigua.conteo.individuos > 0) {
        (difIndividuos.toDouble() / antigua.conteo.individuos.toDouble()) * 100
    } else 0.0

    val densidadAntigua = if (antigua.tramoMetros > 0) antigua.conteo.individuos / antigua.tramoMetros else 0.0
    val densidadReciente = if (reciente.tramoMetros > 0) reciente.conteo.individuos / reciente.tramoMetros else 0.0
    val difDensidad = densidadReciente - densidadAntigua

    val diasDif = calcularDiferenciaDias(antigua.fecha, reciente.fecha)

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_comparar),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Comparativa de Muestras",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Muestra #${antigua.id} vs Muestra #${reciente.id}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Indicador de misma línea y período
                Surface(
                    color = if (mismaLinea) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = if (mismaLinea) "✓ Comparando la misma línea de cultivo" else "ℹ️ Comparando líneas distintas",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (mismaLinea) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${antigua.centro.nombre} · ${antigua.linea.nombre}" +
                                (if (!mismaLinea) " vs ${reciente.linea.nombre}" else "") +
                                (if (diasDif > 0) " · Diferencia de $diasDif días" else " · Mismo día"),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Fotografías lado a lado si existen
                if (antigua.fotoUri != null || reciente.fotoUri != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Foto #${antigua.id} (${antigua.fecha})", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(4.dp))
                            FotoMuestra(antigua.fotoUri, modifier = Modifier.height(110.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Foto #${reciente.id} (${reciente.fecha})", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(4.dp))
                            FotoMuestra(reciente.fotoUri, modifier = Modifier.height(110.dp))
                        }
                    }
                }

                HorizontalDivider()

                // Tabla comparativa de métricas
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilaComparativa(
                        etiqueta = "Fecha y Hora",
                        valorA = "${antigua.fecha}\n${antigua.hora}",
                        valorB = "${reciente.fecha}\n${reciente.hora}"
                    )

                    FilaComparativa(
                        etiqueta = "Tramo muestreado",
                        valorA = "${antigua.tramoMetros} m",
                        valorB = "${reciente.tramoMetros} m"
                    )

                    // Comparación clave de Individuos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1.2f)) {
                            Text("Individuos contados", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            val textoDif = if (difIndividuos >= 0) "+$difIndividuos (+${String.format(Locale.US, "%.1f", pctIndividuos)}%)"
                            else "$difIndividuos (${String.format(Locale.US, "%.1f", pctIndividuos)}%)"

                            val colorChip = if (difIndividuos >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            Text(textoDif, color = colorChip, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${antigua.conteo.individuos}", modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                        Text("${reciente.conteo.individuos}", modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    FilaComparativa(
                        etiqueta = "Densidad est. (ind/m)",
                        valorA = String.format(Locale.US, "%.1f", densidadAntigua),
                        valorB = String.format(Locale.US, "%.1f", densidadReciente) +
                            " (" + (if (difDensidad >= 0) "+" else "") + String.format(Locale.US, "%.1f", difDensidad) + ")"
                    )

                    FilaComparativa(
                        etiqueta = "Calibre promedio",
                        valorA = antigua.conteo.calibrePromedioMm?.let { "$it mm" } ?: "No reg.",
                        valorB = reciente.conteo.calibrePromedioMm?.let { "$it mm" } ?: "No reg."
                    )

                    FilaComparativa(
                        etiqueta = "Operador",
                        valorA = antigua.operador.nombre,
                        valorB = reciente.operador.nombre
                    )

                    FilaComparativa(
                        etiqueta = "Estado revisión",
                        valorA = antigua.estado.etiqueta,
                        valorB = reciente.estado.etiqueta
                    )
                }

                HorizontalDivider()

                // Conclusión del análisis
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = "Análisis comparativo",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        val resumen = if (mismaLinea) {
                            if (difIndividuos > 0) {
                                "En la misma línea (${antigua.linea.nombre}), se observa un incremento del ${String.format(Locale.US, "%.1f", abs(pctIndividuos))}% (+${difIndividuos} ind) entre el ${antigua.fecha} y el ${reciente.fecha}."
                            } else if (difIndividuos < 0) {
                                "En la misma línea (${antigua.linea.nombre}), se observa una disminución del ${String.format(Locale.US, "%.1f", abs(pctIndividuos))}% (${difIndividuos} ind) entre períodos."
                            } else {
                                "La cantidad de individuos se mantuvo idéntica entre ambos muestreos (${antigua.conteo.individuos} individuos)."
                            }
                        } else {
                            "Comparación entre líneas distintas del centro ${antigua.centro.nombre}. La muestra #${reciente.id} presenta ${if (difIndividuos >= 0) "+$difIndividuos" else "$difIndividuos"} individuos respecto a la #${antigua.id}."
                        }
                        Text(resumen, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Button(
                    onClick = onCerrar,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CERRAR COMPARATIVA", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FilaComparativa(
    etiqueta: String,
    valorA: String,
    valorB: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(etiqueta, modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valorA, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        Text(valorB, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun calcularDiferenciaDias(fecha1: String, fecha2: String): Long {
    return runCatching {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val d1 = sdf.parse(fecha1)
        val d2 = sdf.parse(fecha2)
        if (d1 != null && d2 != null) {
            val dif = abs(d2.time - d1.time)
            dif / (1000 * 60 * 60 * 24)
        } else 0L
    }.getOrDefault(0L)
}
