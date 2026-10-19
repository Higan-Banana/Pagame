package gt.pagame.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.luminance

// Light Palette - Warm Financial Collective (Material 3)
val BrandPrimary = Color(0xFFB83214)
val BrandPrimaryDark = Color(0xFF9C280E)
val BrandPrimaryLight = Color(0xFFFDEEE9)
val BrandPrimaryContainer = Color(0xFFFFE8E2)
val BrandOnPrimaryContainer = Color(0xFF7E200A)

val AppBackground = Color(0xFFF3FAFF)
val CardBackground = Color(0xFFFFFFFF)
val CardBorder = Color(0xFFE3EBF3)
val SubtleSurface = Color(0xFFF8FAFC)

val TextTitle = Color(0xFF131B26)
val TextBody = Color(0xFF606C7D)
val TextMuted = Color(0xFF8E9AA8)
val InputIconBg = Color(0xFFF4F7FA)

// Status Colors
val StatusSuccessText = Color(0xFF137A50)
val StatusSuccessBg = Color(0xFFECFDF3)
val StatusSuccessBorder = Color(0xFFD8E6DB)

val StatusOweText = Color(0xFFB83214)
val StatusOweBg = Color(0xFFFEE2E2)

val StatusNeutralText = Color(0xFF475569)
val StatusNeutralBg = Color(0xFFF1F5F9)

val StatusAmberText = Color(0xFFB45309)
val StatusAmberBg = Color(0xFFFEF3C7)

// Dark Theme Palette
val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkCardBorder = Color(0xFF334155)
val DarkTextTitle = Color(0xFFF8FAFC)
val DarkTextBody = Color(0xFF94A3B8)
val DarkTextMuted = Color(0xFF64748B)
val DarkPrimary = Color(0xFF2A9D8F)
val DarkPrimaryContainer = Color(0xFF264653)
val DarkOnPrimary = Color(0xFF061B19)
val DarkOnPrimaryContainer = Color(0xFFF4FFFD)

/** Semantic colors not represented by the standard Material color roles. */
@Immutable
data class PagameStatusColors(
    val successContainer: Color, val successContent: Color,
    val warningContainer: Color, val warningContent: Color,
    val gradientEnd: Color, val balanceGradientEnd: Color, val transparent: Color
)

private val LightStatusColors = PagameStatusColors(
    successContainer = Color(0xFFE8F7EE), successContent = StatusSuccessText,
    warningContainer = StatusAmberBg, warningContent = Color(0xFF92400E),
    gradientEnd = Color(0xFFE04E28), balanceGradientEnd = Color(0xFF991B1B), transparent = Color.Transparent
)
private val DarkStatusColors = PagameStatusColors(
    successContainer = Color(0xFF12382B), successContent = Color(0xFF85E0B6),
    warningContainer = Color(0xFF453516), warningContent = Color(0xFFFFD68A),
    gradientEnd = Color(0xFF1F6F68), balanceGradientEnd = Color(0xFF1F6F68), transparent = Color.Transparent
)

val MaterialTheme.pagameColors: PagameStatusColors
    @Composable @ReadOnlyComposable get() =
        if (colorScheme.background.luminance() > 0.5f) LightStatusColors else DarkStatusColors

// User profile palette is shared by every avatar; colorIndex remains stable.
val AvatarPalette = listOf(
    Color(0xFFFFEDD5) to Color(0xFFC2410C), Color(0xFFFCE7F3) to Color(0xFFBE185D),
    Color(0xFFEDE9FE) to Color(0xFF7C3AED), Color(0xFFE0F2FE) to Color(0xFF0369A1),
    Color(0xFFD1FAE5) to Color(0xFF047857), Color(0xFFFEF3C7) to Color(0xFF92400E),
    Color(0xFFE0E7FF) to Color(0xFF3730A3), Color(0xFFFFE4E6) to Color(0xFFBE123C)
)
