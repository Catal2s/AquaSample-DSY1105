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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.aquasample.R
import com.example.aquasample.model.Rol
import com.example.aquasample.ui.screens.BandejaSupervisorScreen
import com.example.aquasample.ui.screens.ConteoScreen
import com.example.aquasample.ui.screens.DetalleMuestraScreen
import com.example.aquasample.ui.screens.HistorialScreen
import com.example.aquasample.ui.screens.InicioScreen
import com.example.aquasample.ui.screens.LoginScreen
import com.example.aquasample.ui.screens.NuevaMuestraScreen
import com.example.aquasample.ui.screens.ResumenScreen
import com.example.aquasample.viewmodel.HistorialViewModel
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
    val historialViewModel: HistorialViewModel = viewModel()

    val loginEstado by loginViewModel.uiState.collectAsState()
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route ?: Rutas.LOGIN
    val sesionIniciada = loginEstado.sesionIniciada
    val esOperador = loginEstado.usuarioActual?.rol == Rol.OPERADOR
    // Inicio del operador o Bandeja del supervisor: la base de la pila al cambiar de pestaña.
    val rutaInicio = Rutas.inicioDe(loginEstado.usuarioActual?.rol)

    Scaffold(
        topBar = {
            BarraSuperior(
                rutaActual = rutaActual,
                onVolver = { navController.popBackStack() },
                onCerrarSesion = {
                    loginViewModel.cerrarSesion()
                    muestraViewModel.nuevaMuestra()
                    historialViewModel.limpiarFiltros()
                    navController.navigate(Rutas.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        },
        bottomBar = {
            // Operador: Inicio, Nueva muestra e Historial. Supervisor: Bandeja e Historial.
            if (sesionIniciada && rutaActual != Rutas.LOGIN) {
                BarraNavegacion(
                    rutaActual = rutaActual,
                    esOperador = esOperador,
                    onInicio = {
                        navController.navigate(rutaInicio) {
                            popUpTo(rutaInicio)
                            launchSingleTop = true
                        }
                    },
                    onNuevaMuestra = { navController.abrirNuevaMuestra(muestraViewModel) },
                    onHistorial = {
                        navController.navigate(Rutas.HISTORIAL) {
                            popUpTo(rutaInicio)
                            launchSingleTop = true
                        }
                    }
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
            composable(Rutas.HISTORIAL) { HistorialScreen(loginViewModel, historialViewModel, navController) }
            composable(Rutas.BANDEJA) { BandejaSupervisorScreen(loginViewModel, muestraViewModel, navController) }
            composable(
                route = Rutas.DETALLE,
                arguments = listOf(navArgument(Rutas.ARG_ID_MUESTRA) { type = NavType.IntType })
            ) { entrada ->
                val idMuestra = entrada.arguments?.getInt(Rutas.ARG_ID_MUESTRA) ?: 0
                DetalleMuestraScreen(idMuestra, muestraViewModel)
            }
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
        Rutas.HISTORIAL -> "Historial"
        Rutas.BANDEJA -> "Bandeja de revisión"
        Rutas.DETALLE -> "Detalle de muestra"
        else -> "AquaSample"
    }
    TopAppBar(
        title = { Text(titulo) },
        navigationIcon = {
            if (rutaActual == Rutas.CONTEO || rutaActual == Rutas.RESUMEN || rutaActual == Rutas.DETALLE) {
                IconButton(onClick = onVolver) {
                    Icon(painterResource(R.drawable.ic_volver), contentDescription = "Volver")
                }
            }
        },
        actions = {
            if (rutaActual == Rutas.INICIO || rutaActual == Rutas.BANDEJA) {
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
    esOperador: Boolean,
    onInicio: () -> Unit,
    onNuevaMuestra: () -> Unit,
    onHistorial: () -> Unit
) {
    NavigationBar {
        ItemBarra(
            texto = if (esOperador) "Inicio" else "Bandeja",
            icono = if (esOperador) R.drawable.ic_inicio else R.drawable.ic_bandeja,
            seleccionado = rutaActual == Rutas.INICIO || rutaActual == Rutas.BANDEJA,
            onClick = onInicio
        )
        if (esOperador) {
            ItemBarra(
                texto = "Nueva muestra",
                icono = R.drawable.ic_agregar,
                seleccionado = rutaActual in Rutas.FLUJO_MUESTRA,
                onClick = onNuevaMuestra
            )
        }
        ItemBarra(
            texto = "Historial",
            icono = R.drawable.ic_historial,
            seleccionado = rutaActual == Rutas.HISTORIAL,
            onClick = onHistorial
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
