package com.example.aquasample.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aquasample.R
import com.example.aquasample.model.EstadoRevision
import com.example.aquasample.model.Muestra
import com.example.aquasample.ui.components.EstadoVacio
import com.example.aquasample.viewmodel.MuestraViewModel

enum class FiltroTemporalDashboard(val etiqueta: String) {
    TODAS("Histórico completo"),
    ULTIMOS_30_DIAS("Últimos 30 días"),
    ULTIMOS_7_DIAS("Últimos 7 días")
}

@Composable
fun DashboardScreen(
    muestraViewModel: MuestraViewModel
) {
    val todasLasMuestras by muestraViewModel.muestras.collectAsState()
    var filtroTemporal by remember { mutableStateOf(FiltroTemporalDashboard.TODAS) }

    val muestrasFiltradas = remember(todasLasMuestras, filtroTemporal) {
        if (todasLasMuestras.isEmpty()) emptyList()
        else {
            when (filtroTemporal) {
                FiltroTemporalDashboard.TODAS -> todasLasMuestras
                FiltroTemporalDashboard.ULTIMOS_30_DIAS -> {
                    val maxFecha = todasLasMuestras.maxOf { it.fecha }
                    todasLasMuestras.filter { it.fecha >= restarDias(maxFecha, 30) }
                }
                FiltroTemporalDashboard.ULTIMOS_7_DIAS -> {
                    val maxFecha = todasLasMuestras.maxOf { it.fecha }
                    todasLasMuestras.filter { it.fecha >= restarDias(maxFecha, 7) }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_dashboard),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "Dashboard de Muestreos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Métricas operativas y evolución temporal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Filtros temporales
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(FiltroTemporalDashboard.values()) { opcion ->
                FilterChip(
                    selected = filtroTemporal == opcion,
                    onClick = { filtroTemporal = opcion },
                    label = { Text(opcion.etiqueta) }
                )
            }
        }

        if (muestrasFiltradas.isEmpty()) {
            EstadoVacio(
                titulo = "Sin muestras en este período",
                mensaje = "No se encontraron registros de muestreo en el rango seleccionado."
            )
            return@Column
        }

        // 1. Tarjetas KPI
        val totalMuestras = muestrasFiltradas.size
        val totalIndividuos = muestrasFiltradas.sumOf { it.conteo.individuos }
        val promedioIndividuos = if (totalMuestras > 0) totalIndividuos / totalMuestras else 0
        val validadas = muestrasFiltradas.count { it.estado == EstadoRevision.VALIDADO }
        val pctValidadas = if (totalMuestras > 0) (validadas * 100) / totalMuestras else 0

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCard(
                titulo = "Total Muestras",
                valor = "$totalMuestras",
                subtitulo = "$totalIndividuos ind.",
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                titulo = "Promedio",
                valor = "$promedioIndividuos",
                subtitulo = "ind. por muestra",
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                titulo = "Validadas",
                valor = "$pctValidadas%",
                subtitulo = "$validadas de $totalMuestras",
                modifier = Modifier.weight(1f)
            )
        }

        // 2. Gráfico: Muestras por Centro de Cultivo
        SeccionDashboard("Muestras por Centro de Cultivo") {
            val porCentro = muestrasFiltradas.groupBy { it.centro.nombre }
            val maxPorCentro = porCentro.maxOfOrNull { it.value.size } ?: 1

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                porCentro.forEach { (centro, lista) ->
                    val cantidad = lista.size
                    val fraccion = cantidad.toFloat() / maxPorCentro.toFloat()
                    val pct = (cantidad * 100) / totalMuestras

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(centro, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("$cantidad ($pct%)", style = MaterialTheme.typography.bodySmall)
                        }

                        // Barra horizontal animada
                        val animWidth by animateFloatAsState(
                            targetValue = fraccion,
                            animationSpec = tween(durationMillis = 600),
                            label = "barra_centro"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animWidth.coerceIn(0.05f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }

        // 3. Gráfico: Estados de Revisión (Pendiente / Observado / Corregido / Validado)
        SeccionDashboard("Estados de Revisión") {
            val estadosContados = EstadoRevision.values().map { estado ->
                estado to muestrasFiltradas.count { it.estado == estado }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                estadosContados.forEach { (estado, conteo) ->
                    val pct = if (totalMuestras > 0) (conteo * 100) / totalMuestras else 0
                    val fraccion = if (totalMuestras > 0) conteo.toFloat() / totalMuestras.toFloat() else 0f
                    val colorEstado = when (estado) {
                        EstadoRevision.PENDIENTE -> Color(0xFFFFB300)
                        EstadoRevision.OBSERVADO -> MaterialTheme.colorScheme.error
                        EstadoRevision.CORREGIDO -> MaterialTheme.colorScheme.tertiary
                        EstadoRevision.VALIDADO -> MaterialTheme.colorScheme.primary
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = colorEstado,
                                    shape = CircleShape,
                                    modifier = Modifier.size(10.dp)
                                ) {}
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = estado.etiqueta,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "$conteo ($pct%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val animWidth by animateFloatAsState(
                            targetValue = fraccion,
                            animationSpec = tween(durationMillis = 600),
                            label = "barra_estado"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (conteo > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animWidth.coerceIn(0.04f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(colorEstado)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Gráfico: Evolución en el Tiempo (Muestreo e Individuos)
        SeccionDashboard("Evolución en el Tiempo") {
            val porFecha = muestrasFiltradas
                .groupBy { it.fecha }
                .toSortedMap()

            val maxIndividuosFecha = porFecha.maxOfOrNull { (_, lista) -> lista.sumOf { it.conteo.individuos } } ?: 1

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Individuos muestreados por fecha",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Histograma de barras verticales
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    porFecha.forEach { (fecha, muestrasDia) ->
                        val totalIndDia = muestrasDia.sumOf { it.conteo.individuos }
                        val alturaFraccion = (totalIndDia.toFloat() / maxIndividuosFecha.toFloat()).coerceIn(0.08f, 1f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "$totalIndDia",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(alturaFraccion)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = fecha.takeLast(5), // MM-DD
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    titulo: String,
    valor: String,
    subtitulo: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SeccionDashboard(
    titulo: String,
    contenido: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            contenido()
        }
    }
}

private fun restarDias(fecha: String, dias: Int): String {
    // Formato simple AAAA-MM-DD
    return runCatching {
        val partes = fecha.split("-").map { it.toInt() }
        val cal = java.util.Calendar.getInstance().apply {
            set(partes[0], partes[1] - 1, partes[2])
            add(java.util.Calendar.DAY_OF_YEAR, -dias)
        }
        String.format(
            java.util.Locale.US,
            "%04d-%02d-%02d",
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }.getOrDefault(fecha)
}
