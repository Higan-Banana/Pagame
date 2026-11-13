package gt.pagame.app.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gt.pagame.app.data.repository.PagameRepository
import gt.pagame.app.data.repository.userMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class RefreshUiState(val refreshing: Boolean = false, val error: String? = null)

class RefreshViewModel : ViewModel() {
    private val repository = PagameRepository.instance
    private val _uiState = MutableStateFlow(RefreshUiState())
    val uiState = _uiState.asStateFlow()

    fun dismissError() { _uiState.update { it.copy(error = null) } }

    fun refresh(connected: Boolean) {
        if (_uiState.value.refreshing) return
        _uiState.value = RefreshUiState(refreshing = true)
        viewModelScope.launch {
            try {
                if (connected) repository.refresh()
                else _uiState.update { it.copy(error = "Sin conexión. Conéctate a internet para actualizar.") }
            } catch (e: TimeoutCancellationException) {
                _uiState.update { it.copy(error = "La actualización tardó demasiado. Revisa tu conexión e intenta de nuevo.") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(error = userMessage(e)) } }
            finally { _uiState.update { it.copy(refreshing = false) } }
        }
    }
}
