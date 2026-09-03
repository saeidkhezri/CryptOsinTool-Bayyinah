package com.aistudio.orbit.ui.components.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.aistudio.orbit.forensics.graph.VisualConfidence
import com.aistudio.orbit.forensics.graph.VisualInvestigationEdge
import com.aistudio.orbit.forensics.graph.VisualInvestigationNode
import com.aistudio.orbit.forensics.graph.VisualNodeType
import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.model.RiskSeverity

enum class MoneyFlowAggregation {
    BY_TRANSACTION,
    BY_CLUSTER,
    BY_ENTITY
}

/**
 * React Flow-inspired Money Flow Pipeline Visualization.
 */
@Composable
fun ForensicMoneyFlowPipeline(
    investigationCase: InvestigationCase,
    nodes: List<VisualInvestigationNode>,
    edges: List<VisualInvestigationEdge>,
    isFa: Boolean,
    onNodeClick: (VisualInvestigationNode) -> Unit,
    onEdgeClick: (VisualInvestigationEdge) -> Unit,
    modifier: Modifier = Modifier
) {
    var aggregationMode by remember { mutableStateOf(MoneyFlowAggregation.BY_TRANSACTION) }
    val nodeMap = remember(nodes) { nodes.associateBy { it.id } }

    val sortedEdges = remember(edges, aggregationMode) {
        when (aggregationMode) {
            MoneyFlowAggregation.BY_TRANSACTION -> edges.sortedByDescending { it.amountSat }
            MoneyFlowAggregation.BY_CLUSTER -> edges.filter {
                nodeMap[it.sourceId]?.nodeType == VisualNodeType.CLUSTER || nodeMap[it.targetId]?.nodeType == VisualNodeType.CLUSTER
            }.ifEmpty { edges }
            MoneyFlowAggregation.BY_ENTITY -> edges.filter {
                nodeMap[it.sourceId]?.isOffChain == true || nodeMap[it.targetId]?.isOffChain == true
            }.ifEmpty { edges }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode Selector Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isFa) "جریان ارزش مالی (Money Flow - React Flow Mode)" else "Money Flow Pipeline (React Flow Mode)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = aggregationMode == MoneyFlowAggregation.BY_TRANSACTION,
                        onClick = { aggregationMode = MoneyFlowAggregation.BY_TRANSACTION },
                        label = { Text(if (isFa) "تراکنش‌ها" else "By TX") }
                    )
                    FilterChip(
                        selected = aggregationMode == MoneyFlowAggregation.BY_CLUSTER,
                        onClick = { aggregationMode = MoneyFlowAggregation.BY_CLUSTER },
                        label = { Text(if (isFa) "خوشه‌ها" else "By Cluster") }
                    )
                    FilterChip(
                        selected = aggregationMode == MoneyFlowAggregation.BY_ENTITY,
                        onClick = { aggregationMode = MoneyFlowAggregation.BY_ENTITY },
                        label = { Text(if (isFa) "هویت‌ها" else "By Entity") }
                    )
                }
            }
        }

        if (sortedEdges.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isFa) "تراکنشی منطبق بر این فیلتر یافت نشد" else "No matching transactions found for flow pipeline",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedEdges) { edge ->
                    val srcNode = nodeMap[edge.sourceId]
                    val dstNode = nodeMap[edge.targetId]

                    MoneyFlowHopCard(
                        edge = edge,
                        srcNode = srcNode,
                        dstNode = dstNode,
                        isFa = isFa,
                        onNodeClick = onNodeClick,
                        onEdgeClick = onEdgeClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MoneyFlowHopCard(
    edge: VisualInvestigationEdge,
    srcNode: VisualInvestigationNode?,
    dstNode: VisualInvestigationNode?,
    isFa: Boolean,
    onNodeClick: (VisualInvestigationNode) -> Unit,
    onEdgeClick: (VisualInvestigationEdge) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable { onEdgeClick(edge) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = edge.amountDisplay.ifBlank { "${String.format("%.4f", edge.amountSat / 100_000_000.0)} BTC" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Confidence badge
                val confColor = when (edge.confidence) {
                    VisualConfidence.CONFIRMED -> Color(0xFF2E7D32)
                    VisualConfidence.HIGH -> Color(0xFF1976D2)
                    VisualConfidence.MEDIUM -> Color(0xFFFFA000)
                    VisualConfidence.LOW -> Color(0xFF757575)
                    VisualConfidence.UNKNOWN -> Color(0xFF9E9E9E)
                    VisualConfidence.CONTESTED -> Color(0xFFD32F2F)
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = confColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isFa) edge.confidence.displayNameFa else edge.confidence.displayNameEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = confColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Hop Visualization: Source Node -> Edge -> Target Node
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Source
                NodeFlowChip(
                    node = srcNode,
                    fallbackId = edge.sourceId,
                    isFa = isFa,
                    onClick = { srcNode?.let { onNodeClick(it) } },
                    modifier = Modifier.weight(1f)
                )

                // Transfer indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = edge.epistemicStyle.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Target
                NodeFlowChip(
                    node = dstNode,
                    fallbackId = edge.targetId,
                    isFa = isFa,
                    onClick = { dstNode?.let { onNodeClick(it) } },
                    modifier = Modifier.weight(1f)
                )
            }

            // Footer Details (TXID, Evidence, Source)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (edge.transactionId.isNotBlank()) {
                    Text(
                        text = "TX: ${edge.transactionId.take(8)}...${edge.transactionId.takeLast(6)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = edge.source,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (edge.evidenceIds.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        edge.evidenceIds.take(2).forEach { evId ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = evId,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeFlowChip(
    node: VisualInvestigationNode?,
    fallbackId: String,
    isFa: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val riskColor = when (node?.riskSeverity) {
        RiskSeverity.CRITICAL -> Color(0xFFD32F2F)
        RiskSeverity.HIGH -> Color(0xFFE65100)
        RiskSeverity.MEDIUM -> Color(0xFFFFA000)
        RiskSeverity.LOW -> Color(0xFF388E3C)
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, riskColor.copy(alpha = 0.6f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(
                text = node?.label ?: "${fallbackId.take(6)}...${fallbackId.takeLast(4)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (isFa) node?.nodeType?.displayNameFa ?: "آدرس" else node?.nodeType?.displayNameEn ?: "Address",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
