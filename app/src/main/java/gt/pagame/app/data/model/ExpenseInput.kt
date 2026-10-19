package gt.pagame.app.data.model

import java.math.BigDecimal

private val editableAmount = Regex("[0-9]{0,7}(\\.[0-9]{0,2})?")
private val maximumAmount = BigDecimal("1000000")

/** Allow intermediate editing states, but never silently alter pasted amounts. */
fun normalizedAmountInput(input: String): String? {
    val normalized = input.replace(',', '.')
    if (!editableAmount.matches(normalized)) return null
    if (normalized.toBigDecimalOrNull()?.let { it > maximumAmount } == true) return null
    return normalized
}

fun String.takeText(max: Int): String {
    val result = take(max)
    return if (result.lastOrNull()?.isHighSurrogate() == true) result.dropLast(1) else result
}
