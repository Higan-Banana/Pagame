package gt.pagame.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TitleInitialTest {
    @Test fun badgesUseOneLetterFromTheTitle() {
        assertEquals("C", titleInitial("Cena de amigos"))
        assertEquals("Á", titleInitial("  árbol"))
        assertEquals("P", titleInitial("🍕 Pizza"))
        assertEquals("V", titleInitial("2026 Viaje"))
        assertEquals("2", titleInitial("2026"))
        assertEquals("?", titleInitial("🦦"))
        assertEquals("?", titleInitial(""))
        assertEquals(1, titleInitial("𐐨 ejemplo").codePointCount(0, titleInitial("𐐨 ejemplo").length))
    }
}
