package gt.pagame.app.ui.components

import gt.pagame.app.data.model.Currency
import gt.pagame.app.data.model.money
import kotlin.math.roundToLong
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import gt.pagame.app.data.model.BalanceStatus
import gt.pagame.app.ui.theme.*

@Composable
fun PagameCenteredDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val dialogSurface = if (isLightTheme) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface
    val dialogContent = if (isLightTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = Spacing.large, vertical = Spacing.page),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 390.dp)
                    .heightIn(max = 720.dp),
                shape = RoundedCornerShape(28.dp),
                color = dialogSurface,
                contentColor = dialogContent,
                tonalElevation = 0.dp,
                shadowElevation = 24.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    content = content
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagameBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.safeDrawing },
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = if (isLightTheme) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = Spacing.compact, bottom = Spacing.small)
                    .width(42.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun AnimatedPagameFloatingNotice(
    message: String?,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.CheckCircle
) {
    AnimatedContent(
        targetState = message,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220)) +
                slideInVertically(
                    animationSpec = tween(220),
                    initialOffsetY = { it / 2 }
                ) +
                scaleIn(
                    animationSpec = tween(220),
                    initialScale = 0.92f
                )) togetherWith
                (fadeOut(animationSpec = tween(180)) +
                    slideOutVertically(
                        animationSpec = tween(180),
                        targetOffsetY = { it / 2 }
                    ) +
                    scaleOut(
                        animationSpec = tween(180),
                        targetScale = 0.92f
                    ))
        },
        contentAlignment = Alignment.Center,
        label = "FloatingNoticeTransition"
    ) { displayedMessage ->
        displayedMessage?.let {
            PagameFloatingNotice(
                message = it,
                icon = icon
            )
        }
    }
}

@Composable
fun PagameFloatingNotice(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.CheckCircle
) {
    Surface(
        modifier = modifier.widthIn(max = 320.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Spacing.medium,
                vertical = Spacing.compact
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.pagameColors.successContent,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = message,
                modifier = Modifier.weight(1f, fill = false),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PagamePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArrow: Boolean = true,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            ),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        ),
        contentPadding = PaddingValues(horizontal = Spacing.page)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                fontWeight = FontWeight.Bold
            )
            if (showArrow) {
                Spacer(modifier = Modifier.width(Spacing.small))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PagameInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isSuccessBorder: Boolean = false,
    headerTrailing: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.small, start = Spacing.tiny, end = Spacing.tiny),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing,
                fontSize = MaterialTheme.typography.labelMedium.fontSize
            )
            headerTrailing?.invoke()
        }

        val borderColor = if (isSuccessBorder) StatusSuccessBorder else MaterialTheme.colorScheme.outline
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                .padding(horizontal = Spacing.medium, vertical = Spacing.compact),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSuccessBorder) StatusSuccessBg else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isSuccessBorder) StatusSuccessText else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.compact))

            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        fontSize = MaterialTheme.typography.labelLarge.fontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            trailingIcon?.invoke()
        }
    }
}

@Composable
fun BalanceStatusChip(
    status: BalanceStatus,
    amount: Double,
    currency: Currency = Currency.GTQ,
    modifier: Modifier = Modifier
) {
    val (bg, textColor, text, icon) = when (status) {
        BalanceStatus.YOU_ARE_OWED -> Quadruple(
            MaterialTheme.pagameColors.successContainer,
            MaterialTheme.pagameColors.successContent,
            "Te deben ${money((amount * 100).roundToLong(), currency)}",
            Icons.Default.Check
        )
        BalanceStatus.YOU_OWE -> Quadruple(
            MaterialTheme.pagameColors.successContainer,
            MaterialTheme.pagameColors.successContent,
            "Debes ${money((amount * 100).roundToLong(), currency)}",
            null
        )
        BalanceStatus.ALL_SETTLED -> Quadruple(
            MaterialTheme.pagameColors.successContainer,
            MaterialTheme.pagameColors.successContent,
            "Al día",
            null
        )
        BalanceStatus.LIQUIDATED -> Quadruple(
            MaterialTheme.pagameColors.successContainer,
            MaterialTheme.pagameColors.successContent,
            "Liquidado",
            Icons.Default.Check
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = Spacing.compact, vertical = Spacing.tiny),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.tiny)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = textColor
            )
        }
        Text(
            text = text,
            fontSize = MaterialTheme.typography.labelMedium.fontSize,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun PagameBottomBar(currentRoute: String, onTabSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = Spacing.tiny
    ) {
        listOf(
            Triple("events_tab", "Eventos", Icons.Default.CalendarMonth),
            Triple("friends_tab", "Amigos", Icons.Default.Group),
            Triple("options_tab", "Opciones", Icons.Default.Settings)
        ).forEach { (route, title, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { onTabSelected(route) },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun PagameExtendedFab(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(50),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            .height(50.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = Spacing.large, vertical = Spacing.compact)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.small))
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = MaterialTheme.typography.labelLarge.fontSize
        )
    }
}

@Composable
fun AvatarBubble(
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    backgroundColor: Color = MaterialTheme.pagameColors.warningContainer,
    textColor: Color = MaterialTheme.pagameColors.warningContent
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = MaterialTheme.typography.labelMedium.fontSize,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
