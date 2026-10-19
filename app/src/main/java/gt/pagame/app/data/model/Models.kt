package gt.pagame.app.data.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

enum class BalanceStatus { YOU_ARE_OWED, YOU_OWE, ALL_SETTLED, LIQUIDATED }
enum class AppThemeMode { LIGHT, DARK, SYSTEM }
enum class Currency(val symbol: String, val displayName: String) {
    GTQ("Q", "Quetzal (Q)"), USD("$", "Dólar ($ USD)"), EUR("€", "Euro (€)")
}
data class UserProfile(
    val id: String = "", val name: String = "", val email: String = "",
    val avatarInitial: String = "", val friendCode: String = "",
    val isEmailVerified: Boolean = false, val colorIndex: Int = 0
)
data class FriendItem(
    val id: String, val name: String, val initial: String, val eventsTogetherCount: Int = 0,
    val codeOrStatus: String = "", val isSettledBadge: Boolean = false, val colorIndex: Int = 0
)
data class EventItem(
    val id: String, val title: String, val createdAtLabel: String,
    val participantCount: Int, val totalAmount: Double, val balanceStatus: BalanceStatus,
    val balanceAmount: Double = 0.0, val participantInitials: List<String> = emptyList(),
    val emoji: String = "📅", val isToday: Boolean = false,
    val currency: Currency = Currency.GTQ, val participantIds: List<String> = emptyList(),
    val createdByName: String = ""
)
data class ExpenseItem(
    val id: String, val title: String, val paidByName: String, val createdAtLabel: String,
    val amount: Double, val participantCount: Int = 0, val iconType: String = "generic",
    val includedParticipantNames: List<String> = emptyList(), val payerId: String = "",
    val includedIds: List<String> = emptyList(), val createdBy: String = "", val canEdit: Boolean = false,
    val emoji: String = "", val createdByName: String = ""
)
data class ParticipantBalance(
    val name: String, val initial: String, val statusText: String = "Al día",
    val isOwed: Boolean? = null, val amountFormatted: String = "", val id: String = ""
)
data class StoredExpense(
    val id: String, val title: String, val amountMinor: Long, val payerId: String,
    val includedIds: List<String>, val createdBy: String, val createdAt: Long, val emoji: String = "🧾"
)
data class Payment(
    val id: String, val fromId: String, val toId: String, val amountMinor: Long,
    val status: String, val createdAt: Long = 0
)
data class Settlement(val fromId: String, val toId: String, val amountMinor: Long)
data class OnlineEvent(
    val id: String, val title: String, val ownerId: String, val participantIds: List<String>,
    val currency: Currency, val emoji: String, val expenses: List<StoredExpense>,
    val payments: List<Payment>, val revision: Long, val createdAt: Long
) {
    // Event snapshots are replaced, never edited in place, when Firestore changes.
    private val accounting by lazy { EventAccounting(this) }
    val totalMinor: Long get() = accounting.totalMinor
    fun expenseCounts(): Map<String, Int> = accounting.expenseCounts.toMap()
    fun paymentCounts(): Map<String, Int> = accounting.paymentCounts.toMap()
    fun balances(): Map<String, Long> {
        return accounting.balances.toMap()
    }
    fun suggestions(): List<Settlement> {
        val balances = balances().toSortedMap()
        val debtors = balances.filterValues { it < 0 }.map { it.key to -it.value }.toMutableList()
        val creditors = balances.filterValues { it > 0 }.toList().toMutableList()
        val result = mutableListOf<Settlement>()
        var a = 0; var b = 0
        while (a < debtors.size && b < creditors.size) {
            val amount = minOf(debtors[a].second, creditors[b].second)
            result += Settlement(debtors[a].first, creditors[b].first, amount)
            debtors[a] = debtors[a].copy(second = debtors[a].second - amount)
            creditors[b] = creditors[b].copy(second = creditors[b].second - amount)
            if (debtors[a].second == 0L) a++
            if (creditors[b].second == 0L) b++
        }
        return result
    }
}
fun money(minor: Long, currency: Currency): String =
    currency.symbol + NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }.format(BigDecimal.valueOf(minor, 2))
fun amountMinor(input: String): Long? = try {
    val normalized = normalizedAmountInput(input)
    if (normalized == null || !Regex("[0-9]+(\\.[0-9]{1,2})?").matches(normalized)) null else
    BigDecimal(normalized).setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()
        .takeIf { it in 1..100000000L }
} catch (_: Exception) { null }
fun initials(name: String): String = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.map { it.substring(0, it.offsetByCodePoints(0, 1)) }
    .take(2).joinToString("").uppercase()

/** One letter for event/expense badges, without showing old emoji metadata. */
fun titleInitial(title: String): String {
    val points = title.codePoints().toArray()
    val first = points.firstOrNull { Character.isLetter(it) }
        ?: points.firstOrNull { Character.isDigit(it) } ?: return "?"
    return String(Character.toChars(Character.toUpperCase(first)))
}
