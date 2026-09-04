package com.aistudio.orbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.orbit.forensics.AddressValidator
import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.InvestigationCase

enum class SearchItemType {
    CASE,
    ADDRESS,
    TRANSACTION_HASH
}

data class SearchResultItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: SearchItemType,
    val network: BlockchainNetwork? = null,
    val rawPayload: Any? = null
)

@Composable
fun GlobalSearchComponent(
    cases: List<InvestigationCase>,
    onSelectCase: (InvestigationCase) -> Unit,
    onStartNewInvestigation: (address: String, network: BlockchainNetwork) -> Unit,
    onDismiss: () -> Unit,
    isPersian: Boolean = true
) {
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Categorize Query & Matches
    val searchResults by remember(query, cases) {
        derivedStateOf {
            val q = query.trim()
            if (q.isBlank()) return@derivedStateOf emptyList<SearchResultItem>()

            val results = mutableListOf<SearchResultItem>()

            // 1. Search matching local cases
            val matchedCases = cases.filter {
                it.title.contains(q, ignoreCase = true) ||
                it.targetAddress.contains(q, ignoreCase = true) ||
                it.caseReferenceNumber.contains(q, ignoreCase = true) ||
                it.description.contains(q, ignoreCase = true)
            }
            matchedCases.forEach { c ->
                results.add(
                    SearchResultItem(
                        id = "case_${c.id}",
                        title = c.title,
                        subtitle = "${c.blockchainNetwork.displayName} • ${c.targetAddress}",
                        type = SearchItemType.CASE,
                        network = c.blockchainNetwork,
                        rawPayload = c
                    )
                )
            }

            // 2. Validate as Direct Blockchain Address
            val btcVal = AddressValidator.validate(q, BlockchainNetwork.BITCOIN)
            val ethVal = AddressValidator.validate(q, BlockchainNetwork.ETHEREUM)
            val tronVal = AddressValidator.validate(q, BlockchainNetwork.TRON)
            val bscVal = AddressValidator.validate(q, BlockchainNetwork.BNB_CHAIN)

            if (btcVal.isValid) {
                results.add(
                    SearchResultItem(
                        id = "direct_btc_$q",
                        title = if (isPersian) "آدرس معتبر بیت‌کوین (${btcVal.addressType.name})" else "Valid Bitcoin Address (${btcVal.addressType.name})",
                        subtitle = q,
                        type = SearchItemType.ADDRESS,
                        network = BlockchainNetwork.BITCOIN,
                        rawPayload = q
                    )
                )
            }
            if (ethVal.isValid) {
                results.add(
                    SearchResultItem(
                        id = "direct_eth_$q",
                        title = if (isPersian) "آدرس معتبر اتریوم و شبکه‌های EVM" else "Valid Ethereum / EVM Address",
                        subtitle = q,
                        type = SearchItemType.ADDRESS,
                        network = BlockchainNetwork.ETHEREUM,
                        rawPayload = q
                    )
                )
            }
            if (tronVal.isValid) {
                results.add(
                    SearchResultItem(
                        id = "direct_tron_$q",
                        title = if (isPersian) "آدرس معتبر شبکه ترون TRC-20" else "Valid TRON Address",
                        subtitle = q,
                        type = SearchItemType.ADDRESS,
                        network = BlockchainNetwork.TRON,
                        rawPayload = q
                    )
                )
            }

            // 3. Detect 64-hex Transaction Hash
            val isTxid = q.matches(Regex("^(0x)?[0-9a-fA-F]{64}$"))
            if (isTxid) {
                results.add(
                    SearchResultItem(
                        id = "txid_$q",
                        title = if (isPersian) "شناسه هش تراکنش بلاک‌چین" else "Blockchain Transaction Hash (TXID)",
                        subtitle = q,
                        type = SearchItemType.TRANSACTION_HASH,
                        rawPayload = q
                    )
                )
            }

            results
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .widthIn(max = 680.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPersian) "جستجوی جامع در سامانه و داده‌های بلاکچین" else "Global Forensic Search",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Search Bar Input
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            text = if (isPersian) "آدرس بلاک‌چین، هش تراکنش، نام یا شماره پرونده..." else "Address, TXID, or investigation name...",
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )

                // Results Counter & Category Header
                if (query.isNotBlank()) {
                    Text(
                        text = if (isPersian) "نتایج یافت شده: ${searchResults.size}" else "Found matches: ${searchResults.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Search Results List
                if (searchResults.isEmpty() && query.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = if (isPersian) "موردی با این مشخصات در پرونده‌ها یا قالب‌های آدرس یافت نشد."
                                else "No matching cases or recognized blockchain identifiers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults, key = { it.id }) { item ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        when (item.type) {
                                            SearchItemType.CASE -> {
                                                (item.rawPayload as? InvestigationCase)?.let {
                                                    onSelectCase(it)
                                                    onDismiss()
                                                }
                                            }
                                            SearchItemType.ADDRESS -> {
                                                val net = item.network ?: BlockchainNetwork.BITCOIN
                                                onStartNewInvestigation(item.subtitle, net)
                                                onDismiss()
                                            }
                                            SearchItemType.TRANSACTION_HASH -> {
                                                // Trigger lookup by txid
                                                onStartNewInvestigation(item.subtitle, BlockchainNetwork.BITCOIN)
                                                onDismiss()
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = when (item.type) {
                                            SearchItemType.CASE -> Icons.Default.Folder
                                            SearchItemType.ADDRESS -> Icons.Default.AccountBalanceWallet
                                            SearchItemType.TRANSACTION_HASH -> Icons.Default.ReceiptLong
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = if (item.type != SearchItemType.CASE) FontFamily.Monospace else null,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Action Pill
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = when (item.type) {
                                                SearchItemType.CASE -> if (isPersian) "مشاهده" else "Open"
                                                SearchItemType.ADDRESS -> if (isPersian) "شروع تحلیل" else "Investigate"
                                                SearchItemType.TRANSACTION_HASH -> if (isPersian) "ردیابی" else "Trace"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontWeight = FontWeight.Bold
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
