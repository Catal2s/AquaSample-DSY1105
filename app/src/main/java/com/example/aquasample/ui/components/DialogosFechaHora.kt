package com.example.aquasample.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable

/**
 * Calendario de Material 3 para elegir la fecha de la muestra.
 * Los días posteriores a hoy aparecen deshabilitados.
 * Las fechas van y vienen en milisegundos UTC, que es lo que usa el DatePicker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoFecha(
    fechaInicialMillis: Long?,
    hoyMillis: Long,
    onConfirmar: (Long) -> Unit,
    onCerrar: () -> Unit
) {
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = fechaInicialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= hoyMillis
        }
    )

    DatePickerDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(
                onClick = {
                    estado.selectedDateMillis?.let(onConfirmar)
                    onCerrar()
                },
                enabled = estado.selectedDateMillis != null
            ) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        }
    ) {
        DatePicker(state = estado)
    }
}

/** Reloj de Material 3 (formato 24 horas) para elegir la hora de la muestra. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoHora(
    horaInicial: Int,
    minutoInicial: Int,
    onConfirmar: (hora: Int, minuto: Int) -> Unit,
    onCerrar: () -> Unit
) {
    val estado = rememberTimePickerState(
        initialHour = horaInicial,
        initialMinute = minutoInicial,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Hora de la muestra") },
        text = { TimePicker(state = estado) },
        confirmButton = {
            TextButton(onClick = {
                onConfirmar(estado.hour, estado.minute)
                onCerrar()
            }) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )
}
