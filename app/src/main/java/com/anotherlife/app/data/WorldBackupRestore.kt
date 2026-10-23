package com.anotherlife.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

/** Validates a selected SQLite backup, then swaps it in before Room opens the database. */
object WorldBackupRestore {
    private const val DATABASE_NAME = "another_life.db"
    private const val PENDING_NAME = "another_life_restore.pending"
    private const val REJECTED_NAME = "another_life_restore.rejected"
    private const val MAX_BACKUP_BYTES = 512L * 1024L * 1024L

    fun stageRestore(context: Context, source: InputStream) {
        val temporary = File(context.filesDir, "$PENDING_NAME.tmp")
        try {
            FileOutputStream(temporary).use { output ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_BACKUP_BYTES) { "Backup exceeds the 512 MB safety limit." }
                    output.write(buffer, 0, count)
                }
                output.fd.sync()
            }
            validateSqlite(temporary)
            val pending = File(context.filesDir, PENDING_NAME)
            check(temporary.renameTo(pending)) { "Could not stage the restore file." }
        } finally {
            temporary.delete()
        }
    }

    /** Called from Application.onCreate before any Room database is opened. */
    fun applyOnColdStart(context: Context) {
        val pending = File(context.filesDir, PENDING_NAME)
        if (!pending.isFile) return
        val target = context.getDatabasePath(DATABASE_NAME)
        val previous = File(context.filesDir, "$DATABASE_NAME.before-restore")
        runCatching {
            validateSqlite(pending)
            target.parentFile?.mkdirs()
            File(target.parentFile, "$DATABASE_NAME-wal").delete()
            File(target.parentFile, "$DATABASE_NAME-shm").delete()
            previous.delete()
            if (target.exists()) check(target.renameTo(previous)) { "Could not preserve the current database." }
            check(pending.renameTo(target)) { "Could not install the restored database." }
            previous.delete()
        }.onFailure {
            if (!target.exists() && previous.exists()) previous.renameTo(target)
            pending.renameTo(File(context.filesDir, REJECTED_NAME))
        }
    }

    private fun validateSqlite(file: File) {
        require(file.isFile && file.length() in 100L..MAX_BACKUP_BYTES) { "Backup file is empty or too large." }
        val header = ByteArray(16)
        FileInputStream(file).use { input ->
            require(input.read(header) == header.size) { "Backup header is incomplete." }
        }
        require(String(header, Charsets.US_ASCII) == "SQLite format 3\u0000") { "This is not a SQLite database." }
        val database = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            database.rawQuery("PRAGMA quick_check(1)", null).use { cursor ->
                require(cursor.moveToFirst() && cursor.getString(0) == "ok") { "Backup integrity check failed." }
            }
        } finally {
            database.close()
        }
    }
}
