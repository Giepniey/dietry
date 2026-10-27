package app.privatediary

import app.privatediary.domain.Note
import app.privatediary.domain.groupNotes
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteGroupingTest {
    private val today = LocalDate.of(2026, 10, 26)

    private fun note(id: Long, day: LocalDate, pinned: Boolean = false) =
        Note(id, "t", "b", day.atTime(10, 0), pinned)

    @Test
    fun groupsByPinTodayWeekAndMonth() {
        val groups = groupNotes(
            listOf(
                note(1, today),
                note(2, today.minusDays(3)),
                note(3, today.minusDays(40)),
                note(4, today.minusDays(1), pinned = true),
            ),
            today,
        )
        assertEquals(listOf("Pinned", "Today", "Previous 7 Days"), groups.map { it.first }.take(3))
        assertEquals(4, groups.size)
        assertEquals(listOf(4L), groups[0].second.map { it.id })
    }

    @Test
    fun emptyInputGivesNoSections() {
        assertEquals(0, groupNotes(emptyList(), today).size)
    }
}
