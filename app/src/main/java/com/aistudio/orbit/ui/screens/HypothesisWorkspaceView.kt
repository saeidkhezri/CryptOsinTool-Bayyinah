package com.aistudio.orbit.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.forensics.learning.*
import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.model.Hypothesis
import com.aistudio.orbit.model.HypothesisReviewState
import com.aistudio.orbit.model.ConfidenceLevel
import com.aistudio.orbit.forensics.analysis.HypothesisEngine
import com.aistudio.orbit.forensics.osint.OsintCorrelationEngine
import com.aistudio.orbit.ui.InvestigationViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HypothesisWorkspaceView(
    viewModel: InvestigationViewModel,
    investigationCase: InvestigationCase,
    isFa: Boolean
) {
    var activeExplanation by remember { mutableStateOf<ExplanationCardData?>(null) }
    
    val hypotheses = investigationCase.hypotheses

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
                        text = if (isFa) "فرضیات و یافته‌های تحلیلی کارشناس" else "Analyst Hypotheses & Findings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isFa) "تفکیک اصول معرفت‌شناختی: واقعیت، استنباط، و فرضیه" else "Epistemic Separation: Fact, Inference, & Hypothesis",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    val osintReport = viewModel.osintReport.value
                    val correlated = OsintCorrelationEngine.generateCorrelationHypotheses(investigationCase, osintReport)
                    val newHypotheses = hypotheses + correlated
                    viewModel.updateActiveCase(investigationCase.copy(hypotheses = newHypotheses))
                }) {
                    Icon(Icons.Default.Policy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFa) "استنتاج OSINT" else "OSINT Correlate")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val newHypothesis = HypothesisEngine.createHypothesis(
                        caseId = investigationCase.id,
                        investigationId = investigationCase.id,
                        title = if (isFa) "فرضیه جدید" else "New Hypothesis",
                        description = if (isFa) "شرح فرضیه..." else "Hypothesis description...",
                        confidence = ConfidenceLevel.LOW_CONFIDENCE,
                        author = "Analyst"
                    )
                    val updatedList = hypotheses + newHypothesis
                    viewModel.updateActiveCase(investigationCase.copy(hypotheses = updatedList))
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFa) "فرضیه جدید" else "New Hypothesis")
                }
            }
        }
        
        HorizontalDivider()

        if (hypotheses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (isFa) "هیچ فرضیه‌ای برای این پرونده ثبت نشده است." else "No hypotheses recorded for this case.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(hypotheses) { hyp ->
                    HypothesisCard(
                        hypothesis = hyp,
                        isFa = isFa,
                        onWhyAmISeeingThis = {
                            activeExplanation = ExplanationCardData(
                                titleFa = hyp.title,
                                titleEn = hyp.title,
                                certaintyLevel = ForensicCertaintyLevel.HYPOTHESIS,
                                inputDataFa = "داده‌های مستقیم پرونده و ادله دیجیتال ثبت‌شده",
                                inputDataEn = "Direct case data and recorded digital evidence",
                                analysisMethodFa = "ارزیابی اپیستمیک کارشناس بر اساس سطح قطعیت",
                                analysisMethodEn = "Analyst epistemic evaluation based on certainty guidelines",
                                resultSummaryFa = hyp.description,
                                resultSummaryEn = hyp.description,
                                evidenceIds = hyp.evidenceLinks.map { it.evidenceId },
                                limitationsFa = listOf("فرضیات تحلیلی نیازمند صحه‌گذاری با ادله متقابل و استعلامات ثانویه هستند."),
                                limitationsEn = listOf("Hypotheses require corroboration with cross-evidence and secondary inquiries.")
                            )
                        },
                        onUpdate = { updatedHypothesis ->
                            val updatedList = hypotheses.map { if (it.id == updatedHypothesis.id) updatedHypothesis else it }
                            viewModel.updateActiveCase(investigationCase.copy(hypotheses = updatedList))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HypothesisCard(
    hypothesis: Hypothesis,
    isFa: Boolean,
    onWhyAmISeeingThis: () -> Unit,
    onUpdate: (Hypothesis) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val stateColor = when (hypothesis.reviewState) {
        HypothesisReviewState.ACCEPTED -> Color(0xFF16A34A)
        HypothesisReviewState.REJECTED -> Color(0xFFDC2626)
        HypothesisReviewState.PENDING_REVIEW -> Color(0xFFF57C00)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, stateColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = hypothesis.id.take(12),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(shape = RoundedCornerShape(4.dp), color = stateColor) {
                        Text(
                            text = hypothesis.reviewState.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CertaintyBadge(level = ForensicCertaintyLevel.HYPOTHESIS, isPersian = isFa)
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = hypothesis.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (hypothesis.description.isNotBlank()) {
                Text(
                    text = hypothesis.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    
                    Text(
                        text = if (isFa) "پشتیبانی ادله مستند:" else "Evidentiary Support:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (hypothesis.evidenceLinks.isEmpty()) {
                        Text(
                            text = if (isFa) "هیچ مدرکی به این فرضیه لینک نشده است." else "No evidence linked to this hypothesis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        hypothesis.evidenceLinks.forEach { link ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = "${link.supportLevel.name}: ${link.evidenceId.take(8)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (link.analystNote.isNotBlank()) {
                                    Text(" - ${link.analystNote}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    
                    if (hypothesis.analystNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isFa) "یادداشت‌های تحلیلگر / داور:" else "Analyst / Reviewer Notes:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = hypothesis.analystNotes,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WhyAmISeeingThisButton(
                            isPersian = isFa,
                            onClick = onWhyAmISeeingThis
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (hypothesis.reviewState == HypothesisReviewState.DRAFT || hypothesis.reviewState == HypothesisReviewState.REVISED) {
                                Button(
                                    onClick = { onUpdate(HypothesisEngine.requestReview(hypothesis, "Analyst")) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isFa) "درخواست بررسی" else "Request Review", style = MaterialTheme.typography.labelSmall)
                                }
                            } else if (hypothesis.reviewState == HypothesisReviewState.PENDING_REVIEW) {
                                OutlinedButton(
                                    onClick = { onUpdate(HypothesisEngine.rejectHypothesis(hypothesis, "Reviewer", "Insufficient grounded evidence.")) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isFa) "رد فرضیه" else "Reject", style = MaterialTheme.typography.labelSmall)
                                }
                                Button(
                                    onClick = { onUpdate(HypothesisEngine.acceptHypothesis(hypothesis, "Reviewer", "Approved for report inclusion.")) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isFa) "تایید فرضیه" else "Accept", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


