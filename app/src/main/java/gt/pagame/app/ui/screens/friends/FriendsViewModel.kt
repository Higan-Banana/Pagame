package gt.pagame.app.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gt.pagame.app.data.model.FriendItem
import gt.pagame.app.data.model.FriendCode
import gt.pagame.app.data.model.FriendRequest
import gt.pagame.app.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class FriendsUiState(
    val friendToRemove: FriendItem? = null, val requestsOpen: Boolean = false, val requests: List<FriendRequest> = emptyList(), val myId: String = "",
    val myCode: String = "", val friends: List<FriendItem> = emptyList(),
    val isAddFriendDialogOpen: Boolean = false, val newFriendCode: String = "",
    val foundFriend: FriendItem? = null, val lookupLoading: Boolean = false,
    val lookupError: String? = null, val busy: Boolean = false,
    val errorMessage: String? = null, val noticeMessage: String? = null
) {
    val foundFriendName: String get() = foundFriend?.name.orEmpty()
    val receivedCount: Int get() = requests.count { it.status == "pending" && it.recipientId == myId }
}
class FriendsViewModel(private val repository: PagameRepository = PagameRepository.instance) : ViewModel() {
    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState = _uiState.asStateFlow()
    private var lookupJob: Job? = null
    private var noticeJob: Job? = null
    init {
        viewModelScope.launch { repository.friendRequests.collect { requests -> _uiState.update { it.copy(requests = requests) } } }
        viewModelScope.launch { repository.friends.collect { friends -> _uiState.update { it.copy(friends = friends) } } }
        viewModelScope.launch { repository.currentUser.collect { user -> _uiState.update { it.copy(myCode = user.friendCode, myId = user.id) } } }
    }
    fun openRequests() { _uiState.update { it.copy(requestsOpen = true) } }
    fun closeRequests() { _uiState.update { it.copy(requestsOpen = false) } }
    fun requestExistingFriend(friend: FriendItem) {
        _uiState.update { it.copy(isAddFriendDialogOpen = true, foundFriend = friend,
            newFriendCode = friend.codeOrStatus, lookupLoading = false, lookupError = null) }
    }
    fun requestRemoveFriend(friend: FriendItem) { if (!_uiState.value.busy) _uiState.update { it.copy(friendToRemove = friend) } }
    fun cancelRemoveFriend() { if (!_uiState.value.busy) _uiState.update { it.copy(friendToRemove = null) } }
    fun confirmRemoveFriend() {
        val friend = _uiState.value.friendToRemove ?: return
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                withTimeout(15_000L) { repository.removeFriend(friend.id) }
                _uiState.update { it.copy(friendToRemove = null) }
                showNotice("Amistad eliminada. Sus eventos y pagos se conservaron.")
            } catch (e: TimeoutCancellationException) {
                _uiState.update { it.copy(errorMessage = "Revisa la lista antes de volver a intentar; no se pudo confirmar la respuesta.") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(errorMessage = userMessage(e)) } }
            finally { _uiState.update { it.copy(busy = false) } }
        }
    }
    fun dismissError() { _uiState.update { it.copy(errorMessage = null) } }
    fun copyCode() = showNotice("Código ${_uiState.value.myCode} copiado")
    fun openAddFriendDialog() { _uiState.update { it.copy(isAddFriendDialogOpen = true, lookupError = null) } }
    fun closeAddFriendDialog() {
        if (_uiState.value.busy) return
        lookupJob?.cancel()
        _uiState.update { it.copy(isAddFriendDialogOpen = false, foundFriend = null, newFriendCode = "", lookupLoading = false) }
    }
    fun updateNewCode(input: String) {
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
    fun addFriend() {
        val state = _uiState.value
        val friend = state.foundFriend ?: return
        if (state.busy) return
        _uiState.update { it.copy(busy = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val message = withTimeout(15_000L) { repository.addFriend(friend.id) }
                _uiState.update { it.copy(isAddFriendDialogOpen = false, newFriendCode = "", foundFriend = null) }
                showNotice(message)
            } catch (e: TimeoutCancellationException) { _uiState.update { it.copy(errorMessage = "No se pudo confirmar el envío. Revisa Solicitudes antes de volver a intentar.") } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(errorMessage = userMessage(e)) } }
            finally { _uiState.update { it.copy(busy = false) } }
        }
    }
    private fun showNotice(message: String) {
        noticeJob?.cancel()
        _uiState.update { it.copy(noticeMessage = message) }
        noticeJob = viewModelScope.launch { delay(5000); _uiState.update { it.copy(noticeMessage = null) } }
    }
}
