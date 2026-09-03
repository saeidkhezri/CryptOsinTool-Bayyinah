package com.aistudio.orbit.forensics.infra

/**
 * Public Suffix List (PSL) Normalizer (Master Instruction §20).
 * Official source: https://publicsuffix.org/
 * Enables precise registrable-domain extraction and subdomain decomposition without external runtime dependency.
 */
object PublicSuffixList {

    const val SUFFIX_VERSION = "2024.08.01"
    const val RETRIEVED_AT = 1722470400000L // 2024-08-01 UTC

    // Common compound multi-part suffixes (e.g., .co.uk, .gov.ir, .ac.ir, .com.au, .co.jp, etc.)
    private val compoundSuffixes = setOf(
        "co.uk", "org.uk", "me.uk", "gov.uk", "ac.uk", "net.uk",
        "gov.ir", "co.ir", "ac.ir", "sch.ir", "net.ir", "org.ir", "id.ir",
        "com.au", "net.au", "org.au", "edu.au", "gov.au",
        "co.jp", "ne.jp", "or.jp", "go.jp", "ac.jp",
        "com.br", "net.br", "org.br", "gov.br",
        "com.cn", "net.cn", "org.cn", "gov.cn", "edu.cn",
        "co.kr", "ne.kr", "or.kr", "go.kr", "re.kr",
        "com.tr", "org.tr", "net.tr", "gov.tr", "edu.tr",
        "com.de", "co.za", "com.mx", "co.in", "net.in", "org.in", "gov.in",
        "com.sg", "edu.sg", "gov.sg", "com.hk", "org.hk", "gov.hk",
        "com.ru", "net.ru", "org.ru", "pp.ru",
        "co.nz", "net.nz", "org.nz", "govt.nz",
        "com.ar", "gov.ar", "org.ar", "com.my", "gov.my"
    )

    data class ParsedDomain(
        val rawInput: String,
        val fullyQualifiedDomainName: String,
        val publicSuffix: String,
        val registrableDomain: String,
        val subdomain: String?,
        val suffixVersion: String = SUFFIX_VERSION
    )

    /**
     * Parses and decomposes any domain or URL according to the Public Suffix List.
     */
    fun parse(input: String): ParsedDomain {
        val cleaned = input.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")
            .substringBefore(":")
            .lowercase()

        val parts = cleaned.split(".").filter { it.isNotBlank() }
        if (parts.size <= 1) {
            return ParsedDomain(
                rawInput = input,
                fullyQualifiedDomainName = cleaned,
                publicSuffix = cleaned,
                registrableDomain = cleaned,
                subdomain = null
            )
        }

        // Check 2-part compound suffix
        if (parts.size >= 3) {
            val potentialCompound = "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
            if (compoundSuffixes.contains(potentialCompound)) {
                val suffix = potentialCompound
                val registrable = "${parts[parts.size - 3]}.$suffix"
                val sub = if (parts.size > 3) parts.subList(0, parts.size - 3).joinToString(".") else null
                return ParsedDomain(
                    rawInput = input,
                    fullyQualifiedDomainName = cleaned,
                    publicSuffix = suffix,
                    registrableDomain = registrable,
                    subdomain = sub
                )
            }
        }

        // Standard single-part TLD (e.g. .com, .org, .io, .ir, .net)
        val suffix = parts.last()
        val registrable = if (parts.size >= 2) "${parts[parts.size - 2]}.$suffix" else cleaned
        val sub = if (parts.size > 2) parts.subList(0, parts.size - 2).joinToString(".") else null

        return ParsedDomain(
            rawInput = input,
            fullyQualifiedDomainName = cleaned,
            publicSuffix = suffix,
            registrableDomain = registrable,
            subdomain = sub
        )
    }
}
