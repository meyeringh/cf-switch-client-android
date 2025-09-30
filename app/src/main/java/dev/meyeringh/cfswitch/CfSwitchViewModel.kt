package dev.meyeringh.cfswitch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.meyeringh.cfswitch.data.CfSwitchRepository
import dev.meyeringh.cfswitch.data.NetworkError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Loading : UiState
    data class Loaded(val enabled: Boolean) : UiState
    data class Error(val message: String) : UiState
}

class CfSwitchViewModel(private val repository: CfSwitchRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun loadState() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getState().fold(
                onSuccess = { enabled ->
                    _uiState.value = UiState.Loaded(enabled)
                },
                onFailure = { error ->
                    handleError(error)
                }
            )
        }
    }

    fun toggleState() {
        val currentState = _uiState.value
        if (currentState !is UiState.Loaded) return

        val newState = !currentState.enabled

        // Optimistic update
        _uiState.value = UiState.Loaded(newState)

        viewModelScope.launch {
            repository.toggle(newState).fold(
                onSuccess = {
                    // Success, state already updated optimistically
                },
                onFailure = { error ->
                    // Revert on failure
                    _uiState.value = UiState.Loaded(currentState.enabled)
                    handleError(error)
                }
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refresh().fold(
                onSuccess = { enabled ->
                    _uiState.value = UiState.Loaded(enabled)
                },
                onFailure = { error ->
                    handleError(error)
                }
            )
            _isRefreshing.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun handleError(error: Throwable) {
        val message = when (error) {
            is NetworkError.Unauthorized -> "Invalid token"
            is NetworkError.NetworkFailure -> "Network error"
            is NetworkError.Unknown -> error.message ?: "Network error"
            else -> "Network error"
        }
        _errorMessage.value = message

        // Keep showing last known state if available
        if (_uiState.value !is UiState.Loaded) {
            _uiState.value = UiState.Error(message)
        }
    }
}
