package gt.pagame.app.data.model

import org.junit.Assert.*
import org.junit.Test

class FriendCodeTest {
    @Test fun acceptsOnlyCodesSupportedByFirestore() {
        assertEquals("PG-1234", FriendCode.normalize("pg 1a2b3c456"))
        assertEquals("", FriendCode.normalize("PG-"))
        assertEquals("", FriendCode.normalize("١٢٣٤"))
        assertTrue(FriendCode.isComplete("PG-0000"))
        assertFalse(FriendCode.isComplete("PG-123"))
        assertFalse(FriendCode.isComplete("PG-١٢٣٤"))
    }
}
