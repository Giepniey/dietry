package app.privatediary

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.privatediary.ui.editor.EditorScreen
import app.privatediary.ui.home.HomeScreen
import app.privatediary.ui.lock.SetupScreen
import app.privatediary.ui.lock.UnlockScreen
import app.privatediary.ui.settings.SettingsScreen
import app.privatediary.ui.theme.DiaryTheme

class MainActivity : ComponentActivity() {
    private val vm: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Blocks screenshots and hides content in the recent-apps preview.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            DiaryTheme {
                when (vm.gate) {
                    NotesViewModel.Gate.Setup -> SetupScreen(vm.busy, vm.error, vm::setup)
                    NotesViewModel.Gate.Locked -> UnlockScreen(vm.busy, vm.error, vm::unlock)
                    NotesViewModel.Gate.Open -> OpenDiary(vm)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.foreground = true
    }

    // Leaving the app locks it. A rotation also stops the activity, so that case is excluded.
    override fun onStop() {
        super.onStop()
        vm.foreground = false
        if (!isChangingConfigurations) vm.lock()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        vm.poke()
    }
}

@Composable
private fun OpenDiary(vm: NotesViewModel) {
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val open = vm.notes.firstOrNull { it.id == openId }
    val closeNote = {
        openId?.let { vm.discardIfEmpty(it) }
        openId = null
    }

    BackHandler(enabled = open != null || showSettings) {
        if (showSettings) showSettings = false else closeNote()
    }

    when {
        showSettings -> SettingsScreen(
            autoLockSeconds = vm.autoLockSeconds,
            message = vm.message,
            busy = vm.busy,
            onAutoLock = vm::setAutoLock,
            onChangePassword = vm::changePassword,
            onBack = {
                vm.clearMessage()
                showSettings = false
            },
        )
        open == null -> HomeScreen(
            notes = vm.notes,
            onOpen = { openId = it },
            onNew = { openId = vm.create() },
            onLock = vm::lock,
            onSettings = { showSettings = true },
        )
        else -> EditorScreen(open, onChange = vm::update, onBack = closeNote, saveFailed = vm.saveFailed)
    }
}
