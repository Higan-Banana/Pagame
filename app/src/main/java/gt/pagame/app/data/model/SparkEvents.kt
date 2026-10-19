package gt.pagame.app.data.model


/** Pure operations; Firestore independently validates every committed mutation. Amounts are cents. */
class PagameValidationException(message: String) : IllegalArgumentException(message)
object SparkEvents {
    private fun check(ok: Boolean, message: String) { if (!ok) throw PagameValidationException(message) }
    private fun title(value: Any?): String = (value as? String).orEmpty().trim().also {
        check(it.isNotEmpty() && it.length <= 100, "Revisa el nombre o concepto (máximo 100 caracteres).")
    }
    fun ids(value: Any?): List<String> = strings(value).distinct().sorted().also {
        check(it.isNotEmpty() && it.size <= 30 && it.all { id -> id.isNotBlank() && '/' !in id }, "Selecciona entre 1 y 30 participantes.")
    }
    fun create(owner: String, name: String, members: List<String>, currency: String, now: Long): Map<String, Any?> {
        val people = ids(members + owner)
        val nameChecked = title(name)
        return mapOf("schemaVersion" to 2L, "title" to nameChecked, "ownerId" to owner, "participantIds" to people,
            "memberSlots" to (people + List(30 - people.size) { "" }), "currency" to currency,
            "expenses" to emptyMap<String, Any?>(), "payments" to emptyMap<String, Any?>(),
            "pendingPayment" to null, "revision" to 0L, "createdAt" to now, "updatedAt" to now,
            "lastChange" to mapOf("kind" to "createEvent", "id" to ""))
    }
    fun decode(id: String, data: Map<String, Any?>): OnlineEvent {
        check(number(data["schemaVersion"]) == 2L, "Hay un evento de otra versión. Revisa la guía antes de migrar sus datos.")
        val pending = data["pendingPayment"] as? Map<*, *>
        return OnlineEvent(id, data["title"] as String, data["ownerId"] as String, strings(data["participantIds"]),
            Currency.valueOf(data["currency"] as String), data["emoji"] as? String ?: "📅",
            records(data["expenses"]).values.map { e -> StoredExpense(e["id"] as String, e["title"] as String,
                number(e["amountMinor"]), e["payerId"] as String, strings(e["includedIds"]), e["createdBy"] as String, number(e["createdAt"]), e["emoji"] as? String ?: "🧾") },
            (records(data["payments"]).values.toList() + listOfNotNull(pending)).map { p -> Payment(p["id"] as String,
                p["fromId"] as String, p["toId"] as String, number(p["amountMinor"]), p["status"] as String, number(p["createdAt"])) },
            number(data["revision"]), number(data["createdAt"]))
    }
    fun mutate(original: Map<String, Any?>, actor: String, action: String, input: Map<String, Any>, now: Long): Map<String, Any?>? {
        check(number(original["schemaVersion"]) == 2L, "Este evento usa otro formato. Revisa la guía de actualización.")
        val event = decode("", original)
        check(actor in event.participantIds, "No perteneces a este evento.")
        val owner = event.ownerId == actor
        val result = original.toMutableMap()
        val expenses = records(original["expenses"]).toMutableMap()
        val payments = records(original["payments"]).toMutableMap()
        var pending = original["pendingPayment"] as? Map<*, *>
        val itemId = (input["expenseId"] ?: input["paymentId"] ?: "") as String
        when (action) {
            "deleteEvent" -> {
                check(owner, "Solo el organizador puede eliminar el evento.")
                check(expenses.isEmpty() && payments.isEmpty() && pending == null, "No puedes eliminar un evento con gastos o pagos registrados.")
                return null
            }
            "updateParticipants" -> {
                check(owner, "Solo el organizador puede editar los participantes.")
                val next = ids(input["participantIds"])
                check(event.ownerId in next, "El organizador debe permanecer en el evento.")
                (event.participantIds - next.toSet()).forEach { uid ->
                    val count = event.expenses.count { it.payerId == uid || uid in it.includedIds }
                    check(count == 0, "No puedes quitar a este participante: tiene $count gastos asociados. Edita o borra esos gastos primero.")
                    check(event.payments.none { it.fromId == uid || it.toId == uid }, "No puedes quitar a un participante con pagos registrados.")
                }
                val slots = strings(original["memberSlots"]).map { if (it in next) it else "" }.toMutableList()
                (next - event.participantIds.toSet()).forEach { slots[slots.indexOf("")] = it }
                result["participantIds"] = next; result["memberSlots"] = slots
            }
            "addExpense", "updateExpense", "deleteExpense" -> {
                val old = expenses[itemId]
                if (action != "addExpense") {
                    check(old != null, "Este gasto ya no existe.")
                    check(owner || old?.get("createdBy") == actor, "Solo el autor o el organizador puede editar este gasto.")
                }
                if (action == "deleteExpense") expenses.remove(itemId)
                else {
                    val amount = number(input["amountMinor"])
                    val included = ids(input["includedIds"])
                    val payer = (input["payerId"] as? String).orEmpty()
                    check(amount in 1..100000000L, "Ingresa un monto positivo con hasta dos decimales.")
                    check(payer in event.participantIds && included.all { it in event.participantIds }, "El pagador y los participantes deben pertenecer al evento.")
                    val expense = mapOf("id" to itemId, "title" to title(input["title"]), "amountMinor" to amount,
                        "payerId" to payer, "includedIds" to included, "extraThrough" to (if (amount % included.size == 0L) "" else included[(amount % included.size).toInt() - 1]),
                        "createdBy" to (old?.get("createdBy") ?: actor), "createdAt" to (old?.get("createdAt") ?: now)) +
                        (old?.filterKeys { it == "emoji" } ?: emptyMap())
                    if (action == "addExpense" && old != null) {
                        check(old == expense && old["createdBy"] == actor, "Este gasto ya fue registrado. Cierra el formulario y edítalo desde la lista.")
                        return original
                    }
                    if (action == "updateExpense" && old == expense) return original
                    check(action != "addExpense" || expenses.size < 500, "Este evento alcanzó el límite de 500 gastos.")
                    expenses[itemId] = expense
                }
                // A reported transfer may already have happened in real life. Only its receiver resolves it.
            }
            "reportPayment" -> {
                check(input["fromId"] == actor, "Solo puedes informar tus propios pagos.")
                check(number(input["revision"]) == event.revision, "El balance cambió. Revisa los importes antes de confirmar.")
                check(pending == null, "Primero debe confirmarse o rechazarse el pago pendiente.")
                val transfer = event.suggestions().firstOrNull { it.fromId == actor && it.toId == input["toId"] && it.amountMinor == number(input["amountMinor"]) }
                check(transfer != null, "El importe ya no coincide con el balance actual.")
                check(payments.size < 500 && itemId !in payments, "No se puede registrar este pago.")
                val slots = strings(original["memberSlots"])
                pending = mapOf("id" to itemId, "fromId" to actor, "toId" to transfer!!.toId, "amountMinor" to transfer.amountMinor,
                    "fromSlot" to slots.indexOf(actor).toLong(), "toSlot" to slots.indexOf(transfer.toId).toLong(),
                    "status" to "pending", "createdAt" to now)
            }
            "confirmPayment", "rejectPayment" -> {
                val payment = pending
                check(payment != null && payment["id"] == itemId, "Este pago ya fue resuelto o cambió el balance.")
                check(payment?.get("toId") == actor, "Solo quien recibe puede confirmar o rechazar el pago.")
                if (action == "confirmPayment") {
                    @Suppress("UNCHECKED_CAST")
                    payments[itemId] = (payment as Map<String, Any?>) + ("status" to "confirmed")
                }
                pending = null
            }
            else -> throw PagameValidationException("Operación desconocida.")
        }
        result["expenses"] = expenses; result["payments"] = payments; result["pendingPayment"] = pending
        result["revision"] = event.revision + 1L; result["updatedAt"] = now
        result["lastChange"] = mapOf("kind" to action, "id" to itemId)
        return result
    }
    /** Ten small, atomically validated ledgers keep each rules evaluation bounded. */
    fun ledgers(data: Map<String, Any?>): List<Map<String, Any?>> {
        val event = decode("", data)
        val balances = event.balances()
        val expenseCounts = event.expenseCounts()
        val paymentCounts = event.paymentCounts()
        var prefix = 0L
        return strings(data["memberSlots"]).chunked(3).map { slots ->
            prefix += slots.sumOf { balances[it] ?: 0L }
            mapOf(
            "revision" to event.revision, "balancePrefix" to prefix,
            "cells" to slots.map { uid -> mapOf("uid" to uid, "balance" to (balances[uid] ?: 0L),
                "expenseCount" to (expenseCounts[uid] ?: 0).toLong(),
                "paymentCount" to (paymentCounts[uid] ?: 0).toLong()) }
        ) }
    }
    fun number(value: Any?) = (value as? Number)?.toLong() ?: 0L
    fun strings(value: Any?) = (value as? List<*>)?.filterIsInstance<String>().orEmpty()
    @Suppress("UNCHECKED_CAST")
    fun records(value: Any?): Map<String, Map<String, Any?>> = value as? Map<String, Map<String, Any?>> ?: emptyMap()
}
