package gt.pagame.app.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
    object EventDetail : Screen("event_detail/{eventId}") {
        fun createRoute(eventId: String) = "event_detail/$eventId"
    }
}

sealed class BottomTab(val route: String, val title: String) {
    object Events : BottomTab("events_tab", "Eventos")
    object Friends : BottomTab("friends_tab", "Amigos")
    object Options : BottomTab("options_tab", "Opciones")
}
