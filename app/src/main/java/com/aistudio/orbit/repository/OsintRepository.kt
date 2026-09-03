package com.aistudio.orbit.repository

import com.aistudio.orbit.forensics.osint.*
import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.provider.AddressOsintMetadata
import com.aistudio.orbit.provider.OsintProvider
import com.aistudio.orbit.provider.OsintVerifiableEvidence
import com.aistudio.orbit.provider.ThreatIntelIndicator
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Offline-First Forensic Repository for OSINT Intelligence.
 * Checks local Room database cache before invoking live network providers.
 */
class OsintRepository(
    private val osintDao: OsintCacheDao,
    private val osintProvider: OsintProvider,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
) {

    val cachedReportsFlow: Flow<List<OsintCacheEntity>> = osintDao.getAllCached()
    val threatIndicatorsFlow: Flow<List<OsintThreatIndicatorEntity>> = osintDao.getAllThreatIndicators()

    /**
     * Retrieves full OSINT Analysis Report with Offline-First strategy.
     */
    suspend fun getOsintReport(address: String, network: BlockchainNetwork, forceRefresh: Boolean = false): Result<OsintAnalysisReport> {
        val trimmed = address.trim()
        if (!forceRefresh) {
            val cached = osintProvider.getCachedReport(trimmed)
            if (cached != null) {
                return Result.success(cached)
            }
        }

        // Generate/fetch fresh report from provider / forensics engine
        return try {
            val report = OsintForensicsEngine.performOsintInvestigation(trimmed, network)
            osintProvider.cacheReport(trimmed, network, report)

            // Also cache specialized IP associations into dedicated Room table
            if (report.ipExposures.isNotEmpty()) {
                val ipEntities = report.ipExposures.map { ip ->
                    OsintIpEntity(
                        address = trimmed,
                        ipAddress = ip.ipAddress,
                        ispName = ip.ispName,
                        ispNameFa = ip.ispNameFa,
                        country = ip.country,
                        countryFa = ip.countryFa,
                        city = ip.city,
                        cityFa = ip.cityFa,
                        connectionType = ip.connectionType,
                        connectionTypeFa = ip.connectionTypeFa,
                        torOrVpnDetected = ip.torOrVpnDetected,
                        asn = ip.asn,
                        latencyMs = ip.latencyMs.toLong(),
                        timestamp = System.currentTimeMillis()
                    )
                }
                osintDao.insertIpAssociations(ipEntities)
            }

            // Cache threat indicators into dedicated Room table
            if (report.threatIndicators.isNotEmpty()) {
                val threatEntities = report.threatIndicators.map { t ->
                    OsintThreatIndicatorEntity(
                        indicatorType = t.indicatorType,
                        indicatorValue = t.indicatorValue,
                        threatCategory = t.threatCategory,
                        threatCategoryFa = t.threatCategoryFa,
                        riskScore = t.riskScore,
                        reporter = t.reporter,
                        reportedDate = t.reportedDate,
                        notesEn = t.notesEn,
                        notesFa = t.notesFa,
                        sourceUrl = t.sourceUrl,
                        timestamp = System.currentTimeMillis()
                    )
                }
                osintDao.insertThreatIndicators(threatEntities)
            }

            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves Address Metadata (entity tags, cluster associations, domains) with offline cache fallback.
     */
    suspend fun getAddressMetadata(address: String, network: BlockchainNetwork, forceRefresh: Boolean = false): Result<AddressOsintMetadata> {
        val trimmed = address.trim()
        if (!forceRefresh) {
            val cachedTag = osintDao.getEntityTag(trimmed)
            if (cachedTag != null) {
                val domains = try {
                    json.decodeFromString<List<String>>(cachedTag.associatedDomainsJson)
                } catch (_: Exception) {
                    emptyList()
                }
                val labels = try {
                    json.decodeFromString<List<String>>(cachedTag.publicLabelsJson)
                } catch (_: Exception) {
                    emptyList()
                }
                return Result.success(
                    AddressOsintMetadata(
                        address = cachedTag.address,
                        network = cachedTag.network,
                        knownEntity = cachedTag.knownEntity,
                        knownEntityFa = cachedTag.knownEntityFa,
                        entityCategory = cachedTag.entityCategory,
                        entityCategoryFa = cachedTag.entityCategoryFa,
                        associatedDomains = domains,
                        publicLabels = labels,
                        riskScore = cachedTag.riskScore,
                        attributionSource = cachedTag.attributionSource,
                        attributionSourceFa = cachedTag.attributionSourceFa,
                        verificationEvidence = cachedTag.verificationEvidence,
                        verificationEvidenceFa = cachedTag.verificationEvidenceFa,
                        timestamp = cachedTag.timestamp
                    )
                )
            }
        }

        val remoteRes = osintProvider.fetchAddressMetadata(trimmed, network)
        if (remoteRes.isSuccess) {
            val meta = remoteRes.getOrThrow()
            osintDao.insertEntityTag(
                OsintEntityTagEntity(
                    address = trimmed,
                    network = network.name,
                    knownEntity = meta.knownEntity,
                    knownEntityFa = meta.knownEntityFa,
                    entityCategory = meta.entityCategory,
                    entityCategoryFa = meta.entityCategoryFa,
                    publicLabelsJson = json.encodeToString(meta.publicLabels),
                    associatedDomainsJson = json.encodeToString(meta.associatedDomains),
                    riskScore = meta.riskScore,
                    attributionSource = meta.attributionSource,
                    attributionSourceFa = meta.attributionSourceFa,
                    verificationEvidence = meta.verificationEvidence,
                    verificationEvidenceFa = meta.verificationEvidenceFa,
                    timestamp = meta.timestamp
                )
            )
        }
        return remoteRes
    }

    /**
     * Retrieves Verifiable Evidence Chain.
     */
    suspend fun getVerifiableEvidenceChain(address: String, network: BlockchainNetwork): Result<List<OsintVerifiableEvidence>> {
        return osintProvider.fetchVerifiableEvidenceChain(address.trim(), network)
    }

    /**
     * Retrieves IP Exposure list from Room cache or live provider.
     */
    suspend fun getIpExposure(ipOrAddress: String): Result<OsintIpExposure> {
        val trimmed = ipOrAddress.trim()
        val cachedIps = osintDao.getIpAssociations(trimmed)
        if (cachedIps.isNotEmpty()) {
            val first = cachedIps.first()
            return Result.success(
                OsintIpExposure(
                    ipAddress = first.ipAddress,
                    ispName = first.ispName,
                    ispNameFa = first.ispNameFa,
                    country = first.country,
                    countryFa = first.countryFa,
                    city = first.city,
                    cityFa = first.cityFa,
                    connectionType = first.connectionType,
                    connectionTypeFa = first.connectionTypeFa,
                    torOrVpnDetected = first.torOrVpnDetected,
                    broadcastTime = "Cached record",
                    latencyMs = first.latencyMs.toInt(),
                    asn = first.asn,
                    coordinateX = 0f,
                    coordinateY = 0f
                )
            )
        }
        return osintProvider.fetchIpMetadata(trimmed)
    }

    /**
     * Checks Threat Intelligence for any indicator.
     */
    suspend fun checkThreatIntelligence(indicator: String): Result<List<ThreatIntelIndicator>> {
        val trimmed = indicator.trim()
        val cached = osintDao.getThreatIndicators(trimmed, trimmed)
        if (cached.isNotEmpty()) {
            val list = cached.map { c ->
                ThreatIntelIndicator(
                    indicatorType = c.indicatorType,
                    indicatorValue = c.indicatorValue,
                    threatCategory = c.threatCategory,
                    threatCategoryFa = c.threatCategoryFa,
                    riskScore = c.riskScore,
                    reporter = c.reporter,
                    reportedDate = c.reportedDate,
                    notesEn = c.notesEn,
                    notesFa = c.notesFa,
                    sourceUrl = c.sourceUrl
                )
            }
            return Result.success(list)
        }

        val remoteRes = osintProvider.checkThreatIntelligence(trimmed)
        if (remoteRes.isSuccess) {
            val list = remoteRes.getOrThrow()
            val entities = list.map { t ->
                OsintThreatIndicatorEntity(
                    indicatorType = t.indicatorType,
                    indicatorValue = t.indicatorValue,
                    threatCategory = t.threatCategory,
                    threatCategoryFa = t.threatCategoryFa,
                    riskScore = t.riskScore,
                    reporter = t.reporter,
                    reportedDate = t.reportedDate,
                    notesEn = t.notesEn,
                    notesFa = t.notesFa,
                    sourceUrl = t.sourceUrl,
                    timestamp = System.currentTimeMillis()
                )
            }
            osintDao.insertThreatIndicators(entities)
        }
        return remoteRes
    }

    /**
     * Clears cached investigation from Room.
     */
    suspend fun deleteCachedReport(address: String) {
        osintProvider.deleteCachedReport(address)
    }
}
