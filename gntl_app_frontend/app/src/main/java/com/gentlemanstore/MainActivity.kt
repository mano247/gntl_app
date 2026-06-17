package com.gentlemanstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gentlemanstore.core.ui.BottomNavBar
import com.gentlemanstore.core.ui.BottomNavItem
import com.gentlemanstore.core.ui.PlaceholderScreen
import com.gentlemanstore.core.util.Constants
import com.gentlemanstore.data.datastore.TokenDataStore
import com.gentlemanstore.feature.auth.presentation.LoginScreen
import com.gentlemanstore.feature.auth.presentation.RegisterScreen
import com.gentlemanstore.feature.auth.presentation.SplashScreen
import com.gentlemanstore.feature.cart.presentation.CartScreen
import com.gentlemanstore.feature.cart.presentation.CartViewModel
import com.gentlemanstore.feature.cart.presentation.CheckoutScreen
import com.gentlemanstore.feature.order.presentation.OrderConfirmationScreen
import com.gentlemanstore.feature.product.presentation.ProductDetailScreen
import com.gentlemanstore.feature.product.presentation.ProductListScreen
import com.gentlemanstore.feature.swipe.SwipeScreen
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
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // JEDNA instanca CartViewModel za celu aplikaciju
                val cartViewModel: CartViewModel = hiltViewModel()

                val bottomNavRoutes = listOf(
                    BottomNavItem.Home.route,
                    BottomNavItem.Discover.route,
                    BottomNavItem.Cart.route,
                    BottomNavItem.Profile.route
                )

                val showBottomBar = currentRoute in bottomNavRoutes

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.padding(innerPadding)
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
                            ProductListScreen(
                                onProductClick = { productId ->
                                    navController.navigate("product_detail/$productId")
                                }
                            )
                        }
                        composable("product_detail/{productId}") { backStackEntry ->
                            val productId = backStackEntry.arguments?.getString("productId")
                                ?.toLongOrNull() ?: return@composable

                            ProductDetailScreen(
                                productId = productId,
                                onNavigateBack = { navController.popBackStack() },
                                onAddToCart = { id, sizeId, quantity ->
                                    cartViewModel.addToCart(id, sizeId, quantity)
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("swipe") {
                            SwipeScreen(
                                onNavigateToDetail = { productId ->
                                    navController.navigate("product_detail/$productId")
                                }
                            )
                        }
                        composable("cart") {
                            CartScreen(
                                viewModel = cartViewModel,
                                onNavigateToCheckout = {
                                    navController.navigate("checkout")
                                }
                            )
                        }
                        composable("checkout") {
                            CheckoutScreen(
                                cartViewModel = cartViewModel,
                                onOrderPlaced = { order ->
                                    navController.navigate("order_confirmation") {
                                        popUpTo("home_customer")
                                    }
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("order_confirmation") {
                            val cartState by cartViewModel.uiState.collectAsStateWithLifecycle()
                            cartState.completedOrder?.let { order ->
                                OrderConfirmationScreen(
                                    order = order,
                                    onContinueShopping = {
                                        navController.navigate("home_customer") {
                                            popUpTo("home_customer") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                        composable("profile") {
                            PlaceholderScreen("PROFIL")
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