package gt.pagame.app.ui.screens.events

import androidx.lifecycle.ViewModel
import gt.pagame.app.data.model.Currency
import gt.pagame.app.data.model.EventItem
import gt.pagame.app.data.model.FriendItem
import gt.pagame.app.data.repository.PagameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EventsUiState(
    val events: List<EventItem> = emptyList(),
    val currency: Currency = Currency.GTQ,
    val isShowingEmptyState: Boolean = false,
    val isCreateEventDialogOpen: Boolean = false,
    val newEventTitle: String = "",
    val allFriends: List<FriendItem> = emptyList(),
    val friendSearchQuery: String = "",
    val selectedFriendIds: List<String> = emptyList(),
    val isAddFriendDialogOpen: Boolean = false,
    val newFriendCode: String = "",
    val newFriendName: String = ""
)

class EventsViewModel(
    private val repository: PagameRepository = PagameRepository.instance
) : ViewModel() {

    private val availableFriendsByCode = mapOf(
        "PG-5312" to "Mario Gómez",
        "PG-1024" to "Carlos Mendoza",
        "PG-2940" to "Diana Rivas",
        "PG-8821" to "Vale Castillo"
    )

    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                events = repository.events.value,
                currency = repository.currency.value,
                allFriends = repository.friends.value
            )
        }
    }

    fun toggleEmptyState() {
        _uiState.update {
            val nextState = !it.isShowingEmptyState
            if (nextState) {
                it.copy(isShowingEmptyState = true, events = emptyList())
            } else {
                it.copy(isShowingEmptyState = false, events = repository.events.value)
            }
        }
    }

    fun openCreateDialog() {
        _uiState.update { it.copy(isCreateEventDialogOpen = true, allFriends = repository.friends.value) }
    }

    fun closeCreateDialog() {
        _uiState.update {
            it.copy(
                isCreateEventDialogOpen = false,
                isAddFriendDialogOpen = false,
                newEventTitle = "",
                friendSearchQuery = "",
                selectedFriendIds = emptyList(),
                newFriendCode = "",
                newFriendName = ""
            )
        }
    }

    fun updateNewTitle(title: String) {
        _uiState.update { it.copy(newEventTitle = title) }
    }

    fun updateFriendSearch(query: String) {
        _uiState.update { it.copy(friendSearchQuery = query) }
    }

    fun openAddFriendDialog() {
        _uiState.update {
            it.copy(
                isAddFriendDialogOpen = true,
                newFriendCode = "",
                newFriendName = ""
            )
        }
    }

    fun closeAddFriendDialog() {
        _uiState.update {
            it.copy(
                isAddFriendDialogOpen = false,
                newFriendCode = "",
                newFriendName = ""
            )
        }
    }

    fun updateNewFriendCode(input: String) {
        val numbers = input.filter { it.isDigit() }.take(4)
        val completeCode = if (numbers.isEmpty()) "" else "PG-$numbers"

        _uiState.update {
            it.copy(
                newFriendCode = completeCode,
                newFriendName = availableFriendsByCode[completeCode].orEmpty()
            )
        }
    }

    fun addFriendFromEvent() {
        val state = _uiState.value
        if (state.newFriendName.isBlank()) return

        val existingFriend = repository.friends.value.firstOrNull {
            it.codeOrStatus.equals(state.newFriendCode, ignoreCase = true)
        }

        if (existingFriend == null) {
            repository.addFriend(state.newFriendName, state.newFriendCode)
        }

        val updatedFriends = repository.friends.value
        val addedFriend = existingFriend ?: updatedFriends.lastOrNull {
            it.codeOrStatus.equals(state.newFriendCode, ignoreCase = true)
        }

        _uiState.update {
            it.copy(
                allFriends = updatedFriends,
                selectedFriendIds = addedFriend?.id
                    ?.let { friendId -> (it.selectedFriendIds + friendId).distinct() }
                    ?: it.selectedFriendIds,
                isAddFriendDialogOpen = false,
                newFriendCode = "",
                newFriendName = "",
                friendSearchQuery = ""
            )
        }
    }

    fun toggleFriendSelection(friendId: String) {
        _uiState.update {
            val current = it.selectedFriendIds
            val updated = if (friendId in current) current - friendId else current + friendId
            it.copy(selectedFriendIds = updated)
        }
    }

    fun createEvent() {
        val state = _uiState.value
        if (state.newEventTitle.isNotBlank() && state.selectedFriendIds.isNotEmpty()) {
            repository.addEvent(state.newEventTitle, state.selectedFriendIds)
            _uiState.update {
                it.copy(
                    events = repository.events.value,
                    isShowingEmptyState = false,
                    isCreateEventDialogOpen = false,
                    isAddFriendDialogOpen = false,
                    newEventTitle = "",
                    friendSearchQuery = "",
                    selectedFriendIds = emptyList(),
                    newFriendCode = "",
                    newFriendName = ""
                )
            }
        }
    }
}
