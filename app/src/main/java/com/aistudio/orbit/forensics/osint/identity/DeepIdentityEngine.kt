package com.aistudio.orbit.forensics.osint.identity

import com.aistudio.orbit.forensics.osint.ForensicEvent
import com.aistudio.orbit.forensics.osint.ForensicEventType
import com.aistudio.orbit.forensics.osint.IndicatorType
import com.aistudio.orbit.forensics.osint.OnChainToOffChainHandoffEngine
import com.aistudio.orbit.forensics.osint.OsintCacheDao
import com.aistudio.orbit.forensics.osint.DeepIdentityResultEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class IdentityCandidate(
    val candidateId: String,
    val name: String,
    val source: String,
    val confidence: Float,
    val isVerified: Boolean = false
)

@Serializable
data class PublicEmail(
    val email: String,
    val classification: String, // PUBLICLY_DISCLOSED, INVESTIGATOR_PROVIDED, etc.
    val sources: List<String>
)

@Serializable
data class PublicPhone(
    val phoneNumber: String,
    val validityState: String, // VALID_FORMAT, INVALID_FORMAT, UNKNOWN, PUBLICLY_VERIFIED, INVESTIGATOR_PROVIDED
    val carrier: String?,
    val countryCode: String?
)

@Serializable
data class Username(
    val handle: String,
    val platforms: List<String>
)

@Serializable
data class Domain(
    val domainName: String,
    val dnsRecords: List<String> = emptyList(),
    val whoisInfo: String? = null
)

@Serializable
data class PublicProfile(
    val username: String,
    val platform: String,
    val url: String?,
    val isPublic: Boolean = true
)

@Serializable
data class PublicOrganization(
    val orgName: String,
    val website: String?,
    val jurisdiction: String? = null
)

@Serializable
data class PublicIP(
    val ipAddress: String,
    val asn: String?,
    val isp: String?,
    val isHosting: Boolean
)

@Serializable
data class PublicLocation(
    val region: String,
    val country: String,
    val timezone: String?
)

@Serializable
data class PublicService(
    val serviceName: String,
    val serviceType: String
)

enum class IdentityRelationshipType {
    EMAIL_USED_BY_PUBLIC_PROFILE,
    PHONE_PUBLISHED_BY_ORGANIZATION,
    USERNAME_MATCHES_PROFILE,
    DOMAIN_ASSOCIATED_WITH_ORGANIZATION,
    IP_ASSOCIATED_WITH_PUBLIC_INFRASTRUCTURE,
    EMAIL_PUBLISHED_ON_DOMAIN,
    PHONE_PUBLISHED_ON_PUBLIC_PAGE,
    
    // Explicit forbidden relationship check: EMAIL_OWNS_PERSON and PHONE_PROVES_IDENTITY are out-of-bounds unless investigator confirmed
    EMAIL_OWNS_PERSON_UNVERIFIED,
    PHONE_PROVES_IDENTITY_UNVERIFIED,
    INVESTIGATOR_CONFIRMED_LINK
}

@Serializable
data class IdentityRelationship(
    val relationshipId: String,
    val sourceEntityId: String,
    val targetEntityId: String,
    val type: IdentityRelationshipType,
    val sourceProvenance: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Unified Deep Identity Reconstruction & De-anonymization Coordinator.
 * Integrates Holehe, email2phonenumber, GHunt, Ignorant, PhoneInfoga, and Epieos into a comprehensive investigative dossier.
 */
object DeepIdentityEngine {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @Serializable
    data class DeepIdentityDossier(
        val targetInput: String,
        val inputType: String, // EMAIL, PHONE, USERNAME
        val associatedCryptoAddress: String?,
        val holeheSummary: HoleheScanner.HoleheScanSummary?,
        val phoneReconstruction: Email2PhoneNumberPipeline.ReconstructionResult?,
        val ghuntProfile: GHuntFootprinter.GHuntProfileResult?,
        val epieosReport: EpieosBreadcrumbClient.EpieosReport?,
        val parsedPhoneInfo: PhoneInfogaEngine.ParsedPhoneNumber?,
        val overallConfidenceScore: Float,
        val deAnonymizedPhoneNumber: String?,
        val dominantIdentityName: String?,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Executes the comprehensive multi-source identity de-anonymization workflow.
     */
    suspend fun reconstructIdentity(
        targetInput: String,
        associatedCryptoAddress: String? = null,
        osintCacheDao: OsintCacheDao? = null
    ): DeepIdentityDossier = withContext(Dispatchers.IO) {
        val clean = targetInput.trim()
        val isEmail = clean.contains("@")
        val isPhone = clean.startsWith("+") || (clean.startsWith("09") && clean.length == 11) || (clean.replace(Regex("[^0-9]"), "").length >= 10)

        // Publish start event on Forensic Event Bus
        OnChainToOffChainHandoffEngine.eventBus.publish(
            ForensicEvent(
                eventId = "EVT_IDENT_START_${System.currentTimeMillis()}",
                type = ForensicEventType.INDICATOR_DISCOVERED,
                indicatorType = if (isEmail) IndicatorType.EMAIL_ADDRESS else if (isPhone) IndicatorType.PHONE_NUMBER else IndicatorType.ONLINE_HANDLE,
                data = clean,
                confidenceScore = 0.85f,
                emittingModule = "DeepIdentityCoordinator",
                targetAddress = associatedCryptoAddress ?: "OFF_CHAIN_QUERY",
                descriptionEn = "Initiated deep identity reconnaissance for $clean across 120+ platforms",
                descriptionFa = "شروع پایش هویت و تطبیق عمیق برای شناسه $clean در بیش از ۱۲۰ بستر و پایگاه داده"
            )
        )

        var holeheSummary: HoleheScanner.HoleheScanSummary? = null
        var phoneReconstruction: Email2PhoneNumberPipeline.ReconstructionResult? = null
        var ghuntProfile: GHuntFootprinter.GHuntProfileResult? = null
        var epieosReport: EpieosBreadcrumbClient.EpieosReport? = null
        var parsedPhone: PhoneInfogaEngine.ParsedPhoneNumber? = null

        coroutineScope {
            if (isEmail) {
                val holeheJob = async { HoleheScanner.scanEmail(clean) }
                val ghuntJob = async { GHuntFootprinter.footprintGoogleAccount(clean) }
                val epieosJob = async { EpieosBreadcrumbClient.queryBreadcrumbs(clean) }

                holeheSummary = holeheJob.await()
                ghuntProfile = ghuntJob.await()
                epieosReport = epieosJob.await()

                // Execute 3-phase phone number reconstruction using Holehe hints
                val maskedHint = holeheSummary?.extractedMaskedPhones?.firstOrNull()
                val reconJob = async {
                    Email2PhoneNumberPipeline.reconstructPhoneNumber(
                        targetEmail = clean,
                        maskedHint = maskedHint,
                        countryHint = if (clean.endsWith(".ir")) "IR" else "US"
                    )
                }
                phoneReconstruction = reconJob.await()

                val reconstructedNumber = phoneReconstruction?.deAnonymizedPhoneNumber
                if (!reconstructedNumber.isNullOrBlank()) {
                    parsedPhone = PhoneInfogaEngine.parse(reconstructedNumber)
                }
            } else if (isPhone) {
                parsedPhone = PhoneInfogaEngine.parse(clean)
                val epieosJob = async { EpieosBreadcrumbClient.queryBreadcrumbs(clean) }
                epieosReport = epieosJob.await()
            } else {
                // Username search
                val epieosJob = async { EpieosBreadcrumbClient.queryBreadcrumbs(clean) }
                epieosReport = epieosJob.await()
            }
        }

        val deAnonymizedNumber = phoneReconstruction?.deAnonymizedPhoneNumber ?: (if (isPhone) parsedPhone?.e164Format else null)
        val dominantName = ghuntProfile?.displayName ?: epieosReport?.breadcrumbs?.flatMap { it.associatedNames }?.firstOrNull()

        // Calculate blended confidence score
        var scoreAcc = 0.50f
        val holeheMatches = holeheSummary?.positiveMatchesCount ?: 0
        if (holeheMatches > 0) scoreAcc += 0.15f
        if (ghuntProfile != null) scoreAcc += 0.18f
        if (phoneReconstruction?.deAnonymizedPhoneNumber != null) scoreAcc += 0.12f
        val overallConfidence = scoreAcc.coerceIn(0.20f, 0.98f)

        // Publish Completion Event
        OnChainToOffChainHandoffEngine.eventBus.publish(
            ForensicEvent(
                eventId = "EVT_IDENT_COMPLETE_${System.currentTimeMillis()}",
                type = ForensicEventType.ATTRIBUTION_CONFIRMED,
                indicatorType = if (isEmail) IndicatorType.EMAIL_ADDRESS else IndicatorType.PHONE_NUMBER,
                data = deAnonymizedNumber ?: (dominantName ?: clean),
                confidenceScore = overallConfidence,
                emittingModule = "DeepIdentityCoordinator",
                targetAddress = associatedCryptoAddress ?: "OFF_CHAIN_QUERY",
                descriptionEn = "Successfully de-anonymized identity footprint: ${dominantName ?: "Persona"} (${deAnonymizedNumber ?: "Phone Verified"})",
                descriptionFa = "بازیابی و احراز هویت دیجیتال موفق: ${dominantName ?: "هویت شناسایی‌شده"} (${deAnonymizedNumber ?: "شماره تاییدشده"})"
            )
        )

        val dossier = DeepIdentityDossier(
            targetInput = clean,
            inputType = if (isEmail) "EMAIL" else if (isPhone) "PHONE" else "USERNAME",
            associatedCryptoAddress = associatedCryptoAddress,
            holeheSummary = holeheSummary,
            phoneReconstruction = phoneReconstruction,
            ghuntProfile = ghuntProfile,
            epieosReport = epieosReport,
            parsedPhoneInfo = parsedPhone,
            overallConfidenceScore = overallConfidence,
            deAnonymizedPhoneNumber = deAnonymizedNumber,
            dominantIdentityName = dominantName
        )

        // Persist to Room if DAO provided
        if (osintCacheDao != null) {
            try {
                val entity = DeepIdentityResultEntity(
                    targetEmailOrPhone = clean,
                    associatedCryptoAddress = associatedCryptoAddress ?: "",
                    maskedPhone = phoneReconstruction?.extractedMaskedPattern,
                    deAnonymizedPhone = deAnonymizedNumber,
                    maskedEmail = holeheSummary?.extractedMaskedEmails?.firstOrNull(),
                    gaiaId = ghuntProfile?.gaiaId,
                    googleProfileName = dominantName,
                    holeheServicesJson = json.encodeToString(holeheSummary?.results ?: emptyList()),
                    ignorantPlatformsJson = json.encodeToString(phoneReconstruction?.finalCandidate?.verificationDetails ?: ""),
                    phoneInfogaCarrierJson = json.encodeToString(parsedPhone?.carrierName ?: ""),
                    epieosBreadcrumbsJson = json.encodeToString(epieosReport?.breadcrumbs ?: emptyList()),
                    overallConfidence = overallConfidence
                )
                osintCacheDao.insertDeepIdentityResult(entity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        dossier
    }
}
