package gt.pagame.app.ui.components

import gt.pagame.app.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import gt.pagame.app.data.model.FriendItem

val LocalPeople = staticCompositionLocalOf<Map<String, FriendItem>> { emptyMap() }
val LocalCurrentUid = staticCompositionLocalOf { "" }
@Composable
fun avatarColors(key: String): Pair<Color, Color> {
    val people = LocalPeople.current
    val person = if (key == "Tú") people[LocalCurrentUid.current] else people[key]
        ?: people.values.firstOrNull { it.name == key || "${it.name} (${it.codeOrStatus})" == key }
    return AvatarPalette[(person?.colorIndex ?: 0).coerceIn(0, AvatarPalette.lastIndex)]
}
@Composable
fun ProfileAvatar(uid: String, size: Dp = 32.dp) {
    val person = LocalPeople.current[uid]
    val colors = avatarColors(uid)
    Box(Modifier.size(size).clip(CircleShape).background(colors.first), contentAlignment = Alignment.Center) {
        Text(person?.initial ?: "?", color = colors.second, fontWeight = FontWeight.Bold,
            fontSize = if (size <= 24.dp) MaterialTheme.typography.labelSmall.fontSize else MaterialTheme.typography.bodyMedium.fontSize)
    }
}
@Composable
fun OperationError(message: String?, onDismiss: () -> Unit) {
    if (message != null) AlertDialog(onDismissRequest = onDismiss,
        title = { Text("No se pudo completar") }, text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido") } })
}
