package gt.pagame.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import gt.pagame.app.ui.theme.*

@Composable
internal fun AddFriendDialog(
    friendCode: String,
    friendName: String,
    onCodeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAddFriend: () -> Unit,
    friendColorIndex: Int = 0,
    loading: Boolean = false,
    error: String? = null,
    busy: Boolean = false
) {
    PagameCenteredDialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .padding(top = Spacing.compact)
                .width(46.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                .align(Alignment.CenterHorizontally)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.page, top = Spacing.compact, end = Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Agregar amigo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(Spacing.tiny))
                Text(
                    text = "Ingresa el código único de formato PG-XXXX para enviarle una solicitud. Serán amigos cuando la acepte.",
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = Spacing.page, vertical = Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CÓDIGO DE AMIGO",
                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
                    )
                    Text(
                        text = "4 dígitos",
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = friendCode.removePrefix("PG-"),
                    onValueChange = onCodeChange,
                    prefix = {
                        Text(
                            text = "PG-",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    placeholder = { Text("0000") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (friendCode.isNotEmpty()) {
                            IconButton(onClick = { onCodeChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar código",
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !busy,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (loading) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.compact)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Buscando usuario…")
                }
            } else if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error)
            } else if (friendName.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PERSONA A AGREGAR",
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(StatusSuccessBg)
                                .border(1.dp, StatusSuccessBorder, RoundedCornerShape(50))
                                .padding(horizontal = Spacing.small, vertical = Spacing.tiny),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.tiny)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = StatusSuccessText,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Código verificado",
                                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                fontWeight = FontWeight.Bold,
                                color = StatusSuccessText
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.compact),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(AvatarPalette[friendColorIndex.coerceIn(0, 7)].first),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = friendName.split(" ")
                                        .mapNotNull { it.firstOrNull() }
                                        .take(2)
                                        .joinToString("")
                                        .uppercase(),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AvatarPalette[friendColorIndex.coerceIn(0, 7)].second
                                )
                            }
                            Spacer(modifier = Modifier.width(Spacing.compact))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                                ) {
                                    Text(
                                        text = friendName,
                                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = friendCode,
                                            modifier = Modifier.padding(horizontal = Spacing.tiny, vertical = Spacing.tiny),
                                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = "Usuario de Pagame",
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.tiny)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(StatusSuccessText)
                                    )
                                    Text(
                                        text = "Usuario encontrado",
                                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Seleccionado",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            } else if (friendCode.length == 7) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .padding(Spacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(
                        text = "No se encontró ningún usuario",
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Verifica que el código sea correcto",
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large, vertical = Spacing.medium),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(Spacing.small))
            Button(
                onClick = onAddFriend,
                enabled = friendName.isNotBlank() && !loading && !busy,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = Spacing.large, vertical = Spacing.compact)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(if (busy) "Enviando…" else "Enviar solicitud", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
