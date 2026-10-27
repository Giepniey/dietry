package app.privatediary.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Pinned first, then Today, Previous 7 Days, then one section per older month. Empty sections are omitted. */
fun groupNotes(notes: List<Note>, today: LocalDate): List<Pair<String, List<Note>>> {
    val sorted = notes.sortedByDescending { it.createdAt }
    val out = mutableListOf<Pair<String, List<Note>>>()
    fun add(label: String, list: List<Note>) {
        if (list.isNotEmpty()) out += label to list
    }

    add("Pinned", sorted.filter { it.pinned })
    val rest = sorted.filterNot { it.pinned }
    val weekAgo = today.minusDays(7)
    add("Today", rest.filter { !it.createdAt.toLocalDate().isBefore(today) })
    add("Previous 7 Days", rest.filter {
        val d = it.createdAt.toLocalDate()
        d.isBefore(today) && !d.isBefore(weekAgo)
    })
    rest.filter { it.createdAt.toLocalDate().isBefore(weekAgo) }
        .groupBy { YearMonth.from(it.createdAt) }
        .entries.sortedByDescending { it.key }
        .forEach { (ym, list) ->
            add("${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}", list)
        }
    return out
}
