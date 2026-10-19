package gt.pagame.app.ui.navigation

import gt.pagame.app.ui.theme.*

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import gt.pagame.app.data.repository.*
import gt.pagame.app.ui.components.*
import gt.pagame.app.ui.screens.eventdetail.*
import gt.pagame.app.ui.screens.events.*
import gt.pagame.app.ui.screens.friends.*
import gt.pagame.app.ui.screens.login.*
import gt.pagame.app.ui.screens.options.*
import gt.pagame.app.ui.screens.register.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Composable
fun PagameNavGraph() {
    val repository = PagameRepository.instance
    val uid by repository.sessionUid.collectAsState()
    val people by repository.people.collectAsState()
    val user by repository.currentUser.collectAsState()
    val checked by repository.profileChecked.collectAsState()
    val registering by repository.registering.collectAsState()
    val loading by repository.loading.collectAsState()
    val error by repository.error.collectAsState()
    CompositionLocalProvider(LocalPeople provides people, LocalCurrentUid provides uid.orEmpty()) {
        key(uid) {
            val navController = rememberNavController()
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                if (uid != null && registering) {
                    ProfileLoading("Creando cuenta…")
                } else if (uid != null && user.id.isBlank()) {
                    ProfileSetup(loading = !checked, error = error)
                } else {
                    NavHost(navController = navController,
                        startDestination = if (uid == null) Screen.Login.route else Screen.Main.route,
                        enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None }, popExitTransition = { ExitTransition.None }) {
                        if (uid == null) {
                            composable(Screen.Login.route) {
                                LoginScreen(viewModel(), onNavigateToRegister = { navController.navigate(Screen.Register.route) }, onLoginSuccess = {})
                            }
                            composable(Screen.Register.route) {
                                RegisterScreen(viewModel(), onNavigateBack = { navController.popBackStack() },
                                    onNavigateToLogin = { navController.popBackStack() }, onRegisterSuccess = {})
                            }
                        } else {
                            composable(Screen.Main.route) {
                                MainContainer(onNavigateToEventDetail = { navController.navigate(Screen.EventDetail.createRoute(it)) }, onLogoutSuccess = {})
                            }
                            composable(Screen.EventDetail.route, arguments = listOf(navArgument("eventId") { type = NavType.StringType })) {
                                EventDetailScreen(viewModel(), onNavigateBack = { navController.popBackStack() })
                            }
                        }
                    }
                    if (uid != null && loading) LinearProgressIndicator(Modifier.fillMaxWidth().statusBarsPadding())
                    if (uid != null && error != null) AlertDialog(
                        onDismissRequest = {}, title = { Text("No se pudo sincronizar") }, text = { Text(error.orEmpty()) },
                        confirmButton = { TextButton(onClick = repository::retry) { Text("Reintentar") } },
                        dismissButton = { TextButton(onClick = repository::signOut) { Text("Cerrar sesión") } })
                }
            }
        }
    }
}

@Composable
private fun ProfileLoading(message: String) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(Spacing.page),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(Spacing.medium))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileSetup(loading: Boolean, error: String?) {
    if (loading && error == null) {
        ProfileLoading("Cargando perfil…")
        return
    }
    val repository = PagameRepository.instance
    var name by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(Spacing.page),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Completa tu perfil", style = MaterialTheme.typography.headlineSmall)
        Text("Tu cuenta está creada. Guarda tu nombre para comenzar.")
        OutlinedTextField(name, { name = it.take(60) }, label = { Text("Nombre completo") }, singleLine = true)
        (message ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = name.isNotBlank() && !busy, onClick = {
            busy = true; message = null
            scope.launch {
                try { repository.initProfile(name); repository.retry() }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { message = userMessage(e) }
                finally { busy = false }
            }
        }) { Text(if (busy) "Guardando…" else "Guardar perfil") }
        TextButton(onClick = repository::signOut) { Text("Cerrar sesión") }
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
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            PagameTopAppBar(
                title = when (currentTab) {
                    "friends_tab" -> "Amigos"
                    "options_tab" -> "Opciones"
                    else -> "Eventos"
                },
                subtitle = when (currentTab) {
                    "friends_tab" -> "Comparte gastos con las personas de tu círculo"
                    "options_tab" -> "Perfil, preferencias y cuenta"
                    else -> "Divide cuentas fácilmente con tu círculo"
                },
                actions = {
                    if (currentTab == "friends_tab") {
                        IconButton(onClick = friendsViewModel::openAddFriendDialog) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Agregar amigo",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        },
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
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
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
