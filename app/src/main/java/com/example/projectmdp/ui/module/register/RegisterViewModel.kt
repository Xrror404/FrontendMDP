package com.example.projectmdp.ui.module.register

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmdp.data.model.auth.RegisterDto
import com.example.projectmdp.data.repository.AuthRepository
import com.example.projectmdp.data.repository.UserRepository
import com.example.projectmdp.data.source.dataclass.User
import com.example.projectmdp.data.source.remote.RetrofitInstance
import com.example.projectmdp.data.source.remote.VerifyTokenRequest
import com.example.projectmdp.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    var email by mutableStateOf("")
    //        private set
    var password by mutableStateOf("")
    //        private set
    var confirmPassword by mutableStateOf("")
    //        private set
    var address by mutableStateOf("")
    //        private set
    var phoneNumber by mutableStateOf("")
    //        private set
    var isLoading by mutableStateOf(false)
//        private set

    // Add error message state for showing toast
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _googleSignInEvent = MutableSharedFlow<Unit>()
    val googleSignInEvent = _googleSignInEvent.asSharedFlow()

    // Add navigation event for successful registration
    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    var updateResult by mutableStateOf<Result<User>?>(null)

    fun onEmailChange(newEmail: String) { email = newEmail }
    fun onPasswordChange(newPassword: String) { password = newPassword }
    fun onConfirmPasswordChange(newConfirmPassword: String) { confirmPassword = newConfirmPassword }
    fun onAddressChange(newAddress: String) { address = newAddress }
    fun onPhoneNumberChange(newPhoneNumber: String) { phoneNumber = newPhoneNumber }

    // Function to clear error message after it's been shown
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun register() {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email and password cannot be empty"
            return
        }

        if (password != confirmPassword) {
            _errorMessage.value = "Passwords do not match"
            return
        }

        isLoading = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Register", "Firebase auth success")

                    // Ambil ID Token dari Firebase
                    auth.currentUser?.getIdToken(true)
                        ?.addOnSuccessListener { result ->
                            val idToken = result.token
                            Log.d("Register", "ID Token: $idToken")

                            if (idToken != null) {
                                viewModelScope.launch {
                                    try {
                                        // Kirim ID Token ke backend sesuai endpoint verifyFirebaseToken
                                        val response = authRepository.verifyToken(VerifyTokenRequest(idToken))
                                        Log.d("BackendRegister", "Success Authenticate: $response")
                                        Log.d("Register", "address=$address, phoneNumber=$phoneNumber")
                                        RetrofitInstance.setToken(idToken)
                                        userRepository.updateUserProfile(
                                            address = address,
                                            phoneNumber = phoneNumber
                                        ).collect { result ->
                                            Log.d("BackendRegister", "update result: $result")
                                            updateResult = result
                                        }
//                                        val updateDataUser = userRepository.updateUserProfile(address = address, phoneNumber = phoneNumber)
//                                        Log.d("BackendRegister", "Success Add Address and Phone: $updateDataUser")
                                        // Navigate to login screen after successful registration
                                        _navigationEvent.emit(Routes.LOGIN)
                                    } catch (e: Exception) {
                                        Log.e("BackendRegister", "Failed: ${e.message}")
                                        _errorMessage.value = "Server registration failed: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                Log.e("Register", "Failed to retrieve ID token")
                                _errorMessage.value = "Failed to retrieve authentication token"
                                isLoading = false
                            }
                        }
                        ?.addOnFailureListener { e ->
                            Log.e("Register", "Get ID token failed: ${e.message}")
                            _errorMessage.value = "Token retrieval failed: ${e.message}"
                            isLoading = false
                        }
                } else {
                    Log.e("Register", "Firebase auth failed: ${task.exception?.message}")
                    _errorMessage.value = task.exception?.message ?: "Registration failed"
                    isLoading = false
                }
            }
    }

    fun onGoogleSignInClicked() {
        viewModelScope.launch {
            _googleSignInEvent.emit(Unit)
        }
    }

    fun onGoogleSignInResult(idToken: String?) {
        if (idToken == null) {
            Log.e("RegisterViewModel", "Google sign-in failed: ID Token is null")
            _errorMessage.value = "Google sign-in failed: Missing ID token"
            return
        }

        isLoading = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("RegisterViewModel", "Firebase auth with Google success")

                    // Get ID token
                    auth.currentUser?.getIdToken(true)
                        ?.addOnSuccessListener { result ->
                            val idToken = result.token
                            Log.d("Register", "ID Token: $idToken")

                            if (idToken != null) {
                                viewModelScope.launch {
                                    try {
                                        //  Kirim ID Token ke backend sesuai endpoint verifyFirebaseToken
                                        val response = authRepository.verifyToken(VerifyTokenRequest(idToken))
                                        Log.d("Register With Google", "Success: $response")

                                        // Navigate to login screen after successful Google registration
                                        _navigationEvent.emit(Routes.LOGIN)
                                    } catch (e: Exception) {
                                        Log.e("Register With Google", "Failed: ${e.message}")
                                        _errorMessage.value = "Server registration failed: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                Log.e("Register", "Failed to retrieve ID token")
                                _errorMessage.value = "Failed to retrieve authentication token"
                                isLoading = false
                            }
                        }
                        ?.addOnFailureListener { e ->
                            Log.e("Register", "Get ID token failed: ${e.message}")
                            _errorMessage.value = "Token retrieval failed: ${e.message}"
                            isLoading = false
                        }
                } else {
                    Log.e("RegisterViewModel", "Firebase auth with Google failed: ${task.exception?.message}")
                    _errorMessage.value = task.exception?.message ?: "Google registration failed"
                    isLoading = false
                }
            }
    }
}
