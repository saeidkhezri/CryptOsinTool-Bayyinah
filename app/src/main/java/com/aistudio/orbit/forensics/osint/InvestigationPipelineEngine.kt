package com.aistudio.orbit.forensics.osint

import com.aistudio.orbit.model.*
import com.aistudio.orbit.provider.osint.PluggableOsintOrchestrator
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * ============================================================================
 * BIYENA ADVANCED OSINT INVESTIGATION & INTELLIGENCE PIPELINE ENGINE
 * ============================================================================
 * End-to-end evidence-driven forensic correlation and attribution subsystem.
 */

@Serializable
data class InvestigationExecutionSession(
    val sessionId: String = "SESS_${System.currentTimeMillis()}_${(1000..9999).random()}",
    val caseId: String,
    val initialSeeds: List<InvestigationSeed>,
    val collectedEvidence: List<OsintEvidenceItem> = emptyList(),
    val extractedEntities: List<ResolvedEntitySummary> = emptyList(),
    val hypotheses: List<InvestigationHypothesis> = emptyList(),
    val rankedLeads: List<InvestigationLead> = emptyList(),
    val conflicts: List<AttributionConflict> = emptyList(),
    val geoSignals: List<GeoSignal> = emptyList(),
    val temporalProfile: TemporalActivityProfile? = null,
    val knowledgeGraphObservables: List<OpenCtiObservable> = emptyList(),
    val aggregateRiskCategory: BehavioralRiskCategory = BehavioralRiskCategory.ROUTINE_LOW_RISK,
    val aggregateRiskScore: Int = 25,
    val aggregateConfidenceScore: Int = 80,
    val aggregateConfidenceLevel: String = "HIGH",
    val isRunning: Boolean = false,
    val executionProgressPercent: Float = 1.0f,
    val progressMessageEn: String = "Investigation ready.",
    val progressMessageFa: String = "تحقیقات آماده است.",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ResolvedEntitySummary(
    val entityId: String,
    val name: String,
    val nameFa: String,
    val entityClass: ForensicEntityClass,
    val controlRelation: AddressControlRelation,
    val primaryEvidenceId: String,
    val confidence: Int,
    val isSanctioned: Boolean = false,
    val summaryDetailsEn: String,
    val summaryDetailsFa: String
)

object InvestigationPipelineEngine {

    private val caseSessions = mutableMapOf<String, InvestigationExecutionSession>()
    private val _currentSession = MutableStateFlow<InvestigationExecutionSession?>(null)
    val currentSession: StateFlow<InvestigationExecutionSession?> = _currentSession.asStateFlow()

    fun getSessionForCase(caseId: String): InvestigationExecutionSession? {
        return caseSessions[caseId]
    }

    fun setActiveCase(caseId: String) {
        _currentSession.value = caseSessions[caseId]
    }

    fun clearSessionForCase(caseId: String) {
        caseSessions.remove(caseId)
        if (_currentSession.value?.caseId == caseId) {
            _currentSession.value = null
        }
    }

    // ------------------------------------------------------------------------
    // STEP 1 & 2: AUTOMATIC INPUT CLASSIFICATION & VALIDATION
    // ------------------------------------------------------------------------
    fun classifyAndNormalizeInput(raw: String): InvestigationSeed {
        val trimmed = raw.trim()
        val normalized = trimmed.lowercase(Locale.ROOT)

        return when {
            // Bitcoin Bech32
            trimmed.startsWith("bc1", ignoreCase = true) -> {
                InvestigationSeed(
                    type = InvestigationSeedType.BITCOIN_ADDRESS,
                    rawValue = trimmed,
                    normalizedValue = trimmed,
                    customLabel = "Bitcoin Bech32 Native SegWit"
                )
            }
            // Bitcoin Legacy / P2SH
            (trimmed.startsWith("1") || trimmed.startsWith("3")) && trimmed.length in 26..35 && !trimmed.contains("@") -> {
                InvestigationSeed(
                    type = InvestigationSeedType.BITCOIN_ADDRESS,
                    rawValue = trimmed,
                    normalizedValue = trimmed,
                    customLabel = "Bitcoin Legacy / P2SH Address"
                )
            }
            // Ethereum / EVM Address
            trimmed.startsWith("0x", ignoreCase = true) && trimmed.length == 42 -> {
                InvestigationSeed(
                    type = InvestigationSeedType.EVM_ADDRESS,
                    rawValue = trimmed,
                    normalizedValue = normalized,
                    customLabel = "Ethereum / EVM Address"
                )
            }
            // Transaction Hash (64 hex characters)
            (trimmed.length == 64 || (trimmed.startsWith("0x") && trimmed.length == 66)) && trimmed.all { it.isLetterOrDigit() } -> {
                InvestigationSeed(
                    type = InvestigationSeedType.TRANSACTION_ID,
                    rawValue = trimmed,
                    normalizedValue = normalized,
                    customLabel = "Blockchain Transaction Hash"
                )
            }
            // TRON Address
            trimmed.startsWith("T") && trimmed.length == 34 -> {
                InvestigationSeed(
                    type = InvestigationSeedType.TRON_ADDRESS,
                    rawValue = trimmed,
                    normalizedValue = trimmed,
                    customLabel = "TRON Base58 Address"
                )
            }
            // Email Address
            trimmed.contains("@") && trimmed.contains(".") && !trimmed.startsWith("http") -> {
                InvestigationSeed(
                    type = InvestigationSeedType.EMAIL,
                    rawValue = trimmed,
                    normalizedValue = normalized,
                    customLabel = "Email Identifier"
                )
            }
            // URL
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> {
                val isTor = trimmed.contains(".onion", ignoreCase = true)
                InvestigationSeed(
                    type = if (isTor) InvestigationSeedType.ONION_URL else InvestigationSeedType.URL,
                    rawValue = trimmed,
                    normalizedValue = trimmed,
                    customLabel = if (isTor) "Tor Onion Service Endpoint" else "Web URL Resource"
                )
            }
            // Domain Name
            trimmed.contains(".") && !trimmed.contains(" ") && !trimmed.contains("@") && trimmed.all { it.isLetterOrDigit() || it == '.' || it == '-' } -> {
                InvestigationSeed(
                    type = InvestigationSeedType.DOMAIN,
                    rawValue = trimmed,
                    normalizedValue = normalized,
                    customLabel = "Internet Domain Name"
                )
            }
            // IPv4 Address
            Regex("""^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$""").matches(trimmed) -> {
                InvestigationSeed(
                    type = InvestigationSeedType.IP_ADDRESS,
                    rawValue = trimmed,
                    normalizedValue = trimmed,
                    customLabel = "IPv4 Host Address"
                )
            }
            // ASN (e.g. AS13335, ASN24940)
            trimmed.startsWith("AS", ignoreCase = true) && trimmed.drop(2).all { it.isDigit() } -> {
                InvestigationSeed(
                    type = InvestigationSeedType.ASN,
                    rawValue = trimmed,
                    normalizedValue = trimmed.uppercase(Locale.ROOT),
                    customLabel = "Autonomous System Number"
                )
            }
            // Telegram Handle / Mention
            trimmed.startsWith("@") || trimmed.startsWith("t.me/") -> {
                val handle = trimmed.removePrefix("@").removePrefix("t.me/").removePrefix("https://t.me/")
                InvestigationSeed(
                    type = InvestigationSeedType.TELEGRAM_IDENTITY,
                    rawValue = trimmed,
                    normalizedValue = handle,
                    customLabel = "Telegram Channel / Alias"
                )
            }
            // Default: Username or Investigator Keyword
            else -> {
                InvestigationSeed(
                    type = if (trimmed.length < 24 && !trimmed.contains(" ")) InvestigationSeedType.USERNAME else InvestigationSeedType.KEYWORD,
                    rawValue = trimmed,
                    normalizedValue = trimmed,
                    customLabel = if (trimmed.length < 24 && !trimmed.contains(" ")) "Username / Online Alias Candidate" else "Investigator Search Keyword"
                )
            }
        }
    }

    // ------------------------------------------------------------------------
    // FULL INVESTIGATION PIPELINE EXECUTION
    // ------------------------------------------------------------------------
    suspend fun runCompleteInvestigation(
        seeds: List<InvestigationSeed>,
        caseId: String,
        onProgress: (Float, String, String) -> Unit = { _, _, _ -> }
    ): InvestigationExecutionSession = withContext(Dispatchers.Default) {

        onProgress(0.10f, "Normalizing inputs and discovering source registries...", "نرمال‌سازی ورودی‌ها و کشف پایگاه‌های داده...")
        
        // 1. Collect evidence from pluggable OSINT providers
        onProgress(0.30f, "Querying SpiderFoot, Maigret, theHarvester, OpenSanctions, MISP, and GraphSense...", "استعلام موازی از ماژول‌های اسپایدرفوت، میگره، هاروستر، اوپن‌سنکشنز و گراف‌سنس...")
        
        val rawEvidence = mutableListOf<OsintEvidenceItem>()
        for (seed in seeds) {
            val context = com.aistudio.orbit.provider.osint.OsintInvestigationContext(
                caseId = caseId,
                investigationId = "INV_${System.currentTimeMillis()}", // Real integration would pass an actual DB ID
                seed = seed
            )
            val items = PluggableOsintOrchestrator.executeInvestigation(context)
            rawEvidence.addAll(items)
        }

        // 2. Deduplicate, validate source independence, and compute lineage
        onProgress(0.50f, "Validating source quality and detecting syndication / mirrors...", "اعتبارسنجی کیفیت منابع و حذف کپی‌های نامعتبر...")
        val validatedEvidence = validateSourceIndependence(rawEvidence)

        // 3. Resolve entities and control relationships
        onProgress(0.65f, "Resolving entity classes and address control models...", "تفکیک کلاس موجودیت‌ها و مدل کنترل آدرس‌ها...")
        val entities = resolveEntities(validatedEvidence, seeds)

        // 4. Detect conflicts
        onProgress(0.75f, "Analyzing attribution and geographic contradictions...", "تشخیص تعارضات انتساب و ناهماهنگی‌های جغرافیایی...")
        val conflicts = detectConflicts(validatedEvidence, entities)

        // 5. Calculate multi-factor confidence and hypotheses
        onProgress(0.85f, "Formulating investigative hypotheses and calculating multi-factor confidence...", "فرمول‌بندی فرضیات تحقیقاتی و محاسبه مدل اطمینان ریاضی...")
        val hypotheses = generateHypotheses(validatedEvidence, entities, conflicts)
        val leads = generateRankedLeads(validatedEvidence, seeds)

        // 6. Geographic & Temporal behavioral signals
        val geoSignals = buildGeographicSignals(validatedEvidence)
        val temporalProfile = buildTemporalProfile(seeds)
        val knowledgeGraph = buildKnowledgeGraph(validatedEvidence, entities)

        // 7. Aggregate Risk Scoring
        val hasSanctions = validatedEvidence.any { it.sourceQuality == SourceQualityGrade.A_PRIMARY_DIRECT && it.epistemicType == EpistemicType.FACT }
        val aggregateRisk = when {
            hasSanctions -> BehavioralRiskCategory.SANCTIONS_EXPOSURE
            conflicts.isNotEmpty() -> BehavioralRiskCategory.POTENTIALLY_SUSPICIOUS
            entities.any { it.entityClass == ForensicEntityClass.MIXER } -> BehavioralRiskCategory.MIXER_EXPOSURE
            entities.any { it.entityClass == ForensicEntityClass.SCAM } -> BehavioralRiskCategory.SCAM_REPORTS
            else -> BehavioralRiskCategory.ROUTINE_LOW_RISK
        }
        val riskScore = when (aggregateRisk) {
            BehavioralRiskCategory.SANCTIONS_EXPOSURE -> 98
            BehavioralRiskCategory.MIXER_EXPOSURE -> 85
            BehavioralRiskCategory.SCAM_REPORTS -> 80
            BehavioralRiskCategory.POTENTIALLY_SUSPICIOUS -> 55
            else -> 22
        }

        onProgress(1.0f, "Investigation pipeline completed with full provenance trail.", "فرآیند تحقیقات با ثبت کامل شجره ادله به پایان رسید.")

        val session = InvestigationExecutionSession(
            caseId = caseId,
            initialSeeds = seeds,
            collectedEvidence = validatedEvidence,
            extractedEntities = entities,
            hypotheses = hypotheses,
            rankedLeads = leads,
            conflicts = conflicts,
            geoSignals = geoSignals,
            temporalProfile = temporalProfile,
            knowledgeGraphObservables = knowledgeGraph,
            aggregateRiskCategory = aggregateRisk,
            aggregateRiskScore = riskScore,
            aggregateConfidenceScore = calculateOverallConfidence(validatedEvidence, conflicts),
            aggregateConfidenceLevel = if (conflicts.isNotEmpty()) "CONTESTED" else "HIGH",
            isRunning = false,
            executionProgressPercent = 1.0f,
            progressMessageEn = "Investigation completed. ${validatedEvidence.size} evidence items curated.",
            progressMessageFa = "تحقیقات تکمیل شد. تعداد ${validatedEvidence.size} مدرک مستندسازی گردید."
        )

        caseSessions[caseId] = session
        _currentSession.value = session
        session
    }

    // ------------------------------------------------------------------------
    // SOURCE INDEPENDENCE & SYNDICATION DETECTION
    // ------------------------------------------------------------------------
    private fun validateSourceIndependence(evidenceList: List<OsintEvidenceItem>): List<OsintEvidenceItem> {
        val seenHashes = mutableSetOf<String>()
        val result = mutableListOf<OsintEvidenceItem>()

        for (item in evidenceList) {
            val contentKey = "${item.queryTarget}:${item.title}:${item.extractedEntities.sorted().joinToString(",")}"
            val hash = computeSha256(contentKey)

            if (seenHashes.contains(hash)) {
                // Mark as syndicated / mirrored instead of counting as independent proof
                result.add(
                    item.copy(
                        isSyndicatedOrMirrored = true,
                        sourceQuality = SourceQualityGrade.E_UNRELIABLE_CONTRADICTORY,
                        numericConfidence = (item.numericConfidence * 0.4f).toInt(),
                        analystNote = "Identified as mirrored / syndicated content. Downgraded to prevent artificial confidence inflation."
                    )
                )
            } else {
                seenHashes.add(hash)
                result.add(item)
            }
        }
        return result
    }

    // ------------------------------------------------------------------------
    // ENTITY RESOLUTION & ADDRESS CONTROL ENGINE
    // ------------------------------------------------------------------------
    private fun resolveEntities(
        evidenceList: List<OsintEvidenceItem>,
        seeds: List<InvestigationSeed>
    ): List<ResolvedEntitySummary> {
        val summaries = mutableListOf<ResolvedEntitySummary>()

        evidenceList.forEachIndexed { index, ev ->
            when {
                ev.sourceId.contains("OFAC", ignoreCase = true) || ev.title.contains("Sanctions", ignoreCase = true) -> {
                    val rawName = ev.extractedEntities.firstOrNull() ?: ev.title.replace(Regex("(?i)Sanctions:?"), "").trim().ifBlank { "Designated Sanctioned Entity" }
                    val isRansom = rawName.contains("ransom", ignoreCase = true)
                    summaries.add(
                        ResolvedEntitySummary(
                            entityId = "ENT_SANCTIONED_${index + 1}",
                            name = rawName,
                            nameFa = "نهاد تحریم‌شده: $rawName",
                            entityClass = if (isRansom) ForensicEntityClass.RANSOMWARE else ForensicEntityClass.ORGANIZATION,
                            controlRelation = AddressControlRelation.CONTROLLED_BY,
                            primaryEvidenceId = ev.evidenceId,
                            confidence = ev.numericConfidence,
                            isSanctioned = true,
                            summaryDetailsEn = "Official sanctions designation match on verified regulatory registry.",
                            summaryDetailsFa = "تطابق با فهرست تحریم‌های ویژه بر مبنای اسناد رسمی و ثبت‌های تحلیلی."
                        )
                    )
                }
                ev.sourceId.contains("GRAPHSENSE", ignoreCase = true) || ev.title.contains("TagPack", ignoreCase = true) -> {
                    val exName = ev.extractedEntities.firstOrNull() ?: ev.title.replace(Regex("(?i)TagPack:?"), "").trim().ifBlank { "Attributed VASP / Exchange" }
                    summaries.add(
                        ResolvedEntitySummary(
                            entityId = "ENT_VASP_${index + 1}",
                            name = exName,
                            nameFa = "نهاد منتسب: $exName",
                            entityClass = ForensicEntityClass.EXCHANGE,
                            controlRelation = AddressControlRelation.ATTRIBUTED_TO,
                            primaryEvidenceId = ev.evidenceId,
                            confidence = ev.numericConfidence,
                            summaryDetailsEn = "Identified via TagPack and cluster attribution heuristics.",
                            summaryDetailsFa = "شناسایی بر اساس خوشه‌بندی تگ‌پک و الگوهای مالکیتی اشتراکی."
                        )
                    )
                }
                ev.sourceId.contains("MAIGRET", ignoreCase = true) -> {
                    val profileName = ev.extractedEntities.firstOrNull() ?: ev.queryTarget
                    summaries.add(
                        ResolvedEntitySummary(
                            entityId = "ENT_USER_${index + 1}",
                            name = "Public Alias: $profileName",
                            nameFa = "شناسه کاربری عمومی: $profileName",
                            entityClass = ForensicEntityClass.PERSON,
                            controlRelation = AddressControlRelation.SELF_PUBLISHED_BY,
                            primaryEvidenceId = ev.evidenceId,
                            confidence = ev.numericConfidence,
                            summaryDetailsEn = "Public profile self-disclosure linking cryptocurrency identifiers across web endpoints.",
                            summaryDetailsFa = "پروفایل عمومی خودافشاشده حاوی شناسه‌های رمزارز در محیط وب."
                        )
                    )
                }
            }
        }

        if (summaries.isEmpty()) {
            summaries.add(
                ResolvedEntitySummary(
                    entityId = "ENT_UNKNOWN_DEFAULT",
                    name = "Unclassified Digital Wallet Owner",
                    nameFa = "مالک کیف‌پول دیجیتال طبقه‌بندی‌نشده",
                    entityClass = ForensicEntityClass.UNKNOWN,
                    controlRelation = AddressControlRelation.POTENTIAL_CONTROL,
                    primaryEvidenceId = evidenceList.firstOrNull()?.evidenceId ?: "EVD_DEFAULT",
                    confidence = 0,
                    summaryDetailsEn = "Insufficient multi-source corroboration to determine conclusive natural person or organizational identity.",
                    summaryDetailsFa = "شواهد چندمنبعی کافی جهت احراز قطعی شخصیت حقیقی یا حقوقی وجود ندارد."
                )
            )
        }

        return summaries
    }

    // ------------------------------------------------------------------------
    // CONFLICT DETECTION ENGINE
    // ------------------------------------------------------------------------
    private fun detectConflicts(
        evidenceList: List<OsintEvidenceItem>,
        entities: List<ResolvedEntitySummary>
    ): List<AttributionConflict> {
        val conflicts = mutableListOf<AttributionConflict>()

        // Check if there are competing VASP attributions
        val vaspEntities = entities.filter { it.entityClass == ForensicEntityClass.EXCHANGE || it.entityClass == ForensicEntityClass.VASP }
        if (vaspEntities.size >= 2) {
            val a = vaspEntities[0]
            val b = vaspEntities[1]
            // Only add conflict if names are different
            if (a.name != b.name) {
                val totalConf = (a.confidence + b.confidence).coerceAtLeast(1)
                val confA = (a.confidence * 100) / totalConf
                val confB = (b.confidence * 100) / totalConf
                val unknown = (100 - confA - confB).coerceAtLeast(0)
                conflicts.add(
                    AttributionConflict(
                        targetIndicator = evidenceList.firstOrNull()?.queryTarget ?: "Target Address",
                        conflictDomain = "ENTITY_ATTRIBUTION",
                        candidateA = a.name,
                        candidateAFa = a.nameFa,
                        candidateB = b.name,
                        candidateBFa = b.nameFa,
                        confidenceA = confA,
                        confidenceB = confB,
                        unknownShare = unknown,
                        supportingEvidenceA = listOf(a.primaryEvidenceId),
                        supportingEvidenceB = listOf(b.primaryEvidenceId),
                        resolutionStatus = "UNRESOLVED",
                        analystReviewNotes = "Conflicting TagPack attribution clusters detected between ${a.name} and ${b.name}. Analyst manual verification of co-spending transaction topology is mandatory."
                    )
                )
            }
        }
        return conflicts
    }

    // ------------------------------------------------------------------------
    // HYPOTHESIS & LEAD GENERATION
    // ------------------------------------------------------------------------
    private fun generateHypotheses(
        evidenceList: List<OsintEvidenceItem>,
        entities: List<ResolvedEntitySummary>,
        conflicts: List<AttributionConflict>
    ): List<InvestigationHypothesis> {
        val hypotheses = mutableListOf<InvestigationHypothesis>()

        // Filter out the 'unknown default' to prevent generating a hypothesis about 'unknown'
        entities.filter { it.entityId != "ENT_UNKNOWN_DEFAULT" }.forEachIndexed { idx, ent ->
            val supporting = listOf(ent.primaryEvidenceId)
            val contradicting = conflicts.flatMap { if (it.candidateA == ent.name) it.supportingEvidenceB else it.supportingEvidenceA }

            hypotheses.add(
                InvestigationHypothesis(
                    hypothesisId = "HYP-${100 + idx + 1}",
                    titleEn = "Target is associated with ${ent.name}",
                    titleFa = "آدرس تحت بررسی مرتبط با ${ent.nameFa} است",
                    propositionEn = "Evidence suggests address control or custodial interaction under ${ent.name} (${ent.entityClass.displayNameEn}).",
                    propositionFa = "شواهد حاکی از کنترل یا تعامل امانتداری آدرس تحت ${ent.nameFa} (${ent.entityClass.displayNameFa}) می‌باشد.",
                    supportingEvidenceIds = supporting,
                    contradictingEvidenceIds = contradicting,
                    confidenceLevel = if (contradicting.isNotEmpty()) "CONTESTED" else "HIGH",
                    numericConfidence = if (contradicting.isNotEmpty()) 58 else ent.confidence,
                    status = if (contradicting.isNotEmpty()) "ACTIVE_CONTESTED" else "ACTIVE"
                )
            )
        }
        return hypotheses
    }

    private fun generateRankedLeads(
        evidenceList: List<OsintEvidenceItem>,
        seeds: List<InvestigationSeed>
    ): List<InvestigationLead> {
        val leads = mutableListOf<InvestigationLead>()

        evidenceList.filter { it.extractedEntities.isNotEmpty() }.take(4).forEachIndexed { idx, ev ->
            val target = ev.extractedEntities.firstOrNull() ?: ev.queryTarget
            leads.add(
                InvestigationLead(
                    leadId = "LEAD-${200 + idx + 1}",
                    targetValue = target,
                    targetType = InvestigationSeedType.USERNAME,
                    reasonEn = "Discovered across ${ev.providerName} in association with primary investigation seed.",
                    reasonFa = "در جریان پویش ${ev.providerName} در ارتباط با سرنخ اولیه پرونده کشف شد.",
                    supportingEvidenceIds = listOf(ev.evidenceId),
                    confidenceScore = ev.numericConfidence,
                    expectedValueEn = "High: May identify operational infrastructure and historical identity footprints.",
                    expectedValueFa = "ارزش بالا: امکان شناسایی زیرساخت‌های عملیاتی و ردپای هویتی تاریخی.",
                    nextRecommendedStepEn = "Validate domain registrar records, GitHub commit PGP signatures, and historical snapshots.",
                    nextRecommendedStepFa = "بررسی اطلاعات ثبتی دامنه، امضاهای PGP گیت‌هاب و آرشیو تاریخی وب."
                )
            )
        }
        return leads
    }

    // ------------------------------------------------------------------------
    // GEOGRAPHIC & TEMPORAL SIGNALS
    // ------------------------------------------------------------------------
    private fun buildGeographicSignals(evidenceList: List<OsintEvidenceItem>): List<GeoSignal> {
        val geoSignals = mutableListOf<GeoSignal>()
        
        // Dynamically build based on actual evidence IP addresses/hosting
        evidenceList.filter { it.providerModule.contains("geoip") || it.relationship == EntityRelationType.HOSTED_BY }.forEach { ev ->
            geoSignals.add(
                GeoSignal(
                    regionNameEn = ev.extractedEntities.joinToString(", "),
                    regionNameFa = "منطقه استخراج شده از ادله",
                    countryCode = "UNKNOWN",
                    isCandidate = true,
                    supportingIndicatorsCount = 1,
                    contradictoryIndicatorsCount = 0,
                    confidencePercent = ev.numericConfidence,
                    signalSources = listOf(ev.providerName),
                    isVpnOrTorExit = ev.extractedEntities.any { it.contains("VPN", ignoreCase = true) },
                    precisionType = "ISP_REGISTERED_LOCATION"
                )
            )
        }
        
        return geoSignals
    }

    private fun buildTemporalProfile(seeds: List<InvestigationSeed>): TemporalActivityProfile? {
        // Without real blockchain RPC / transaction timestamps, we cannot reliably compute temporal patterns.
        return null
    }

    private fun buildKnowledgeGraph(
        evidenceList: List<OsintEvidenceItem>,
        entities: List<ResolvedEntitySummary>
    ): List<OpenCtiObservable> {
        return evidenceList.map { ev ->
            val threatActors = ev.extractedEntities.filter { it.contains("APT", ignoreCase = true) || it.contains("Lazarus", ignoreCase = true) }
            val sanctionsMatches = if (ev.sourceId.contains("OFAC", ignoreCase = true) || ev.title.contains("Sanctions", ignoreCase = true)) {
                ev.extractedEntities.ifEmpty { listOf(ev.title) }
            } else {
                emptyList()
            }
            OpenCtiObservable(
                observableType = "Cryptocurrency-Address",
                value = ev.queryTarget,
                standardLabels = ev.extractedEntities,
                firstSeen = ev.firstSeenTimestamp?.let { java.time.Instant.ofEpochMilli(it).toString() } ?: "DATA_NOT_AVAILABLE",
                lastSeen = ev.lastSeenTimestamp?.let { java.time.Instant.ofEpochMilli(it).toString() } ?: "DATA_NOT_AVAILABLE",
                confidence = ev.numericConfidence,
                mispGalaxyThreatActors = threatActors,
                openSanctionsMatches = sanctionsMatches
            )
        }
    }

    private fun calculateOverallConfidence(evidenceList: List<OsintEvidenceItem>, conflicts: List<AttributionConflict>): Int {
        if (evidenceList.isEmpty()) return 30
        var totalWeightedScore = 0f
        var totalWeight = 0f

        evidenceList.forEach { ev ->
            val w = ev.sourceQuality.reliabilityMultiplier
            totalWeightedScore += (ev.numericConfidence * w)
            totalWeight += w
        }

        val baseScore = if (totalWeight > 0f) (totalWeightedScore / totalWeight).toInt() else 50
        val conflictPenalty = conflicts.size * 15
        return (baseScore - conflictPenalty).coerceIn(10, 99)
    }

    fun updateConflictResolution(conflictId: String, decision: String, reviewNotes: String) {
        val session = _currentSession.value ?: return
        val updatedConflicts = session.conflicts.map {
            if (it.conflictId == conflictId) {
                it.copy(
                    resolutionStatus = decision,
                    analystReviewNotes = reviewNotes
                )
            } else it
        }
        val remainingUnresolved = updatedConflicts.filter { it.resolutionStatus == "UNRESOLVED" }
        val updatedConfidence = calculateOverallConfidence(session.collectedEvidence, remainingUnresolved)
        val updatedConfidenceLevel = if (remainingUnresolved.isNotEmpty()) "CONTESTED" else "HIGH"

        _currentSession.value = session.copy(
            conflicts = updatedConflicts,
            aggregateConfidenceScore = updatedConfidence,
            aggregateConfidenceLevel = updatedConfidenceLevel
        )
    }

    fun updateEvidenceVerification(evidenceId: String, newStatus: String, analystNote: String? = null) {
        val session = _currentSession.value ?: return
        val updatedEvidence = session.collectedEvidence.map {
            if (it.evidenceId == evidenceId) {
                it.copy(
                    verificationStatus = newStatus,
                    analystNote = analystNote ?: it.analystNote
                )
            } else it
        }
        _currentSession.value = session.copy(collectedEvidence = updatedEvidence)
    }

    private fun computeSha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
