package com.example.aquasample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.aquasample.data.MuestraRepository
import com.example.aquasample.ui.navigation.AquaSampleApp
import com.example.aquasample.ui.theme.AquaSampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Abre la base de datos Room antes de que las pantallas pidan el repositorio.
        MuestraRepository.inicializar(applicationContext)
        enableEdgeToEdge()
        setContent {
            AquaSampleTheme {
                AquaSampleApp()
            }
        }
    }
}
