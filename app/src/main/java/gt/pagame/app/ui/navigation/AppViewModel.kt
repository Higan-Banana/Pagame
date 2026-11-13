package gt.pagame.app.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gt.pagame.app.data.model.FriendItem
import gt.pagame.app.data.model.UserProfile
import gt.pagame.app.data.model.takeText
import gt.pagame.app.data.repository.PagameRepository
import gt.pagame.app.data.repository.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppUiState(
    val uid: String? = null,
    val people: Map<String, FriendItem> = emptyMap(),
    val user: UserProfile = UserProfile(),
    val profileChecked: Boolean = false,
    val registering: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val profileName: String = "",
    val savingProfile: Boolean = false,
    val profileError: String? = null
)

/** Session and profile operations shared by the activity and its navigation graph. */
class AppViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val repository = PagameRepository.instance
    val theme = repository.theme
    private val _uiState = MutableStateFlow(AppUiState(
        uid = repository.sessionUid.value,
        profileName = savedStateHandle["profileName"] ?: ""
    ))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = combine(
                repository.sessionUid, repository.people, repository.currentUser,
                repository.profileChecked, repository.registering
            ) { uid, people, user, checked, registering ->
                AppUiState(uid = uid, people = people, user = user,
                    profileChecked = checked, registering = registering)
            }
            combine(session, repository.loading, repository.error) { data, loading, error ->
                data.copy(loading = loading, error = error)
            }.collect { data ->
                if (_uiState.value.uid != data.uid) savedStateHandle["profileName"] = ""
                _uiState.update { previous ->
                    if (previous.uid != data.uid) data
                    else data.copy(profileName = previous.profileName,
                        savingProfile = previous.savingProfile, profileError = previous.profileError)
                }
            }
        }
    }

    fun updateProfileName(value: String) {
        val name = value.takeText(60)
        savedStateHandle["profileName"] = name
        _uiState.update { it.copy(profileName = name, profileError = null) }
    }

    fun saveProfile() {
        val state = _uiState.value
        if (state.savingProfile || state.profileName.isBlank()) return
        _uiState.update { it.copy(savingProfile = true, profileError = null) }
        viewModelScope.launch {
            try {
                repository.initProfile(state.profileName)
                repository.retry()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_uiState.value.uid == state.uid) _uiState.update { it.copy(profileError = userMessage(e)) }
            } finally {
                if (_uiState.value.uid == state.uid) _uiState.update { it.copy(savingProfile = false) }
            }
        }
    }

    fun retry() = repository.retry()
    fun signOut() = repository.signOut()
}
