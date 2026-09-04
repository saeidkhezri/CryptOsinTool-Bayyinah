package com.aistudio.orbit.db

import androidx.room.TypeConverter
import com.aistudio.orbit.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)
    @TypeConverter
    fun toStringList(value: String): List<String> = try { json.decodeFromString(value) } catch (e: Exception) { emptyList() }

    @TypeConverter
    fun fromCounterpartySummaryList(value: List<CounterpartySummary>): String = json.encodeToString(value)
    @TypeConverter
    fun toCounterpartySummaryList(value: String): List<CounterpartySummary> = try { json.decodeFromString(value) } catch (e: Exception) { emptyList() }

    @TypeConverter
    fun fromForensicTransactionList(value: List<ForensicTransaction>): String = json.encodeToString(value)
    @TypeConverter
    fun toForensicTransactionList(value: String): List<ForensicTransaction> = try { json.decodeFromString(value) } catch (e: Exception) { emptyList() }

    @TypeConverter
    fun fromEvidenceItemList(value: List<EvidenceItem>): String = json.encodeToString(value)
    @TypeConverter
    fun toEvidenceItemList(value: String): List<EvidenceItem> = try { json.decodeFromString(value) } catch (e: Exception) { emptyList() }

    @TypeConverter
    fun fromRiskIndicatorList(value: List<RiskIndicator>): String = json.encodeToString(value)
    @TypeConverter
    fun toRiskIndicatorList(value: String): List<RiskIndicator> = try { json.decodeFromString(value) } catch (e: Exception) { emptyList() }

    @TypeConverter
    fun fromPatternMatchResultList(value: List<PatternMatchResult>): String = json.encodeToString(value)
    @TypeConverter
    fun toPatternMatchResultList(value: String): List<PatternMatchResult> = try { json.decodeFromString(value) } catch (e: Exception) { emptyList() }

    @TypeConverter
    fun fromHypothesisList(value: List<Hypothesis>): String = json.encodeToString(value)
    @TypeConverter
    fun toHypothesisList(value: String): List<Hypothesis> = try { json.decodeFromString(value) } catch (e: Exception) { emptyList() }
}
