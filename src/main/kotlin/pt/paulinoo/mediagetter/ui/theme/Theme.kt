package pt.paulinoo.mediagetter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6B6B),
    onPrimary = Color(0xFF410006),
    primaryContainer = Color(0xFF93000F),
    onPrimaryContainer = Color(0xFFFFDAD7),
    secondary = Color(0xFFFFB4AB),
    onSecondary = Color(0xFF690008),
    tertiary = Color(0xFF9CCAFF),
    background = Color(0xFF0E0F13),
    onBackground = Color(0xFFE4E2E6),
    surface = Color(0xFF16181D),
    onSurface = Color(0xFFE4E2E6),
    surfaceVariant = Color(0xFF262A33),
    onSurfaceVariant = Color(0xFFC4C7D0),
    surfaceContainer = Color(0xFF1C1F25),
    surfaceContainerHigh = Color(0xFF22252C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF3B3E46),
    outlineVariant = Color(0xFF2C2F37)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFC30015),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD7),
    onPrimaryContainer = Color(0xFF410006),
    secondary = Color(0xFF9A4040),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF1B5E9B),
    background = Color(0xFFFBF8F9),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFF1ECED),
    onSurfaceVariant = Color(0xFF52454C),
    surfaceContainer = Color(0xFFF4EFF0),
    surfaceContainerHigh = Color(0xFFEEE9EA),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF847377),
    outlineVariant = Color(0xFFD6C2C5)
)

@Composable
fun MediaGetterTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
