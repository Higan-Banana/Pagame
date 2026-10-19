package gt.pagame.app.ui.screens.options

import android.content.ClipData
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import gt.pagame.app.data.model.Currency
import gt.pagame.app.ui.components.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import gt.pagame.app.data.model.AppThemeMode
import androidx.compose.ui.graphics.SolidColor
import gt.pagame.app.ui.components.AnimatedPagameFloatingNotice
import gt.pagame.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OptionsScreen(
    viewModel: OptionsViewModel,
    onLogoutSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var currencyExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    var colorExpanded by remember { mutableStateOf(false) }
    var passwordExpanded by remember { mutableStateOf(false) }
    val connected by rememberInternetAvailable()
    val context = LocalContext.current
    OperationError(uiState.errorMessage, viewModel::dismissError)
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val cardShape = RoundedCornerShape(22.dp)
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val cardBorder = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.large)
        ) {

            if (!connected) {
                Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.page, vertical = Spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Icon(Icons.Default.WifiOff, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Text("Sin conexión · Los datos pueden estar desactualizados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(Spacing.medium))

            // 1. Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.large),
                shape = cardShape,
                colors = cardColors,
                border = cardBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    // Persistent profile color
                    Box(Modifier.clickable { colorExpanded = true }) { ProfileAvatar(uiState.user.id, 58.dp) }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.user.name,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.tiny))
                        Text(
                            text = uiState.user.email,
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.small))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.tiny)
                        ) {
                            TextButton(onClick = viewModel::openNameDialog, enabled = !uiState.busy,
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = Spacing.tiny, vertical = Spacing.small)) {
                                Text("Editar nombre", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                            TextButton(onClick = { colorExpanded = true }, enabled = !uiState.busy,
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = Spacing.tiny, vertical = Spacing.small)) {
                                Text("Cambiar color", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }

                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.compact))

            // 2. Friend Code Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.large),
                shape = cardShape,
                colors = cardColors,
                border = cardBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.medium, vertical = Spacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.compact)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "TU CÓDIGO DE AMIGO",
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
                            )
                            Spacer(modifier = Modifier.height(Spacing.tiny))
                            Text(
                                text = uiState.user.friendCode,
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Copy button
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipEntry(ClipData.newPlainText("Código de amigo", uiState.user.friendCode))
                                )
                                viewModel.copyFriendCode()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(
                            enabled = true
                        ).copy(
                            brush = SolidColor(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            )
                        ),
                        contentPadding = PaddingValues(horizontal = Spacing.compact, vertical = Spacing.small)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text("Copiar", fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(
                    enabled = uiState.user.friendCode.isNotBlank(),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = Spacing.small),
                    onClick = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Agrégame en Pagame con mi código: ${uiState.user.friendCode}")
                        }
                        context.startActivity(Intent.createChooser(share, "Compartir código de amigo"))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(Spacing.small))
                    Text("Compartir código", fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            // 3. General Preferences Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.large)
            ) {
                Text(
                    text = "PREFERENCIAS GENERALES",
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing,
                    modifier = Modifier.padding(start = Spacing.tiny, bottom = Spacing.small)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShape,
                    colors = cardColors,
                    border = cardBorder
                ) {
                    Column {
                        PreferenceRow(
                            icon = Icons.Default.Payments,
                            iconBg = MaterialTheme.pagameColors.successContainer, iconColor = MaterialTheme.pagameColors.successContent,
                            title = "Moneda",
                            trailing = {
                                Box {
                                    Row(
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                            .clickable(enabled = !uiState.busy) { currencyExpanded = true }
                                            .padding(vertical = Spacing.small),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = Spacing.small, vertical = Spacing.tiny)
                                        ) {
                                            Text(uiState.currency.displayName, fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Spacer(Modifier.width(Spacing.tiny))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Elegir moneda",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp))
                                    }
                                    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
                                        DropdownMenu(
                                            expanded = currencyExpanded,
                                            onDismissRequest = { currencyExpanded = false },
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            tonalElevation = 0.dp
                                        ) {
                                            Currency.entries.forEach { currency ->
                                                DropdownMenuItem(
                                                    text = { Text(currency.displayName, fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold) },
                                                    enabled = !uiState.busy,
                                                    trailingIcon = {
                                                        if (currency == uiState.currency) Icon(Icons.Default.Check,
                                                            contentDescription = "Seleccionada",
                                                            tint = MaterialTheme.colorScheme.primary)
                                                    },
                                                    onClick = { currencyExpanded = false; viewModel.selectCurrency(currency) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }, onClick = { if (!uiState.busy) currencyExpanded = true }
                        )
                        Text("Se aplica a eventos nuevos; no convierte importes existentes.",
                            fontSize = MaterialTheme.typography.labelMedium.fontSize, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small))

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                        // Tema
                        PreferenceRow(
                            icon = Icons.Default.LightMode,
                            iconBg = MaterialTheme.pagameColors.warningContainer,
                            iconColor = MaterialTheme.pagameColors.warningContent,
                            title = "Tema",
                            trailing = {
                                Box {
                                    Row(
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                            .clickable(enabled = !uiState.busy) { themeExpanded = true }
                                            .padding(vertical = Spacing.small),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.tiny)
                                    ) {
                                        Box(Modifier.clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = Spacing.small, vertical = Spacing.tiny)) {
                                            Text(themeLabel(uiState.theme), fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Elegir tema",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp))
                                    }
                                    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
                                        DropdownMenu(
                                            expanded = themeExpanded,
                                            onDismissRequest = { themeExpanded = false },
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            tonalElevation = 0.dp
                                        ) {
                                            AppThemeMode.entries.forEach { theme ->
                                                DropdownMenuItem(
                                                    text = { Text(themeLabel(theme), fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                                        fontWeight = FontWeight.Bold) },
                                                    enabled = !uiState.busy,
                                                    trailingIcon = {
                                                        if (theme == uiState.theme) Icon(Icons.Default.Check,
                                                            contentDescription = "Seleccionado",
                                                            tint = MaterialTheme.colorScheme.primary)
                                                    },
                                                    onClick = { themeExpanded = false; viewModel.selectTheme(theme) }
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            onClick = { if (!uiState.busy) themeExpanded = true }
                        )


                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.large),
                shape = cardShape,
                colors = cardColors,
                border = cardBorder
            ) {
                PreferenceRow(
                    icon = Icons.Default.Lock,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = "Cambiar contraseña",
                    trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = { if (!uiState.busy) passwordExpanded = true }
                )
            }
            Spacer(Modifier.height(Spacing.medium))

            // 4. Cerrar Sesión Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.large)
                    .clickable { viewModel.openLogoutDialog() },
                shape = cardShape,
                colors = cardColors,
                border = cardBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.medium, vertical = Spacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Cerrar sesión",
                                fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(Spacing.tiny))
                            Text(
                                text = "Podrás volver a ingresar en cualquier momento",
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.page))

            // 5. Eliminar cuenta text link
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Eliminar cuenta",
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { viewModel.openDeleteDialog() }
                )

                Spacer(modifier = Modifier.height(Spacing.small))


            }
        }

        if (uiState.showNameDialog) {
            AlertDialog(
                onDismissRequest = viewModel::closeNameDialog,
                title = { Text("Editar nombre") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.compact)) {
                        OutlinedTextField(
                            value = uiState.editedName, onValueChange = viewModel::editName,
                            label = { Text("Nombre") }, singleLine = true,
                            enabled = !uiState.busy, shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!connected) Text("Necesitás conexión para guardar el nombre.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    Button(onClick = viewModel::saveName,
                        enabled = connected && !uiState.busy && uiState.editedName.isNotBlank()) {
                        Text(if (uiState.busy) "Guardando…" else "Guardar", fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = { TextButton(onClick = viewModel::closeNameDialog,
                    enabled = !uiState.busy) { Text("Cancelar", fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold) } },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }
        if (passwordExpanded) {
            AlertDialog(
                onDismissRequest = { if (!uiState.busy) passwordExpanded = false },
                title = { Text("Cambiar contraseña") },
                text = { Text(if (connected)
                    "Te enviaremos un enlace a ${uiState.user.email} para elegir una nueva contraseña."
                    else "Necesitás conexión para solicitar el enlace de cambio de contraseña.") },
                confirmButton = {
                    Button(enabled = connected && !uiState.busy, onClick = {
                        viewModel.sendPasswordLink { passwordExpanded = false }
                    }) { Text(if (uiState.busy) "Enviando…" else "Enviar enlace", fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(enabled = !uiState.busy, onClick = { passwordExpanded = false }) { Text("Cancelar", fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold) } },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }

        if (colorExpanded) {
            AlertDialog(onDismissRequest = { colorExpanded = false },
                title = { Text("Color de tu perfil") },
                text = {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.compact)) {
                        items(AvatarPalette.size) { index ->
                            val colors = AvatarPalette[index]
                            Surface(
                                modifier = Modifier.size(48.dp).clip(CircleShape)
                                    .clickable(enabled = !uiState.busy) { viewModel.selectColor(index); colorExpanded = false },
                                shape = CircleShape, color = colors.first,
                                border = if (index == uiState.user.colorIndex) androidx.compose.foundation.BorderStroke(2.dp, colors.second) else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(uiState.user.avatarInitial, color = colors.second, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }, confirmButton = { TextButton(onClick = { colorExpanded = false }) { Text("Cerrar", fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold) } })
        }

        // Logout Confirmation Dialog
        if (uiState.showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.closeLogoutDialog() },
                title = { Text("¿Cerrar sesión?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Puedes volver a entrar con tu correo y contraseña cuando quieras. Tus cuentas y grupos guardados se mantendrán sincronizados.",
                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmLogout(onLogoutSuccess) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Cerrar sesión", fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeLogoutDialog() }) {
                        Text("Cancelar", fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        // Delete Account Confirmation Dialog
        if (uiState.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.closeDeleteDialog() },
                title = { Text("Eliminar tu cuenta", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                text = {
                    Text(
                        "Se eliminarán tu cuenta, perfil y lista de amigos. Primero debes dejar de pertenecer a todos los eventos. Esta acción no se puede deshacer.",
                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmDelete(onLogoutSuccess) },
                        enabled = !uiState.busy,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Eliminar", fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeDeleteDialog() }) {
                        Text("Cancelar", fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        // Floating Snackbar notice
        AnimatedPagameFloatingNotice(
            message = uiState.noticeMessage,
            icon = Icons.Default.CheckCircle,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = Spacing.compact,
                    start = Spacing.large,
                    end = Spacing.large
                )
        )
    }
}

@Composable
private fun PreferenceRow(
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    title: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Spacing.medium, vertical = Spacing.medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = title,
                fontSize = MaterialTheme.typography.labelLarge.fontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        trailing()
    }
}

private fun themeLabel(theme: AppThemeMode): String = when (theme) {
    AppThemeMode.LIGHT -> "Claro"
    AppThemeMode.DARK -> "Oscuro"
    AppThemeMode.SYSTEM -> "Automático"
}
