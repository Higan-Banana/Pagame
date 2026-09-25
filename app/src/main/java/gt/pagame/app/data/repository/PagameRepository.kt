package gt.pagame.app.data.repository

import gt.pagame.app.data.model.AppThemeMode
import gt.pagame.app.data.model.BalanceStatus
import gt.pagame.app.data.model.Currency
import gt.pagame.app.data.model.EventItem
import gt.pagame.app.data.model.ExpenseItem
import gt.pagame.app.data.model.FriendItem
import gt.pagame.app.data.model.ParticipantBalance
import gt.pagame.app.data.model.SuggestedPayment
import gt.pagame.app.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PagameRepository private constructor() {

    private val _currentUser = MutableStateFlow(UserProfile())
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _currency = MutableStateFlow(Currency.GTQ)
    val currency: StateFlow<Currency> = _currency.asStateFlow()

    private val _theme = MutableStateFlow(AppThemeMode.LIGHT)
    val theme: StateFlow<AppThemeMode> = _theme.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val initialEvents = listOf(
        EventItem(
            id = "event_pizza",
            title = "Pizza & Birra",
            dateOrSubtitle = "Hoy, 21:30",
            participantCount = 6,
            totalAmount = 12400.0,
            balanceStatus = BalanceStatus.YOU_ARE_OWED,
            balanceAmount = 320.0,
            participantInitials = listOf("MG", "SM", "VC", "+3"),
            categoryIcon = "🍕",
            isToday = true
        ),
        EventItem(
            id = "event_antigua",
            title = "Viaje a Antigua",
            dateOrSubtitle = "Fin de semana pasado",
            participantCount = 4,
            totalAmount = 3850.0,
            balanceStatus = BalanceStatus.YOU_OWE,
            balanceAmount = 180.0,
            participantInitials = listOf("SR", "PM", "MG"),
            categoryIcon = "🚗"
        ),
        EventItem(
            id = "event_almuerzo",
            title = "Almuerzo Universidad",
            dateOrSubtitle = "14 Oct",
            participantCount = 3,
            totalAmount = 420.0,
            balanceStatus = BalanceStatus.ALL_SETTLED,
            balanceAmount = 0.0,
            participantInitials = listOf("VC", "MG"),
            categoryIcon = "🍔"
        ),
        EventItem(
            id = "event_tacos",
            title = "Noche de Tacos & Juegos",
            dateOrSubtitle = "28 Sep",
            participantCount = 5,
            totalAmount = 1150.0,
            balanceStatus = BalanceStatus.LIQUIDATED,
            balanceAmount = 0.0,
            participantInitials = listOf("SM", "SR", "+2"),
            categoryIcon = "🌮"
        )
    )

    private val _events = MutableStateFlow<List<EventItem>>(initialEvents)
    val events: StateFlow<List<EventItem>> = _events.asStateFlow()

    private val initialExpenses = listOf(
        ExpenseItem(
            id = "exp_pizza",
            title = "Pizzas napolitanas",
            paidByName = "Santi Morales",
            timeFormatted = "21:45 PM",
            amount = 4500.0,
            participantCount = 5,
            iconType = "pizza",
            includedParticipantNames = listOf(
                "Tú",
                "Santi Morales",
                "Vale Castillo",
                "Sofía Reyes",
                "Mario Gómez"
            )
        ),
        ExpenseItem(
            id = "exp_cerveza",
            title = "Cervezas y bebidas",
            paidByName = "Vale Castillo",
            timeFormatted = "22:15 PM",
            amount = 7900.0,
            participantCount = 5,
            iconType = "beer",
            includedParticipantNames = listOf(
                "Tú",
                "Santi Morales",
                "Vale Castillo",
                "Sofía Reyes",
                "Mario Gómez"
            )
        )
    )

    private val _pizzaExpenses = MutableStateFlow<List<ExpenseItem>>(initialExpenses)
    val pizzaExpenses: StateFlow<List<ExpenseItem>> = _pizzaExpenses.asStateFlow()

    val pizzaParticipants: List<ParticipantBalance>
        get() = _friends.value
            .filter { it.id in _pizzaParticipantIds.value }
            .map { friend ->
                when (friend.id) {
                    "f_santi" -> ParticipantBalance(
                        name = friend.name,
                        initial = friend.initial,
                        statusText = "Le deben Q1.500",
                        isOwed = true,
                        amountFormatted = "Q1.500"
                    )
                    "f_vale" -> ParticipantBalance(
                        name = friend.name,
                        initial = friend.initial,
                        statusText = "Debe Q1.500",
                        isOwed = false,
                        amountFormatted = "Q1.500"
                    )
                    else -> ParticipantBalance(
                        name = friend.name,
                        initial = friend.initial,
                        statusText = "Al día",
                        isOwed = null
                    )
                }
            }

    val pizzaSuggestedPayment = SuggestedPayment(
        fromName = "Lu",
        fromInitial = "L",
        toName = "Santi",
        toInitial = "S",
        amountFormatted = "Q1.500"
    )

    private val initialFriends = listOf(
        FriendItem(
            id = "f_santi",
            name = "Santi Morales",
            initial = "S",
            eventsTogetherCount = 3,
            codeOrStatus = "Al día",
            isSettledBadge = true
        ),
        FriendItem(
            id = "f_vale",
            name = "Vale Castillo",
            initial = "V",
            eventsTogetherCount = 2,
            codeOrStatus = "PG-3821",
            isSettledBadge = false
        ),
        FriendItem(
            id = "f_sofia",
            name = "Sofía Reyes",
            initial = "S",
            eventsTogetherCount = 4,
            codeOrStatus = "PG-9014",
            isSettledBadge = false
        ),
        FriendItem(
            id = "f_pedro",
            name = "Pedro Méndez",
            initial = "P",
            eventsTogetherCount = 1,
            codeOrStatus = "PG-4180",
            isSettledBadge = false
        ),
        FriendItem(
            id = "f_mario",
            name = "Mario Gómez",
            initial = "M",
            eventsTogetherCount = 2,
            codeOrStatus = "PG-5312",
            isSettledBadge = false
        ),
        FriendItem(
            id = "f_carlos",
            name = "Carlos Mendoza",
            initial = "C",
            eventsTogetherCount = 1,
            codeOrStatus = "PG-1024",
            isSettledBadge = false
        ),
        FriendItem(
            id = "f_diana",
            name = "Diana Rivas",
            initial = "D",
            eventsTogetherCount = 1,
            codeOrStatus = "PG-2940",
            isSettledBadge = false
        ),
        FriendItem(
            id = "f_andrea",
            name = "Andrea Fuentes",
            initial = "A",
            eventsTogetherCount = 0,
            codeOrStatus = "PG-6681",
            isSettledBadge = false
        )
    )

    private val _friends = MutableStateFlow<List<FriendItem>>(initialFriends)
    val friends: StateFlow<List<FriendItem>> = _friends.asStateFlow()

    private val _pizzaParticipantIds = MutableStateFlow(
        setOf("f_santi", "f_vale", "f_sofia", "f_pedro", "f_mario")
    )
    val pizzaParticipantIds: StateFlow<Set<String>> = _pizzaParticipantIds.asStateFlow()

    fun updatePizzaParticipants(participantIds: Set<String>) {
        _pizzaParticipantIds.value = participantIds
        val selectedFriends = _friends.value.filter { it.id in participantIds }
        val initials = listOf(_currentUser.value.avatarInitial) + selectedFriends.map { it.initial }
        val displayInitials = if (initials.size > 3) {
            initials.take(3) + "+${initials.size - 3}"
        } else {
            initials
        }

        _events.update { events ->
            events.map { event ->
                if (event.id == "event_pizza") {
                    event.copy(
                        participantCount = participantIds.size + 1,
                        participantInitials = displayInitials
                    )
                } else {
                    event
                }
            }
        }
    }

    fun addEvent(title: String, participantIds: List<String>) {
        val selectedFriends = _friends.value.filter { it.id in participantIds }
        val initials = (listOf(_currentUser.value.avatarInitial) + selectedFriends.map { it.initial })
        val displayInitials = if (initials.size > 3) {
            initials.take(3) + "+${initials.size - 3}"
        } else initials
        val newEvent = EventItem(
            id = "event_${System.currentTimeMillis()}",
            title = title,
            dateOrSubtitle = "Hoy, recién creado",
            participantCount = selectedFriends.size + 1,
            totalAmount = 0.0,
            balanceStatus = BalanceStatus.ALL_SETTLED,
            balanceAmount = 0.0,
            participantInitials = displayInitials,
            categoryIcon = "🎉",
            isToday = true
        )
        _events.update { listOf(newEvent) + it }
    }

    fun deleteEvent(eventId: String) {
        _events.update { events -> events.filterNot { it.id == eventId } }
    }

    fun addExpense(title: String, amount: Double, paidBy: String, includedNames: List<String>) {
        val newExp = ExpenseItem(
            id = "exp_${System.currentTimeMillis()}",
            title = title,
            paidByName = paidBy,
            timeFormatted = "Ahora",
            amount = amount,
            participantCount = includedNames.size,
            iconType = "generic",
            includedParticipantNames = includedNames
        )
        _pizzaExpenses.update { it + newExp }
    }

    fun updateExpense(expenseId: String, title: String, amount: Double, paidBy: String, includedNames: List<String>) {
        _pizzaExpenses.update { list ->
            list.map {
                if (it.id == expenseId) {
                    it.copy(
                        title = title,
                        amount = amount,
                        paidByName = paidBy,
                        participantCount = includedNames.size,
                        includedParticipantNames = includedNames
                    )
                } else it
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        _pizzaExpenses.update { list -> list.filterNot { it.id == expenseId } }
    }

    fun addFriend(name: String, code: String) {
        val initial = name.firstOrNull()?.uppercase() ?: "A"
        val newFriend = FriendItem(
            id = "f_${System.currentTimeMillis()}",
            name = name,
            initial = initial,
            eventsTogetherCount = 0,
            codeOrStatus = code.ifBlank { "PG-${(1000..9999).random()}" },
            isSettledBadge = false
        )
        _friends.update { it + newFriend }
    }

    fun settlePizzaEvent() {
        _events.update { list ->
            list.map {
                if (it.id == "event_pizza") {
                    it.copy(balanceStatus = BalanceStatus.LIQUIDATED, balanceAmount = 0.0)
                } else it
            }
        }
    }

    fun clearEventsForEmptyState() {
        _events.value = emptyList()
    }

    fun restoreEvents() {
        _events.value = initialEvents
    }

    fun clearFriendsForEmptyState() {
        _friends.value = emptyList()
    }

    fun restoreFriends() {
        _friends.value = initialFriends
    }

    fun cycleCurrency() {
        val next = when (_currency.value) {
            Currency.GTQ -> Currency.USD
            Currency.USD -> Currency.EUR
            Currency.EUR -> Currency.GTQ
        }
        _currency.value = next
    }

    fun cycleTheme() {
        val next = when (_theme.value) {
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.SYSTEM
            AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
        }
        _theme.value = next
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    companion object {
        val instance by lazy { PagameRepository() }
    }
}
