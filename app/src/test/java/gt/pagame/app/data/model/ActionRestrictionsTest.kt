package gt.pagame.app.data.model

import org.junit.Assert.*
import org.junit.Test

class ActionRestrictionsTest {
    private fun event() = SparkEvents.decode("e", SparkEvents.create("a", "Cena", listOf("a", "b"), "GTQ", 1000))

    @Test fun emptyAndValidEventNamesHaveDifferentOutcomes() {
        assertNotNull(ActionRestrictions.eventTitle("  "))
        assertNotNull(ActionRestrictions.eventTitle("a".repeat(101)))
        assertNull(ActionRestrictions.eventTitle("Cena"))
    }

    @Test fun expenseCannotBeSavedWithoutParticipantsOrWithAnInvalidAmount() {
        for (amount in listOf("", "0", "-1", "1.001", "100000.01"))
            assertNotNull(ActionRestrictions.expense("Cena", amount, 1))
        assertTrue(ActionRestrictions.expense("Cena", "10", 0)!!.contains("participante"))
        assertNull(ActionRestrictions.expense("Cena", "10.50", 1))
    }

    @Test fun rejectedInputStillBlocksSavingThePreviousValidAmount() {
        assertNotNull(ActionRestrictions.expense("Cena", "10", 2, "Solo números"))
    }

    @Test fun onlyOwnerCanDeleteAnEmptyEvent() {
        assertNull(ActionRestrictions.deleteEvent(event(), "a"))
        assertNotNull(ActionRestrictions.deleteEvent(event(), "b"))
        assertNotNull(ActionRestrictions.deleteEvent(null, "a"))
    }

    @Test fun pendingAndConfirmedPaymentsBothProtectHistoryWithDifferentReasons() {
        val pending = event().copy(payments = listOf(Payment("p", "b", "a", 100, "pending")))
        val confirmed = pending.copy(payments = pending.payments.map { it.copy(status = "confirmed") })
        assertTrue(ActionRestrictions.deleteEvent(pending, "a")!!.contains("pendiente"))
        assertTrue(ActionRestrictions.deleteEvent(confirmed, "a")!!.contains("historial"))
        assertTrue(ActionRestrictions.removeParticipant(pending, "b")!!.contains("pendiente"))
        assertTrue(ActionRestrictions.removeParticipant(confirmed, "b")!!.contains("historial"))
    }

    @Test fun participantWithoutDebtCanLeaveButExpenseAssociationStillBlocksRemoval() {
        assertNull(ActionRestrictions.removeParticipant(event(), "b"))
        assertNotNull(ActionRestrictions.removeParticipant(event(), "a"))
        val spent = event().copy(expenses = listOf(StoredExpense("x", "Cena", 100, "a", listOf("a", "b"), "a", 1000)))
        assertTrue(ActionRestrictions.removeParticipant(spent, "b")!!.contains("gastos"))
    }

    @Test fun registrationExplainsTheSpecificMissingRequirement() {
        assertTrue(ActionRestrictions.registration("", "a@b.gt", "123456", "123456")!!.contains("nombre"))
        assertTrue(ActionRestrictions.registration("Ana", "", "123456", "123456")!!.contains("correo"))
        assertTrue(ActionRestrictions.registration("Ana", "a@b.gt", "123", "123")!!.contains("6 caracteres"))
        assertTrue(ActionRestrictions.registration("Ana", "a@b.gt", "123456", "654321")!!.contains("coinciden"))
        assertNull(ActionRestrictions.registration("Ana", "a@b.gt", "123456", "123456"))
    }
}
