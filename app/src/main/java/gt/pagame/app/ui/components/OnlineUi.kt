package gt.pagame.app.ui.components

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
import androidx.compose.ui.unit.sp
import gt.pagame.app.data.model.FriendItem

val LocalPeople = staticCompositionLocalOf<Map<String, FriendItem>> { emptyMap() }
val LocalCurrentUid = staticCompositionLocalOf { "" }
val AvatarPalette = listOf(
    Color(0xFFFFEDD5) to Color(0xFFC2410C), Color(0xFFFCE7F3) to Color(0xFFBE185D),
    Color(0xFFEDE9FE) to Color(0xFF7C3AED), Color(0xFFE0F2FE) to Color(0xFF0369A1),
    Color(0xFFD1FAE5) to Color(0xFF047857), Color(0xFFFEF3C7) to Color(0xFF92400E),
    Color(0xFFE0E7FF) to Color(0xFF3730A3), Color(0xFFFFE4E6) to Color(0xFFBE123C)
)
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
            fontSize = if (size <= 24.dp) 9.sp else 13.sp)
    }
}
@Composable
fun OperationError(message: String?, onDismiss: () -> Unit) {
    if (message != null) AlertDialog(onDismissRequest = onDismiss,
        title = { Text("No se pudo completar") }, text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido") } })
}
