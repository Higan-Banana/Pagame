package gt.pagame.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import gt.pagame.app.ui.components.PagameBottomBar
import gt.pagame.app.ui.screens.eventdetail.EventDetailScreen
import gt.pagame.app.ui.screens.eventdetail.EventDetailViewModel
import gt.pagame.app.ui.screens.events.EventsScreen
import gt.pagame.app.ui.screens.events.EventsViewModel
import gt.pagame.app.ui.screens.friends.FriendsScreen
import gt.pagame.app.ui.screens.friends.FriendsViewModel
import gt.pagame.app.ui.screens.login.LoginScreen
import gt.pagame.app.ui.screens.login.LoginViewModel
import gt.pagame.app.ui.screens.options.OptionsScreen
import gt.pagame.app.ui.screens.options.OptionsViewModel
import gt.pagame.app.ui.screens.register.RegisterScreen
import gt.pagame.app.ui.screens.register.RegisterViewModel
import androidx.compose.material.icons.filled.Add
import gt.pagame.app.ui.components.PagameExtendedFab

@Composable
fun PagameNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        // 1. Login Screen
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Register Screen
        composable(Screen.Register.route) {
            val registerViewModel: RegisterViewModel = viewModel()
            RegisterScreen(
                viewModel = registerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        // 3. Main Container (Events, Friends, Options with Bottom Bar)
        composable(Screen.Main.route) {
            MainContainer(
                onNavigateToEventDetail = { eventId ->
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                },
                onLogoutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }

        // 4. Event Detail Screen (Pizza & Birra)
        composable(
            route = Screen.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) {
            val detailViewModel: EventDetailViewModel = viewModel()
            EventDetailScreen(
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainContainer(
    onNavigateToEventDetail: (String) -> Unit,
    onLogoutSuccess: () -> Unit
) {
    var currentTab by remember { mutableStateOf("events_tab") }

    val eventsViewModel: EventsViewModel = viewModel()
    val friendsViewModel: FriendsViewModel = viewModel()
    val optionsViewModel: OptionsViewModel = viewModel()
    val eventsUiState by eventsViewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            PagameBottomBar(
                currentRoute = currentTab,
                onTabSelected = { currentTab = it }
            )
        },
        floatingActionButton = {
            // Solo aparece en Eventos, y solo si ya hay eventos
            // (el estado vacío ya trae su propio botón centrado)
            if (currentTab == "events_tab" && eventsUiState.events.isNotEmpty()) {
                PagameExtendedFab(
                    text = "Crear evento",
                    icon = Icons.Default.Add,
                    onClick = { eventsViewModel.openCreateDialog() }
                )
            }
            // En "friends_tab" y "options_tab" no se dibuja nada aquí
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (currentTab) {
                "events_tab" -> EventsScreen(
                    viewModel = eventsViewModel,
                    onNavigateToDetail = onNavigateToEventDetail
                )
                "friends_tab" -> FriendsScreen(viewModel = friendsViewModel)
                "options_tab" -> OptionsScreen(
                    viewModel = optionsViewModel,
                    onLogoutSuccess = onLogoutSuccess
                )
            }
        }
    }
}
