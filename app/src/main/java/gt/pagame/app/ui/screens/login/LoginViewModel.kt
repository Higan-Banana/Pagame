package gt.pagame.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gt.pagame.app.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class LoginUiState(
    val email: String = "", val password: String = "", val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false, val errorMessage: String? = null, val notice: String? = null
)
class LoginViewModel : ViewModel() {
    private val repository = PagameRepository.instance
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()
    fun onEmailChanged(value: String) { _uiState.update { it.copy(email = value, errorMessage = null) } }
    fun onPasswordChanged(value: String) { _uiState.update { it.copy(password = value, errorMessage = null) } }
    fun togglePasswordVisibility() { _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) } }
    fun login(onSuccess: () -> Unit) = execute {
        val s = _uiState.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa tu correo y contraseña") }; return@execute
        }
        repository.login(s.email, s.password); onSuccess()
    }
    fun resetPassword() = execute {
        val email = _uiState.value.email
        if (email.isBlank()) { _uiState.update { it.copy(errorMessage = "Escribe tu correo para recuperar la contraseña") }; return@execute }
        repository.resetPassword(email)
        _uiState.update { it.copy(notice = "Revisa tu correo para restablecer la contraseña") }
    }
    private fun execute(action: suspend () -> Unit) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null, notice = null) }
        viewModelScope.launch {
            try { action() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(errorMessage = userMessage(e)) } }
            finally { _uiState.update { it.copy(isLoading = false) } }
        }
    }
}
