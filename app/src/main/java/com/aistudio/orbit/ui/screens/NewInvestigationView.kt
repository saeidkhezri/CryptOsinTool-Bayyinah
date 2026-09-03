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

            // Target Address Input & Live Validation
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            text = if (isPersian) "آدرس‌های نمونه جهت تست:" else "Sample Addresses for Verification:",
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

            // Investigation Date Range Configuration
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isPersian) "بازه زمانی جرم‌یابی و جستجو در دفترکل" else "Ledger Investigation Timeframe",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

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

            // Query Limit and Depth Configuration
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isPersian) "تنظیمات واکشی تراکنش‌ها و محدودیت‌ها" else "Transaction Query Depth & Limits",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val limitText = if (queryLimit >= 10000) {
                                if (isPersian) "نامحدود" else "No Limit"
                            } else {
                                queryLimit.toPersianDigits(isPersian)
                            }
                            Text(
                                text = "${if (isPersian) "سقف تعداد تراکنش اولیه: " else "Initial Tx Query Cap: "} $limitText",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(25, 50, 100, 200, 10000).forEach { cap ->
                                    FilterChip(
                                        selected = queryLimit == cap,
                                        onClick = { queryLimit = cap },
                                        label = {
                                            Text(
                                                text = if (cap >= 10000) {
                                                    if (isPersian) "نامحدود" else "No Limit"
                                                } else {
                                                    cap.toPersianDigits(isPersian)
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                        }
                                    )
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

            // Action Buttons
            item {
                Button(
                    onClick = {
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
                        .height(52.dp),
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
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPersian) "شروع استعلام و جرم‌یابی دفترکل" else "Begin Ledger Forensic Investigation",
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
