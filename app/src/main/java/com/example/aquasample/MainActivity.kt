package com.example.aquasample

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.aquasample.data.MuestraRepository
import com.example.aquasample.ui.navigation.AquaSampleApp
import com.example.aquasample.ui.theme.AquaSampleTheme

class MainActivity : ComponentActivity() {

    // Si se rechaza, la app sigue funcionando sin conexión y los cambios quedan pendientes.
    private val pedirRedLocal = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

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
        if (savedInstanceState == null) pedirPermisoRedLocal()
    }

    /** Desde Android 17 (API 37) hay que pedir permiso para usar IP de la red local, como la del backend. */
    private fun pedirPermisoRedLocal() {
        if (Build.VERSION.SDK_INT < 37) return
        val permiso = "android.permission.ACCESS_LOCAL_NETWORK"
        if (ContextCompat.checkSelfPermission(this, permiso) != PackageManager.PERMISSION_GRANTED) {
            pedirRedLocal.launch(permiso)
        }
    }
}
