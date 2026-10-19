package gt.pagame.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = CardBackground,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    secondary = BrandPrimaryDark,
    background = AppBackground,
    surface = CardBackground,
    onBackground = TextTitle,
    onSurface = TextTitle,
    outline = CardBorder,
    surfaceVariant = SubtleSurface,
    onSurfaceVariant = TextBody
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkPrimary,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkTextTitle,
    onSurface = DarkTextTitle,
    outline = DarkCardBorder,
    surfaceVariant = DarkPrimaryContainer,
    onSurfaceVariant = DarkTextBody
)

@Composable
fun PagameTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
