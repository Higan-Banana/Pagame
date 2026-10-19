package gt.pagame.app.ui.components

import gt.pagame.app.ui.theme.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gt.pagame.app.data.repository.PagameRepository
import gt.pagame.app.data.repository.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Shared by Friends and the event draft; accepting never closes or resets the draft. */
@Composable
fun FriendRequestsSheet(onDismiss: () -> Unit) {
    val repository = PagameRepository.instance
    val requests by repository.friendRequests.collectAsState()
    val people by repository.people.collectAsState()
    val uid by repository.sessionUid.collectAsState()
    val received = requests.filter { it.status == "pending" && it.recipientId == uid }
    val sent = requests.filter { it.status == "pending" && it.senderId == uid }
    var busyId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun respond(id: String, accept: Boolean, incoming: Boolean) {
        if (busyId != null) return
        busyId = id
        notice = null
        scope.launch {
            try {
                withTimeout(15_000L) {
                    if (accept) repository.acceptFriendRequest(id) else repository.dismissFriendRequest(id)
                }
                notice = if (accept) "Solicitud aceptada. Ahora aparecen en la lista de amigos de ambos."
                    else if (incoming) "Solicitud rechazada." else "Solicitud cancelada."
            } catch (e: TimeoutCancellationException) {
                error = "No se pudo confirmar la respuesta. Revisa el estado de la solicitud antes de volver a intentar."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = userMessage(e)
            } finally { busyId = null }
        }
    }
    PagameBottomSheet(onDismissRequest = { if (busyId == null) onDismiss() }) {
        Row(Modifier.fillMaxWidth().padding(start = Spacing.page, end = Spacing.compact, top = Spacing.small),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Solicitudes", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold)
            IconButton(onClick = onDismiss, enabled = busyId == null) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar solicitudes")
            }
        }
        Text("Al aceptar, ambos aparecerán en sus listas de amigos.",
            Modifier.padding(horizontal = Spacing.page, vertical = Spacing.small), fontSize = MaterialTheme.typography.bodySmall.fontSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        notice?.let {
            Text(it, Modifier.padding(horizontal = Spacing.page, vertical = Spacing.small), fontSize = MaterialTheme.typography.bodySmall.fontSize,
                color = MaterialTheme.colorScheme.primary)
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false),
            contentPadding = PaddingValues(start = Spacing.page, end = Spacing.page, bottom = Spacing.page),
            verticalArrangement = Arrangement.spacedBy(Spacing.compact)) {
            item {
                Text("RECIBIDAS (${received.size})", fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (received.isEmpty()) item {
                Text("No tienes solicitudes pendientes.", fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    modifier = Modifier.padding(vertical = Spacing.compact))
            }
            items(received + sent, key = { it.id }) { request ->
                val incoming = request.recipientId == uid
                if (!incoming && request.id == sent.firstOrNull()?.id) {
                    Text("ENVIADAS (${sent.size})", fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = Spacing.medium, bottom = Spacing.compact),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val otherId = request.otherId(uid.orEmpty())
                val person = people[otherId]
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Column(Modifier.fillMaxWidth().padding(Spacing.medium)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.compact)) {
                            ProfileAvatar(otherId, 42.dp)
                            Column(Modifier.weight(1f)) {
                                Text(person?.name ?: "Cargando perfil…", fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(if (incoming) "Quiere agregarte como amigo" else "Esperando su aceptación",
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (busyId == request.id) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                        Row(Modifier.fillMaxWidth().padding(top = Spacing.small), horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { respond(request.id, false, incoming) }, enabled = busyId == null,
                                shape = RoundedCornerShape(50)) {
                                Text(if (incoming) "Rechazar" else "Cancelar solicitud", fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    fontWeight = FontWeight.Bold)
                            }
                            if (incoming) {
                                Spacer(Modifier.width(Spacing.small))
                                Button(onClick = { respond(request.id, true, true) }, enabled = busyId == null,
                                    shape = RoundedCornerShape(50)) {
                                    Text("Aceptar", fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        OperationError(error) { error = null }
    }
}
