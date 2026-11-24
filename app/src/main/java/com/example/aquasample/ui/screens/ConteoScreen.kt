package com.example.aquasample.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aquasample.R
import com.example.aquasample.ui.components.CampoTexto
import com.example.aquasample.ui.components.FotoConteoAsistido
import com.example.aquasample.ui.components.SelectorFoto
import com.example.aquasample.ui.navigation.Rutas
import com.example.aquasample.viewmodel.CampoMuestra
import com.example.aquasample.viewmodel.MuestraViewModel

@Composable
fun ConteoScreen(muestraViewModel: MuestraViewModel, navController: NavController) {
    val estado by muestraViewModel.uiState.collectAsState()
    val errorIndividuos = estado.errores[CampoMuestra.INDIVIDUOS]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Recordatorio de qué muestra se está contando.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = listOfNotNull(estado.centro?.nombre, estado.tren?.nombre, estado.linea?.nombre)
                        .joinToString(" · "),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${estado.fecha} ${estado.hora} · tramo ${estado.tramo.trim()} m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Selector y captura de foto
        SelectorFoto(
            ruta = estado.fotoUri,
            onFotoCambiada = muestraViewModel::onFotoChange,
            error = estado.errores[CampoMuestra.FOTO]
        )

        // 2. Conteo asistido táctil sobre la fotografía (si hay foto seleccionada)
        if (estado.fotoUri != null) {
            FotoConteoAsistido(
                ruta = estado.fotoUri,
                marcas = estado.marcasPuntos,
                modoMarcadoActivo = estado.modoMarcadoActivo,
                onMarcar = muestraViewModel::agregarMarca,
                onDeshacer = muestraViewModel::deshacerUltimaMarca,
                onLimpiar = muestraViewModel::limpiarMarcas,
                onAlternarModo = muestraViewModel::alternarModoMarcado
            )
        }

        // 3. Resumen y ajuste manual del total
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total individuos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (estado.marcasPuntos.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Asistido (${estado.totalMarcas} en foto)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Ajuste de total mediante - / campo / +
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = muestraViewModel::restarIndividuo,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_restar), contentDescription = "Restar uno")
                    }

                    OutlinedTextField(
                        value = estado.individuos.toString(),
                        onValueChange = muestraViewModel::onIndividuosChange,
                        isError = errorIndividuos != null,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = muestraViewModel::sumarIndividuo,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(painterResource(R.drawable.ic_agregar), contentDescription = "Sumar uno")
                    }
                }

                AnimatedVisibility(
                    visible = estado.marcasPuntos.isNotEmpty() && estado.ajusteManual > 0,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "ℹ️ Incluye ajuste manual de +${estado.ajusteManual} sobre las ${estado.totalMarcas} marcas de la foto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                errorIndividuos?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // 4. Calibre promedio
        CampoTexto(
            valor = estado.calibre,
            onCambio = muestraViewModel::onCalibreChange,
            etiqueta = "Calibre promedio en mm (opcional)",
            ejemplo = "Ej: 45",
            teclado = KeyboardType.Decimal,
            error = estado.errores[CampoMuestra.CALIBRE]
        )

        // 5. Botón continuar con alto contraste
        Button(
            onClick = {
                if (muestraViewModel.validarConteo()) navController.navigate(Rutas.RESUMEN)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("VER RESUMEN", fontWeight = FontWeight.Bold)
        }
    }
}

