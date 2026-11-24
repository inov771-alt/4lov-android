package ru.fourlov.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val FourLovColors = lightColorScheme(
    primary = Green,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = GreenDark,
    background = Background,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary
)

@Composable
fun FourLovTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FourLovColors, content = content)
}
