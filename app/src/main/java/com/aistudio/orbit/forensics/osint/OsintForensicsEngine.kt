package com.aistudio.orbit.forensics.osint

import com.aistudio.orbit.model.BlockchainNetwork
import java.security.MessageDigest
import kotlin.math.abs
import kotlinx.serialization.Serializable

@Serializable
data class OsintLeakRecord(
    val title: String,
    val titleFa: String,
    val source: String,
    val date: String,
    val emailAssociated: String,
    val aliasAssociated: String,
    val phoneAssociated: String,
    val leakedDataCount: Int,
    val dataDetailsEn: String,
    val dataDetailsFa: String,
    val confidenceScore: Float
)

@Serializable
data class OsintIpExposure(
    val ipAddress: String,
    val ispName: String,
    val ispNameFa: String,
    val country: String,
    val countryFa: String,
    val city: String,
    val cityFa: String,
    val connectionType: String,
    val connectionTypeFa: String,
    val torOrVpnDetected: Boolean,
    val broadcastTime: String,
    val latencyMs: Int,
    val asn: String,
    val coordinateX: Float,
    val coordinateY: Float
)

@Serializable
data class SubpoenaTemplate(
    val exchangeName: String,
    val letterSubjectEn: String,
    val letterSubjectFa: String,
    val bodyEn: String,
    val bodyFa: String,
    val requiredInformationEn: List<String>,
    val requiredInformationFa: List<String>
)

@Serializable
data class OsintAnalysisReport(
    val address: String,
    val network: BlockchainNetwork,
    val leakRecords: List<OsintLeakRecord>,
    val ipExposures: List<OsintIpExposure>,
    val torVpnProbability: Float, // 0 to 100
    val likelyOperatorCountryEn: String,
    val likelyOperatorCountryFa: String,
    val estimatedUserTypeEn: String,
    val estimatedUserTypeFa: String,
    val subpoenaTemplates: List<SubpoenaTemplate>,
    val threatIndicators: List<com.aistudio.orbit.provider.ThreatIntelIndicator> = emptyList(),
    val aggregateRiskScore: Int = 20,
    val timestamp: Long = System.currentTimeMillis()
)

object OsintForensicsEngine {

    /**
     * Generates a factual OSINT analysis report based on the target address and verified intelligence.
     * Adheres strictly to the Zero Mock Data policy: no fake leaks, fake IPs, or random personas are generated.
     */
    fun performOsintInvestigation(
        address: String,
        network: BlockchainNetwork,
        verifiedLeaks: List<OsintLeakRecord> = emptyList(),
        verifiedIps: List<OsintIpExposure> = emptyList(),
        verifiedThreats: List<com.aistudio.orbit.provider.ThreatIntelIndicator> = emptyList()
    ): OsintAnalysisReport {
        val trimmedAddress = address.trim()

        // 1. Evaluate Leaks & IP Exposures (Real data only)
        val leaks = verifiedLeaks
        val ips = verifiedIps

        // 2. Compute Probability of TOR or VPN Tunnels based on verified IP observations
        var torCount = 0
        ips.forEach { if (it.torOrVpnDetected) torCount++ }
        val torVpnProb = if (ips.isNotEmpty()) (torCount.toFloat() / ips.size.toFloat()) * 100f else 0f

        // 3. Evaluate operator geography and profile from real indicators
        val containsIranIp = ips.any { it.country.equals("Iran", ignoreCase = true) || it.countryFa.contains("ایران") }
        val likelyOperatorCountryEn = when {
            containsIranIp -> "Iran (Observed IP Broadcast)"
            ips.isNotEmpty() -> ips.first().country
            else -> "Unknown (No verified IP telemetry)"
        }
        val likelyOperatorCountryFa = when {
            containsIranIp -> "ایران (مشاهده‌شده در انتشار IP)"
            ips.isNotEmpty() -> ips.first().countryFa
            else -> "نامشخص (عدم وجود ردپای ثبت‌شده IP)"
        }

        val estimatedUserTypeEn = when {
            torVpnProb > 60f -> "Privacy-Conscious / Obfuscated Routing (Inference)"
            containsIranIp -> "Regional Trader / Client Node (Inference)"
            leaks.isNotEmpty() -> "Publicly Attributed Digital Footprint (Observed)"
            else -> "Standard Unassociated Address (Unknown Attribution)"
        }
        val estimatedUserTypeFa = when {
            torVpnProb > 60f -> "استفاده از شبکه ناشناس‌سازی و مسیریابی امن (استنباط)"
            containsIranIp -> "گره یا کاربر در حوزه جغرافیایی محلی (استنباط)"
            leaks.isNotEmpty() -> "دارای ردپای افشاشده در منابع باز (مشاهده‌شده)"
            else -> "آدرس استاندارد بدون انتساب عمومی (نامشخص)"
        }

        // 4. Generate structured Subpoena Letter templates
        val subpoenas = generateSubpoenaTemplates(trimmedAddress, network)

        // 5. Threat Intel indicators (Only real verified observations)
        val threats = verifiedThreats

        val riskScore = when {
            torVpnProb > 60f -> 75
            leaks.isNotEmpty() -> 50
            threats.any { it.riskScore >= 70 } -> 80
            threats.isNotEmpty() -> threats.maxOf { it.riskScore }
            else -> 0
        }

        return OsintAnalysisReport(
            address = trimmedAddress,
            network = network,
            leakRecords = leaks,
            ipExposures = ips,
            torVpnProbability = torVpnProb,
            likelyOperatorCountryEn = likelyOperatorCountryEn,
            likelyOperatorCountryFa = likelyOperatorCountryFa,
            estimatedUserTypeEn = estimatedUserTypeEn,
            estimatedUserTypeFa = estimatedUserTypeFa,
            subpoenaTemplates = subpoenas,
            threatIndicators = threats,
            aggregateRiskScore = riskScore,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun generateSubpoenaTemplates(address: String, network: BlockchainNetwork): List<SubpoenaTemplate> {
        val template1 = SubpoenaTemplate(
            exchangeName = "Virtual Asset Service Provider (VASP) Legal & Compliance Desk",
            letterSubjectEn = "Formal Judicial Disclosure Request: Address $address",
            letterSubjectFa = "برگ استعلام قضایی رسمی: استخراج اطلاعات حساب آدرس $address",
            bodyEn = """
                Dear Compliance Officer / Legal Operations,

                Pursuant to authorized judicial inquiry concerning financial forensics analysis, you are hereby requested to identify and disclose any Account Information, transaction audit trails, and KYC verification records associated with the wallet address listed below:

                TARGET ADDRESS: $address
                BLOCKCHAIN NETWORK: ${network.displayName}
                INVESTIGATION SCOPE: Historical deposit/withdrawal interactions and associated account telemetry

                Kindly provide the response in a structured and encrypted format in accordance with applicable financial crimes compliance protocols.
            """.trimIndent(),
            bodyFa = """
                مدیریت محترم واحد حقوقی و انطباق ارائه‌دهنده خدمات دارایی دیجیتال (VASP)

                با عنایت به دستور مراجع قانونی و در راستای تکمیل تحقیقات پرونده جرایم مالی، بدین‌وسیله مقتضی است نسبت به بررسی سوابق، استخراج اطلاعات هویتی (KYC)، تاریخچه ورود و خروج، آدرس‌های IP و حساب‌های بانکی متصل به آدرس زیر اقدام و نتیجه را ارسال فرمایید:

                آدرس هدف تحقیقات: $address
                شبکه بلاکچین: ${network.displayName}
                بازه زمانی: کلیه تراکنش‌های واریز یا برداشت مرتبط

                خواهشمند است دستور فرمایید تا زمان تکمیل بررسی‌ها محرمانگی استعلام حفظ گردد.
            """.trimIndent(),
            requiredInformationEn = listOf(
                "Full Legal Name and Registered Email / Phone",
                "IP Access Logs with Timestamps and User-Agent",
                "Connected Banking Details / Fiat Payment Gateways",
                "Official Identification Documents on File"
            ),
            requiredInformationFa = listOf(
                "نام و نام خانوادگی، شماره همراه و نشانی رایانامه ثبت‌شده کاربر",
                "لاگ‌های دسترسی و آدرس‌های IP ورود به همراه تاریخ و زمان دقیق",
                "حساب‌های بانکی، کارت‌ها و درگاه‌های پرداخت متصل",
                "تصاویر مدارک هویتی ثبت‌شده در مرحله احراز هویت"
            )
        )

        val template2 = SubpoenaTemplate(
            exchangeName = "Domestic Payment & Exchange Gateway Legal Desk",
            letterSubjectEn = "Judicial Inquiry - Domestic Gateway & Settlement Analysis",
            letterSubjectFa = "استعلام قضایی رسمی - درگاه پرداخت و تسویه داخلی",
            bodyEn = """
                To Legal Operations / Domestic Gateway Desk,

                Under formal judicial inquiry, we demand the extraction of telemetry and identification details for accounts executing transactions to or receiving funds from the following digital asset address:

                ADDRESS: $address
                NETWORK: ${network.displayName}

                Please preserve and provide registration records, access logs, and related settlement details.
            """.trimIndent(),
            bodyFa = """
                به مدیریت محترم حقوقی درگاه پرداخت و تسویه دارایی دیجیتال

                در اجرای دستور قضایی صادره، مقرر است نسبت به بررسی و استخراج اطلاعات کاربری، لاگ‌های سیستمی و تراکنش‌های بانکی متناظر با آدرس زیر اقدام فرمایید:

                آدرس بلاکچین هدف: $address
                شبکه بلاکچین: ${network.displayName}

                خواهشمند است مستندات مربوطه در اسرع وقت به این مرجع ارسال گردد.
            """.trimIndent(),
            requiredInformationEn = listOf(
                "User Account Profile & Verification Identity",
                "Internal Ledger transfers matching the address flows",
                "National ID, Bank account IBAN and card details",
                "Mobile Phone Geolocation / Registration Cell Info"
            ),
            requiredInformationFa = listOf(
                "پروفایل کاربری و اطلاعات هویتی تاییدشده کاربر در سامانه",
                "تراکنش‌های دفتری (سفارشات خرید و فروش) و انتقالات داخلی صرافی",
                "شماره ملی، شماره شبا و شماره کارت‌های متناظر بانکی",
                "شماره همراه تایید شده و تاریخچه آخرین تغییر شماره یا دستگاه ورود"
            )
        )

        return listOf(template1, template2)
    }
}
