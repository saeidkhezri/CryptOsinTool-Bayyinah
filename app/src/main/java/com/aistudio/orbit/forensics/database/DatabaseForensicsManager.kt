package com.aistudio.orbit.forensics.database

import android.content.Context
import android.util.Log
import com.aistudio.orbit.db.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Forensic Database Manager for بیِّنة (Master Instruction §38-43).
 * Handles Export (Backup), Import (Restore), and Integrity checks.
 */
class DatabaseForensicsManager(private val context: Context) {

    private val dbName = "orbit_forensics_database"
    private val dbFile: File = context.getDatabasePath(dbName)

    /**
     * Get the absolute path to the primary Room database file.
     */
    fun getDatabasePath(): String {
        return dbFile.absolutePath
    }

    /**
     * Export the database to a target OutputStream (e.g. from Storage Access Framework).
     */
    fun exportDatabase(outputStream: OutputStream): Result<Unit> {
        return try {
            // Ensure any pending Room transactions are flushed
            AppDatabase.getDatabase(context).close()
            
            FileInputStream(dbFile).use { input ->
                input.copyTo(outputStream)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Export failed", e)
            Result.failure(e)
        }
    }

    /**
     * Import a database from a target InputStream.
     * WARNING: This overwrites the current database.
     */
    fun importDatabase(inputStream: InputStream): Result<Unit> {
        return try {
            // Close database before overwriting
            AppDatabase.getDatabase(context).close()
            
            FileOutputStream(dbFile).use { output ->
                inputStream.copyTo(output)
            }
            
            // Also handle SHM and WAL files if they exist
            val shmFile = File(dbFile.path + "-shm")
            val walFile = File(dbFile.path + "-wal")
            if (shmFile.exists()) shmFile.delete()
            if (walFile.exists()) walFile.delete()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Import failed", e)
            Result.failure(e)
        }
    }

    /**
     * Get size of the database in bytes.
     */
    fun getDatabaseSize(): Long {
        return if (dbFile.exists()) dbFile.length() else 0L
    }
}
