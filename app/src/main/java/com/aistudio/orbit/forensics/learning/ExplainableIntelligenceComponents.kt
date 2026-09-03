package com.aistudio.orbit.forensics.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Four Epistemic Levels required by Bayyinah Master Instruction 2.0:
 * 1. FACT (واقعیت مشاهده‌شده) - Direct on-chain ledger or authoritative primary data
 * 2. CALCULATED (محاسبه‌شده) - Deterministic mathematical/algorithmic output
 * 3. INFERENCE (استنباط) - Probabilistic deduction supported by evidence
 * 4. HYPOTHESIS (فرضیه) - Analytical suggestion requiring further investigation
 */
enum class ForensicCertaintyLevel(
    val titleFa: String,
    val titleEn: String,
    val descriptionFa: String,
    val descriptionEn: String,
    val colorHex: Long
) {
    OBSERVED_FACT(
        titleFa = "واقعیت مشاهده‌شده",
        titleEn = "Observed Fact",
        descriptionFa = "داده مستقیم و دستکاری‌ناپذیر ثبت‌شده در بلاکچین یا منبع معتبر اولیه‌.",
        descriptionEn = "Direct, tamper-proof data directly recorded on-chain or authoritative primary source.",
        colorHex = 0xFF2E7D32 // Deep Forest Green
    ),
    CALCULATED_RESULT(
        titleFa = "محاسبه‌شده",
        titleEn = "Calculated Result",
        descriptionFa = "نتیجه حاصل از محاسبات ریاضی قطعی و الگوریتمی روی داده‌های خام.",
        descriptionEn = "Deterministic mathematical or algorithmic calculation output on raw data.",
        colorHex = 0xFF1565C0 // Royal Blue
    ),
    INFERENCE(
        titleFa = "استنباط تحلیلی",
        titleEn = "Analytical Inference",
        descriptionFa = "نتیجه احتمالی تقویت‌شده توسط شواهد چندگانه که نیازمند تایید نهایی کارشناس است.",
        descriptionEn = "Probabilistic conclusion supported by multiple evidence items needing human review.",
        colorHex = 0xFFEF6C00 // Amber / Warm Orange
    ),
    HYPOTHESIS(
        titleFa = "فرضیه تحلیلی",
        titleEn = "Investigative Hypothesis",
        descriptionFa = "پیشنهاد تحلیلی یا الگوی اولیه که هنوز شواهد قطعی برای اثبات آن کامل نیست.",
        descriptionEn = "Initial analytical suggestion or pattern match requiring further verification.",
        colorHex = 0xFFC62828 // Crimson Red
    )
}

@Composable
fun CertaintyBadge(
    level: ForensicCertaintyLevel,
    isPersian: Boolean,
    modifier: Modifier = Modifier
) {
    val badgeColor = Color(level.colorHex)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = badgeColor.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(badgeColor, CircleShape)
            )
            Text(
                text = if (isPersian) level.titleFa else level.titleEn,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = badgeColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class ExplainableConfidenceDetails(
    val scorePercentage: Int,
    val evidenceStrengthFa: String,
    val evidenceStrengthEn: String,
    val independentSourcesCount: Int,
    val sourceDerivedDuplicatesCount: Int,
    val recencyTextFa: String,
    val recencyTextEn: String,
    val corroboratingEvidence: List<String> = emptyList(),
    val contradictingEvidence: List<String> = emptyList()
)

data class ExplanationCardData(
    val titleFa: String,
    val titleEn: String,
    val certaintyLevel: ForensicCertaintyLevel,
    val inputDataFa: String,
    val inputDataEn: String,
    val analysisMethodFa: String,
    val analysisMethodEn: String,
    val resultSummaryFa: String,
    val resultSummaryEn: String,
    val evidenceIds: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    val confidenceDetails: ExplainableConfidenceDetails? = null,
    val limitationsFa: List<String> = emptyList(),
    val limitationsEn: List<String> = emptyList(),
    val riskDriversFa: List<String> = emptyList(),
    val riskDriversEn: List<String> = emptyList(),
    val riskMitigatorsFa: List<String> = emptyList(),
    val riskMitigatorsEn: List<String> = emptyList(),
    val geographicCaveatFa: String? = null,
    val geographicCaveatEn: String? = null
)

@Composable
fun WhyAmISeeingThisButton(
    isPersian: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = modifier
    ) {
        Icon(
            Icons.Default.Psychology,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isPersian) "چرا این نتیجه نمایش داده شده؟" else "Why am I seeing this?",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun WhyAmISeeingThisDialog(
    data: ExplanationCardData,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onEvidenceClick: ((String) -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isPersian) "این نتیجه چگونه به دست آمد؟" else "How was this result derived?",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isPersian) data.titleFa else data.titleEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                CertaintyBadge(level = data.certaintyLevel, isPersian = isPersian)

                HorizontalDivider()

                // Explanation Sections
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Input Data
                    ExplanationDetailRow(
                        label = if (isPersian) "داده ورودی:" else "Input Data:",
                        value = if (isPersian) data.inputDataFa else data.inputDataEn,
                        icon = Icons.Default.Input
                    )

                    // Analysis Method
                    ExplanationDetailRow(
                        label = if (isPersian) "روش تحلیل:" else "Analysis Method:",
                        value = if (isPersian) data.analysisMethodFa else data.analysisMethodEn,
                        icon = Icons.Default.Analytics
                    )

                    // Result Summary
                    ExplanationDetailRow(
                        label = if (isPersian) "نتیجه تحلیلی:" else "Derived Result:",
                        value = if (isPersian) data.resultSummaryFa else data.resultSummaryEn,
                        icon = Icons.Default.CheckCircleOutline
                    )

                    // Evidence Grounding & Sources
                    if (data.evidenceIds.isNotEmpty() || data.sources.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isPersian) "ادله و منابع پشتیبان:" else "Supporting Evidence & Sources:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    data.evidenceIds.forEach { evId ->
                                        Surface(
                                            onClick = { onEvidenceClick?.invoke(evId) },
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = evId,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                if (data.sources.isNotEmpty()) {
                                    Text(
                                        text = "${if (isPersian) "منابع:" else "Sources:"} ${data.sources.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Explainable Confidence Breakdown
                    data.confidenceDetails?.let { conf ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isPersian) "ارزیابی درجه اطمینان:" else "Confidence Breakdown:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${conf.scorePercentage}٪",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${if (isPersian) "کیفیت ادله: " else "Evidence Quality: "}${if (isPersian) conf.evidenceStrengthFa else conf.evidenceStrengthEn}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "${if (isPersian) "منابع مستقل: " else "Independent Sources: "}${conf.independentSourcesCount} | ${if (isPersian) "کپی‌های وابسته: " else "Derived Duplicates: "}${conf.sourceDerivedDuplicatesCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // Risk Drivers and Mitigators (Explainable Risk)
                    if (data.riskDriversFa.isNotEmpty() || data.riskMitigatorsFa.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (data.riskDriversFa.isNotEmpty()) {
                                Text(
                                    text = if (isPersian) "عوامل افزایش‌دهنده ریسک:" else "Risk Drivers:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                data.riskDriversFa.forEach { driver ->
                                    Text("• $driver", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            if (data.riskMitigatorsFa.isNotEmpty()) {
                                Text(
                                    text = if (isPersian) "عوامل کاهش‌دهنده ریسک:" else "Risk Mitigators:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                data.riskMitigatorsFa.forEach { mitigator ->
                                    Text("• $mitigator", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // Geographic Cautionary Note
                    data.geographicCaveatFa?.let { caveat ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPersian) caveat else (data.geographicCaveatEn ?: caveat),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Limitations / Non-Inferences
                    val limits = if (isPersian) data.limitationsFa else data.limitationsEn
                    if (limits.isNotEmpty()) {
                        Column {
                            Text(
                                text = if (isPersian) "محدودیت‌ها و عدم‌قطعیت‌ها:" else "Limitations & Uncertainties:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Bold
                            )
                            limits.forEach { lim ->
                                Text("• $lim", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (isPersian) "متوجه شدم" else "Understood")
                }
            }
        }
    }
}

@Composable
private fun ExplanationDetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun NegativeEvidenceNotice(
    messageFa: String,
    messageEn: String,
    isPersian: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.SearchOff, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            Column {
                Text(
                    text = if (isPersian) "نتیجه بررسی عدم مشاهده (Negative Evidence)" else "Negative Evidence Notice",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = if (isPersian) messageFa else messageEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InsufficientEvidenceNotice(
    missingDataFa: String,
    missingDataEn: String,
    recommendedActionFa: String,
    recommendedActionEn: String,
    isPersian: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    text = if (isPersian) "شواهد کافی نیست (Insufficient Evidence)" else "Insufficient Evidence Warning",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${if (isPersian) "داده‌های مفقود: " else "Missing Data: "}${if (isPersian) missingDataFa else missingDataEn}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "${if (isPersian) "اقدام پیشنهادی جهت رفع نقص: " else "Recommended Action: "}${if (isPersian) recommendedActionFa else recommendedActionEn}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
