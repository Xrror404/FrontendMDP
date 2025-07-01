package com.example.projectmdp.ui.module.login

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmdp.data.repository.AuthRepository
import com.example.projectmdp.data.source.local.SessionManager
import com.example.projectmdp.data.source.remote.RetrofitInstance
import com.example.projectmdp.data.source.remote.VerifyTokenRequest
import com.example.projectmdp.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set

    var idToken by mutableStateOf("")
        private set

    // Add error message state for showing toast
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _googleSignInEvent = MutableSharedFlow<Unit>()
    val googleSignInEvent = _googleSignInEvent.asSharedFlow()
    private val _resetPasswordState = MutableStateFlow<Result<String>?>(null)
    val resetPasswordState = _resetPasswordState.asStateFlow()

    fun onEmailChange(newEmail: String) { email = newEmail }
    fun onPasswordChange(newPassword: String) { password = newPassword }

    // Function to clear error message after it's been shown
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun deleteAutoLogin(){
        sessionManager.clearToken()
    }

    fun checkAutoLogin() {
        isLoading = true
        val savedToken = sessionManager.getToken()
        if (!savedToken.isNullOrBlank()) {
            Log.d("AutoLogin", "Saved token found: $savedToken")
            RetrofitInstance.setToken(savedToken)

            viewModelScope.launch {
                try {
                    val response = authRepository.verifyToken(VerifyTokenRequest(savedToken))
                    val userRole = response.data?.user?.role
                    _navigationEvent.emit(Routes.USER_DASHBOARD)
                    isLoading = false
                } catch (e: Exception) {
                    Log.e("AutoLogin", "Failed: ${e.message}")
                    // Jika token tidak valid, bisa clear dari session
                    sessionManager.clearToken()
                    isLoading = false
                }
            }
        } else {
            Log.d("AutoLogin", "No token found")
            isLoading = false
        }
    }

    fun login() {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email and password cannot be empty"
            return
        }
        isLoading = true
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Login", "Success")
                    auth.currentUser?.getIdToken(true)
                        ?.addOnSuccessListener { result ->
                            idToken = result.token ?: ""
                            Log.d("Token", "ID Token: $idToken")
                            RetrofitInstance.setToken(idToken)
                            sessionManager.saveToken(idToken)
                            // Call backend login
                            viewModelScope.launch {
                                try {
                                    val response = authRepository.verifyToken(VerifyTokenRequest(idToken))
                                    Log.d("BackendLogin", "Success: $response")

                                    // Check user role and navigate accordingly
                                    val userRole = response.data?.user?.role
                                    if (userRole?.equals("user", ignoreCase = true) == true || userRole?.equals("buyer", ignoreCase = true) == true) {
                                        response.data?.user?.id?.let { userId ->
                                            sessionManager.saveUserId(userId)
                                            Log.d("Login", "User ID saved: $userId")
                                        }
                                        _navigationEvent.emit(Routes.USER_DASHBOARD)
                                    } else {
                                        // For other roles, you can add navigation to their respective screens
                                        Log.d("Login", "User has role: $userRole")
                                    }
                                } catch (e: Exception) {
                                    Log.e("BackendLogin", "Failed: ${e.message}")
                                    _errorMessage.value = "Failed to authenticate: ${e.message}"
                                    isLoading = false
                                }
                            }
                        }
                        ?.addOnFailureListener { e ->
                            Log.e("Token", "Gagal ambil token: ${e.message}")
                            _errorMessage.value = "Failed to get authentication token"
                            isLoading = false
                        }
                } else {
                    Log.e("Login", "Failed: ${task.exception?.message}")
                    _errorMessage.value = task.exception?.message ?: "Authentication failed"
                    isLoading = false
                }
            }
    }

    fun firebaseAuthWithGoogleIdToken(idToken: String) {
        isLoading = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("GoogleAuth", "signInWithCredential:success")
                    auth.currentUser?.getIdToken(true)
                        ?.addOnSuccessListener { result ->
                            this.idToken = result.token ?: ""
                            Log.d("GoogleAuth", "ID Token: ${this.idToken}")
                            RetrofitInstance.setToken(this.idToken)

                            // Call backend to verify token and navigate to dashboard
                            viewModelScope.launch {
                                try {
                                    val response = authRepository.verifyToken(VerifyTokenRequest(this@LoginViewModel.idToken))
                                    Log.d("GoogleAuth", "Backend success: $response")

                                    // Check user role and navigate accordingly
                                    val userRole = response.data?.user?.role
                                    if (userRole?.equals("user", ignoreCase = true) == true) {
                                        _navigationEvent.emit(Routes.USER_DASHBOARD)
                                    } else {
                                        // For other roles, you can add navigation to their respective screens
                                        Log.d("Login", "User has role: $userRole")
                                    }
                                } catch (e: Exception) {
                                    Log.e("GoogleAuth", "Backend error: ${e.message}")
                                    _errorMessage.value = "Server authentication failed: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                        ?.addOnFailureListener { e ->
                            Log.e("GoogleAuth", "Failed to get token: ${e.message}")
                            _errorMessage.value = "Failed to get authentication token"
                            isLoading = false
                        }
                } else {
                    Log.e("GoogleAuth", "signInWithCredential:failure", task.exception)
                    _errorMessage.value = task.exception?.message ?: "Google authentication failed"
                    isLoading = false
                }
            }
    }

    // Function to build the One Tap sign-in request
    fun buildOneTapSignInRequest(webClientId: String): BeginSignInRequest {
        return BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true) // Wajib true untuk bisa sign-in ke Firebase
                    .setServerClientId(webClientId) // Web client ID Anda dari Firebase
                    .setFilterByAuthorizedAccounts(false) // Set false agar semua akun Google di perangkat ditampilkan
                    .build()
            )
            //tggl diubah true false, kalau true lgsg login otomatis
            .setAutoSelectEnabled(false) // Coba login otomatis jika memungkinkan
            .build()
    }

    fun signInWithGoogle(idToken: String?) {
        if (idToken == null) {
            Log.e("Auth", "ID Token is null")
            _errorMessage.value = "Google authentication failed: Missing ID token"
            return
        }

        isLoading = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Auth", "Firebase sign-in success")
                    auth.currentUser?.getIdToken(true)
                        ?.addOnSuccessListener { result ->
                            val token = result.token
                            if (token != null) {
                                Log.d("Auth", "Firebase ID Token: $token")
                                RetrofitInstance.setToken(token)
                                sessionManager.saveToken(token)
                                viewModelScope.launch {
                                    try {
                                        val response = authRepository.verifyToken(VerifyTokenRequest(token))
                                        Log.d("Auth", "Backend success: $response")

                                        // Check user role and navigate accordingly
                                        val userRole = response.data?.user?.role
                                        if (userRole?.equals("user", ignoreCase = true) == true || userRole?.equals("buyer", ignoreCase = true) == true) {
                                            response.data?.user?.id?.let { userId ->
                                                sessionManager.saveUserId(userId)
                                                Log.d("Login", "User ID saved: $userId")
                                            }
                                            _navigationEvent.emit(Routes.USER_DASHBOARD)
                                        } else {
                                            // For other roles, you can add navigation to their respective screens
                                            Log.d("Login", "User has role: $userRole")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("Auth", "Backend error: ${e.message}")
                                        _errorMessage.value = "Server authentication failed: ${e.message}"
                                        isLoading = false
                                    }
                                }
                            }
                        }
                        ?.addOnFailureListener { e ->
                            Log.e("Auth", "Failed to get token: ${e.message}")
                            _errorMessage.value = "Failed to get authentication token"
                            isLoading = false
                        }
                } else {
                    Log.e("Auth", "Firebase sign-in failed: ${task.exception?.message}")
                    _errorMessage.value = task.exception?.message ?: "Google authentication failed"
                    isLoading = false
                }
            }
    }

    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _resetPasswordState.value = Result.failure(Exception("Email cannot be empty"))
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _resetPasswordState.value = Result.success("Reset email sent to $email")
                } else {
                    val error = task.exception?.localizedMessage ?: "Unknown error"
                    _resetPasswordState.value = Result.failure(Exception(error))
                }
            }
    }

    fun clearResetPasswordState() {
        _resetPasswordState.value = null
    }

}
