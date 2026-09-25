package gt.pagame.app.ui.screens.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gt.pagame.app.data.model.AppThemeMode
import gt.pagame.app.data.model.Currency
import gt.pagame.app.data.model.UserProfile
import gt.pagame.app.data.repository.PagameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class OptionsUiState(
    val user: UserProfile = UserProfile(),
    val currency: Currency = Currency.GTQ,
    val theme: AppThemeMode = AppThemeMode.LIGHT,
    val notificationsEnabled: Boolean = true,
    val showLogoutDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val snackbarMessage: String? = null
)

class OptionsViewModel(
    private val repository: PagameRepository = PagameRepository.instance
) : ViewModel() {

    private var snackbarJob: Job? = null

    private val _uiState = MutableStateFlow(OptionsUiState())
    val uiState: StateFlow<OptionsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                user = repository.currentUser.value,
                currency = repository.currency.value,
                theme = repository.theme.value,
                notificationsEnabled = repository.notificationsEnabled.value
            )
        }
    }

    fun cycleCurrency() {
        repository.cycleCurrency()
        _uiState.update {
            it.copy(
                currency = repository.currency.value
            )
        }
        showSnackbar("Moneda cambiada a ${repository.currency.value.displayName}")
    }

    fun cycleTheme() {
        repository.cycleTheme()
        val themeName = when (repository.theme.value) {
            AppThemeMode.LIGHT -> "Claro"
            AppThemeMode.DARK -> "Oscuro"
            AppThemeMode.SYSTEM -> "Automático"
        }
        _uiState.update {
            it.copy(
                theme = repository.theme.value
            )
        }
        showSnackbar("Tema cambiado a $themeName")
    }

    fun toggleNotifications(enabled: Boolean) {
        repository.toggleNotifications(enabled)
        _uiState.update {
            it.copy(
                notificationsEnabled = enabled
            )
        }
        showSnackbar(if (enabled) "Notificaciones activadas" else "Notificaciones desactivadas")
    }

    fun copyFriendCode() {
        showSnackbar("Código ${_uiState.value.user.friendCode} copiado")
    }

    fun openLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = true) }
    }

    fun closeLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = false) }
    }

    fun openDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun closeDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun dismissSnackbar() {
        snackbarJob?.cancel()
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun showSnackbar(message: String) {
        snackbarJob?.cancel()
        _uiState.update { it.copy(snackbarMessage = message) }
        snackbarJob = viewModelScope.launch {
            delay(2200)
            _uiState.update { it.copy(snackbarMessage = null) }
        }
    }

    fun confirmLogout(onLogoutSuccess: () -> Unit) {
        _uiState.update { it.copy(showLogoutDialog = false) }
        onLogoutSuccess()
    }

    fun confirmDelete(onDeleteSuccess: () -> Unit) {
        _uiState.update { it.copy(showDeleteDialog = false) }
        onDeleteSuccess()
    }
}
