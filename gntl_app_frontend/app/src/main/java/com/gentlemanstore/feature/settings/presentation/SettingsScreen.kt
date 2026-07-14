package com.gentlemanstore.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gentlemanstore.core.util.Constants
import com.gentlemanstore.ui.theme.Gold500
import kotlinx.coroutines.launch

data class CurrencyOption(
    val code: String,
    val label: String,
    val symbol: String
)

val currencyOptions = listOf(
    CurrencyOption(Constants.CURRENCY_RSD, "Serbian Dinar", "din"),
    CurrencyOption(Constants.CURRENCY_EUR, "Euro", "€"),
    CurrencyOption(Constants.CURRENCY_USD, "US Dollar", "$")
)

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentCurrency by viewModel.currency.collectAsState()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(profileState.successMessage) {
        profileState.successMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
            viewModel.clearMessages()
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
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.titleLarge,
                    color = Gold500
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Edit Profile sekcija
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "EDIT PROFILE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = profileState.firstName,
                            onValueChange = { viewModel.onFirstNameChange(it) },
                            label = { Text("First Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = profileState.fieldErrors.containsKey("firstName"),
                            supportingText = profileState.fieldErrors["firstName"]?.let { msg ->
                                { Text(text = msg, color = MaterialTheme.colorScheme.error) }
                            }
                        )
                        OutlinedTextField(
                            value = profileState.lastName,
                            onValueChange = { viewModel.onLastNameChange(it) },
                            label = { Text("Last Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = profileState.fieldErrors.containsKey("lastName"),
                            supportingText = profileState.fieldErrors["lastName"]?.let { msg ->
                                { Text(text = msg, color = MaterialTheme.colorScheme.error) }
                            }
                        )
                        OutlinedTextField(
                            value = profileState.phoneNumber,
                            onValueChange = { viewModel.onPhoneNumberChange(it) },
                            label = { Text("Phone (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = profileState.fieldErrors.containsKey("phoneNumber"),
                            supportingText = profileState.fieldErrors["phoneNumber"]?.let { msg ->
                                { Text(text = msg, color = MaterialTheme.colorScheme.error) }
                            }
                        )

                        profileState.error?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        profileState.successMessage?.let {
                            Text(text = it, color = androidx.compose.ui.graphics.Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = { viewModel.updateProfile() },
                            enabled = !profileState.isUpdating && profileState.firstName.isNotBlank() && profileState.lastName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (profileState.isUpdating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Save Changes", color = MaterialTheme.colorScheme.background)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Currency sekcija
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "CURRENCY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                currencyOptions.forEach { option ->
                    val isSelected = currentCurrency == option.code

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { viewModel.setCurrency(option.code) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Gold500.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.symbol,
                                style = MaterialTheme.typography.labelMedium,
                                color = Gold500
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = option.code,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Gold500
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}