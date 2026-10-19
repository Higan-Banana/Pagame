package gt.pagame.app.data.model

/** Derived values for one event snapshot. A new event revision gets a new calculation. */
internal class EventAccounting(event: OnlineEvent) {
    val totalMinor: Long
    val balances: Map<String, Long>
    val expenseCounts: Map<String, Int>
    val paymentCounts: Map<String, Int>

    init {
        var total = 0L
        val balances = event.participantIds.associateWith { 0L }.toMutableMap()
        val expenseCounts = event.participantIds.associateWith { 0 }.toMutableMap()
        val paymentCounts = event.participantIds.associateWith { 0 }.toMutableMap()
        event.expenses.forEach { expense ->
            total += expense.amountMinor
            balances[expense.payerId] = balances.getValue(expense.payerId) + expense.amountMinor
            val included = expense.includedIds.sorted()
            if (included.isNotEmpty()) {
                val base = expense.amountMinor / included.size
                val remainder = expense.amountMinor % included.size
                included.forEachIndexed { index, uid ->
                    balances[uid] = balances.getValue(uid) - base - if (index < remainder) 1L else 0L
                }
            }
            (expense.includedIds + expense.payerId).toSet().forEach { uid ->
                expenseCounts[uid] = expenseCounts.getValue(uid) + 1
            }
        }
        event.payments.forEach { payment ->
            setOf(payment.fromId, payment.toId).forEach { uid ->
                paymentCounts[uid] = paymentCounts.getValue(uid) + 1
            }
            if (payment.status == "confirmed") {
                balances[payment.fromId] = balances.getValue(payment.fromId) + payment.amountMinor
                balances[payment.toId] = balances.getValue(payment.toId) - payment.amountMinor
            }
        }
        this.totalMinor = total
        this.balances = balances
        this.expenseCounts = expenseCounts
        this.paymentCounts = paymentCounts
    }
}
