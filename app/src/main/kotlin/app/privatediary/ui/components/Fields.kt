package app.privatediary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.privatediary.ui.theme.DiaryColors

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
) {
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        modifier = Modifier.fillMaxWidth().clip(shape).background(DiaryColors.Card)
            .border(1.dp, DiaryColors.CardBorder, shape).padding(16.dp),
        textStyle = TextStyle(color = DiaryColors.OnSurface, fontSize = 18.sp),
        cursorBrush = SolidColor(DiaryColors.Accent),
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(hint, color = DiaryColors.OnSurfaceMuted, fontSize = 18.sp)
                inner()
            }
        },
    )
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(52.dp).alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape).background(DiaryColors.Accent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = DiaryColors.Background, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}
