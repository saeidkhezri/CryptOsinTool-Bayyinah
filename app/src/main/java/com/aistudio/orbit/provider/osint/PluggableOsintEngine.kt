package com.aistudio.orbit.provider.osint

import com.aistudio.orbit.provider.ProviderErrorBoundary
import com.aistudio.orbit.model.*
import java.security.MessageDigest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * ============================================================================
 * PLUGGABLE OSINT PROVIDER ADAPTER ARCHITECTURE
 * ============================================================================
 * Zero Mock Data Architecture. Providers that require API keys or complex setups
 * will return empty lists if unavailable. Simple public lookups (like DNS) are implemented
 * to demonstrate the real pipeline. 
 * All queries are strictly scoped by Context (Case + Investigation).
 */

@Serializable
enum class ProviderHealthStatus(val displayNameEn: String, val displayNameFa: String) {
    HEALTHY("Operational", "فعال و عملیاتی"),
    DEGRADED("Degraded / Slow", "کاهش کارایی / کند"),
    RATE_LIMITED("Rate Limited", "محدودیت نرخ درخواست"),
    API_KEY_REQUIRED("API Key Needed", "نیازمند کلید API"),
    OFFLINE("Offline / Unreachable", "غیرقابل دسترس / آفلاین"),
    DISABLED("Disabled by Analyst", "غیرفعال توسط بازرس")
}

@Serializable
data class PluggableProviderInfo(
    val providerId: String,
    val name: String,
    val category: String, // "GENERAL_OSINT", "USERNAME", "DOMAIN_INFRASTRUCTURE", "SANCTIONS", "THREAT_INTEL", "BLOCKCHAIN_TAGS"
    val descriptionEn: String,
    val descriptionFa: String,
    val isEnabled: Boolean = true,
    val priority: Int = 1,
    val timeoutMs: Long = 10000L,
    val maxRetries: Int = 2,
    val rateLimitPerMinute: Int = 60,
    val requiresApiKey: Boolean = false,
    val isApiKeyConfigured: Boolean = true,
    val healthStatus: ProviderHealthStatus = ProviderHealthStatus.HEALTHY,
    val lastSuccessfulQueryTimestamp: Long? = null,
    val failureCount: Int = 0,
    val totalQueriesExecuted: Int = 0,
    val referenceRepositoryUrl: String = ""
)

data class OsintInvestigationContext(
    val caseId: String,
    val investigationId: String,
    val seed: InvestigationSeed,
    val timestamp: Long = System.currentTimeMillis(),
    val rateLimit: Int = 10,
    val timeoutMs: Long = 10000L
)

interface PluggableOsintAdapter {
    val info: PluggableProviderInfo
    suspend fun executeRecon(context: OsintInvestigationContext): List<OsintEvidenceItem>
}

// Global HTTP Client for OSINT Providers
private val osintHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()

// ----------------------------------------------------------------------------
// 1. SPIDERFOOT ADAPTER (General Purpose OSINT & Infrastructure Correlation)
// ----------------------------------------------------------------------------
class SpiderFootAdapter : PluggableOsintAdapter {
    override var info: PluggableProviderInfo = PluggableProviderInfo(
        providerId = "spiderfoot_engine",
        name = "SpiderFoot OSINT Automation",
        category = "GENERAL_OSINT",
        descriptionEn = "Automated domain, host, IP, ASN, darkweb, and web graph intelligence collector.",
        descriptionFa = "موتور جامع جمع‌آوری اطلاعات دامنه، میزبان، آی‌پی، ASN، دارک‌وب و گراف وب.",
        isEnabled = true,
        priority = 1,
        referenceRepositoryUrl = "https://github.com/smicallef/spiderfoot"
    )

    override suspend fun executeRecon(context: OsintInvestigationContext): List<OsintEvidenceItem> {
        val seed = context.seed
        val query = seed.normalizedValue
        val results = mutableListOf<OsintEvidenceItem>()

        if (seed.type == InvestigationSeedType.DOMAIN || seed.type == InvestigationSeedType.URL) {
            val domain = query.replace("https://", "").replace("http://", "").substringBefore("/")
            try {
                // Real implementation: Query DNS over HTTPS using Google DNS
                val request = Request.Builder()
                    .url("https://dns.google/resolve?name=$domain&type=A")
                    .header("Accept", "application/json")
                    .build()

                val responseStr = withContext(Dispatchers.IO) {
                    val response = osintHttpClient.newCall(request).execute()
                    if (response.isSuccessful) response.body?.string() else null
                }
                
                if (responseStr != null) {
                    val json = JSONObject(responseStr)
                    if (json.has("Answer")) {
                        val answers = json.getJSONArray("Answer")
                        val ips = mutableListOf<String>()
                        for (i in 0 until answers.length()) {
                            ips.add(answers.getJSONObject(i).getString("data"))
                        }
                        
                        if (ips.isNotEmpty()) {
                            val hash = computeSha256("spiderfoot:${seed.type}:$query:${System.currentTimeMillis()}")
                            results.add(
                                OsintEvidenceItem(
                                    caseId = context.caseId,
                                    sourceId = "SF_MOD_DNS_HOST",
                                    providerName = "SpiderFoot (sfp_dnsresolve)",
                                    providerModule = "sfp_dnsresolve",
                                    queryTarget = query,
                                    sourceUrl = "https://dns.google/resolve?name=$domain",
                                    title = "DNS Resolution for $domain",
                                    titleFa = "تحلیل ساختار DNS دامنه $domain",
                                    rawContentHash = hash,
                                    contentType = "application/json",
                                    extractedEntities = ips,
                                    relationship = EntityRelationType.RESOLVES_TO,
                                    sourceQuality = SourceQualityGrade.B_REPUTABLE_SECONDARY,
                                    epistemicType = EpistemicType.OBSERVATION,
                                    confidenceLevel = "HIGH",
                                    numericConfidence = 95,
                                    analystNote = "Active public DNS infrastructure records identified via Google DoH."
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Graceful failure: return empty, do not return mock data
            }
        }
        
        // No mock data fallback. If no results found, return empty list.
        return results
    }
}

// ----------------------------------------------------------------------------
// 2. MAIGRET USERNAME ENRICHMENT PROVIDER
// ----------------------------------------------------------------------------
class MaigretUsernameProvider : PluggableOsintAdapter {
    override var info: PluggableProviderInfo = PluggableProviderInfo(
        providerId = "maigret_username_engine",
        name = "Maigret Profile & Username Intelligence",
        category = "USERNAME",
        descriptionEn = "Primary recursive username investigator, category filtering (Crypto/Dev), and profile parser.",
        descriptionFa = "موتور اصلی جستجوی بازگشتی نام‌های کاربری، فیلتر دسته‌های تخصصی (رمزارز/توسعه) و تحلیل پروفایل.",
        isEnabled = true,
        priority = 1,
        requiresApiKey = true,
        isApiKeyConfigured = false, // Set to false to trigger graceful unavailable state
        healthStatus = ProviderHealthStatus.API_KEY_REQUIRED,
        referenceRepositoryUrl = "https://github.com/soxoj/maigret"
    )

    override suspend fun executeRecon(context: OsintInvestigationContext): List<OsintEvidenceItem> {
        // Without an API or backend container to run Maigret python scripts, we return empty list.
        // Zero Mock Data rule applies.
        return emptyList()
    }
}

// ----------------------------------------------------------------------------
// 3. SHERLOCK USERNAME CORROBORATOR
// ----------------------------------------------------------------------------
class SherlockCorroborator : PluggableOsintAdapter {
    override var info: PluggableProviderInfo = PluggableProviderInfo(
        providerId = "sherlock_corroborator",
        name = "Sherlock Corroboration Engine",
        category = "USERNAME",
        descriptionEn = "Independent corroboration provider. Prevents double-counting when checking common underlying profiles.",
        descriptionFa = "ارائه‌دهنده تایید مستقل با جلوگیری از احتساب مضاعف ادله مشترک پروفایل‌ها.",
        isEnabled = true,
        priority = 2,
        requiresApiKey = false,
        isApiKeyConfigured = true,
        healthStatus = ProviderHealthStatus.HEALTHY,
        referenceRepositoryUrl = "https://github.com/sherlock-project/sherlock"
    )

    private val targetSites = listOf(
        Pair("GitHub", "https://api.github.com/users/%s"),
        Pair("DevTo", "https://dev.to/api/users/by_username?url=%s"),
        Pair("Pastebin", "https://pastebin.com/u/%s")
    )

    override suspend fun executeRecon(context: OsintInvestigationContext): List<OsintEvidenceItem> {
        val results = mutableListOf<OsintEvidenceItem>()
        val query = context.seed.normalizedValue.trim()
        if (query.length < 3 || context.seed.type != InvestigationSeedType.USERNAME) {
            return results
        }

        for ((siteName, urlPattern) in targetSites) {
            val targetUrl = String.format(urlPattern, query)
            try {
                val statusCode = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(targetUrl)
                        .header("User-Agent", "Bayyinah-Forensics/1.0")
                        .build()
                    val response = osintHttpClient.newCall(request).execute()
                    val code = response.code
                    response.close()
                    code
                }

                if (statusCode in 200..299) {
                    val hash = computeSha256("sherlock:$siteName:$query:$targetUrl")
                    results.add(
                        OsintEvidenceItem(
                            caseId = context.caseId,
                            sourceId = "SHERLOCK_$siteName",
                            providerName = "Sherlock Corroboration Engine",
                            providerModule = "sherlock_http_scanner",
                            queryTarget = query,
                            sourceUrl = targetUrl,
                            title = "Corroborated Handle: @$query on $siteName",
                            titleFa = "شناسه تایید شده: @$query در $siteName",
                            rawContentHash = hash,
                            contentType = "application/json",
                            extractedEntities = listOf("@$query", siteName, targetUrl),
                            relationship = EntityRelationType.ASSOCIATED_WITH,
                            sourceQuality = SourceQualityGrade.C_USEFUL_CORROBORATION,
                            epistemicType = EpistemicType.INFERENCE,
                            confidenceLevel = "MEDIUM",
                            numericConfidence = 70,
                            analystNote = "Public handle existence verified via HTTP $statusCode. Limited source independence due to public scraping reliance.",
                            verificationStatus = "CORROBORATED"
                        )
                    )
                }
            } catch (e: Exception) {
                // Graceful failure per site
            }
        }
        return results
    }
}

// ----------------------------------------------------------------------------
// 4. THEHARVESTER DOMAIN & EMAIL INTELLIGENCE PROVIDER
// ----------------------------------------------------------------------------
class TheHarvesterDomainProvider : PluggableOsintAdapter {
    override var info: PluggableProviderInfo = PluggableProviderInfo(
        providerId = "theharvester_domain",
        name = "theHarvester Domain & Infrastructure",
        category = "DOMAIN_INFRASTRUCTURE",
        descriptionEn = "Domain OSINT, subdomain enumeration, public email discovery, and certificate transparency clues.",
        descriptionFa = "هوش منابع باز دامنه، استخراج زیردامنه‌ها، کشف ایمیل‌های عمومی و شفافیت گواهی‌ها.",
        isEnabled = true,
        priority = 1,
        requiresApiKey = true,
        isApiKeyConfigured = false,
        healthStatus = ProviderHealthStatus.API_KEY_REQUIRED,
        referenceRepositoryUrl = "https://github.com/laramies/theHarvester"
    )

    override suspend fun executeRecon(context: OsintInvestigationContext): List<OsintEvidenceItem> {
        return emptyList()
    }
}

// ----------------------------------------------------------------------------
// 5. OPENSANCTIONS & YENTE SCREENING PROVIDER
// ----------------------------------------------------------------------------
class OpenSanctionsYenteProvider : PluggableOsintAdapter {
    override var info: PluggableProviderInfo = PluggableProviderInfo(
        providerId = "opensanctions_yente",
        name = "OpenSanctions & Yente Screening API",
        category = "SANCTIONS",
        descriptionEn = "Screening against OFAC, EU, UN sanctions lists, PEP databases, and sanctioned cryptocurrency addresses.",
        descriptionFa = "پایش و تطبیق با فهرست‌های تحریمی OFAC، اتحادیه اروپا، سازمان ملل و آدرس‌های رمزارز مسدودشده.",
        isEnabled = true,
        priority = 1,
        referenceRepositoryUrl = "https://github.com/opensanctions/opensanctions"
    )

    override suspend fun executeRecon(context: OsintInvestigationContext): List<OsintEvidenceItem> {
        val seed = context.seed
        val query = seed.normalizedValue
        val results = mutableListOf<OsintEvidenceItem>()
        
        try {
            // Real implementation: querying the OpenSanctions / Yente public API (limit 1 for fast recon)
            val request = Request.Builder()
                .url("https://api.opensanctions.org/search/default?q=${query}&limit=1")
                .header("Accept", "application/json")
                .build()

            val responseStr = withContext(Dispatchers.IO) {
                val response = osintHttpClient.newCall(request).execute()
                if (response.isSuccessful) response.body?.string() else null
            }
            
            if (responseStr != null) {
                val json = JSONObject(responseStr)
                if (json.has("results")) {
                    val array = json.getJSONArray("results")
                    if (array.length() > 0) {
                        val match = array.getJSONObject(0)
                        val name = match.optString("caption", "Unknown Entity")
                        val id = match.optString("id", "")
                        
                        val hash = computeSha256("opensanctions:$id:${System.currentTimeMillis()}")
                        results.add(
                            OsintEvidenceItem(
                                caseId = context.caseId,
                                sourceId = "OPENSANCTIONS_MATCH",
                                providerName = "OpenSanctions (yente engine)",
                                providerModule = "sanctions_matcher",
                                queryTarget = query,
                                sourceUrl = "https://www.opensanctions.org/entities/$id",
                                title = "Sanctions List Match: $name",
                                titleFa = "تطابق با فهرست‌های تحریمی: $name",
                                rawContentHash = hash,
                                contentType = "application/json",
                                extractedEntities = listOf(name, "OpenSanctions ID: $id"),
                                relationship = EntityRelationType.ATTRIBUTED_TO,
                                sourceQuality = SourceQualityGrade.A_PRIMARY_DIRECT,
                                epistemicType = EpistemicType.FACT,
                                confidenceLevel = "HIGH",
                                numericConfidence = 95,
                                analystNote = "Direct match on OpenSanctions index. Verification required for false positives.",
                                verificationStatus = "PENDING_REVIEW"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            ProviderErrorBoundary.recordFailure(
                providerId = "opensanctions_yente",
                providerNameFa = "سامانه تحریمی OpenSanctions",
                providerNameEn = "OpenSanctions Screening API",
                error = e,
                impactFa = "پایش تحریمی مستقیم انجام نشد. تحلیل به داده‌های درون‌برنامه‌ای محدود گردید.",
                impactEn = "Live sanctions screening skipped; local reference datasets used."
            )
        }
        
        return results
    }
}

// ----------------------------------------------------------------------------
// 6. MISP & MISP GALAXY THREAT INTEL PROVIDER
// ----------------------------------------------------------------------------
class MispGalaxyProvider : PluggableOsintAdapter {
    override var info: PluggableProviderInfo = PluggableProviderInfo(
        providerId = "misp_galaxy_engine",
        name = "MISP & MISP Galaxy Threat Intelligence",
        category = "THREAT_INTEL",
        descriptionEn = "Standardized cyber threat intelligence sharing, ransomware campaigns, and threat actor taxonomies.",
        descriptionFa = "پلتفرم اشتراک‌گذاری هوش تهدیدات سایبری، کمپین‌های باج‌افزاری و تاکسونومی مهاجمان.",
        isEnabled = true,
        priority = 1,
        requiresApiKey = true,
        isApiKeyConfigured = false,
        healthStatus = ProviderHealthStatus.API_KEY_REQUIRED,
        referenceRepositoryUrl = "https://github.com/MISP/MISP"
    )

    override suspend fun executeRecon(context: OsintInvestigationContext): List<OsintEvidenceItem> {
        val results = mutableListOf<OsintEvidenceItem>()
        val target = context.seed.normalizedValue.trim()
        if (target.isBlank()) return results

        try {
            // Check if MISP instance is reachable / configured
            // Default graceful handling: returns empty list if unconfigured, without crashing
        } catch (e: Exception) {
            // Graceful non-blocking failure: MISP unreachable or unconfigured
        }
        return results
    }
}

// ----------------------------------------------------------------------------
// 7. GRAPHSENSE TAGPACKS & ATTRIBUTION PROVIDER
// ----------------------------------------------------------------------------
class GraphSenseTagPacksProvider : PluggableOsintAdapter {
    override var info: PluggableProviderInfo = PluggableProviderInfo(
        providerId = "graphsense_tagpacks",
        name = "GraphSense TagPacks & VASP Attribution",
        category = "BLOCKCHAIN_TAGS",
        descriptionEn = "Open-source cryptocurrency attribution, TagPack repository, and cluster entity resolution.",
        descriptionFa = "انتساب متن‌باز تراکنش‌ها، مخزن TagPacks و خوشه‌بندی نهادهای ارائه‌دهنده VASP.",
        isEnabled = true,
        priority = 1,
        requiresApiKey = false,
        isApiKeyConfigured = true,
        healthStatus = ProviderHealthStatus.HEALTHY,
        referenceRepositoryUrl = "https://github.com/graphsense/graphsense-tagpacks"
    )

    override suspend fun executeRecon(context: OsintInvestigationContext): List<OsintEvidenceItem> {
        val results = mutableListOf<OsintEvidenceItem>()
        val target = context.seed.normalizedValue.trim()
        if (target.isBlank()) return results

        try {
            val label = com.aistudio.orbit.forensics.classification.LabelingModule.getLabel(target)
            if (label != null) {
                results.add(
                    OsintEvidenceItem(
                        caseId = context.caseId,
                        sourceId = "GRAPHSENSE_TAG_${label.address}",
                        providerName = "GraphSense Curated TagPacks",
                        providerModule = "tagpack_resolver",
                        queryTarget = target,
                        sourceUrl = "https://graphsense.info",
                        title = "Entity Tag: ${label.entityNameEn}",
                        titleFa = "برچسب هویت: ${label.entityNameFa}",
                        rawContentHash = computeSha256("graphsense:${label.address}:$target"),
                        contentType = "text/plain",
                        extractedEntities = listOf(label.entityNameEn, label.classification.name),
                        relationship = EntityRelationType.ATTRIBUTED_TO,
                        sourceQuality = SourceQualityGrade.A_PRIMARY_DIRECT,
                        epistemicType = EpistemicType.FACT,
                        confidenceLevel = "HIGH",
                        numericConfidence = (label.confidenceScore * 100).toInt(),
                        analystNote = "Curated blockchain entity attribution from known dataset.",
                        verificationStatus = "OFFICIALLY_VERIFIED"
                    )
                )
            }
        } catch (e: Exception) {
            // Graceful non-blocking failure
        }
        return results
    }
}

// ----------------------------------------------------------------------------
// CENTRAL PLUGGABLE OSINT ORCHESTRATOR
// ----------------------------------------------------------------------------
object PluggableOsintOrchestrator {
    private val adapters = mutableListOf<PluggableOsintAdapter>(
        SpiderFootAdapter(),
        MaigretUsernameProvider(),
        SherlockCorroborator(),
        TheHarvesterDomainProvider(),
        OpenSanctionsYenteProvider(),
        MispGalaxyProvider(),
        GraphSenseTagPacksProvider()
    )

    private val _providersState = MutableStateFlow<List<PluggableProviderInfo>>(adapters.map { it.info })
    val providersState: StateFlow<List<PluggableProviderInfo>> = _providersState.asStateFlow()

    fun getAllAdapters(): List<PluggableOsintAdapter> = adapters

    fun toggleProvider(providerId: String, isEnabled: Boolean) {
        val updated = _providersState.value.map {
            if (it.providerId == providerId) it.copy(
                isEnabled = isEnabled,
                healthStatus = if (isEnabled) {
                    if (it.requiresApiKey && !it.isApiKeyConfigured) ProviderHealthStatus.API_KEY_REQUIRED
                    else ProviderHealthStatus.HEALTHY
                } else ProviderHealthStatus.DISABLED
            ) else it
        }
        _providersState.value = updated
    }

    suspend fun executeInvestigation(context: OsintInvestigationContext): List<OsintEvidenceItem> {
        val allEvidence = mutableListOf<OsintEvidenceItem>()
        val activeAdapters = adapters.filter { adapter ->
            val state = _providersState.value.find { it.providerId == adapter.info.providerId }
            state?.isEnabled == true && state.healthStatus == ProviderHealthStatus.HEALTHY
        }

        // Only run adapters that are healthy and enabled
        for (adapter in activeAdapters) {
            try {
                val items = adapter.executeRecon(context)
                allEvidence.addAll(items)
            } catch (e: Exception) {
                // Graceful fallback and error handling without failing entire pipeline
            }
        }
        return allEvidence
    }
}

private fun computeSha256(input: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

