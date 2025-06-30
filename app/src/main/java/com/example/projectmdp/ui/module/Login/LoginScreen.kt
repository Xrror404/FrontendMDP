package com.example.projectmdp.ui.module.login

import android.app.Activity
import android.content.IntentSender
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.Identity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel(), modifier: Modifier = Modifier, navController: NavController) {
    val email = viewModel.email
    val password = viewModel.password
    val isLoading = viewModel.isLoading
    val context = LocalContext.current

    // Collect error messages for toast notifications
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Show toast for error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(true) {
        //buat lek gk mau auto login
//        viewModel.deleteAutoLogin()
        viewModel.checkAutoLogin()
    }


    // Collect navigation events
    val scope = rememberCoroutineScope()
    LaunchedEffect(key1 = true) {
        viewModel.navigationEvent.collectLatest { destination ->
            navController.navigate(destination) {
                // Clear the back stack so user can't go back to login screen
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Sign in to continue",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = @Composable { Text("Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                leadingIcon = @Composable {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email"
                    )
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = @Composable { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                leadingIcon = @Composable {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password"
                    )
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            val emailState = remember { mutableStateOf("") }
            val showDialog = remember { mutableStateOf(false) }
            val resetResult by viewModel.resetPasswordState.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Forgot Password?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        showDialog.value = true // Munculkan dialog saat diklik
                    }
                )
            }

// ⬇️ Tambahkan bagian ini langsung di bawah Row di atas
//            val context = LocalContext.current


            if (showDialog.value) {
                AlertDialog(
                    onDismissRequest = {
                        showDialog.value = false
                        viewModel.clearResetPasswordState()
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.sendPasswordResetEmail(emailState.value)
                        }) {
                            Text("Send")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDialog.value = false
                            viewModel.clearResetPasswordState()
                        }) {
                            Text("Cancel")
                        }
                    },
                    title = { Text("Reset Password") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = emailState.value,
                                onValueChange = { emailState.value = it },
                                label = { Text("Email") },
                                singleLine = true,
                            )
                            if (resetResult != null) {
                                when {
                                    resetResult?.isSuccess == true -> {
                                        Text("✔ ${resetResult?.getOrNull()}", color = Color.Green)
                                    }
                                    resetResult?.isFailure == true -> {
                                        Text("✘ ${resetResult?.exceptionOrNull()?.message}", color = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                )
            }


            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.login() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        "Login",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val activity = context as Activity
            val webClientId = "634972513606-mbo2jqteefq4teo1mlhb62ekeleh34fa.apps.googleusercontent.com" // replace with actual Web Client ID from Firebase

            val oneTapClient = remember { Identity.getSignInClient(context) }
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                    val idToken = credential.googleIdToken
                    if (idToken != null) {
                        viewModel.signInWithGoogle(idToken)
                    } else {
                        Log.e("OneTap", "No ID token!")
                    }
                } else {
                    Log.e("OneTap", "Sign-in failed or canceled")
                }
            }

            OutlinedButton(
                onClick = {
                    val signInRequest = viewModel.buildOneTapSignInRequest(webClientId)
                    oneTapClient.beginSignIn(signInRequest)
                        .addOnSuccessListener { result ->
                            try {
                                val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                                launcher.launch(intentSenderRequest)
                            } catch (e: IntentSender.SendIntentException) {
                                Log.e("OneTap", "Couldn't start One Tap UI: ${e.localizedMessage}")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("OneTap", "One Tap Sign-in failed: ${e.localizedMessage}")
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Login with Google",
                    style = MaterialTheme.typography.titleMedium
                )
            }


            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Register here",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        navController.navigate("register")
                    }
                )
            }
        }
    }
}

private fun LoginViewModel.loginWithGoogle() {
    Log.d("GoogleLogin", "Google login initiated")
}
