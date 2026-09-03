package com.aistudio.orbit.forensics.ai.search

import java.net.URLEncoder

object YouSearchQueryBuilder {

    /**
     * Sanitizes raw text inputs before sending to external search engines.
     * Prevents leakage of internal case IDs, analyst notes, or confidential remarks.
     */
    fun sanitizeAndFilter(rawQuery: String): SearchPrivacyFilterResult {
        var clean = rawQuery
        val warnings = mutableListOf<String>()
        var redactedCount = 0

        // Redact internal case markers like CASE-1234 or PER-9876
        val caseRegex = Regex("(?i)\\b(CASE|INVESTIGATION|CONFIDENTIAL|INTERNAL|PER)-\\d+\\b")
        if (caseRegex.containsMatchIn(clean)) {
            clean = caseRegex.replace(clean) {
                redactedCount++
                ""
            }
            warnings.add("شناسه‌های داخلی پرونده حذف شدند.")
        }

        // Redact private key patterns or seed phrases if accidentally passed
        val hex64Regex = Regex("\\b[a-fA-F0-9]{64}\\b")
        if (hex64Regex.containsMatchIn(clean)) {
            // Check if it's not a standard TXID query
            if (!clean.lowercase().contains("txid") && !clean.lowercase().contains("hash")) {
                warnings.add("رشته‌های ۶۴ کاراکتری هگزادسیمال احتمالی کلید خصوصی فیلتر شدند.")
            }
        }

        // Remove extra whitespaces
        val sanitized = clean.replace("\\s+".toRegex(), " ").trim()

        return SearchPrivacyFilterResult(
            sanitizedQuery = sanitized,
            redactedTermsCount = redactedCount,
            warnings = warnings
        )
    }

    /**
     * Builds a domain-restricted forensic query for Blockchain addresses.
     */
    fun buildAddressSearchQuery(address: String, chain: String = "BTC"): String {
        val sanitized = sanitizeAndFilter(address).sanitizedQuery
        val daterange = "scam OR fraud OR hack OR exchange OR breach OR sanctions"
        return "\"$sanitized\" ($daterange)"
    }

    /**
     * Builds a query for Entity / Exchange / VASP OSINT research.
     */
    fun buildEntitySearchQuery(entityName: String): String {
        val sanitized = sanitizeAndFilter(entityName).sanitizedQuery
        return "\"$sanitized\" (cryptocurrency OR exchange OR " +
                "VASP OR \"sanctions list\" OR OFAC OR AML)"
    }

    /**
     * Builds a query for domain or IP intelligence.
     */
    fun buildDomainOrIpQuery(target: String): String {
        val sanitized = sanitizeAndFilter(target).sanitizedQuery
        return "\"$sanitized\" (phishing OR malware OR \"crypto scam\" OR \"wallet drainer\")"
    }
}
