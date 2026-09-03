package com.aistudio.orbit.ui.components.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.aistudio.orbit.forensics.graph.VisualInvestigationEdge
import com.aistudio.orbit.forensics.graph.VisualInvestigationNode
import com.aistudio.orbit.model.RiskSeverity

/**
 * Forensic Graph & Entity Comparison Component.
 */
@Composable
fun ForensicComparisonComponent(
    nodes: List<VisualInvestigationNode>,
    edges: List<VisualInvestigationEdge>,
    isFa: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedNodeAId by remember { mutableStateOf(nodes.firstOrNull()?.id ?: "") }
    var selectedNodeBId by remember { mutableStateOf(nodes.getOrNull(1)?.id ?: nodes.firstOrNull()?.id ?: "") }

    val nodeA = remember(selectedNodeAId, nodes) { nodes.find { it.id == selectedNodeAId } }
    val nodeB = remember(selectedNodeBId, nodes) { nodes.find { it.id == selectedNodeBId } }

    val commonNeighbors = remember(selectedNodeAId, selectedNodeBId, edges) {
        val neighborsA = edges.filter { it.sourceId == selectedNodeAId || it.targetId == selectedNodeAId }
            .flatMap { listOf(it.sourceId, it.targetId) }.filter { it != selectedNodeAId }.toSet()
        val neighborsB = edges.filter { it.sourceId == selectedNodeBId || it.targetId == selectedNodeBId }
            .flatMap { listOf(it.sourceId, it.targetId) }.filter { it != selectedNodeBId }.toSet()
        neighborsA.intersect(neighborsB)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
                    imageVector = Icons.Default.CompareArrows,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = if (isFa) "واکاوی و مقایسه تطبیقی گره‌ها (Forensic Comparison)" else "Forensic Node Comparison Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isFa) "مقایسه همسایگان مشترک، حجم تراکنش و ابعاد ریسک دو گره" else "Compare shared neighbors, transaction volumes, and risk profiles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (nodeA == null || nodeB == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (isFa) "گره کافی جهت مقایسه موجود نیست" else "Not enough nodes to perform comparison")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Node Selection Pickers
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ComparisonNodePicker(
                            title = if (isFa) "گره الف (Node A)" else "Node A",
                            selectedNode = nodeA,
                            allNodes = nodes,
                            onSelect = { selectedNodeAId = it.id },
                            modifier = Modifier.weight(1f)
                        )
                        ComparisonNodePicker(
                            title = if (isFa) "گره ب (Node B)" else "Node B",
                            selectedNode = nodeB,
                            allNodes = nodes,
                            onSelect = { selectedNodeBId = it.id },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Comparison Metrics Table
                item {
                    ComparisonMetricsCard(nodeA = nodeA, nodeB = nodeB, isFa = isFa)
                }

                // Shared Counterparties & Common Neighbors
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.GroupWork, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "${if (isFa) "همسایگان و طرف‌های مقابل مشترک:" else "Shared Common Neighbors:"} ${commonNeighbors.size}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (commonNeighbors.isEmpty()) {
                                Text(
                                    text = if (isFa) "هیچ طرف مقابل مشترک مستقیمی میان این دو گره یافت نشد." else "No direct common counterparties found between these nodes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                commonNeighbors.forEach { neighborId ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = neighborId,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonNodePicker(
    title: String,
    selectedNode: VisualInvestigationNode,
    allNodes: List<VisualInvestigationNode>,
    onSelect: (VisualInvestigationNode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(text = selectedNode.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = selectedNode.primaryIdentifier.take(12) + "...", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun ComparisonMetricsCard(
    nodeA: VisualInvestigationNode,
    nodeB: VisualInvestigationNode,
    isFa: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = if (isFa) "جدول مقایسه شاخص‌های آماری و ریسک" else "Statistical & Risk Comparison Table",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            MetricRow(label = if (isFa) "نوع گره" else "Node Type", valA = nodeA.nodeType.name, valB = nodeB.nodeType.name)
            MetricRow(label = if (isFa) "موجودی / تراز" else "Balance", valA = nodeA.balanceDisplay.ifBlank { "N/A" }, valB = nodeB.balanceDisplay.ifBlank { "N/A" })
            MetricRow(label = if (isFa) "تعداد تراکنش‌ها" else "Tx Count", valA = "${nodeA.txCount}", valB = "${nodeB.txCount}")
            MetricRow(label = if (isFa) "درجه اتصال گراف" else "Graph Degree", valA = "${nodeA.inDegree + nodeA.outDegree}", valB = "${nodeB.inDegree + nodeB.outDegree}")
            MetricRow(label = if (isFa) "سطح ریسک" else "Risk Severity", valA = nodeA.riskSeverity.name, valB = nodeB.riskSeverity.name)
            MetricRow(label = if (isFa) "درجه قطعیت ادله" else "Confidence", valA = nodeA.confidence.name, valB = nodeB.confidence.name)
            MetricRow(label = if (isFa) "تعداد اسناد ادله" else "Evidence Items", valA = "${nodeA.evidenceIds.size}", valB = "${nodeB.evidenceIds.size}")
        }
    }
}

@Composable
private fun MetricRow(label: String, valA: String, valB: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = valA, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.2f))
        Text(text = valB, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    }
}
