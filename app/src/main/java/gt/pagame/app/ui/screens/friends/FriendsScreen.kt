package gt.pagame.app.ui.screens.friends

import gt.pagame.app.ui.components.*
import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gt.pagame.app.data.model.FriendItem
import androidx.compose.material.icons.filled.CheckCircle
import gt.pagame.app.ui.components.AnimatedPagameFloatingNotice
import gt.pagame.app.ui.components.AddFriendDialog
import gt.pagame.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    OperationError(uiState.errorMessage, viewModel::dismissError)
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
        ) {
            // Header Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Amigos",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = viewModel::openAddFriendDialog) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Agregar amigo",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp)) {
                Text("Comparte gastos con las personas de tu círculo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("MIS AMIGOS", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.sizeIn(minWidth = 20.dp, minHeight = 20.dp)
                        .clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 5.dp), contentAlignment = Alignment.Center) {
                        Text("${uiState.friends.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                    TextButton(onClick = viewModel::openRequests, shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("${uiState.receivedCount}", Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Solicitudes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            PagamePullToRefresh(Modifier.weight(1f).fillMaxWidth()) {
                // List of Friends or Empty State
                if (uiState.friends.isEmpty()) {
                    FriendsEmptyState(
                        onAddClick = { viewModel.openAddFriendDialog() },
                        modifier = Modifier.fillMaxSize().scrollable(rememberScrollableState { 0f }, Orientation.Vertical)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp)
                    ) {
                        items(uiState.friends, key = { it.id }) { friend ->
                            val request = uiState.requests.firstOrNull { it.otherId(uiState.myId) == friend.id }
                            Column {
                                FriendCard(friend = friend, onRemove = { viewModel.requestRemoveFriend(friend) })
                                if (request == null) {
                                    TextButton(onClick = {
                                        viewModel.requestExistingFriend(friend)
                                    }, shape = RoundedCornerShape(50), modifier = Modifier.align(Alignment.End)) {
                                        Text("Confirmar amistad",
                                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        uiState.friendToRemove?.let { friend ->
            AlertDialog(onDismissRequest = viewModel::cancelRemoveFriend,
                title = { Text("Eliminar amigo") },
                text = { Text("¿Eliminar a ${friend.name} de tus amigos? La amistad se quitará para ambos. Los eventos compartidos, gastos y pagos se conservarán.") },
                confirmButton = { TextButton(onClick = viewModel::confirmRemoveFriend, enabled = !uiState.busy) {
                    Text(if (uiState.busy) "Eliminando…" else "Eliminar", fontWeight = FontWeight.Bold) } },
                dismissButton = { TextButton(onClick = viewModel::cancelRemoveFriend, enabled = !uiState.busy) {
                    Text("Cancelar", fontWeight = FontWeight.Bold) } }, containerColor = MaterialTheme.colorScheme.surface)
        }
        if (uiState.requestsOpen) FriendRequestsSheet(onDismiss = viewModel::closeRequests)

        // Add Friend Dialog
        if (uiState.isAddFriendDialogOpen) {
            AddFriendDialog(
                friendCode = uiState.newFriendCode,
                friendName = uiState.foundFriendName,
                friendColorIndex = uiState.foundFriend?.colorIndex ?: 0,
                loading = uiState.lookupLoading, error = uiState.lookupError, busy = uiState.busy,
                onCodeChange = viewModel::updateNewCode,
                onDismiss = viewModel::closeAddFriendDialog,
                onAddFriend = viewModel::addFriend
            )
        }

        // Toast notice when copied
        AnimatedPagameFloatingNotice(
            message = uiState.noticeMessage,
            icon = Icons.Default.CheckCircle,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = 12.dp,
                    start = 20.dp,
                    end = 20.dp
                )
        )
    }
}

@Composable
fun FriendCard(
    friend: FriendItem,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ProfileAvatar(friend.id, 42.dp)

                Column {
                    Text(
                        text = friend.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "En ${friend.eventsTogetherCount} eventos juntos",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Default.PersonRemove, contentDescription = "Eliminar a ${friend.name}",
                    tint = MaterialTheme.colorScheme.primary)
            }
            if (friend.isSettledBadge) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFE8F7EE))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = friend.codeOrStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccessText
                    )
                }
            } else {
                Text(
                    text = friend.codeOrStatus,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FriendsEmptyState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PersonOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Aún no tienes amigos agregados",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Comparte tu código o ingresa el de un amigo para empezar a dividir cuentas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Agregar primer amigo", fontWeight = FontWeight.Bold)
        }
    }
}
