package gt.pagame.app.ui.screens.eventdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gt.pagame.app.data.model.ExpenseItem
import gt.pagame.app.data.model.FriendItem
import gt.pagame.app.ui.components.PagameExtendedFab
import gt.pagame.app.ui.components.PagameBottomSheet
import gt.pagame.app.ui.components.PagameCenteredDialog
import gt.pagame.app.ui.theme.*
import androidx.compose.material.icons.automirrored.filled.ReceiptLong

private fun participantAvatarColors(name: String): Pair<Color, Color> {
    val normalizedName = name.lowercase()
    return when {
        normalizedName == "tú" -> Color(0xFFFDEEE9) to Color(0xFFB83214)
        normalizedName.startsWith("santi") -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        normalizedName.startsWith("lu") || normalizedName.startsWith("vale") ->
            Color(0xFFFFE4E6) to Color(0xFFBE123C)
        normalizedName.startsWith("pedro") -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
        normalizedName.startsWith("sofi") -> Color(0xFFEDE9FE) to Color(0xFF7C3AED)
        normalizedName.startsWith("mario") -> Color(0xFFFFEDD5) to Color(0xFFC2410C)
        normalizedName.startsWith("diana") -> Color(0xFFFCE7F3) to Color(0xFFBE185D)
        normalizedName.startsWith("carlos") -> Color(0xFFE2E8F0) to Color(0xFF334155)
        else -> Color(0xFFD1FAE5) to Color(0xFF047857)
    }
}

@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {

            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = uiState.subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Q${String.format("%,d", uiState.totalAmount.toInt())} total",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = viewModel::requestDeleteEvent,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar evento",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Segmented Control (Gastos vs Balance)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    val isExpenses = uiState.selectedTab == EventDetailTab.EXPENSES

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isExpenses) MaterialTheme.colorScheme.surface
                                else Color.Transparent
                            )
                            .clickable { viewModel.selectTab(EventDetailTab.EXPENSES) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isExpenses) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Gastos (${uiState.expenses.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExpenses) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (!isExpenses) MaterialTheme.colorScheme.surface
                                else Color.Transparent
                            )
                            .clickable { viewModel.selectTab(EventDetailTab.BALANCE) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (!isExpenses) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Balance",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isExpenses) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Tab Content
            if (uiState.selectedTab == EventDetailTab.EXPENSES) {
                ExpensesTabView(
                    uiState = uiState,
                    onExpenseClick = { viewModel.openEditExpenseDialog(it) },
                    onParticipantsClick = { viewModel.openParticipantsSheet() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                BalanceTabView(
                    uiState = uiState,
                    onSettleSuggestion = viewModel::settleBalanceSuggestion,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Floating Action Button (only on Expenses tab)
        if (uiState.selectedTab == EventDetailTab.EXPENSES) {
            PagameExtendedFab(
                text = "Agregar gasto",
                icon = Icons.Default.Add,
                onClick = { viewModel.openAddExpenseDialog() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 90.dp)
            )
        }

        // Add/Edit Expense Dialog
        if (uiState.isAddExpenseDialogOpen) {
            val isEditing = uiState.editingExpenseId != null
            PagameBottomSheet(
                onDismissRequest = { viewModel.closeAddExpenseDialog() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, top = 12.dp, end = 18.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEditing) "Editar gasto" else "Agregar nuevo gasto",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (isEditing) {
                            Text(
                                text = uiState.title,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isEditing) {
                        IconButton(
                            onClick = { viewModel.deleteExpense() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar gasto",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 510.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isEditing) {
                            Text(
                                text = "Concepto del gasto",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedTextField(
                            value = uiState.newExpenseTitle,
                            onValueChange = viewModel::updateNewTitle,
                            placeholder = {
                                Text(if (isEditing) "¿Qué se pagó?" else "Concepto del gasto")
                            },
                            trailingIcon = { Text("🍕", fontSize = 18.sp) },
                            singleLine = true,
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

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isEditing) {
                            Text(
                                text = "Monto (Q)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedTextField(
                            value = uiState.newExpenseAmount,
                            onValueChange = viewModel::updateNewAmount,
                            placeholder = { Text(if (isEditing) "0.00" else "Monto (Q)") },
                            prefix = {
                                Text(
                                    text = "Q ",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿QUIÉN PAGÓ?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.4.sp
                        )
                        Text(
                            text = "Pagado por: ${uiState.newExpensePayer}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.payerOptions.forEach { name ->
                            val isSelected = uiState.newExpensePayer == name
                            val (avatarBackground, avatarContent) = participantAvatarColors(name)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .widthIn(min = 48.dp)
                                    .clickable { viewModel.updateNewPayer(name) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else avatarBackground
                                        )
                                        .border(
                                            if (isSelected) 2.dp else 1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (name == "Tú") "Tú" else name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else avatarContent
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(17.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                                .border(2.dp, Color.White, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = name.substringBefore(" "),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "¿QUIÉN PARTICIPA?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.4.sp
                            )
                            Text(
                                text = "${uiState.newExpenseIncludedNames.size}/${uiState.allParticipantNames.size} seleccionados",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val allSelected = uiState.newExpenseIncludedNames.size == uiState.allParticipantNames.size
                        Text(
                            text = if (allSelected) "Desmarcar todos" else "Marcar todos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { viewModel.toggleAllIncluded() }
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            uiState.allParticipantNames.forEach { name ->
                                val isChecked = name in uiState.newExpenseIncludedNames
                                val (avatarBackground, avatarContent) = participantAvatarColors(name)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
                                                Color.White
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.toggleIncludedParticipant(name) }
                                        .padding(start = 9.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(avatarBackground),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (name == "Tú") "Tú" else name.take(1),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = avatarContent
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (name == "Tú") "Tú (Organizador)" else name,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 12.5.sp,
                                        fontWeight = if (name == "Tú") FontWeight.Bold else FontWeight.Medium
                                    )
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { viewModel.toggleIncludedParticipant(name) }
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { viewModel.closeAddExpenseDialog() }) {
                        Text("Cancelar", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.saveExpense() },
                        enabled = uiState.newExpenseTitle.isNotBlank() &&
                            (uiState.newExpenseAmount.toDoubleOrNull() ?: 0.0) > 0.0 &&
                            uiState.newExpenseIncludedNames.isNotEmpty(),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp)
                    ) {
                        Text(
                            text = if (isEditing) "Guardar cambios" else "Guardar",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        if (uiState.isParticipantsSheetOpen) {
            ParticipantsEditorSheet(
                uiState = uiState,
                onSearchChange = viewModel::updateParticipantSearch,
                onParticipantClick = viewModel::toggleEventParticipant,
                onOrganizerClick = viewModel::showOrganizerRemovalBlockedMessage,
                onDismiss = viewModel::closeParticipantsSheet,
                onDone = viewModel::requestSaveParticipantChanges
            )
        }

        uiState.participantRemovalBlockedMessage?.let { message ->
            ParticipantRemovalBlockedDialog(
                message = message,
                onDismiss = viewModel::dismissParticipantRemovalBlockedMessage
            )
        }

        if (uiState.isParticipantChangesConfirmationOpen) {
            ParticipantChangesConfirmationDialog(
                addedNames = uiState.addedParticipantNames,
                removedNames = uiState.removedParticipantNames,
                onDismiss = viewModel::closeParticipantChangesConfirmation,
                onConfirm = viewModel::confirmParticipantChanges
            )
        }

        uiState.deleteEventBlockedMessage?.let { message ->
            EventDeletionBlockedDialog(
                message = message,
                onDismiss = viewModel::dismissDeleteEventBlockedMessage
            )
        }

        if (uiState.isDeleteEventConfirmationOpen) {
            EventDeletionConfirmationDialog(
                eventTitle = uiState.title,
                onDismiss = viewModel::dismissDeleteEventConfirmation,
                onConfirm = {
                    viewModel.confirmDeleteEvent()
                    onNavigateBack()
                }
            )
        }
    }
}

@Composable
private fun ExpensesTabView(
    uiState: EventDetailUiState,
    onExpenseClick: (ExpenseItem) -> Unit,
    onParticipantsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
    ) {
        // Summary Cards: Total Gastado and Participantes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Total Gastado
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                                    listOf(Color(0xFF2A9D8F), Color(0xFF1F6F68))
                                } else {
                                    listOf(MaterialTheme.colorScheme.primary, Color(0xFF991B1B))
                                }
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Total Gastado",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Q${String.format("%,d", uiState.totalAmount.toInt())}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Moneda oficial GT",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
                        )
                    }
                }

                // Card 2: Participantes
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            RoundedCornerShape(24.dp)
                        )
                        .clickable(onClick = onParticipantsClick)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Participantes",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${uiState.participantCount} personas",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Q${String.format("%,d", uiState.averagePerPerson.toInt())} promedio / p",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        // Section header: Detalle de gastos
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DETALLE DE GASTOS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Orden cronológico",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(uiState.expenses, key = { it.id }) { exp ->
            ExpenseCard(expense = exp, onClick = { onExpenseClick(exp) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParticipantsEditorSheet(
    uiState: EventDetailUiState,
    onSearchChange: (String) -> Unit,
    onParticipantClick: (FriendItem) -> Unit,
    onOrganizerClick: () -> Unit,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val query = uiState.participantSearchQuery.trim()
    val matchesSearch: (FriendItem) -> Boolean = { friend ->
        query.isBlank() ||
            friend.name.contains(query, ignoreCase = true) ||
            friend.codeOrStatus.contains(query, ignoreCase = true)
    }
    val currentParticipants = uiState.allFriends
        .filter { it.id in uiState.draftParticipantIds && matchesSearch(it) }
        .sortedBy { it.name }
    val otherFriends = uiState.allFriends
        .filter { it.id !in uiState.draftParticipantIds && matchesSearch(it) }
        .sortedBy { it.name }
    val showOrganizer = query.isBlank() ||
        "Tú (Organizador)".contains(query, ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
            Color.White
        } else {
            MaterialTheme.colorScheme.surface
        },
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(42.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 14.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Participantes",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = uiState.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            OutlinedTextField(
                value = uiState.participantSearchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Buscar amigo") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.participantSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Limpiar búsqueda",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showOrganizer || currentParticipants.isNotEmpty()) {
                    item {
                        ParticipantSectionHeader(
                            title = "YA ESTÁN EN EL EVENTO",
                            count = uiState.draftParticipantIds.size + 1,
                            accent = true
                        )
                    }
                }

                if (showOrganizer) {
                    item(key = "organizer") {
                        ParticipantEditorRow(
                            name = "Tú",
                            initials = "Tú",
                            supportingText = "Organizador del evento",
                            isLocked = true,
                            isInEvent = true,
                            onAction = onOrganizerClick
                        )
                    }
                }

                items(currentParticipants, key = { "current_${it.id}" }) { friend ->
                    val expenseCount = uiState.participantExpenseCounts[friend.id] ?: 0
                    val wasJustAdded = friend.id !in uiState.originalParticipantIds
                    ParticipantEditorRow(
                        name = friend.name,
                        initials = friend.name.split(" ")
                            .mapNotNull { it.firstOrNull() }
                            .take(2)
                            .joinToString("")
                            .uppercase(),
                        supportingText = when {
                            wasJustAdded -> "Se agregará al confirmar"
                            expenseCount == 1 -> "1 gasto registrado"
                            expenseCount > 1 -> "$expenseCount gastos registrados"
                            else -> "Sin gastos registrados"
                        },
                        isLocked = expenseCount > 0 && !wasJustAdded,
                        isInEvent = true,
                        onAction = { onParticipantClick(friend) }
                    )
                }

                if (otherFriends.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        )
                        ParticipantSectionHeader(
                            title = "TUS OTROS AMIGOS",
                            count = uiState.allFriends.count { it.id !in uiState.draftParticipantIds },
                            accent = false
                        )
                    }
                    items(otherFriends, key = { "other_${it.id}" }) { friend ->
                        val wasRemoved = friend.id in uiState.originalParticipantIds
                        ParticipantEditorRow(
                            name = friend.name,
                            initials = friend.name.split(" ")
                                .mapNotNull { it.firstOrNull() }
                                .take(2)
                                .joinToString("")
                                .uppercase(),
                            supportingText = if (wasRemoved) {
                                "Se quitará al confirmar"
                            } else {
                                friend.codeOrStatus
                            },
                            isLocked = false,
                            isInEvent = false,
                            onAction = { onParticipantClick(friend) }
                        )
                    }
                }

                if (!showOrganizer && currentParticipants.isEmpty() && otherFriends.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No se encontraron amigos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Prueba buscando con otro nombre o código",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Bloqueado con gastos",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 25.dp, vertical = 11.dp)
                ) {
                    Text("Listo", fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ParticipantSectionHeader(
    title: String,
    count: Int,
    accent: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.6.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = if (accent) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (accent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ParticipantEditorRow(
    name: String,
    initials: String,
    supportingText: String,
    isLocked: Boolean,
    isInEvent: Boolean,
    onAction: () -> Unit
) {
    val (avatarBackground, avatarContent) = participantAvatarColors(name)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onAction)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = avatarContent
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = supportingText,
                fontSize = 10.5.sp,
                fontWeight = if (isLocked) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isLocked -> MaterialTheme.colorScheme.primary
                    supportingText.startsWith("Se ") -> StatusSuccessText
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        IconButton(
            onClick = onAction,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (!isInEvent) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            Icon(
                imageVector = when {
                    isLocked -> Icons.Default.Lock
                    isInEvent -> Icons.Default.Close
                    else -> Icons.Default.Add
                },
                contentDescription = when {
                    isLocked -> "No se puede quitar"
                    isInEvent -> "Quitar del evento"
                    else -> "Agregar al evento"
                },
                tint = if (!isInEvent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun ParticipantRemovalBlockedDialog(
    message: String,
    onDismiss: () -> Unit
) {
    PagameCenteredDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No se puede quitar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("Entendido", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun ParticipantChangesConfirmationDialog(
    addedNames: List<String>,
    removedNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    PagameCenteredDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Confirmar cambios",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Revisa cómo quedarán los participantes antes de guardar.",
                modifier = Modifier.padding(top = 6.dp),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (addedNames.isNotEmpty()) {
                ParticipantChangeSummary(
                    title = "SE AGREGARÁN",
                    names = addedNames,
                    icon = Icons.Default.PersonAdd,
                    containerColor = StatusSuccessBg,
                    contentColor = StatusSuccessText
                )
            }
            if (removedNames.isNotEmpty()) {
                ParticipantChangeSummary(
                    title = "SE QUITARÁN",
                    names = removedNames,
                    icon = Icons.Default.PersonRemove,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Seguir revisando", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp)
                ) {
                    Text("Confirmar", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun EventDeletionBlockedDialog(
    message: String,
    onDismiss: () -> Unit
) {
    PagameCenteredDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No se puede eliminar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("Entendido", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun EventDeletionConfirmationDialog(
    eventTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    PagameCenteredDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Eliminar evento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = "¿Seguro que quieres eliminar \"$eventTitle\"? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun ParticipantChangeSummary(
    title: String,
    names: List<String>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    color = contentColor
                )
            }
            names.forEach { name ->
                Text(
                    text = "• $name",
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ExpenseCard(
    expense: ExpenseItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 14.dp, end = 18.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (expense.iconType == "pizza") Color(0xFFFFEDE6) else Color(0xFFFEF3C7)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (expense.iconType == "pizza") "🍕" else "🍻",
                        fontSize = 20.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Pagó ${expense.paidByName} • ${expense.timeFormatted}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Q${String.format("%,d", expense.amount.toInt())}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${expense.participantCount} personas",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Editar gasto",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BalanceTabView(
    uiState: EventDetailUiState,
    onSettleSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val settledIds = uiState.settledBalanceSuggestionIds
    val pendingCount = 3 - settledIds.size

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = if (isLightTheme) Color(0xFFFFEFEA) else MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                ),
                tonalElevation = 0.dp,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(20.dp))  {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "DEBES EN TOTAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.4.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Q2,066.67",
                                fontSize = 28.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            )
                        ) {
                            Text(
                                text = "Balance neto",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Saldando estas sugerencias quedas al día con el grupo.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 2.dp,
                        top = 5.dp,
                        end = 2.dp,
                        bottom = 1.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SUGERENCIAS PARA SALDAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.7.sp
                )

                Text(
                    text = "$pendingCount pendientes",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
            }
        }

        item {
            BalanceSuggestionCard(
                fromName = "Tú",
                fromInitials = "Tú",
                toName = "Santi Morales",
                amount = "Q1,250.00",
                state = if ("you_to_santi" in settledIds) {
                    BalanceSuggestionState.SETTLED
                } else {
                    BalanceSuggestionState.PAYMENT_REQUIRED
                },
                onPrimaryAction = { onSettleSuggestion("you_to_santi") }
            )
        }

        item {
            BalanceSuggestionCard(
                fromName = "Vale Castillo",
                fromInitials = "VC",
                toName = "Tú",
                amount = "Q816.67",
                message = "Vale Castillo dice que ya te pagó",
                state = if ("vale_to_you" in settledIds) {
                    BalanceSuggestionState.SETTLED
                } else {
                    BalanceSuggestionState.RECEIPT_CONFIRMATION
                },
                onPrimaryAction = { onSettleSuggestion("vale_to_you") }
            )
        }

        item {
            BalanceSuggestionCard(
                fromName = "Pedro Méndez",
                fromInitials = "PM",
                toName = "Santi Morales",
                amount = "Q750.00",
                state = BalanceSuggestionState.SETTLED,
                onPrimaryAction = {}
            )
        }
    }
}

private enum class BalanceSuggestionState {
    PAYMENT_REQUIRED,
    RECEIPT_CONFIRMATION,
    SETTLED
}

@Composable
private fun BalanceSuggestionCard(
    fromName: String,
    fromInitials: String,
    toName: String,
    amount: String,
    state: BalanceSuggestionState,
    onPrimaryAction: () -> Unit,
    message: String? = null,
    modifier: Modifier = Modifier
) {
    val isSettled = state == BalanceSuggestionState.SETTLED
    val avatarColors = participantAvatarColors(fromName)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSettled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outline.copy(alpha = if (isSettled) 0.45f else 0.75f)
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSettled) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(avatarColors.first),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = fromInitials,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = avatarColors.second
                        )
                    }
                    Text(
                        text = fromName,
                        modifier = Modifier.weight(1f, fill = false),
                        fontSize = 12.sp,
                        fontWeight = if (isSettled) FontWeight.Medium else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "→",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                    Text(
                        text = toName,
                        modifier = Modifier.weight(1f, fill = false),
                        fontSize = 12.sp,
                        fontWeight = if (isSettled) FontWeight.Medium else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = amount,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (state == BalanceSuggestionState.PAYMENT_REQUIRED) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1
                )
            }

            message?.let {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (state) {
                BalanceSuggestionState.PAYMENT_REQUIRED -> {
                    Button(
                        onClick = onPrimaryAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Marcar como pagado", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                BalanceSuggestionState.RECEIPT_CONFIRMATION -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPrimaryAction,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusSuccessText,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Sí, lo recibí", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text("Aún no", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                BalanceSuggestionState.SETTLED -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = StatusSuccessText,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Liquidado",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccessText
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun EventDetailScreenPreview() {
    PagameTheme {
        EventDetailScreen(
            viewModel = EventDetailViewModel(),
            onNavigateBack = {}
        )
    }
}
