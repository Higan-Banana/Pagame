package gt.pagame.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FriendDirectoryTest {
    @Test
    fun normalizesCodesExactlyLikeBothOriginalForms() {
        val inputs = listOf("", "PG-", "abcd", "5312", "PG-5312", "pg 53 12", "123456", "1a2b3c4")
        for (input in inputs) {
            val digits = input.filter { it.isDigit() }.take(4)
            val expected = if (digits.isEmpty()) "" else "PG-$digits"
            assertEquals(input, expected, FriendDirectory.normalizeCode(input))
        }
    }

    @Test
    fun retainsTheSameDemoPeopleAndEmails() {
        val entries = listOf(
            Triple("PG-5312", "Mario Gómez", "mario.gomez@correo.gt"),
            Triple("PG-1024", "Carlos Mendoza", "carlos.mendoza@correo.gt"),
            Triple("PG-2940", "Diana Rivas", "diana.rivas@correo.gt"),
            Triple("PG-8821", "Vale Castillo", "vale.castillo@correo.gt")
        )
        for ((code, name, email) in entries) {
            assertEquals(name, FriendDirectory.nameForCode(code))
            assertEquals(email, FriendDirectory.previewEmail(name))
        }
    }

    @Test
    fun unknownAndIncompleteCodesDoNotProduceAName() {
        for (code in listOf("", "PG-5", "PG-0000", "5312", "pg-5312")) {
            assertEquals("", FriendDirectory.nameForCode(code))
        }
        assertEquals("usuario@correo.gt", FriendDirectory.previewEmail("Desconocido"))
    }
}
