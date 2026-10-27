package app.privatediary.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.privatediary.domain.Note
import app.privatediary.ui.components.CircleButton
import app.privatediary.ui.theme.DiaryColors
import java.time.format.DateTimeFormatter

private val MetaFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy, hh:mm a")

@Composable
fun EditorScreen(note: Note, onChange: (Note) -> Unit, onBack: () -> Unit, saveFailed: Boolean = false) {
    val words = note.body.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

    Column(
        Modifier.fillMaxSize()
            .background(DiaryColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DiaryColors.OnSurface)
            }
            Spacer(Modifier.weight(1f))
            // Undo/redo and the menu arrive with the editor stages; shown disabled until then.
            Row(
                Modifier.height(52.dp).clip(CircleShape).background(DiaryColors.Card)
                    .border(1.dp, DiaryColors.CardBorder, CircleShape).alpha(0.4f).padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("↶", color = DiaryColors.OnSurface, fontSize = 22.sp)
                Text("↷", color = DiaryColors.OnSurface, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            CircleButton(onClick = {}, enabled = false) {
                Icon(Icons.Default.MoreVert, "More (not available yet)", tint = DiaryColors.OnSurface)
            }
        }

        if (saveFailed) {
            Text(
                "Couldn't save your latest changes. Keep this screen open and try typing again.",
                color = DiaryColors.Danger,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Text(
            "${note.createdAt.format(MetaFormat)}  |  $words words",
            color = DiaryColors.OnSurfaceMuted,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 20.dp, bottom = 12.dp),
        )
        HintField(
            value = note.title,
            onValueChange = { onChange(note.copy(title = it)) },
            hint = "Title",
            textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
            singleLine = true,
        )
        HintField(
            value = note.body,
            onValueChange = { onChange(note.copy(body = it)) },
            hint = "Start writing...",
            textStyle = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
            modifier = Modifier.weight(1f).padding(top = 16.dp),
        )

        // Formatting, image, drawing, checklist and table tools arrive in Stages 6-8.
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp).height(56.dp).clip(CircleShape)
                .background(DiaryColors.Card).border(1.dp, DiaryColors.CardBorder, CircleShape).alpha(0.5f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("Aa", "Image", "Draw", "Tasks", "Table").forEach {
                Text(it, color = DiaryColors.OnSurface, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun HintField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = textStyle.copy(color = DiaryColors.OnSurface),
        singleLine = singleLine,
        cursorBrush = SolidColor(DiaryColors.Accent),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(hint, style = textStyle.copy(color = DiaryColors.OnSurfaceMuted))
                inner()
            }
        },
    )
}
