package com.example.projectmdp.ui.module.TransactionHistory

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmdp.data.repository.TransactionRepository
import com.example.projectmdp.data.source.dataclass.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionHistoryUiState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TransactionHistoryUiState())
    val uiState: StateFlow<TransactionHistoryUiState> = _uiState.asStateFlow()

    init {
        // Load transactions when ViewModel is created
        loadTransactions()
    }

    fun loadTransactions(forceRefresh: Boolean = false) {
        Log.d("TransactionHistoryVM", "Loading transactions, forceRefresh: $forceRefresh")
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                transactionRepository.getMyTransactions(forceRefresh).collect { result ->
                    result.onSuccess { transactions ->
                        Log.d("TransactionHistoryVM", "Loaded ${transactions.size} transactions")
                        _uiState.value = _uiState.value.copy(
                            transactions = transactions,
                            isLoading = false,
                            errorMessage = null
                        )
                    }.onFailure { error ->
                        Log.e("TransactionHistoryVM", "Error loading transactions", error)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load transactions"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("TransactionHistoryVM", "Exception loading transactions", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load transactions: ${e.message}"
                )
            }
        }
    }

    fun refreshTransactions() {
        loadTransactions(forceRefresh = true)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
