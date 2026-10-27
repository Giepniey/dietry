package app.privatediary

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.privatediary.crypto.Argon2Kdf
import app.privatediary.crypto.KeystoreWrap
import app.privatediary.crypto.VaultCrypto
import app.privatediary.crypto.validateNewPassword
import app.privatediary.data.NoteDb
import app.privatediary.domain.Note
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    enum class Gate { Setup, Locked, Open }

    private val vaultFile = File(app.filesDir, "vault.bin")
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val crypto = VaultCrypto(Argon2Kdf, KeystoreWrap())
    // One thread for all database work keeps writes in order and lets close() queue behind them.
    private val dbThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    var gate by mutableStateOf(if (vaultFile.exists()) Gate.Locked else Gate.Setup)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var saveFailed by mutableStateOf(false)
        private set
    var autoLockSeconds by mutableIntStateOf(prefs.getInt("autolock_s", 60))
        private set

    val notes = mutableStateListOf<Note>()

    /** Set by the activity; stops an unlock that finishes while the app is hidden from staying open. */
    var foreground = true

    private var dek: ByteArray? = null
    private var db: NoteDb? = null
    private var idleJob: Job? = null
    // shortcut: in-memory throttle only, resets when the process restarts; the Argon2 cost is the real brake.
    private var failures = 0

    // ---- master password ----

    fun setup(password: String) = launchBusy {
        check(!vaultFile.exists()) { "A vault already exists" }
        val pw = password.toByteArray()
        val created = try {
            withContext(Dispatchers.Default) { crypto.create(pw) }
        } finally {
            pw.fill(0)
        }
        writeVault(created.file)
        openDiary(created.dek)
    }

    fun unlock(password: String) = launchBusy {
        val pw = password.toByteArray()
        val key = try {
            withContext(Dispatchers.Default) { crypto.unlock(vaultFile.readBytes(), pw) }
        } finally {
            pw.fill(0)
        }
        if (key == null) {
            failures++
            error = "Wrong password."
            delay(failures.coerceAtMost(8) * 1000L)
        } else {
            failures = 0
            openDiary(key)
        }
    }

    fun changePassword(old: String, new: String, confirm: String) = launchBusy {
        message = null
        val problem = validateNewPassword(new, confirm)
        if (problem != null) {
            message = problem
            return@launchBusy
        }
        val o = old.toByteArray()
        val n = new.toByteArray()
        try {
            val newFile = withContext(Dispatchers.Default) { crypto.changePassword(vaultFile.readBytes(), o, n) }
            if (newFile == null) {
                message = "The current password is wrong."
            } else {
                writeVault(newFile)
                message = "Password changed."
            }
        } finally {
            o.fill(0)
            n.fill(0)
        }
    }

    // ---- locking ----

    fun lock() {
        if (gate != Gate.Open) return
        idleJob?.cancel()
        val database = db
        db = null
        viewModelScope.launch(dbThread) { database?.close() } // runs after any queued saves
        dek?.fill(0)
        dek = null
        notes.clear()
        error = null
        message = null
        saveFailed = false
        gate = Gate.Locked
    }

    /** Restarts the inactivity timer; called on touches and on every edit (typing sends no touch events). */
    fun poke() {
        if (gate != Gate.Open) return
        idleJob?.cancel()
        idleJob = viewModelScope.launch {
            delay(autoLockSeconds * 1000L)
            lock()
        }
    }

    fun setAutoLock(seconds: Int) {
        prefs.edit().putInt("autolock_s", seconds).apply()
        autoLockSeconds = seconds
        poke()
    }

    fun clearMessage() {
        message = null
    }

    // ---- notes ----

    fun create(): Long {
        val note = Note(System.currentTimeMillis(), "", "", LocalDateTime.now())
        notes.add(note)
        poke()
        return note.id
    }

    fun update(note: Note) {
        val i = notes.indexOfFirst { it.id == note.id }
        if (i >= 0) notes[i] = note
        val database = db ?: return
        viewModelScope.launch(dbThread) {
            try {
                database.save(note)
                saveFailed = false
            } catch (e: Exception) {
                saveFailed = true
            }
        }
        poke()
    }

    fun discardIfEmpty(id: Long) {
        val empty = notes.removeAll { it.id == id && it.title.isBlank() && it.body.isBlank() }
        val database = db
        if (empty && database != null) viewModelScope.launch(dbThread) { runCatching { database.delete(id) } }
    }

    // ---- internals ----

    private suspend fun openDiary(key: ByteArray) {
        try {
            val database = NoteDb(getApplication<Application>(), key.joinToString("") { "%02x".format(it) })
            val loaded = withContext(dbThread) { database.loadAll() }
            dek = key
            db = database
            notes.clear()
            notes.addAll(loaded)
            gate = Gate.Open
            poke()
            if (!foreground) lock()
        } catch (e: Exception) {
            key.fill(0)
            throw e
        }
    }

    private fun writeVault(bytes: ByteArray) {
        val tmp = File(vaultFile.parentFile, "vault.bin.tmp")
        FileOutputStream(tmp).use {
            it.write(bytes)
            it.fd.sync()
        }
        Files.move(tmp.toPath(), vaultFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun launchBusy(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                // Class name only: never put password or diary text in a message.
                error = "Something went wrong (${e.javaClass.simpleName})."
            } finally {
                busy = false
            }
        }
    }
}
