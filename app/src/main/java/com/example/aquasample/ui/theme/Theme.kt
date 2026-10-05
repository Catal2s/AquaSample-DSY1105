package com.example.aquasample.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AquaPrincipal,
    onPrimary = Color.White,
    primaryContainer = AquaPrincipalContenedor,
    onPrimaryContainer = AquaTextoOscuro,
    secondary = AquaSecundario,
    onSecondary = Color.White,
    secondaryContainer = AquaSecundarioContenedor,
    onSecondaryContainer = AquaTextoOscuro,
    tertiary = AquaAlerta,
    onTertiary = AquaTexto,
    tertiaryContainer = AquaAlertaContenedor,
    onTertiaryContainer = AquaTexto,
    background = AquaFondo,
    onBackground = AquaTexto,
    surface = AquaFondo,
    onSurface = AquaTexto,
    surfaceVariant = AquaSuperficieVariante,
    onSurfaceVariant = AquaTextoSecundario,
    outline = AquaBorde,
    // Superficies de Card y NavigationBar en tonos verde agua (no los grises/lila por defecto).
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEEF5F3),
    surfaceContainer = Color(0xFFE8EFED),
    surfaceContainerHigh = Color(0xFFE3EAE8),
    surfaceContainerHighest = Color(0xFFDDE4E2)
)

private val DarkColorScheme = darkColorScheme(
    primary = AquaPrincipalOscuro,
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = AquaPrincipalContenedor,
    secondary = AquaSecundarioOscuro,
    onSecondary = Color(0xFF003732),
    secondaryContainer = Color(0xFF1F4D48),
    onSecondaryContainer = AquaSecundarioContenedor,
    tertiary = AquaAlerta,
    onTertiary = AquaTexto,
    background = AquaFondoOscuro,
    onBackground = AquaTextoClaro,
    surface = AquaFondoOscuro,
    onSurface = AquaTextoClaro,
    surfaceVariant = AquaTextoSecundario,
    onSurfaceVariant = Color(0xFFBEC9C6),
    surfaceContainerLowest = Color(0xFF0A0F0E),
    surfaceContainerLow = Color(0xFF171D1C),
    surfaceContainer = Color(0xFF1B2120),
    surfaceContainerHigh = Color(0xFF252B2A),
    surfaceContainerHighest = Color(0xFF303635)
)

/**
 * Tema de AquaSample con la paleta del equipo.
 * El color dinámico de Android 12+ queda desactivado para que la app
 * mantenga siempre su identidad visual, sin tomar los colores del fondo de pantalla.
 */
@Composable
fun AquaSampleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
