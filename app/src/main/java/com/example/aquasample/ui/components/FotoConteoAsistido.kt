package com.example.aquasample.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aquasample.R
import com.example.aquasample.data.FotoStorage
import com.example.aquasample.viewmodel.PuntoMarca
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Visor interactivo de fotografía para el conteo asistido de individuos (choritos).
 * Permite registrar puntos tocando la imagen, dibujarlos en tiempo real,
 * deshacer la última marca y alternar entre modo marcado y modo vista.
 */
@Composable
fun FotoConteoAsistido(
    ruta: String?,
    marcas: List<PuntoMarca>,
    modoMarcadoActivo: Boolean,
    onMarcar: (Float, Float) -> Unit,
    onDeshacer: () -> Unit,
    onLimpiar: () -> Unit,
    onAlternarModo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<Bitmap?>(null, ruta) {
        value = if (ruta == null) null else withContext(Dispatchers.IO) {
            FotoStorage.cargarBitmap(ruta, 1024)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabecera del visor interactivo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${marcas.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Conteo asistido en foto",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Interruptor de modo de marcado
                OutlinedButton(
                    onClick = onAlternarModo,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (modoMarcadoActivo) "Modo: Marcando" else "Modo: Vista",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Lienzo interactivo sobre la foto
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val imagen = bitmap
                if (imagen != null) {
                    Image(
                        bitmap = imagen.asImageBitmap(),
                        contentDescription = "Fotografía para conteo asistido",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Capa Canvas interactiva
                    val colorPrimario = MaterialTheme.colorScheme.primary
                    val colorAcento = Color(0xFFFFB300)

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(modoMarcadoActivo) {
                                if (modoMarcadoActivo) {
                                    detectTapGestures { offset ->
                                        val w = size.width.toFloat()
                                        val h = size.height.toFloat()
                                        if (w > 0 && h > 0) {
                                            val xRel = (offset.x / w).coerceIn(0f, 1f)
                                            val yRel = (offset.y / h).coerceIn(0f, 1f)
                                            onMarcar(xRel, yRel)
                                        }
                                    }
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height

                        marcas.forEachIndexed { indice, marca ->
                            val centro = Offset(marca.xRel * w, marca.yRel * h)
                            val esUltima = indice == marcas.lastIndex

                            // Círculo exterior semitransparente
                            drawCircle(
                                color = if (esUltima) colorAcento.copy(alpha = 0.5f) else colorPrimario.copy(alpha = 0.4f),
                                radius = 16.dp.toPx(),
                                center = centro
                            )
                            // Anillo de borde
                            drawCircle(
                                color = if (esUltima) colorAcento else colorPrimario,
                                radius = 16.dp.toPx(),
                                center = centro,
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                            // Punto central
                            drawCircle(
                                color = Color.White,
                                radius = 4.dp.toPx(),
                                center = centro
                            )
                        }
                    }

                    // Aviso flotante si está en modo marcado
                    if (modoMarcadoActivo) {
                        Surface(
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Toca un chorito para marcarlo (+1)",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (ruta == null) "Toma o adjunta una fotografía para habilitar el conteo asistido" else "Cargando fotografía...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Barra de acciones: Deshacer y Limpiar marcas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDeshacer,
                    enabled = marcas.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_deshacer),
                        contentDescription = "Deshacer última marca",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Deshacer marca")
                }

                AnimatedVisibility(visible = marcas.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    TextButton(onClick = onLimpiar) {
                        Text("Limpiar marcas", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
