package com.example.aquasample.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquasample.data.FotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Muestra la foto guardada en [ruta], o un recuadro con "Sin foto" si no hay. */
@Composable
fun FotoMuestra(ruta: String?, modifier: Modifier = Modifier) {
    val bitmap by produceState<Bitmap?>(null, ruta) {
        value = if (ruta == null) null else withContext(Dispatchers.IO) {
            FotoStorage.cargarBitmap(ruta, 1024)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val imagen = bitmap
        if (imagen != null) {
            Image(
                bitmap = imagen.asImageBitmap(),
                contentDescription = "Foto de la muestra",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = if (ruta == null) "Sin foto" else "No se pudo cargar la foto",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Bloque para adjuntar la foto de la muestra: vista previa y botones. */
@Composable
fun SelectorFoto(
    ruta: String?,
    onFotoCambiada: (String?) -> Unit,
    error: String?,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    // Galería: el sistema abre su selector y devuelve la imagen elegida (o null si cancela).
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            alcance.launch {
                val rutaCopia = withContext(Dispatchers.IO) {
                    FotoStorage.copiarDesdeGaleria(contexto, uri)
                }
                if (rutaCopia != null) {
                    FotoStorage.borrar(ruta)
                    onFotoCambiada(rutaCopia)
                }
            }
        }
    }

    // Ruta del archivo donde la cámara va a dejar la foto. Va en rememberSaveable porque
    // Android puede destruir la pantalla mientras la app de cámara está abierta.
    var rutaPendiente by rememberSaveable { mutableStateOf<String?>(null) }

    // Cámara: el resultado es true (se tomó la foto) o false (se canceló).
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { seTomoFoto ->
        val pendiente = rutaPendiente
        if (seTomoFoto && pendiente != null) {
            FotoStorage.borrar(ruta)
            onFotoCambiada(pendiente)
        } else {
            FotoStorage.borrar(pendiente)
        }
        rutaPendiente = null
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Foto de la muestra",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        FotoMuestra(ruta)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val archivo = FotoStorage.crearArchivo(contexto)
                    rutaPendiente = archivo.absolutePath
                    camara.launch(FotoStorage.uriParaCamara(contexto, archivo))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("TOMAR FOTO")
            }
            OutlinedButton(
                onClick = {
                    galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("GALERÍA")
            }
        }

        if (ruta != null) {
            TextButton(onClick = {
                FotoStorage.borrar(ruta)
                onFotoCambiada(null)
            }) {
                Text("Quitar foto")
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}