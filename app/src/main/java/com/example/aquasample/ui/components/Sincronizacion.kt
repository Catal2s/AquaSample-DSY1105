package com.example.aquasample.ui.components

import android.widget.Toast
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aquasample.R
import com.example.aquasample.viewmodel.SincronizacionViewModel

/** Sube lo pendiente, baja del servidor y avisa el resultado con un Toast. */
@Composable
fun BotonSincronizar(viewModel: SincronizacionViewModel = viewModel()) {
    val estado by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            Toast.makeText(contexto, it, Toast.LENGTH_LONG).show()
            viewModel.mensajeMostrado()
        }
    }

    OutlinedButton(
        onClick = viewModel::sincronizar,
        enabled = !estado.enCurso,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(painterResource(R.drawable.ic_sync), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (estado.enCurso) "Sincronizando…" else "Sincronizar")
    }
}

/** Elimina la muestra después de confirmar y vuelve a la pantalla anterior. */
@Composable
fun BotonEliminarMuestra(idMuestra: Int, onEliminar: () -> Unit) {
    var confirmar by rememberSaveable { mutableStateOf(false) }
    val volver = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    OutlinedButton(
        onClick = { confirmar = true },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Eliminar muestra")
    }

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("¿Eliminar la muestra #$idMuestra?") },
            text = { Text("También se eliminará del servidor. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmar = false
                    onEliminar()
                    volver?.onBackPressed()
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = false }) { Text("Cancelar") }
            }
        )
    }
}
