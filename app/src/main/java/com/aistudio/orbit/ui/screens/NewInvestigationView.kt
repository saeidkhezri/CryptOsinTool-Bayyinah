package com.aistudio.orbit.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.forensics.AddressValidator
import com.aistudio.orbit.localization.AppLocalization
import com.aistudio.orbit.localization.toPersianDigits
import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.repository.AppLanguage
import com.aistudio.orbit.ui.components.Blockchain3dBadge
import com.aistudio.orbit.ui.components.Phosphor3dIconBadge
import com.aistudio.orbit.ui.InvestigationViewModel

enum class InvestigationDateRangeType(val labelFa: String, val labelEn: String) {
    ENTIRE_HISTORY("تمام تاریخچه بلاک‌چین (پیش‌فرض)", "Entire Available History (Default)"),
    CUSTOM_RANGE("بازه زمانی سفارشی", "Custom Date Range"),
    SPECIFIC_YEAR("سال مشخص", "Specific Year"),
    SPECIFIC_MONTH("ماه مشخص", "Specific Month")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewInvestigationView(
    viewModel: InvestigationViewModel,
    onInvestigationStarted: () -> Unit
) {
    val language by viewModel.settingsRepo.language.collectAsState()
    val isPersian = language == AppLanguage.PERSIAN
    val strings = AppLocalization.getStrings(language)
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingMsg by viewModel.loadingMessage.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()

    var referenceNumber by remember { mutableStateOf("") }
    var caseTitle by remember { mutableStateOf("") }
    var targetAddress by remember { mutableStateOf("") }
    var selectedNetwork by remember { mutableStateOf(BlockchainNetwork.BITCOIN) }
    var scopeDescription by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var queryLimit by remember { mutableIntStateOf(50) }
    var searchDepth by remember { mutableIntStateOf(1) }
    var selectedMode by remember { mutableStateOf(com.aistudio.orbit.model.ExperienceMode.GUIDED_INVESTIGATION) }

    // Collapsible Drawers & Custom Numeric Controls
    var isTimeframeExpanded by remember { mutableStateOf(false) }
    var isQueryConfigExpanded by remember { mutableStateOf(true) }
    var isCustomLimitMode by remember { mutableStateOf(false) }
    var customLimitText by remember { mutableStateOf("50") }
    var isCustomDepthMode by remember { mutableStateOf(false) }
    var customDepthText by remember { mutableStateOf("1") }

    // Date Range Selection State
    var dateRangeType by remember { mutableStateOf(InvestigationDateRangeType.ENTIRE_HISTORY) }
    var startDateText by remember { mutableStateOf("2020-01-01") }
    var endDateText by remember { mutableStateOf("2026-08-29") }
    var selectedYearText by remember { mutableStateOf("2024") }

    // Live address validation
    val validationResult = remember(targetAddress, selectedNetwork) {
        if (targetAddress.isNotBlank()) {
            AddressValidator.validate(targetAddress, selectedNetwork)
        } else null
    }

    val isEvmAddress = remember(targetAddress) {
        targetAddress.trim().startsWith("0x", ignoreCase = true) && targetAddress.trim().length == 42
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = if (maxWidth < 600.dp) 12.dp else 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Section Header
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Troubleshoot,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = strings.tabNewInvestigation,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isPersian) "ردیابی و تحلیل دفترکل بر پایه آدرس‌های عمومی بلاک‌چین"
                                else "Initialize forensic ledger tracing based on public blockchain addresses",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Blockchain Network Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.blockchainNetwork,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(BlockchainNetwork.entries.toTypedArray()) { net ->
                            val isSelected = selectedNetwork == net
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedNetwork = net },
                                leadingIcon = {
                                    Blockchain3dBadge(network = net, size = 22.dp)
                                },
                                label = { Text(net.displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                }
            }

            // EVM Ambiguity Resolution Banner
            if (isEvmAddress && selectedNetwork == BlockchainNetwork.BITCOIN) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Help, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                Text(
                                    text = if (isPersian) "تشخیص آدرس EVM (0x): انتخاب شبکه مقصد" else "EVM Address Detected: Select Chain Context",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Text(
                                text = if (isPersian) "آدرس‌های با پیشوند 0x می‌توانند در شبکه‌های متعددی استفاده شوند. شبکه مورد نظر برای این جرم‌یابی را تعیین نمایید:"
                                else "0x addresses can exist on multiple EVM-compatible ledgers. Choose target chain:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledTonalButton(onClick = { selectedNetwork = BlockchainNetwork.ETHEREUM }) {
                                    Text("Ethereum / USDT (ERC-20)", fontSize = 11.sp)
                                }
                                FilledTonalButton(onClick = { selectedNetwork = BlockchainNetwork.BNB_CHAIN }) {
                                    Text("BNB Chain / USDT (BEP-20)", fontSize = 11.sp)
                                }
                                FilledTonalButton(onClick = { selectedNetwork = BlockchainNetwork.POLYGON }) {
                                    Text("Polygon", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Target Address Input & Live Validation (wrapped in a beautiful Card to prevent stretching and form a beautiful box)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isPersian) "هدف و آدرس تحت تحقیق" else "Investigation Target Address",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = targetAddress,
                            onValueChange = { targetAddress = it },
                            label = { Text(strings.targetAddressLabel) },
                            placeholder = { Text(if (isPersian) "آدرس عمومی (مثلاً: 1A1zP1e... یا 0x... یا T...)" else "Public address (e.g. 1A1zP1... or 0x... or T...)") },
                            leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                            trailingIcon = {
                                if (targetAddress.isNotBlank()) {
                                    IconButton(onClick = { targetAddress = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            isError = validationResult != null && !validationResult.isValid,
                            supportingText = {
                                if (validationResult != null) {
                                    if (validationResult.isValid) {
                                        Text(
                                            text = "✓ ${AddressValidator.getAddressTypeLabel(validationResult.addressType, isPersian)}",
                                            color = Color(0xFF16A34A),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        Text(
                                            text = validationResult.errorReason ?: "Invalid format",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Responsive Quick-Fill Sample Chips
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isPersian) "آدرس‌های نمونه جهت تست سریع:" else "Sample Addresses for Fast Testing:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SuggestionChip(
                                    onClick = {
                                        targetAddress = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
                                        selectedNetwork = BlockchainNetwork.BITCOIN
                                    },
                                    label = { Text("BTC Genesis", fontSize = 11.sp) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        targetAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
                                        selectedNetwork = BlockchainNetwork.ETHEREUM
                                    },
                                    label = { Text("USDT ERC20", fontSize = 11.sp) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        targetAddress = "TR7NHqJEKQxGTCi8q8ZY4pL8otSzgjLj6t"
                                        selectedNetwork = BlockchainNetwork.TRON
                                    },
                                    label = { Text("USDT TRC20", fontSize = 11.sp) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        targetAddress = "0x55d398326f99059fF775485246999027B3197955"
                                        selectedNetwork = BlockchainNetwork.BNB_CHAIN
                                    },
                                    label = { Text("USDT BSC", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Investigation Date Range Configuration (Collapsible)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isTimeframeExpanded = !isTimeframeExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isPersian) "بازه زمانی جرم‌یابی و جستجو در دفترکل" else "Ledger Investigation Timeframe",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = if (isPersian) dateRangeType.labelFa else dateRangeType.labelEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = if (isTimeframeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AnimatedVisibility(visible = isTimeframeExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(InvestigationDateRangeType.entries.toTypedArray()) { rType ->
                                        val isSelected = dateRangeType == rType
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { dateRangeType = rType },
                                            label = { Text(if (isPersian) rType.labelFa else rType.labelEn, fontSize = 12.sp) }
                                        )
                                    }
                                }

                                if (dateRangeType == InvestigationDateRangeType.CUSTOM_RANGE) {
                                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                        val isNarrow = maxWidth < 380.dp
                                        if (isNarrow) {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = startDateText,
                                                    onValueChange = { startDateText = it },
                                                    label = { Text(if (isPersian) "تاریخ شروع (YYYY-MM-DD)" else "Start Date") },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                OutlinedTextField(
                                                    value = endDateText,
                                                    onValueChange = { endDateText = it },
                                                    label = { Text(if (isPersian) "تاریخ پایان (YYYY-MM-DD)" else "End Date") },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = startDateText,
                                                    onValueChange = { startDateText = it },
                                                    label = { Text(if (isPersian) "تاریخ شروع (YYYY-MM-DD)" else "Start Date") },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                OutlinedTextField(
                                                    value = endDateText,
                                                    onValueChange = { endDateText = it },
                                                    label = { Text(if (isPersian) "تاریخ پایان (YYYY-MM-DD)" else "End Date") },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                            }
                                        }
                                    }
                                } else if (dateRangeType == InvestigationDateRangeType.SPECIFIC_YEAR) {
                                    OutlinedTextField(
                                        value = selectedYearText,
                                        onValueChange = { selectedYearText = it },
                                        label = { Text(if (isPersian) "سال میلادی (مثلاً: 2024)" else "Target Year (e.g. 2024)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Case Title & Reference Number (Adaptive Layout)
            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isNarrow = maxWidth < 380.dp
                    if (isNarrow) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = referenceNumber,
                                onValueChange = { referenceNumber = it },
                                label = { Text(strings.caseRefLabel) },
                                placeholder = { Text("CASE-2026-001") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = caseTitle,
                                onValueChange = { caseTitle = it },
                                label = { Text(strings.caseTitleLabel) },
                                placeholder = { Text(if (isPersian) "عنوان پرونده" else "Case title") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = referenceNumber,
                                onValueChange = { referenceNumber = it },
                                label = { Text(strings.caseRefLabel) },
                                placeholder = { Text("CASE-2026-001") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = caseTitle,
                                onValueChange = { caseTitle = it },
                                label = { Text(strings.caseTitleLabel) },
                                placeholder = { Text(if (isPersian) "عنوان پرونده" else "Case title") },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Scope & Forensic Notes
            item {
                OutlinedTextField(
                    value = scopeDescription,
                    onValueChange = { scopeDescription = it },
                    label = { Text(strings.investigationScope) },
                    placeholder = { Text(if (isPersian) "توضیح مختصر هدف و دامنه بررسی" else "Brief investigation scope") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Query Limit and Depth Configuration (Collapsible & Custom Numeric)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isQueryConfigExpanded = !isQueryConfigExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isPersian) "تنظیمات واکشی تراکنش‌ها و لایه‌ها" else "Query Depth & Transaction Limits",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val currentLimitBadge = if (queryLimit >= 10000) (if (isPersian) "نامحدود" else "Unlimited") else "${queryLimit.toPersianDigits(isPersian)} تراکنش"
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = "$currentLimitBadge • لایه ${searchDepth.toPersianDigits(isPersian)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Icon(
                                    imageVector = if (isQueryConfigExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AnimatedVisibility(visible = isQueryConfigExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // Transaction Cap Selection
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val limitText = if (queryLimit >= 10000) {
                                        if (isPersian) "نامحدود (واکشی تمام صفحات دفترکل)" else "Unlimited (All Ledger Pages)"
                                    } else {
                                        "${queryLimit.toPersianDigits(isPersian)} تراکنش"
                                    }
                                    Text(
                                        text = "${if (isPersian) "سقف تعداد تراکنش: " else "Transaction Query Cap: "} $limitText",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(25, 50, 100, 250, 500, 1000, 10000).forEach { cap ->
                                            val isCapSelected = !isCustomLimitMode && queryLimit == cap
                                            FilterChip(
                                                selected = isCapSelected,
                                                onClick = {
                                                    isCustomLimitMode = false
                                                    queryLimit = cap
                                                },
                                                label = {
                                                    Text(
                                                        text = if (cap >= 10000) {
                                                            if (isPersian) "نامحدود" else "No Limit"
                                                        } else {
                                                            cap.toPersianDigits(isPersian)
                                                        },
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isCapSelected) FontWeight.Bold else FontWeight.Normal,
                                                        modifier = Modifier.padding(horizontal = 4.dp)
                                                    )
                                                }
                                            )
                                        }
                                        FilterChip(
                                            selected = isCustomLimitMode,
                                            onClick = {
                                                isCustomLimitMode = true
                                                customLimitText.toIntOrNull()?.let { queryLimit = it }
                                            },
                                            label = {
                                                Text(
                                                    text = if (isPersian) "سفارشی..." else "Custom...",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isCustomLimitMode) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        )
                                    }

                                    if (isCustomLimitMode) {
                                        OutlinedTextField(
                                            value = customLimitText,
                                            onValueChange = { input ->
                                                val clean = input.filter { it.isDigit() }
                                                customLimitText = clean
                                                clean.toIntOrNull()?.let { queryLimit = it.coerceIn(1, 100000) }
                                            },
                                            label = { Text(if (isPersian) "تعداد دقیق تراکنش مدنظر" else "Exact Transaction Count") },
                                            placeholder = { Text("مثلاً: 350") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                // Investigation Depth / Layers Selection
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "${if (isPersian) "عمق لایه‌ها و گام‌های جستجو: " else "Investigation Layer Depth: "} لایه ${searchDepth.toPersianDigits(isPersian)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            1 to (if (isPersian) "لایه ۱ (مستقیم / Direct)" else "Layer 1 (Direct)"),
                                            2 to (if (isPersian) "لایه ۲ (دو گام / 2-Hops)" else "Layer 2 (2-Hops)"),
                                            3 to (if (isPersian) "لایه ۳ (سه گام / 3-Hops)" else "Layer 3 (3-Hops)")
                                        ).forEach { (depthVal, label) ->
                                            val isDepthSelected = !isCustomDepthMode && searchDepth == depthVal
                                            FilterChip(
                                                selected = isDepthSelected,
                                                onClick = {
                                                    isCustomDepthMode = false
                                                    searchDepth = depthVal
                                                },
                                                label = {
                                                    Text(
                                                        text = label,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isDepthSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            )
                                        }
                                        FilterChip(
                                            selected = isCustomDepthMode,
                                            onClick = {
                                                isCustomDepthMode = true
                                                customDepthText.toIntOrNull()?.let { searchDepth = it }
                                            },
                                            label = {
                                                Text(
                                                    text = if (isPersian) "لایه سفارشی..." else "Custom Layer...",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isCustomDepthMode) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        )
                                    }

                                    if (isCustomDepthMode) {
                                        OutlinedTextField(
                                            value = customDepthText,
                                            onValueChange = { input ->
                                                val clean = input.filter { it.isDigit() }
                                                customDepthText = clean
                                                clean.toIntOrNull()?.let { searchDepth = it.coerceIn(1, 10) }
                                            },
                                            label = { Text(if (isPersian) "عدد لایه بررسی (مثلاً ۴)" else "Layer Depth Number") },
                                            placeholder = { Text("مثلاً: 4") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Error message banner
            if (errorMsg != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = errorMsg ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Experience Mode Selector Section (resolves the 3 modes vs 2 buttons layout inconsistency beautifully)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isPersian) "حالت اجرای تحقیق و ردیابی مالی" else "Investigation Experience Mode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val modesList = listOf(
                                Triple(
                                    com.aistudio.orbit.model.ExperienceMode.QUICK_CHECK,
                                    if (isPersian) "بررسی سریع (Quick Check)" else "Quick Check",
                                    if (isPersian) "تحلیل سریع ریسک و انتساب بدون تشکیل پرونده سنگین" else "Fast risk and attribution summary for a single lead"
                                ),
                                Triple(
                                    com.aistudio.orbit.model.ExperienceMode.GUIDED_INVESTIGATION,
                                    if (isPersian) "تحقیق هدایت‌شده (Guided)" else "Guided Investigation",
                                    if (isPersian) "راهنمایی گام‌به‌گام از تایید ورودی تا ثبت ادله و گزارش نهایی" else "Step-by-step guided workflow with structured checkpoints"
                                ),
                                Triple(
                                    com.aistudio.orbit.model.ExperienceMode.ANALYST_WORKSPACE,
                                    if (isPersian) "محیط جامع کارشناس (Workspace)" else "Analyst Workspace",
                                    if (isPersian) "دسترسی آزاد به تمام پکیج‌های فارنزیک، گراف تعاملی و ممیزی ادله" else "Unrestricted, non-linear access to full forensic suite and tools"
                                )
                            )

                            modesList.forEach { (mode, title, desc) ->
                                val isSelected = selectedMode == mode
                                Surface(
                                    onClick = { selectedMode = mode },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.5.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedMode = mode }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = when (mode) {
                                                com.aistudio.orbit.model.ExperienceMode.QUICK_CHECK -> Icons.Default.Bolt
                                                com.aistudio.orbit.model.ExperienceMode.GUIDED_INVESTIGATION -> Icons.Default.Explore
                                                com.aistudio.orbit.model.ExperienceMode.ANALYST_WORKSPACE -> Icons.Default.Layers
                                            },
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Button(
                    onClick = {
                        viewModel.setExperienceMode(selectedMode)
                        viewModel.startNewInvestigation(
                            referenceNumber = referenceNumber,
                            caseTitle = caseTitle,
                            targetAddress = targetAddress,
                            network = selectedNetwork,
                            scopeDescription = scopeDescription,
                            notes = notes,
                            tags = listOf("STAGE-2", selectedNetwork.name),
                            searchDepth = searchDepth,
                            queryLimit = queryLimit
                        )
                        onInvestigationStarted()
                    },
                    enabled = !isLoading && (validationResult?.isValid == true || targetAddress.isNotBlank()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = loadingMsg, fontSize = 14.sp)
                    } else {
                        val startText = when (selectedMode) {
                            com.aistudio.orbit.model.ExperienceMode.QUICK_CHECK -> if (isPersian) "شروع بررسی سریع آدرس" else "Initialize Quick Check"
                            com.aistudio.orbit.model.ExperienceMode.GUIDED_INVESTIGATION -> if (isPersian) "شروع تحقیق هدایت‌شده پرونده" else "Initialize Guided Investigation"
                            com.aistudio.orbit.model.ExperienceMode.ANALYST_WORKSPACE -> if (isPersian) "ورود به محیط جامع کارشناسی" else "Initialize Analyst Workspace"
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = startText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
}
