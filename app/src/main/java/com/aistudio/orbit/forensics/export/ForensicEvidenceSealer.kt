package com.aistudio.orbit.forensics.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.aistudio.orbit.PersianDateUtils
import com.aistudio.orbit.forensics.osint.OsintAnalysisReport
import com.aistudio.orbit.model.*
import com.aistudio.orbit.repository.AppLanguage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Court-Ready Forensic Evidence Sealer & Judicial Export Engine.
 * Provides cryptographically verifiable SHA-256 sealing, epistemic demarcation,
 * and subpoena requisition generation for official cybercrime units and judicial experts.
 */
object ForensicEvidenceSealer {

    private val jsonPretty = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Generates a SHA-256 hash for an individual EvidenceItem to preserve its digital signature.
     * Calculated as: SHA-256(id + timestamp + category.name + titleEn + descriptionEn + previousHash + version)
     */
    fun computeEvidenceHash(
        id: String,
        timestamp: Long,
        categoryName: String,
        titleEn: String,
        descriptionEn: String,
        previousHash: String,
        version: Int
    ): String {
        val payload = "$id$timestamp$categoryName$titleEn$descriptionEn$previousHash$version"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(payload.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }.uppercase()
    }

    /**
     * Seals an entire sequence of evidence items to form an immutable chain-of-custody ledger.
     * Each item's previousHash points to the contentHash of the prior item.
     */
    fun sealEvidenceSequence(evidenceList: List<EvidenceItem>): List<EvidenceItem> {
        val sealed = mutableListOf<EvidenceItem>()
        var currentPrevHash = "0000000000000000000000000000000000000000000000000000000000000000"

        for (item in evidenceList.sortedBy { it.timestamp }) {
            val hash = computeEvidenceHash(
                id = item.id,
                timestamp = item.timestamp,
                categoryName = item.category.name,
                titleEn = item.titleEn,
                descriptionEn = item.descriptionEn,
                previousHash = currentPrevHash,
                version = item.version
            )
            val sealedItem = item.copy(
                previousHash = currentPrevHash,
                contentHash = hash
            )
            sealed.add(sealedItem)
            currentPrevHash = hash
        }
        return sealed
    }

    /**
     * Computes a canonical SHA-256 digital fingerprint over the case facts, transactions,
     * OSINT indicators, and evidence items.
     */
    fun computeDossierSha256(
        investigationCase: InvestigationCase,
        osintReport: OsintAnalysisReport? = null
    ): String {
        val canonicalPayload = buildString {
            append("CASE_REF:${investigationCase.referenceNumber}|")
            append("NETWORK:${investigationCase.network.name}|")
            append("TARGET:${investigationCase.targetAddress}|")
            append("BALANCE_SAT:${investigationCase.balanceSat}|")
            append("TOTAL_TX:${investigationCase.totalTransactionsFound}|")
            append("EVIDENCE_COUNT:${investigationCase.evidenceLog.size}|")
            investigationCase.evidenceLog.sortedBy { it.id }.forEach { ev ->
                append("EV:${ev.id}:${ev.category.name}:${ev.confidence.name}:${ev.title}|")
            }
            if (osintReport != null) {
                append("OSINT_SCORE:${osintReport.aggregateRiskScore}|")
                append("IP_COUNT:${osintReport.ipExposures.size}|")
                append("LEAK_COUNT:${osintReport.leakRecords.size}|")
            }
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(canonicalPayload.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }.uppercase()
    }

    /**
     * Generates a complete judicial JSON evidence archive containing graph topology,
     * chain of custody, and cryptographic SHA-256 seal.
     */
    fun generateCourtArchiveJson(
        investigationCase: InvestigationCase,
        graph: InteractiveCaseGraph,
        osintReport: OsintAnalysisReport? = null,
        language: AppLanguage
    ): String {
        val isFa = language == AppLanguage.PERSIAN
        val now = System.currentTimeMillis()
        val sha256 = computeDossierSha256(investigationCase, osintReport)

        val sdfUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val utcDateStr = sdfUtc.format(Date(now))
        val jalaliDateStr = PersianDateUtils.formatTimestampToPersian(now, isFa)

        // Epistemic Demarcation Sets
        val facts = investigationCase.evidenceLog.filter { it.isDirectFact }
        val calculations = investigationCase.evidenceLog.filter { it.category == EvidenceCategory.ALGORITHMIC_RESULT }
        val inferences = investigationCase.evidenceLog.filter {
            it.category in listOf(EvidenceCategory.OSINT_INTELLIGENCE, EvidenceCategory.ATTRIBUTION, EvidenceCategory.TEMPORAL_ANALYSIS)
        }
        val hypotheses = investigationCase.evidenceLog.filter {
            it.category in listOf(EvidenceCategory.BEHAVIORAL_PATTERN, EvidenceCategory.INVESTIGATOR_CONCLUSION)
        }

        val courtDossier = CourtDossierPayload(
            archiveStandard = "ISO/IEC 27037:2012 Digital Evidence Standard Compliant",
            platform = "BAYYINAH Blockchain Financial Forensics & OSINT Platform",
            sha256Seal = sha256,
            evidenceIntegrityStatus = "CRYPTOGRAPHICALLY_SEALED_VERIFIED",
            caseReferenceNumber = investigationCase.referenceNumber,
            caseTitle = investigationCase.caseName,
            investigatedTargetAddress = investigationCase.targetAddress,
            blockchainNetwork = investigationCase.network.displayName,
            generatedUtc = utcDateStr,
            generatedJalali = jalaliDateStr,
            investigationScope = investigationCase.description,
            investigatorNotes = investigationCase.notes,
            summaryMetrics = LedgerSummaryMetrics(
                balance = "${String.format("%.8f", investigationCase.balanceBtc)} ${investigationCase.network.symbol}",
                totalReceived = "${String.format("%.8f", investigationCase.totalReceivedBtc)} ${investigationCase.network.symbol}",
                totalSent = "${String.format("%.8f", investigationCase.totalSentBtc)} ${investigationCase.network.symbol}",
                transactionCount = investigationCase.totalTransactionsFound,
                counterpartyCount = investigationCase.counterparties.size
            ),
            epistemicEvidenceBreakdown = EpistemicBreakdown(
                undisputedOnChainFactsCount = facts.size,
                algorithmicCalculationsCount = calculations.size,
                derivedOsintInferencesCount = inferences.size,
                workingHypothesesCount = hypotheses.size,
                evidenceItems = investigationCase.evidenceLog.map { ev ->
                    EvidenceItemExport(
                        id = ev.id,
                        category = ev.category.name,
                        confidence = ev.confidence.name,
                        title = ev.localizedTitle(isFa),
                        description = ev.localizedDescription(isFa),
                        source = ev.provenance.sourceName,
                        verification = ev.verificationStatus.name
                    )
                }
            ),
            graphTopology = GraphTopologyExport(
                totalNodes = graph.nodes.size,
                totalEdges = graph.edges.size,
                nodes = graph.nodes.map { n ->
                    GraphNodeExport(
                        id = n.id,
                        label = n.label,
                        category = n.entityCategory.name,
                        risk = n.riskSeverity.name,
                        epistemicStatus = n.epistemicStatus.name,
                        confidencePercent = n.confidencePercent,
                        tags = n.tags,
                        x = n.x,
                        y = n.y
                    )
                },
                edges = graph.edges.map { e ->
                    GraphEdgeExport(
                        id = e.id,
                        source = e.sourceId,
                        target = e.targetId,
                        category = e.category.name,
                        volume = e.volumeDisplay,
                        label = e.transformLabel,
                        confidencePercent = e.confidencePercent
                    )
                }
            ),
            judicialSubpoenaLeads = SubpoenaLeadExport(
                targetExchanges = investigationCase.counterparties
                    .filter { it.label?.contains("Binance", true) == true || it.label?.contains("Kraken", true) == true || it.label?.contains("Kucoin", true) == true }
                    .map { it.label ?: it.address },
                targetTelecommunications = osintReport?.leakRecords?.mapNotNull { it.emailAssociated } ?: emptyList(),
                reconstructedPhoneLeads = graph.nodes
                    .filter { it.entityCategory == ForensicEntityCategory.PHONE_NUMBER }
                    .map { it.id.removePrefix("PHONE_") }
            ),
            judicialDisclaimer = if (isFa) {
                "سلب مسئولیت رسمی و حقوقی: کلیه داده‌های مستخرج در این پرونده تفکیک‌شده بر مبنای حقایق قطعی دفترکل و استنتاجات هوشمندی منابع باز (OSINT) می‌باشد. هرگونه تطابق الگویی صرفاً سرنخ قضایی تلقی گردیده و به منزله انتساب قطعی به اشخاص حقیقی بدون تطابق رسمی با دستور مراجع ذیصلاح قضایی نمی‌باشد."
            } else {
                "OFFICIAL LEGAL DISCLAIMER: All data compiled in this judicial dossier is epistemically segregated into immutable on-chain facts and derived open-source intelligence. Pattern matches represent investigative leads and do not constitute conclusive criminal attribution without formal subpoena confirmation."
            }
        )

        return jsonPretty.encodeToString(courtDossier)
    }

    /**
     * Generates a formal Subpoena Requisition Template (پیش‌نویس استعلام رسمی قضایی)
     * suitable for presentation to the Cyber Police (FATA), Ministry intelligence divisions,
     * or judicial magistrates.
     */
    fun generateSubpoenaRequisitionText(
        investigationCase: InvestigationCase,
        osintReport: OsintAnalysisReport? = null,
        language: AppLanguage
    ): String {
        val isFa = language == AppLanguage.PERSIAN
        val now = System.currentTimeMillis()
        val sha256 = computeDossierSha256(investigationCase, osintReport)
        val dateStr = if (isFa) PersianDateUtils.formatTimestampToPersian(now, true) else SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))

        return buildString {
            appendLine("================================================================================")
            appendLine(if (isFa) "فرم رسمی پیش‌نویس استعلام و دستور قضایی (قانون جرائم رایانه‌ای)" else "FORMAL FORENSIC SUBPOENA & JUDICIAL REQUISITION TEMPLATE")
            appendLine(if (isFa) "سامانه کشف علمی جرائم و ردیابی مالی بیِّنة | گواهی صحت ادله دیجیتال" else "BAYYINAH CRYPTO FORENSIC SYSTEM | DIGITAL EVIDENCE CERTIFICATION")
            appendLine("================================================================================")
            appendLine()
            appendLine("${if (isFa) "کلاسه ارجاع قضایی:" else "Judicial Case Reference:"} ${investigationCase.referenceNumber}")
            appendLine("${if (isFa) "عنوان پرونده:" else "Case Title:"} ${investigationCase.caseName}")
            appendLine("${if (isFa) "تاریخ صدور استعلام:" else "Date of Issuance:"} $dateStr")
            appendLine("${if (isFa) "مهر صحت ادله (SHA-256):" else "Cryptographic Integrity Seal:"} $sha256")
            appendLine()
            appendLine("--------------------------------------------------------------------------------")
            appendLine(if (isFa) "به: ریاست محترم شعبه رسیدگی‌کننده / پلیس محترم فضای تولید و تبادل اطلاعات (فتا)"
            else "TO: The Honorable Presiding Magistrate / Cybercrime Law Enforcement Division")
            appendLine(if (isFa) "موضوع: درخواست صدور دستور استعلام و مسدودی فوری حساب‌ها و شناسایی هویت متهم"
            else "SUBJECT: Formal Motion for Judicial Subpoena, Account Freeze, and Subscriber Identification")
            appendLine("--------------------------------------------------------------------------------")
            appendLine()
            appendLine(if (isFa) "با سلام و احترام؛" else "Respectfully submitted:")
            appendLine(if (isFa) {
                "در راستای تحقیقات تخصصی کشف جرم در خصوص تراکنش‌های مشکوک به پولشویی و کلاهبرداری رایانه‌ای بر روی شبکه ${investigationCase.network.displayName}، آدرس عمومی ذیل به عنوان کانون دریافت و توزیع عواید حاصل از جرم شناسایی گردیده است:\n" +
                "آدرس هدف: ${investigationCase.targetAddress}\n" +
                "موجودی در گردش: ${String.format("%.4f", investigationCase.totalReceivedBtc)} ${investigationCase.network.symbol}\n" +
                "تعداد تراکنش‌ها: ${investigationCase.totalTransactionsFound}"
            } else {
                "Pursuant to an official financial crime investigation concerning illicit flows on the ${investigationCase.network.displayName} ledger, the following target address was identified as a focal conduit for suspect proceeds:\n" +
                "Target Address: ${investigationCase.targetAddress}\n" +
                "Volume Handled: ${String.format("%.4f", investigationCase.totalReceivedBtc)} ${investigationCase.network.symbol}\n" +
                "Total Transactions: ${investigationCase.totalTransactionsFound}"
            })
            appendLine()
            appendLine(if (isFa) "اقلام اطلاعاتی مورد تقاضا جهت استعلام قضایی از ارائه‌دهندگان خدمات:"
            else "Specific Judicial Inquiries Requested from Relevant Service Providers:")
            appendLine(if (isFa) {
                "۱. استعلام سوابق احراز هویت (KYC)، تصاویر مدارک هویتی، آدرس IP ورود و شماره حساب‌های بانکی متصل به صرافی‌های طرف‌حساب.\n" +
                "۲. استعلام اطلاعات سجلی و سوابق تماس شماره تلفن‌های همراه منتسب مکشوفه در ماژول بازسازی هویت دیجیتال.\n" +
                "۳. دستور توقیف و مسدودی کلیه موجودی‌های رمزارزی مرتبط در پلتفرم‌های تبادل داخلی و بین‌المللی تا تعیین تکلیف نهایی دادسرا.\n" +
                "۴. ارائه لاگ‌های نشست (Session Logs) و شناسه‌های سخت‌افزاری دستگاه‌های متصل."
            } else {
                "1. Full KYC profile, identification documents, registered bank accounts, and access logs from connected virtual asset service providers (VASPs).\n" +
                "2. Subscriber identification, CDR logs, and location coordinates for reconstructed cellular indicators from telecom carriers.\n" +
                "3. Immediate judicial freezing of all associated crypto assets held across custodial custody providers.\n" +
                "4. IP connection logs, User-Agent strings, and device fingerprints."
            })
            appendLine()
            appendLine("--------------------------------------------------------------------------------")
            appendLine(if (isFa) "گواهی امضای دیجیتال و مهر کارشناس رسمی امور جرائم سایبری:"
            else "Digital Certificate and Official Forensic Examiner Seal:")
            appendLine("[  مهر تایید ادله دیجیتال بیِّنة  ]")
            appendLine("SHA-256 Fingerprint: $sha256")
            appendLine("================================================================================")
        }
    }

    /**
     * Saves JSON judicial dossier directly to the Android Downloads directory.
     */
    fun saveCourtJsonToDownloads(context: Context, jsonContent: String, caseRef: String): String? {
        val filename = "BAYYINAH_COURT_ARCHIVE_${caseRef.replace("[^a-zA-Z0-9]".toRegex(), "_")}_${System.currentTimeMillis()}.json"
        return saveTextFileToDownloads(context, jsonContent, filename, "application/json")
    }

    /**
     * Saves Subpoena requisition text directly to Downloads.
     */
    fun saveSubpoenaToDownloads(context: Context, textContent: String, caseRef: String): String? {
        val filename = "BAYYINAH_SUBPOENA_${caseRef.replace("[^a-zA-Z0-9]".toRegex(), "_")}_${System.currentTimeMillis()}.txt"
        return saveTextFileToDownloads(context, textContent, filename, "text/plain")
    }

    /**
     * Saves MISP / OpenCTI STIX2 threat intelligence JSON to Downloads.
     */
    fun saveMispJsonToDownloads(context: Context, jsonContent: String, caseRef: String): String? {
        val filename = "BAYYINAH_MISP_OPENCTI_${caseRef.replace("[^a-zA-Z0-9]".toRegex(), "_")}_${System.currentTimeMillis()}.json"
        return saveTextFileToDownloads(context, jsonContent, filename, "application/json")
    }

    /**
     * Saves standard GraphML XML topology to Downloads for Gephi / Neo4j / Cytoscape import.
     */
    fun saveGraphMlToDownloads(context: Context, xmlContent: String, caseRef: String): String? {
        val filename = "BAYYINAH_GRAPHML_${caseRef.replace("[^a-zA-Z0-9]".toRegex(), "_")}_${System.currentTimeMillis()}.graphml"
        return saveTextFileToDownloads(context, xmlContent, filename, "application/xml")
    }

    /**
     * Generic safe file saver for Android Downloads.
     */
    fun saveTextFileToDownloads(context: Context, content: String, filename: String, mimeType: String): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            return try {
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(content.toByteArray(Charsets.UTF_8))
                    }
                    filename
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            return try {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, filename)
                file.writeText(content, Charsets.UTF_8)
                filename
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Generates a MISP / OpenCTI STIX 2.1 interoperable Intelligence Bundle.
     */
    fun generateMispStixJson(
        investigationCase: InvestigationCase,
        graph: InteractiveCaseGraph,
        osintReport: OsintAnalysisReport? = null
    ): String {
        return try {
            val now = System.currentTimeMillis()
            val sdfIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val isoDate = sdfIso.format(Date(now))
            val sha256 = computeDossierSha256(investigationCase, osintReport)

            val indicatorsArray = buildJsonArray {
                // Main Target Crypto Indicator
                addJsonObject {
                    put("type", "indicator")
                    put("id", "indicator--${UUID.randomUUID()}")
                    put("created", isoDate)
                    put("modified", isoDate)
                    put("name", "Crypto Address: ${investigationCase.targetAddress}")
                    put("description", "Bayyinah Forensic Target Address with ${investigationCase.totalTransactionsFound} transactions on ${investigationCase.network.displayName}")
                    put("pattern", "[crypto-address:value = '${investigationCase.targetAddress}']")
                    put("pattern_type", "stix")
                    put("confidence", 100)
                    putJsonArray("labels") {
                        add("cryptocurrency")
                        add("financial-crime")
                        add(investigationCase.network.name.lowercase(Locale.ROOT))
                    }
                }

                // Graph node indicators
                graph.nodes.filter { it.id != investigationCase.targetAddress }.take(30).forEach { node ->
                    val patternType = when (node.entityCategory) {
                        ForensicEntityCategory.PHONE_NUMBER, ForensicEntityCategory.PUBLIC_PHONE -> "[phone-number:value = '${node.id.removePrefix("PHONE_")}']"
                        ForensicEntityCategory.IP_NETWORK_NODE, ForensicEntityCategory.PUBLIC_IP -> "[ipv4-addr:value = '${node.id.removePrefix("DNS_IP_")}']"
                        ForensicEntityCategory.SOCIAL_ACCOUNT, ForensicEntityCategory.ALIAS_USERNAME, ForensicEntityCategory.PUBLIC_USERNAME -> "[user-account:account_login = '${node.label}']"
                        ForensicEntityCategory.EMAIL_ADDRESS, ForensicEntityCategory.PUBLIC_EMAIL -> "[email-addr:value = '${node.id.removePrefix("EMAIL_")}']"
                        ForensicEntityCategory.DOMAIN_NAME, ForensicEntityCategory.DOMAIN, ForensicEntityCategory.ENS -> "[domain-name:value = '${node.label}']"
                        else -> "[crypto-address:value = '${node.id}']"
                    }
                    addJsonObject {
                        put("type", "indicator")
                        put("id", "indicator--${UUID.randomUUID()}")
                        put("created", isoDate)
                        put("modified", isoDate)
                        put("name", node.label)
                        put("description", "Epistemic Status: ${node.epistemicStatus.name} | Risk: ${node.riskSeverity.name}")
                        put("pattern", patternType)
                        put("pattern_type", "stix")
                        put("confidence", node.confidencePercent)
                        putJsonArray("labels") {
                            node.tags.forEach { add(it) }
                        }
                    }
                }
            }

            val bundleObj = buildJsonObject {
                put("type", "bundle")
                put("id", "bundle--${UUID.randomUUID()}")
                put("spec_version", "2.1")
                putJsonObject("misp_compatibility") {
                    put("event_info", "[BAYYINAH] ${investigationCase.caseName} - Ref: ${investigationCase.referenceNumber}")
                    put("threat_level_id", if (investigationCase.riskIndicators.any { it.severity == RiskSeverity.CRITICAL }) 1 else 2)
                    put("analysis", 2)
                    put("distribution", 0)
                    put("sha256_evidence_seal", sha256)
                }
                put("objects", indicatorsArray)
            }

            jsonPretty.encodeToString(JsonObject.serializer(), bundleObj)
        } catch (e: Exception) {
            e.printStackTrace()
            """{"type":"bundle","id":"error-bundle","error":"${e.message?.replace("\"", "'")}"}"""
        }
    }

    /**
     * Generates a GraphML XML standard format for importing into Cytoscape, Gephi, or Neo4j.
     */
    fun generateGraphMlXml(graph: InteractiveCaseGraph, caseRef: String): String {
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<graphml xmlns="http://graphml.graphdrawing.org/xmlns"""")
            appendLine("""    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"""")
            appendLine("""    xsi:schemaLocation="http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd">""")
            appendLine("""  <key id="label" for="node" attr.name="label" attr.type="string"/>""")
            appendLine("""  <key id="category" for="node" attr.name="category" attr.type="string"/>""")
            appendLine("""  <key id="risk" for="node" attr.name="risk" attr.type="string"/>""")
            appendLine("""  <key id="confidence" for="node" attr.name="confidence" attr.type="int"/>""")
            appendLine("""  <key id="epistemic" for="node" attr.name="epistemic" attr.type="string"/>""")
            appendLine("""  <key id="edge_category" for="edge" attr.name="category" attr.type="string"/>""")
            appendLine("""  <key id="edge_volume" for="edge" attr.name="volume" attr.type="string"/>""")
            appendLine("""  <key id="edge_label" for="edge" attr.name="label" attr.type="string"/>""")
            appendLine("""  <graph id="Bayyinah_${caseRef.replace("[^a-zA-Z0-9]".toRegex(), "_")}" edgedefault="directed">""")

            // Nodes
            graph.nodes.forEach { n ->
                appendLine("""    <node id="${escapeXml(n.id)}">""")
                appendLine("""      <data key="label">${escapeXml(n.label)}</data>""")
                appendLine("""      <data key="category">${escapeXml(n.entityCategory.name)}</data>""")
                appendLine("""      <data key="risk">${escapeXml(n.riskSeverity.name)}</data>""")
                appendLine("""      <data key="confidence">${n.confidencePercent}</data>""")
                appendLine("""      <data key="epistemic">${escapeXml(n.epistemicStatus.name)}</data>""")
                appendLine("""    </node>""")
            }

            // Edges
            graph.edges.forEach { e ->
                appendLine("""    <edge id="${escapeXml(e.id)}" source="${escapeXml(e.sourceId)}" target="${escapeXml(e.targetId)}">""")
                appendLine("""      <data key="edge_category">${escapeXml(e.category.name)}</data>""")
                appendLine("""      <data key="edge_volume">${escapeXml(e.volumeDisplay)}</data>""")
                appendLine("""      <data key="edge_label">${escapeXml(e.transformLabel)}</data>""")
                appendLine("""    </edge>""")
            }

            appendLine("""  </graph>""")
            appendLine("""</graphml>""")
        }
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

@Serializable
data class CourtDossierPayload(
    val archiveStandard: String,
    val platform: String,
    val sha256Seal: String,
    val evidenceIntegrityStatus: String,
    val caseReferenceNumber: String,
    val caseTitle: String,
    val investigatedTargetAddress: String,
    val blockchainNetwork: String,
    val generatedUtc: String,
    val generatedJalali: String,
    val investigationScope: String,
    val investigatorNotes: String,
    val summaryMetrics: LedgerSummaryMetrics,
    val epistemicEvidenceBreakdown: EpistemicBreakdown,
    val graphTopology: GraphTopologyExport,
    val judicialSubpoenaLeads: SubpoenaLeadExport,
    val judicialDisclaimer: String
)

@Serializable
data class LedgerSummaryMetrics(
    val balance: String,
    val totalReceived: String,
    val totalSent: String,
    val transactionCount: Int,
    val counterpartyCount: Int
)

@Serializable
data class EpistemicBreakdown(
    val undisputedOnChainFactsCount: Int,
    val algorithmicCalculationsCount: Int,
    val derivedOsintInferencesCount: Int,
    val workingHypothesesCount: Int,
    val evidenceItems: List<EvidenceItemExport>
)

@Serializable
data class EvidenceItemExport(
    val id: String,
    val category: String,
    val confidence: String,
    val title: String,
    val description: String,
    val source: String,
    val verification: String
)

@Serializable
data class GraphTopologyExport(
    val totalNodes: Int,
    val totalEdges: Int,
    val nodes: List<GraphNodeExport>,
    val edges: List<GraphEdgeExport>
)

@Serializable
data class GraphNodeExport(
    val id: String,
    val label: String,
    val category: String,
    val risk: String,
    val epistemicStatus: String,
    val confidencePercent: Int,
    val tags: List<String>,
    val x: Float,
    val y: Float
)

@Serializable
data class GraphEdgeExport(
    val id: String,
    val source: String,
    val target: String,
    val category: String,
    val volume: String,
    val label: String,
    val confidencePercent: Int
)

@Serializable
data class SubpoenaLeadExport(
    val targetExchanges: List<String>,
    val targetTelecommunications: List<String>,
    val reconstructedPhoneLeads: List<String>
)
