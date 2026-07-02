package com.gentlemanstore.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gentlemanstore.core.util.JwtUtils
import com.gentlemanstore.data.datastore.TokenDataStore
import com.gentlemanstore.feature.auth.domain.AuthRepository
import com.gentlemanstore.ui.theme.Gold500
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: (String) -> Unit,
    tokenDataStore: TokenDataStore,
    authRepository: AuthRepository
){
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            var token = tokenDataStore.token.first()

            // Access token is short-lived; if it expired while the app was closed,
            // try a silent refresh with the long-lived refresh token before forcing a re-login.
            if (token != null && JwtUtils.isTokenExpired(token)) {
                token = if (authRepository.tryRefreshToken()) tokenDataStore.token.first() else null
            }

            if (token == null || JwtUtils.isTokenExpired(token)) {
                onNavigateToLogin()
            } else {
                // The backend JWT carries only the subject (email) — no role claim —
                // so the role saved to DataStore at login is the source of truth here.
                val role = tokenDataStore.userRole.first() ?: run {
                    onNavigateToLogin()
                    return@launch
                }
                onNavigateToHome(role)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment =  Alignment.Center
    ){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            Text(
                text = "GENTLEMAN",
                style = MaterialTheme.typography.headlineLarge,
                color = Gold500
            )
            Text(
                text = "STORE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier =  Modifier.height(32.dp))
            CircularProgressIndicator(
                color = Gold500,
                strokeWidth = 2.dp
            )
        }
    }
}