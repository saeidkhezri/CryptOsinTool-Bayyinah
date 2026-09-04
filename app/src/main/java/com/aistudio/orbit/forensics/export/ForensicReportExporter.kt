package com.aistudio.orbit.forensics.export

import com.aistudio.orbit.model.*
import com.aistudio.orbit.repository.AppLanguage
import java.text.SimpleDateFormat
import java.util.*

/**
 * Forensic Report Exporter
 * Generates official investigative dossiers and CSV datasets conforming to strict evidence categorization.
 */
object ForensicReportExporter {

    fun generateTextDossier(
        investigationCase: InvestigationCase,
        language: AppLanguage
    ): String {
        val isPersian = language == AppLanguage.PERSIAN
        val sdfUtc = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val sdfTehran = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'IRST'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Tehran")
        }
        val now = Date()

        val sb = StringBuilder()
        sb.appendLine("================================================================================")
        sb.appendLine(if (isPersian) "گزارش رسمی جرم‌یابی و واکاوی تراکنش‌های بلاکچین" else "OFFICIAL BLOCKCHAIN FORENSIC & FINANCIAL CRIME INVESTIGATION DOSSIER")
        sb.appendLine(if (isPersian) "سامانه تخصصی تحلیل ادله دیجیتال و ردیابی مالی بیِّنة" else "BAYYINAH DIGITAL FORENSICS & FINANCIAL CRIME ANALYSIS PLATFORM")
        sb.appendLine("================================================================================")
        sb.appendLine()
        sb.appendLine("--- ${if (isPersian) "مشخصات پرونده و ارجاع قضایی" else "CASE IDENTIFICATION"} ---")
        sb.appendLine("${if (isPersian) "شماره کلاسه پرونده:" else "Case Reference:"} ${investigationCase.referenceNumber}")
        sb.appendLine("${if (isPersian) "عنوان پرونده:" else "Case Title:"} ${investigationCase.caseName}")
        sb.appendLine("${if (isPersian) "شبکه بلاکچین:" else "Blockchain Network:"} ${investigationCase.network.displayName} (${investigationCase.network.symbol})")
        sb.appendLine("${if (isPersian) "آدرس هدف مورد تحقیق:" else "Target Address:"} ${investigationCase.targetAddress}")
        sb.appendLine("${if (isPersian) "شرح قلمرو و فرضیه اولیه:" else "Investigation Scope:"} ${investigationCase.description.ifBlank { if (isPersian) "نامشخص" else "N/A" }}")
        sb.appendLine("${if (isPersian) "زمان صدور گزارش (UTC):" else "Report Timestamp (UTC):"} ${sdfUtc.format(now)}")
        sb.appendLine("${if (isPersian) "زمان محلی تهران (IRST):" else "Local Time (Tehran):"} ${sdfTehran.format(now)}")
        sb.appendLine("${if (isPersian) "وضعیت تحقیقات:" else "Investigation Status:"} ${investigationCase.status.name}")
        sb.appendLine()

        sb.appendLine("--- ${if (isPersian) "خلاصه داده‌های درون بلاکچین (حقایق قطعی)" else "ON-CHAIN LEDGER OVERVIEW (FACTS)"} ---")
        sb.appendLine("${if (isPersian) "موجودی فعلی آدرس:" else "Current Balance:"} ${String.format("%.8f", investigationCase.balanceBtc)} ${investigationCase.network.symbol}")
        sb.appendLine("${if (isPersian) "مجموع دریافتی تایید شده:" else "Total Confirmed Received:"} ${String.format("%.8f", investigationCase.totalReceivedBtc)} ${investigationCase.network.symbol}")
        sb.appendLine("${if (isPersian) "مجموع ارسالی تایید شده:" else "Total Confirmed Sent:"} ${String.format("%.8f", investigationCase.totalSentBtc)} ${investigationCase.network.symbol}")
        sb.appendLine("${if (isPersian) "تعداد کل تراکنش‌ها:" else "Total On-Chain Transactions:"} ${investigationCase.totalTransactionsFound}")
        sb.appendLine("${if (isPersian) "تعداد طرف‌های مقابل شناسایی‌نشده/شده:" else "Identified Counterparties:"} ${investigationCase.counterparties.size}")
        sb.appendLine()

        sb.appendLine("--- ${if (isPersian) "زنجیره اصل ادله و طبقه‌بندی حقوقی" else "EVIDENCE LOG & FORENSIC CATEGORIZATION"} ---")
        sb.appendLine(if (isPersian) "[تفکیک حقایق قطعی، محاسبات الگوریتمی، انتسابات هویتی و الگوهای رفتاری مشکوک]" else "[Direct Facts, Calculations, Attributions & Suspicious Behavioral Patterns]")
        investigationCase.evidenceLog.forEachIndexed { idx, item ->
            val catLabel = if (isPersian) item.category.displayNameFa else item.category.displayNameEn
            val confLabel = if (isPersian) item.confidence.displayNameFa else item.confidence.displayNameEn
            sb.appendLine("${idx + 1}. [$catLabel] [$confLabel]")
            sb.appendLine("   - ${item.localizedTitle(isPersian)}: ${item.localizedDescription(isPersian)}")
            sb.appendLine("   - ${if (isPersian) "منبع و خط لوله اصل ادله:" else "Provenance Source:"} ${item.provenance.sourceName} (${if (isPersian) item.provenance.sourceType.displayNameFa else item.provenance.sourceType.displayNameEn}) | ${if (isPersian) "وضعیت تایید:" else "Verification:"} ${if (isPersian) item.verificationStatus.displayNameFa else item.verificationStatus.displayNameEn}")
        }
        sb.appendLine()

        if (investigationCase.riskIndicators.isNotEmpty()) {
            sb.appendLine("--- ${if (isPersian) "شاخص‌های ریسک و انطباق با الگوهای پولشویی" else "RISK INDICATORS & AML TYPOLOGIES"} ---")
            investigationCase.riskIndicators.forEach { risk ->
                sb.appendLine("• [${risk.severity.name}] [${if (isPersian) "امتیاز انطباق:" else "Score:"} ${String.format("%.1f", risk.matchingScore)}%] ${risk.title}")
                sb.appendLine("  ${risk.description}")
                if (risk.recommendedAction.isNotBlank()) {
                    sb.appendLine("  -> ${if (isPersian) "سرنخ پیشنهادی:" else "Actionable Lead:"} ${risk.recommendedAction}")
                }
            }
            sb.appendLine()
        }

        sb.appendLine("--- ${if (isPersian) "ماتریس طرف‌های مقابل عمده" else "TOP COUNTERPARTIES MATRIX"} ---")
        investigationCase.counterparties.take(15).forEach { cp ->
            sb.appendLine("• ${cp.address} | ${if (isPersian) "تراکنش‌ها:" else "Txs:"} ${cp.txCount} | ${if (isPersian) "دریافت:" else "Recv:"} ${String.format("%.4f", cp.totalReceivedBtc)} | ${if (isPersian) "ارسال:" else "Sent:"} ${String.format("%.4f", cp.totalSentBtc)} | ${if (isPersian) "خالص:" else "Net:"} ${String.format("%.4f", cp.netVolumeBtc)} BTC")
        }
        sb.appendLine()

        if (investigationCase.notes.isNotBlank()) {
            sb.appendLine("--- ${if (isPersian) "یادداشت‌ها و نتایج کارشناس پرونده" else "INVESTIGATOR NOTES & HYPOTHESES"} ---")
            sb.appendLine(investigationCase.notes)
            sb.appendLine()
        }

        sb.appendLine("================================================================================")
        sb.appendLine(if (isPersian) "سلب مسئولیت قانونی: تطابق با الگوهای رفتاری صرفاً سرنخ تحلیلی است و به منزله اثبات نهایی ارتکاب جرم نیست." else "LEGAL DISCLAIMER: Behavioral pattern matches represent analytical leads and do not automatically constitute conclusive proof of criminal conduct.")
        sb.appendLine("================================================================================")

        return sb.toString()
    }

    fun generateCsvDataset(
        investigationCase: InvestigationCase,
        language: AppLanguage
    ): String {
        val isPersian = language == AppLanguage.PERSIAN
        val sdfUtc = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val sb = StringBuilder()
        // Metadata headers
        sb.appendLine("# Case Reference,${investigationCase.referenceNumber}")
        sb.appendLine("# Target Address,${investigationCase.targetAddress}")
        sb.appendLine("# Network,${investigationCase.network.displayName}")
        sb.appendLine("# Total Transactions,${investigationCase.totalTransactionsFound}")
        sb.appendLine()

        // Transactions Table
        sb.appendLine(if (isPersian) "شناسه تراکنش,زمان (UTC),ارتفاع بلوک,جهت جریان,مبلغ (ساتوشی),مبلغ (${investigationCase.network.symbol}),کارمزد (ساتوشی)" else "TxID,Timestamp_UTC,BlockHeight,Direction,Amount_Sat,Amount_${investigationCase.network.symbol},Fee_Sat")
        for (tx in investigationCase.transactions) {
            val dateStr = sdfUtc.format(Date(tx.timestamp * 1000L))
            val amountBtc = tx.relevantAmountSat.toDouble() / 100_000_000.0
            sb.appendLine("${tx.txId},$dateStr,${tx.blockHeight ?: 0},${tx.direction.name},${tx.relevantAmountSat},$amountBtc,${tx.feeSat}")
        }

        sb.appendLine()
        // Counterparties Table
        sb.appendLine(if (isPersian) "آدرس طرف مقابل,تعداد تعاملات,مجموع دریافتی (BTC),مجموع ارسالی (BTC),خالص جریان (BTC),برچسب هویتی" else "Counterparty_Address,Tx_Count,Total_Received_BTC,Total_Sent_BTC,Net_Flow_BTC,Entity_Label")
        for (cp in investigationCase.counterparties) {
            sb.appendLine("${cp.address},${cp.txCount},${cp.totalReceivedBtc},${cp.totalSentBtc},${cp.netVolumeBtc},${cp.label ?: if (isPersian) "نامشخص" else "UNKNOWN"}")
        }

        return sb.toString()
    }

    fun exportCsvToStorage(
        context: android.content.Context,
        investigationCase: InvestigationCase,
        language: AppLanguage
    ): String? {
        val csvData = generateCsvDataset(investigationCase, language)
        val filename = "bayyinah_case_${investigationCase.referenceNumber.replace("/", "_")}_dataset.csv"
        return try {
            val file = java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), filename)
            file.writeText(csvData, Charsets.UTF_8)
            file.name
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a structured OSINT Investigation Dossier in text format
     * with clear epistemic demarcation between observed facts, network observations,
     * OSINT inferences, and threat intelligence hypotheses.
     */
    fun generateOsintTextReport(
        report: com.aistudio.orbit.forensics.osint.OsintAnalysisReport,
        language: AppLanguage
    ): String {
        val isFa = language == AppLanguage.PERSIAN
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val nowStr = sdf.format(Date(report.timestamp))

        val sb = StringBuilder()
        sb.appendLine("================================================================================")
        sb.appendLine(if (isFa) "گزارش رسمی استعلام هوشمندی منابع باز (OSINT) و ارزیابی ریسک" else "OFFICIAL OSINT INTELLIGENCE & METADATA FORENSIC DOSSIER")
        sb.appendLine(if (isFa) "سامانه تخصصی جرم‌یابی و ادله دیجیتال بیِّنة" else "BAYYINAH CRYPTO FORENSIC & OSINT PLATFORM")
        sb.appendLine("================================================================================")
        sb.appendLine()
        sb.appendLine("--- ${if (isFa) "۱. اطلاعات پرونده و شناسه زنجیره‌ای بلاکچین (Blockchain Facts)" else "1. TARGET LEDGER IDENTIFICATION"} ---")
        sb.appendLine("${if (isFa) "آدرس عمومی هدف:" else "Target Address:"} ${report.address}")
        sb.appendLine("${if (isFa) "شبکه بلاک‌چین:" else "Blockchain Network:"} ${report.network.displayName} (${report.network.symbol})")
        sb.appendLine("${if (isFa) "مجموع شاخص ریسک OSINT:" else "Aggregated OSINT Risk Score:"} ${report.aggregateRiskScore} / 100")
        sb.appendLine("${if (isFa) "زمان استعلام:" else "Timestamp:"} $nowStr")
        sb.appendLine()

        sb.appendLine("--- ${if (isFa) "۲. رهگیری آدرس‌های آی‌پی و نودهای شبکه (Network Observations)" else "2. IP EXPOSURE & PROPAGATION METADATA"} ---")
        if (report.ipExposures.isNotEmpty()) {
            report.ipExposures.forEachIndexed { idx, ip ->
                sb.appendLine("${idx + 1}. IP: ${ip.ipAddress} [${if (isFa) ip.ispNameFa else ip.ispName}]")
                sb.appendLine("   - ${if (isFa) "موقعیت جغرافیایی:" else "Geo:"} ${if (isFa) ip.countryFa else ip.country} / ${if (isFa) ip.cityFa else ip.city}")
                sb.appendLine("   - ${if (isFa) "نوع اتصال:" else "Connection:"} ${if (isFa) ip.connectionTypeFa else ip.connectionType}")
                sb.appendLine("   - ${if (isFa) "شناسه سیستم خودمختار (ASN):" else "ASN:"} ${ip.asn}")
                sb.appendLine("   - ${if (isFa) "وضعیت Tor/VPN:" else "Tor/VPN Flag:"} ${if (ip.torOrVpnDetected) (if (isFa) "بله (تشخیص داده شد)" else "YES") else (if (isFa) "خیر (اتصال مستقیم)" else "NO")}")
            }
        } else {
            sb.appendLine(if (isFa) "هیچ نشت IP مستقیمی برای این آدرس ثبت نشده است." else "No direct IP exposure logged.")
        }
        sb.appendLine()

        sb.appendLine("--- ${if (isFa) "۳. سوابق نشت هویت و شناسه‌های متصل (Identity & Leak Intelligence)" else "3. IDENTITY & BREACH CORRELATION"} ---")
        if (report.leakRecords.isNotEmpty()) {
            report.leakRecords.forEachIndexed { idx, leak ->
                sb.appendLine("${idx + 1}. [${leak.source}] ${if (isFa) leak.titleFa else leak.title}")
                sb.appendLine("   - ${if (isFa) "ایمیل/نام کاربری:" else "Email/Alias:"} ${leak.emailAssociated}")
                sb.appendLine("   - ${if (isFa) "جزئیات داده‌های افشا شده:" else "Breach Details:"} ${if (isFa) leak.dataDetailsFa else leak.dataDetailsEn}")
                sb.appendLine("   - ${if (isFa) "ضریب اطمینان:" else "Confidence:"} ${leak.confidenceScore}%")
            }
        } else {
            sb.appendLine(if (isFa) "هیچ سابقه افشای هویت فعالی یافت نشد." else "No identity breach records found.")
        }
        sb.appendLine()

        sb.appendLine("--- ${if (isFa) "۴. انطباق با پایگاه‌های هشدار و تهدیدات سایبری (Threat Intelligence Feeds)" else "4. THREAT INTELLIGENCE & SANCTIONS MATCHING"} ---")
        if (report.threatIndicators.isNotEmpty()) {
            report.threatIndicators.forEachIndexed { idx, threat ->
                sb.appendLine("${idx + 1}. [${threat.indicatorType}] ${threat.indicatorValue}")
                sb.appendLine("   - ${if (isFa) "دسته‌بندی تهدید:" else "Category:"} ${if (isFa) threat.threatCategoryFa else threat.threatCategory}")
                sb.appendLine("   - ${if (isFa) "امتیاز ریسک:" else "Risk:"} ${threat.riskScore}/100 | ${if (isFa) "گزارش‌دهنده:" else "Reporter:"} ${threat.reporter}")
                sb.appendLine("   - ${if (isFa) "توضیحات:" else "Notes:"} ${if (isFa) threat.notesFa else threat.notesEn}")
                sb.appendLine("   - ${if (isFa) "راستی‌آزمایی:" else "Verifiability:"} ${if (isFa) threat.verifiabilityDetailsFa else threat.verifiabilityDetailsEn}")
            }
        } else {
            sb.appendLine(if (isFa) "هیچ مورد تهدید شناخته‌شده‌ای برای این شاخص ثبت نشده است." else "No threat intelligence matches found.")
        }
        sb.appendLine()

        sb.appendLine("--- ${if (isFa) "۵. پیش‌نویس استعلام قانونی و قضایی (Forensic Subpoena Lead)" else "5. LEGAL SUBPOENA REQUISITION TEMPLATE"} ---")
        val subpoena = report.subpoenaTemplates.firstOrNull()
        if (subpoena != null) {
            sb.appendLine("${if (isFa) "مرجع مخاطب:" else "Target Entity:"} ${subpoena.exchangeName}")
            sb.appendLine("${if (isFa) "موضوع استعلام:" else "Subject:"} ${if (isFa) subpoena.letterSubjectFa else subpoena.letterSubjectEn}")
            sb.appendLine("${if (isFa) "متن نامه رسمی:" else "Subpoena Body:"}\n${if (isFa) subpoena.bodyFa else subpoena.bodyEn}")
            sb.appendLine("${if (isFa) "اقلام اطلاعاتی درخواستی:" else "Required Data Points:"}")
            val reqList = if (isFa) subpoena.requiredInformationFa else subpoena.requiredInformationEn
            reqList.forEach { item ->
                sb.appendLine("  • $item")
            }
        } else {
            sb.appendLine(if (isFa) "پیش‌نویس استعلام ثبت نشده است." else "No subpoena requisition templates generated.")
        }
        sb.appendLine()

        sb.appendLine("================================================================================")
        sb.appendLine(if (isFa) "سلب مسئولیت قانونی: کلیه داده‌های OSINT بر پایه منابع اطلاعاتی عمومی استخراج شده و به عنوان ادله و قرائن تحلیلی تلقی می‌گردد و نیازمند تطبیق تکمیلی با دستور قضایی می‌باشد."
        else "LEGAL DISCLAIMER: All OSINT data is derived from open-source intelligence feeds and represents analytical indicators requiring legal confirmation.")
        sb.appendLine("================================================================================")

        return sb.toString()
    }
}
