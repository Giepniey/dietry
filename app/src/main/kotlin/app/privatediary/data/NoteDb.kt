package app.privatediary.data

import android.content.Context
import app.privatediary.domain.Note
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteOpenHelper

/** SQLCipher database. [hexKey] is the 256-bit data key; SQLCipher gets it as a raw key, so no password KDF runs here. */
class NoteDb(context: Context, hexKey: String) :
      SQLiteOpenHelper(context, "diary.db", "x'$hexKey'", null, 1, 0, null, null, false) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE notes(id INTEGER PRIMARY KEY, title TEXT NOT NULL, body TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, pinned INTEGER NOT NULL DEFAULT 0, locked INTEGER NOT NULL DEFAULT 0)",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun loadAll(): List<Note> {
        val out = mutableListOf<Note>()
        readableDatabase.rawQuery("SELECT id, title, body, created_at, pinned, locked FROM notes", emptyArray<String>()).use { c ->
            while (c.moveToNext()) {
                out += Note(
                    id = c.getLong(0),
                    title = c.getString(1),
                    body = c.getString(2),
                    createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(c.getLong(3)), ZoneId.systemDefault()),
                    pinned = c.getInt(4) == 1,
                    locked = c.getInt(5) == 1,
                )
            }
        }
        return out
    }

    /** Single statement, so SQLite makes it atomic: a crash leaves the old or the new row, never half. */
    fun save(note: Note) {
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO notes(id, title, body, created_at, pinned, locked) VALUES(?,?,?,?,?,?)",
            arrayOf<Any>(
                note.id,
                note.title,
                note.body,
                note.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                if (note.pinned) 1 else 0,
                if (note.locked) 1 else 0,
            ),
        )
    }

    fun delete(id: Long) {
        writableDatabase.execSQL("DELETE FROM notes WHERE id=?", arrayOf<Any>(id))
    }

    companion object {
        init {
            System.loadLibrary("sqlcipher")
        }
    }
}
