package gt.pagame.app.ui.screens.eventdetail

import gt.pagame.app.data.model.FriendItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EventDetailUiStateTest {
    private val friends = listOf("Santi", "Vale", "Pedro", "Sofía").mapIndexed { index, name ->
        FriendItem(
            id = "friend_$index",
            name = name,
            initial = name.take(1),
            eventsTogetherCount = 0,
            codeOrStatus = "PG-000$index"
        )
    }

    @Test
    fun participantChangeSummariesMatchOriginalForAllSubsets() {
        val subsets = (0 until (1 shl friends.size)).map { mask ->
            friends.filterIndexed { index, _ -> (mask and (1 shl index)) != 0 }
                .map { it.id }.toSet()
        }
        for (original in subsets) {
            for (draft in subsets) {
                val state = EventDetailUiState(
                    participantCandidates = friends,
                    originalParticipantIds = original,
                    draftParticipantIds = draft
                )
                val added = friends.filter { it.id in (draft - original) }.map { it.name }
                val removed = friends.filter { it.id in (original - draft) }.map { it.name }
                assertEquals(added, state.addedParticipantNames)
                assertEquals(removed, state.removedParticipantNames)
                assertEquals(original != draft, state.hasParticipantChanges)
            }
        }
    }

    @Test
    fun idsOutsideTheFriendListDoNotInventNames() {
        val state = EventDetailUiState(
            participantCandidates = friends,
            originalParticipantIds = setOf("old_missing"),
            draftParticipantIds = setOf("new_missing", friends[1].id)
        )
        assertEquals(listOf("Vale"), state.addedParticipantNames)
        assertEquals(emptyList<String>(), state.removedParticipantNames)
    }

    @Test
    fun organizerRemainsAnAvailablePayerWithoutFriends() {
        val state = EventDetailUiState()
        assertEquals(listOf("Tú"), state.payerNames)
        assertEquals(state.payerNames, state.allParticipantNames)
        assertFalse(state.hasParticipantChanges)
    }
}
