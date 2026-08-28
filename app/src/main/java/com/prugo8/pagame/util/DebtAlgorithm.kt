package com.prugo8.pagame.util

import com.prugo8.pagame.model.Event
import com.prugo8.pagame.model.Participant
import com.prugo8.pagame.model.ParticipantBalance
import com.prugo8.pagame.model.Settlement
import kotlin.math.abs
import kotlin.math.min

object DebtAlgorithm {

    /**
     * Calcula los balances netos individuales de cada participante
     */
    fun calculateBalances(event: Event): List<ParticipantBalance> {
        val netMap = mutableMapOf<String, Double>()
        val paidMap = mutableMapOf<String, Double>()
        val owedMap = mutableMapOf<String, Double>()

        event.participants.forEach { p ->
            netMap[p.id] = 0.0
            paidMap[p.id] = 0.0
            owedMap[p.id] = 0.0
        }

        event.expenses.forEach { expense ->
            val payerId = expense.payerId
            val splitIds = if (expense.splitParticipantIds.isNotEmpty()) {
                expense.splitParticipantIds
            } else {
                event.participants.map { it.id }
            }

            if (splitIds.isNotEmpty()) {
                val perPerson = expense.amount / splitIds.size

                // Sumar al pagador
                paidMap[payerId] = (paidMap[payerId] ?: 0.0) + expense.amount
                netMap[payerId] = (netMap[payerId] ?: 0.0) + expense.amount

                // Restar a cada consumidor
                splitIds.forEach { consumerId ->
                    owedMap[consumerId] = (owedMap[consumerId] ?: 0.0) + perPerson
                    netMap[consumerId] = (netMap[consumerId] ?: 0.0) - perPerson
                }
            }
        }

        return event.participants.map { p ->
            ParticipantBalance(
                participant = p,
                netBalance = netMap[p.id] ?: 0.0,
                totalPaid = paidMap[p.id] ?: 0.0,
                totalOwed = owedMap[p.id] ?: 0.0
            )
        }
    }

    /**
     * Minimiza las transacciones requeridas para saldar todas las deudas (Greedy Settlement)
     */
    fun calculateSettlements(event: Event): List<Settlement> {
        val balances = calculateBalances(event)
        
        // Deudores (deben dinero, saldo negativo)
        val debtors = balances
            .filter { it.netBalance < -0.01 }
            .map { it.participant to abs(it.netBalance) }
            .toMutableList()

        // Acreedores (les deben dinero, saldo positivo)
        val creditors = balances
            .filter { it.netBalance > 0.01 }
            .map { it.participant to it.netBalance }
            .toMutableList()

        val settlements = mutableListOf<Settlement>()

        var dIdx = 0
        var cIdx = 0

        while (dIdx < debtors.size && cIdx < creditors.size) {
            val (debtor, debtAmount) = debtors[dIdx]
            val (creditor, creditAmount) = creditors[cIdx]

            val settleAmount = min(debtAmount, creditAmount)

            if (settleAmount > 0.01) {
                settlements.add(
                    Settlement(
                        fromParticipantId = debtor.id,
                        fromParticipantName = debtor.name,
                        toParticipantId = creditor.id,
                        toParticipantName = creditor.name,
                        amount = settleAmount
                    )
                )
            }

            debtors[dIdx] = debtor to (debtAmount - settleAmount)
            creditors[cIdx] = creditor to (creditAmount - settleAmount)

            if (debtors[dIdx].second < 0.01) {
                dIdx++
            }
            if (creditors[cIdx].second < 0.01) {
                cIdx++
            }
        }

        return settlements
    }
}
