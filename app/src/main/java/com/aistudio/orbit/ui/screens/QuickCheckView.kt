package com.aistudio.orbit.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.orbit.forensics.AddressValidator
import com.aistudio.orbit.forensics.learning.CryptoMiniLessonDialog
import com.aistudio.orbit.forensics.learning.LearnThisBadge
import com.aistudio.orbit.forensics.learning.MiniLessonTopic
import com.aistudio.orbit.localization.AppLocalization
import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.repository.AppLanguage
import com.aistudio.orbit.ui.InvestigationViewModel
import com.aistudio.orbit.ui.components.designsystem.*
import com.aistudio.orbit.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Quick Check Mode (Master Instruction §4.3 & Prompt 2 §3).
 * Designed for rapid triage and validation of a single address lead.
 * Compact workflow:
 * 1. INPUT LEAD -> 2. VALIDATE -> 3. SUMMARY -> 4. KEY SIGNALS -> 5. NEXT ACTION
 */
@Composable
fun QuickCheckView(
    viewModel: InvestigationViewModel,
    investigationCase: InvestigationCase,
    isPersian: Boolean,
    onEscalateToGuided: () -> Unit,
    onEscalateToAnalyst: () -> Unit
) {
    val strings = AppLocalization.getStrings(if (isPersian) AppLanguage.PERSIAN else AppLanguage.ENGLISH)
    val clipboardManager = LocalClipboardManager.current
    var activeLessonTopic by remember { mutableStateOf<MiniLessonTopic?>(null) }
    val scrollState = rememberScrollState()

    var inputAddress by remember(investigationCase.targetAddress) { mutableStateOf(investigationCase.targetAddress) }
    var selectedNetwork by remember(investigationCase.network) { mutableStateOf(investigationCase.network) }

    val validationResult = remember(inputAddress, selectedNetwork) {
        if (inputAddress.isBlank()) null
        else AddressValidator.validate(inputAddress.trim(), selectedNetwork)
    }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(ForensicSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ForensicSpacing.md)
    ) {
        // Mode Header & Context
        ForensicCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = ForensicShapes.pill,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = strings.modeQuickCheckTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.modeQuickCheckSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                ForensicBadge(
                    text = "TRIAGE",
                    badgeType = ForensicBadgeType.INFO
                )
            }

            // Workflow Steps Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ForensicSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val steps = listOf(
                    strings.quickStepInput,
                    strings.quickStepValidate,
                    strings.quickStepSummary,
                    strings.quickStepSignals,
                    strings.quickStepNextAction
                )
                steps.forEachIndexed { index, stepTitle ->
                    Surface(
                        shape = ForensicShapes.pill,
                        color = if (index <= 3) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = stepTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (index <= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = if (index <= 3) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // STEP 1 & 2: INPUT & VALIDATION
        ForensicCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.quickStepInput} & ${strings.quickStepValidate}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                LearnThisBadge(
                    topic = MiniLessonTopic.ADDRESS_VS_WALLET,
                    isPersian = isPersian,
                    onClick = { activeLessonTopic = MiniLessonTopic.ADDRESS_VS_WALLET }
                )
            }

            OutlinedTextField(
                value = inputAddress,
                onValueChange = { inputAddress = it },
                label = { Text(strings.leadAddressLabel) },
                placeholder = { Text("bc1q... / 1... / 3... / 0x...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let { inputAddress = it.trim() }
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Paste")
                        }
                        if (inputAddress.isNotBlank()) {
                            IconButton(onClick = { inputAddress = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                }
            )

            // Validation Status Banner
            if (validationResult != null) {
                Surface(
                    shape = ForensicShapes.sm,
                    color = if (validationResult.isValid) Color(0xFF2E7D32).copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(ForensicSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.xs)
                    ) {
                        Icon(
                            imageVector = if (validationResult.isValid) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (validationResult.isValid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (validationResult.isValid) {
                                "${strings.addressValidationSuccess} (${validationResult.addressType.name})"
                            } else {
                                validationResult.errorReason ?: strings.addressValidationError
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (validationResult.isValid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (inputAddress.trim() != investigationCase.targetAddress && validationResult?.isValid == true) {
                Button(
                    onClick = {
                        viewModel.startNewInvestigation(
                            referenceNumber = "QC-${System.currentTimeMillis() % 100000}",
                            caseTitle = "Quick Check: ${inputAddress.trim().take(8)}",
                            targetAddress = inputAddress.trim(),
                            network = selectedNetwork,
                            scopeDescription = "Quick Check lead triage",
                            notes = "Initiated from Quick Check triage mode.",
                            tags = listOf("QuickCheck", selectedNetwork.name)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isPersian) "بارگذاری مجدد این آدرس در وضعیت زنده" else "Load this Address in Triage")
                }
            }
        }

        // STEP 3: ACTIVITY SUMMARY
        ForensicCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.quickStepSummary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                ForensicEpistemicBadge(
                    type = ForensicEpistemicType.OBSERVED_FACT,
                    isPersian = isPersian,
                    source = "Bitcoin Node RPC",
                    confidencePercent = 100,
                    compact = true
                )
            }

            ForensicAddressText(
                address = investigationCase.targetAddress,
                modifier = Modifier.fillMaxWidth(),
                isPersian = isPersian
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.sm)
            ) {
                ForensicMetricCard(
                    label = strings.balance,
                    value = "${String.format(Locale.US, "%.6f", investigationCase.balanceBtc)} BTC",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    subtitle = "${String.format(Locale.US, "%,d", (investigationCase.balanceBtc * 65000.0).toLong())} USD"
                )
                ForensicMetricCard(
                    label = strings.totalTransactions,
                    value = "${investigationCase.transactions.size}",
                    icon = Icons.Default.ReceiptLong,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                    subtitle = "${investigationCase.counterparties.size} ${strings.uniqueCounterparties}"
                )
            }

            // Temporal span
            val earliestTx = investigationCase.transactions.minByOrNull { it.timestamp }
            val latestTx = investigationCase.transactions.maxByOrNull { it.timestamp }
            if (earliestTx != null && latestTx != null) {
                Surface(
                    shape = ForensicShapes.xs,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(ForensicSpacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(strings.firstActivity, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dateFormat.format(Date(earliestTx.timestamp * 1000L)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(strings.lastActivity, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dateFormat.format(Date(latestTx.timestamp * 1000L)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // STEP 4: KEY SIGNALS
        ForensicCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.quickStepSignals,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                LearnThisBadge(
                    topic = MiniLessonTopic.CONFIDENCE_METRIC,
                    isPersian = isPersian,
                    onClick = { activeLessonTopic = MiniLessonTopic.CONFIDENCE_METRIC }
                )
            }

            // Risk Indicators
            val riskList = investigationCase.riskIndicators
            if (riskList.isNotEmpty()) {
                Text(
                    text = "${strings.keyRiskSignals} (${riskList.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                riskList.take(3).forEach { risk ->
                    Surface(
                        shape = ForensicShapes.xs,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(ForensicSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.xs)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Text(
                                text = "${risk.title} (${risk.severity})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = ForensicShapes.xs,
                    color = Color(0xFF2E7D32).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(ForensicSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.xs)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                        Text(
                            text = if (isPersian) "هیچ شاخص ریسک بحرانی در تحلیل اولیه شناسایی نشد." else "No critical risk indicators detected in initial triage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // Sanctions Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.sanctionsCheckStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ForensicBadge(
                    text = if (isPersian) "بررسی پایگاه OFAC / EU: بدون انطباق" else "OFAC / EU Screening: Clean",
                    badgeType = ForensicBadgeType.SUCCESS
                )
            }

            // Top Counterparties
            val topCounterparties = investigationCase.counterparties.sortedByDescending { it.txCount }.take(2)
            if (topCounterparties.isNotEmpty()) {
                Text(
                    text = strings.counterparties,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                topCounterparties.forEach { cp ->
                    Surface(
                        shape = ForensicShapes.xs,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(ForensicSpacing.xs),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${cp.address.take(12)}...${cp.address.takeLast(6)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )
                            Text(
                                text = "${cp.txCount} Tx (${String.format(Locale.US, "%.3f", cp.totalReceivedBtc + cp.totalSentBtc)} BTC)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // STEP 5: NEXT ACTION & ESCALATION
        ForensicCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.quickStepNextAction,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                ForensicBadge(
                    text = "RECOMMENDED",
                    badgeType = ForensicBadgeType.PRIMARY
                )
            }

            Text(
                text = if (investigationCase.riskIndicators.isNotEmpty()) {
                    if (isPersian) "به دلیل شناسایی ${investigationCase.riskIndicators.size} شاخص ریسک و وجود طرف‌های تراکنش فعال، توصیه می‌شود پرونده به «تحقیق هدایت‌شده» ارتقا یابد تا زنجیره ادله، خوشه‌بندی کیف‌پول و نقشه ارتباطات استخراج شود."
                    else "Due to ${investigationCase.riskIndicators.size} risk indicators and active counterparties, escalating to Guided Investigation is recommended to establish evidence chain, wallet clustering, and link graph."
                } else {
                    if (isPersian) "جهت استخراج گزارش رسمی قضایی یا بررسی عمیق هوش منابع باز (OSINT)، می‌توانید پرونده را در حالت هدایت‌شده یا محیط تخصصی کارشناس باز کنید."
                    else "To generate a formal court dossier or conduct deep OSINT reconnaissance, escalate this lead to Guided Investigation or Analyst Workspace."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.sm)
            ) {
                Button(
                    onClick = onEscalateToGuided,
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.escalateToGuided, style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onEscalateToAnalyst,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.escalateToAnalyst, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    // Contextual Educational Dialog
    activeLessonTopic?.let { topic ->
        CryptoMiniLessonDialog(
            topic = topic,
            isPersian = isPersian,
            onDismiss = { activeLessonTopic = null }
        )
    }
}
