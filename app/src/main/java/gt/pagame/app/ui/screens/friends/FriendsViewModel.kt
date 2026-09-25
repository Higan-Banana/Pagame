package gt.pagame.app.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gt.pagame.app.data.model.FriendItem
import gt.pagame.app.data.repository.PagameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class FriendsUiState(
    val myCode: String = "PG-7489",
    val friends: List<FriendItem> = emptyList(),
    val isShowingEmptyState: Boolean = false,
    val isAddFriendDialogOpen: Boolean = false,
    val newFriendName: String = "",
    val newFriendCode: String = "",
    val noticeMessage: String? = null
)

class FriendsViewModel(
    private val repository: PagameRepository = PagameRepository.instance
) : ViewModel() {

    private val availableFriendsByCode = mapOf(
        "PG-5312" to "Mario Gómez",
        "PG-1024" to "Carlos Mendoza",
        "PG-2940" to "Diana Rivas",
        "PG-8821" to "Vale Castillo"
    )

    private var noticeJob: Job? = null

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                myCode = repository.currentUser.value.friendCode,
                friends = repository.friends.value
            )
        }
    }

    fun toggleEmptyState() {
        _uiState.update {
            val nextState = !it.isShowingEmptyState
            if (nextState) {
                it.copy(isShowingEmptyState = true, friends = emptyList())
            } else {
                it.copy(isShowingEmptyState = false, friends = repository.friends.value)
            }
        }
    }

    fun copyCode() {
        showNotice("Código ${_uiState.value.myCode} copiado")
    }

    fun dismissNotice() {
        noticeJob?.cancel()
        _uiState.update {
            it.copy(noticeMessage = null)
        }
    }

    fun openAddFriendDialog() {
        _uiState.update { it.copy(isAddFriendDialogOpen = true) }
    }

    fun closeAddFriendDialog() {
        _uiState.update { it.copy(isAddFriendDialogOpen = false, newFriendName = "", newFriendCode = "") }
    }

    fun updateNewName(name: String) {
        _uiState.update { it.copy(newFriendName = name) }
    }

    fun updateNewCode(input: String) {
        val numbers = input.filter { it.isDigit() }.take(4)
        val normalizedCode = if (numbers.isEmpty()) "" else "PG-$numbers"

        _uiState.update {
            it.copy(
                newFriendCode = normalizedCode,
                newFriendName = availableFriendsByCode[normalizedCode].orEmpty()
            )
        }
    }

    fun addFriend() {
        val state = _uiState.value

        if (state.newFriendName.isNotBlank()) {
            repository.addFriend(
                state.newFriendName,
                state.newFriendCode
            )

            _uiState.update {
                it.copy(
                    friends = repository.friends.value,
                    isShowingEmptyState = false,
                    isAddFriendDialogOpen = false,
                    newFriendName = "",
                    newFriendCode = ""
                )
            }
            showNotice("Tu amigo ha sido agregado")
        }
    }

    private fun showNotice(message: String) {
        noticeJob?.cancel()
        _uiState.update { it.copy(noticeMessage = message) }
        noticeJob = viewModelScope.launch {
            delay(2200)
            _uiState.update { it.copy(noticeMessage = null) }
        }
    }
}
