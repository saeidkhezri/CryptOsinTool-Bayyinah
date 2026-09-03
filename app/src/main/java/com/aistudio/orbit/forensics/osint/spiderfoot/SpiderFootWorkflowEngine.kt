package com.aistudio.orbit.forensics.osint.spiderfoot

import com.aistudio.orbit.db.EpistemicStatus
import com.aistudio.orbit.forensics.infra.InfrastructureIntelEngine
import com.aistudio.orbit.forensics.infra.PublicSuffixList
import com.aistudio.orbit.forensics.osint.bus.*
import com.aistudio.orbit.forensics.osint.contract.OsintExecutionContext
import org.json.JSONObject

/**
 * SpiderFoot-style Event-Driven Reconnaissance Workflow Orchestrator (Master Instruction §6).
 * Repository: https://github.com/smicallef/spiderfoot
 * Automatically cascades discoveries:
 * DOMAIN_FOUND → DNS → IP_FOUND → ASN_FOUND → GEOIP → CERTIFICATE → RELATED_DOMAIN
 */
object SpiderFootWorkflowEngine {

    /**
     * Executes an event-driven SpiderFoot cascade for an initial indicator event.
     * Recursively enriches infrastructure and emits correlated events.
     */
    suspend fun executeCascade(
        initialEvent: OsintEvent,
        context: OsintExecutionContext,
        maxDepth: Int = 2
    ): List<OsintEvent> {
        val discoveredEvents = mutableListOf<OsintEvent>()
        if (context.isOfflineOnly) return discoveredEvents

        when (initialEvent.indicatorType) {
            IndicatorType.DOMAIN -> {
                val domain = initialEvent.normalizedValue
                val parsed = PublicSuffixList.parse(domain)

                // 1. DNS A Records (DOMAIN -> IP)
                val aRecords = InfrastructureIntelEngine.resolveDns(parsed.fullyQualifiedDomainName, "A")
                for (rec in aRecords) {
                    val ipEvent = OsintEvent(
                        caseId = initialEvent.caseId,
                        investigationId = initialEvent.investigationId,
                        source = "SpiderFoot (DNS Resolver)",
                        indicatorType = IndicatorType.IP,
                        indicatorValue = rec.value,
                        normalizedValue = rec.value,
                        observedAt = System.currentTimeMillis(),
                        confidence = 0.90f,
                        provenance = "DNS A record for ${parsed.fullyQualifiedDomainName} via ${rec.resolver}",
                        resultState = OsintResultState.DISCOVERED,
                        epistemicStatus = EpistemicStatus.FACT,
                        sourceLineage = SourceLineageType.ORIGINAL_SOURCE,
                        payloadJson = JSONObject().apply {
                            put("recordType", "A")
                            put("ttl", rec.ttl)
                            put("domain", parsed.fullyQualifiedDomainName)
                        }.toString(),
                        tags = listOf("SPIDERFOOT", "DNS", "IP_FOUND")
                    )
                    discoveredEvents.add(ipEvent)

                    // 2. IP -> ASN & GeoIP (IP -> ASN / GEOIP)
                    if (maxDepth > 1) {
                        val geo = InfrastructureIntelEngine.resolveIpGeoAsn(rec.value)
                        if (geo != null) {
                            if (!geo.asn.isNullOrBlank()) {
                                val asnEvent = OsintEvent(
                                    caseId = initialEvent.caseId,
                                    investigationId = initialEvent.investigationId,
                                    source = "SpiderFoot (GeoIP & BGP ASN)",
                                    indicatorType = IndicatorType.ASN,
                                    indicatorValue = "${geo.asn} (${geo.org ?: "Autonomous System"})",
                                    normalizedValue = geo.asn,
                                    observedAt = System.currentTimeMillis(),
                                    confidence = 0.85f,
                                    provenance = "BGP Route Announcement for IP ${rec.value}",
                                    resultState = OsintResultState.DISCOVERED,
                                    epistemicStatus = EpistemicStatus.FACT,
                                    sourceLineage = SourceLineageType.ORIGINAL_SOURCE,
                                    payloadJson = JSONObject().apply {
                                        put("ip", rec.value)
                                        put("country", geo.countryName)
                                        put("countryCode", geo.countryCode)
                                        put("org", geo.org)
                                        put("isHosting", geo.isHostingOrVpn)
                                    }.toString(),
                                    tags = listOf("SPIDERFOOT", "ASN_FOUND", "BGP")
                                )
                                discoveredEvents.add(asnEvent)
                            }
                        }
                    }
                }

                // 3. DNS MX Records (DOMAIN -> MX -> Related Domain / Mail Provider)
                val mxRecords = InfrastructureIntelEngine.resolveDns(parsed.registrableDomain, "MX")
                for (rec in mxRecords) {
                    val mxDomain = rec.value.substringAfter(" ").trim().removeSuffix(".")
                    val mxEvent = OsintEvent(
                        caseId = initialEvent.caseId,
                        investigationId = initialEvent.investigationId,
                        source = "SpiderFoot (MX Inspector)",
                        indicatorType = IndicatorType.SERVICE,
                        indicatorValue = "Mail Exchange: $mxDomain for ${parsed.registrableDomain}",
                        normalizedValue = mxDomain,
                        observedAt = System.currentTimeMillis(),
                        confidence = 0.85f,
                        provenance = "DNS MX query for ${parsed.registrableDomain}",
                        resultState = OsintResultState.DISCOVERED,
                        epistemicStatus = EpistemicStatus.FACT,
                        sourceLineage = SourceLineageType.ORIGINAL_SOURCE,
                        tags = listOf("SPIDERFOOT", "MX_RECORD")
                    )
                    discoveredEvents.add(mxEvent)
                }

                // 4. Certificate Transparency Logs (DOMAIN -> CERTIFICATE -> RELATED_DOMAIN)
                val certs = InfrastructureIntelEngine.queryCertificateTransparency(parsed.registrableDomain)
                for (cert in certs) {
                    for (san in cert.matchingIdentities) {
                        if (san != parsed.fullyQualifiedDomainName && san.contains(parsed.registrableDomain)) {
                            val sanEvent = OsintEvent(
                                caseId = initialEvent.caseId,
                                investigationId = initialEvent.investigationId,
                                source = "SpiderFoot (Certificate Transparency crt.sh)",
                                indicatorType = IndicatorType.DOMAIN,
                                indicatorValue = san,
                                normalizedValue = san,
                                observedAt = System.currentTimeMillis(),
                                confidence = 0.95f,
                                provenance = "CT Log entry issued by ${cert.issuerName}",
                                resultState = OsintResultState.DISCOVERED,
                                epistemicStatus = EpistemicStatus.FACT,
                                sourceLineage = SourceLineageType.ORIGINAL_SOURCE,
                                tags = listOf("SPIDERFOOT", "CERTIFICATE_SAN", "SUBDOMAIN")
                            )
                            discoveredEvents.add(sanEvent)
                        }
                    }
                }
            }

            IndicatorType.IP -> {
                val ip = initialEvent.normalizedValue
                val geo = InfrastructureIntelEngine.resolveIpGeoAsn(ip)
                if (geo != null && !geo.asn.isNullOrBlank()) {
                    val asnEvent = OsintEvent(
                        caseId = initialEvent.caseId,
                        investigationId = initialEvent.investigationId,
                        source = "SpiderFoot (BGP Route Origin)",
                        indicatorType = IndicatorType.ASN,
                        indicatorValue = "${geo.asn} (${geo.org ?: ""})",
                        normalizedValue = geo.asn,
                        observedAt = System.currentTimeMillis(),
                        confidence = 0.85f,
                        provenance = "IP route origin for $ip",
                        resultState = OsintResultState.DISCOVERED,
                        epistemicStatus = EpistemicStatus.FACT,
                        sourceLineage = SourceLineageType.ORIGINAL_SOURCE,
                        tags = listOf("SPIDERFOOT", "ASN_FOUND")
                    )
                    discoveredEvents.add(asnEvent)
                }
            }

            else -> {
                // Other types handled by dedicated adapters
            }
        }

        return discoveredEvents
    }
}
