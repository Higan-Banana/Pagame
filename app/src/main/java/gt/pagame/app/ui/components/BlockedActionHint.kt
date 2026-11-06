package gt.pagame.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Makes the reason for an unavailable action visible without relying on a tap. */
@Composable
fun BlockedActionHint(reason: String?, modifier: Modifier = Modifier) {
    if (reason != null) Text(reason, modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}
