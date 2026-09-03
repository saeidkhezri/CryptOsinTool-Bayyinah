@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.aistudio.orbit.ui.components.graph

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.forensics.graph.VisualConfidence
import com.aistudio.orbit.forensics.graph.VisualInvestigationEdge
import com.aistudio.orbit.forensics.graph.VisualInvestigationNode
import com.aistudio.orbit.forensics.graph.VisualNodeType
import com.aistudio.orbit.model.RiskSeverity

/**
 * Forensic Node / Edge Detail Drawer.
 */
@Composable
fun ForensicNodeDetailDrawer(
    selectedNode: VisualInvestigationNode?,
    selectedEdge: VisualInvestigationEdge?,
    isFa: Boolean,
    onClose: () -> Unit,
    onFindPaths: (String) -> Unit,
    onExpandNeighborhood: (String) -> Unit,
    onCreateHypothesis: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (selectedNode != null) Icons.Default.Info else Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (selectedNode != null) {
                            if (isFa) "شناسنامه کارشناسی گره" else "Forensic Node Dossier"
                        } else {
                            if (isFa) "مشخصات رابطه / تراکنش" else "Forensic Edge Dossier"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            if (selectedNode != null) {
                NodeDetailContent(
                    node = selectedNode,
                    isFa = isFa,
                    context = context,
                    onFindPaths = onFindPaths,
                    onExpandNeighborhood = onExpandNeighborhood,
                    onCreateHypothesis = onCreateHypothesis
                )
            } else if (selectedEdge != null) {
                EdgeDetailContent(
                    edge = selectedEdge,
                    isFa = isFa,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun NodeDetailContent(
    node: VisualInvestigationNode,
    isFa: Boolean,
    context: Context,
    onFindPaths: (String) -> Unit,
    onExpandNeighborhood: (String) -> Unit,
    onCreateHypothesis: (String) -> Unit
) {
    val riskColor = when (node.riskSeverity) {
        RiskSeverity.CRITICAL -> Color(0xFFD32F2F)
        RiskSeverity.HIGH -> Color(0xFFE65100)
        RiskSeverity.MEDIUM -> Color(0xFFFFA000)
        RiskSeverity.LOW -> Color(0xFF388E3C)
        RiskSeverity.INFO -> Color(0xFF1976D2)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ID & Copy Row
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = node.primaryIdentifier,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Node ID", node.primaryIdentifier))
                    Toast.makeText(context, if (isFa) "شناسه کپی شد" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                }
            }
        }

        // Metrics Grid (Risk vs Confidence - Orthogonal Display)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Risk Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = riskColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, riskColor.copy(alpha = 0.5f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = if (isFa) "سطح ریسک رفتاری" else "Behavioral Risk",
                        style = MaterialTheme.typography.labelSmall,
                        color = riskColor
                    )
                    Text(
                        text = node.riskSeverity.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = riskColor
                    )
                }
            }

            // Confidence Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = if (isFa) "درجه قطعیت ادله" else "Attribution Confidence",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = if (isFa) node.confidence.displayNameFa else node.confidence.displayNameEn,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Ledger & OSINT Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (node.balanceDisplay.isNotBlank()) {
                Text(
                    text = "${if (isFa) "موجودی:" else "Balance:"} ${node.balanceDisplay}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${if (isFa) "تراکنش‌ها:" else "Txs:"} ${node.txCount}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${if (isFa) "درجه اتصال:" else "Degree:"} ${node.inDegree + node.outDegree}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Epistemic Explanation Section ("WHY?")
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isFa) "مستندات و توجیه ادله (WHY? Explanation)" else "Evidence Justification (WHY?)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = if (isFa) {
                        "این موجودیت بر اساس ${node.sourceCount} منبع مستقل و ${node.evidenceIds.size} سند ثبت‌شده در زنجیره ادله ارزیابی شده است. قطعیت انتساب بر پایه مدارک متقاطع تایید شده است."
                    } else {
                        "Entity assessed based on ${node.sourceCount} independent sources and ${node.evidenceIds.size} sealed evidence items in the provenance chain."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (node.evidenceIds.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        node.evidenceIds.forEach { evId ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = evId,
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
        }

        // Action Buttons
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isNarrow = maxWidth < 420.dp
            if (isNarrow) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onFindPaths(node.id) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFa) "مسیر‌یابی" else "Find Paths", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { onExpandNeighborhood(node.id) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.GroupWork, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFa) "توسعه همسایگی" else "Expand", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = { onCreateHypothesis(node.id) },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isFa) "ثبت فرضیه تحقیقاتی جدید" else "Create New Hypothesis", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { onFindPaths(node.id) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFa) "مسیر‌یابی" else "Find Paths", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { onExpandNeighborhood(node.id) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.GroupWork, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFa) "توسعه همسایگی" else "Expand", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onCreateHypothesis(node.id) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFa) "فرضیه جدید" else "Hypothesis", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EdgeDetailContent(
    edge: VisualInvestigationEdge,
    isFa: Boolean,
    context: Context
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isFa) edge.relationshipType.displayNameFa else edge.relationshipType.displayNameEn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${edge.sourceId.take(8)}... ➔ ${edge.targetId.take(8)}...",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                if (edge.amountDisplay.isNotBlank()) {
                    Text(
                        text = "${if (isFa) "حجم انتقال:" else "Transfer Amount:"} ${edge.amountDisplay}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Epistemic Status & Evidence
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "${if (isFa) "وضعیت معرفت‌شناختی:" else "Epistemic Status:"} ${edge.epistemicStyle.name}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${if (isFa) "منبع:" else "Source:"} ${edge.source}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (edge.evidenceIds.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                edge.evidenceIds.forEach { ev ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = ev,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
