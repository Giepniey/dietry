package app.privatediary.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

object DiaryColors {
    val Background = Color(0xFF000000)
    val Card = Color(0xFF1C1C1E)
    val CardBorder = Color(0xFF2C2C2E)
    val OnSurface = Color(0xFFEDEDED)
    val OnSurfaceMuted = Color(0xFF9A9AA0)
    val Accent = Color(0xFFFFB000)
    val Danger = Color(0xFFFF5A4D)
}

private val Scheme = darkColorScheme(
    background = DiaryColors.Background,
    surface = DiaryColors.Card,
    onBackground = DiaryColors.OnSurface,
    onSurface = DiaryColors.OnSurface,
    primary = DiaryColors.Accent,
    error = DiaryColors.Danger,
)

// Dark is the only theme by design. The Surface sets the default text color (the Stage 1 black-on-black bug).
@Composable
fun DiaryTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DiaryColors.Background,
        contentColor = DiaryColors.OnSurface,
        content = content,
    )
}
