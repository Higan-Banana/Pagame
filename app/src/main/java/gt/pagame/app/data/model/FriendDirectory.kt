package gt.pagame.app.data.model

/** Shared demo lookup; preserves the codes and preview data used by both forms. */
internal object FriendDirectory {
    private val namesByCode = mapOf(
        "PG-5312" to "Mario Gómez",
        "PG-1024" to "Carlos Mendoza",
        "PG-2940" to "Diana Rivas",
        "PG-8821" to "Vale Castillo"
    )

    fun normalizeCode(input: String): String {
        val numbers = input.filter { it.isDigit() }.take(4)
        return if (numbers.isEmpty()) "" else "PG-$numbers"
    }

    fun nameForCode(code: String): String = namesByCode[code].orEmpty()

    fun previewEmail(name: String): String = when (name) {
        "Mario Gómez" -> "mario.gomez@correo.gt"
        "Carlos Mendoza" -> "carlos.mendoza@correo.gt"
        "Diana Rivas" -> "diana.rivas@correo.gt"
        "Vale Castillo" -> "vale.castillo@correo.gt"
        else -> "usuario@correo.gt"
    }
}
