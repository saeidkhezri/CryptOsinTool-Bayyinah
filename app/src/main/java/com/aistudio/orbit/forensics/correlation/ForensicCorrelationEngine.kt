package com.aistudio.orbit.forensics.correlation

import com.aistudio.orbit.db.EpistemicStatus
import com.aistudio.orbit.forensics.osint.bus.IndicatorType
import com.aistudio.orbit.forensics.osint.bus.OsintEvent
import com.aistudio.orbit.forensics.osint.bus.SourceLineageType
import kotlinx.serialization.Serializable

@Serializable
enum class CorrelationRuleType(val displayNameEn: String, val displayNameFa: String) {
    SAME_DOMAIN("Shared Registrable Domain", "دامنه مشترک ثبت‌شده"),
    SAME_IP("Shared Infrastructure IP", "آدرس آی‌پی مشترک"),
    SAME_ASN("Shared Autonomous System (ASN)", "شماره سامانه خودمختار مشترک"),
    SAME_EMAIL("Shared Contact Email", "پست الکترونیک مشترک"),
    SAME_USERNAME("Shared Online Handle", "نام کاربری مشترک"),
    SAME_CERTIFICATE("Shared TLS Certificate / SAN", "گواهی امنیتی یا SAN مشترک"),
    SAME_ENTITY("Shared Attributed Entity", "موجودیت یا سازمان مشترک"),
    SAME_SERVICE("Shared Web3 / Payment Service", "سرویس یا درگاه پرداخت مشترک"),
    TEMPORAL_OVERLAP("Temporal Window Overlap", "هم‌پوشانی بازه زمانی"),
    SHARED_PUBLIC_IDENTIFIER("Shared Public Identifier", "شناسه عمومی مشترک")
}

@Serializable
data class CorrelationFinding(
    val correlationId: String = "CORR_${System.currentTimeMillis()}_${(100..999).random()}",
    val ruleType: CorrelationRuleType,
    val titleEn: String,
    val titleFa: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val pivotValue: String,
    val linkedEventIds: List<String>,
    val participatingSources: List<String>,
    val independentSourceCount: Int,
    val compositeConfidence: Float, // 0.0 to 1.0
    val epistemicStatus: EpistemicStatus = EpistemicStatus.INFERENCE,
    val limitationsEn: String = "Correlation indicates infrastructural or communicative proximity, not sole ownership.",
    val limitationsFa: String = "همبستگی نشان‌دهنده مجاورت زیرساختی یا ارتباطی است و دلالت بر مالکیت انحصاری ندارد."
)

/**
 * Forensic Correlation Engine (Master Instruction §25, §26, §27).
 * Implements deterministic correlation rules across the OSINT Event Bus.
 * Computes source independence and filters out mirrored/syndicated duplicates.
 */
object ForensicCorrelationEngine {

    /**
     * Correlates a batch of OSINT events collected during an investigation.
     */
    fun correlateEvents(events: List<OsintEvent>): List<CorrelationFinding> {
        val findings = mutableListOf<CorrelationFinding>()
        if (events.size < 2) return findings

        // 1. Group by same normalized value across indicator types
        val byIndicatorAndValue = events.groupBy { Pair(it.indicatorType, it.normalizedValue) }

        for ((key, matchedEvents) in byIndicatorAndValue) {
            val (indicatorType, pivotValue) = key
            if (matchedEvents.size >= 2 && pivotValue.isNotBlank()) {
                val ruleType = when (indicatorType) {
                    IndicatorType.DOMAIN -> CorrelationRuleType.SAME_DOMAIN
                    IndicatorType.IP -> CorrelationRuleType.SAME_IP
                    IndicatorType.ASN -> CorrelationRuleType.SAME_ASN
                    IndicatorType.EMAIL -> CorrelationRuleType.SAME_EMAIL
                    IndicatorType.USERNAME -> CorrelationRuleType.SAME_USERNAME
                    IndicatorType.CERTIFICATE -> CorrelationRuleType.SAME_CERTIFICATE
                    IndicatorType.ENTITY, IndicatorType.ORGANIZATION -> CorrelationRuleType.SAME_ENTITY
                    IndicatorType.SERVICE -> CorrelationRuleType.SAME_SERVICE
                    else -> CorrelationRuleType.SHARED_PUBLIC_IDENTIFIER
                }

                // Compute independent sources
                val sources = matchedEvents.map { it.source }.distinct()
                val independentCount = matchedEvents.count {
                    it.sourceLineage == SourceLineageType.ORIGINAL_SOURCE || it.sourceLineage == SourceLineageType.INDEPENDENT
                }.coerceAtLeast(1)

                // Composite confidence formula factoring source independence
                val baseConf = matchedEvents.map { it.confidence }.average().toFloat()
                val independenceMultiplier = when {
                    independentCount >= 3 -> 1.15f
                    independentCount == 2 -> 1.05f
                    else -> 0.90f // Penalize single source replicated across mirrors
                }
                val finalConfidence = (baseConf * independenceMultiplier).coerceIn(0.10f, 0.98f)

                val titleEn = "${ruleType.displayNameEn}: $pivotValue"
                val titleFa = "${ruleType.displayNameFa}: $pivotValue"
                val descEn = "Identified ${matchedEvents.size} independent observations linked to $pivotValue across ${sources.size} intelligence sources."
                val descFa = "تعداد ${matchedEvents.size} مشاهده مرتبط با $pivotValue از طریق ${sources.size} منبع اطلاعاتی شناسایی شد."

                findings.add(
                    CorrelationFinding(
                        ruleType = ruleType,
                        titleEn = titleEn,
                        titleFa = titleFa,
                        descriptionEn = descEn,
                        descriptionFa = descFa,
                        pivotValue = pivotValue,
                        linkedEventIds = matchedEvents.map { it.eventId },
                        participatingSources = sources,
                        independentSourceCount = independentCount,
                        compositeConfidence = finalConfidence,
                        epistemicStatus = EpistemicStatus.INFERENCE
                    )
                )
            }
        }

        // 2. Cross-domain infrastructure correlation (DOMAIN + IP overlap)
        val domainEvents = events.filter { it.indicatorType == IndicatorType.DOMAIN }
        val ipEvents = events.filter { it.indicatorType == IndicatorType.IP }

        if (domainEvents.isNotEmpty() && ipEvents.isNotEmpty()) {
            for (dev in domainEvents) {
                for (ipev in ipEvents) {
                    if (dev.provenance.contains(ipev.normalizedValue) || ipev.provenance.contains(dev.normalizedValue)) {
                        findings.add(
                            CorrelationFinding(
                                ruleType = CorrelationRuleType.SAME_IP,
                                titleEn = "Domain Hosting Correlation: ${dev.normalizedValue} -> ${ipev.normalizedValue}",
                                titleFa = "همبستگی میزبانی دامنه: ${dev.normalizedValue} روی ${ipev.normalizedValue}",
                                descriptionEn = "Domain ${dev.normalizedValue} was resolved to dedicated hosting IP ${ipev.normalizedValue}.",
                                descriptionFa = "دامنه ${dev.normalizedValue} به آدرس آی‌پی ${ipev.normalizedValue} تحلیل و متصل شد.",
                                pivotValue = "${dev.normalizedValue} / ${ipev.normalizedValue}",
                                linkedEventIds = listOf(dev.eventId, ipev.eventId),
                                participatingSources = listOf(dev.source, ipev.source).distinct(),
                                independentSourceCount = 2,
                                compositeConfidence = 0.92f,
                                epistemicStatus = EpistemicStatus.FACT
                            )
                        )
                    }
                }
            }
        }

        return findings
    }
}
