package gt.pagame.app.data.model

enum class BalanceStatus {
    YOU_ARE_OWED,
    YOU_OWE,
    ALL_SETTLED,
    LIQUIDATED
}

data class UserProfile(
    val id: String = "user_mateo",
    val name: String = "Mateo González",
    val email: String = "mateo@universidad.edu.gt",
    val avatarInitial: String = "M",
    val friendCode: String = "PG-7489",
    val isVerified: Boolean = true
)

data class EventItem(
    val id: String,
    val title: String,
    val dateOrSubtitle: String,
    val participantCount: Int,
    val totalAmount: Double,
    val balanceStatus: BalanceStatus,
    val balanceAmount: Double = 0.0,
    val participantInitials: List<String> = emptyList(),
    val categoryIcon: String = "🍕",
    val isToday: Boolean = false
)

data class ExpenseItem(
    val id: String,
    val title: String,
    val paidByName: String,
    val timeFormatted: String,
    val amount: Double,
    val participantCount: Int = 6,
    val iconType: String = "pizza",
    val includedParticipantNames: List<String> = emptyList()
)

data class ParticipantBalance(
    val name: String,
    val initial: String,
    val statusText: String,
    val isOwed: Boolean?, // true = le deben, false = debe, null = al día
    val amountFormatted: String = ""
)

data class SuggestedPayment(
    val fromName: String,
    val fromInitial: String,
    val toName: String,
    val toInitial: String,
    val amountFormatted: String
)

data class FriendItem(
    val id: String,
    val name: String,
    val initial: String,
    val eventsTogetherCount: Int,
    val codeOrStatus: String,
    val isSettledBadge: Boolean = false
)

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class Currency(val symbol: String, val displayName: String) {
    GTQ("Q", "Quetzal (Q)"),
    USD("$", "Dólar ($ USD)"),
    EUR("€", "Euro (€)")
}
