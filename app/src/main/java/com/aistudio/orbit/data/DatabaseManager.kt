package com.aistudio.orbit.data

import android.content.Context
import com.aistudio.orbit.db.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class DatabaseManager(private val context: Context) {

    private val dbName = "orbit_forensics_database"

    /**
     * Gets the path to the main database file.
     */
    fun getDatabaseFilePath(): String {
        return context.getDatabasePath(dbName).absolutePath
    }

    /**
     * Exports the database files to a destination directory.
     * Note: This performs a file-level backup of the SQLite database.
     */
    fun exportDatabase(destinationDir: File): Boolean {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        // Close the database to ensure a consistent file copy
        AppDatabase.getDatabase(context).close()

        val dbFiles = listOf(
            context.getDatabasePath(dbName),
            File(context.getDatabasePath(dbName).path + "-shm"),
            File(context.getDatabasePath(dbName).path + "-wal")
        )

        return try {
            for (file in dbFiles) {
                if (file.exists()) {
                    val destFile = File(destinationDir, file.name)
                    copyFile(file, destFile)
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Imports the database files from a source directory.
     */
    fun importDatabase(sourceDir: File): Boolean {
        // Close the database before replacing files
        AppDatabase.getDatabase(context).close()

        val dbFiles = listOf(dbName, "$dbName-shm", "$dbName-wal")

        return try {
            for (fileName in dbFiles) {
                val sourceFile = File(sourceDir, fileName)
                if (sourceFile.exists()) {
                    val destFile = context.getDatabasePath(fileName)
                    copyFile(sourceFile, destFile)
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    @Throws(IOException::class)
    private fun copyFile(source: File, destination: File) {
        FileInputStream(source).use { inputStream ->
            FileOutputStream(destination).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
}
