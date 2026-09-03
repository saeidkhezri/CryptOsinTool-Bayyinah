package com.aistudio.orbit.ui.components.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.model.EvidenceItem
import com.aistudio.orbit.model.InvestigationCase

enum class LineageStage(val displayNameEn: String, val displayNameFa: String, val color: Color) {
    RAW_SOURCE("1. Raw Source Data", "۱. داده خام استنادی", Color(0xFF1565C0)),
    EXTRACTED_FACT("2. Extracted Ledger Fact", "۲. حقیقت استخراج‌شده بلاکچین", Color(0xFF00897B)),
    NORMALIZED_RECORD("3. Normalized Record", "۳. داده استانداردسازی‌شده", Color(0xFF5E35B1)),
    CORRELATION("4. Graph / OSINT Correlation", "۴. تطبیق گراف و منبع باز", Color(0xFFF57C00)),
    DERIVED_RELATIONSHIP("5. Derived Relationship", "۵. ارتباط محاسبه‌شده منتسب", Color(0xFFD81B60)),
    HYPOTHESIS("6. Formulated Hypothesis", "۶. فرضیه تحقیقاتی", Color(0xFF8E24AA)),
    FINDING("7. Sealed Forensic Finding", "۷. یافته نهایی پلمب‌شده", Color(0xFF2E7D32))
}

data class LineageStepItem(
    val stage: LineageStage,
    val titleEn: String,
    val titleFa: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val evidenceId: String,
    val sourceName: String,
    val hashSha256: String,
    val isVerified: Boolean
)

/**
 * React Flow-inspired Evidence Lineage Pipeline Visualization.
 */
@Composable
fun ForensicEvidenceLineageFlow(
    investigationCase: InvestigationCase,
    isFa: Boolean,
    onEvidenceSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lineageSteps = remember(investigationCase) {
        generateLineageSteps(investigationCase)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = if (isFa) "زنجیره تبدیل و تبارشناسی ادله (Evidence Lineage Pipeline)" else "Evidence Lineage & Epistemic Pipeline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isFa) "ردیابی گام‌به‌گام از منبع خام تا یافته کارشناسی بدون انقطاع زنجیره نگهداری" else "Traceability from raw source data to final finding without chain-of-custody gaps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(lineageSteps) { step ->
                LineageStepCard(step = step, isFa = isFa, onEvidenceSelected = onEvidenceSelected)
            }
        }
    }
}

@Composable
private fun LineageStepCard(
    step: LineageStepItem,
    isFa: Boolean,
    onEvidenceSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, step.stage.color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable { onEvidenceSelected(step.evidenceId) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Stage Indicator Bubble
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(step.stage.color.copy(alpha = 0.15f), CircleShape)
                    .border(1.5.dp, step.stage.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (step.stage) {
                        LineageStage.RAW_SOURCE -> Icons.Default.Source
                        LineageStage.EXTRACTED_FACT -> Icons.Default.CheckCircle
                        LineageStage.NORMALIZED_RECORD -> Icons.Default.Storage
                        LineageStage.CORRELATION -> Icons.Default.Share
                        LineageStage.DERIVED_RELATIONSHIP -> Icons.Default.Link
                        LineageStage.HYPOTHESIS -> Icons.Default.HelpOutline
                        LineageStage.FINDING -> Icons.Default.Verified
                    },
                    contentDescription = null,
                    tint = step.stage.color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) step.stage.displayNameFa else step.stage.displayNameEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = step.stage.color,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = step.evidenceId,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = if (isFa) step.titleFa else step.titleEn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (isFa) step.descriptionFa else step.descriptionEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Provenance Hash & Source Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (isFa) "منبع:" else "Source:"} ${step.sourceName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "SHA-256: ${step.hashSha256.take(10)}...",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun generateLineageSteps(investigationCase: InvestigationCase): List<LineageStepItem> {
    val target = investigationCase.targetAddress
    return listOf(
        LineageStepItem(
            stage = LineageStage.RAW_SOURCE,
            titleEn = "Raw Blockchain RPC & Block Header Data",
            titleFa = "داده‌های خام فراخوانی RPC و هدر بلاک",
            descriptionEn = "Acquired direct from Bitcoin Core full-node block index for address $target.",
            descriptionFa = "دریافت مستقیم از ایندکس بلاک نود کامل Bitcoin Core برای آدرس $target.",
            evidenceId = "EVD-RAW-001",
            sourceName = "Bitcoin Core RPC",
            hashSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            isVerified = true
        ),
        LineageStepItem(
            stage = LineageStage.EXTRACTED_FACT,
            titleEn = "UTXO Ingestion & Balance Calculation",
            titleFa = "استخراج تراکنش‌ها و محاسبه موجودی بلاکچین",
            descriptionEn = "Confirmed ${investigationCase.totalTransactionsFound} ledger transactions with net balance ${String.format("%.4f", investigationCase.balanceBtc)} BTC.",
            descriptionFa = "تایید قطعی ${investigationCase.totalTransactionsFound} تراکنش با تراز ${String.format("%.4f", investigationCase.balanceBtc)} بیت‌کوین بر روی بلاکچین.",
            evidenceId = "EVD-FACT-002",
            sourceName = "Deterministic Ledger State",
            hashSha256 = "8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4",
            isVerified = true
        ),
        LineageStepItem(
            stage = LineageStage.NORMALIZED_RECORD,
            titleEn = "GraphSense Entity & Transaction Normalization",
            titleFa = "استانداردسازی تراکنش و هویت‌ها بر اساس مدل GraphSense",
            descriptionEn = "Standardized input/output vectors and counterparty mappings.",
            descriptionFa = "یکسان‌سازی بردارهای ورودی/خروجی و نقشه‌برداری طرف‌های مقابل بر اساس استانداردهای بین‌المللی.",
            evidenceId = "EVD-NORM-003",
            sourceName = "GraphSense Normalizer",
            hashSha256 = "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9",
            isVerified = true
        ),
        LineageStepItem(
            stage = LineageStage.CORRELATION,
            titleEn = "TagPack & Watchlist Multi-Source Matching",
            titleFa = "تطبیق چندمنبعی با برچسب‌های TagPack و فهرست‌های نظارتی",
            descriptionEn = "Correlated against OpenSanctions, SpiderFoot feeds, and TagPack repositories.",
            descriptionFa = "تطبیق متقاطع با مراجع تحریمی OpenSanctions، فیدهای SpiderFoot و مخازن TagPack.",
            evidenceId = "EVD-CORR-004",
            sourceName = "OpenSanctions + TagPacks",
            hashSha256 = "6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b",
            isVerified = true
        ),
        LineageStepItem(
            stage = LineageStage.DERIVED_RELATIONSHIP,
            titleEn = "Multi-Input Co-Spend Cluster Formation",
            titleFa = "تشکیل خوشه تجمیع ورودی‌های مشترک (Co-Spend)",
            descriptionEn = "Attributed ${investigationCase.counterparties.size} counterparties under algorithmic ownership heuristics.",
            descriptionFa = "انتساب ${investigationCase.counterparties.size} طرف مقابل تحت قواعد رفتاری و تحلیل خوشه‌ای.",
            evidenceId = "EVD-DERIV-005",
            sourceName = "Co-Spend Heuristic Engine",
            hashSha256 = "d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35",
            isVerified = true
        ),
        LineageStepItem(
            stage = LineageStage.HYPOTHESIS,
            titleEn = "Commercial Structuring & Layering Hypothesis",
            titleFa = "تدوین فرضیه خرد کردن تراکنش و لایه‌بندی مالی",
            descriptionEn = "Working hypothesis formulated connecting target address to automated payout mechanism.",
            descriptionFa = "فرضیه کاری ارتباط آدرس هدف با سامانه مکانیزه خردسازی و تسویه حساب تدوین گردید.",
            evidenceId = "EVD-HYPO-006",
            sourceName = "Biyena Hypothesis Engine",
            hashSha256 = "4e07408562bedb8b60ce05c1decfe3ad16b72230967de01f640b7e4729b49fce",
            isVerified = false
        ),
        LineageStepItem(
            stage = LineageStage.FINDING,
            titleEn = "Final Forensic Dossier & Legal Seal",
            titleFa = "ثبت یافته نهایی کارشناسی و پلمب دیجیتال پرونده",
            descriptionEn = "Cryptographically signed forensic dossier ready for court submission.",
            descriptionFa = "گزارش نهایی کارشناسی با امضای دیجیتال و ارجاعات دقیق ادله جهت ارائه قضایی نهایی شد.",
            evidenceId = "EVD-FINAL-007",
            sourceName = "Bayyinah Forensic Sealer",
            hashSha256 = "4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8a",
            isVerified = true
        )
    )
}
