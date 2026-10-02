package com.example.aquasample.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.aquasample.R

/**
 * OutlinedTextField que muestra el error del ViewModel debajo del campo
 * (isError + supportingText), como se acordó en el PR de modelos.
 */
@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String?,
    modifier: Modifier = Modifier,
    ejemplo: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    unaLinea: Boolean = true,
    minLineas: Int = 1
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        placeholder = ejemplo?.let { { Text(it) } },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        singleLine = unaLinea,
        minLines = minLineas,
        modifier = modifier.fillMaxWidth()
    )
}

/** Campo de solo lectura que abre un menú para elegir una opción (centro, tren o línea). */
@Composable
fun <T> Selector(
    etiqueta: String,
    opciones: List<T>,
    seleccionado: T?,
    textoDe: (T) -> String,
    onSeleccionar: (T) -> Unit,
    error: String?,
    textoSinOpciones: String,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = seleccionado?.let(textoDe) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { Icon(painterResource(R.drawable.ic_desplegar), contentDescription = null) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // Capa encima del campo para que todo el campo abra el menú al tocarlo.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { abierto = true }
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            if (opciones.isEmpty()) {
                DropdownMenuItem(text = { Text(textoSinOpciones) }, onClick = {}, enabled = false)
            }
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(textoDe(opcion)) },
                    onClick = {
                        onSeleccionar(opcion)
                        abierto = false
                    }
                )
            }
        }
    }
}
