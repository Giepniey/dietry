package app.privatediary.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.privatediary.domain.Note
import app.privatediary.domain.groupNotes
import app.privatediary.ui.components.CircleButton
import app.privatediary.ui.theme.DiaryColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class ViewMode(val label: String) {
    ListMode("List View"),
    CardMode("Card View"),
    GridMode("Grid View"),
}

private val TimeFormat = DateTimeFormatter.ofPattern("hh:mm a")
private val DateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")

@Composable
fun HomeScreen(
    notes: List<Note>,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    onLock: () -> Unit,
    onSettings: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(ViewMode.CardMode) }
    var menuOpen by remember { mutableStateOf(false) }
    val groups = remember(notes.toList()) { groupNotes(notes, LocalDate.now()) }

    Box(Modifier.fillMaxSize().background(DiaryColors.Background).statusBarsPadding()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (mode == ViewMode.GridMode) 2 else 1),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 130.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Diary",
                        color = DiaryColors.OnSurface,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    CircleButton(onClick = onLock) {
                        Icon(Icons.Default.Lock, "Lock diary", tint = DiaryColors.OnSurface)
                    }
                    Spacer(Modifier.size(8.dp))
                    CircleButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = DiaryColors.OnSurface)
                    }
                    Spacer(Modifier.size(8.dp))
                    Box {
                        CircleButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.Menu, "View mode: ${mode.label}", tint = DiaryColors.OnSurface)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            ViewMode.entries.forEach { m ->
                                DropdownMenuItem(
                                    text = {
                                        Text(m.label, color = if (m == mode) DiaryColors.Accent else DiaryColors.OnSurface)
                                    },
                                    trailingIcon = {
                                        if (m == mode) Icon(Icons.Default.Check, null, tint = DiaryColors.Accent)
                                    },
                                    onClick = { mode = m; menuOpen = false },
                                )
                            }
                        }
                    }
                }
            }

            if (groups.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "No entries yet. Tap + to write your first one.",
                        color = DiaryColors.OnSurfaceMuted,
                        modifier = Modifier.padding(top = 32.dp),
                    )
                }
            }
            groups.forEach { (label, list) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        label,
                        color = DiaryColors.OnSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }
                items(list, key = { it.id }) { note -> NoteCard(note, mode) { onOpen(note.id) } }
            }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Visual placeholder until Stage 4 wires up search.
            Row(
                Modifier.weight(1f).height(52.dp).clip(CircleShape).background(DiaryColors.Card)
                    .border(1.dp, DiaryColors.CardBorder, CircleShape).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Search, null, tint = DiaryColors.OnSurfaceMuted)
                Spacer(Modifier.size(8.dp))
                Text("Search", color = DiaryColors.OnSurfaceMuted)
            }
            CircleButton(onClick = onNew) { Icon(Icons.Default.Add, "New entry", tint = DiaryColors.Accent) }
        }
    }
}

@Composable
private fun NoteCard(note: Note, mode: ViewMode, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val minHeight = when (mode) {
        ViewMode.CardMode -> 150.dp
        ViewMode.GridMode -> 170.dp
        ViewMode.ListMode -> 0.dp
    }
    // Locked notes never show their title or a preview.
    val title = if (note.locked) "Locked entry" else note.title.ifBlank { "Untitled" }
    val preview = if (note.locked) "" else note.body.trim().replace('\n', ' ').ifBlank { "No additional text" }
    val dateText = if (note.createdAt.toLocalDate() == LocalDate.now()) {
        note.createdAt.format(TimeFormat)
    } else {
        note.createdAt.format(DateFormat)
    }

    Column(
        Modifier.fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(shape)
            .background(DiaryColors.Card)
            .border(1.dp, DiaryColors.CardBorder, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                title,
                color = DiaryColors.OnSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (preview.isNotEmpty()) {
                Text(
                    preview,
                    color = DiaryColors.OnSurfaceMuted,
                    maxLines = if (mode == ViewMode.GridMode) 4 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                dateText,
                color = DiaryColors.OnSurfaceMuted,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2C2C2E))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            if (note.locked) {
                Spacer(Modifier.size(8.dp))
                Icon(Icons.Default.Lock, "Locked", tint = DiaryColors.OnSurfaceMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}
