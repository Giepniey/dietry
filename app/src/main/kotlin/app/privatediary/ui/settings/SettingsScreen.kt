package app.privatediary.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.privatediary.ui.components.CircleButton
import app.privatediary.ui.components.PasswordField
import app.privatediary.ui.components.PrimaryButton
import app.privatediary.ui.theme.DiaryColors

private val AutoLockChoices = listOf(30 to "30 seconds", 60 to "1 minute", 300 to "5 minutes", 900 to "15 minutes")

@Composable
fun SettingsScreen(
    autoLockSeconds: Int,
    message: String?,
    busy: Boolean,
    onAutoLock: (Int) -> Unit,
    onChangePassword: (old: String, new: String, confirm: String) -> Unit,
    onBack: () -> Unit,
) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().background(DiaryColors.Background)
            .statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircleButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DiaryColors.OnSurface)
        }
        Text("Settings", color = DiaryColors.OnSurface, fontSize = 34.sp, fontWeight = FontWeight.Bold)

        Text("Auto-lock after inactivity", color = DiaryColors.OnSurface, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        AutoLockChoices.forEach { (seconds, label) ->
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.RadioButton) { onAutoLock(seconds) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = autoLockSeconds == seconds,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = DiaryColors.Accent),
                )
                Text(label, color = DiaryColors.OnSurface, modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp))
            }
        }
        Text("The diary also locks the moment you leave the app.", color = DiaryColors.OnSurfaceMuted)

        Text("Change master password", color = DiaryColors.OnSurface, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        PasswordField(old, { old = it }, "Current password")
        PasswordField(new, { new = it }, "New password")
        PasswordField(confirm, { confirm = it }, "Confirm new password")
        message?.let { Text(it, color = if (it == "Password changed.") DiaryColors.Accent else DiaryColors.Danger) }
        PrimaryButton(if (busy) "Working..." else "Change password", !busy && old.isNotEmpty() && new.isNotEmpty()) {
            onChangePassword(old, new, confirm)
            old = ""
            new = ""
            confirm = ""
        }

        Text("If you forget your password", color = DiaryColors.OnSurface, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Your diary cannot be recovered. There is no reset and no backdoor. " +
                "Uninstalling the app or losing this phone erases it for good.",
            color = DiaryColors.OnSurfaceMuted,
        )
    }
}
