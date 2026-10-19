package gt.pagame.app.data.model

/** Shared input rules for real Firebase friend-code lookups. */
object FriendCode {
    private val completeCode = Regex("PG-[0-9]{4}")

    fun normalize(input: String): String {
        val digits = input.filter { it in '0'..'9' }.take(4)
        return if (digits.isEmpty()) "" else "PG-$digits"
    }

    fun isComplete(code: String): Boolean = completeCode.matches(code)
}
