package com.aistudio.orbit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.forensics.TemporalUtils
import com.aistudio.orbit.localization.AppLocalization
import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.repository.AppLanguage
import com.aistudio.orbit.ui.InvestigationViewModel
import com.aistudio.orbit.ui.components.designsystem.*
import com.aistudio.orbit.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CasesHistoryView(
    viewModel: InvestigationViewModel,
    onSelectCase: (InvestigationCase) -> Unit
) {
    val language by viewModel.settingsRepo.language.collectAsState()
    val isPersian = language == AppLanguage.PERSIAN
    val strings = AppLocalization.getStrings(language)
    val cases by viewModel.investigationRepo.cases.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var caseToDelete by remember { mutableStateOf<InvestigationCase?>(null) }

    val filteredCases = remember(cases, searchQuery) {
        if (searchQuery.isBlank()) cases
        else cases.filter {
            it.caseName.contains(searchQuery, ignoreCase = true) ||
            it.referenceNumber.contains(searchQuery, ignoreCase = true) ||
            it.targetAddress.contains(searchQuery, ignoreCase = true)
        }
    }

    val isLoading by viewModel.isLoading.collectAsState()

    val casesState by remember(isLoading, cases, filteredCases) {
        derivedStateOf {
            when {
                isLoading -> ForensicUiState.Loading
                cases.isEmpty() -> ForensicUiState.Empty
                filteredCases.isEmpty() -> ForensicUiState.Empty
                else -> ForensicUiState.Success(filteredCases)
            }
        }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = strings.tabCases,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isPersian) "جستجو بر اساس کلاسه، عنوان، یا آدرس..." else "Search cases, reference, or address...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                ForensicUiStateBoundary(
                    state = casesState,
                    emptyMessage = strings.noCasesFound,
                    successContent = { /* No-op here, items handled below */ }
                )
            }

            if (casesState is ForensicUiState.Success) {
                items(filteredCases) { c ->
                    val formattedAddress = remember(c.targetAddress) {
                        com.aistudio.orbit.util.ForensicBidiUtils.formatLtrTechnicalString(c.targetAddress)
                    }
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCase(c) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = c.caseName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${if (isPersian) "کلاسه: " else "Ref: "}${c.referenceNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { caseToDelete = c },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = strings.deleteCase, tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            Text(
                                text = formattedAddress,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    textDirection = TextDirection.Ltr
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${if (isPersian) "موجودی: " else "Bal: "}${TemporalUtils.formatCryptoAmount(c.balanceSat, c.network, isPersian)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${TemporalUtils.formatNumber(c.transactions.size, isPersian)} ${if (isPersian) "تراکنش" else "txs"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = TemporalUtils.formatDateTime(c.createdTimestamp / 1000, isPersian, true),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog before deleting an evidence case
    caseToDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { caseToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(if (isPersian) "حذف پرونده جرم‌یابی" else "Delete Investigation Case", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isPersian) "آیا از حذف پرونده «${c.caseName}» اطمینان دارید؟ داده‌های تحلیل‌شده محلی پاک خواهند شد."
                    else "Are you sure you want to delete case '${c.caseName}'? All local forensic artifacts will be permanently purged."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.investigationRepo.deleteCase(c.caseId)
                            caseToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isPersian) "حذف قطعی" else "Delete", textAlign = TextAlign.Center)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { caseToDelete = null }) {
                    Text(strings.close, textAlign = TextAlign.Center)
                }
            }
        )
    }
}
