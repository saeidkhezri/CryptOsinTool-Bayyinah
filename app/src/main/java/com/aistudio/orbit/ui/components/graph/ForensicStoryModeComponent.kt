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
import com.aistudio.orbit.forensics.graph.InvestigationStoryStep
import com.aistudio.orbit.forensics.graph.StoryStepCategory
import com.aistudio.orbit.forensics.graph.VisualConfidence
import com.aistudio.orbit.model.RiskSeverity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Interactive Forensic Story Mode Component.
 */
@Composable
fun ForensicStoryModeComponent(
    storySteps: List<InvestigationStoryStep>,
    isFa: Boolean,
    onStepSelected: (InvestigationStoryStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val sdfUtc = SimpleDateFormat("yyyy-MM-dd HH:mm 'UTC'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
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
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = if (isFa) "روایت پی‌جویی و گاه‌شمار ادله (Investigation Story Mode)" else "Interactive Investigation Story",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isFa) "روایت گام‌به‌گام واکاوی پرونده از شناسایی اولیه تا یافته‌های پلمب‌شده" else "Chronological investigative narrative from initial inception to sealed findings",
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
            items(storySteps) { step ->
                val categoryColor = when (step.stepCategory) {
                    StoryStepCategory.STARTING_POINT -> Color(0xFF1565C0)
                    StoryStepCategory.TRANSACTION_FLOW -> Color(0xFF00897B)
                    StoryStepCategory.COUNTERPARTY -> Color(0xFF5E35B1)
                    StoryStepCategory.CLUSTER_EXPANSION -> Color(0xFF1B5E20)
                    StoryStepCategory.SERVICE_EXIT -> Color(0xFF0D47A1)
                    StoryStepCategory.OSINT_DISCOVERY -> Color(0xFFE65100)
                    StoryStepCategory.ENTITY_RESOLVED -> Color(0xFFD81B60)
                    StoryStepCategory.EVIDENCE_SEALED -> Color(0xFF4A148C)
                    StoryStepCategory.FINDING_FORMULATED -> Color(0xFF2E7D32)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, categoryColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { onStepSelected(step) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Step number bubble
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(categoryColor.copy(alpha = 0.15f), CircleShape)
                                .border(1.5.dp, categoryColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${step.stepIndex}",
                                fontWeight = FontWeight.Bold,
                                color = categoryColor,
                                fontSize = 14.sp
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
                                    text = if (isFa) step.stepCategory.displayNameFa else step.stepCategory.displayNameEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = categoryColor,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = sdfUtc.format(Date(step.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = if (isFa) step.titleFa else step.titleEn,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = if (isFa) step.descriptionFa else step.descriptionEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Evidence tags
                            if (step.evidenceIds.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    step.evidenceIds.forEach { ev ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = ev,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
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
        }
    }
}
