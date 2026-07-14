package com.gentlemanstore.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gentlemanstore.ui.theme.Gold500

@Composable
fun RegisterScreen(
    onRegisterSuccess: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.isSuccess) {
        if(uiState.isSuccess && uiState.userRole != null){
            onRegisterSuccess(uiState.userRole!!)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "GENTLEMAN",
                style =  MaterialTheme.typography.headlineLarge,
                color = Gold500
            )
            Text(
                text = "CREATE ACCOUNT",
                style =  MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Backend validacione greske po polju (RegisterRequest nazivi polja)
            val fieldErrors = uiState.fieldErrors

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it; viewModel.clearError() },
                label = { Text("First Name") },
                singleLine = true,
                isError = fieldErrors.containsKey("firstName"),
                supportingText = fieldErrors["firstName"]?.let { msg ->
                    { Text(text = msg, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it; viewModel.clearError() },
                label = { Text("Last Name") },
                singleLine = true,
                isError = fieldErrors.containsKey("lastName"),
                supportingText = fieldErrors["lastName"]?.let { msg ->
                    { Text(text = msg, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; viewModel.clearError() },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                isError = fieldErrors.containsKey("email"),
                supportingText = fieldErrors["email"]?.let { msg ->
                    { Text(text = msg, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; viewModel.clearError() },
                label = { Text("Phone (optional)") },
                singleLine = true,
                isError = fieldErrors.containsKey("phoneNumber"),
                supportingText = fieldErrors["phoneNumber"]?.let { msg ->
                    { Text(text = msg, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                    viewModel.clearError()
                },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = {passwordVisible = !passwordVisible}) {
                        Text(
                            text = if (passwordVisible) "Hide" else "Show",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold500
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                isError = fieldErrors.containsKey("password"),
                supportingText = fieldErrors["password"]?.let { msg ->
                    { Text(text = msg, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    passwordError = null
                },
                label = { Text("Confirm password") },
                singleLine = true,
                isError = passwordError != null,
                supportingText = {
                    if (passwordError != null){
                        Text(
                            text = passwordError!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            if(uiState.error != null){
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (password != confirmPassword){
                        passwordError = "Passwords do not match"
                        return@Button
                    }
                    // Uskladjeno sa backend politikom lozinke (min 8 karaktera)
                    if (password.length < 8){
                        passwordError = "Password must be at least 8 characters"
                        return@Button
                    }
                    viewModel.register(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        password = password,
                        phone = phone.ifBlank { null }
                    )
                },
                enabled = !uiState.isLoading
                        &&firstName.isNotBlank()
                        &&lastName.isNotBlank()
                        &&email.isNotBlank()
                        &&password.isNotBlank()
                        &&confirmPassword.isNotBlank(),
                modifier =  Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =  Gold500,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ){
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.background,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = buildAnnotatedString {
                        append("Already have an account? ")
                        withStyle(
                            style = SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                color = Gold500
                            )
                        ) {
                            append("Sign In")
                        }
                    },
                    color = Gold500,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}