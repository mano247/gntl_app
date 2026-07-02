package com.gentlemanstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gentlemanstore.core.ui.BottomNavBar
import com.gentlemanstore.core.ui.BottomNavItem
import com.gentlemanstore.core.util.Constants
import com.gentlemanstore.data.datastore.TokenDataStore
import com.gentlemanstore.feature.admin.presentation.AdminHomeScreen
import com.gentlemanstore.feature.auth.domain.AuthRepository
import com.gentlemanstore.feature.auth.presentation.LoginScreen
import com.gentlemanstore.feature.auth.presentation.RegisterScreen
import com.gentlemanstore.feature.auth.presentation.SplashScreen
import com.gentlemanstore.feature.cart.presentation.CartScreen
import com.gentlemanstore.feature.cart.presentation.CartViewModel
import com.gentlemanstore.feature.cart.presentation.CheckoutScreen
import com.gentlemanstore.feature.employee.presentation.EmployeeHomeScreen
import com.gentlemanstore.feature.loyalty.presentation.LoyaltyScreen
import com.gentlemanstore.feature.manager.presentation.ManagerHomeScreen
import com.gentlemanstore.feature.notification.presentation.NotificationScreen
import com.gentlemanstore.feature.order.presentation.MyOrdersScreen
import com.gentlemanstore.feature.order.presentation.OrderConfirmationScreen
import com.gentlemanstore.feature.order.presentation.OrderDetailScreen
import com.gentlemanstore.feature.product.presentation.ProductDetailScreen
import com.gentlemanstore.feature.product.presentation.ProductListScreen
import com.gentlemanstore.feature.profile.presentation.ProfileScreen
import com.gentlemanstore.feature.settings.presentation.SettingsScreen
import com.gentlemanstore.feature.support.presentation.BotFlowScreen
import com.gentlemanstore.feature.support.presentation.ChatScreen
import com.gentlemanstore.feature.support.presentation.SupportScreen
import com.gentlemanstore.feature.support.presentation.SupportViewModel
import com.gentlemanstore.feature.swipe.SwipeScreen
import com.gentlemanstore.ui.theme.GentlemanStoreTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenDataStore: TokenDataStore

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GentlemanStoreTheme(darkTheme = true) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val cartViewModel: CartViewModel = hiltViewModel()
                val notificationViewModel: com.gentlemanstore.feature.notification.presentation.NotificationViewModel = hiltViewModel()
                val notificationState by notificationViewModel.uiState.collectAsStateWithLifecycle()

                val supportViewModel: SupportViewModel = hiltViewModel()
                val supportState by supportViewModel.supportUiState.collectAsStateWithLifecycle()

                LaunchedEffect(currentRoute) {
                    if (currentRoute == "profile" || currentRoute == "home_customer") {
                        notificationViewModel.loadUnreadCount()
                        supportViewModel.loadMyTickets()
                    }
                    if (currentRoute == "notifications") {
                        notificationViewModel.loadNotifications()
                    }
                }

                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val bottomNavRoutes = listOf(
                    BottomNavItem.Home.route,
                    BottomNavItem.Discover.route,
                    BottomNavItem.Cart.route,
                    BottomNavItem.Profile.route
                )

                val showBottomBar = currentRoute in bottomNavRoutes

                val showSnackbar: (String) -> Unit = { message ->
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavBar(
                                navController = navController,
                                unreadNotificationCount = notificationState.unreadCount,
                                unreadSupportCount = supportState.tickets.sumOf { it.unreadCount }
                            )
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
                                tokenDataStore = tokenDataStore,
                                authRepository = authRepository
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
                                        // Clear both "login" and "register" so back from home
                                        // doesn't land on an auth screen while logged in.
                                        popUpTo("login") { inclusive = true }
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
                                },
                                cartViewModel = cartViewModel
                            )
                        }
                        composable("cart") {
                            CartScreen(
                                onNavigateToCheckout = {
                                    navController.navigate("checkout")
                                },
                                onShowError = showSnackbar,
                                viewModel = cartViewModel
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
                                onNavigateBack = { navController.popBackStack() },
                                onShowError = showSnackbar
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
                            ProfileScreen(
                                onNavigateToOrders = { navController.navigate("my_orders") },
                                onNavigateToLoyalty = { navController.navigate("loyalty") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToNotifications = { navController.navigate("notifications") },
                                onNavigateToSupport = { navController.navigate("support") },
                                onLoggedOut = {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("support") {
                            SupportScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onStartBotFlow = { navController.navigate("bot_flow") },
                                onOpenChat = { ticketId, sessionId ->
                                    navController.navigate("chat/$ticketId/$sessionId")
                                }
                            )
                        }
                        composable("bot_flow") {
                            BotFlowScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onFlowComplete = { ticketId, sessionId ->
                                    navController.navigate("chat/$ticketId/$sessionId") {
                                        popUpTo("support")
                                    }
                                }
                            )
                        }
                        composable("chat/{ticketId}/{sessionId}") { backStackEntry ->
                            val ticketId = backStackEntry.arguments?.getString("ticketId")?.toLongOrNull() ?: return@composable
                            val sessionId = backStackEntry.arguments?.getString("sessionId")?.toLongOrNull() ?: return@composable
                            ChatScreen(
                                ticketId = ticketId,
                                sessionId = sessionId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("loyalty") {
                            LoyaltyScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("my_orders") {
                            MyOrdersScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onOrderClick = { orderId ->
                                    navController.navigate("order_detail/$orderId")
                                },
                                onShowError = showSnackbar
                            )
                        }
                        composable("order_detail/{orderId}") { backStackEntry ->
                            val orderId = backStackEntry.arguments?.getString("orderId")
                                ?.toLongOrNull() ?: return@composable
                            OrderDetailScreen(
                                orderId = orderId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("notifications") {
                            NotificationScreen(
                                onNavigateBack = { navController.popBackStack() },
                                viewModel = notificationViewModel
                            )
                        }
                        composable("home_employee") {
                            EmployeeHomeScreen(
                                onOpenChat = { ticketId, sessionId ->
                                    navController.navigate("chat/$ticketId/$sessionId")
                                },
                                onLogout = {
                                    lifecycleScope.launch {
                                        authRepository.logout()
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                },
                                onShowError = showSnackbar
                            )
                        }
                        composable("home_manager") {
                            ManagerHomeScreen(
                                onLogout = {
                                    lifecycleScope.launch {
                                        authRepository.logout()
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                },
                                onShowError = showSnackbar
                            )
                        }
                        composable("home_admin") {
                            AdminHomeScreen(
                                onLogout = {
                                    lifecycleScope.launch {
                                        authRepository.logout()
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                },
                                onShowError = showSnackbar,
                                onShowSuccess = showSnackbar
                            )
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