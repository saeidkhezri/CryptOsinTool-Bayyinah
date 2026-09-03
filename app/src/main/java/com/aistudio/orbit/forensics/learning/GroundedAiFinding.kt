package com.aistudio.orbit.forensics.learning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.orbit.model.ConfidenceLevel

data class GroundedAiFinding(
    val findingId: String,
    val claimFa: String,
    val claimEn: String,
    val evidenceIds: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    val confidenceLevel: ConfidenceLevel = ConfidenceLevel.MEDIUM_CONFIDENCE,
    val certaintyLevel: ForensicCertaintyLevel = ForensicCertaintyLevel.INFERENCE,
    val explanationFa: String,
    val explanationEn: String,
    val isInsufficientEvidence: Boolean = false,
    val missingDataFa: String? = null,
    val missingDataEn: String? = null,
    val recommendedActionFa: String? = null,
    val recommendedActionEn: String? = null
)

@Composable
fun GroundedAiFindingCard(
    finding: GroundedAiFinding,
    isPersian: Boolean,
    onEvidenceClick: ((String) -> Unit)? = null,
    onWhyAmISeeingThis: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (finding.isInsufficientEvidence) {
        InsufficientEvidenceNotice(
            missingDataFa = finding.missingDataFa ?: "شواهد و تراکنش‌های كافی جهت استنباط صریح یافت نشد.",
            missingDataEn = finding.missingDataEn ?: "Insufficient transactions or evidence to formulate a grounded claim.",
            recommendedActionFa = finding.recommendedActionFa ?: "توسعه دامنه بررسی عمیق‌تر، استعلام OSINT یا افزودن داده از صرافی‌ها.",
            recommendedActionEn = finding.recommendedActionEn ?: "Expand search hop depth, run OSINT queries, or query exchange attribution.",
            isPersian = isPersian,
            modifier = modifier
        )
    } else {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = finding.findingId,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    CertaintyBadge(level = finding.certaintyLevel, isPersian = isPersian)
                }

                // Claim Statement
                Text(
                    text = if (isPersian) finding.claimFa else finding.claimEn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Explanation
                Text(
                    text = if (isPersian) finding.explanationFa else finding.explanationEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Evidence Items list
                if (finding.evidenceIds.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (isPersian) "ادله پشتیبان:" else "Grounding Evidence:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        finding.evidenceIds.forEach { evId ->
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
                }

                // Why am I seeing this? Button
                if (onWhyAmISeeingThis != null) {
                    WhyAmISeeingThisButton(
                        isPersian = isPersian,
                        onClick = onWhyAmISeeingThis,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}
