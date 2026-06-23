package com.gentlemanstore.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.gentlemanstore.ui.theme.Gold500

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem("home_customer", Icons.Default.Home, "Home")
    object Discover : BottomNavItem("swipe", Icons.Default.Style, "Discover")
    object Cart : BottomNavItem("cart", Icons.Default.ShoppingCart, "Cart")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
}

@Composable
fun BottomNavBar(
    navController: NavController,
    unreadNotificationCount: Int = 0,
    unreadSupportCount: Int = 0
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Discover,
        BottomNavItem.Cart,
        BottomNavItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo("home_customer") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Box {
                        Icon(imageVector = item.icon, contentDescription = item.label)
                        if (item is BottomNavItem.Profile) {
                            val totalCount = unreadNotificationCount + unreadSupportCount
                            if (totalCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color.Red, CircleShape)
                                        .align(Alignment.TopEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (totalCount > 9) "9+" else totalCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontSize = TextUnit(8f, TextUnitType.Sp)
                                    )
                                }
                            }
                        }
                    }
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Gold500,
                    selectedTextColor = Gold500,
                    indicatorColor = MaterialTheme.colorScheme.surface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}