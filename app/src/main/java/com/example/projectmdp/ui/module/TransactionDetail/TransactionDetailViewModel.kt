package com.example.projectmdp.ui.module.TransactionDetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmdp.data.repository.TransactionRepository
import com.example.projectmdp.data.repository.UserRepository
import com.example.projectmdp.data.source.dataclass.Transaction
import com.example.projectmdp.data.source.dataclass.User
import com.example.projectmdp.data.source.dataclass.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionDetailUiState(
    val transaction: Transaction? = null,
    val seller: User? = null,
    val product: Product? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    fun loadTransactionDetail(transactionId: String) {
        Log.d("TransactionDetailVM", "Loading transaction detail for ID: $transactionId")
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                transactionRepository.getTransactionWithDetails(transactionId).collect { result ->
                    result.onSuccess { transactionDetails ->
                        Log.d("TransactionDetailVM", "Loaded transaction details successfully")
                        _uiState.value = _uiState.value.copy(
                            transaction = transactionDetails.transaction,
                            seller = transactionDetails.seller,
                            product = transactionDetails.product,
                            isLoading = false,
                            errorMessage = null
                        )
                        userRepository.getUserById(transactionDetails.seller!!.id).collect {
                            _uiState.value = _uiState.value.copy(
                                seller = it.getOrNull()
                            )
                        }
                        Log.d("TransactionDetailVM", "Transaction: ${_uiState.value.transaction}")
                        Log.d("TransactionDetailVM", "Transaction User: ${_uiState.value.seller}")
                    }.onFailure { error ->
                        Log.e("TransactionDetailVM", "Error loading transaction detail", error)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load transaction details"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("TransactionDetailVM", "Exception loading transaction detail", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load transaction details: ${e.message}"
                )
            }
        }
    }

    fun refreshTransactionDetail(transactionId: String) {
        loadTransactionDetail(transactionId)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
