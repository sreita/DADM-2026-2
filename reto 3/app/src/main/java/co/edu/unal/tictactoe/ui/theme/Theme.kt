package co.edu.unal.tictactoe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GameColorScheme = darkColorScheme(
    primary = AccentViolet,
    secondary = AccentBlue,
    tertiary = HumanGreen,
    error = ComputerRed,
    background = BackgroundTop,
    surface = SurfaceColor,
    surfaceContainer = SurfaceColor,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun AndroidTicTacToeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GameColorScheme,
        typography = Typography,
        content = content
    )
}
