package com.prugo8.pagame.model

data class Participant(
    val id: String,
    val name: String,
    val isUser: Boolean = false,
    val avatarInitials: String = name.take(2).uppercase()
)

data class Expense(
    val id: String,
    val title: String,
    val amount: Double,
    val payerId: String,
    val splitParticipantIds: List<String>,
    val time: String,
    val category: String = "general"
)

data class Event(
    val id: String,
    val name: String,
    val date: String,
    val currency: String = "Q",
    val status: EventStatus = EventStatus.ACTIVE,
    val participants: List<Participant>,
    val expenses: List<Expense>
) {
    val totalSpent: Double
        get() = expenses.sumOf { it.amount }
}

enum class EventStatus {
    ACTIVE,
    SETTLED
}

data class Settlement(
    val fromParticipantId: String,
    val fromParticipantName: String,
    val toParticipantId: String,
    val toParticipantName: String,
    val amount: Double
)

data class ParticipantBalance(
    val participant: Participant,
    val netBalance: Double, // Positivo: le deben; Negativo: debe
    val totalPaid: Double,
    val totalOwed: Double
)
