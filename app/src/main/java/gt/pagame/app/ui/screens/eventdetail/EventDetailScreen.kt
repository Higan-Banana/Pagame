package gt.pagame.app.ui.screens.eventdetail

import gt.pagame.app.data.model.*
import gt.pagame.app.ui.components.*
import kotlin.math.roundToLong
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gt.pagame.app.data.model.ExpenseItem
import gt.pagame.app.data.model.FriendItem
import gt.pagame.app.data.model.titleInitial
import gt.pagame.app.ui.components.PagameExtendedFab
import gt.pagame.app.ui.components.PagameBottomSheet
import gt.pagame.app.ui.components.PagameCenteredDialog
import gt.pagame.app.ui.theme.*
import androidx.compose.material.icons.automirrored.filled.ReceiptLong

@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    OperationError(uiState.errorMessage, viewModel::dismissError)
    if (uiState.loading || uiState.event == null) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(Spacing.page),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            if (uiState.loading) CircularProgressIndicator() else Text("El evento ya no está disponible")
            TextButton(onClick = onNavigateBack) { Text("Volver") }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
        ) {

            PagameTopAppBar(
                title = uiState.title,
                titleStyle = MaterialTheme.typography.titleLarge,
                titleMaxLines = 2,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    Text(
                        text = "${money((uiState.totalAmount * 100).roundToLong(), uiState.currency)} total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.widthIn(max = 130.dp),
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = viewModel::requestDeleteEvent, enabled = !uiState.busy) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar evento",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            )

            Text(
                text = "${uiState.subtitle}\nCreado por: ${uiState.createdByName}",
                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(start = Spacing.large, end = Spacing.large, bottom = Spacing.small)
            )

            // Segmented Control (Gastos vs Balance)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.large, vertical = Spacing.small)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(Spacing.tiny)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    val isExpenses = uiState.selectedTab == EventDetailTab.EXPENSES

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isExpenses) MaterialTheme.colorScheme.surface
                                else MaterialTheme.pagameColors.transparent
                            )
                            .clickable { viewModel.selectTab(EventDetailTab.EXPENSES) }
                            .padding(vertical = Spacing.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isExpenses) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Gastos (${uiState.expenses.size})",
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
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
                                else MaterialTheme.pagameColors.transparent
                            )
                            .clickable { viewModel.selectTab(EventDetailTab.BALANCE) }
                            .padding(vertical = Spacing.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (!isExpenses) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Balance",
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                fontWeight = FontWeight.Bold,
                                color = if (!isExpenses) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            PagamePullToRefresh(Modifier.weight(1f).fillMaxWidth()) {
                // Tab Content
                if (uiState.selectedTab == EventDetailTab.EXPENSES) {
                    ExpensesTabView(
                        uiState = uiState,
                        onExpenseClick = { viewModel.openEditExpenseDialog(it) },
                        onParticipantsClick = { viewModel.openParticipantsSheet() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    BalanceTabView(
                        uiState = uiState,
                        onReport = viewModel::reportPayment,
                        onConfirm = viewModel::confirmPayment,
                        onReject = viewModel::rejectPayment,
                        modifier = Modifier.fillMaxSize()
                    )
                }
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
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(end = Spacing.large, bottom = Spacing.fabClearance)
            )
        }

        if (uiState.showRestoreDraftDialog) {
            AlertDialog(onDismissRequest = viewModel::dismissRestoreDraft,
                title = { Text("Tienes un gasto sin terminar") },
                text = { Text("El borrador se conserva en este teléfono durante 30 minutos desde el último cambio. ¿Quieres continuar o empezar uno nuevo?") },
                confirmButton = { TextButton(onClick = viewModel::restoreExpenseDraft) {
                    Text("Continuar borrador", fontWeight = FontWeight.Bold) } },
                dismissButton = { TextButton(onClick = viewModel::startNewExpense) {
                    Text("Crear uno nuevo", fontWeight = FontWeight.Bold) } }, containerColor = MaterialTheme.colorScheme.surface)
        }

        // Add/Edit Expense Dialog
        if (uiState.isExpenseSheetOpen) {
            val isEditing = uiState.editingExpenseId != null
            PagameBottomSheet(
                onDismissRequest = { viewModel.closeAddExpenseDialog() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Spacing.page, top = Spacing.compact, end = Spacing.large, bottom = Spacing.small),
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
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
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
                        .padding(horizontal = Spacing.page, vertical = Spacing.small),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                        if (isEditing) {
                            Text(
                                text = "Concepto del gasto",
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
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

                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                        if (isEditing) {
                            Text(
                                text = "Monto (${uiState.currency.symbol})",
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedTextField(
                            value = uiState.newExpenseAmount,
                            onValueChange = viewModel::updateNewAmount,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = uiState.amountInputError != null,
                            supportingText = if (uiState.amountInputError != null) {
                                { Text(uiState.amountInputError.orEmpty(), fontSize = MaterialTheme.typography.labelMedium.fontSize) }
                            } else null,
                            placeholder = { Text("Máximo: 1,000,000.00", fontSize = MaterialTheme.typography.labelLarge.fontSize) },
                            prefix = {
                                Text(
                                    text = "${uiState.currency.symbol} ",
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
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
                        )
                        Text(
                            text = "Pagado por: ${uiState.expensePayerName}",
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.compact)
                    ) {
                        uiState.payerNames.forEach { name ->
                            val isSelected = uiState.expensePayerName == name
                            val (avatarBackground, avatarContent) = avatarColors(name)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .widthIn(min = 48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { viewModel.updateNewPayer(name) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(avatarBackground, CircleShape)
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
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = avatarContent
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(17.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(Spacing.tiny))
                                Text(
                                    text = name.substringBefore(" "),
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
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
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
                            )
                            Text(
                                text = "${uiState.newExpenseIncludedNames.size}/${uiState.allParticipantNames.size} seleccionados",
                                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val allSelected = uiState.newExpenseIncludedNames.size == uiState.allParticipantNames.size
                        Text(
                            text = if (allSelected) "Desmarcar todos" else "Marcar todos",
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { viewModel.toggleAllIncluded() }
                        )
                    }

                    Box {
                        Column(modifier = Modifier.padding(Spacing.tiny), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                            uiState.allParticipantNames.forEach { name ->
                                val isChecked = name in uiState.newExpenseIncludedNames
                                val (avatarBackground, avatarContent) = avatarColors(name)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
                                                MaterialTheme.colorScheme.surface
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
                                        .padding(start = Spacing.small, top = Spacing.tiny, bottom = Spacing.tiny),
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
                                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                            fontWeight = FontWeight.Bold,
                                            color = avatarContent
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(Spacing.compact))
                                    Text(
                                        text = if (name == "Tú" && uiState.isOrganizer) "Tú (Organizador)" else name,
                                        modifier = Modifier.weight(1f),
                                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
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
                        .padding(horizontal = Spacing.large, vertical = Spacing.medium),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { viewModel.closeAddExpenseDialog() }) {
                        Text("Cancelar", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Button(
                        onClick = { viewModel.saveExpense() },
                        enabled = !uiState.busy && uiState.newExpenseTitle.isNotBlank() &&
                            gt.pagame.app.data.model.amountMinor(uiState.newExpenseAmount) != null && uiState.amountInputError == null &&
                            uiState.newExpenseIncludedNames.isNotEmpty(),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = Spacing.page, vertical = Spacing.compact)
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
                    viewModel.confirmDeleteEvent(onNavigateBack)
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
    val bottomInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues()
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .consumeWindowInsets(bottomInsets)
            .padding(horizontal = Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        contentPadding = PaddingValues(top = Spacing.small, bottom = Spacing.bottomClearance + bottomInsets.calculateBottomPadding())
    ) {
        // Summary Cards: Total Gastado and Participantes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.compact)
            ) {
                // Card 1: Total Gastado
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.pagameColors.gradientEnd)
                                } else {
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.pagameColors.balanceGradientEnd)
                                }
                            )
                        )
                        .padding(Spacing.medium)
                ) {
                    Column {
                        Text(
                            text = "Total Gastado",
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(Spacing.small))
                        Text(
                            text = money((uiState.totalAmount * 100).roundToLong(), uiState.currency),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(Spacing.tiny))
                        Text(
                            text = uiState.currency.displayName,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
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
                        .padding(Spacing.medium)
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Participantes",
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(Spacing.small))
                            Text(
                                text = "${uiState.participantCount} personas",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.compact))
                        Text(
                            text = "${money((uiState.averagePerPerson * 100).roundToLong(), uiState.currency)} promedio / p",
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
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
                    .padding(top = Spacing.small, bottom = Spacing.tiny),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DETALLE DE GASTOS",
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
                )
                Text(
                    text = "Más recientes primero",
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (uiState.expenses.isEmpty()) item {
            Text("Todavía no hay gastos. Agrega el primero para calcular el balance.",
                modifier = Modifier.padding(vertical = Spacing.page), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(uiState.expenses, key = { it.id }) { exp ->
            ExpenseCard(expense = exp, currency = uiState.currency, onClick = { onExpenseClick(exp) })
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
    val (currentParticipants, otherFriends) = remember(
        uiState.participantCandidates, uiState.draftParticipantIds, query
    ) {
        uiState.participantCandidates
            .filter { friend ->
                query.isBlank() ||
                    friend.name.contains(query, ignoreCase = true) ||
                    friend.codeOrStatus.contains(query, ignoreCase = true)
            }
            .sortedBy { it.name }
            .partition { it.id in uiState.draftParticipantIds }
    }
    val showOrganizer = query.isBlank() ||
        "Tú (Organizador)".contains(query, ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.safeDrawing },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surface
        },
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
                .fillMaxWidth()
                .heightIn(max = 720.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.page, end = Spacing.medium, bottom = Spacing.compact),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Participantes",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(Spacing.tiny))
                    Text(
                        text = uiState.title,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
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
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.page)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = Spacing.page, vertical = Spacing.compact),
                verticalArrangement = Arrangement.spacedBy(Spacing.tiny)
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
                            avatarKey = friend.id,
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
                            modifier = Modifier.padding(vertical = Spacing.compact),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        )
                        ParticipantSectionHeader(
                            title = "TUS OTROS AMIGOS",
                            count = uiState.participantCandidates.count { it.id !in uiState.draftParticipantIds },
                            accent = false
                        )
                    }
                    items(otherFriends, key = { "other_${it.id}" }) { friend ->
                        val wasRemoved = friend.id in uiState.originalParticipantIds
                        ParticipantEditorRow(
                            name = friend.name,
                            avatarKey = friend.id,
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
                                .padding(vertical = Spacing.roomy),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(Spacing.small))
                            Text(
                                text = "No se encontraron amigos",
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize
                            )
                            Text(
                                text = "Prueba buscando con otro nombre o código",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = MaterialTheme.typography.labelMedium.fontSize
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.page, vertical = Spacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.tiny)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Bloqueado con gastos",
                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = Spacing.page, vertical = Spacing.compact)
                ) {
                    Text("Listo", fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.width(Spacing.small))
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
            .padding(top = Spacing.tiny, bottom = Spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = MaterialTheme.typography.labelMedium.fontSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = if (accent) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.tiny),
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
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
    avatarKey: String = name,
    initials: String,
    supportingText: String,
    isLocked: Boolean,
    isInEvent: Boolean,
    onAction: () -> Unit
) {
    val (avatarBackground, avatarContent) = avatarColors(avatarKey)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onAction)
            .padding(horizontal = Spacing.small, vertical = Spacing.small),
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
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                fontWeight = FontWeight.ExtraBold,
                color = avatarContent
            )
        }
        Spacer(modifier = Modifier.width(Spacing.compact))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = MaterialTheme.typography.labelLarge.fontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = supportingText,
                fontSize = MaterialTheme.typography.labelMedium.fontSize,
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
                .size(48.dp)
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
        Column(modifier = Modifier.padding(Spacing.page)) {
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
            Spacer(modifier = Modifier.height(Spacing.medium))
            Text(
                text = "No se puede quitar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.page))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = Spacing.compact)
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
        Column(modifier = Modifier.padding(Spacing.page)) {
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
            Spacer(modifier = Modifier.height(Spacing.medium))
            Text(
                text = "Confirmar cambios",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Revisa cómo quedarán los participantes antes de guardar.",
                modifier = Modifier.padding(top = Spacing.small),
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
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

            Spacer(modifier = Modifier.height(Spacing.large))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Seguir revisando", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(Spacing.small))
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = Spacing.large, vertical = Spacing.compact)
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
        Column(modifier = Modifier.padding(Spacing.page)) {
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
            Spacer(modifier = Modifier.height(Spacing.medium))
            Text(
                text = "No se puede eliminar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.page))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = Spacing.compact)
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
        Column(modifier = Modifier.padding(Spacing.page)) {
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
            Spacer(modifier = Modifier.height(Spacing.medium))
            Text(
                text = "Eliminar evento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Text(
                text = "¿Seguro que quieres eliminar \"$eventTitle\"? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.page))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(Spacing.small))
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    contentPadding = PaddingValues(horizontal = Spacing.large, vertical = Spacing.compact)
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
            .padding(top = Spacing.medium),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = title,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing,
                    color = contentColor
                )
            }
            names.forEach { name ->
                Text(
                    text = "• $name",
                    modifier = Modifier.padding(start = Spacing.page, top = Spacing.tiny),
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
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
    currency: Currency,
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
                .padding(start = Spacing.medium, top = Spacing.medium, end = Spacing.large, bottom = Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.compact)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = titleInitial(expense.title),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize
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
                    Spacer(modifier = Modifier.height(Spacing.tiny))
                    Text(
                        text = "Pagó ${expense.paidByName}\nAgregado por: ${expense.createdByName}\n${expense.createdAtLabel.substringBefore(" • ")}",
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.small))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = money((expense.amount * 100).roundToLong(), currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

            }
            Spacer(modifier = Modifier.width(Spacing.medium))
            IconButton(
                onClick = onClick,
                enabled = expense.canEdit,
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
    onReport: (Settlement) -> Unit,
    onConfirm: (String) -> Unit,
    onReject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val event = uiState.event ?: return
    val people = LocalPeople.current
    val uid = uiState.currentUid
    fun name(id: String): String = if (id == uid) "Tú" else people[id]?.name ?: "Usuario"
    val suggestions = event.suggestions()
    val pending = event.payments.filter { it.status == "pending" }
    val balance = event.balances()[uid] ?: 0L
    val pendingCount = suggestions.count { suggestion -> pending.none { it.fromId == suggestion.fromId && it.toId == suggestion.toId } } + pending.size
    val bottomInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues()

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .consumeWindowInsets(bottomInsets)
            .padding(horizontal = Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.compact),
        contentPadding = PaddingValues(top = Spacing.small, bottom = Spacing.bottomClearance + bottomInsets.calculateBottomPadding())
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = if (isLightTheme) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                ),
                tonalElevation = 0.dp,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(Spacing.large)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = when { balance < 0 -> "DEBES EN TOTAL"; balance > 0 -> "TE DEBEN EN TOTAL"; else -> "ESTÁS AL DÍA" },
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(Spacing.tiny))
                            Text(
                                text = money(kotlin.math.abs(balance), event.currency),
                                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                                lineHeight = MaterialTheme.typography.headlineLarge.lineHeight,
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
                                modifier = Modifier.padding(horizontal = Spacing.compact, vertical = Spacing.tiny),
                                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(
                        text = "Los pagos se registran cuando quien recibe confirma.",
                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
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
                        start = Spacing.tiny,
                        top = Spacing.tiny,
                        end = Spacing.tiny,
                        bottom = Spacing.tiny
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SUGERENCIAS PARA SALDAR",
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
                )

                Text(
                    text = "$pendingCount pendientes",
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
            }
        }

        if (suggestions.isEmpty() && pending.isEmpty()) item {
            Text(if (event.expenses.isEmpty()) "Agrega gastos para calcular el balance." else "No hay pagos pendientes.",
                modifier = Modifier.padding(vertical = Spacing.medium), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(suggestions.filter { suggestion -> pending.none { it.fromId == suggestion.fromId && it.toId == suggestion.toId } },
            key = { "suggestion_${it.fromId}_${it.toId}" }) { suggestion ->
            BalanceSuggestionCard(
                fromName = name(suggestion.fromId), fromInitials = people[suggestion.fromId]?.initial ?: "?",
                fromKey = suggestion.fromId, toName = name(suggestion.toId), amount = money(suggestion.amountMinor, event.currency),
                state = if (suggestion.fromId == uid) BalanceSuggestionState.PAYMENT_REQUIRED else BalanceSuggestionState.WAITING,
                onPrimaryAction = { onReport(suggestion) }, enabled = !uiState.busy && pending.isEmpty(),
                message = if (suggestion.fromId == uid) null else "Pendiente de pago")
        }
        items(event.payments.sortedBy { it.status == "confirmed" }, key = { "payment_${it.id}" }) { payment ->
            BalanceSuggestionCard(
                fromName = name(payment.fromId), fromInitials = people[payment.fromId]?.initial ?: "?",
                fromKey = payment.fromId, toName = name(payment.toId), amount = money(payment.amountMinor, event.currency),
                state = when {
                    payment.status == "confirmed" -> BalanceSuggestionState.SETTLED
                    payment.toId == uid -> BalanceSuggestionState.RECEIPT_CONFIRMATION
                    else -> BalanceSuggestionState.WAITING
                },
                message = if (payment.status == "pending") "${name(payment.fromId)} indicó que pagó. Confirma el dinero recibido aunque los gastos hayan cambiado." else null,
                enabled = !uiState.busy, onPrimaryAction = { onConfirm(payment.id) }, onReject = { onReject(payment.id) })
        }
    }
}

private enum class BalanceSuggestionState {
    PAYMENT_REQUIRED,
    RECEIPT_CONFIRMATION,
    WAITING,
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
    fromKey: String = fromName,
    onReject: () -> Unit = {},
    enabled: Boolean = true,
    message: String? = null,
    modifier: Modifier = Modifier
) {
    val isSettled = state == BalanceSuggestionState.SETTLED
    val avatarColors = avatarColors(fromKey)

    // Use the current elevation directly when the action button leaves composition.
    // A retained CardElevation can leave a stale shadow on a translucent settled card.
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (isSettled) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outline.copy(alpha = if (isSettled) 0.45f else 0.75f)
            )
        ),
        shadowElevation = if (isSettled) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.compact)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
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
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = avatarColors.second
                        )
                    }
                    Text(
                        text = fromName,
                        modifier = Modifier.weight(1f, fill = false),
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        fontWeight = if (isSettled) FontWeight.Medium else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "→",
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                    Text(
                        text = toName,
                        modifier = Modifier.weight(1f, fill = false),
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        fontWeight = if (isSettled) FontWeight.Medium else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.compact))
                Text(
                    text = amount,
                    fontSize = MaterialTheme.typography.labelLarge.fontSize,
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
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (state) {
                BalanceSuggestionState.PAYMENT_REQUIRED -> {
                    Button(
                        onClick = onPrimaryAction,
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = Spacing.medium, vertical = Spacing.small)
                    ) {
                        Text("Marcar como pagado", fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.ExtraBold)
                    }
                }

                BalanceSuggestionState.RECEIPT_CONFIRMATION -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.compact)
                    ) {
                        Button(
                            onClick = onPrimaryAction,
                        enabled = enabled,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusSuccessText,
                                contentColor = MaterialTheme.colorScheme.surface
                            ),
                            contentPadding = PaddingValues(horizontal = Spacing.compact, vertical = Spacing.small)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.tiny))
                            Text("Sí, lo recibí", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.ExtraBold)
                        }
                        OutlinedButton(
                            onClick = onReject,
                            enabled = enabled,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = Spacing.compact, vertical = Spacing.small)
                        ) {
                            Text("Aún no", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                BalanceSuggestionState.WAITING -> {
                    Text("Pendiente de confirmación o pago", fontSize = MaterialTheme.typography.labelMedium.fontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BalanceSuggestionState.SETTLED -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.tiny)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = StatusSuccessText,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Liquidado",
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccessText
                        )
                    }
                }
            }
        }
    }
}
