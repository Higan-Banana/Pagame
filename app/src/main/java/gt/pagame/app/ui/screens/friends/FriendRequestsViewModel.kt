package gt.pagame.app.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gt.pagame.app.data.model.FriendItem
import gt.pagame.app.data.model.FriendRequest
import gt.pagame.app.data.repository.PagameRepository
import gt.pagame.app.data.repository.userMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class FriendRequestsUiState(
    val uid: String = "",
    val received: List<FriendRequest> = emptyList(),
    val sent: List<FriendRequest> = emptyList(),
    val people: Map<String, FriendItem> = emptyMap(),
    val loading: Boolean = true,
    val busyId: String? = null,
    val error: String? = null,
    val notice: String? = null
)

class FriendRequestsViewModel : ViewModel() {
    private val repository = PagameRepository.instance
    private val _uiState = MutableStateFlow(FriendRequestsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.friendRequests, repository.people, repository.sessionUid, repository.loading) { requests, people, uid, loading ->
                FriendRequestsUiState(
                    uid = uid.orEmpty(), people = people, loading = loading,
                    received = requests.filter { it.status == "pending" && it.recipientId == uid },
                    sent = requests.filter { it.status == "pending" && it.senderId == uid }
                )
            }.collect { data ->
                _uiState.update { previous ->
                    if (previous.uid != data.uid) data
                    else data.copy(busyId = previous.busyId, error = previous.error, notice = previous.notice)
                }
            }
        }
    }

    fun onOpened() { _uiState.update { it.copy(error = null, notice = null) } }
    fun dismissError() { _uiState.update { it.copy(error = null) } }

    fun respond(id: String, accept: Boolean) {
        val state = _uiState.value
        if (state.busyId != null) return
        val incoming = state.received.any { it.id == id }
        if (!incoming && (accept || state.sent.none { it.id == id })) return
        _uiState.update { it.copy(busyId = id, notice = null, error = null) }
        viewModelScope.launch {
            try {
                withTimeout(15_000L) {
                    if (accept) repository.acceptFriendRequest(id) else repository.dismissFriendRequest(id)
                }
                if (_uiState.value.uid == state.uid) _uiState.update {
                    it.copy(notice = if (accept) "Solicitud aceptada. Ahora aparecen en la lista de amigos de ambos."
                        else if (incoming) "Solicitud rechazada." else "Solicitud cancelada.")
                }
            } catch (e: TimeoutCancellationException) {
                if (_uiState.value.uid == state.uid) _uiState.update { it.copy(error = "No se pudo confirmar la respuesta. Revisa el estado de la solicitud antes de volver a intentar.") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (_uiState.value.uid == state.uid) _uiState.update { it.copy(error = userMessage(e)) }
            } finally {
                if (_uiState.value.uid == state.uid) _uiState.update { it.copy(busyId = null) }
            }
        }
    }
}
