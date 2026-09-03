package com.aistudio.orbit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.orbit.db.FindingEntity
import com.aistudio.orbit.db.EpistemicStatus
import com.aistudio.orbit.forensics.learning.*
import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.ui.InvestigationViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HypothesisWorkspaceView(
    viewModel: InvestigationViewModel,
    investigationCase: InvestigationCase,
    isFa: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    var findings by remember { mutableStateOf<List<FindingEntity>>(emptyList()) }
    var activeExplanation by remember { mutableStateOf<ExplanationCardData?>(null) }
    
    // Simplistic side effect to load findings
    LaunchedEffect(investigationCase.id) {
        findings = com.aistudio.orbit.db.AppDatabase.getDatabase(viewModel.getApplication())
            .findingDao().getFindingsListForCase(investigationCase.id)
    }

    activeExplanation?.let { data ->
        WhyAmISeeingThisDialog(
            data = data,
            isPersian = isFa,
            onDismiss = { activeExplanation = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isFa) "فرضیات و یافته‌های کارشناس" else "Analyst Hypotheses & Findings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isFa) "تفکیک واقعیت، محاسبه، استنباط و فرضیه" else "Categorized Fact, Calculation, Inference & Hypothesis",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            Button(onClick = {
                val newFinding = FindingEntity(
                    findingId = "HYP-${UUID.randomUUID().toString().take(6).uppercase()}",
                    caseId = investigationCase.id,
                    title = if (isFa) "فرضیه جدید" else "New Hypothesis",
                    description = "",
                    epistemicStatus = EpistemicStatus.HYPOTHESIS
                )
                coroutineScope.launch {
                    com.aistudio.orbit.db.AppDatabase.getDatabase(viewModel.getApplication()).findingDao().insertFinding(newFinding)
                    findings = com.aistudio.orbit.db.AppDatabase.getDatabase(viewModel.getApplication()).findingDao().getFindingsListForCase(investigationCase.id)
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isFa) "افزودن" else "Add")
            }
        }
        
        HorizontalDivider()

        if (findings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (isFa) "هیچ فرضیه یا یافته‌ای ثبت نشده است." else "No hypotheses or findings recorded.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(findings) { finding ->
                    HypothesisCard(
                        finding = finding,
                        isFa = isFa,
                        onWhyAmISeeingThis = {
                            val certainty = when (finding.epistemicStatus) {
                                EpistemicStatus.FACT -> ForensicCertaintyLevel.OBSERVED_FACT
                                EpistemicStatus.INFERENCE -> ForensicCertaintyLevel.INFERENCE
                                EpistemicStatus.HYPOTHESIS -> ForensicCertaintyLevel.HYPOTHESIS
                                else -> ForensicCertaintyLevel.CALCULATED_RESULT
                            }
                            activeExplanation = ExplanationCardData(
                                titleFa = finding.titleFa.ifBlank { finding.title },
                                titleEn = finding.title.ifBlank { finding.titleFa },
                                certaintyLevel = certainty,
                                inputDataFa = "داده‌های مستقیم پرونده و ادله دیجیتال ثبت‌شده",
                                inputDataEn = "Direct case data and recorded digital evidence",
                                analysisMethodFa = "ارزیابی اپیستمیک کارشناس بر اساس سطح قطعیت",
                                analysisMethodEn = "Analyst epistemic evaluation based on certainty guidelines",
                                resultSummaryFa = finding.descriptionFa.ifBlank { finding.description },
                                resultSummaryEn = finding.description.ifBlank { finding.descriptionFa },
                                evidenceIds = investigationCase.evidenceLog.map { it.id }.take(2),
                                limitationsFa = listOf("فرضیات تحلیلی نیازمند صحه‌گذاری با ادله متقابل و استعلامات ثانویه هستند."),
                                limitationsEn = listOf("Hypotheses require corroboration with cross-evidence and secondary inquiries.")
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HypothesisCard(
    finding: FindingEntity,
    isFa: Boolean,
    onWhyAmISeeingThis: (() -> Unit)? = null
) {
    val certainty = when (finding.epistemicStatus) {
        EpistemicStatus.FACT -> ForensicCertaintyLevel.OBSERVED_FACT
        EpistemicStatus.INFERENCE -> ForensicCertaintyLevel.INFERENCE
        EpistemicStatus.HYPOTHESIS -> ForensicCertaintyLevel.HYPOTHESIS
        else -> ForensicCertaintyLevel.CALCULATED_RESULT
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = finding.findingId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CertaintyBadge(level = certainty, isPersian = isFa)
            }

            Text(
                text = if (isFa) finding.titleFa else finding.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (finding.description.isNotBlank()) {
                Text(
                    text = if (isFa) finding.descriptionFa else finding.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (onWhyAmISeeingThis != null) {
                WhyAmISeeingThisButton(
                    isPersian = isFa,
                    onClick = onWhyAmISeeingThis,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

