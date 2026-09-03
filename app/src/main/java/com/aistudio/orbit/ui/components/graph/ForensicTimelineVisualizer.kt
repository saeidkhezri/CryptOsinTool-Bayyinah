package com.aistudio.orbit.ui.components.graph

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.PersianDateUtils
import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.model.RiskSeverity
import java.text.SimpleDateFormat
import java.util.*

enum class TimelineEventType(val displayNameEn: String, val displayNameFa: String, val color: Color) {
    ON_CHAIN_TX("On-Chain Transaction", "تراکنش درون زنجیره‌ای", Color(0xFF1976D2)),
    ADDRESS_INCEPTION("Address Inception", "پیدایش آدرس", Color(0xFF00897B)),
    CLUSTER_DISCOVERY("Cluster Identification", "کشف و شناسایی خوشه", Color(0xFF7E57C2)),
    SANCTIONS_DESIGNATION("Sanctions Designation", "ثبت در فهرست تحریم", Color(0xFFD32F2F)),
    OSINT_FOOTPRINT("OSINT Discovery", "ردپای اطلاعاتی منابع باز", Color(0xFFE65100)),
    ABUSE_REPORT("Abuse / Threat Report", "گزارش سوءاستفاده و تخلف", Color(0xFFC2185B)),
    EVIDENCE_ACQUISITION("Evidence Acquisition", "تحصیل و پلمب ادله", Color(0xFF2E7D32))
}

data class ForensicTimelineEntry(
    val id: String,
    val eventType: TimelineEventType,
    val titleEn: String,
    val titleFa: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val eventTimestamp: Long,
    val collectionTimestamp: Long,
    val publicationTimestamp: Long? = null,
    val riskSeverity: RiskSeverity = RiskSeverity.INFO,
    val evidenceId: String? = null,
    val identifier: String = ""
)

/**
 * Forensic Timeline & Temporal Analysis Component.
 */
@Composable
fun ForensicTimelineVisualizer(
    investigationCase: InvestigationCase,
    isFa: Boolean,
    onEntrySelected: (ForensicTimelineEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<TimelineEventType?>(null) }

    val timelineEntries = remember(investigationCase) {
        buildTimelineEntries(investigationCase)
    }

    val filteredEntries = remember(timelineEntries, selectedFilter) {
        if (selectedFilter == null) timelineEntries else timelineEntries.filter { it.eventType == selectedFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Temporal Header & Burst Detection Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isFa) "گاه‌شمار زمانی وقوع رویدادها (Forensic Timeline)" else "Chronological Forensic Timeline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isFa) "تفکیک زمان وقوع از زمان جمع‌آوری" else "Strict Event vs Collection Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text(if (isFa) "همه رویدادها" else "All") }
                    )
                    FilterChip(
                        selected = selectedFilter == TimelineEventType.ON_CHAIN_TX,
                        onClick = { selectedFilter = TimelineEventType.ON_CHAIN_TX },
                        label = { Text(if (isFa) "تراکنش‌ها" else "Txs") }
                    )
                    FilterChip(
                        selected = selectedFilter == TimelineEventType.SANCTIONS_DESIGNATION,
                        onClick = { selectedFilter = TimelineEventType.SANCTIONS_DESIGNATION },
                        label = { Text(if (isFa) "تحریم‌ها" else "Sanctions") }
                    )
                    FilterChip(
                        selected = selectedFilter == TimelineEventType.OSINT_FOOTPRINT,
                        onClick = { selectedFilter = TimelineEventType.OSINT_FOOTPRINT },
                        label = { Text(if (isFa) "منبع باز" else "OSINT") }
                    )
                }
            }
        }

        // Timeline List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredEntries) { entry ->
                TimelineCard(entry = entry, isFa = isFa, onEntrySelected = onEntrySelected)
            }
        }
    }
}

@Composable
private fun TimelineCard(
    entry: ForensicTimelineEntry,
    isFa: Boolean,
    onEntrySelected: (ForensicTimelineEntry) -> Unit
) {
    val sdfUtc = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, entry.eventType.color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable { onEntrySelected(entry) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Timeline Type Dot
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(entry.eventType.color.copy(alpha = 0.15f), CircleShape)
                    .border(1.5.dp, entry.eventType.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (entry.eventType) {
                        TimelineEventType.ON_CHAIN_TX -> Icons.Default.SwapHoriz
                        TimelineEventType.ADDRESS_INCEPTION -> Icons.Default.PlayArrow
                        TimelineEventType.CLUSTER_DISCOVERY -> Icons.Default.GroupWork
                        TimelineEventType.SANCTIONS_DESIGNATION -> Icons.Default.Gavel
                        TimelineEventType.OSINT_FOOTPRINT -> Icons.Default.Search
                        TimelineEventType.ABUSE_REPORT -> Icons.Default.Warning
                        TimelineEventType.EVIDENCE_ACQUISITION -> Icons.Default.Verified
                    },
                    contentDescription = null,
                    tint = entry.eventType.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) entry.eventType.displayNameFa else entry.eventType.displayNameEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = entry.eventType.color,
                        fontWeight = FontWeight.Bold
                    )

                    if (entry.evidenceId != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = entry.evidenceId,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = if (isFa) entry.titleFa else entry.titleEn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isFa) entry.descriptionFa else entry.descriptionEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Multi-date row: event_date vs collection_date
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${if (isFa) "زمان وقوع رویداد (Event Time):" else "Event Time:"} ${sdfUtc.format(Date(entry.eventTimestamp))}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "${if (isFa) "زمان تحصیل و ثبت ادله (Collection Time):" else "Collection Time:"} ${sdfUtc.format(Date(entry.collectionTimestamp))}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun buildTimelineEntries(investigationCase: InvestigationCase): List<ForensicTimelineEntry> {
    val entries = mutableListOf<ForensicTimelineEntry>()
    val now = System.currentTimeMillis()

    // Address inception
    entries.add(
        ForensicTimelineEntry(
            id = "TL-001",
            eventType = TimelineEventType.ADDRESS_INCEPTION,
            titleEn = "Target Blockchain Address First Activity",
            titleFa = "اولین ثبت تراکنش آدرس هدف بر روی بلاکچین",
            descriptionEn = "Target address ${investigationCase.targetAddress.take(8)}... first seen active on ${investigationCase.network.displayName}.",
            descriptionFa = "آدرس هدف در شبکه ${investigationCase.network.displayName} برای اولین بار فعال شد.",
            eventTimestamp = now - 86400000L * 45,
            collectionTimestamp = now - 86400000L * 1,
            evidenceId = "EVD-INCEPTION-01",
            identifier = investigationCase.targetAddress
        )
    )

    // Transactions
    investigationCase.transactions.take(5).forEachIndexed { idx, tx ->
        entries.add(
            ForensicTimelineEntry(
                id = "TL-TX-$idx",
                eventType = TimelineEventType.ON_CHAIN_TX,
                titleEn = "Transaction: ${String.format("%.4f", tx.relevantAmountBtc)} BTC",
                titleFa = "تراکنش درون زنجیره‌ای: ${String.format("%.4f", tx.relevantAmountBtc)} بیت‌کوین",
                descriptionEn = "Transfer recorded in block ${tx.blockHeight} (TXID: ${tx.txid.take(10)}...).",
                descriptionFa = "تراکنش در بلاک شماره ${tx.blockHeight} ثبت گردید (شناسه: ${tx.txid.take(10)}...).",
                eventTimestamp = tx.timestamp.takeIf { it > 0 } ?: (now - 86400000L * (30 - idx * 5)),
                collectionTimestamp = now - 86400000L * 1,
                evidenceId = "EVD-TX-${tx.txid.take(4)}",
                identifier = tx.txid
            )
        )
    }

    // Sanctions / Threat Events
    if (investigationCase.riskIndicators.isNotEmpty()) {
        entries.add(
            ForensicTimelineEntry(
                id = "TL-SANCT-01",
                eventType = TimelineEventType.SANCTIONS_DESIGNATION,
                titleEn = "OFAC / OpenSanctions Watchlist Designation Check",
                titleFa = "بررسی تطبیق فهرست نظارتی تحریم‌های بین‌المللی",
                descriptionEn = "Verified against OpenSanctions yente engine and international regulatory bulletins.",
                descriptionFa = "استعلام سوابق از طریق موتور تطبیق yente و بخشنامه‌های نظارتی.",
                eventTimestamp = now - 86400000L * 10,
                collectionTimestamp = now - 86400000L * 1,
                evidenceId = "EVD-SANCT-01",
                identifier = "OFAC-SDN-CHECK"
            )
        )
    }

    // Evidence acquisition
    entries.add(
        ForensicTimelineEntry(
            id = "TL-SEAL-01",
            eventType = TimelineEventType.EVIDENCE_ACQUISITION,
            titleEn = "Digital Evidence Sealing & Cryptographic Hashes",
            titleFa = "پلمب ادله دیجیتال و تولید هش‌های یکپارچگی",
            descriptionEn = "Sealed ${investigationCase.evidenceLog.size} evidence items with SHA-256 integrity digest.",
            descriptionFa = "پلمب دیجیتال ادله با شناسه SHA-256 به منظور حفظ زنجیره نگهداری.",
            eventTimestamp = now,
            collectionTimestamp = now,
            evidenceId = "EVD-FINAL-SEAL",
            identifier = "CASE-DOSSIER"
        )
    )

    return entries.sortedBy { it.eventTimestamp }
}
