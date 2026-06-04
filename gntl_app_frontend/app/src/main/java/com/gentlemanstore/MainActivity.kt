package com.gentlemanstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gentlemanstore.feature.auth.presentation.LoginScreen
import com.gentlemanstore.feature.auth.presentation.RegisterScreen
import com.gentlemanstore.ui.theme.GentlemanStoreTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GentlemanStoreTheme(darkTheme = true) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "login"
                ){
                    composable("login"){
                        LoginScreen(
                            onLoginSuccess = { role ->

                            },
                            onNavigateToRegister = {
                                navController.navigate("register")
                            }
                        )
                    }
                    composable("register"){
                        RegisterScreen(
                            onRegisterSuccess =  { role ->

                            },
                            onNavigateToLogin = {
                                navController.popBackStack()
                            }
                        )
                    }
                }

            }
        }
    }
}