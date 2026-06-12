package com.gentlemanstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gentlemanstore.core.util.Constants
import com.gentlemanstore.core.ui.PlaceholderScreen
import com.gentlemanstore.data.datastore.TokenDataStore
import com.gentlemanstore.feature.auth.presentation.LoginScreen
import com.gentlemanstore.feature.auth.presentation.RegisterScreen
import com.gentlemanstore.feature.auth.presentation.SplashScreen
import com.gentlemanstore.ui.theme.GentlemanStoreTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenDataStore: TokenDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GentlemanStoreTheme(darkTheme = true) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "splash"
                ) {
                    composable("splash") {
                        SplashScreen(
                            onNavigateToLogin = {
                                navController.navigate("login") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            },
                            onNavigateToHome = { role ->
                                val destination = getRoleDestination(role)
                                navController.navigate(destination) {
                                    popUpTo("splash") { inclusive = true }
                                }
                            },
                            tokenDataStore = tokenDataStore
                        )
                    }
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = { role ->
                                val destination = getRoleDestination(role)
                                navController.navigate(destination) {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToRegister = {
                                navController.navigate("register")
                            }
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = { role ->
                                val destination = getRoleDestination(role)
                                navController.navigate(destination) {
                                    popUpTo("register") { inclusive = true }
                                }
                            },
                            onNavigateToLogin = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("home_customer") {
                        // placeholder — dodajemo u Fazi 3
                        PlaceholderScreen("CUSTOMER HOME")
                    }
                    composable("home_employee") {
                        PlaceholderScreen("EMPLOYEE HOME")
                    }
                    composable("home_manager") {
                        PlaceholderScreen("MANAGER HOME")
                    }
                    composable("home_admin") {
                        PlaceholderScreen("ADMIN HOME")
                    }
                }
            }
        }
    }
}

fun getRoleDestination(role: String): String {
    return when (role) {
        Constants.ROLE_CUSTOMER -> "home_customer"
        Constants.ROLE_EMPLOYEE -> "home_employee"
        Constants.ROLE_MANAGER -> "home_manager"
        Constants.ROLE_ADMIN -> "home_admin"
        else -> "home_customer"
    }
}