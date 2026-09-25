package gt.pagame.app.ui.screens.register

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RegisterUiState(
    val fullName: String = "Mateo González",
    val emailOrPhone: String = "mateo@universidad.edu.gt",
    val password: String = "segura123",
    val confirmPassword: String = "segura123",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val acceptedTerms: Boolean = true,
    val passwordsMatch: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class RegisterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChanged(value: String) {
        _uiState.update { it.copy(fullName = value, errorMessage = null) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(emailOrPhone = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update {
            val matches = value.isNotEmpty() && value == it.confirmPassword
            it.copy(password = value, passwordsMatch = matches, errorMessage = null)
        }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update {
            val matches = value.isNotEmpty() && value == it.password
            it.copy(confirmPassword = value, passwordsMatch = matches, errorMessage = null)
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun toggleTerms(accepted: Boolean) {
        _uiState.update { it.copy(acceptedTerms = accepted) }
    }

    fun register(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.fullName.isBlank() || state.emailOrPhone.isBlank() || state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Por favor completa todos los campos (contraseña mín. 6 caracteres)") }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Las contraseñas no coinciden") }
            return
        }
        if (!state.acceptedTerms) {
            _uiState.update { it.copy(errorMessage = "Debes aceptar los términos y políticas") }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        onSuccess()
    }
}
