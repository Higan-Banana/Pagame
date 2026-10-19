package gt.pagame.app.ui.screens.events

import gt.pagame.app.ui.components.*
import gt.pagame.app.data.model.money
import gt.pagame.app.data.model.titleInitial
import kotlin.math.roundToLong
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gt.pagame.app.data.model.BalanceStatus
import gt.pagame.app.data.model.EventItem
import gt.pagame.app.ui.components.AvatarBubble
import gt.pagame.app.ui.components.BalanceStatusChip
import gt.pagame.app.ui.components.PagameBottomSheet
import gt.pagame.app.ui.components.AddFriendDialog
import gt.pagame.app.ui.theme.*

@Composable
fun EventsScreen(
    viewModel: EventsViewModel,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    OperationError(uiState.errorMessage, viewModel::dismissError)
    // Reset only after a successful creation, alongside the updated event list.
    // A fresh state prevents keyed items from retaining the previous scroll anchor.
    val eventListState = rememberSaveable(uiState.listResetVersion, saver = LazyListState.Saver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.page, vertical = Spacing.small)) {
                // Metadata row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        Text(
                            text = "MIS EVENTOS",
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${uiState.events.size}",
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = Spacing.compact, vertical = Spacing.tiny)
                    ) {
                        Text(
                            text = "Moneda: ${uiState.currency.displayName}",
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.compact))

            PagamePullToRefresh(Modifier.weight(1f).fillMaxWidth()) {
                // Content: Event List or Empty State
                if (uiState.events.isEmpty()) {
                    EventsEmptyState(
                        onCreateClick = { viewModel.openCreateDialog() },
                        modifier = Modifier.fillMaxSize().scrollable(rememberScrollableState { 0f }, Orientation.Vertical)
                    )
                } else {
                    LazyColumn(
                        state = eventListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.large),
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        contentPadding = PaddingValues(top = Spacing.tiny, bottom = Spacing.bottomClearance)
                    ) {
                        items(uiState.events, key = { it.id }) { event ->
                            EventCard(
                                event = event,
                                onClick = { onNavigateToDetail(event.id) }
                            )
                        }
                    }
                }
            }
        }

        // Create Event Dialog
        if (uiState.isCreateEventSheetOpen) {
            PagameBottomSheet(
                onDismissRequest = { viewModel.closeCreateDialog() }
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(start = Spacing.page, top = Spacing.page, end = Spacing.page),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    Text(
                        text = "Nuevo Evento",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )

                    OutlinedTextField(
                        value = uiState.newEventTitle,
                        onValueChange = viewModel::updateNewTitle,
                        placeholder = { Text("Nombre del evento") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿QUIÉN VA?",
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            )
                        ) {
                            Text(
                                text = "${uiState.selectedFriendIds.size} seleccionados",
                                modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.tiny),
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiState.friendSearchQuery,
                        onValueChange = viewModel::updateFriendSearch,
                        placeholder = { Text("Buscar por nombre o código", fontSize = MaterialTheme.typography.bodySmall.fontSize) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.openAddFriendDialog() }
                            .padding(horizontal = Spacing.tiny, vertical = Spacing.tiny),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿Aún no son amigos? ",
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Agregar amigo",
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextButton(onClick = viewModel::openRequests, shape = RoundedCornerShape(50),
                        modifier = Modifier.align(Alignment.End)) {
                        Text("Solicitudes (${uiState.receivedCount})", fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold)
                    }
                    uiState.friendNotice?.let { notice ->
                        Text(notice, fontSize = MaterialTheme.typography.bodySmall.fontSize, color = MaterialTheme.colorScheme.primary)
                    }

                    val selectedIds = remember(uiState.selectedFriendIds) {
                        uiState.selectedFriendIds.toSet()
                    }
                    val selectedFriends = remember(uiState.allFriends, selectedIds) {
                        uiState.allFriends.filter { it.id in selectedIds }
                    }
                    if (selectedFriends.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                        ) {
                            selectedFriends.forEach { friend ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                            RoundedCornerShape(50)
                                        )
                                        .clickable { viewModel.toggleFriendSelection(friend.id) }
                                        .padding(horizontal = Spacing.compact, vertical = Spacing.small),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.tiny)
                                ) {
                                    Text(
                                        text = friend.name.substringBefore(" "),
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    val filteredFriends = remember(uiState.allFriends, uiState.friendSearchQuery) {
                        val query = uiState.friendSearchQuery.trim()
                        uiState.allFriends.filter { friend ->
                            query.isEmpty() ||
                                friend.name.contains(query, ignoreCase = true) ||
                                friend.codeOrStatus.contains(query, ignoreCase = true)
                        }.sortedBy { it.name }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 190.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                            .padding(horizontal = Spacing.small, vertical = Spacing.tiny)
                    ) {
                        items(filteredFriends, key = { it.id }) { friend ->
                            val isSelected = friend.id in selectedIds
                            val (avatarBackground, avatarContent) = avatarColors(friend.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.toggleFriendSelection(friend.id) }
                                    .padding(horizontal = Spacing.small, vertical = Spacing.small),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.compact)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            avatarBackground
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        friend.initial,
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = avatarContent
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(friend.name, fontSize = MaterialTheme.typography.bodyMedium.fontSize, fontWeight = FontWeight.Bold)
                                    Text(
                                        friend.codeOrStatus,
                                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.toggleFriendSelection(friend.id) }
                                )
                            }
                            if (friend != filteredFriends.lastOrNull()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = Spacing.medium),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.large, vertical = Spacing.medium),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { viewModel.closeCreateDialog() }) {
                        Text("Cancelar", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Button(
                        onClick = { viewModel.createEvent() },
                        enabled = uiState.newEventTitle.isNotBlank() && !uiState.busy,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = Spacing.page, vertical = Spacing.compact)
                    ) {
                        Text("Crear", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        if (uiState.requestsOpen) FriendRequestsSheet(onDismiss = viewModel::closeRequests)

        if (uiState.isAddFriendDialogOpen) {
            AddFriendDialog(
                friendCode = uiState.newFriendCode,
                friendName = uiState.foundFriendName,
                friendColorIndex = uiState.foundFriend?.colorIndex ?: 0,
                loading = uiState.lookupLoading, error = uiState.lookupError, busy = uiState.busy,
                onCodeChange = viewModel::updateNewFriendCode,
                onDismiss = viewModel::closeAddFriendDialog,
                onAddFriend = viewModel::addFriendFromEvent
            )
        }
    }
}

@Composable
fun EventCard(
    event: EventItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.large)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Icon Squircle
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = titleInitial(event.title),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.headlineMedium.fontSize
                        )
                    }

                    Column {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(Spacing.tiny))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.tiny)
                        ) {
                            if (event.isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.pagameColors.successContent)
                                )
                            }
                            Text(
                                text = "${event.createdAtLabel}\n${event.participantCount} participantes\nCreado por: ${event.createdByName}",
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "GASTO TOTAL",
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
                    )
                    Spacer(modifier = Modifier.height(Spacing.tiny))
                    Text(
                        text = money((event.totalAmount * 100).roundToLong(), event.currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (event.balanceStatus == BalanceStatus.YOU_ARE_OWED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Bottom row: Avatar stack & Status chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stack of participant avatars
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.avatarOverlap)) {
                    event.participantIds.take(3).forEach { uid -> ProfileAvatar(uid, 28.dp) }
                    if (event.participantIds.size > 3) AvatarBubble(
                        text = "+${event.participantIds.size - 3}",
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant, size = 28.dp)

                }

                BalanceStatusChip(
                    status = event.balanceStatus,
                    amount = event.balanceAmount, currency = event.currency
                )
            }
        }
    }
}

@Composable
fun EventsEmptyState(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.section),
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
                imageVector = Icons.Default.EventBusy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.large))

        Text(
            text = "No tienes eventos activos",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        Text(
            text = "Crea tu primer evento para comenzar a dividir salidas, viajes o compras con tus amigos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.large))

        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = Spacing.page, vertical = Spacing.compact)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(Spacing.small))
            Text("Crear nuevo evento", fontWeight = FontWeight.Bold)
        }
    }
}
