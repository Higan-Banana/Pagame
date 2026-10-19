package gt.pagame.app.ui.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gt.pagame.app.data.model.*
import gt.pagame.app.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

data class EventsUiState(
    val requestsOpen: Boolean = false, val receivedCount: Int = 0, val friendNotice: String? = null,
    val events: List<EventItem> = emptyList(), val currency: Currency = Currency.GTQ,
    val isCreateEventSheetOpen: Boolean = false, val newEventTitle: String = "",
    val allFriends: List<FriendItem> = emptyList(), val friendSearchQuery: String = "",
    val selectedFriendIds: List<String> = emptyList(), val isAddFriendDialogOpen: Boolean = false,
    val newFriendCode: String = "", val foundFriend: FriendItem? = null,
    val lookupLoading: Boolean = false, val lookupError: String? = null,
    val busy: Boolean = false, val errorMessage: String? = null, val listResetVersion: Long = 0L
) { val foundFriendName: String get() = foundFriend?.name.orEmpty() }
class EventsViewModel(private val repository: PagameRepository = PagameRepository.instance) : ViewModel() {
    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState = _uiState.asStateFlow()
    private var lookupJob: Job? = null
    private val waitingFriendIds = mutableSetOf<String>()
    private var creationId: String? = null
    init {
        viewModelScope.launch { repository.events.collect { events -> _uiState.update { it.copy(events = events) } } }
        viewModelScope.launch { repository.friends.collect { friends ->
            val accepted = friends.filter { it.id in waitingFriendIds }
            waitingFriendIds.removeAll(accepted.map { it.id }.toSet())
            _uiState.update { it.copy(allFriends = friends, selectedFriendIds = it.selectedFriendIds.filter { id -> friends.any { friend -> friend.id == id } },
                friendNotice = if (accepted.isNotEmpty()) "${accepted.joinToString { friend -> friend.name }} ya está en tus amigos. Puedes seleccionarlo en la lista." else it.friendNotice) }
        } }
        viewModelScope.launch {
            combine(repository.friendRequests, repository.sessionUid) { requests, uid ->
                requests.count { it.status == "pending" && it.recipientId == uid }
            }.collect { count -> _uiState.update { it.copy(receivedCount = count) } }
        }
        viewModelScope.launch { repository.currency.collect { currency -> _uiState.update { it.copy(currency = currency) } } }
    }
    fun openRequests() { _uiState.update { it.copy(requestsOpen = true) } }
    fun closeRequests() { _uiState.update { it.copy(requestsOpen = false) } }
    fun dismissError() { _uiState.update { it.copy(errorMessage = null) } }
    fun openCreateDialog() { _uiState.update { it.copy(isCreateEventSheetOpen = true) } }
    fun closeCreateDialog() {
        if (_uiState.value.busy) return
        lookupJob?.cancel(); creationId = null; waitingFriendIds.clear()
        _uiState.update { it.copy(isCreateEventSheetOpen = false, isAddFriendDialogOpen = false, requestsOpen = false, friendNotice = null,
            newEventTitle = "",
            friendSearchQuery = "", selectedFriendIds = emptyList(), newFriendCode = "", foundFriend = null) }
    }
    fun updateNewTitle(title: String) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(newEventTitle = title.takeText(100)) }
    }
    fun updateFriendSearch(query: String) { _uiState.update { it.copy(friendSearchQuery = query) } }
    fun toggleFriendSelection(id: String) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(selectedFriendIds = if (id in it.selectedFriendIds) it.selectedFriendIds - id else it.selectedFriendIds + id) }
    }
    fun openAddFriendDialog() { _uiState.update { it.copy(isAddFriendDialogOpen = true, newFriendCode = "", foundFriend = null, lookupError = null) } }
    fun closeAddFriendDialog() {
        if (_uiState.value.busy) return
        lookupJob?.cancel()
        _uiState.update { it.copy(isAddFriendDialogOpen = false, newFriendCode = "", foundFriend = null, lookupLoading = false) }
    }
    fun updateNewFriendCode(input: String) {
        if (_uiState.value.busy) return
        lookupJob?.cancel()
        val code = FriendCode.normalize(input)
        _uiState.update { it.copy(newFriendCode = code, foundFriend = null, lookupError = null, lookupLoading = code.length == 7) }
        if (code.length != 7) return
        lookupJob = viewModelScope.launch {
            try {
                delay(300)
                val friend = repository.lookupFriend(code)
                _uiState.update { it.copy(foundFriend = friend, lookupLoading = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(lookupLoading = false, lookupError = userMessage(e)) } }
        }
    }
    fun addFriendFromEvent() = runAction {
        val friend = _uiState.value.foundFriend ?: return@runAction
        val message = withTimeout(15_000L) { repository.addFriend(friend.id) }
        if (_uiState.value.allFriends.none { it.id == friend.id }) waitingFriendIds.add(friend.id)
        _uiState.update { it.copy(friendNotice = message,
            isAddFriendDialogOpen = false, newFriendCode = "", foundFriend = null, friendSearchQuery = "") }
    }
    fun createEvent() {
        val state = _uiState.value
        if (state.newEventTitle.isBlank()) return
        val id = creationId ?: UUID.randomUUID().toString().also { creationId = it }
        runAction {
            repository.createEvent(id, state.newEventTitle, state.selectedFriendIds)
            val events = withTimeout(20000) { repository.events.first { list -> list.any { it.id == id } } }
            creationId = null
            waitingFriendIds.clear()
            _uiState.update { it.copy(events = events, listResetVersion = it.listResetVersion + 1,
                isCreateEventSheetOpen = false, isAddFriendDialogOpen = false, requestsOpen = false, friendNotice = null, newEventTitle = "",
                friendSearchQuery = "", selectedFriendIds = emptyList(), newFriendCode = "", foundFriend = null) }
        }
    }
    private fun runAction(block: suspend () -> Unit) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true, errorMessage = null) }
        viewModelScope.launch {
            try { block() }
            catch (e: TimeoutCancellationException) { _uiState.update { it.copy(errorMessage = "No se pudo confirmar la operación a tiempo. Revisa si aparece en la lista o en Solicitudes antes de volver a intentar.") } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(errorMessage = userMessage(e)) } }
            finally { _uiState.update { it.copy(busy = false) } }
        }
    }
}
