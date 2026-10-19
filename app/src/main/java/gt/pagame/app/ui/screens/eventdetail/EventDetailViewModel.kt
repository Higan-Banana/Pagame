package gt.pagame.app.ui.screens.eventdetail

import androidx.lifecycle.SavedStateHandle
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gt.pagame.app.data.model.*
import gt.pagame.app.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.util.UUID

enum class EventDetailTab { EXPENSES, BALANCE }
data class EventDetailUiState(
    val selectedTab: EventDetailTab = EventDetailTab.EXPENSES,
    val title: String = "", val subtitle: String = "", val totalAmount: Double = 0.0,
    val createdByName: String = "",
    val participantCount: Int = 0, val averagePerPerson: Double = 0.0,
    val expenses: List<ExpenseItem> = emptyList(), val participants: List<ParticipantBalance> = emptyList(),
    val showRestoreDraftDialog: Boolean = false, val amountInputError: String? = null,
    val isExpenseSheetOpen: Boolean = false, val editingExpenseId: String? = null,
    val newExpenseTitle: String = "", val newExpenseAmount: String = "", val expensePayerName: String = "Tú",
    val newExpenseIncludedNames: List<String> = emptyList(), val participantCandidates: List<FriendItem> = emptyList(),
    val originalParticipantIds: Set<String> = emptySet(), val draftParticipantIds: Set<String> = emptySet(),
    val participantSearchQuery: String = "", val isParticipantsSheetOpen: Boolean = false,
    val isParticipantChangesConfirmationOpen: Boolean = false, val participantRemovalBlockedMessage: String? = null,
    val participantExpenseCounts: Map<String, Int> = emptyMap(), val isDeleteEventConfirmationOpen: Boolean = false,
    val deleteEventBlockedMessage: String? = null, val busy: Boolean = false, val errorMessage: String? = null,
    val event: OnlineEvent? = null, val currency: Currency = Currency.GTQ, val currentUid: String = "",
    val isOrganizer: Boolean = false, val loading: Boolean = true
) {
    val payerNames get() = listOf("Tú") + participants.map { it.name }
    val allParticipantNames get() = payerNames
    val addedParticipantNames get(): List<String> {
        val ids = draftParticipantIds - originalParticipantIds
        return participantCandidates.filter { it.id in ids }.map { it.name }
    }
    val removedParticipantNames get(): List<String> {
        val ids = originalParticipantIds - draftParticipantIds
        return participantCandidates.filter { it.id in ids }.map { it.name }
    }
    val hasParticipantChanges get() = originalParticipantIds != draftParticipantIds
}
class EventDetailViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val repository = PagameRepository.instance
    private val draftStore = ExpenseDraftStore(application)
    private var expenseDraftId = UUID.randomUUID().toString()
    private val eventId: String = checkNotNull(savedStateHandle["eventId"])
    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState = _uiState.asStateFlow()
    init {
        viewModelScope.launch {
            combine(repository.onlineEvents, repository.people, repository.friends, repository.loading) { events, people, friends, loading ->
                val event = events.firstOrNull { it.id == eventId }
                val uid = repository.sessionUid.value.orEmpty()
                _uiState.update { state ->
                    if (event == null) state.copy(event = null, loading = loading)
                    else {
                        val balances = event.balances()
                        val others = event.participantIds.filter { it != uid }
                        val nonOwnerParticipantIds = (event.participantIds - event.ownerId).toSet()
                        state.copy(event = event, title = event.title, currency = event.currency,
                            createdByName = repository.displayName(event.ownerId),
                            currentUid = uid, isOrganizer = uid == event.ownerId,
                            loading = loading || !people.keys.containsAll(event.participantIds),
                            subtitle = "${PagameRepository.dateLabel(event.createdAt)}\n${event.participantIds.size} participantes",
                            participantCount = event.participantIds.size, totalAmount = event.totalMinor / 100.0,
                            averagePerPerson = event.totalMinor / 100.0 / event.participantIds.size.coerceAtLeast(1),
                            participants = others.map { id -> ParticipantBalance(repository.displayName(id), people[id]?.initial ?: "?",
                                amountFormatted = money(balances[id] ?: 0, event.currency), id = id) },
                            expenses = event.expenses.sortedWith(compareByDescending<StoredExpense> { it.createdAt }.thenByDescending { it.id }).map { e -> ExpenseItem(e.id, e.title,
                                repository.displayName(e.payerId), PagameRepository.dateLabel(e.createdAt), e.amountMinor / 100.0,
                                e.includedIds.size, includedParticipantNames = e.includedIds.map(repository::displayName),
                                payerId = e.payerId, includedIds = e.includedIds, createdBy = e.createdBy,
                                canEdit = uid == event.ownerId || uid == e.createdBy,
                                createdByName = repository.displayName(e.createdBy)) },
                            participantCandidates = (friends + nonOwnerParticipantIds.mapNotNull { people[it] }).distinctBy { it.id }.filter { it.id != event.ownerId },
                            originalParticipantIds = if (state.isParticipantsSheetOpen) state.originalParticipantIds else nonOwnerParticipantIds,
                            draftParticipantIds = if (state.isParticipantsSheetOpen) state.draftParticipantIds else nonOwnerParticipantIds,
                            participantExpenseCounts = event.expenseCounts())
                    }
                }
            }.collect()
        }
    }
    fun dismissError() { _uiState.update { it.copy(errorMessage = null) } }
    fun selectTab(tab: EventDetailTab) { _uiState.update { it.copy(selectedTab = tab) } }
    fun openParticipantsSheet() {
        if (!_uiState.value.isOrganizer) { _uiState.update { it.copy(errorMessage = "Solo el organizador puede editar los participantes") }; return }
        _uiState.update { it.copy(isParticipantsSheetOpen = true, participantSearchQuery = "", draftParticipantIds = it.originalParticipantIds) }
    }
    fun closeParticipantsSheet() {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(isParticipantsSheetOpen = false, isParticipantChangesConfirmationOpen = false, participantRemovalBlockedMessage = null) }
    }
    fun updateParticipantSearch(query: String) { _uiState.update { it.copy(participantSearchQuery = query) } }
    fun toggleEventParticipant(friend: FriendItem) {
        if (_uiState.value.busy) return
        _uiState.update { s ->
            val count = s.participantExpenseCounts[friend.id] ?: 0
            if (friend.id in s.draftParticipantIds && count > 0) s.copy(participantRemovalBlockedMessage =
                "No puedes quitar a ${friend.name} porque tiene $count gastos asociados. Edita o borra esos gastos primero.")
            else s.copy(draftParticipantIds = if (friend.id in s.draftParticipantIds) s.draftParticipantIds - friend.id else s.draftParticipantIds + friend.id)
        }
    }
    fun dismissParticipantRemovalBlockedMessage() { _uiState.update { it.copy(participantRemovalBlockedMessage = null) } }
    fun showOrganizerRemovalBlockedMessage() { _uiState.update { it.copy(participantRemovalBlockedMessage = "El organizador debe permanecer en el evento.") } }
    fun requestSaveParticipantChanges() {
        _uiState.update { if (it.hasParticipantChanges) it.copy(isParticipantChangesConfirmationOpen = true) else it.copy(isParticipantsSheetOpen = false) }
    }
    fun closeParticipantChangesConfirmation() { _uiState.update { it.copy(isParticipantChangesConfirmationOpen = false) } }
    fun confirmParticipantChanges() = action {
        val s = _uiState.value
        command("updateParticipants", mapOf("participantIds" to (s.draftParticipantIds + s.event!!.ownerId).toList()))
        _uiState.update { it.copy(isParticipantsSheetOpen = false, isParticipantChangesConfirmationOpen = false) }
    }
    fun openAddExpenseDialog() {
        val state = _uiState.value
        if (state.loading || state.busy) return
        val draft = draftStore.read(state.currentUid, eventId)
        if (draft != null && state.expenses.none { it.id == draft.id }) {
            _uiState.update { it.copy(showRestoreDraftDialog = true) }
        } else startNewExpense()
    }
    fun dismissRestoreDraft() { _uiState.update { it.copy(showRestoreDraftDialog = false) } }
    fun startNewExpense() {
        val state = _uiState.value
        if (state.busy || state.loading) return
        draftStore.clear(state.currentUid, eventId)
        expenseDraftId = UUID.randomUUID().toString()
        _uiState.update { it.copy(showRestoreDraftDialog = false, isExpenseSheetOpen = true,
            editingExpenseId = null, newExpenseTitle = "", amountInputError = null,
            newExpenseAmount = "", expensePayerName = "Tú", newExpenseIncludedNames = it.allParticipantNames) }
    }
    fun restoreExpenseDraft() {
        val state = _uiState.value
        val event = state.event ?: return
        val draft = draftStore.read(state.currentUid, eventId)
        if (draft == null || event.expenses.any { it.id == draft.id }) { startNewExpense(); return }
        expenseDraftId = draft.id
        val payer = draft.payerId.takeIf { it in event.participantIds } ?: state.currentUid
        val included = draft.includedIds.filter { it in event.participantIds }
        _uiState.update { it.copy(showRestoreDraftDialog = false, isExpenseSheetOpen = true,
            editingExpenseId = null, newExpenseTitle = draft.title, newExpenseAmount = draft.amount,
            amountInputError = null,
            expensePayerName = repository.displayName(payer), newExpenseIncludedNames = included.map(repository::displayName)) }
    }
    private fun rememberExpenseDraft() {
        val state = _uiState.value
        val event = state.event ?: return
        if (!state.isExpenseSheetOpen || state.editingExpenseId != null || state.currentUid.isBlank()) return
        val byName = event.participantIds.associateBy(repository::displayName)
        val payer = byName[state.expensePayerName] ?: state.currentUid
        val included = state.newExpenseIncludedNames.mapNotNull { byName[it] }
        val meaningful = state.newExpenseTitle.isNotBlank() || state.newExpenseAmount.isNotBlank()
            || payer != state.currentUid || included.toSet() != event.participantIds.toSet()
        if (!meaningful) { draftStore.clear(state.currentUid, eventId); return }
        draftStore.save(state.currentUid, eventId, ExpenseDraft(expenseDraftId, state.newExpenseTitle,
            state.newExpenseAmount, payer, included,
            System.currentTimeMillis() + ExpenseDraftStore.LIFETIME_MS))
    }
    fun openEditExpenseDialog(expense: ExpenseItem) {
        if (!expense.canEdit) { _uiState.update { it.copy(errorMessage = "Solo el autor o el organizador puede editar este gasto") }; return }
        _uiState.update { it.copy(isExpenseSheetOpen = true, editingExpenseId = expense.id, amountInputError = null,
            newExpenseTitle = expense.title, newExpenseAmount = BigDecimal.valueOf(expense.amount).toPlainString(),
            expensePayerName = expense.paidByName, newExpenseIncludedNames = expense.includedParticipantNames) }
    }
    fun closeAddExpenseDialog() {
        if (_uiState.value.busy) return
        rememberExpenseDraft()
        _uiState.update { it.copy(isExpenseSheetOpen = false, editingExpenseId = null) }
    }
    fun updateNewTitle(value: String) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(newExpenseTitle = value.takeText(100)) }; rememberExpenseDraft()
    }
    fun updateNewAmount(value: String) {
        if (_uiState.value.busy) return
        val normalized = normalizedAmountInput(value)
        _uiState.update { if (normalized == null) it.copy(amountInputError = "Solo números, hasta 2 decimales. Máximo: 1,000,000.00")
            else it.copy(newExpenseAmount = normalized, amountInputError = null) }
        if (normalized != null) rememberExpenseDraft()
    }
    fun updateNewPayer(value: String) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(expensePayerName = value) }; rememberExpenseDraft()
    }
    fun toggleIncludedParticipant(name: String) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(newExpenseIncludedNames = if (name in it.newExpenseIncludedNames) it.newExpenseIncludedNames - name else it.newExpenseIncludedNames + name) }
        rememberExpenseDraft()
    }
    fun toggleAllIncluded() {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(newExpenseIncludedNames = if (it.newExpenseIncludedNames.size == it.allParticipantNames.size) emptyList() else it.allParticipantNames) }
        rememberExpenseDraft()
    }
    fun saveExpense() = action {
        val s = _uiState.value
        val amount = amountMinor(s.newExpenseAmount) ?: run { _uiState.update { it.copy(errorMessage = "Ingresa un monto entre 0.01 y 1,000,000.00, con hasta dos decimales") }; return@action }
        val event = s.event ?: return@action
        val names = event.participantIds.associateBy(repository::displayName)
        val payer = names[s.expensePayerName] ?: error("Pagador no disponible")
        val included = s.newExpenseIncludedNames.mapNotNull { names[it] }
        if (included.size != s.newExpenseIncludedNames.size || included.isEmpty()) error("Participantes no disponibles")
        command(if (s.editingExpenseId == null) "addExpense" else "updateExpense", mapOf(
            "expenseId" to (s.editingExpenseId ?: expenseDraftId), "title" to s.newExpenseTitle,
            "amountMinor" to amount, "payerId" to payer, "includedIds" to included))
        if (s.editingExpenseId == null) draftStore.clear(s.currentUid, eventId)
        _uiState.update { it.copy(isExpenseSheetOpen = false, editingExpenseId = null) }
    }
    fun deleteExpense() = action {
        val id = _uiState.value.editingExpenseId ?: return@action
        command("deleteExpense", mapOf("expenseId" to id))
        _uiState.update { it.copy(isExpenseSheetOpen = false, editingExpenseId = null) }
    }
    fun requestDeleteEvent() {
        val s = _uiState.value
        _uiState.update {
            when {
                !s.isOrganizer -> it.copy(deleteEventBlockedMessage = "No puedes borrar este evento porque no eres su creador.")
                s.expenses.isNotEmpty() || s.event?.payments?.isNotEmpty() == true -> it.copy(deleteEventBlockedMessage = "No puedes borrar un evento con gastos o pagos registrados.")
                else -> it.copy(isDeleteEventConfirmationOpen = true)
            }
        }
    }
    fun dismissDeleteEventBlockedMessage() { _uiState.update { it.copy(deleteEventBlockedMessage = null) } }
    fun dismissDeleteEventConfirmation() { _uiState.update { it.copy(isDeleteEventConfirmationOpen = false) } }
    fun confirmDeleteEvent(onDeleted: () -> Unit) = action {
        command("deleteEvent"); draftStore.clear(_uiState.value.currentUid, eventId); _uiState.update { it.copy(isDeleteEventConfirmationOpen = false) }; onDeleted()
    }
    fun reportPayment(value: Settlement) = action {
        command("reportPayment", mapOf("paymentId" to UUID.randomUUID().toString(), "fromId" to value.fromId,
            "toId" to value.toId, "amountMinor" to value.amountMinor, "revision" to (_uiState.value.event?.revision ?: 0)))
    }
    fun confirmPayment(id: String) = action { command("confirmPayment", mapOf("paymentId" to id)) }
    fun rejectPayment(id: String) = action { command("rejectPayment", mapOf("paymentId" to id)) }
    private suspend fun command(name: String, data: Map<String, Any> = emptyMap()) { repository.command(name, data + ("eventId" to eventId)) }
    private fun action(block: suspend () -> Unit) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true, errorMessage = null) }
        viewModelScope.launch {
            try { block() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(errorMessage = userMessage(e)) } }
            finally { _uiState.update { it.copy(busy = false) } }
        }
    }
}
