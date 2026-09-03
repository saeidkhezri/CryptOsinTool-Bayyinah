package com.aistudio.orbit

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SavedSearchResult(
    val id: String,
    val timestamp: Long,
    val persianDate: String,
    val seeds: String,
    val depth: String,
    val limit: String,
    val topCount: String,
    val graph: Graph,
    val note: String = ""
)

class SavedResultsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("saved_results_prefs", Context.MODE_PRIVATE)
    private val KEY = "saved_results_json"

    fun saveResult(result: SavedSearchResult) {
        val list = loadResults().toMutableList()
        list.add(0, result) // Add to the top of the history list
        val jsonString = Json.encodeToString(list)
        prefs.edit().putString(KEY, jsonString).apply()
    }

    fun loadResults(): List<SavedSearchResult> {
        val jsonString = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            Json.decodeFromString<List<SavedSearchResult>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteResult(id: String) {
        val list = loadResults().filter { it.id != id }
        val jsonString = Json.encodeToString(list)
        prefs.edit().putString(KEY, jsonString).apply()
    }
}
