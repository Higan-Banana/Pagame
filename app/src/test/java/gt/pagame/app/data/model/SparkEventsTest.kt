package gt.pagame.app.data.model

import org.junit.Assert.*
import org.junit.Test

class SparkEventsTest {
    private val expense: Map<String, Any> = mapOf("expenseId" to "expense1", "title" to "Cena", "amountMinor" to 100L,
        "payerId" to "a", "includedIds" to listOf("a", "b", "c"))
    private fun empty() = SparkEvents.create("a", "Cena", listOf("b", "c"), "GTQ", 1000)
    private fun spent() = SparkEvents.mutate(empty(), "a", "addExpense", expense, 1000)!!
    private fun rejects(block: () -> Unit) {
        try { block(); fail("The operation should be rejected") } catch (_: PagameValidationException) { }
    }
    @Test fun splitUsesExactCentsAndTenLedgers() {
        val state = spent()
        assertEquals(mapOf("a" to 66L, "b" to -33L, "c" to -33L), SparkEvents.decode("e", state).balances())
        val ledgers = SparkEvents.ledgers(state)
        assertEquals(10, ledgers.size)
        assertEquals(0L, ledgers.last()["balancePrefix"])
        assertEquals("a", SparkEvents.records(state["expenses"]).getValue("expense1")["extraThrough"])
    }
    @Test fun successfulRetryDoesNotDuplicateExpense() {
        val state = spent()
        assertEquals(state, SparkEvents.mutate(state, "a", "addExpense", expense, 2000))
        rejects { SparkEvents.mutate(state, "a", "addExpense", expense + ("amountMinor" to 500L), 2000) }
    }
    @Test fun participantMustHaveNoExpenseOrPaymentBeforeRemoval() {
        val state = spent()
        rejects { SparkEvents.mutate(state, "a", "updateParticipants", mapOf("participantIds" to listOf("a", "b")), 1000) }
        val edited = SparkEvents.mutate(state, "a", "updateExpense", expense + ("includedIds" to listOf("a", "b")), 1000)!!
        val removed = SparkEvents.mutate(edited, "a", "updateParticipants", mapOf("participantIds" to listOf("a", "b")), 1000)!!
        assertEquals(listOf("a", "b"), SparkEvents.decode("e", removed).participantIds)
        assertEquals(30, SparkEvents.strings(removed["memberSlots"]).size)
    }
    @Test fun receiverMustConfirmBeforeBalanceChanges() {
        val state = spent()
        val report: Map<String, Any> = mapOf("paymentId" to "payment1", "fromId" to "b", "toId" to "a", "amountMinor" to 33L, "revision" to 1L)
        val pending = SparkEvents.mutate(state, "b", "reportPayment", report, 1000)!!
        assertEquals(SparkEvents.decode("e", state).balances(), SparkEvents.decode("e", pending).balances())
        rejects { SparkEvents.mutate(pending, "b", "confirmPayment", mapOf("paymentId" to "payment1"), 1000) }
        val confirmed = SparkEvents.mutate(pending, "a", "confirmPayment", mapOf("paymentId" to "payment1"), 1000)!!
        assertEquals(0L, SparkEvents.decode("e", confirmed).balances()["b"])
        assertNull(confirmed["pendingPayment"])
        assertEquals(1, SparkEvents.records(confirmed["payments"]).size)
    }
    @Test fun staleBalanceAndInvalidAmountsAreRejected() {
        rejects { SparkEvents.mutate(spent(), "b", "reportPayment", mapOf("paymentId" to "p", "fromId" to "b", "toId" to "a", "amountMinor" to 33L, "revision" to 0L), 1000) }
        for (amount in listOf(0L, -1L, 100000001L)) rejects { SparkEvents.mutate(empty(), "a", "addExpense", expense + ("amountMinor" to amount), 1000) }
    }
    @Test fun thirtyParticipantsKeepEveryCentAcrossShards() {
        val people = (0 until 30).map { "u" + it.toString().padStart(2, '0') }
        val start = SparkEvents.create(people[0], "Viaje", people, "USD", 1000)
        for (amount in 1L..100L) {
            val state = SparkEvents.mutate(start, people[0], "addExpense", expense + mapOf("amountMinor" to amount, "payerId" to people[0], "includedIds" to people), 1000)!!
            assertEquals(0L, SparkEvents.decode("e", state).balances().values.sum())
            assertEquals(0L, SparkEvents.ledgers(state).last()["balancePrefix"])
        }
    }
    @Test fun eventCurrencyPersistsWithoutServerCode() {
        assertEquals(Currency.GTQ, SparkEvents.decode("e", empty()).currency)
    }
    @Test fun onlyOwnerCanDeleteAnEmptyEvent() {
        rejects { SparkEvents.mutate(empty(), "b", "deleteEvent", emptyMap(), 1000) }
        rejects { SparkEvents.mutate(spent(), "a", "deleteEvent", emptyMap(), 1000) }
        assertNull(SparkEvents.mutate(empty(), "a", "deleteEvent", emptyMap(), 1000))
    }
    @Test fun legacyEmojiMetadataDoesNotBlockExpenseEdits() {
        val original = spent()
        val oldExpense = SparkEvents.records(original["expenses"]).getValue("expense1") + ("emoji" to "texto antiguo")
        val state = original + ("expenses" to mapOf("expense1" to oldExpense))
        val edited = SparkEvents.mutate(state, "a", "updateExpense", expense + ("title" to "Agua"), 1001)!!
        assertEquals("texto antiguo", SparkEvents.decode("e", edited).expenses.single().emoji)
        val legacyExpense = SparkEvents.records(edited["expenses"]).getValue("expense1") - "emoji"
        val legacy = edited + ("expenses" to mapOf("expense1" to legacyExpense))
        assertEquals("🧾", SparkEvents.decode("e", legacy).expenses.single().emoji)
    }

    @Test fun anyMemberMayAddAnExpenseAndItsCreatorIsPreserved() {
        val added = SparkEvents.mutate(empty(), "b", "addExpense", expense, 1000)!!
        val stored = SparkEvents.records(added["expenses"]).getValue("expense1")
        assertEquals("b", stored["createdBy"])
        assertEquals("a", stored["payerId"])
        assertFalse(stored.containsKey("emoji"))
        assertFalse(empty().containsKey("emoji"))
        val edited = SparkEvents.mutate(added, "a", "updateExpense", expense + ("title" to "Cena compartida"), 1001)!!
        assertEquals("b", SparkEvents.decode("e", edited).expenses.single().createdBy)
        rejects { SparkEvents.mutate(empty(), "outsider", "addExpense", expense, 1000) }
    }
    @Test fun confirmedReceiptClearsBothBalancesAndPendingSuggestions() {
        val start = SparkEvents.create("a", "Prueba", listOf("b"), "EUR", 1000)
        val spent = SparkEvents.mutate(start, "a", "addExpense", expense + mapOf(
            "amountMinor" to 2345L, "includedIds" to listOf("a", "b")), 1001)!!
        val pending = SparkEvents.mutate(spent, "b", "reportPayment", mapOf(
            "paymentId" to "p", "fromId" to "b", "toId" to "a", "amountMinor" to 1172L, "revision" to 1L), 1002)!!
        val confirmed = SparkEvents.mutate(pending, "a", "confirmPayment", mapOf("paymentId" to "p"), 1003)!!
        val event = SparkEvents.decode("e", confirmed)
        assertTrue(event.suggestions().isEmpty())
        assertEquals(mapOf("a" to 0L, "b" to 0L), event.balances())
        assertNull(confirmed["pendingPayment"])
        assertEquals("confirmed", event.payments.single().status)
        assertEquals(1, event.expenses.size)
    }


    @Test fun amountsRejectLettersSignsExponentsAndExcessPrecision() {
        assertEquals(100000000L, amountMinor("1000000.00"))
        assertEquals(1L, amountMinor("0.01"))
        assertEquals(2345L, amountMinor("23,45"))
        listOf("1000000.01", "10000000", "1e3", "-1", "+1", "1.234", "2a", "1,000.00", "0").forEach {
            assertNull(amountMinor(it))
        }
        assertEquals("12.34", normalizedAmountInput("12,34"))
        assertEquals("", normalizedAmountInput(""))
        assertEquals("1.", normalizedAmountInput("1."))
        assertNull(normalizedAmountInput("a"))
        assertEquals("", "🦦".takeText(1))
    }

    @Test fun settlementSurvivesRemovingAndRestoringExpenseParticipant() {
        val bill = expense + ("amountMinor" to 2345L) + ("includedIds" to listOf("a", "b"))
        var state = SparkEvents.create("a", "Cena", listOf("b"), "GTQ", 1000)
        state = SparkEvents.mutate(state, "a", "addExpense", bill, 1001)!!
        state = SparkEvents.mutate(state, "b", "reportPayment", mapOf("paymentId" to "p", "fromId" to "b",
            "toId" to "a", "amountMinor" to 1172L, "revision" to 1L), 1002)!!
        state = SparkEvents.mutate(state, "a", "updateExpense", bill + ("includedIds" to listOf("a")), 1003)!!
        assertNotNull(state["pendingPayment"])
        state = SparkEvents.mutate(state, "a", "updateExpense", bill, 1004)!!
        state = SparkEvents.mutate(state, "a", "confirmPayment", mapOf("paymentId" to "p"), 1005)!!
        state = SparkEvents.mutate(state, "a", "updateExpense", bill + ("includedIds" to listOf("a")), 1006)!!
        state = SparkEvents.mutate(state, "a", "updateExpense", bill, 1007)!!
        assertTrue(SparkEvents.decode("e", state).suggestions().isEmpty())
        assertEquals(1, SparkEvents.decode("e", state).payments.size)
    }
    @Test fun savingAnUnchangedExpensePreservesRevisionAndPaymentReport() {
        val state = spent()
        val pending = SparkEvents.mutate(state, "b", "reportPayment", mapOf("paymentId" to "p",
            "fromId" to "b", "toId" to "a", "amountMinor" to 33L, "revision" to 1L), 1001)!!
        assertEquals(pending, SparkEvents.mutate(pending, "a", "updateExpense", expense, 1002))
    }
}
