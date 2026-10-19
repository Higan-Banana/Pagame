package gt.pagame.app.data.model

import org.junit.Assert.*
import org.junit.Test

class OnlineEventTest {
    @Test fun accountingMatchesIndependentReferenceAtEventLimits() {
        val people = (0 until 30).map { "u" + it.toString().padStart(2, '0') }
        val random = kotlin.random.Random(731)
        val expenses = (0 until 500).map { index ->
            StoredExpense("e$index", "Gasto", random.nextLong(1, 100000001), people.random(random),
                people.shuffled(random).take(random.nextInt(1, 31)), people[0], index.toLong())
        }
        val payments = (0 until 500).map { index ->
            val pair = people.shuffled(random).take(2)
            Payment("p$index", pair[0], pair[1], random.nextLong(1, 100000001),
                if (index % 2 == 0) "confirmed" else "pending")
        }
        val snapshot = OnlineEvent("e", "Viaje", people[0], people, Currency.GTQ, "📅", expenses, payments, 1, 0)
        val expected = people.associateWith { uid ->
            expenses.sumOf { expense ->
                val included = expense.includedIds.sorted()
                val index = included.indexOf(uid)
                val debit = if (index < 0) 0L else expense.amountMinor / included.size +
                    if (index < expense.amountMinor % included.size) 1L else 0L
                (if (expense.payerId == uid) expense.amountMinor else 0L) - debit
            } + payments.filter { it.status == "confirmed" }.sumOf { payment ->
                when (uid) { payment.fromId -> payment.amountMinor; payment.toId -> -payment.amountMinor; else -> 0L }
            }
        }
        assertEquals(expected, snapshot.balances())
        assertEquals(0L, snapshot.balances().values.sum())
        assertEquals(expenses.sumOf { it.amountMinor }, snapshot.totalMinor)
        people.forEach { uid ->
            assertEquals(expenses.count { it.payerId == uid || uid in it.includedIds }, snapshot.expenseCounts()[uid])
            assertEquals(payments.count { it.fromId == uid || it.toId == uid }, snapshot.paymentCounts()[uid])
        }
    }

    @Test fun accountingCountsPayerOnlyOnceAndRecalculatesForNewSnapshots() {
        val original = event()
        assertEquals(mapOf("a" to 1, "b" to 1, "c" to 1), original.expenseCounts())
        val pending = Payment("p", "b", "a", 33, "pending")
        val updated = original.copy(payments = listOf(pending))
        assertEquals(mapOf("a" to 1, "b" to 1, "c" to 0), updated.paymentCounts())
        assertEquals(original.balances(), updated.balances())
        assertEquals(0L, updated.copy(payments = listOf(pending.copy(status = "confirmed"))).balances()["b"])
        assertEquals(-33L, original.balances()["b"])
        assertEquals(0L, original.copy(expenses = emptyList()).totalMinor)
    }

    private fun event(payments: List<Payment> = emptyList()) = OnlineEvent(
        "event", "Cena", "a", listOf("a", "b", "c"), Currency.GTQ, "🍽️",
        listOf(StoredExpense("expense", "Cena", 100, "a", listOf("a", "b", "c"), "a", 0)),
        payments, 0, 0
    )
    @Test fun centsAndSuggestionsMatchServer() {
        assertEquals(mapOf("a" to 66L, "b" to -33L, "c" to -33L), event().balances())
        assertEquals(listOf(Settlement("b", "a", 33), Settlement("c", "a", 33)), event().suggestions())
    }
    @Test fun onlyConfirmedPaymentsAffectBalances() {
        val pending = Payment("payment", "b", "a", 33, "pending")
        assertEquals(event().balances(), event(listOf(pending)).balances())
        assertEquals(0L, event(listOf(pending.copy(status = "confirmed"))).balances()["b"])
    }
    @Test fun inputAmountsAreExact() {
        assertEquals(450000L, amountMinor("4500"))
        assertEquals(123L, amountMinor("1.23"))
        for (invalid in listOf("", "0", "-1", "0.001", "NaN", "1000001")) assertNull(amountMinor(invalid))
        assertEquals("Q4,500.00", money(450000, Currency.GTQ))
    }
}
