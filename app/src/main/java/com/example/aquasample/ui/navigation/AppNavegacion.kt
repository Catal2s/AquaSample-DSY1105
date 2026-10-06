package com.example.aquasample.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.aquasample.R
import com.example.aquasample.model.Rol
import com.example.aquasample.ui.screens.ConteoScreen
import com.example.aquasample.ui.screens.InicioScreen
import com.example.aquasample.ui.screens.LoginScreen
import com.example.aquasample.ui.screens.NuevaMuestraScreen
import com.example.aquasample.ui.screens.ResumenScreen
import com.example.aquasample.viewmodel.LoginViewModel
import com.example.aquasample.viewmodel.MuestraViewModel

/**
 * Pantalla principal: Scaffold con TopAppBar, NavigationBar y el NavHost.
 *
 * Los ViewModels se crean una sola vez aquí, arriba del NavHost, para que
 * Nueva muestra, Conteo y Resumen compartan el mismo formulario.
 */
@Composable
fun AquaSampleApp() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = viewModel()
    val muestraViewModel: MuestraViewModel = viewModel()

    val loginEstado by loginViewModel.uiState.collectAsState()
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route ?: Rutas.LOGIN
    val esOperador = loginEstado.usuarioActual?.rol == Rol.OPERADOR

    Scaffold(
        topBar = {
            BarraSuperior(
                rutaActual = rutaActual,
                onVolver = { navController.popBackStack() },
                onCerrarSesion = {
                    loginViewModel.cerrarSesion()
                    muestraViewModel.nuevaMuestra()
                    navController.navigate(Rutas.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        },
        bottomBar = {
            if (esOperador && rutaActual != Rutas.LOGIN) {
                BarraNavegacion(
                    rutaActual = rutaActual,
                    onInicio = {
                        navController.navigate(Rutas.INICIO) {
                            popUpTo(Rutas.INICIO)
                            launchSingleTop = true
                        }
                    },
                    onNuevaMuestra = { navController.abrirNuevaMuestra(muestraViewModel) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Rutas.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Rutas.LOGIN) { LoginScreen(loginViewModel, navController) }
            composable(Rutas.INICIO) { InicioScreen(loginViewModel, muestraViewModel, navController) }
            composable(Rutas.NUEVA_MUESTRA) { NuevaMuestraScreen(muestraViewModel, navController) }
            composable(Rutas.CONTEO) { ConteoScreen(muestraViewModel, navController) }
            composable(Rutas.RESUMEN) { ResumenScreen(loginViewModel, muestraViewModel, navController) }
        }
    }
}

/**
 * Abre el formulario de Nueva muestra. Solo lo limpia si la muestra anterior
 * ya se envió, así no se pierde un borrador al cambiar de pestaña.
 */
fun NavController.abrirNuevaMuestra(muestraViewModel: MuestraViewModel) {
    if (muestraViewModel.uiState.value.muestraEnviada != null) muestraViewModel.nuevaMuestra()
    navigate(Rutas.NUEVA_MUESTRA) {
        popUpTo(Rutas.INICIO)
        launchSingleTop = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraSuperior(
    rutaActual: String,
    onVolver: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val titulo = when (rutaActual) {
        Rutas.NUEVA_MUESTRA -> "Nueva muestra"
        Rutas.CONTEO -> "Conteo"
        Rutas.RESUMEN -> "Resumen"
        else -> "AquaSample"
    }
    TopAppBar(
        title = { Text(titulo) },
        navigationIcon = {
            if (rutaActual == Rutas.CONTEO || rutaActual == Rutas.RESUMEN) {
                IconButton(onClick = onVolver) {
                    Icon(painterResource(R.drawable.ic_volver), contentDescription = "Volver")
                }
            }
        },
        actions = {
            if (rutaActual == Rutas.INICIO) {
                IconButton(onClick = onCerrarSesion) {
                    Icon(painterResource(R.drawable.ic_cerrar_sesion), contentDescription = "Cerrar sesión")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun BarraNavegacion(
    rutaActual: String,
    onInicio: () -> Unit,
    onNuevaMuestra: () -> Unit
) {
    NavigationBar {
        ItemBarra(
            texto = "Inicio",
            icono = R.drawable.ic_inicio,
            seleccionado = rutaActual == Rutas.INICIO,
            onClick = onInicio
        )
        ItemBarra(
            texto = "Nueva muestra",
            icono = R.drawable.ic_agregar,
            seleccionado = rutaActual in Rutas.FLUJO_MUESTRA,
            onClick = onNuevaMuestra
        )
    }
}

@Composable
private fun RowScope.ItemBarra(
    texto: String,
    @DrawableRes icono: Int,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = seleccionado,
        onClick = onClick,
        icon = { Icon(painterResource(icono), contentDescription = null) },
        label = { Text(texto) }
    )
}
