package gt.pagame.app.ui.screens.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val emailOrPhone: String = "mateo@universidad.edu.gt",
    val password: String = "mateo12345",
    val isPasswordVisible: Boolean = false,
    val rememberAccount: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(emailOrPhone = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleRememberAccount(checked: Boolean) {
        _uiState.update { it.copy(rememberAccount = checked) }
    }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.emailOrPhone.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor ingresa tu correo y contraseña") }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        // Emulate realistic auth validation
        _uiState.update { it.copy(isLoading = false) }
        onSuccess()
    }
}
