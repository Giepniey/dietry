package app.privatediary.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.privatediary.crypto.validateNewPassword
import app.privatediary.ui.components.PasswordField
import app.privatediary.ui.components.PrimaryButton
import app.privatediary.ui.theme.DiaryColors

@Composable
private fun LockColumn(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(DiaryColors.Background)
            .statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

@Composable
fun SetupScreen(busy: Boolean, error: String?, onCreate: (String) -> Unit) {
    // Passwords are kept in plain `remember`, never rememberSaveable, so they are not written into saved state.
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var understood by remember { mutableStateOf(false) }
    val problem = validateNewPassword(password, confirm)

    LockColumn {
        Text("Create your master password", color = DiaryColors.OnSurface, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(
            "This password locks the whole diary. Your entries are encrypted on this phone with a key that only your password can unlock.",
            color = DiaryColors.OnSurfaceMuted,
        )
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DiaryColors.Card)
                .border(1.dp, DiaryColors.Danger, RoundedCornerShape(16.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("There is no recovery", color = DiaryColors.Danger, fontWeight = FontWeight.SemiBold)
            Text(
                "If you forget this password, your diary cannot be opened by anyone, including us. " +
                    "There is no reset, no account and no backdoor. Uninstalling the app or losing the phone also erases it.",
                color = DiaryColors.OnSurface,
            )
        }
        PasswordField(password, { password = it }, "Master password")
        PasswordField(confirm, { confirm = it }, "Confirm password")
        if (password.isNotEmpty() && problem != null) Text(problem, color = DiaryColors.Danger)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = understood,
                onCheckedChange = { understood = it },
                colors = CheckboxDefaults.colors(checkedColor = DiaryColors.Accent),
            )
            Text("I understand it cannot be recovered", color = DiaryColors.OnSurface)
        }
        error?.let { Text(it, color = DiaryColors.Danger) }
        PrimaryButton(if (busy) "Securing your diary..." else "Create diary", !busy && problem == null && understood) {
            onCreate(password)
        }
    }
}

@Composable
fun UnlockScreen(busy: Boolean, error: String?, onUnlock: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    val submit = {
        if (!busy && password.isNotEmpty()) {
            onUnlock(password)
            password = ""
        }
    }
    LockColumn {
        Text("Private Diary", color = DiaryColors.OnSurface, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("Enter your master password to open your diary.", color = DiaryColors.OnSurfaceMuted)
        PasswordField(password, { password = it }, "Master password", ImeAction.Done) { submit() }
        error?.let { Text(it, color = DiaryColors.Danger) }
        PrimaryButton(if (busy) "Unlocking..." else "Unlock", !busy && password.isNotEmpty()) { submit() }
    }
}
