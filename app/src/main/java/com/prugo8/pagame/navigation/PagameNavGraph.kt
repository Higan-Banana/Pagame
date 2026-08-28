package com.prugo8.pagame.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prugo8.pagame.ui.screens.*
import com.prugo8.pagame.viewmodel.PagameViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CreateEvent : Screen("create_event")
    object EventDetail : Screen("event_detail/{eventId}") {
        fun createRoute(eventId: String) = "event_detail/$eventId"
    }
    object Balance : Screen("balance/{eventId}") {
        fun createRoute(eventId: String) = "balance/$eventId"
    }
    object Settled : Screen("settled/{eventId}") {
        fun createRoute(eventId: String) = "settled/$eventId"
    }
}

@Composable
fun PagameNavGraph(
    viewModel: PagameViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onCreateEventClick = { navController.navigate(Screen.CreateEvent.route) },
                onEventClick = { eventId ->
                    viewModel.selectEvent(eventId)
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                }
            )
        }

        composable(Screen.CreateEvent.route) {
            CreateEventScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEventCreated = { eventId ->
                    navController.popBackStack()
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                }
            )
        }

        composable(
            route = Screen.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            EventDetailScreen(
                eventId = eventId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onBalanceClick = { navController.navigate(Screen.Balance.createRoute(eventId)) }
            )
        }

        composable(
            route = Screen.Balance.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            BalanceScreen(
                eventId = eventId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSettleClick = {
                    viewModel.settleEvent(eventId)
                    navController.navigate(Screen.Settled.createRoute(eventId))
                }
            )
        }

        composable(
            route = Screen.Settled.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            SettledScreen(
                eventId = eventId,
                viewModel = viewModel,
                onReturnHome = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }
    }
}
