package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val viewModel: ReceiptViewModel = viewModel()
    
    // Bottom nav items
    val bottomNavItems = listOf(
        BottomNavItem("dashboard", "Dashboard", Icons.Filled.Dashboard),
        BottomNavItem("customer_list", "Customers", Icons.Filled.People),
        BottomNavItem("product_list", "Products", Icons.Filled.Inventory),
        BottomNavItem("business_profile", "Profile", Icons.Filled.Business)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom navigation bar on sub-pages
    val shouldShowBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo("dashboard") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag("nav_item_${item.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Dashboard
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToCreateReceipt = {
                        navController.navigate("receipt_creator")
                    },
                    onNavigateToReceiptDetail = { receiptId ->
                        navController.navigate("receipt_detail/$receiptId")
                    },
                    onNavigateToPremium = {
                        navController.navigate("premium")
                    }
                )
            }

            // Customer List
            composable("customer_list") {
                CustomerScreen(
                    viewModel = viewModel,
                    onNavigateToReceiptDetail = { receiptId ->
                        navController.navigate("receipt_detail/$receiptId")
                    }
                )
            }

            // Product List
            composable("product_list") {
                ProductScreen(
                    viewModel = viewModel
                )
            }

            // Business Profile
            composable("business_profile") {
                ProfileScreen(
                    viewModel = viewModel
                )
            }

            // Receipt Creator (Full Page)
            composable("receipt_creator") {
                ReceiptCreatorScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onReceiptGenerated = { generatedId ->
                        // Redirect to receipt detail
                        navController.navigate("receipt_detail/$generatedId") {
                            popUpTo("dashboard")
                        }
                    }
                )
            }

            // Receipt Detail (Full Page)
            composable(
                route = "receipt_detail/{receiptId}",
                arguments = listOf(navArgument("receiptId") { type = NavType.LongType })
            ) { backStackEntry ->
                val receiptId = backStackEntry.arguments?.getLong("receiptId") ?: 0L
                ReceiptDetailScreen(
                    receiptId = receiptId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Premium screen
            composable("premium") {
                PremiumScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
