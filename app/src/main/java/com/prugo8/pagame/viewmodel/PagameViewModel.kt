package com.prugo8.pagame.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prugo8.pagame.model.*
import com.prugo8.pagame.util.DebtAlgorithm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PagameViewModel : ViewModel() {

    private val defaultUser = Participant("user", "Tú", isUser = true)

    private val initialEvents = listOf(
        Event(
            id = "pizza-birra",
            name = "Pizza & Birra",
            date = "Hoy, 21:30",
            currency = "Q",
            status = EventStatus.ACTIVE,
            participants = listOf(
                defaultUser,
                Participant("p1", "Vale"),
                Participant("p2", "Marcos"),
                Participant("p3", "Lu"),
                Participant("p4", "Facu"),
                Participant("p5", "Cami")
            ),
            expenses = listOf(
                Expense("e1", "Pizzas Variadas", 4500.0, "user", listOf("user", "p1", "p2", "p3", "p4", "p5"), "09:45 PM", "pizza"),
                Expense("e2", "Cervezas Artesanales", 5800.0, "p1", listOf("user", "p1", "p2", "p3", "p4", "p5"), "10:12 PM", "drinks"),
                Expense("e3", "Helado & Postre", 2100.0, "p2", listOf("user", "p1", "p2", "p3", "p4", "p5"), "11:05 PM", "dessert")
            )
        ),
        Event(
            id = "asado-sabado",
            name = "Asado del Sábado",
            date = "23 de Octubre",
            currency = "Q",
            status = EventStatus.ACTIVE,
            participants = listOf(
                defaultUser,
                Participant("p1", "Nico"),
                Participant("p2", "Mati")
            ),
            expenses = listOf(
                Expense("e4", "Carne y Carbón", 45600.0, "user", listOf("user", "p1", "p2"), "01:30 PM", "bbq")
            )
        )
    )

    private val _events = MutableStateFlow<List<Event>>(initialEvents)
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _activeEventId = MutableStateFlow<String>("pizza-birra")
    val activeEventId: StateFlow<String> = _activeEventId.asStateFlow()

    fun selectEvent(eventId: String) {
        _activeEventId.value = eventId
    }

    fun getActiveEvent(): Event? {
        return _events.value.find { it.id == _activeEventId.value }
    }

    fun createEvent(name: String, participantNames: List<String>): String {
        val newId = "evt_${System.currentTimeMillis()}"
        val participants = mutableListOf(defaultUser)
        participantNames.forEachIndexed { index, pName ->
            if (pName.isNotBlank()) {
                participants.add(Participant("p_${System.currentTimeMillis()}_$index", pName.trim()))
            }
        }

        val newEvent = Event(
            id = newId,
            name = name.ifBlank { "Nuevo Evento" },
            date = "Hoy, recién creado",
            currency = "Q",
            status = EventStatus.ACTIVE,
            participants = participants,
            expenses = emptyList()
        )

        _events.update { listOf(newEvent) + it }
        _activeEventId.value = newId
        return newId
    }

    fun addExpense(
        eventId: String,
        title: String,
        amount: Double,
        payerId: String,
        splitIds: List<String>,
        category: String
    ) {
        val newExpense = Expense(
            id = "exp_${System.currentTimeMillis()}",
            title = title,
            amount = amount,
            payerId = payerId,
            splitParticipantIds = splitIds,
            time = "Ahora",
            category = category
        )

        _events.update { currentList ->
            currentList.map { event ->
                if (event.id == eventId) {
                    event.copy(expenses = event.expenses + newExpense)
                } else {
                    event
                }
            }
        }
    }

    fun deleteExpense(eventId: String, expenseId: String) {
        _events.update { currentList ->
            currentList.map { event ->
                if (event.id == eventId) {
                    event.copy(expenses = event.expenses.filterNot { it.id == expenseId })
                } else {
                    event
                }
            }
        }
    }

    fun settleEvent(eventId: String) {
        _events.update { currentList ->
            currentList.map { event ->
                if (event.id == eventId) {
                    event.copy(status = EventStatus.SETTLED)
                } else {
                    event
                }
            }
        }
    }

    fun getBalances(eventId: String): List<ParticipantBalance> {
        val event = _events.value.find { it.id == eventId } ?: return emptyList()
        return DebtAlgorithm.calculateBalances(event)
    }

    fun getSettlements(eventId: String): List<Settlement> {
        val event = _events.value.find { it.id == eventId } ?: return emptyList()
        return DebtAlgorithm.calculateSettlements(event)
    }
}
