package pt.paulinoo.mediagetter

import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import pt.paulinoo.mediagetter.ui.App
import java.awt.Dimension
import javax.imageio.ImageIO

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(520.dp, 760.dp),
        position = WindowPosition(Alignment.Center)
    )

    val appIcon = remember {
        Thread.currentThread().contextClassLoader
            .getResourceAsStream("icon.png")
            ?.use { BitmapPainter(ImageIO.read(it).toComposeImageBitmap()) }
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        icon = appIcon,
        title = "MediaGetter"
    ) {
        window.minimumSize = Dimension(420, 600)
        App()
    }
}
