@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.aistudio.orbit.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.ui.components.designsystem.*
import com.aistudio.orbit.ui.theme.*
import com.aistudio.orbit.GraphExporter
import com.aistudio.orbit.GraphView
import com.aistudio.orbit.PdfReportExporter
import com.aistudio.orbit.forensics.AddressValidator
import com.aistudio.orbit.forensics.TemporalUtils
import com.aistudio.orbit.localization.AppLocalization
import com.aistudio.orbit.model.*
import com.aistudio.orbit.repository.AppLanguage
import com.aistudio.orbit.ui.InvestigationViewModel
import com.aistudio.orbit.ui.components.InteractiveCaseGraphVisualizer
import com.aistudio.orbit.forensics.export.ForensicEvidenceSealer
import com.aistudio.orbit.forensics.graph.ForensicCaseGraphEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewTab(
    investigationCase: InvestigationCase,
    isPersian: Boolean,
    strings: com.aistudio.orbit.localization.ForensicStrings
) {
    var dateFilterDays by remember { mutableStateOf<Int?>(null) } // null = All Time
    var minSeverity by remember { mutableStateOf<RiskSeverity?>(null) }
    var minBtcAmount by remember { mutableStateOf(0.0) }

    val cutoffSecs = dateFilterDays?.let { days -> (System.currentTimeMillis() / 1000L) - (days * 86400L) } ?: 0L

    val filteredTxs = remember(investigationCase.transactions, dateFilterDays, minBtcAmount) {
        investigationCase.transactions.filter { tx ->
            (tx.timestamp >= cutoffSecs) && (tx.relevantAmountSat.toDouble() / 100_000_000.0 >= minBtcAmount)
        }
    }

    val filteredRisks = remember(investigationCase.riskIndicators, minSeverity) {
        investigationCase.riskIndicators.filter { risk ->
            minSeverity?.let { risk.severity.ordinal >= it.ordinal } ?: true
        }
    }

    val totalTransactionsCount = filteredTxs.size
    val totalReceivedSat = filteredTxs.filter { it.direction == TxDirection.INCOMING }.sumOf { it.relevantAmountSat }
    val totalSentSat = filteredTxs.filter { it.direction == TxDirection.OUTGOING }.sumOf { it.relevantAmountSat }
    val currentBalanceSat = if (dateFilterDays == null && minBtcAmount == 0.0) investigationCase.balanceSat else (totalReceivedSat - totalSentSat).coerceAtLeast(0L)

    val uniqueCounterpartiesCount = remember(filteredTxs) {
        filteredTxs.flatMap { it.counterpartyAddresses }.distinct().size
    }

    val evidenceCount = investigationCase.evidenceLog.size
    val onChainEvidenceCount = investigationCase.evidenceLog.count { it.category == EvidenceCategory.OBSERVED_ON_CHAIN }
    val osintEvidenceCount = investigationCase.evidenceLog.count { it.category == EvidenceCategory.OSINT_INTELLIGENCE || it.category == EvidenceCategory.EXTERNAL_SOURCE }
    val behavioralEvidenceCount = investigationCase.evidenceLog.count { it.category == EvidenceCategory.BEHAVIORAL_PATTERN }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 960.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(
                                text = if (isPersian) "فیلترهای تحلیل پویای پرونده" else "Dynamic Investigative Filters",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isPersian) "دوره زمانی واکاوی:" else "Observation Period:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    Pair(null, if (isPersian) "همه زمان‌ها" else "All Time"),
                                    Pair(30, if (isPersian) "۳۰ روز اخیر" else "30 Days"),
                                    Pair(90, if (isPersian) "۹۰ روز اخیر" else "90 Days"),
                                    Pair(365, if (isPersian) "یک سال اخیر" else "1 Year")
                                ).forEach { (days, label) ->
                                    val isSelected = dateFilterDays == days
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { dateFilterDays = days },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isPersian) "حداقل شدت ریسک:" else "Min Risk Severity:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(
                                        Pair(null, if (isPersian) "همه" else "ALL"),
                                        Pair(RiskSeverity.MEDIUM, if (isPersian) "متوسط+" else "MED+"),
                                        Pair(RiskSeverity.HIGH, if (isPersian) "بالا+" else "HIGH+")
                                    ).forEach { (sev, label) ->
                                        val isSelected = minSeverity == sev
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { minSeverity = sev },
                                            label = { Text(label, fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isPersian) "حداقل مبلغ تراکنش (${investigationCase.network.symbol}):" else "Min Amount (${investigationCase.network.symbol}):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(0.0, 0.1, 1.0).forEach { amt ->
                                        val isSelected = minBtcAmount == amt
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { minBtcAmount = amt },
                                            label = { Text(if (amt == 0.0) (if (isPersian) "همه مبالغ" else "0.0") else "$amt ${investigationCase.network.symbol}", fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = strings.balance,
                            value = TemporalUtils.formatCryptoAmount(currentBalanceSat, investigationCase.network, isPersian),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = strings.totalTransactions,
                            value = TemporalUtils.formatNumber(totalTransactionsCount, isPersian),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = strings.totalReceived,
                            value = TemporalUtils.formatCryptoAmount(totalReceivedSat, investigationCase.network, isPersian),
                            color = Color(0xFF16A34A),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = strings.totalSent,
                            value = TemporalUtils.formatCryptoAmount(totalSentSat, investigationCase.network, isPersian),
                            color = Color(0xFFDC2626),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isPersian) "بازه زمانی فعالیت آن‌چین" else "On-Chain Activity Window",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = strings.firstActivity, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = investigationCase.firstTxTimestamp?.let { TemporalUtils.formatDateTime(it, isPersian, true) } ?: (if (isPersian) "ثبت نشده" else "None"),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = strings.lastActivity, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = investigationCase.lastTxTimestamp?.let { TemporalUtils.formatDateTime(it, isPersian, true) } ?: (if (isPersian) "ثبت نشده" else "None"),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isPersian) "شاخص‌های خلاصه پرونده" else "Forensic Summary Indicators",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (isPersian) "طرف‌های مقابل یکتا:" else "Unique Counterparties:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = TemporalUtils.formatNumber(uniqueCounterpartiesCount, isPersian), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (isPersian) "کل ادله ثبت‌شده:" else "Total Evidence Logged:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = TemporalUtils.formatNumber(evidenceCount, isPersian), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        if (evidenceCount > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = if (isPersian) "ادله مستقیم تراکنشی:" else "Direct On-Chain Facts:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = TemporalUtils.formatNumber(onChainEvidenceCount, isPersian), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = if (isPersian) "ادله هویت‌یابی و OSINT:" else "OSINT Identity Correlations:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = TemporalUtils.formatNumber(osintEvidenceCount, isPersian), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = if (isPersian) "انطباق الگوهای رفتاری مشکوک:" else "Behavioral Pattern Matches:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = TemporalUtils.formatNumber(behavioralEvidenceCount, isPersian), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                        Column {
                            Text(
                                text = if (isPersian) "تفکیک اصول معرفت‌شناختی کارشناسی پرونده" else "Forensic Epistemic Separation Principal",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isPersian)
                                    "طبق اصول ابلاغی جرم‌یابی مالی، ادله پرونده به طور صریح به دو بخش «حقایق مشاهده‌شده آن‌چین» با قطعیت مطلق و «فرضیه‌های تحلیلی/انتساب هویت» با مقادیر احتمالاتی مجزا طبقه‌بندی می‌شوند تا از تداخل گمانه‌زنی با شواهد عینی ممانعت به عمل آید."
                                else "Under financial forensics mandates, all findings strictly delineate 'Observed On-Chain Facts' from probabilistic 'Investigative Hypotheses' and attributions to maintain forensic integrity for legal presentation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "${strings.riskIndicatorsDetected} (${TemporalUtils.formatNumber(filteredRisks.size, isPersian)})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (filteredRisks.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isPersian) "هیچ شاخص ریسک یا الگوی مشکوکی منطبق بر معیارهای فیلتر انتخاب شده یافت نشد."
                            else "No risk indicators match the selected filtering criteria.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredRisks) { risk ->
                    RiskIndicatorCard(risk = risk, isPersian = isPersian)
                }
            }
        }
    }
}

@Composable
fun EvidenceChainTab(
    investigationCase: InvestigationCase,
    isPersian: Boolean,
    strings: com.aistudio.orbit.localization.ForensicStrings
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 960.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = strings.evidenceLogTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = strings.evidenceLogSubtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }

            items(investigationCase.evidenceLog) { item ->
                EvidenceItemCard(item = item, isPersian = isPersian)
            }
        }
    }
}

@Composable
fun TransactionsTab(
    investigationCase: InvestigationCase,
    isPersian: Boolean,
    strings: com.aistudio.orbit.localization.ForensicStrings,
    viewModel: InvestigationViewModel
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 960.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(investigationCase.transactions) { tx ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (tx.direction) {
                                    TxDirection.INCOMING -> Color(0xFF16A34A)
                                    TxDirection.OUTGOING -> Color(0xFFDC2626)
                                    TxDirection.SELF_TRANSFER -> Color(0xFF0284C7)
                                    TxDirection.MIXED -> Color(0xFFEA580C)
                                    TxDirection.CONTRACT_CALL -> Color(0xFF7C3AED)
                                }
                            ) {
                                Text(
                                    text = when (tx.direction) {
                                        TxDirection.INCOMING -> strings.dirIncoming
                                        TxDirection.OUTGOING -> strings.dirOutgoing
                                        TxDirection.SELF_TRANSFER -> strings.dirSelf
                                        TxDirection.MIXED -> strings.dirMixed
                                        TxDirection.CONTRACT_CALL -> if (isPersian) "فراخوانی قرارداد" else "Contract"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = TemporalUtils.formatCryptoAmount(tx.relevantAmountSat, investigationCase.network, isPersian),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "TxID: ${tx.txId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            
                            val aiConfigs by viewModel.aiSettingsRepo.configs.collectAsState()
                            val hasAiConfig = aiConfigs.values.any { it.enabled }
                            if (hasAiConfig) {
                                IconButton(
                                    onClick = { 
                                        viewModel.setAiCopilotPromptQueue(if (isPersian) "لطفاً تراکنش زیر را تحلیل کن و هدف احتمالی آن را توضیح بده:\n${tx.txId}" else "Please analyze this transaction and explain its likely purpose:\n${tx.txId}") 
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.SmartToy, contentDescription = "Explain with AI", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${strings.time}: ${TemporalUtils.formatDateTime(tx.timestamp, isPersian, true)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${strings.fee}: ${TemporalUtils.formatCryptoAmount(tx.feeSat, investigationCase.network, isPersian)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GraphTab(
    viewModel: InvestigationViewModel,
    graph: com.aistudio.orbit.Graph?,
    selectedNodeId: String?,
    investigationCase: InvestigationCase,
    isPersian: Boolean,
    strings: com.aistudio.orbit.localization.ForensicStrings
) {
    val context = LocalContext.current
    val osintReport by viewModel.osintReport.collectAsState()
    val deepIdentityDossier by viewModel.deepIdentityDossier.collectAsState()
    val deepCandidates = deepIdentityDossier?.phoneReconstruction?.topCandidates ?: emptyList()
    val osintSession by viewModel.osintSession.collectAsState()

    var useInteractiveEngine by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toggle toolbar for choosing visualization engine
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (useInteractiveEngine) {
                            if (isPersian) "گراف پیشرفته" else "Interactive Graph"
                        } else {
                            if (isPersian) "گراف کلاسیک" else "Classic Graph"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val aiConfigs by viewModel.aiSettingsRepo.configs.collectAsState()
                    val hasAiConfig = aiConfigs.values.any { it.enabled }
                    if (hasAiConfig) {
                        IconButton(
                            onClick = { 
                                viewModel.setAiCopilotPromptQueue(if (isPersian) "الگوی گراف و خوشه‌های ارتباطی در این پرونده را تحلیل کن." else "Please analyze the graph topology and clustering patterns in this case.") 
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = "AI Graph Analysis", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    FilterChip(
                        selected = useInteractiveEngine,
                        onClick = { useInteractiveEngine = true },
                        label = { Text(if (isPersian) "پیشرفته" else "Advanced", fontSize = 11.sp, softWrap = false, maxLines = 1) },
                        leadingIcon = {
                            if (useInteractiveEngine) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                        }
                    )
                    FilterChip(
                        selected = !useInteractiveEngine,
                        onClick = { useInteractiveEngine = false },
                        label = { Text(if (isPersian) "کلاسیک" else "Classic", fontSize = 11.sp, softWrap = false, maxLines = 1) }
                    )
                }
            }
        }

        val interactiveGraph = remember(investigationCase, osintReport, deepCandidates, osintSession) {
            ForensicCaseGraphEngine.buildCaseGraph(
                investigationCase = investigationCase,
                osintReport = osintReport,
                deepIdentityCandidates = deepCandidates,
                osintSession = osintSession
            )
        }

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            if (useInteractiveEngine) {
                InteractiveCaseGraphVisualizer(
                    initialGraph = interactiveGraph,
                    isPersian = isPersian,
                    modifier = Modifier.fillMaxSize(),
                    onRunDeepReconForNode = { node ->
                        val cleanTarget = node.label.replace("Email: ", "").replace("Phone: ", "").trim()
                        viewModel.runDeepIdentityReconstruction(cleanTarget, investigationCase.targetAddress)
                    },
                    onAddNodeToEvidence = { node ->
                        viewModel.addNodeAsEvidence(node)
                        Toast.makeText(
                            context,
                            if (isPersian) "گره ${node.label} با امضای دیجیتال و مهر زنجیره مدرک به ادله پرونده پیوست شد" else "Node ${node.label} cryptographically attached to case evidence",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } else {
                if (graph != null && graph.nodes.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        GraphView(
                            graph = graph,
                            onSetAsSeed = { selectedSeed ->
                                viewModel.selectNode(selectedSeed)
                            }
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            tonalElevation = 4.dp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                        ) {
                            Text(
                                text = strings.selectNodePrompt,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = if (isPersian) "اطلاعاتی جهت رسم گراف موجود نیست." else "No graph data available.")
                    }
                }
            }
        }
    }
}

@Composable
fun CounterpartyMatrixTab(
    investigationCase: InvestigationCase,
    isPersian: Boolean,
    strings: com.aistudio.orbit.localization.ForensicStrings
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 960.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "${strings.counterpartyMatrix} (${TemporalUtils.formatNumber(investigationCase.counterparties.size, isPersian)})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(investigationCase.counterparties) { cp ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cp.address,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${TemporalUtils.formatNumber(cp.txCount, isPersian)} ${if (isPersian) "تراکنش" else "txs"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${strings.volumeReceived}: ${TemporalUtils.formatCryptoAmount(cp.totalReceivedSatFromCounterparty, investigationCase.network, isPersian)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF16A34A)
                            )
                            Text(
                                text = "${strings.volumeSent}: ${TemporalUtils.formatCryptoAmount(cp.totalSentSatToCounterparty, investigationCase.network, isPersian)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun RiskIndicatorCard(risk: RiskIndicator, isPersian: Boolean) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                when (risk.severity) {
                    RiskSeverity.CRITICAL, RiskSeverity.HIGH -> Color(0xFFD32F2F)
                    RiskSeverity.MEDIUM -> Color(0xFFF57C00)
                    else -> Color(0xFF1976D2)
                }
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = risk.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (risk.severity) {
                        RiskSeverity.CRITICAL, RiskSeverity.HIGH -> Color(0xFFD32F2F)
                        RiskSeverity.MEDIUM -> Color(0xFFF57C00)
                        else -> Color(0xFF1976D2)
                    }
                ) {
                    Text(
                        text = risk.severity.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(text = risk.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (risk.recommendedAction.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${if (isPersian) "اقدام پیشنهادی: " else "Recommended Action: "}${risk.recommendedAction}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun EvidenceItemCard(item: EvidenceItem, isPersian: Boolean) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (item.isDirectFact) Color(0xFF1B5E20) else Color(0xFF0D47A1)
                ) {
                    Text(
                        text = if (item.isDirectFact) (if (isPersian) "حقیقت قطعی داده بلاکچین" else "ON-CHAIN FACT")
                        else (if (isPersian) "تحلیل محاسبه‌شده / الگو" else "DERIVED / INFERENCE"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (isPersian) item.confidence.displayNameFa else item.confidence.displayNameEn,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(text = if (isPersian) item.titleFa else item.titleEn, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = if (isPersian) item.descriptionFa else item.descriptionEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${if (isPersian) "منبع داده: " else "Source: "}${item.providerName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = TemporalUtils.formatDateTime(item.timestamp, isPersian, true),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (item.contentHash.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (isPersian) "هش یکپارچگی مدرک (SHA-256):" else "Evidence Integrity Hash:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "v${item.version}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = item.contentHash,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                textDirection = TextDirection.Ltr
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.previousHash.isNotEmpty()) {
                            Text(
                                text = if (isPersian) "هش پیوند زنجیره قبلی:" else "Previous Chain Link Hash:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = item.previousHash,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    textDirection = TextDirection.Ltr
                                ),
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
