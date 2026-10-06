package com.example.aquasample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aquasample.ui.navigation.Rutas
import com.example.aquasample.viewmodel.LoginViewModel

@Composable
fun LoginScreen(loginViewModel: LoginViewModel, navController: NavController) {
    val estado by loginViewModel.uiState.collectAsState()

    val ingresar = {
        if (loginViewModel.iniciarSesion()) {
            // Se saca Login de la pila para que "atrás" desde Inicio no vuelva al login.
            navController.navigate(Rutas.INICIO) {
                popUpTo(Rutas.LOGIN) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Iniciar sesión",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = estado.usuario,
            onValueChange = loginViewModel::onUsuarioChange,
            label = { Text("Usuario") },
            isError = estado.error != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = estado.clave,
            onValueChange = loginViewModel::onClaveChange,
            label = { Text("Contraseña") },
            isError = estado.error != null,
            supportingText = estado.error?.let { { Text(it) } },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { ingresar() }),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { ingresar() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("INGRESAR", fontWeight = FontWeight.Bold)
        }

        Text(
            text = "Usuarios de prueba: operador1, operador2 o supervisor1 (clave 1234).\nEl rol se detecta según el usuario.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
