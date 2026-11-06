package gt.pagame.app.data.model

/** Reasons shared by the form and ViewModel. Firebase still authorizes every write. */
object ActionRestrictions {
    fun eventTitle(title: String): String? = when {
        title.isBlank() -> "No puedes crear el evento porque falta su nombre."
        title.trim().length > 100 -> "No puedes crear el evento porque el nombre supera los 100 caracteres."
        else -> null
    }

    fun expense(title: String, amount: String, includedCount: Int, inputError: String? = null): String? = when {
        title.isBlank() -> "No puedes guardar el gasto porque falta el concepto."
        title.trim().length > 100 -> "No puedes guardar el gasto porque el concepto supera los 100 caracteres."
        inputError != null -> "No puedes guardar el gasto: $inputError"
        amountMinor(amount) == null -> "No puedes guardar el gasto: ingresa un monto entre 0.01 y 100,000.00, con hasta dos decimales."
        includedCount == 0 -> "No puedes guardar el gasto porque debes incluir al menos un participante."
        else -> null
    }

    fun removeParticipant(event: OnlineEvent, uid: String): String? = when {
        uid == event.ownerId -> "No puedes quitar al organizador porque debe permanecer en el evento."
        event.expenses.any { it.payerId == uid || uid in it.includedIds } ->
            "No puedes quitar a esta persona porque tiene gastos asociados. Edita o elimina esos gastos primero."
        event.payments.any { (it.fromId == uid || it.toId == uid) && it.status == "pending" } ->
            "No puedes quitar a esta persona porque tiene un pago pendiente de confirmación."
        event.payments.any { it.fromId == uid || it.toId == uid } ->
            "No puedes quitar a esta persona porque tiene pagos confirmados que deben conservarse en el historial."
        else -> null
    }

    fun deleteEvent(event: OnlineEvent?, uid: String): String? = when {
        event == null -> "No puedes eliminar el evento porque ya no está disponible."
        event.ownerId != uid -> "No puedes eliminar el evento porque solo su organizador tiene permiso."
        event.expenses.isNotEmpty() -> "No puedes eliminar el evento porque tiene gastos registrados."
        event.payments.any { it.status == "pending" } -> "No puedes eliminar el evento porque tiene un pago pendiente de confirmación."
        event.payments.isNotEmpty() -> "No puedes eliminar el evento porque tiene pagos confirmados que deben conservarse en el historial."
        else -> null
    }

    fun registration(name: String, email: String, password: String, confirmation: String): String? = when {
        name.isBlank() -> "No puedes crear la cuenta porque falta tu nombre."
        email.isBlank() -> "No puedes crear la cuenta porque falta tu correo."
        password.length < 6 -> "No puedes crear la cuenta porque la contraseña debe tener al menos 6 caracteres."
        password != confirmation -> "No puedes crear la cuenta porque las contraseñas no coinciden."
        else -> null
    }
}
