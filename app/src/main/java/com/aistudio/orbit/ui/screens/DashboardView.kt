@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
package com.aistudio.orbit.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.R
import com.aistudio.orbit.forensics.TemporalUtils
import com.aistudio.orbit.localization.AppLocalization
import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.repository.AppLanguage
import com.aistudio.orbit.ui.InvestigationViewModel
import com.aistudio.orbit.ui.components.WindowWidthSizeClass
import com.aistudio.orbit.ui.components.designsystem.*
import com.aistudio.orbit.ui.theme.*

@Composable
fun DashboardView(
    viewModel: InvestigationViewModel,
    onNavigateToNew: () -> Unit,
    onNavigateToCase: (InvestigationCase) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToOsint: () -> Unit
) {
    val language by viewModel.settingsRepo.language.collectAsState()
    val isPersian = language == AppLanguage.PERSIAN
    val strings = AppLocalization.getStrings(language)
    val cases by viewModel.investigationRepo.cases.collectAsState(initial = emptyList())
    val activeCase by viewModel.activeCase.collectAsState()
    val providerConfigs by viewModel.providerManager.providerConfigs.collectAsState()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val widthClass = when {
            maxWidth < 600.dp -> WindowWidthSizeClass.COMPACT
            maxWidth < 840.dp -> WindowWidthSizeClass.MEDIUM
            else -> WindowWidthSizeClass.EXPANDED
        }

        val horizontalPadding = when (widthClass) {
            WindowWidthSizeClass.COMPACT -> ForensicSpacing.base
            WindowWidthSizeClass.MEDIUM -> ForensicSpacing.xl
            WindowWidthSizeClass.EXPANDED -> ForensicSpacing.xxl
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 1100.dp)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = ForensicSpacing.base),
            verticalArrangement = Arrangement.spacedBy(ForensicSpacing.base)
        ) {
            // Main Dashboard Identity Hero Card
            item {
                ForensicCard(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(ForensicSpacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.md),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Black.copy(alpha = 0.35f))
                                        .border(
                                            width = 1.5.dp,
                                            brush = Brush.linearGradient(
                                                listOf(Color(0xFFFFD54F), Color(0xFF00E5FF), Color(0xFF10B981))
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.app_logo),
                                        contentDescription = "Application Logo",
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }

                                Column {
                                    Text(
                                        text = if (isPersian) "بیِّنة" else "BAYYINAH",
                                        style = TextStyle(
                                            brush = Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFFFFD54F),
                                                    Color(0xFF00E5FF),
                                                    Color(0xFF38BDF8)
                                                )
                                            ),
                                            fontSize = if (isPersian) 30.sp else 26.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = if (isPersian) 1.2.sp else 2.5.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = strings.appSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Enterprise Status Tag
                            Surface(
                                shape = ForensicShapes.pill,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color(0xFFFFD54F).copy(alpha = 0.6f), Color(0xFF00E5FF).copy(alpha = 0.6f))
                                    )
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = ForensicSpacing.md, vertical = ForensicSpacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.xs)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Text(
                                        text = if (isPersian) "مرکز کنترل فعال" else "COMMAND CENTER ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFFD54F),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Quick Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.sm)
                        ) {
                            Button(
                                onClick = onNavigateToNew,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = ForensicShapes.md,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = ForensicTouchTarget.minSize)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(ForensicSpacing.xs))
                                Text(
                                    text = strings.tabNewInvestigation,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }

                            FilledTonalButton(
                                onClick = onNavigateToOsint,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = ForensicShapes.md,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = ForensicTouchTarget.minSize)
                            ) {
                                Icon(Icons.Default.TravelExplore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(ForensicSpacing.xs))
                                Text(
                                    text = if (isPersian) "سامانه OSINT" else "OSINT Hub",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                }
            }

            // PROVIDER STATUS GATE (Dynamic Gateway Registry Statuses)
            item {
                ForensicSectionHeader(
                    title = if (isPersian) "درگاه‌ها و ارائه‌دهندگان سرویس" else "API Service Providers & Gateways",
                    icon = Icons.Default.CloudQueue
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.xs)
                ) {
                    val displayProviders = remember(providerConfigs) {
                        providerConfigs.filter { 
                            it.id in listOf("mempool_space_btc", "etherscan_eth", "trongrid_tron", "cryptoapis_multi") 
                        }
                    }
                    
                    displayProviders.forEach { config ->
                        val color = when (config.status) {
                            com.aistudio.orbit.model.ProviderStatus.HEALTHY,
                            com.aistudio.orbit.model.ProviderStatus.CONFIGURED -> Color(0xFF10B981)
                            com.aistudio.orbit.model.ProviderStatus.TESTING -> Color(0xFF38BDF8)
                            com.aistudio.orbit.model.ProviderStatus.DISABLED -> MaterialTheme.colorScheme.outline
                            com.aistudio.orbit.model.ProviderStatus.UNVERIFIED -> Color(0xFFF59E0B)
                            com.aistudio.orbit.model.ProviderStatus.FAILED -> MaterialTheme.colorScheme.error
                            else -> Color(0xFFFFD54F)
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Text(
                                    text = if (isPersian) {
                                        when (config.id) {
                                            "mempool_space_btc" -> "بیت‌کوین (Mempool)"
                                            "etherscan_eth" -> "اتریوم (Etherscan)"
                                            "trongrid_tron" -> "ترون (TronGrid)"
                                            else -> "مولتی‌چین (CryptoAPIs)"
                                        }
                                    } else {
                                        config.name.substringBefore(" (")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.5.sp,
                                    maxLines = 2,
                                    softWrap = true,
                                    lineHeight = 10.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.heightIn(min = 20.dp)
                                )
                                Text(
                                    text = if (config.status == com.aistudio.orbit.model.ProviderStatus.DISABLED) {
                                        if (isPersian) "غیرفعال" else "DISABLED"
                                    } else if (config.status == com.aistudio.orbit.model.ProviderStatus.NOT_CONFIGURED) {
                                        if (isPersian) "تنظیم‌نشده" else "NOT CONFIG"
                                    } else {
                                        "${config.lastResponseTimeMs.coerceAtLeast(12L)}ms"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = color.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Key Forensic Metrics Grid
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(ForensicSpacing.md),
                    maxItemsInEachRow = 3
                ) {
                    ForensicMetricCard(
                        label = if (isPersian) "پرونده‌های فعال و ثبت‌شده" else "Active Cases",
                        value = TemporalUtils.formatNumber(cases.size, isPersian),
                        icon = Icons.Default.Folder,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.widthIn(min = 150.dp).weight(1f)
                    )
                    ForensicMetricCard(
                        label = if (isPersian) "یافته‌های مهم و مشکوک" else "Suspicious Alerts",
                        value = TemporalUtils.formatNumber(cases.sumOf { it.riskIndicators.size }, isPersian),
                        icon = Icons.Default.Shield,
                        accentColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.widthIn(min = 150.dp).weight(1f)
                    )
                    ForensicMetricCard(
                        label = if (isPersian) "ادله ممهور شده" else "Sealed Evidence",
                        value = TemporalUtils.formatNumber(cases.sumOf { it.evidenceLog.size }, isPersian),
                        icon = Icons.Default.Policy,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.widthIn(min = 150.dp).weight(1f)
                    )
                }
            }

            // Active Case Guided Action & Roadmap Banner
            activeCase?.let { case ->
                item {
                    ForensicSectionHeader(
                        title = if (isPersian) "پرونده فعال تحت ردیابی زنده" else "Active Case LIVE Stream",
                        icon = Icons.Default.HourglassTop
                    )
                }
                item {
                    ForensicCard(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        borderColor = MaterialTheme.colorScheme.primary
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = case.caseName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${if (isPersian) "کلاسه: " else "Ref: "}${case.referenceNumber.ifBlank { "N/A" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Surface(
                                    shape = ForensicShapes.xs,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = case.network.symbol,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = ForensicSpacing.sm, vertical = ForensicSpacing.xxs),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            ForensicAddressText(
                                address = case.targetAddress,
                                label = if (isPersian) "آدرس سرنخ تحت ردیابی:" else "Target Address:",
                                isPersian = isPersian
                            )

                            // Roadmap Status & Current Step
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            ) {
                                val stageInfo = remember(activeCase) { getCaseCompletedStageInfo(case, isPersian) }
                                val activeNextAction = remember(activeCase) {
                                    val currentStageEnum = InvestigationStage.values().firstOrNull { it.id == case.activeStageId } ?: InvestigationStage.BLOCKCHAIN_DISCOVERY
                                    calculateNextBestAction(currentStageEnum, case)
                                }

                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isPersian) "مرحله فعلی نقشه راه:" else "Current Roadmap Stage:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = stageInfo.second,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isPersian) "پیشنهاد اقدام بعدی:" else "Recommended Next Best Action:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = if (isPersian) activeNextAction.proposalFa else activeNextAction.proposalEn,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD54F), // Gold Accent
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { onNavigateToCase(case) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPersian) "ورود به نقشه راه هدایت‌شده پرونده" else "Enter Guided Case Roadmap",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Recent Cases Section
            item {
                ForensicSectionHeader(
                    title = if (isPersian) "تاریخچه آخرین پرونده‌های ردیابی‌شده" else "Recent Active Subpoenas & Cases",
                    subtitle = if (isPersian) "نمایش میزان پیشرفت مراحل و زنجیره ادله ممهور" else "Analytical stage progress and sealed digital custody",
                    icon = Icons.Default.History
                )
            }

            if (cases.isEmpty()) {
                item {
                    ForensicEmptyState(
                        message = strings.noCasesFound,
                        icon = Icons.Default.SearchOff,
                        actionButton = {
                            OutlinedButton(
                                onClick = onNavigateToNew,
                                shape = ForensicShapes.md
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(ForensicSpacing.xs))
                                Text(strings.tabNewInvestigation)
                            }
                        }
                    )
                }
            } else {
                items(cases.take(6)) { c ->
                    CaseCommandCenterItem(
                        c = c,
                        isPersian = isPersian,
                        onClick = { onNavigateToCase(c) }
                    )
                }
            }

            // Legal & Methodological Disclaimer Notice
            item {
                ForensicCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.md),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = strings.disclaimerText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CaseCommandCenterItem(
    c: InvestigationCase,
    isPersian: Boolean,
    onClick: () -> Unit
) {
    ForensicCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ForensicSpacing.xxs)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.xs)) {
                    Text(
                        text = c.caseName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = ForensicShapes.xs,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = c.network.symbol,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 9.sp
                        )
                    }
                }
                val formattedAddress = remember(c.targetAddress) {
                    com.aistudio.orbit.util.ForensicBidiUtils.formatLtrTechnicalString(c.targetAddress)
                }
                Text(
                    text = formattedAddress,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        textDirection = TextDirection.Ltr
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Interactive stage progress bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val stageInfo = remember(c) { getCaseCompletedStageInfo(c, isPersian) }
                    LinearProgressIndicator(
                        progress = { stageInfo.first },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .width(80.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = stageInfo.second,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.width(ForensicSpacing.sm))
            Surface(
                shape = ForensicShapes.sm,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "${TemporalUtils.formatNumber(c.evidenceLog.size, isPersian)} ${if (isPersian) "سند ممهور" else "Seals"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = ForensicSpacing.sm, vertical = ForensicSpacing.xs),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun getCaseCompletedStageInfo(case: InvestigationCase, isPersian: Boolean): Pair<Float, String> {
    val totalStages = 11
    val currentStageId = case.activeStageId.coerceIn(1, 11)
    
    val progress = currentStageId.toFloat() / totalStages.toFloat()
    val stageName = when (currentStageId) {
        1 -> if (isPersian) "شروع تحقیق" else "Start Investigation"
        2 -> if (isPersian) "سرنخ اولیه" else "Initial Lead"
        3 -> if (isPersian) "بررسی بلاکچین" else "Blockchain Discovery"
        4 -> if (isPersian) "بررسی تراکنش‌ها" else "Transactions Ledger"
        5 -> if (isPersian) "بررسی ارتباط‌ها" else "Related Addresses"
        6 -> if (isPersian) "بررسی الگوها" else "Pattern Analysis"
        7 -> if (isPersian) "بررسی OSINT" else "OSINT Review"
        8 -> if (isPersian) "بررسی ریسک" else "Risk Review"
        9 -> if (isPersian) "مرور شواهد" else "Evidence Review"
        10 -> if (isPersian) "نتیجه‌گیری" else "Conclusion"
        else -> if (isPersian) "گزارش پرونده" else "Report"
    }
    
    val displayText = if (isPersian) "مرحله $currentStageId از $totalStages ($stageName)" else "Stage $currentStageId of $totalStages ($stageName)"
    return Pair(progress, displayText)
}
