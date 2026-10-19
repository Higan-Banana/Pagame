package gt.pagame.app.ui.screens.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gt.pagame.app.data.model.*
import gt.pagame.app.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class OptionsUiState(
    val user: UserProfile = UserProfile(), val currency: Currency = Currency.GTQ,
    val theme: AppThemeMode = AppThemeMode.SYSTEM, val showLogoutDialog: Boolean = false,
    val showDeleteDialog: Boolean = false, val noticeMessage: String? = null,
    val busy: Boolean = false, val errorMessage: String? = null,
    val showNameDialog: Boolean = false, val editedName: String = ""
)
class OptionsViewModel(private val repository: PagameRepository = PagameRepository.instance) : ViewModel() {
    private val _uiState = MutableStateFlow(OptionsUiState())
    val uiState = _uiState.asStateFlow()
    private var noticeJob: Job? = null
    init {
        viewModelScope.launch { repository.currentUser.collect { user -> _uiState.update { it.copy(user = user) } } }
        viewModelScope.launch { repository.currency.collect { currency -> _uiState.update { it.copy(currency = currency) } } }
        viewModelScope.launch { repository.theme.collect { theme -> _uiState.update { it.copy(theme = theme) } } }
    }
    fun selectCurrency(value: Currency) = action { repository.updateProfile(mapOf("currency" to value.name)); notice("Moneda para eventos nuevos: ${value.displayName}") }
    fun selectColor(index: Int) = action { repository.updateProfile(mapOf("colorIndex" to index)); notice("Color de perfil actualizado") }
    fun selectTheme(theme: AppThemeMode) = action {
        repository.updateProfile(mapOf("theme" to theme.name))
    }
    fun openNameDialog() { _uiState.update { it.copy(showNameDialog = true, editedName = it.user.name) } }
    fun closeNameDialog() { if (!_uiState.value.busy) _uiState.update { it.copy(showNameDialog = false) } }
    fun editName(value: String) { _uiState.update { it.copy(editedName = value.takeText(60)) } }
    fun saveName() = action {
        repository.updateName(_uiState.value.editedName)
        _uiState.update { it.copy(showNameDialog = false) }
        notice("Nombre actualizado")
    }
    fun sendPasswordLink(onSent: () -> Unit) = action {
        repository.resetPassword(_uiState.value.user.email)
        onSent()
        notice("Enlace enviado. Revisa tu correo y la carpeta de spam.")
    }
    fun dismissError() { _uiState.update { it.copy(errorMessage = null) } }
    fun copyFriendCode() = notice("Código copiado")
    fun openLogoutDialog() { _uiState.update { it.copy(showLogoutDialog = true) } }
    fun closeLogoutDialog() { _uiState.update { it.copy(showLogoutDialog = false) } }
    fun openDeleteDialog() { _uiState.update { it.copy(showDeleteDialog = true) } }
    fun closeDeleteDialog() { if (!_uiState.value.busy) _uiState.update { it.copy(showDeleteDialog = false) } }
    fun confirmLogout(onLogoutSuccess: () -> Unit) { repository.signOut(); onLogoutSuccess() }
    fun confirmDelete(onDeleteSuccess: () -> Unit) = action { repository.deleteAccount(); onDeleteSuccess() }
    private fun notice(message: String) {
        noticeJob?.cancel(); _uiState.update { it.copy(noticeMessage = message) }
        noticeJob = viewModelScope.launch { delay(2200); _uiState.update { it.copy(noticeMessage = null) } }
    }
    private fun action(block: suspend () -> Unit) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true, errorMessage = null) }
        viewModelScope.launch {
            try { block() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(errorMessage = userMessage(e), showDeleteDialog = false) } }
            finally { _uiState.update { it.copy(busy = false) } }
        }
    }
}
