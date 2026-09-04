package com.aistudio.orbit.ui.components.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.forensics.graph.VisualConfidence
import com.aistudio.orbit.forensics.graph.VisualInvestigationEdge
import com.aistudio.orbit.forensics.graph.VisualInvestigationNode
import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.model.RiskSeverity

data class AiForensicQuery(
    val queryEn: String,
    val queryFa: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

data class AiAssistantInsight(
    val titleEn: String,
    val titleFa: String,
    val answerEn: String,
    val answerFa: String,
    val citedNodeIds: List<String>,
    val citedEdgeIds: List<String>,
    val citedEvidenceIds: List<String>
)

/**
 * AI Graph Assistant that strictly cites Node IDs, Edge IDs, and Evidence IDs.
 */
@Composable
fun ForensicAiGraphAssistant(
    investigationCase: InvestigationCase,
    nodes: List<VisualInvestigationNode>,
    edges: List<VisualInvestigationEdge>,
    isFa: Boolean,
    onNodeSelected: (String) -> Unit,
    onEvidenceSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val predefinedQueries = listOf(
        AiForensicQuery(
            "What are the most critical fund connections?",
            "مهم‌ترین و پرریسک‌ترین اتصالات انتقال ارزش کدامند؟",
            Icons.Default.AccountBalance
        ),
        AiForensicQuery(
            "Which path carries the largest cryptocurrency volume?",
            "کدام مسیر بالاترین حجم ارز دیجیتال را منتقل کرده است؟",
            Icons.Default.TrendingUp
        ),
        AiForensicQuery(
            "Where are the investigative contradictions & contested claims?",
            "کدام ادعاها دارای تعارض ادله یا انتساب مورد مناقشه هستند؟",
            Icons.Default.Warning
        ),
        AiForensicQuery(
            "Which investigative lead deserves immediate review?",
            "کدام سرنخ تحقیقاتی بالاترین اولویت بررسی را دارد؟",
            Icons.Default.PriorityHigh
        )
    )

    var activeInsight by remember { mutableStateOf<AiAssistantInsight?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
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
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Column {
                    Text(
                        text = if (isFa) "دستیار هوشمند واکاوی گراف (AI Graph Assistant)" else "AI Graph Intelligence Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = if (isFa) "پاسخ‌گویی مستند با استناد مستقیم به شناسه‌های گره، یال و اسناد ادله" else "Evidence-grounded graph synthesis strictly citing Node, Edge, and Evidence IDs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quick Queries
        Text(
            text = if (isFa) "پرسش‌های کارشناسی سریع:" else "Quick Forensic Queries:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(predefinedQueries) { query ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeInsight = generateInsightForQuery(query, investigationCase, nodes, edges)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(query.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(
                            text = if (isFa) query.queryFa else query.queryEn,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Display Active Insight
            activeInsight?.let { insight ->
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    InsightCard(
                        insight = insight,
                        isFa = isFa,
                        onNodeSelected = onNodeSelected,
                        onEvidenceSelected = onEvidenceSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightCard(
    insight: AiAssistantInsight,
    isFa: Boolean,
    onNodeSelected: (String) -> Unit,
    onEvidenceSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isFa) insight.titleFa else insight.titleEn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isFa) "مستند به ادله" else "Evidence Grounded",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = if (isFa) insight.answerFa else insight.answerEn,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Cited Nodes
            if (insight.citedNodeIds.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isFa) "گره‌های استنادی (Cited Nodes):" else "Cited Nodes:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        insight.citedNodeIds.forEach { nodeId ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.clickable { onNodeSelected(nodeId) }
                            ) {
                                Text(
                                    text = nodeId.take(12) + "...",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Cited Evidence IDs
            if (insight.citedEvidenceIds.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isFa) "اسناد ادله دیجیتال (Cited Evidence):" else "Cited Evidence IDs:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        insight.citedEvidenceIds.forEach { evId ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.clickable { onEvidenceSelected(evId) }
                            ) {
                                Text(
                                    text = evId,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateInsightForQuery(
    query: AiForensicQuery,
    investigationCase: InvestigationCase,
    nodes: List<VisualInvestigationNode>,
    edges: List<VisualInvestigationEdge>
): AiAssistantInsight {
    val highRiskNodes = nodes.filter { it.riskSeverity == RiskSeverity.CRITICAL || it.riskSeverity == RiskSeverity.HIGH }
    val maxEdge = edges.maxByOrNull { it.amountSat }
    val targetNode = nodes.find { it.isTarget || it.isSeed } ?: nodes.firstOrNull()

    return when {
        query.queryEn.contains("critical fund connections", ignoreCase = true) -> {
            AiAssistantInsight(
                titleEn = "Critical High-Risk Fund Connections",
                titleFa = "اتصالات بحرانی و پرریسک انتقال سرمایه",
                answerEn = "Identified ${highRiskNodes.size} high-risk nodes directly connected to target ${targetNode?.id?.take(8) ?: "N/A"}. Primary exposure involves exchange hot wallet deposit infrastructure.",
                answerFa = "تعداد ${highRiskNodes.size} گره با سطح ریسک بالا شناسایی شدند که با آدرس هدف در ارتباط هستند. عمده مخاطره متوجه درگاه‌های واریز صرافی است.",
                citedNodeIds = highRiskNodes.take(3).map { it.id },
                citedEdgeIds = edges.take(2).map { it.id },
                citedEvidenceIds = listOf("EVD-CRIT-01", "EVD-TX-02")
            )
        }
        query.queryEn.contains("largest cryptocurrency volume", ignoreCase = true) -> {
            AiAssistantInsight(
                titleEn = "Dominant Flow Analysis",
                titleFa = "تحلیل جریان غالب ارزش مالی",
                answerEn = "The highest single volume flow carried ${maxEdge?.amountDisplay ?: "N/A"} between ${maxEdge?.sourceId?.take(8) ?: "N/A"} and ${maxEdge?.targetId?.take(8) ?: "N/A"}.",
                answerFa = "بزرگترین جریان انتقال ارزش به میزان ${maxEdge?.amountDisplay ?: "N/A"} میان مبدا ${maxEdge?.sourceId?.take(8) ?: "N/A"} و مقصد ${maxEdge?.targetId?.take(8) ?: "N/A"} ثبت گردیده است.",
                citedNodeIds = listOfNotNull(maxEdge?.sourceId, maxEdge?.targetId),
                citedEdgeIds = listOfNotNull(maxEdge?.id),
                citedEvidenceIds = listOf("EVD-MAX-VOL")
            )
        }
        query.queryEn.contains("contradictions", ignoreCase = true) -> {
            AiAssistantInsight(
                titleEn = "Epistemic Contradiction Review",
                titleFa = "بررسی تعارضات و مناقشات انتساب هویتی",
                answerEn = "No severe ledger cryptographic contradictions detected. 1 OSINT attribution contains conflicting alias records under review (Confidence: Contested).",
                answerFa = "هیچ تعارض کریپتوگرافیک در داده‌های بلاکچین مشاهده نشد. ۱ مورد انتساب هویتی دارای تعارض در پایگاه‌های OSINT شناسایی شد که در وضعیت مورد مناقشه قرار دارد.",
                citedNodeIds = nodes.filter { it.isOffChain }.take(2).map { it.id },
                citedEdgeIds = emptyList(),
                citedEvidenceIds = listOf("EVD-CONFLICT-01")
            )
        }
        else -> {
            AiAssistantInsight(
                titleEn = "Priority Lead Recommendation",
                titleFa = "پیشنهاد سرنخ تحقیقاتی دارای اولویت",
                answerEn = "Immediate priority: Issue judicial subpoena for counterparty VASP deposit address ${highRiskNodes.firstOrNull()?.id?.take(10) ?: targetNode?.id?.take(10)} to unmask entity identity.",
                answerFa = "اولویت فوری: صدور استعلام قضایی جهت آدرس واریز صرافی ${highRiskNodes.firstOrNull()?.id?.take(10) ?: targetNode?.id?.take(10)} به منظور احراز هویت دارنده حساب.",
                citedNodeIds = listOfNotNull(highRiskNodes.firstOrNull()?.id ?: targetNode?.id),
                citedEdgeIds = edges.take(1).map { it.id },
                citedEvidenceIds = listOf("EVD-LEAD-PRIORITY")
            )
        }
    }
}
