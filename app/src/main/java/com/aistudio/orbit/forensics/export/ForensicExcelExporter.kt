package com.aistudio.orbit.forensics.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.aistudio.orbit.PersianDateUtils
import com.aistudio.orbit.model.InteractiveCaseGraph
import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.model.RiskSeverity
import com.aistudio.orbit.model.TxDirection
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * High-Performance OpenXML Excel (.xlsx) Exporter.
 * Generates genuine multi-sheet spreadsheets containing forensic data,
 * transaction ledgers, GraphSense clusters, and OSINT correlations.
 */
object ForensicExcelExporter {

    fun exportCaseToXlsx(
        context: Context,
        investigationCase: InvestigationCase,
        graph: InteractiveCaseGraph,
        isPersian: Boolean
    ): String? {
        return try {
            val timestamp = System.currentTimeMillis()
            val filename = "BAYYINAH_FORENSIC_DOSSIER_${investigationCase.referenceNumber}_$timestamp.xlsx"
            val xlsxBytes = buildXlsxBundle(investigationCase, graph, isPersian)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Bayyinah_Forensics")
                }
                val uri: Uri? = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(xlsxBytes)
                        os.flush()
                    }
                    filename
                } else null
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Bayyinah_Forensics")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, filename)
                FileOutputStream(file).use { os ->
                    os.write(xlsxBytes)
                    os.flush()
                }
                filename
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildXlsxBundle(
        investigationCase: InvestigationCase,
        graph: InteractiveCaseGraph,
        isPersian: Boolean
    ): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            // 1. [Content_Types].xml
            writeZipEntry(zos, "[Content_Types].xml", getContentTypesXml())

            // 2. _rels/.rels
            writeZipEntry(zos, "_rels/.rels", getRootRelsXml())

            // 3. xl/_rels/workbook.xml.rels
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", getWorkbookRelsXml())

            // 4. xl/workbook.xml
            writeZipEntry(zos, "xl/workbook.xml", getWorkbookXml(isPersian))

            // 5. xl/styles.xml
            writeZipEntry(zos, "xl/styles.xml", getStylesXml())

            // 6. Worksheets
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", buildSummarySheet(investigationCase, isPersian))
            writeZipEntry(zos, "xl/worksheets/sheet2.xml", buildTransactionsSheet(investigationCase, isPersian))
            writeZipEntry(zos, "xl/worksheets/sheet3.xml", buildGraphTopologySheet(graph, isPersian))
            writeZipEntry(zos, "xl/worksheets/sheet4.xml", buildOsintSheet(investigationCase, graph, isPersian))
        }
        return bos.toByteArray()
    }

    private fun writeZipEntry(zos: ZipOutputStream, path: String, content: String) {
        zos.putNextEntry(ZipEntry(path))
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun getContentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

    private fun getRootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun getWorkbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
    <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
    <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
    <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun getWorkbookXml(isPersian: Boolean): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheets>
        <sheet name="${if (isPersian) "خلاصه پرونده" else "Case Summary"}" sheetId="1" r:id="rId1"/>
        <sheet name="${if (isPersian) "تراکنش‌ها و مبالغ" else "Transactions Ledger"}" sheetId="2" r:id="rId2"/>
        <sheet name="${if (isPersian) "توپولوژی گراف و خوشه‌ها" else "Graph Topology"}" sheetId="3" r:id="rId3"/>
        <sheet name="${if (isPersian) "هویت‌ها و اوسینت" else "OSINT Identifiers"}" sheetId="4" r:id="rId4"/>
    </sheets>
</workbook>"""

    private fun getStylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <fonts count="3">
        <font><name val="Calibri"/><sz val="11"/></font>
        <font><b/><name val="Calibri"/><sz val="11"/><color rgb="FFFFFFFF"/></font>
        <font><b/><name val="Calibri"/><sz val="12"/><color rgb="FF0F2042"/></font>
    </fonts>
    <fills count="4">
        <fill><patternFill patternType="none"/></fill>
        <fill><patternFill patternType="gray125"/></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FF0F2042"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FFF1F5F9"/></patternFill></fill>
    </fills>
    <borders count="2">
        <border><left/><right/><top/><bottom/></border>
        <border>
            <left style="thin"><color rgb="FFCBD5E1"/></left>
            <right style="thin"><color rgb="FFCBD5E1"/></right>
            <top style="thin"><color rgb="FFCBD5E1"/></top>
            <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
        </border>
    </borders>
    <cellXfs count="4">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="1" applyBorder="1"/>
        <xf numFmtId="0" fontId="1" fillId="2" borderId="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
            <alignment horizontal="center" vertical="center"/>
        </xf>
        <xf numFmtId="0" fontId="2" fillId="3" borderId="1" applyFont="1" applyFill="1" applyBorder="1"/>
        <xf numFmtId="0" fontId="0" fillId="0" borderId="1" applyBorder="1">
            <alignment vertical="center"/>
        </xf>
    </cellXfs>
</styleSheet>"""

    private fun buildSummarySheet(case: InvestigationCase, isPersian: Boolean): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val isoDate = sdf.format(Date())
        val sha256 = ForensicEvidenceSealer.computeDossierSha256(case)

        val maxRisk = (case.riskIndicators.maxOfOrNull { it.matchingScore } ?: 0.0).toInt()
        val rows = listOf(
            listOf(if (isPersian) "عنوان فیلد" else "Field", if (isPersian) "مقدار / داده فنی" else "Value"),
            listOf(if (isPersian) "سامانه گزارش‌گیری" else "Platform", "Bayyinah Forensic Suite v3.2"),
            listOf(if (isPersian) "کلاسه پرونده" else "Case Reference", case.referenceNumber),
            listOf(if (isPersian) "عنوان پرونده" else "Case Title", case.caseName),
            listOf(if (isPersian) "آدرس هدف آن‌چین" else "Target Crypto Address", case.targetAddress),
            listOf(if (isPersian) "شبکه بلاکچین" else "Blockchain Network", "${case.network.displayName} (${case.network.symbol})"),
            listOf(if (isPersian) "موجودی کل آن‌چین" else "Current Ledger Balance", "${case.balanceBtc} ${case.network.symbol}"),
            listOf(if (isPersian) "تعداد کل تراکنش‌ها" else "Total Transactions", case.totalTransactionsFound.toString()),
            listOf(if (isPersian) "سطح ریسک کلی" else "Risk Severity", "$maxRisk/100 (${case.riskIndicators.firstOrNull()?.severity?.name ?: "EVALUATED"})"),
            listOf(if (isPersian) "اثرانگشت دیجیتال پکیج (SHA-256)" else "ISO/IEC 27037 Integrity Hash", sha256),
            listOf(if (isPersian) "تاریخ و ساعت استخراج" else "Export Timestamp (UTC)", isoDate),
            listOf(if (isPersian) "شناسه کارشناس بررسی‌کننده" else "Lead Forensic Investigator", "BAYYINAH-OFFICER-3916")
        )

        return generateSheetXml(rows)
    }

    private fun buildTransactionsSheet(case: InvestigationCase, isPersian: Boolean): String {
        val header = listOf(
            if (isPersian) "شناسه تراکنش (Tx Hash)" else "Tx Hash",
            if (isPersian) "تاریخ و زمان (UTC)" else "Timestamp UTC",
            if (isPersian) "جهت انتقال" else "Direction",
            if (isPersian) "آدرس طرف مقابل" else "Counterparty Address",
            if (isPersian) "مبلغ انتقال" else "Amount",
            if (isPersian) "نماد ارز" else "Asset",
            if (isPersian) "وضعیت" else "Status"
        )

        val rows = mutableListOf<List<String>>()
        rows.add(header)

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        case.transactions.take(150).forEach { tx ->
            val isIncoming = tx.direction == TxDirection.INCOMING
            val counterparty = tx.counterpartyAddresses.firstOrNull() ?: "-"
            rows.add(
                listOf(
                    tx.txId,
                    sdf.format(Date(if (tx.timestamp > 1_000_000_000_000L) tx.timestamp else tx.timestamp * 1000L)),
                    if (isIncoming) (if (isPersian) "ورودی (Inflow)" else "Incoming") else (if (isPersian) "خروجی (Outflow)" else "Outgoing"),
                    counterparty,
                    tx.relevantAmountBtc.toString(),
                    case.network.symbol,
                    if (tx.isConfirmed) "CONFIRMED" else "PENDING"
                )
            )
        }

        return generateSheetXml(rows)
    }

    private fun buildGraphTopologySheet(graph: InteractiveCaseGraph, isPersian: Boolean): String {
        val header = listOf(
            if (isPersian) "شناسه گره" else "Node ID",
            if (isPersian) "برچسب / هویت" else "Label",
            if (isPersian) "دسته‌بندی هویتی" else "Entity Category",
            if (isPersian) "درجه ریسک" else "Risk Severity",
            if (isPersian) "سطح قطعیت معرفتی" else "Epistemic Status",
            if (isPersian) "درصد اطمینان" else "Confidence %",
            if (isPersian) "تگ‌ها و منابع" else "Tags & Source"
        )

        val rows = mutableListOf<List<String>>()
        rows.add(header)

        graph.nodes.forEach { node ->
            rows.add(
                listOf(
                    node.id,
                    node.label,
                    node.entityCategory.name,
                    node.riskSeverity.name,
                    node.epistemicStatus.name,
                    "${node.confidencePercent}%",
                    node.tags.joinToString(", ")
                )
            )
        }

        return generateSheetXml(rows)
    }

    private fun buildOsintSheet(case: InvestigationCase, graph: InteractiveCaseGraph, isPersian: Boolean): String {
        val header = listOf(
            if (isPersian) "شناسه / شناساگر" else "Identifier",
            if (isPersian) "نوع داده OSINT" else "OSINT Type",
            if (isPersian) "ارتباط با پرونده" else "Association",
            if (isPersian) "منبع اطلاعاتی" else "Intelligence Source",
            if (isPersian) "درجه ریسک" else "Threat Level"
        )

        val rows = mutableListOf<List<String>>()
        rows.add(header)

        // Add Target Address
        rows.add(
            listOf(
                case.targetAddress,
                "CRYPTO_ADDRESS",
                "Primary Subject",
                "Blockchain On-Chain RPC Node",
                if (case.riskIndicators.any { it.severity == RiskSeverity.CRITICAL }) "CRITICAL" else "MEDIUM"
            )
        )

        // Add non-crypto nodes (IPs, Phones, Emails, Telegram, Domains)
        graph.nodes.filter { it.entityCategory.name.contains("PHONE") || it.entityCategory.name.contains("EMAIL") || it.entityCategory.name.contains("IP") || it.entityCategory.name.contains("SOCIAL") || it.entityCategory.name.contains("DOMAIN") }
            .forEach { node ->
                rows.add(
                    listOf(
                        node.label,
                        node.entityCategory.name,
                        "Associated Counterparty / Off-Chain Entity",
                        if (node.tags.isNotEmpty()) node.tags.joinToString(", ") else "GraphSense / SpiderFoot OSINT Hub",
                        node.riskSeverity.name
                    )
                )
            }

        return generateSheetXml(rows)
    }

    private fun getColLetter(colIndex: Int): String {
        var temp = colIndex
        val sb = StringBuilder()
        while (temp >= 0) {
            sb.append(('A'.code + (temp % 26)).toChar())
            temp = (temp / 26) - 1
        }
        return sb.reverse().toString()
    }

    private fun generateSheetXml(rows: List<List<String>>): String {
        val maxCols = rows.maxOfOrNull { it.size } ?: 1
        val maxRows = rows.size
        val lastColLetter = getColLetter(maxCols - 1)
        val lastRowIndex = maxRows.coerceAtLeast(1)
        val dimensionRef = "A1:$lastColLetter$lastRowIndex"

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <dimension ref="$dimensionRef"/>
    <sheetData>
""")

        rows.forEachIndexed { rowIndex, row ->
            val r = rowIndex + 1
            sb.append("""        <row r="$r">""")
            row.forEachIndexed { colIndex, cellVal ->
                val colLetter = getColLetter(colIndex)
                val cellRef = "$colLetter$r"
                val styleId = if (rowIndex == 0) "1" else "0"
                val escapedVal = escapeXml(cellVal)
                sb.append("""<c r="$cellRef" s="$styleId" t="str"><v>$escapedVal</v></c>""")
            }
            sb.append("</row>\n")
        }

        sb.append("""    </sheetData>
</worksheet>""")
        return sb.toString()
    }
}
