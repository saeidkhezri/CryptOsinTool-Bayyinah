package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

@Serializable
enum class SourceReuseStatus(val labelEn: String, val labelFa: String) {
    ACTIVE_REUSE("Active Reimplementation / Integrated", "پیاده‌سازی و بازنویسی فعال"),
    REFERENCE_ONLY("Reference Specification Only", "صرفاً ارجاع و الگوی معماری"),
    ADAPTED("Adapted for Android / Kotlin", "بومی‌سازی‌شده برای اندروید و کاتلین"),
    PLANNED_PHASE_2("Scheduled for Phase 2", "برنامه‌ریزی‌شده برای فاز ۲"),
    PLANNED_PHASE_3("Scheduled for Phase 3", "برنامه‌ریزی‌شده برای فاز ۳"),
    PLANNED_PHASE_4("Scheduled for Phase 4", "برنامه‌ریزی‌شده برای فاز ۴"),
    PLANNED_PHASE_5("Scheduled for Phase 5", "برنامه‌ریزی‌شده برای فاز ۵"),
    PLANNED_PHASE_6("Scheduled for Phase 6", "برنامه‌ریزی‌شده برای فاز ۶"),
    PLANNED_PHASE_7("Scheduled for Phase 7", "برنامه‌ریزی‌شده برای فاز ۷"),
    REJECTED_OUT_OF_SCOPE("Excluded (Out of Scope / Private Keys)", "ردشده (خارج از دامنه جرم‌یابی عمومی)")
}

@Serializable
data class SourceRegistryEntry(
    val sourceId: String,
    val sourceName: String,
    val sourceType: String,
    val repositoryUrl: String,
    val documentationUrl: String,
    val versionOrBranch: String,
    val license: String,
    val primaryPurpose: String,
    val relevantModules: List<String>,
    val relevantAlgorithms: List<String>,
    val reuseStatus: SourceReuseStatus,
    val reuseReason: String,
    val licenseRestrictions: String,
    val implementationPhase: String,
    val notes: String
)

object SourceRegistry {
    val entries: List<SourceRegistryEntry> = listOf(
        SourceRegistryEntry(
            sourceId = "SOURCE-000",
            sourceName = "Base Forensic Application (Orbit Android)",
            sourceType = "Base Android Client Application",
            repositoryUrl = "https://github.com/aistudio/orbit-android-forensics",
            documentationUrl = "https://developer.android.com/jetpack/compose",
            versionOrBranch = "main / Phase-1",
            license = "MIT / Apache 2.0",
            primaryPurpose = "Core Android Forensic Architecture, MVVM State Management, Jetpack Compose UI, Room/Data Persistence, and Bilingual Persian/English Foundation",
            relevantModules = listOf("ui", "model", "provider", "forensics", "repository", "localization", "security"),
            relevantAlgorithms = listOf("Bilingual Directional UI", "State Flow Normalization", "Audit Logging Engine"),
            reuseStatus = SourceReuseStatus.ACTIVE_REUSE,
            reuseReason = "Provides foundational mobile application runtime, edge-to-edge UI, and high-performance Kotlin Coroutines orchestration.",
            licenseRestrictions = "Permissive MIT license allows direct integration with proper attribution.",
            implementationPhase = "Phase 1 - 7",
            notes = "Serves as the unified base project for all seven investigative stages."
        ),
        SourceRegistryEntry(
            sourceId = "SOURCE-001",
            sourceName = "Bitcoin Core Transaction & Consensus Reference",
            sourceType = "Blockchain Core Node Specification",
            repositoryUrl = "https://github.com/bitcoin/bitcoin",
            documentationUrl = "https://developer.bitcoin.org/reference/",
            versionOrBranch = "v27.x",
            license = "MIT License",
            primaryPurpose = "Standard UTXO transaction parsing, SegWit (BIP141/BIP173) & Taproot (BIP341/BIP350) address normalization, script decoding, and fee rate metrics",
            relevantModules = listOf("src/primitives/transaction", "src/script", "src/bech32", "src/base58"),
            relevantAlgorithms = listOf("Bech32/Bech32m Checksum", "Base58Check Decoding", "UTXO In/Out Flow Normalization"),
            reuseStatus = SourceReuseStatus.ADAPTED,
            reuseReason = "Adapted address validation and UTXO transaction decomposition logic into pure Kotlin without depending on native C++ builds or private wallet keystores.",
            licenseRestrictions = "MIT license permitting adaptation with attribution; no .dat wallet files utilized.",
            implementationPhase = "Phase 1 & Phase 2",
            notes = "Strictly excludes wallet.dat, keystore files, and private keys. Only public transaction and address structures are referenced."
        ),
        SourceRegistryEntry(
            sourceId = "SOURCE-002",
            sourceName = "GraphSense Blockchain Analytics Platform",
            sourceType = "Forensic Graph & Tagging Engine",
            repositoryUrl = "https://github.com/graphsense/graphsense-lib",
            documentationUrl = "https://graphsense.github.io/",
            versionOrBranch = "v24.x",
            license = "MIT License",
            primaryPurpose = "Heuristic address clustering (Common Input Ownership), multi-hop entity resolution, counterparty exposure matrices, and attribution tagpacks",
            relevantModules = listOf("graphsense.tagpacks", "graphsense.transform", "graphsense.heuristics"),
            relevantAlgorithms = listOf("Common Input Ownership Heuristic (CIOH)", "Change Address Heuristic", "Entity Direct Exposure Aggregator"),
            reuseStatus = SourceReuseStatus.ADAPTED,
            reuseReason = "Adapted clustering and tag matching concepts into mobile-friendly in-memory graph models and SQLite persistence.",
            licenseRestrictions = "MIT license; attribution retained in forensic documentation.",
            implementationPhase = "Phase 3, Phase 4 & Phase 6",
            notes = "Enables robust counterparty discovery and network graph topology visualization without heavy cluster server dependencies."
        ),
        SourceRegistryEntry(
            sourceId = "SOURCE-003",
            sourceName = "AML Graph Neural Network & Typology Frameworks (Elliptic Data)",
            sourceType = "Financial Crime Typology Research",
            repositoryUrl = "https://github.com/spraph/AML-GNN",
            documentationUrl = "https://arxiv.org/abs/1908.02591",
            versionOrBranch = "main",
            license = "Apache 2.0",
            primaryPurpose = "Statistical money laundering typology classification, structuring indicators, peel-chain heuristic rules, and velocity scoring",
            relevantModules = listOf("models/typologies", "features/temporal_velocity", "heuristics/layering"),
            relevantAlgorithms = listOf("Rapid Pass-Through Velocity Ratio", "Peel Chain Step Degradation", "Cyclic Transfer Loop Detection"),
            reuseStatus = SourceReuseStatus.ADAPTED,
            reuseReason = "Implemented deterministic and rule-based heuristic scoring functions with explicit confidence intervals.",
            licenseRestrictions = "Apache 2.0; modular algorithmic adaptation with clear epistemic classification.",
            implementationPhase = "Phase 4 & Phase 5",
            notes = "Classifications are strictly formulated as probabilistic behavioral indicators rather than definitive factual guilt."
        ),
        SourceRegistryEntry(
            sourceId = "SOURCE-004",
            sourceName = "BlockSci High-Performance Blockchain Analytics Engine",
            sourceType = "Blockchain Science & Analysis Tool",
            repositoryUrl = "https://github.com/citp/BlockSci",
            documentationUrl = "https://citp.github.io/BlockSci/",
            versionOrBranch = "v0.7.0",
            license = "GPL-3.0",
            primaryPurpose = "Reference for transaction graph traversals, UTXO lifespan tracking, change address heuristic validation, and diurnal time-zone affinity clustering",
            relevantModules = listOf("blocksci/heuristics", "blocksci/temporal"),
            relevantAlgorithms = listOf("Diurnal Activity Cosine Fit", "Change Output Selection Heuristic", "UTXO Age Distribution"),
            reuseStatus = SourceReuseStatus.REFERENCE_ONLY,
            reuseReason = "Architectural reference for temporal and diurnal activity modeling. Reimplemented in clean-room Kotlin without GPL code inclusion.",
            licenseRestrictions = "GPL-3.0 reference only; clean-room implementation avoids license contamination.",
            implementationPhase = "Phase 5 & Phase 6",
            notes = "Used purely as methodological reference for diurnal activity peak alignment across global time zones."
        ),
        SourceRegistryEntry(
            sourceId = "SOURCE-005",
            sourceName = "OpenSanctions & Public Intelligence Registries",
            sourceType = "Sanctions & Compliance Intelligence",
            repositoryUrl = "https://github.com/opensanctions/opensanctions",
            documentationUrl = "https://www.opensanctions.org/docs/",
            versionOrBranch = "latest",
            license = "CC-BY 4.0 / Open Data",
            primaryPurpose = "Public OFAC, EU, UN sanctions list address mapping, ransomware attribution tags, and high-risk entity matching",
            relevantModules = listOf("datasets/sanctions", "datasets/crypto_addresses"),
            relevantAlgorithms = listOf("Exact & Levenshtein Entity Name Matching", "Checksummed Address Lookup"),
            reuseStatus = SourceReuseStatus.ADAPTED,
            reuseReason = "Curated high-risk designation tags and sanctions indicators into an on-device offline forensic lookup engine.",
            licenseRestrictions = "CC-BY 4.0 requires attribution in exports and reports.",
            implementationPhase = "Phase 3 & Phase 7",
            notes = "Provides verifiable provenance records for all sanctions and blacklisted entity matches."
        ),
        SourceRegistryEntry(
            sourceId = "SOURCE-006",
            sourceName = "Multi-Chain & Rust-Bitcoin Reference Specifications",
            sourceType = "Cross-Chain Address & Serialization Standard",
            repositoryUrl = "https://github.com/rust-bitcoin/rust-bitcoin",
            documentationUrl = "https://docs.rs/bitcoin/latest/bitcoin/",
            versionOrBranch = "v0.32",
            license = "CC0-1.0 / Apache-2.0",
            primaryPurpose = "Multi-chain address identification rules for Bitcoin, Ethereum (EIP-55 checksum), TRON (Base58 TRX/TRC20), BNB Smart Chain, and Solana",
            relevantModules = listOf("address/validation", "crypto/keccak", "crypto/sha256"),
            relevantAlgorithms = listOf("EIP-55 Checksum Validation", "TRON Base58Check Validation", "Bech32m Taproot Validation"),
            reuseStatus = SourceReuseStatus.ACTIVE_REUSE,
            reuseReason = "Implemented robust Kotlin multi-chain address parser with unambiguous format disambiguation.",
            licenseRestrictions = "Permissive CC0/Apache-2.0.",
            implementationPhase = "Phase 1 & Phase 2",
            notes = "Ensures correct network identification before invoking EVM vs TRON vs UTXO pipeline."
        ),
        SourceRegistryEntry(
            sourceId = "SOURCE-007",
            sourceName = "SpiderFoot Open-Source Intelligence (OSINT) Automation Platform",
            sourceType = "OSINT Reconnaissance & Correlation Engine",
            repositoryUrl = "https://github.com/smicallef/spiderfoot",
            documentationUrl = "https://www.spiderfoot.net/documentation/",
            versionOrBranch = "v4.0.0",
            license = "MIT License",
            primaryPurpose = "Passive reconnaissance, DNS/Reverse IP resolution, Autonomous System Number (ASN) intelligence, Threat feed correlation (Abuse.ch, AlienVault OTX, Blocklist.de), and credential leak verification",
            relevantModules = listOf("modules/sfp_dns", "modules/sfp_whois", "modules/sfp_threatintel", "modules/sfp_haveibeenpwned", "modules/sfp_blockchain"),
            relevantAlgorithms = listOf("Passive DNS/PTR Traversal", "Threat Intel Multi-Feed Correlation", "Credential Leak Signature Matching"),
            reuseStatus = SourceReuseStatus.ADAPTED,
            reuseReason = "Adapted modular OSINT inspection patterns into Android Kotlin coroutines, enabling verified lookup across open threat intelligence feeds.",
            licenseRestrictions = "MIT license; attribution retained in forensic documentation and export reports.",
            implementationPhase = "Phase 4, Phase 6 & Phase 7",
            notes = "Provides verifiable provenance, ISP routing classification, and threat feed indicators with clear distinction between facts and inferences."
        ),
        SourceRegistryEntry(
            sourceId = "SOURCE-008",
            sourceName = "Blockchain Awesome OSINT & Intelligence Curations",
            sourceType = "Curated Crypto Forensic Feeds & Threat Directory",
            repositoryUrl = "https://github.com/OffensiveOsint/awesome-blockchain-osint",
            documentationUrl = "https://github.com/OffensiveOsint/awesome-blockchain-osint#readme",
            versionOrBranch = "main",
            license = "CC0-1.0 / MIT",
            primaryPurpose = "Curated directory of public blockchain explorers, public scam trackers (CryptoScamDB, BitcoinWhosWho), mixer and ransomware tagging heuristics, and open-source tagpacks",
            relevantModules = listOf("explorers/evm", "explorers/utxo", "threat_feeds/scam_databases", "heuristics/mixer_attribution"),
            relevantAlgorithms = listOf("Public Tagpack Normalization", "Scam Database Hash Cross-Matching", "Mempool Broadcast Geolocation Estimator"),
            reuseStatus = SourceReuseStatus.ADAPTED,
            reuseReason = "Integrated curated public directory heuristics and verifiable threat feed indicators directly into the OsintForensicsEngine and DefaultOsintProvider.",
            licenseRestrictions = "Permissive CC0/MIT.",
            implementationPhase = "Phase 1 - 7",
            notes = "Serves as the comprehensive benchmark for multi-chain OSINT indicators, ensuring all findings link to verifiable public sources."
        )
    )

    fun getEntryById(sourceId: String): SourceRegistryEntry? {
        return entries.find { it.sourceId.equals(sourceId, ignoreCase = true) }
    }
}
