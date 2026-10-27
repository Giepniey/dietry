package app.privatediary.domain

import java.time.LocalDateTime

data class Note(
    val id: Long,
    val title: String,
    val body: String,
    val createdAt: LocalDateTime,
    val pinned: Boolean = false,
    val locked: Boolean = false,
)
