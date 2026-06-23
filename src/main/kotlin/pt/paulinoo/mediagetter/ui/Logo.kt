package pt.paulinoo.mediagetter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The MediaGetter mark drawn vectorially so it stays crisp at any size:
 * a rounded red tile with a white "download to tray" glyph.
 */
@Composable
fun AppLogo(size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val corner = s * 0.24f

        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFFF6B6B), Color(0xFFE02424))
            ),
            size = Size(s, s),
            cornerRadius = CornerRadius(corner, corner)
        )

        val white = Color.White
        val stroke = s * 0.085f

        // Arrow shaft
        drawLine(
            color = white,
            start = Offset(s * 0.5f, s * 0.27f),
            end = Offset(s * 0.5f, s * 0.55f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Arrow head
        val head = Path().apply {
            moveTo(s * 0.355f, s * 0.50f)
            lineTo(s * 0.645f, s * 0.50f)
            lineTo(s * 0.5f, s * 0.72f)
            close()
        }
        drawPath(head, white)

        // Tray (open box at the bottom)
        val tray = Path().apply {
            moveTo(s * 0.31f, s * 0.66f)
            lineTo(s * 0.31f, s * 0.80f)
            lineTo(s * 0.69f, s * 0.80f)
            lineTo(s * 0.69f, s * 0.66f)
        }
        drawPath(tray, white, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}
