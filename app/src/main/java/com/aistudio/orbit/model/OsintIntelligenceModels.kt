package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

/**
 * ============================================================================
 * BIYENA OSINT & INVESTIGATION INTELLIGENCE MODEL
 * ============================================================================
 * Strict Epistemic Taxonomy, Multi-Seed Classification, Provider Architecture,
 * Entity Resolution, Confidence Formulation, Hypotheses, Leads & Conflicts.
 */

// ----------------------------------------------------------------------------
// 1. EPISTEMIC TAXONOMY (Strict separation of facts, observations & inferences)
// ----------------------------------------------------------------------------

@Serializable
enum class EpistemicType(
    val displayNameEn: String,
    val displayNameFa: String,
    val descriptionEn: String,
    val descriptionFa: String
) {
    FACT(
        "Cryptographic / Ledger Fact",
        "حقیقت قطعی دفترکل / رمزنگاری",
        "Deterministic, immutable record directly on the blockchain or cryptographic proof.",
        "رکورد قطعی و تغییرناپذیر ثبت‌شده در بلاک‌چین یا اثبات رمزنگاری‌شده."
    ),
    OBSERVATION(
        "Direct Public Observation",
        "مشاهده مستقیم منبع عمومی",
        "Data directly observed on a publicly accessible web resource or registry at a given time.",
        "داده‌هایی که مستقیماً در یک منبع یا پایگاه عمومی در یک بازه زمانی مشاهده شده‌اند."
    ),
    SOURCE_DATA(
        "Raw Source Data",
        "داده خام منبع",
        "Verbatim raw payload collected from an external intelligence provider.",
        "داده خام و دست‌نخورده دریافتی از ارائه‌دهنده اطلاعات خارجی."
    ),
    DERIVED_DATA(
        "Algorithmic Derivation",
        "مشتقات الگوریتمی",
        "Normalized, transformed, or computed metrics based on direct observations.",
        "شاخص‌های نرمال‌شده، تبدیل‌یافته یا محاسبه‌شده بر اساس مشاهدات مستقیم."
    ),
    CORRELATION(
        "Cross-Source Correlation",
        "همبستگی چندمنبعی",
        "Statistical, temporal, or network intersection identified between multiple sources.",
        "تقاطع آماری، زمانی یا شبکه‌ای شناسایی‌شده میان چندین منبع مستقل."
    ),
    HEURISTIC(
        "Behavioral / Clustering Heuristic",
        "هیوریستیک رفتاری / خوشه‌بندی",
        "Probabilistic clustering rule (e.g. CIOH, peel chain, change address detection).",
        "قاعده احتمالاتی خوشه‌بندی (نظیر مالکیت مشترک ورودی‌ها یا تشخیص آدرس باقی‌مانده)."
    ),
    INFERENCE(
        "Analytical Inference",
        "استنتاج تحلیلی",
        "Deductive or inductive analytical reasoning based on corroborated evidence.",
        "استدلال استنتاجی یا استقرایی مبتنی بر ادله چندگانه (غیرقطعی)."
    ),
    HYPOTHESIS(
        "Investigative Hypothesis",
        "فرضیه کارشناسی پرونده",
        "Plausible proposition formulated for testing against supporting and contradictory evidence.",
        "گزاره محتمل فرمول‌بندی‌شده جهت ارزیابی در برابر ادله موافق و مخالف."
    ),
    FINDING(
        "Verified Finding",
        "یافته اثبات‌شده",
        "Substantiated conclusion supported by verified evidence meeting confidence thresholds.",
        "نتیجه مدلل و متکی به ادله راستی‌آزمایی‌شده که حدنصاب اطمینان را احراز کرده است."
    ),
    CONCLUSION(
        "Reportable Legal / Audit Conclusion",
        "جمع‌بندی نهایی حقوقی / گزارش",
        "Formal investigative conclusion suitable for judicial or regulatory reporting.",
        "جمع‌بندی نهایی و رسمی قابل ارجاع در مراجع قضایی و نظارتی."
    )
}

// ----------------------------------------------------------------------------
// 2. SEED TYPES & INPUT CLASSIFICATION
// ----------------------------------------------------------------------------

@Serializable
enum class InvestigationSeedType(val displayNameEn: String, val displayNameFa: String, val iconName: String) {
    BITCOIN_ADDRESS("Bitcoin Address", "آدرس بیت‌کوین", "currency_bitcoin"),
    EVM_ADDRESS("EVM / Ethereum Address", "آدرس اتریوم / EVM", "token"),
    TRON_ADDRESS("TRON Address", "آدرس ترون", "toll"),
    SOLANA_ADDRESS("Solana Address", "آدرس سولانا", "account_balance_wallet"),
    TRANSACTION_ID("Transaction Hash / TXID", "هش تراکنش / TXID", "receipt_long"),
    DOMAIN("Domain / FQDN", "دامنه اینترنتی", "language"),
    URL("Web URL / Endpoint", "آدرس وب / URL", "link"),
    EMAIL("Email Address", "پست الکترونیک", "email"),
    USERNAME("Online Handle / Username", "نام کاربری / شناسه", "alternate_email"),
    IP_ADDRESS("IP Address (IPv4/IPv6)", "آدرس آی‌پی (IPv4/IPv6)", "router"),
    ASN("Autonomous System Number (ASN)", "شماره سامانه خودمختار (ASN)", "dns"),
    ORGANIZATION_NAME("Organization / Company", "سازمان / شرکت", "business"),
    ENTITY_NAME("Entity / Person Name", "نام شخص / موجودیت", "person"),
    VASP_EXCHANGE_NAME("VASP / Exchange Name", "صرافی / ارائه‌دهنده VASP", "account_balance"),
    CRYPTO_SERVICE_NAME("Cryptocurrency Service", "سرویس رمزارزی", "miscellaneous_services"),
    SOCIAL_PROFILE("Public Social Profile", "پروفایل شبکه اجتماعی", "public"),
    FORUM_PROFILE("Public Forum Profile", "پروفایل تالار گفتگو", "forum"),
    GITHUB_PROFILE("Public GitHub / Dev Account", "حساب گیت‌هاب / توسعه‌دهنده", "code"),
    TELEGRAM_IDENTITY("Public Telegram Handle / Channel", "شناسه / کانال تلگرام", "send"),
    DISCORD_IDENTITY("Public Discord Handle", "شناسه عمومی دیسکورد", "tag"),
    ONION_URL("Public Onion / Tor Link", "پیوند پیازی / Tor", "security"),
    KEYWORD("Investigator Keyword", "کلیدواژه بازرس", "search"),
    CUSTOM_ENTITY("Investigator-Defined Entity", "موجودیت سفارشی بازرس", "badge"),
    ARTIFACT("Imported OSINT Artifact", "مدرک واردشده OSINT", "attach_file")
}

@Serializable
data class InvestigationSeed(
    val seedId: String = "SEED_${System.currentTimeMillis()}_${(1000..9999).random()}",
    val type: InvestigationSeedType,
    val rawValue: String,
    val normalizedValue: String,
    val customLabel: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val isValid: Boolean = true,
    val validationMessageEn: String? = null,
    val validationMessageFa: String? = null
)

// ----------------------------------------------------------------------------
// 3. SOURCE QUALITY GRADES & SOURCE INDEPENDENCE
// ----------------------------------------------------------------------------

@Serializable
enum class SourceQualityGrade(
    val grade: String,
    val displayNameEn: String,
    val displayNameFa: String,
    val reliabilityMultiplier: Float,
    val descriptionEn: String,
    val descriptionFa: String
) {
    A_PRIMARY_DIRECT(
        "A",
        "Primary / Direct Official Source",
        "منبع دست اول / رسمی مستقیم",
        1.0f,
        "Official regulatory filing, full-node blockchain event, signed cryptographic proof, verified entity self-disclosure.",
        "گزارش رسمی رگولاتوری، رویداد نود کامل بلاک‌چین، امضای رمزنگاری یا خودافشایی تاییدشده نهاد."
    ),
    B_REPUTABLE_SECONDARY(
        "B",
        "Reputable Secondary Source",
        "منبع معتبر دست دوم",
        0.85f,
        "Recognized cybersecurity intelligence feed (MISP, OpenSanctions), verified exchange announcement, established explorer API.",
        "فیدهای اطلاعاتی معتبر سایبری (MISP، OpenSanctions)، اطلاعیه تاییدشده صرافی یا کاوشگرهای شناخته‌شده."
    ),
    C_USEFUL_CORROBORATION(
        "C",
        "Useful Corroboration",
        "هم‌راستایی و تایید مفید",
        0.65f,
        "Public forum threads with technical details (Bitcointalk), GitHub repositories, public social profiles with historical footprint.",
        "تاپیک‌های فروم با جزئیات فنی (بیت‌کوین‌تاک)، مخازن گیت‌هاب و پروفایل‌های عمومی با پیشینه تاریخی."
    ),
    D_WEAK_UNVERIFIED(
        "D",
        "Weak / Unverified Source",
        "منبع ضعیف / تاییدنشده",
        0.35f,
        "Single blog post, search engine snippet, crowdsourced forum comment without provenance or transaction linkage.",
        "پست وبلاگی منفرد، قطعه متنی موتور جستجو، نظر کاربر در فروم بدون اثبات تراکنشی یا شجره داده."
    ),
    E_UNRELIABLE_CONTRADICTORY(
        "E",
        "Unreliable / Contradictory Source",
        "منبع غیرقابل اتکا / دارای تعارض",
        0.10f,
        "Anonymously submitted unvetted claim, syndicated copy-paste without original citation, disproven claim.",
        "ادعای بدون نام و نشان، کپی بازنشرشده بدون ارجاع به منبع اصلی، یا ادعای ردشده توسط ادله قطعی."
    )
}

// ----------------------------------------------------------------------------
// 4. ENTITY RESOLUTION & ATTRIBUTION CLASSES
// ----------------------------------------------------------------------------

@Serializable
enum class ForensicEntityClass(
    val code: String,
    val displayNameEn: String,
    val displayNameFa: String,
    val defaultRiskWeight: Int
) {
    PERSON("PERSON", "Natural Person", "شخص حقیقی", 20),
    ORGANIZATION("ORGANIZATION", "Legal Organization / Company", "شخصیت حقوقی / شرکت", 25),
    EXCHANGE("EXCHANGE", "Centralized Exchange (CEX)", "صرافی متمرکز", 30),
    VASP("VASP", "Virtual Asset Service Provider (VASP)", "ارائه‌دهنده خدمات دارایی مجازی (VASP)", 35),
    MERCHANT("MERCHANT", "Commercial Merchant / Gateway", "پذیرنده تجاری / درگاه پرداخت", 20),
    SERVICE("SERVICE", "Infrastructure / Web3 Service", "سرویس زیرساختی / وب۳", 25),
    MIXER("MIXER", "Anonymizing Mixer / Tumbler", "سرویس میکسر / ناشناس‌ساز", 90),
    MINING_POOL("MINING_POOL", "Mining Pool / Validator", "استخر استخراج / اعتبارسنج", 15),
    GAMBLING_SERVICE("GAMBLING_SERVICE", "Online Gambling / Casino", "سایت شرط‌بندی / قمار آنلاین", 75),
    DARKWEB_SERVICE("DARKWEB_SERVICE", "Darknet Marketplace / Hidden Service", "سرویس مخفی / مارکت دارک‌وب", 95),
    SCAM("SCAM", "Reported Fraud / Ponzi / Phishing", "طرح کلاهبرداری / پانزی / فیشینگ", 85),
    RANSOMWARE("RANSOMWARE", "Ransomware / Extortion Actor", "باج‌افزار / اخاذی دیجیتال", 95),
    CHARITY("CHARITY", "Non-Profit / Humanitarian Charity", "خیریه / نهاد مردم‌نهاد", 10),
    DEVELOPER("DEVELOPER", "Smart Contract / Core Developer", "توسعه‌دهنده قرارداد / هسته", 15),
    PROJECT("PROJECT", "DeFi / Protocol Project", "پروژه دیفای / پروتکل", 20),
    UNKNOWN("UNKNOWN", "Unclassified / Unknown Entity", "نامشخص / طبقه‌بندی‌نشده", 40)
}

@Serializable
enum class AddressControlRelation(
    val code: String,
    val displayNameEn: String,
    val displayNameFa: String,
    val evidentiaryWeight: Float
) {
    CONTROLLED_BY("CONTROLLED_BY", "Strictly Controlled By", "کنترل قطعی توسط", 1.0f),
    OPERATED_BY("OPERATED_BY", "Operated / Hosted By", "بهره‌برداری / میزبانی توسط", 0.90f),
    SELF_PUBLISHED_BY("SELF_PUBLISHED_BY", "Self-Published On Official Channel", "خودافشایی در کانال رسمی", 0.85f),
    ATTRIBUTED_TO("ATTRIBUTED_TO", "Attributed By Threat Intel / Tags", "انتساب‌یافته بر اساس برچسب‌های اطلاعاتی", 0.70f),
    LABELED_AS("LABELED_AS", "Publicly Labeled As", "برچسب‌گذاری عمومی به عنوان", 0.55f),
    USED_BY("USED_BY", "Transacted / Used By", "مورد استفاده یا تعامل با", 0.50f),
    POTENTIAL_CONTROL("POTENTIAL_CONTROL", "Potential Control Candidate", "کاندیدای احتمالی کنترل", 0.40f),
    ASSOCIATED_WITH("ASSOCIATED_WITH", "Indirect Association", "ارتباط غیرمستقیم / محیطی", 0.30f)
}

@Serializable
enum class BehavioralRiskCategory(
    val code: String,
    val displayNameEn: String,
    val displayNameFa: String,
    val severityLevel: String
) {
    ROUTINE_LOW_RISK("ROUTINE_LOW_RISK", "Routine / Low-Risk Activity", "فعالیت عادی و کم‌خطر", "LOW"),
    POTENTIALLY_SUSPICIOUS("POTENTIALLY_SUSPICIOUS", "Potentially Suspicious Pattern", "الگوی بالقوه مشکوک", "MEDIUM"),
    HIGH_RISK_EXPOSURE("HIGH_RISK_EXPOSURE", "High-Risk Network Exposure", "مجاورت با شبکه‌های پرخطر", "HIGH"),
    SANCTIONS_EXPOSURE("SANCTIONS_EXPOSURE", "Sanctions / Watchlist Exposure", "تطابق با فهرست‌های تحریمی", "CRITICAL"),
    SCAM_REPORTS("SCAM_REPORTS", "Corroborated Scam Reports", "گزارش‌های مستند کلاهبرداری", "HIGH"),
    RANSOMWARE_EXPOSURE("RANSOMWARE_EXPOSURE", "Ransomware / Extortion Exposure", "ردپای باج‌افزاری / اخاذی", "CRITICAL"),
    MIXER_EXPOSURE("MIXER_EXPOSURE", "Direct / Hop Mixer Interaction", "تعامل مستقیم یا باواسطه با میکسر", "HIGH"),
    DARKWEB_EXPOSURE("DARKWEB_EXPOSURE", "Darknet Market Exposure", "ارتباط با بازارهای دارک‌وب", "CRITICAL"),
    UNKNOWN("UNKNOWN", "Insufficient Risk Indicators", "نشانگرهای ناکافی برای ارزیابی ریسک", "INFO")
}

@Serializable
enum class EntityRelationType(val displayNameEn: String, val displayNameFa: String) {
    MENTIONS("Mentions Identifier", "اشاره به شناسه"),
    SELF_PUBLISHED("Self-Published By", "خودافشایی توسط"),
    ASSOCIATED_WITH("Associated With", "مرتبط با"),
    RESOLVES_TO("Resolves To", "تحلیل نام به"),
    HOSTED_BY("Hosted / Hosted On", "میزبانی‌شده بر روی"),
    BELONGS_TO_CLUSTER("Belongs To Cluster", "متعلق به خوشه"),
    ATTRIBUTED_TO("Attributed To", "منتسب به"),
    USED_BY("Used By", "استفاده‌شده توسط"),
    REPORTED_BY("Reported By", "گزارش‌شده توسط"),
    LINKED_TO("Linked To", "متصل به"),
    OBSERVED_ON("Observed On", "مشاهده‌شده در"),
    REFERENCED_BY("Referenced By", "ارجاع‌داده‌شده توسط")
}

// ----------------------------------------------------------------------------
// 5. EVIDENCE OBJECT & PROVENANCE ARTIFACT
// ----------------------------------------------------------------------------

@Serializable
data class OsintEvidenceItem(
    val evidenceId: String = "EVD_${System.currentTimeMillis()}_${(1000..9999).random()}",
    val caseId: String,
    val sourceId: String,
    val providerName: String,
    val providerModule: String,
    val queryTarget: String,
    val sourceUrl: String = "",
    val title: String,
    val titleFa: String,
    val rawContentHash: String, // SHA-256 fingerprint of verbatim payload
    val collectionTimestamp: Long = System.currentTimeMillis(),
    val firstSeenTimestamp: Long? = null,
    val lastSeenTimestamp: Long? = null,
    val contentType: String = "text/plain",
    val extractedEntities: List<String> = emptyList(),
    val relationship: EntityRelationType = EntityRelationType.OBSERVED_ON,
    val sourceQuality: SourceQualityGrade = SourceQualityGrade.C_USEFUL_CORROBORATION,
    val epistemicType: EpistemicType = EpistemicType.OBSERVATION,
    val confidenceLevel: String = "MEDIUM", // CONFIRMED, HIGH, MEDIUM, LOW, UNCERTAIN, CONTESTED
    val numericConfidence: Int = 65, // 0 to 100
    val analystNote: String = "",
    val verificationStatus: String = "PENDING_REVIEW", // PENDING_REVIEW, VERIFIED, REJECTED, CONTESTED
    val isSyndicatedOrMirrored: Boolean = false,
    val parentLineageEvidenceId: String? = null
)

// ----------------------------------------------------------------------------
// 6. HYPOTHESES, LEADS & CONFLICTS
// ----------------------------------------------------------------------------

@Serializable
data class InvestigationHypothesis(
    val hypothesisId: String = "HYP_${(100..999).random()}",
    val titleEn: String,
    val titleFa: String,
    val propositionEn: String,
    val propositionFa: String,
    val supportingEvidenceIds: List<String> = emptyList(),
    val contradictingEvidenceIds: List<String> = emptyList(),
    val confidenceLevel: String = "MEDIUM", // CONFIRMED, HIGH, MEDIUM, LOW, CONTESTED
    val numericConfidence: Int = 50, // 0 to 100
    val status: String = "ACTIVE", // ACTIVE, CONFIRMED_BY_ANALYST, REFUTED, INSUFFICIENT_DATA
    val analystRationale: String = "",
    val generatedTimestamp: Long = System.currentTimeMillis()
)

@Serializable
data class InvestigationLead(
    val leadId: String = "LEAD_${(100..999).random()}",
    val targetValue: String,
    val targetType: InvestigationSeedType,
    val reasonEn: String,
    val reasonFa: String,
    val supportingEvidenceIds: List<String> = emptyList(),
    val confidenceScore: Int = 60, // 0 to 100
    val expectedValueEn: String,
    val expectedValueFa: String,
    val nextRecommendedStepEn: String,
    val nextRecommendedStepFa: String,
    val isActioned: Boolean = false
)

@Serializable
data class AttributionConflict(
    val conflictId: String = "CONF_${(100..999).random()}",
    val targetIndicator: String,
    val conflictDomain: String, // "ENTITY_ATTRIBUTION", "GEOGRAPHIC_ORIGIN", "SERVICE_ROLE"
    val candidateA: String,
    val candidateAFa: String,
    val candidateB: String,
    val candidateBFa: String,
    val confidenceA: Int, // e.g. 64%
    val confidenceB: Int, // e.g. 31%
    val unknownShare: Int = 5, // e.g. 5%
    val supportingEvidenceA: List<String> = emptyList(),
    val supportingEvidenceB: List<String> = emptyList(),
    val resolutionStatus: String = "UNRESOLVED", // UNRESOLVED, RESOLVED_A, RESOLVED_B, REJECTED_BOTH
    val analystReviewNotes: String = ""
)

// ----------------------------------------------------------------------------
// 7. GEOGRAPHIC & TEMPORAL SIGNALS
// ----------------------------------------------------------------------------

@Serializable
data class GeoSignal(
    val regionNameEn: String,
    val regionNameFa: String,
    val countryCode: String,
    val isCandidate: Boolean,
    val supportingIndicatorsCount: Int,
    val contradictoryIndicatorsCount: Int,
    val confidencePercent: Int,
    val signalSources: List<String> = emptyList(),
    val isVpnOrTorExit: Boolean = false,
    val precisionType: String = "COUNTRY_ESTIMATE" // COUNTRY_ESTIMATE, METROPOLITAN_ESTIMATE, ISP_REGISTERED_LOCATION
)

@Serializable
data class TemporalActivityProfile(
    val peakActiveHoursUtc: List<Int> = emptyList(),
    val activeDaysOfWeek: List<String> = emptyList(),
    val diurnalConsistencyScore: Float = 0.75f, // 0.0 to 1.0
    val burstTransactionFrequency: Boolean = false,
    val recurringPeriodicityHours: Int? = null,
    val candidateTimezoneOffsets: List<String> = emptyList(), // e.g. ["UTC+3:30 (Tehran)", "UTC+2:00 (Helsinki/Frankfurt)"]
    val notesEn: String = "Temporal pattern reflects business hours activity with weekend reduction.",
    val notesFa: String = "الگوی زمانی نشان‌دهنده فعالیت در ساعات اداری و کاهش در روزهای تعطیل است."
)

// ----------------------------------------------------------------------------
// 8. OPENCTI & MISP GALAXY KNOWLEDGE MODEL
// ----------------------------------------------------------------------------

@Serializable
data class OpenCtiObservable(
    val id: String = "OBS_${System.currentTimeMillis()}_${(100..999).random()}",
    val observableType: String, // "Cryptocurrency-Address", "Domain-Name", "Email-Address", "IPv4-Addr", "User-Account"
    val value: String,
    val standardLabels: List<String> = emptyList(),
    val firstSeen: String,
    val lastSeen: String,
    val confidence: Int = 75,
    val mispGalaxyThreatActors: List<String> = emptyList(),
    val mispGalaxyRansomware: List<String> = emptyList(),
    val openSanctionsMatches: List<String> = emptyList()
)
