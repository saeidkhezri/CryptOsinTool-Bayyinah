package com.aistudio.orbit.forensics.learning

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LearningProgressManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bayyinah_learning_progress", Context.MODE_PRIVATE)

    private val _learningStates = MutableStateFlow<Map<MiniLessonTopic, LearningStatus>>(emptyMap())
    val learningStates: StateFlow<Map<MiniLessonTopic, LearningStatus>> = _learningStates.asStateFlow()

    init {
        loadState()
    }

    private fun loadState() {
        val map = mutableMapOf<MiniLessonTopic, LearningStatus>()
        MiniLessonTopic.entries.forEach { topic ->
            val statusStr = prefs.getString("topic_${topic.name}", null)
            if (statusStr != null) {
                try {
                    map[topic] = LearningStatus.valueOf(statusStr)
                } catch (_: Exception) {}
            }
        }
        _learningStates.value = map
    }

    fun updateStatus(topic: MiniLessonTopic, status: LearningStatus) {
        val current = _learningStates.value[topic]
        // Progress status from SEEN -> UNDERSTOOD -> USED -> REVISITED
        val nextStatus = when (status) {
            LearningStatus.SEEN -> current ?: LearningStatus.SEEN
            LearningStatus.UNDERSTOOD -> if (current == LearningStatus.USED) LearningStatus.USED else LearningStatus.UNDERSTOOD
            LearningStatus.USED -> LearningStatus.USED
            LearningStatus.REVISITED -> LearningStatus.REVISITED
        }

        prefs.edit().putString("topic_${topic.name}", nextStatus.name).apply()
        _learningStates.value = _learningStates.value + (topic to nextStatus)
    }

    fun getStatus(topic: MiniLessonTopic): LearningStatus? {
        return _learningStates.value[topic]
    }
}
