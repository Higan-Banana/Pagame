package gt.pagame.app.ui.screens.register

import gt.pagame.app.data.model.ActionRestrictions
import gt.pagame.app.data.model.takeText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gt.pagame.app.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class RegisterUiState(
    val fullName: String = "", val email: String = "", val password: String = "",
    val confirmPassword: String = "", val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false, val passwordsMatch: Boolean = true,
    val isLoading: Boolean = false, val errorMessage: String? = null
)
class RegisterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()
    fun onFullNameChanged(value: String) { _uiState.update { it.copy(fullName = value.takeText(60), errorMessage = null) } }
    fun onEmailChanged(value: String) { _uiState.update { it.copy(email = value, errorMessage = null) } }
    fun onPasswordChanged(value: String) { _uiState.update { it.copy(password = value, passwordsMatch = value == it.confirmPassword, errorMessage = null) } }
    fun onConfirmPasswordChanged(value: String) { _uiState.update { it.copy(confirmPassword = value, passwordsMatch = value == it.password, errorMessage = null) } }
    fun togglePasswordVisibility() { _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) } }
    fun toggleConfirmPasswordVisibility() { _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) } }
    fun register(onSuccess: () -> Unit) {
        val s = _uiState.value
        if (s.isLoading) return
        val blocked = ActionRestrictions.registration(s.fullName, s.email, s.password, s.confirmPassword)
        if (blocked != null) {
            _uiState.update { it.copy(errorMessage = blocked) }; return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try { PagameRepository.instance.register(s.fullName, s.email, s.password); onSuccess() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(errorMessage = userMessage(e)) } }
            finally { _uiState.update { it.copy(isLoading = false) } }
        }
    }
}
