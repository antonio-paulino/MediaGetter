package pt.paulinoo.mediagetter.ui.theme

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorPalette = darkColors(
    primary = Color(0xFF424242), // Colors.grey.shade800
    primaryVariant = Color(0xFF121212),
    secondary = Color(0xFF616161), // Colors.grey.shade700
    background = Color(0xFF212121), // Colors.grey.shade900
    surface = Color(0xFF212121),
    error = Color(0xFFCF6679),
    onPrimary = Color(0xFFB0BEC5), // Colors.grey.shade300
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black
)

@Composable
fun MediaGetterTheme(
    content: @Composable () -> Unit
) {
    val colors = ColorPalette

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}