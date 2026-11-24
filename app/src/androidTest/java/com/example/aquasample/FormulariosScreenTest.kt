package com.example.aquasample

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import com.example.aquasample.data.MuestraRepository
import com.example.aquasample.data.local.MuestraDao
import com.example.aquasample.data.local.MuestraEntity
import com.example.aquasample.ui.screens.LoginScreen
import com.example.aquasample.ui.screens.NuevaMuestraScreen
import com.example.aquasample.ui.theme.AquaSampleTheme
import com.example.aquasample.viewmodel.LoginViewModel
import com.example.aquasample.viewmodel.MuestraViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/** DAO falso en memoria: la pantalla se prueba sin abrir la base de datos real. */
private class DaoEnMemoria : MuestraDao {
    private val filas = MutableStateFlow<List<MuestraEntity>>(emptyList())
    override fun observarTodas(): Flow<List<MuestraEntity>> = filas
    override suspend fun buscarPorId(id: Int) = filas.value.find { it.id == id }
    override suspend fun contar() = filas.value.size
    override suspend fun insertar(muestra: MuestraEntity) { filas.value = filas.value + muestra }
    override suspend fun insertarTodas(muestras: List<MuestraEntity>) { filas.value = filas.value + muestras }
    override suspend fun actualizarEstado(id: Int, estado: String, comentario: String?) {}
}

/**
 * Compose UI Test de los formularios: se dibuja la pantalla real y se simula
 * lo que hace el usuario (tocar botones y escribir) para comprobar que los
 * mensajes de error aparecen debajo de cada campo cuando los datos son inválidos.
 */
class FormulariosScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun mostrarNuevaMuestra() {
        val viewModel = MuestraViewModel(repositorio = MuestraRepository(DaoEnMemoria()))
        composeRule.setContent {
            AquaSampleTheme {
                NuevaMuestraScreen(muestraViewModel = viewModel, navController = rememberNavController())
            }
        }
    }

    private fun campoTramo() =
        composeRule.onNode(hasSetTextAction() and hasText("Tramo muestreado (m)"))

    @Test
    fun nuevaMuestra_sinDatos_muestraErroresDebajoDeCadaCampo() {
        mostrarNuevaMuestra()

        composeRule.onNodeWithText("SIGUIENTE: CONTEO").performScrollTo().performClick()

        composeRule.onNodeWithText("Selecciona un centro").assertIsDisplayed()
        composeRule.onNodeWithText("Selecciona un tren").assertIsDisplayed()
        composeRule.onNodeWithText("Selecciona una línea").assertIsDisplayed()
        composeRule.onNodeWithText("Ingresa un largo de tramo mayor a 0").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun nuevaMuestra_tramoEnCero_esInvalido_yAlCorregirloDesapareceElError() {
        mostrarNuevaMuestra()

        campoTramo().performScrollTo().performTextInput("0")
        composeRule.onNodeWithText("SIGUIENTE: CONTEO").performScrollTo().performClick()
        composeRule.onNodeWithText("Ingresa un largo de tramo mayor a 0").performScrollTo().assertIsDisplayed()

        campoTramo().performTextInput(",5")

        composeRule.onNodeWithText("Ingresa un largo de tramo mayor a 0").assertDoesNotExist()
    }

    @Test
    fun login_conClaveIncorrecta_muestraMensajeDeError() {
        composeRule.setContent {
            AquaSampleTheme {
                LoginScreen(loginViewModel = LoginViewModel(), navController = rememberNavController())
            }
        }

        composeRule.onNode(hasSetTextAction() and hasText("Usuario")).performTextInput("operador1")
        composeRule.onNode(hasSetTextAction() and hasText("Contraseña")).performTextInput("0000")
        composeRule.onNodeWithText("INGRESAR").performClick()

        composeRule.onNodeWithText("Usuario o contraseña incorrectos").assertIsDisplayed()
    }

    @Test
    fun login_sinDatos_pideUsuarioYContrasena() {
        composeRule.setContent {
            AquaSampleTheme {
                LoginScreen(loginViewModel = LoginViewModel(), navController = rememberNavController())
            }
        }

        composeRule.onNodeWithText("INGRESAR").performClick()

        composeRule.onNodeWithText("Ingresa usuario y contraseña").assertIsDisplayed()
    }
}
