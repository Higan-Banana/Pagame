package gt.pagame.app.ui.screens.eventdetail

import androidx.lifecycle.ViewModel
import gt.pagame.app.data.model.ExpenseItem
import gt.pagame.app.data.model.FriendItem
import gt.pagame.app.data.model.ParticipantBalance
import gt.pagame.app.data.model.SuggestedPayment
import gt.pagame.app.data.repository.PagameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class EventDetailTab {
    EXPENSES,
    BALANCE
}

data class EventDetailUiState(
    val selectedTab: EventDetailTab = EventDetailTab.EXPENSES,
    val title: String = "Pizza & Birra",
    val subtitle: String = "6 participantes • Hoy, 21:30",
    val totalAmount: Double = 12400.0,
    val participantCount: Int = 6,
    val averagePerPerson: Double = 2066.0,
    val expenses: List<ExpenseItem> = emptyList(),
    val participants: List<ParticipantBalance> = emptyList(),
    val suggestedPayment: SuggestedPayment? = null,
    val isAddExpenseDialogOpen: Boolean = false,
    val editingExpenseId: String? = null,
    val newExpenseTitle: String = "",
    val newExpenseAmount: String = "",
    val newExpensePayer: String = "Tú",
    val newExpenseIncludedNames: List<String> = emptyList(),
    val isSettledNoticeShown: Boolean = false,
    val allFriends: List<FriendItem> = emptyList(),
    val originalParticipantIds: Set<String> = emptySet(),
    val draftParticipantIds: Set<String> = emptySet(),
    val participantSearchQuery: String = "",
    val isParticipantsSheetOpen: Boolean = false,
    val isParticipantChangesConfirmationOpen: Boolean = false,
    val participantRemovalBlockedMessage: String? = null,
    val participantExpenseCounts: Map<String, Int> = emptyMap(),
    val isDeleteEventConfirmationOpen: Boolean = false,
    val deleteEventBlockedMessage: String? = null,
    val settledBalanceSuggestionIds: Set<String> = emptySet()
) {
    val payerOptions: List<String> get() = listOf("Tú") + participants.map { it.name }
    val allParticipantNames: List<String> get() = payerOptions
    val addedParticipantNames: List<String>
        get() = allFriends
            .filter { it.id in (draftParticipantIds - originalParticipantIds) }
            .map { it.name }
    val removedParticipantNames: List<String>
        get() = allFriends
            .filter { it.id in (originalParticipantIds - draftParticipantIds) }
            .map { it.name }
    val hasParticipantChanges: Boolean
        get() = originalParticipantIds != draftParticipantIds
}

class EventDetailViewModel(
    private val repository: PagameRepository = PagameRepository.instance
) : ViewModel() {

    private val initialParticipantIds = repository.pizzaParticipantIds.value
    private val initialParticipantCount = initialParticipantIds.size + 1

    private val _uiState = MutableStateFlow(
        EventDetailUiState(
            subtitle = "$initialParticipantCount participantes • Hoy, 21:30",
            participantCount = initialParticipantCount,
            averagePerPerson = 12400.0 / initialParticipantCount,
            expenses = repository.pizzaExpenses.value,
            participants = repository.pizzaParticipants,
            suggestedPayment = repository.pizzaSuggestedPayment,
            allFriends = repository.friends.value,
            originalParticipantIds = initialParticipantIds,
            draftParticipantIds = initialParticipantIds
        )
    )
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    fun selectTab(tab: EventDetailTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun openParticipantsSheet() {
        val currentIds = repository.pizzaParticipantIds.value
        val friends = repository.friends.value
        val expenses = _uiState.value.expenses
        val expenseCounts = friends.associate { friend ->
            friend.id to expenses.count { expense ->
                expenseIsRelatedToFriend(expense, friend, currentIds)
            }
        }
        _uiState.update {
            it.copy(
                allFriends = friends,
                originalParticipantIds = currentIds,
                draftParticipantIds = currentIds,
                participantExpenseCounts = expenseCounts,
                participantSearchQuery = "",
                isParticipantsSheetOpen = true,
                isParticipantChangesConfirmationOpen = false,
                participantRemovalBlockedMessage = null
            )
        }
    }

    fun closeParticipantsSheet() {
        _uiState.update {
            it.copy(
                draftParticipantIds = it.originalParticipantIds,
                participantSearchQuery = "",
                isParticipantsSheetOpen = false,
                isParticipantChangesConfirmationOpen = false,
                participantRemovalBlockedMessage = null
            )
        }
    }

    fun updateParticipantSearch(query: String) {
        _uiState.update { it.copy(participantSearchQuery = query) }
    }

    fun toggleEventParticipant(friend: FriendItem) {
        val state = _uiState.value
        if (friend.id !in state.draftParticipantIds) {
            _uiState.update { it.copy(draftParticipantIds = it.draftParticipantIds + friend.id) }
            return
        }

        val relatedExpenseCount = state.participantExpenseCounts[friend.id] ?: 0
        if (relatedExpenseCount > 0) {
            val expenseWord = if (relatedExpenseCount == 1) "gasto registrado" else "gastos registrados"
            _uiState.update {
                it.copy(
                    participantRemovalBlockedMessage =
                        "No puedes quitar a ${friend.name} porque ya tiene $relatedExpenseCount $expenseWord. " +
                            "Edita o borra esos gastos primero."
                )
            }
        } else {
            _uiState.update { it.copy(draftParticipantIds = it.draftParticipantIds - friend.id) }
        }
    }

    fun dismissParticipantRemovalBlockedMessage() {
        _uiState.update { it.copy(participantRemovalBlockedMessage = null) }
    }

    fun showOrganizerRemovalBlockedMessage() {
        _uiState.update {
            it.copy(participantRemovalBlockedMessage = "No puedes quitarte del evento porque eres el organizador.")
        }
    }

    fun requestSaveParticipantChanges() {
        _uiState.update {
            if (it.hasParticipantChanges) {
                it.copy(isParticipantChangesConfirmationOpen = true)
            } else {
                it.copy(isParticipantsSheetOpen = false)
            }
        }
    }

    fun closeParticipantChangesConfirmation() {
        _uiState.update { it.copy(isParticipantChangesConfirmationOpen = false) }
    }

    fun confirmParticipantChanges() {
        val participantIds = _uiState.value.draftParticipantIds
        repository.updatePizzaParticipants(participantIds)
        val count = participantIds.size + 1

        _uiState.update {
            val subtitleSuffix = it.subtitle.substringAfter("•", "Hoy, 21:30").trim()
            it.copy(
                subtitle = "$count participantes • $subtitleSuffix",
                participantCount = count,
                averagePerPerson = if (count > 0) it.totalAmount / count else 0.0,
                participants = repository.pizzaParticipants,
                originalParticipantIds = participantIds,
                draftParticipantIds = participantIds,
                participantSearchQuery = "",
                isParticipantsSheetOpen = false,
                isParticipantChangesConfirmationOpen = false,
                participantRemovalBlockedMessage = null
            )
        }
    }

    private fun expenseIsRelatedToFriend(
        expense: ExpenseItem,
        friend: FriendItem,
        currentParticipantIds: Set<String>
    ): Boolean {
        fun matchesFriend(value: String): Boolean {
            val normalized = value.trim()
            return normalized.equals(friend.name, ignoreCase = true) ||
                normalized.equals(friend.name.substringBefore(" "), ignoreCase = true)
        }

        val paidByFriend = matchesFriend(expense.paidByName)
        val includedInExpense = if (expense.includedParticipantNames.isEmpty()) {
            friend.id in currentParticipantIds
        } else {
            expense.includedParticipantNames.any(::matchesFriend)
        }
        return paidByFriend || includedInExpense
    }

    fun openAddExpenseDialog() {
        _uiState.update {
            it.copy(
                isAddExpenseDialogOpen = true,
                editingExpenseId = null,
                newExpenseTitle = "",
                newExpenseAmount = "",
                newExpensePayer = "Tú",
                newExpenseIncludedNames = it.allParticipantNames
            )
        }
    }

    fun openEditExpenseDialog(expense: ExpenseItem) {
        _uiState.update {
            it.copy(
                isAddExpenseDialogOpen = true,
                editingExpenseId = expense.id,
                newExpenseTitle = expense.title,
                newExpenseAmount = expense.amount.toInt().toString(),
                newExpensePayer = expense.paidByName,
                newExpenseIncludedNames = expense.includedParticipantNames.ifEmpty { it.allParticipantNames }
            )
        }
    }

    fun closeAddExpenseDialog() {
        _uiState.update {
            it.copy(
                isAddExpenseDialogOpen = false,
                editingExpenseId = null,
                newExpenseTitle = "",
                newExpenseAmount = ""
            )
        }
    }

    fun updateNewTitle(title: String) {
        _uiState.update { it.copy(newExpenseTitle = title) }
    }

    fun updateNewAmount(amount: String) {
        _uiState.update { it.copy(newExpenseAmount = amount) }
    }

    fun updateNewPayer(payer: String) {
        _uiState.update { it.copy(newExpensePayer = payer) }
    }

    fun toggleIncludedParticipant(name: String) {
        _uiState.update {
            val current = it.newExpenseIncludedNames
            val updated = if (name in current) current - name else current + name
            it.copy(newExpenseIncludedNames = updated)
        }
    }

    fun toggleAllIncluded() {
        _uiState.update {
            val allSelected = it.newExpenseIncludedNames.size == it.allParticipantNames.size
            it.copy(newExpenseIncludedNames = if (allSelected) emptyList() else it.allParticipantNames)
        }
    }

    fun saveExpense() {
        val state = _uiState.value
        val amount = state.newExpenseAmount.toDoubleOrNull() ?: 0.0
        if (state.newExpenseTitle.isNotBlank() && amount > 0 && state.newExpenseIncludedNames.isNotEmpty()) {
            val editingId = state.editingExpenseId
            if (editingId != null) {
                repository.updateExpense(editingId, state.newExpenseTitle, amount, state.newExpensePayer, state.newExpenseIncludedNames)
            } else {
                repository.addExpense(state.newExpenseTitle, amount, state.newExpensePayer, state.newExpenseIncludedNames)
            }
            val updated = repository.pizzaExpenses.value
            val newTotal = updated.sumOf { it.amount }
            _uiState.update {
                it.copy(
                    expenses = updated,
                    totalAmount = newTotal,
                    averagePerPerson = newTotal / it.participantCount,
                    isAddExpenseDialogOpen = false,
                    editingExpenseId = null,
                    newExpenseTitle = "",
                    newExpenseAmount = ""
                )
            }
        }
    }

    fun deleteExpense() {
        val editingId = _uiState.value.editingExpenseId ?: return
        repository.deleteExpense(editingId)
        val updated = repository.pizzaExpenses.value
        val newTotal = updated.sumOf { it.amount }
        _uiState.update {
            it.copy(
                expenses = updated,
                totalAmount = newTotal,
                averagePerPerson = if (it.participantCount > 0) newTotal / it.participantCount else 0.0,
                isAddExpenseDialogOpen = false,
                editingExpenseId = null,
                newExpenseTitle = "",
                newExpenseAmount = ""
            )
        }
    }

    fun requestDeleteEvent() {
        val expenseCount = _uiState.value.expenses.size
        if (expenseCount > 0) {
            val expenseWord = if (expenseCount == 1) "gasto registrado" else "gastos registrados"
            _uiState.update {
                it.copy(
                    deleteEventBlockedMessage =
                        "No puedes eliminar este evento porque tiene $expenseCount $expenseWord. " +
                            "Elimina esos gastos primero."
                )
            }
        } else {
            _uiState.update { it.copy(isDeleteEventConfirmationOpen = true) }
        }
    }

    fun dismissDeleteEventBlockedMessage() {
        _uiState.update { it.copy(deleteEventBlockedMessage = null) }
    }

    fun dismissDeleteEventConfirmation() {
        _uiState.update { it.copy(isDeleteEventConfirmationOpen = false) }
    }

    fun confirmDeleteEvent() {
        repository.deleteEvent("event_pizza")
        _uiState.update { it.copy(isDeleteEventConfirmationOpen = false) }
    }

    fun settleAccounts() {
        repository.settlePizzaEvent()
        _uiState.update { it.copy(isSettledNoticeShown = true) }
    }

    fun settleBalanceSuggestion(suggestionId: String) {
        _uiState.update { state ->
            val settledIds = state.settledBalanceSuggestionIds + suggestionId
            if (settledIds.containsAll(setOf("you_to_santi", "vale_to_you"))) {
                repository.settlePizzaEvent()
            }
            state.copy(
                settledBalanceSuggestionIds = settledIds,
                isSettledNoticeShown = true
            )
        }
    }
}
