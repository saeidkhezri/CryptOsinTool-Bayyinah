package com.aistudio.orbit.forensics.osint.bus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * OSINT Event Bus
 * Core asynchronous reactive messaging fabric for intelligence fusion (Master Instruction §2).
 * Enables modular loose coupling between collectors, adapters, correlation engines, and graph builders.
 */
object OsintEventBus {

    private val _eventsFlow = MutableSharedFlow<OsintEvent>(replay = 50, extraBufferCapacity = 500)
    val eventsFlow: Flow<OsintEvent> = _eventsFlow.asSharedFlow()

    // Thread-safe in-memory cache of published events keyed by caseId
    private val caseEventStore = ConcurrentHashMap<String, CopyOnWriteArrayList<OsintEvent>>()

    /**
     * Publishes a new OSINT event to the bus.
     */
    suspend fun publish(event: OsintEvent) {
        val list = caseEventStore.computeIfAbsent(event.caseId) { CopyOnWriteArrayList() }
        list.add(event)
        _eventsFlow.emit(event)
    }

    /**
     * Publishes multiple OSINT events in batch.
     */
    suspend fun publishBatch(events: List<OsintEvent>) {
        events.forEach { publish(it) }
    }

    /**
     * Subscribes to events for a specific case.
     */
    fun observeCase(caseId: String): Flow<OsintEvent> {
        return eventsFlow.filter { it.caseId == caseId }
    }

    /**
     * Subscribes to events of a specific indicator type.
     */
    fun observeType(type: IndicatorType): Flow<OsintEvent> {
        return eventsFlow.filter { it.indicatorType == type }
    }

    /**
     * Retrieves all events recorded for a given case.
     */
    fun getEventsForCase(caseId: String): List<OsintEvent> {
        return caseEventStore[caseId]?.toList() ?: emptyList()
    }

    /**
     * Retrieves events by indicator value and type across a case.
     */
    fun findEventsByIndicator(caseId: String, type: IndicatorType, value: String): List<OsintEvent> {
        val normalized = OsintEvent.normalizeIndicator(type, value)
        return getEventsForCase(caseId).filter { it.indicatorType == type && it.normalizedValue == normalized }
    }

    /**
     * Clears cached events for a given case.
     */
    fun clearCase(caseId: String) {
        caseEventStore.remove(caseId)
    }

    /**
     * Resets all in-memory events across all cases.
     */
    fun reset() {
        caseEventStore.clear()
    }
}
