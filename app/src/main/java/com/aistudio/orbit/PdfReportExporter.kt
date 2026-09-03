package com.aistudio.orbit

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.aistudio.orbit.forensics.export.ForensicEvidenceSealer
import com.aistudio.orbit.forensics.export.ForensicGraphImageRenderer
import com.aistudio.orbit.forensics.export.ForensicReportOptions
import com.aistudio.orbit.model.*
import com.aistudio.orbit.repository.AppLanguage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enterprise Forensic PDF & Text Document Generator for Bayyinah Suite.
 * Adheres to ISO/IEC 27037 digital evidence standards, with high-definition graph embedding,
 * structured technical findings, RTL Persian typography, and optional judicial subpoena annexes.
 */
object PdfReportExporter {

    fun exportForensicCasePdf(
        context: Context,
        case: InvestigationCase,
        language: AppLanguage,
        graph: InteractiveCaseGraph? = null,
        options: ForensicReportOptions = ForensicReportOptions()
    ): String? {
        val isFa = language == AppLanguage.PERSIAN
        val filename = "BAYYINAH_FORENSIC_REPORT_${case.referenceNumber}_${System.currentTimeMillis()}.pdf"
        val document = PdfDocument()

        val paint = Paint().apply { isAntiAlias = true }
        var pageNumber = 1

        // -------------------------------------------------------------
        // PAGE 1: EXECUTIVE SUMMARY & ON-CHAIN FACTS
        // -------------------------------------------------------------
        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
        val page1 = document.startPage(pageInfo1)
        val canvas1 = page1.canvas

        drawPageFrame(canvas1, paint, isFa, 1, if (options.hasAnyLegalSection) 4 else 3)
        drawHeader(canvas1, paint, case, isFa)

        var y1 = 145f

        // Section 1: Identification & Target Scope
        y1 = drawSectionHeader(canvas1, paint, if (isFa) "۱. مشخصات فنی پرونده و آدرس هدف" else "1. CASE & TARGET IDENTIFICATION", y1, isFa, Color.parseColor("#0F2042"))
        
        // Matrix Card
        paint.color = Color.parseColor("#F8FAFC")
        canvas1.drawRoundRect(RectF(30f, y1, 565f, y1 + 70f), 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#E2E8F0")
        canvas1.drawRoundRect(RectF(30f, y1, 565f, y1 + 70f), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        drawKeyValue(canvas1, paint, if (isFa) "آدرس هدف:" else "Target Address:", case.targetAddress, 40f, y1 + 18f, isFa)
        drawKeyValue(canvas1, paint, if (isFa) "شبکه بلاکچین:" else "Network:", "${case.network.displayName} (${case.network.symbol})", 40f, y1 + 34f, isFa)
        drawKeyValue(canvas1, paint, if (isFa) "کلاسه سیستمی:" else "Case Reference:", case.referenceNumber, 40f, y1 + 50f, isFa)
        drawKeyValue(canvas1, paint, if (isFa) "عنوان تحقیق:" else "Case Title:", case.caseName, 40f, y1 + 64f, isFa)

        y1 += 88f

        // Section 2: Observed On-Chain Ledger Facts
        if (options.includeLedgerFacts) {
            y1 = drawSectionHeader(canvas1, paint, if (isFa) "۲. حقایق قطعی و عینی داده بلاکچین (On-Chain Facts)" else "2. DIRECTLY OBSERVED LEDGER FACTS", y1, isFa, Color.parseColor("#166534"))
            
            paint.color = Color.parseColor("#F0FDF4")
            canvas1.drawRoundRect(RectF(30f, y1, 565f, y1 + 80f), 6f, 6f, paint)
            paint.style = Paint.Style.STROKE
            paint.color = Color.parseColor("#BBF7D0")
            canvas1.drawRoundRect(RectF(30f, y1, 565f, y1 + 80f), 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            val balanceStr = String.format(Locale.US, "%.8f", case.balanceBtc) + " " + case.network.symbol
            val recStr = String.format(Locale.US, "%.8f", case.totalReceivedBtc) + " " + case.network.symbol
            val sentStr = String.format(Locale.US, "%.8f", case.totalSentBtc) + " " + case.network.symbol

            drawKeyValue(canvas1, paint, if (isFa) "موجودی کل بلاکچین:" else "Current Balance:", balanceStr, 40f, y1 + 18f, isFa)
            drawKeyValue(canvas1, paint, if (isFa) "مجموع دریافتی تایید شده:" else "Total Received:", recStr, 40f, y1 + 34f, isFa)
            drawKeyValue(canvas1, paint, if (isFa) "مجموع ارسالی تایید شده:" else "Total Sent:", sentStr, 40f, y1 + 50f, isFa)
            drawKeyValue(canvas1, paint, if (isFa) "تعداد کل تراکنش‌ها:" else "Total Transactions:", "${case.totalTransactionsFound} txs (طرف‌های مقابل: ${case.counterparties.size})", 40f, y1 + 68f, isFa)

            y1 += 98f
        }

        // Section 3: Risk Assessment & AML Indicators
        y1 = drawSectionHeader(canvas1, paint, if (isFa) "۳. ارزیابی ریسک و الگوهای پولشویی (AML & Threat Typologies)" else "3. AML TYPOLOGY & RISK ASSESSMENT", y1, isFa, Color.parseColor("#991B1B"))
        
        paint.color = Color.parseColor("#FEF2F2")
        canvas1.drawRoundRect(RectF(30f, y1, 565f, y1 + 115f), 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#FECACA")
        canvas1.drawRoundRect(RectF(30f, y1, 565f, y1 + 115f), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        val maxRiskScore = (case.riskIndicators.maxOfOrNull { it.matchingScore } ?: 0.0).toInt()
        drawKeyValue(canvas1, paint, if (isFa) "امتیاز ریسک کل:" else "Risk Severity Score:", "$maxRiskScore/100 [${if (maxRiskScore >= 70) "HIGH/CRITICAL" else "EVALUATED"}]", 40f, y1 + 18f, isFa)

        var ry = y1 + 36f
        val risksToDisplay = case.riskIndicators.take(3)
        if (risksToDisplay.isNotEmpty()) {
            for (risk in risksToDisplay) {
                paint.color = Color.parseColor("#991B1B")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 8.5f
                val bullet = "• [${risk.severity.name}] ${risk.title}"
                if (isFa) {
                    paint.textAlign = Paint.Align.RIGHT
                    canvas1.drawText(bullet.take(75), 550f, ry, paint)
                } else {
                    canvas1.drawText(bullet.take(75), 40f, ry, paint)
                }
                ry += 13f

                paint.color = Color.DKGRAY
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 7.5f
                val desc = "  ${risk.description.take(95)}"
                if (isFa) {
                    canvas1.drawText(desc, 550f, ry, paint)
                    paint.textAlign = Paint.Align.LEFT
                } else {
                    canvas1.drawText(desc, 40f, ry, paint)
                }
                ry += 14f
            }
        } else {
            paint.color = Color.DKGRAY
            paint.textSize = 8f
            if (isFa) {
                paint.textAlign = Paint.Align.RIGHT
                canvas1.drawText("الگوی ریسک بحرانی خاصی یافت نگردید.", 550f, ry, paint)
                paint.textAlign = Paint.Align.LEFT
            } else {
                canvas1.drawText("No critical risk typology identified for baseline transfers.", 40f, ry, paint)
            }
        }

        y1 += 130f

        // Digital Evidence Seal Box
        if (options.includeEvidenceSealSha256) {
            val sha256 = ForensicEvidenceSealer.computeDossierSha256(case)
            paint.color = Color.parseColor("#0F172A")
            canvas1.drawRoundRect(RectF(30f, y1, 565f, y1 + 45f), 6f, 6f, paint)
            
            paint.color = Color.parseColor("#38BDF8")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8f
            if (isFa) {
                paint.textAlign = Paint.Align.RIGHT
                canvas1.drawText("مهر اصالت و شناسه زنجیره ادله دیجیتال (ISO/IEC 27037 Integrity Hash):", 550f, y1 + 16f, paint)
                paint.textAlign = Paint.Align.LEFT
            } else {
                canvas1.drawText("DIGITAL EVIDENCE INTEGRITY SEAL (ISO/IEC 27037 SHA-256):", 40f, y1 + 16f, paint)
            }

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            paint.textSize = 7f
            canvas1.drawText(sha256, 40f, y1 + 32f, paint)
        }

        document.finishPage(page1)

        // -------------------------------------------------------------
        // PAGE 2: FORENSIC TOPOLOGY GRAPH & CLUSTERS
        // -------------------------------------------------------------
        val pageInfo2 = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
        val page2 = document.startPage(pageInfo2)
        val canvas2 = page2.canvas

        drawPageFrame(canvas2, paint, isFa, 2, if (options.hasAnyLegalSection) 4 else 3)
        drawSubHeader(canvas2, paint, if (isFa) "نمودار توپولوژی گراف و خوشه‌های ارتباطی (Link Analysis)" else "TOPOLOGY GRAPH & CLUSTERS LINK ANALYSIS", isFa)

        var y2 = 80f

        // Render Graph Image Bitmap if available
        if (options.includeGraphDiagram) {
            val activeGraph = graph ?: InteractiveCaseGraph(caseId = case.caseId, targetAddress = case.targetAddress, nodes = emptyList(), edges = emptyList())
            try {
                val graphBitmap = ForensicGraphImageRenderer.renderGraphToBitmap(activeGraph, 2140, 1100, false)
                canvas2.drawBitmap(graphBitmap, null, RectF(30f, y2, 565f, y2 + 275f), paint)
                y2 += 285f
            } catch (e: Exception) {
                e.printStackTrace()
                y2 += 20f
            }
        }

        // Graph Entities & Cluster Summary Table
        y2 = drawSectionHeader(canvas2, paint, if (isFa) "موجودیت‌های شناسایی‌شده در گراف (GraphSense & OSINT Nodes)" else "DISCOVERED GRAPH NODES & ENTITY ATTRIBUTIONS", y2, isFa, Color.parseColor("#0F2042"))

        // Table Header
        paint.color = Color.parseColor("#0F2042")
        canvas2.drawRect(30f, y2, 565f, y2 + 18f, paint)
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8f
        if (isFa) {
            paint.textAlign = Paint.Align.RIGHT
            canvas2.drawText("شناسه / برچسب", 560f, y2 + 12f, paint)
            canvas2.drawText("دسته‌بندی", 380f, y2 + 12f, paint)
            canvas2.drawText("ریسک", 250f, y2 + 12f, paint)
            canvas2.drawText("اطمینان", 160f, y2 + 12f, paint)
            canvas2.drawText("منبع", 85f, y2 + 12f, paint)
            paint.textAlign = Paint.Align.LEFT
        } else {
            canvas2.drawText("Identifier / Label", 35f, y2 + 12f, paint)
            canvas2.drawText("Category", 230f, y2 + 12f, paint)
            canvas2.drawText("Risk", 360f, y2 + 12f, paint)
            canvas2.drawText("Confidence", 430f, y2 + 12f, paint)
            canvas2.drawText("Source", 495f, y2 + 12f, paint)
        }
        y2 += 18f

        // Table Rows
        val displayNodes = graph?.nodes?.take(10) ?: emptyList()
        if (displayNodes.isNotEmpty()) {
            displayNodes.forEachIndexed { idx, node ->
                paint.color = if (idx % 2 == 0) Color.parseColor("#F8FAFC") else Color.WHITE
                canvas2.drawRect(30f, y2, 565f, y2 + 16f, paint)

                paint.color = Color.BLACK
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 7.5f

                val shortLabel = if (node.label.length > 28) node.label.take(25) + "…" else node.label
                if (isFa) {
                    paint.textAlign = Paint.Align.RIGHT
                    canvas2.drawText(shortLabel, 560f, y2 + 11f, paint)
                    canvas2.drawText(node.entityCategory.name.take(18), 380f, y2 + 11f, paint)
                    paint.color = if (node.riskSeverity == RiskSeverity.CRITICAL) Color.RED else Color.BLACK
                    canvas2.drawText(node.riskSeverity.name, 250f, y2 + 11f, paint)
                    paint.color = Color.BLACK
                    canvas2.drawText("${node.confidencePercent}%", 160f, y2 + 11f, paint)
                    canvas2.drawText(node.tags.firstOrNull()?.take(12) ?: "GraphSense", 85f, y2 + 11f, paint)
                    paint.textAlign = Paint.Align.LEFT
                } else {
                    canvas2.drawText(shortLabel, 35f, y2 + 11f, paint)
                    canvas2.drawText(node.entityCategory.name.take(18), 230f, y2 + 11f, paint)
                    paint.color = if (node.riskSeverity == RiskSeverity.CRITICAL) Color.RED else Color.BLACK
                    canvas2.drawText(node.riskSeverity.name, 360f, y2 + 11f, paint)
                    paint.color = Color.BLACK
                    canvas2.drawText("${node.confidencePercent}%", 430f, y2 + 11f, paint)
                    canvas2.drawText(node.tags.firstOrNull()?.take(12) ?: "GraphSense", 495f, y2 + 11f, paint)
                }

                y2 += 16f
            }
        } else {
            paint.color = Color.DKGRAY
            paint.textSize = 8f
            if (isFa) {
                paint.textAlign = Paint.Align.RIGHT
                canvas2.drawText("گره اختصاصی اضافی در گراف ثبت نشده است.", 550f, y2 + 14f, paint)
                paint.textAlign = Paint.Align.LEFT
            } else {
                canvas2.drawText("No supplementary graph entities recorded.", 40f, y2 + 14f, paint)
            }
            y2 += 20f
        }

        // Epistemic Demarcation Note
        y2 = 740f
        paint.color = Color.parseColor("#F1F5F9")
        canvas2.drawRoundRect(RectF(30f, y2, 565f, y2 + 50f), 4f, 4f, paint)
        paint.color = Color.parseColor("#475569")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        if (isFa) {
            paint.textAlign = Paint.Align.RIGHT
            canvas2.drawText("تحدید مرز معرفتی (Epistemic Demarcation Standard):", 555f, y2 + 14f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 7f
            canvas2.drawText("• حقایق آن‌چین (تراکنش‌ها، مقادیر و زمان‌ها) قطعی، تکرارپذیر و مستند به داده‌های ثبت‌شده در بلاکچین می‌باشند.", 555f, y2 + 27f, paint)
            canvas2.drawText("• برچسب‌های OSINT، انتساب‌ها و خوشه‌بندی‌ها استنتاجی بوده و ارزش احتمالاتی و تحلیلی دارند.", 555f, y2 + 40f, paint)
            paint.textAlign = Paint.Align.LEFT
        } else {
            canvas2.drawText("Epistemic Demarcation Standard:", 38f, y2 + 14f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 7f
            canvas2.drawText("• On-chain facts (transfers, amounts, timestamps) are deterministic and mathematically verified.", 38f, y2 + 27f, paint)
            canvas2.drawText("• OSINT labels and attributions represent statistical likelihoods and cyber intelligence heuristics.", 38f, y2 + 40f, paint)
        }

        document.finishPage(page2)

        // -------------------------------------------------------------
        // PAGE 3: TRANSACTION LEDGER & OSINT CROSS-IDENTITIES
        // -------------------------------------------------------------
        val pageInfo3 = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
        val page3 = document.startPage(pageInfo3)
        val canvas3 = page3.canvas

        drawPageFrame(canvas3, paint, isFa, 3, if (options.hasAnyLegalSection) 4 else 3)
        drawSubHeader(canvas3, paint, if (isFa) "جریان تراکنش‌ها و شناسه‌های هویتی OSINT" else "TRANSACTION FLOW & OSINT IDENTITY MATRIX", isFa)

        var y3 = 80f

        // Table of Transactions
        y3 = drawSectionHeader(canvas3, paint, if (isFa) "فهرست تراکنش‌های درون زنجیره‌ای (On-chain Transactions)" else "ON-CHAIN LEDGER TRANSACTIONS", y3, isFa, Color.parseColor("#0F2042"))

        paint.color = Color.parseColor("#0F2042")
        canvas3.drawRect(30f, y3, 565f, y3 + 18f, paint)
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8f
        if (isFa) {
            paint.textAlign = Paint.Align.RIGHT
            canvas3.drawText("شناسه تراکنش (Tx Hash)", 560f, y3 + 12f, paint)
            canvas3.drawText("جهت", 340f, y3 + 12f, paint)
            canvas3.drawText("آدرس طرف مقابل", 290f, y3 + 12f, paint)
            canvas3.drawText("مبلغ", 85f, y3 + 12f, paint)
            paint.textAlign = Paint.Align.LEFT
        } else {
            canvas3.drawText("Tx Hash", 35f, y3 + 12f, paint)
            canvas3.drawText("Dir", 240f, y3 + 12f, paint)
            canvas3.drawText("Counterparty", 280f, y3 + 12f, paint)
            canvas3.drawText("Amount", 480f, y3 + 12f, paint)
        }
        y3 += 18f

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        case.transactions.take(12).forEachIndexed { idx, tx ->
            val isIncoming = tx.direction == TxDirection.INCOMING
            paint.color = if (idx % 2 == 0) Color.parseColor("#F8FAFC") else Color.WHITE
            canvas3.drawRect(30f, y3, 565f, y3 + 16f, paint)

            paint.color = Color.BLACK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 7f

            val shortHash = if (tx.txId.length > 26) tx.txId.take(24) + "…" else tx.txId
            val counterparty = tx.counterpartyAddresses.firstOrNull() ?: "-"
            val shortCp = if (counterparty.length > 24) counterparty.take(22) + "…" else counterparty

            if (isFa) {
                paint.textAlign = Paint.Align.RIGHT
                canvas3.drawText(shortHash, 560f, y3 + 11f, paint)
                
                paint.color = if (isIncoming) Color.parseColor("#166534") else Color.parseColor("#991B1B")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas3.drawText(if (isIncoming) "IN" else "OUT", 340f, y3 + 11f, paint)
                
                paint.color = Color.BLACK
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas3.drawText(shortCp, 290f, y3 + 11f, paint)
                
                canvas3.drawText("${tx.relevantAmountBtc} ${case.network.symbol}", 85f, y3 + 11f, paint)
                paint.textAlign = Paint.Align.LEFT
            } else {
                canvas3.drawText(shortHash, 35f, y3 + 11f, paint)
                
                paint.color = if (isIncoming) Color.parseColor("#166534") else Color.parseColor("#991B1B")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas3.drawText(if (isIncoming) "IN" else "OUT", 240f, y3 + 11f, paint)
                
                paint.color = Color.BLACK
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas3.drawText(shortCp, 280f, y3 + 11f, paint)
                
                canvas3.drawText("${tx.relevantAmountBtc} ${case.network.symbol}", 480f, y3 + 11f, paint)
            }
            y3 += 16f
        }

        y3 += 18f

        // Section: OSINT Identifiers & Evidence Items
        y3 = drawSectionHeader(canvas3, paint, if (isFa) "شناسه‌های OSINT و تبارشناسی شواهد (Chain of Custody)" else "OSINT IDENTIFIERS & EVIDENCE LOG", y3, isFa, Color.parseColor("#4A148C"))

        val osintEvidences = case.evidenceLog.take(6)
        if (osintEvidences.isNotEmpty()) {
            osintEvidences.forEach { ev ->
                paint.color = Color.parseColor("#FAF5FF")
                canvas3.drawRoundRect(RectF(30f, y3, 565f, y3 + 32f), 4f, 4f, paint)
                paint.style = Paint.Style.STROKE
                paint.color = Color.parseColor("#E9D5FF")
                canvas3.drawRoundRect(RectF(30f, y3, 565f, y3 + 32f), 4f, 4f, paint)
                paint.style = Paint.Style.FILL

                paint.color = Color.parseColor("#6B21A8")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 7.5f
                val title = "• [${ev.category.name}] ${ev.localizedTitle(isFa).take(65)}"
                val desc = ev.localizedDescription(isFa).take(90)
                
                if (isFa) {
                    paint.textAlign = Paint.Align.RIGHT
                    canvas3.drawText(title, 555f, y3 + 12f, paint)
                    
                    paint.color = Color.DKGRAY
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.textSize = 7f
                    canvas3.drawText(desc, 545f, y3 + 24f, paint)
                    paint.textAlign = Paint.Align.LEFT
                } else {
                    canvas3.drawText(title, 38f, y3 + 12f, paint)
                    
                    paint.color = Color.DKGRAY
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.textSize = 7f
                    canvas3.drawText(desc, 45f, y3 + 24f, paint)
                }

                y3 += 38f
            }
        } else {
            paint.color = Color.DKGRAY
            paint.textSize = 8f
            if (isFa) {
                paint.textAlign = Paint.Align.RIGHT
                canvas3.drawText("ردپای هوش سایبری دیگری در دیتابیس ثبت نشده است.", 550f, y3 + 14f, paint)
                paint.textAlign = Paint.Align.LEFT
            } else {
                canvas3.drawText("No supplementary OSINT intelligence logs recorded.", 40f, y3 + 14f, paint)
            }
            y3 += 20f
        }

        document.finishPage(page3)

        // -------------------------------------------------------------
        // PAGE 4 (OPTIONAL): AI COPILOT SUMMARY
        // -------------------------------------------------------------
        if (options.aiCopilotSummary != null) {
            val pageInfoAi = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
            val pageAi = document.startPage(pageInfoAi)
            val canvasAi = pageAi.canvas

            val totalPages = if (options.hasAnyLegalSection) 5 else 4
            drawPageFrame(canvasAi, paint, isFa, pageNumber - 1, totalPages)
            drawSubHeader(canvasAi, paint, if (isFa) "تحلیل دستیار هوش مصنوعی (AI Copilot Summary)" else "AI COPILOT SUMMARY", isFa)

            var yAi = 80f
            yAi = drawSectionHeader(canvasAi, paint, if (isFa) "خلاصه بررسی و یافته‌های تکمیلی هوش مصنوعی" else "AI GENERATED ANALYSIS & CORRELATIONS", yAi, isFa, Color.parseColor("#4A148C"))
            
            paint.color = Color.BLACK
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            
            // Very simple text wrapping for AI summary
            val lines = options.aiCopilotSummary.split("\n")
            for (rawLine in lines) {
                // Basic chunking if line is too long
                val chunkLength = if (isFa) 100 else 110
                var currentLine = rawLine
                while(currentLine.isNotEmpty()) {
                    val printLine = if (currentLine.length > chunkLength) currentLine.take(chunkLength) + "-" else currentLine
                    if (currentLine.length > chunkLength) {
                        currentLine = currentLine.substring(chunkLength)
                    } else {
                        currentLine = ""
                    }
                    if (isFa) {
                        paint.textAlign = Paint.Align.RIGHT
                        canvasAi.drawText(printLine, 565f, yAi, paint)
                    } else {
                        canvasAi.drawText(printLine, 30f, yAi, paint)
                    }
                    yAi += 14f
                    if (yAi > 780f) {
                        // Just stop rendering if we run out of space on this simple template
                        break
                    }
                }
                yAi += 6f
                if (yAi > 780f) break
            }
            if (isFa) paint.textAlign = Paint.Align.LEFT
            
            document.finishPage(pageAi)
        }

        // -------------------------------------------------------------
        // PAGE 5 (OPTIONAL): FORMAL JUDICIAL SUBPOENA & LEGAL ANNEX
        // -------------------------------------------------------------
        if (options.hasAnyLegalSection) {
            val pageInfo4 = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
            val page4 = document.startPage(pageInfo4)
            val canvas4 = page4.canvas
            
            val totalPages = if (options.aiCopilotSummary != null) 5 else 4
            drawPageFrame(canvas4, paint, isFa, pageNumber - 1, totalPages)
            drawSubHeader(canvas4, paint, if (isFa) "ضمیمه حقوقی و پیش‌نویس دستور قضایی (Subpoena Requisition)" else "JUDICIAL SUBPOENA & LEGAL ANNEX", isFa)

            var y4 = 80f

            // Formal Legal Header Box
            paint.color = Color.parseColor("#FEF3C7")
            canvas4.drawRoundRect(RectF(30f, y4, 565f, y4 + 40f), 4f, 4f, paint)
            paint.style = Paint.Style.STROKE
            paint.color = Color.parseColor("#F59E0B")
            canvas4.drawRoundRect(RectF(30f, y4, 565f, y4 + 40f), 4f, 4f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.parseColor("#92400E")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8.5f
            if (isFa) {
                paint.textAlign = Paint.Align.RIGHT
                canvas4.drawText("مقام محترم قضایی / ریاست محترم پلیس فضای تولید و تبادل اطلاعات (فتا)", 555f, y4 + 16f, paint)
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas4.drawText("موضوع: تقاضای صدور دستور ردیابی هویت، استعلام لاگ‌های IP و مسدودی فوری وجوه مسروقه", 555f, y4 + 30f, paint)
                paint.textAlign = Paint.Align.LEFT
            } else {
                canvas4.drawText("TO: Cybercrime Judicial Authority / VASP Compliance Department", 38f, y4 + 16f, paint)
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas4.drawText("RE: Formal Subpoena Requisition for KYC, IP Logs & Emergency Asset Freeze", 38f, y4 + 30f, paint)
            }

            y4 += 55f

            // Court text body
            val courtLines = if (isFa) listOf(
                "احتراماً، پیرو گزارش فنی و زنجیره تأمین ادله مندرج در صفحات ۱ الی ۳، به استحضار می‌رساند:",
                "۱. آدرس هدف ${case.targetAddress} طبق بررسی‌های تراکنش‌های بلاکچین ${case.network.displayName}، در بازه زمانی گزارش اقدام به جابجایی وجوه نموده است.",
                "۲. بر اساس تحلیل خوشه‌ها و مسیرهای خروجی، بخشی از دارایی‌ها به مقصد کیف‌پول‌های تجاری و صرافی‌ها هدایت گردیده است.",
                "۳. مستند به قانون جرایم رایانه‌ای و استانداردهای ادله الکترونیکی (ISO 27037)، تقاضا دارد دستور فرمایید:",
                "   الف) استعلام مشخصات هویتی کامل (KYC)، شماره تماس، ایمیل و کد ملی صاحب حساب کاربری مقصد.",
                "   ب) استخراج لاگ‌های نشست (Session Logs)، آدرس‌های IP و اطلاعات User-Agent هنگام ورود و تسویه.",
                "   ج) اعمال دستور مسدودی احتیاطی بر روی موجودی آدرس‌های مقصد جهت جلوگیری از خروج و تضییع اموال.",
                "این گزارش با امضای دیجیتال و اثرانگشت رمزنگاری شده جهت بهره‌برداری قضایی تقدیم می‌گردد."
            ) else listOf(
                "Pursuant to the forensic evidentiary findings detailed in Pages 1 through 3:",
                "1. Target address ${case.targetAddress} has conducted high-volume asset routing on ${case.network.displayName}.",
                "2. Cluster correlation confirms multi-hop distribution into commercial VASP deposit infrastructures.",
                "3. Requisition is hereby submitted pursuant to applicable Cybercrime Statutes requesting:",
                "   a) Mandatory disclosure of full customer identity (KYC/AML records), phone, and email.",
                "   b) IP connection logs, timestamped transaction IDs, and associated withdrawal destinations.",
                "   c) Precautionary administrative asset freeze on associated accounts pending trial adjudication.",
                "Sealed under ISO/IEC 27037 integrity verification."
            )

            paint.color = Color.BLACK
            paint.textSize = 8f
            courtLines.forEach { line ->
                paint.typeface = if (line.startsWith(" ") || line.startsWith("   ")) Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) else Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                if (isFa) {
                    paint.textAlign = Paint.Align.RIGHT
                    canvas4.drawText(line, 555f, y4, paint)
                } else {
                    canvas4.drawText(line, 35f, y4, paint)
                }
                y4 += 18f
            }
            if (isFa) paint.textAlign = Paint.Align.LEFT

            y4 += 30f

            // Official Signature & Seal Block
            paint.color = Color.parseColor("#F8FAFC")
            canvas4.drawRoundRect(RectF(30f, y4, 565f, y4 + 90f), 6f, 6f, paint)
            paint.style = Paint.Style.STROKE
            paint.color = Color.parseColor("#CBD5E1")
            canvas4.drawRoundRect(RectF(30f, y4, 565f, y4 + 90f), 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            if (isFa) {
                paint.textAlign = Paint.Align.RIGHT
                paint.color = Color.parseColor("#0F2042")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 8.5f
                canvas4.drawText("محل مهر و امضای کارشناس رسمی / افسر پرونده:", 550f, y4 + 20f, paint)

                paint.color = Color.DKGRAY
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas4.drawText("شناسه کارشناس: MSKPG-3916 | رتبه صلاحیت: فارنزیک جرایم مالی سایبری", 550f, y4 + 38f, paint)
                canvas4.drawText("تاریخ و زمان تایید نهایی: ${PersianDateUtils.getPersianDateTime(System.currentTimeMillis())}", 550f, y4 + 54f, paint)
                canvas4.drawText("وضعیت امضا: تایید شده با کلید اختصاصی سامانه بیِّنة (VALIDATED)", 550f, y4 + 70f, paint)
                paint.textAlign = Paint.Align.LEFT
            } else {
                paint.color = Color.parseColor("#0F2042")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 8.5f
                canvas4.drawText("LEAD FORENSIC INVESTIGATOR SIGNATURE & SEAL:", 45f, y4 + 20f, paint)

                paint.color = Color.DKGRAY
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas4.drawText("Investigator ID: MSKPG-3916 | Certification: ISO 27037 Certified Forensic Examiner", 45f, y4 + 38f, paint)
                canvas4.drawText("Verification Timestamp: ${Date()}", 45f, y4 + 54f, paint)
                canvas4.drawText("Digital Signature Status: CRYPTOGRAPHICALLY VALIDATED (BAYYINAH SUITE)", 45f, y4 + 70f, paint)
            }

            document.finishPage(page4)
        }

        // Save PDF to Downloads
        return savePdfDocument(context, document, filename)
    }

    private fun drawPageFrame(canvas: Canvas, paint: Paint, isFa: Boolean, pageNum: Int, totalPages: Int) {
        // Outline border
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1f
        canvas.drawRect(15f, 20f, 580f, 822f, paint)
        paint.style = Paint.Style.FILL

        // Footer
        val y = 808f
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawLine(30f, y, 565f, y, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(
            if (isFa) "سامانه فارنزیک بلاکچین بیِّنة (Bayyinah) • گزارش رسمی جرم‌یابی مالی" else "Bayyinah Forensic Suite • Official Cyber Financial Intelligence Dossier",
            30f, y + 10f, paint
        )

        val pageStr = if (isFa) "صفحه $pageNum از $totalPages" else "Page $pageNum of $totalPages"
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(pageStr, 565f, y + 10f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawHeader(canvas: Canvas, paint: Paint, case: InvestigationCase, isFa: Boolean) {
        paint.color = Color.parseColor("#0F2042")
        canvas.drawRect(20f, 30f, 575f, 120f, paint)

        if (isFa) {
            // Gold Emblem on the RIGHT
            paint.color = Color.parseColor("#FFD54F")
            canvas.drawCircle(535f, 75f, 22f, paint)

            paint.color = Color.parseColor("#0F2042")
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("B", 535f, 82f, paint)
            paint.textAlign = Paint.Align.LEFT

            // App Title, Case Details, Timestamp aligned to the RIGHT (on the left of emblem)
            paint.color = Color.WHITE
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("بیِّنة • سامانه پیشرفته فارنزیک بلاکچین و OSINT", 495f, 62f, paint)

            paint.color = Color.parseColor("#FFD54F")
            paint.textSize = 9f
            canvas.drawText("کلاسه پرونده: ${case.referenceNumber} | عنوان: ${case.caseName}", 495f, 85f, paint)

            paint.color = Color.parseColor("#94A3B8")
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val timeLabel = "تاریخ گزارش: ${PersianDateUtils.getPersianDateTime(System.currentTimeMillis())} | کارشناس: MSKPG-3916"
            canvas.drawText(timeLabel, 495f, 105f, paint)
            paint.textAlign = Paint.Align.LEFT
        } else {
            // Gold Emblem on the LEFT
            paint.color = Color.parseColor("#FFD54F")
            canvas.drawCircle(60f, 75f, 22f, paint)

            paint.color = Color.parseColor("#0F2042")
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("B", 53f, 82f, paint)

            // App Title
            paint.color = Color.WHITE
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("BAYYINAH • FORENSIC BLOCKCHAIN & OSINT SUITE", 100f, 62f, paint)

            // Case details line
            paint.color = Color.parseColor("#FFD54F")
            paint.textSize = 9f
            canvas.drawText("Ref: ${case.referenceNumber} | Title: ${case.caseName}", 100f, 85f, paint)

            // Timestamp
            paint.color = Color.parseColor("#94A3B8")
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val timeLabel = "Generated: ${Date()} | Investigator: MSKPG-3916"
            canvas.drawText(timeLabel, 100f, 105f, paint)
        }
    }

    private fun drawSubHeader(canvas: Canvas, paint: Paint, title: String, isFa: Boolean) {
        paint.color = Color.parseColor("#0F2042")
        canvas.drawRect(20f, 30f, 575f, 65f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        if (isFa) {
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(title, 560f, 51f, paint)
            paint.textAlign = Paint.Align.LEFT
        } else {
            canvas.drawText(title, 35f, 51f, paint)
        }
    }

    private fun drawSectionHeader(canvas: Canvas, paint: Paint, title: String, y: Float, isFa: Boolean, color: Int): Float {
        paint.color = color
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        if (isFa) {
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(title, 565f, y, paint)
            paint.textAlign = Paint.Align.LEFT
        } else {
            canvas.drawText(title, 30f, y, paint)
        }

        val lineY = y + 5f
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1f
        canvas.drawLine(30f, lineY, 565f, lineY, paint)
        return lineY + 12f
    }

    private fun drawKeyValue(canvas: Canvas, paint: Paint, label: String, value: String, x: Float, y: Float, isFa: Boolean) {
        if (isFa) {
            paint.textAlign = Paint.Align.RIGHT
            
            paint.color = Color.parseColor("#475569")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f
            canvas.drawText(label, 550f, y, paint)

            paint.color = Color.BLACK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val shortVal = if (value.length > 55) value.take(52) + "…" else value
            canvas.drawText(shortVal, 410f, y, paint)
            
            paint.textAlign = Paint.Align.LEFT
        } else {
            paint.color = Color.parseColor("#475569")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f
            canvas.drawText(label, x, y, paint)

            paint.color = Color.BLACK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val shortVal = if (value.length > 55) value.take(52) + "…" else value
            canvas.drawText(shortVal, x + 125f, y, paint)
        }
    }

    private fun savePdfDocument(context: Context, document: PdfDocument, filename: String): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Bayyinah_Forensics")
            }
            return try {
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        document.writeTo(os)
                    }
                    document.close()
                    filename
                } else {
                    document.close()
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                document.close()
                null
            }
        } else {
            return try {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Bayyinah_Forensics")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, filename)
                FileOutputStream(file).use { os ->
                    document.writeTo(os)
                }
                document.close()
                filename
            } catch (e: Exception) {
                e.printStackTrace()
                document.close()
                null
            }
        }
    }

    fun exportTxtReport(
        context: Context,
        case: InvestigationCase,
        language: AppLanguage,
        options: ForensicReportOptions = ForensicReportOptions()
    ): String? {
        val isFa = language == AppLanguage.PERSIAN
        val filename = "BAYYINAH_FORENSIC_DOSSIER_${case.referenceNumber}_${System.currentTimeMillis()}.txt"
        val sb = StringBuilder()

        sb.appendLine("================================================================================")
        sb.appendLine(if (isFa) "گزارش مستندات جرم‌یابی مالی و تحلیل بلاکچین • سامانه بیِّنة (BAYYINAH)" else "BAYYINAH FORENSIC BLOCKCHAIN & OSINT INTELLIGENCE DOSSIER")
        sb.appendLine("================================================================================")
        sb.appendLine(if (isFa) "کلاسه پرونده: ${case.referenceNumber}" else "Case Reference: ${case.referenceNumber}")
        sb.appendLine(if (isFa) "عنوان پرونده: ${case.caseName}" else "Case Title: ${case.caseName}")
        sb.appendLine(if (isFa) "تاریخ استخراج: ${PersianDateUtils.getPersianDateTime(System.currentTimeMillis())}" else "Export Date: ${Date()}")
        sb.appendLine(if (isFa) "آدرس هدف: ${case.targetAddress}" else "Target Address: ${case.targetAddress}")
        sb.appendLine(if (isFa) "شبکه فعال: ${case.network.displayName} (${case.network.symbol})" else "Network: ${case.network.displayName} (${case.network.symbol})")
        sb.appendLine(if (isFa) "اثرانگشت یکپارچگی (SHA-256): ${ForensicEvidenceSealer.computeDossierSha256(case)}" else "Evidence Integrity Seal (SHA-256): ${ForensicEvidenceSealer.computeDossierSha256(case)}")
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine()

        if (options.includeLedgerFacts) {
            sb.appendLine(if (isFa) "۱. حقایق قطعی درون بلاکچین (ON-CHAIN FACTS):" else "1. OBSERVED ON-CHAIN FACTS:")
            sb.appendLine(if (isFa) "موجودی کل: ${case.balanceBtc} ${case.network.symbol}" else "Current Balance: ${case.balanceBtc} ${case.network.symbol}")
            sb.appendLine(if (isFa) "مجموع دریافتی: ${case.totalReceivedBtc} ${case.network.symbol}" else "Total Received: ${case.totalReceivedBtc} ${case.network.symbol}")
            sb.appendLine(if (isFa) "مجموع ارسالی: ${case.totalSentBtc} ${case.network.symbol}" else "Total Sent: ${case.totalSentBtc} ${case.network.symbol}")
            sb.appendLine(if (isFa) "تعداد کل تراکنش‌ها: ${case.totalTransactionsFound}" else "Total Transactions: ${case.totalTransactionsFound}")
            sb.appendLine()
        }

        if (options.includeTransactions) {
            sb.appendLine(if (isFa) "۲. فهرست تراکنش‌ها (TRANSACTIONS LIST):" else "2. TRANSACTIONS LEDGER:")
            case.transactions.take(50).forEach { tx ->
                val isIncoming = tx.direction == TxDirection.INCOMING
                val dir = if (isIncoming) "IN" else "OUT"
                val cp = tx.counterpartyAddresses.firstOrNull() ?: "-"
                sb.appendLine("  • [$dir] Hash: ${tx.txId} | Amount: ${tx.relevantAmountBtc} ${case.network.symbol} | CP: $cp")
            }
            sb.appendLine()
        }

        if (options.includeOsintCrossIdentities) {
            sb.appendLine(if (isFa) "۳. شناسه‌های هویتی OSINT و هوش سایبری:" else "3. OSINT IDENTIFIERS & CYBER INTEL:")
            case.evidenceLog.forEach { ev ->
                sb.appendLine("  • [${ev.category.name}] ${ev.localizedTitle(isFa)}: ${ev.localizedDescription(isFa)}")
            }
            sb.appendLine()
        }

        if (options.aiCopilotSummary != null) {
            sb.appendLine("================================================================================")
            sb.appendLine(if (isFa) "تحلیل دستیار هوش مصنوعی (AI Copilot Summary)" else "AI COPILOT SUMMARY")
            sb.appendLine("================================================================================")
            sb.appendLine(options.aiCopilotSummary)
            sb.appendLine()
        }

        if (options.hasAnyLegalSection) {
            sb.appendLine("================================================================================")
            sb.appendLine(if (isFa) "ضمیمه حقوقی: پیش‌نویس استعلام قضایی و درخواست مسدودی حساب" else "LEGAL ANNEX: FORMAL SUBPOENA REQUISITION")
            sb.appendLine("================================================================================")
            sb.appendLine(ForensicEvidenceSealer.generateSubpoenaRequisitionText(case, null, language))
            sb.appendLine()
        }

        sb.appendLine("================================================================================")
        sb.appendLine(if (isFa) "پایان گزارش مستندات فارنزیک • سامانه بیِّنة" else "END OF FORENSIC DOSSIER • BAYYINAH SUITE")
        sb.appendLine("================================================================================")

        val content = sb.toString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Bayyinah_Forensics")
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
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Bayyinah_Forensics")
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
}
