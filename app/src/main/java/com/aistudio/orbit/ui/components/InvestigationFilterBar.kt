package com.aistudio.orbit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.orbit.forensics.filter.FilterDirection
import com.aistudio.orbit.forensics.filter.InvestigationFilterState
import com.aistudio.orbit.model.ConfidenceLevel
import com.aistudio.orbit.model.EntityClassificationType
import com.aistudio.orbit.repository.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestigationFilterBar(
    filterState: InvestigationFilterState,
    onFilterChange: (InvestigationFilterState) -> Unit,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    showEntityFilters: Boolean = true,
    showDirectionFilters: Boolean = true
) {
    val isFa = language == AppLanguage.PERSIAN
    var showFilterDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (isFa) "جستجو و فیلتر" else "Search & Filter",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            // Row 1: Search field + Filter Dialog button + Reset Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = filterState.searchQuery,
                    onValueChange = { onFilterChange(filterState.copy(searchQuery = it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = if (isFa) "جستجوی آدرس، هش تراکنش یا برچسب..." else "Filter by address, tx hash, or tag...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (filterState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onFilterChange(filterState.copy(searchQuery = "")) }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = RoundedCornerShape(8.dp)
                )

                // Advanced Filter button
                OutlinedButton(
                    onClick = { showFilterDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter", modifier = Modifier.size(18.dp))
                    if (filterState.activeFilterCount() > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text(
                                text = if (isFa) "${filterState.activeFilterCount()}".replace("1", "۱").replace("2", "۲").replace("3", "۳").replace("4", "۴").replace("5", "۵") else "${filterState.activeFilterCount()}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Reset button
                if (filterState.hasActiveFilters()) {
                    IconButton(
                        onClick = { onFilterChange(InvestigationFilterState()) }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset Filters",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Row 2: Horizontal Quick Filter Chips (Direction & Key Categories)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showDirectionFilters) {
                    FilterChip(
                        selected = filterState.direction == FilterDirection.ALL,
                        onClick = { onFilterChange(filterState.copy(direction = FilterDirection.ALL)) },
                        label = { Text(if (isFa) "همه جریان‌ها" else "All Flows", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = filterState.direction == FilterDirection.INCOMING_ONLY,
                        onClick = { onFilterChange(filterState.copy(direction = FilterDirection.INCOMING_ONLY)) },
                        label = { Text(if (isFa) "فقط ورودی (+)" else "Incoming (+)", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = filterState.direction == FilterDirection.OUTGOING_ONLY,
                        onClick = { onFilterChange(filterState.copy(direction = FilterDirection.OUTGOING_ONLY)) },
                        label = { Text(if (isFa) "فقط خروجی (-)" else "Outgoing (-)", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                if (showEntityFilters) {
                    // Suspicious toggle
                    FilterChip(
                        selected = filterState.onlySuspiciousOrRisky,
                        onClick = { onFilterChange(filterState.copy(onlySuspiciousOrRisky = !filterState.onlySuspiciousOrRisky)) },
                        label = { Text(if (isFa) "⚠️ فقط مشکوک/پرخطر" else "⚠️ High Risk Only", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )

                    // Exchange Filter
                    val isExchangeSelected = filterState.selectedEntityTypes.contains(EntityClassificationType.EXCHANGE_HOT_WALLET)
                    FilterChip(
                        selected = isExchangeSelected,
                        onClick = {
                            val newSet = if (isExchangeSelected) {
                                filterState.selectedEntityTypes - EntityClassificationType.EXCHANGE_HOT_WALLET
                            } else {
                                filterState.selectedEntityTypes + EntityClassificationType.EXCHANGE_HOT_WALLET
                            }
                            onFilterChange(filterState.copy(selectedEntityTypes = newSet))
                        },
                        label = { Text(if (isFa) "صرافی‌ها" else "Exchanges", style = MaterialTheme.typography.labelSmall) }
                    )

                    // Mixer Filter
                    val isMixerSelected = filterState.selectedEntityTypes.contains(EntityClassificationType.MIXER_TUMBLER)
                    FilterChip(
                        selected = isMixerSelected,
                        onClick = {
                            val newSet = if (isMixerSelected) {
                                filterState.selectedEntityTypes - EntityClassificationType.MIXER_TUMBLER
                            } else {
                                filterState.selectedEntityTypes + EntityClassificationType.MIXER_TUMBLER
                            }
                            onFilterChange(filterState.copy(selectedEntityTypes = newSet))
                        },
                        label = { Text(if (isFa) "میکسر / کوین‌جوین" else "Mixers / Privacy", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    }

    // Advanced Filter Modal Dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = {
                Text(
                    text = if (isFa) "فیلترهای پیشرفته و قلمرو تحلیل" else "Advanced Forensic Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Minimum Value Slider
                    Text(
                        text = if (isFa) "حداقل مبلغ تراکنش: ${String.format("%.4f", filterState.minAmountBtc)} BTC" else "Min Amount: ${String.format("%.4f", filterState.minAmountBtc)} BTC",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = filterState.minAmountBtc.toFloat(),
                        onValueChange = { onFilterChange(filterState.copy(minAmountBtc = it.toDouble())) },
                        valueRange = 0f..2.0f,
                        steps = 20
                    )

                    // Minimum Confidence Level
                    Text(
                        text = if (isFa) "حداقل سطح قطعیت ادله:" else "Min Confidence Level:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = filterState.minConfidence == null,
                            onClick = { onFilterChange(filterState.copy(minConfidence = null)) },
                            label = { Text(if (isFa) "همه" else "All", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = filterState.minConfidence == ConfidenceLevel.MEDIUM_CONFIDENCE,
                            onClick = { onFilterChange(filterState.copy(minConfidence = ConfidenceLevel.MEDIUM_CONFIDENCE)) },
                            label = { Text(if (isFa) "متوسط (≥۵۰٪)" else "Medium (≥50%)", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = filterState.minConfidence == ConfidenceLevel.HIGH_CONFIDENCE,
                            onClick = { onFilterChange(filterState.copy(minConfidence = ConfidenceLevel.HIGH_CONFIDENCE)) },
                            label = { Text(if (isFa) "بالا (≥۸۵٪)" else "High (≥85%)", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFilterDialog = false }) {
                    Text(if (isFa) "اعمال فیلتر" else "Apply Filters")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onFilterChange(InvestigationFilterState())
                        showFilterDialog = false
                    }
                ) {
                    Text(if (isFa) "پاکسازی همه" else "Clear All", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}
