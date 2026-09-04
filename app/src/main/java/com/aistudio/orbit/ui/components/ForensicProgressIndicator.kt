package com.aistudio.orbit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class ForensicResourceLed(
    val id: String,
    val nameEn: String,
    val nameFa: String,
    val isOnline: Boolean = true,
    val isConnected: Boolean = true,
    val isLocal: Boolean = false,
    val pingMs: Long = 28
)

data class ForensicSubTask(
    val id: String,
    val titleEn: String,
    val titleFa: String,
    val isCompleted: Boolean = false,
    val isCurrent: Boolean = false,
    val isFailed: Boolean = false
)

data class ForensicProgressState(
    val isRunning: Boolean = false,
    val operationTitle: String = "",
    val stepDescription: String = "",
    val network: String = "",
    val progress: Float? = null, // null for indeterminate, 0.0 - 1.0 for determinate
    val itemsProcessed: Int = 0,
    val totalItems: Int = 0,
    val resourceLeds: List<ForensicResourceLed> = emptyList(),
    val subTasks: List<ForensicSubTask> = emptyList()
)

@Composable
fun ForensicOperationProgressDialog(
    state: ForensicProgressState,
    onCancel: () -> Unit,
    isPersian: Boolean = true
) {
    if (!state.isRunning) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Phosphor LED pulsing brightness
    val phosphorPulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phosphorPulse"
    )

    val listState = rememberLazyListState()

    // Auto-scroll to currently running subtask
    LaunchedEffect(state.subTasks) {
        val currentIndex = state.subTasks.indexOfFirst { it.isCurrent }
        if (currentIndex >= 0) {
            listState.animateScrollToItem(currentIndex.coerceAtLeast(0))
        }
    }

    Dialog(
        onDismissRequest = { /* Prevent accidental dismiss without cancel */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .widthIn(min = 300.dp, max = 500.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Rotation Hub & Phosphor Accent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = phosphorPulse), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Synchronizing",
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(rotation),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.operationTitle.ifBlank {
                                if (isPersian) "در حال دریافت اطلاعات از بلاک‌چین..." else "Querying Blockchain Ledger..."
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (state.network.isNotBlank()) {
                            Text(
                                text = "${if (isPersian) "شبکه فعال: " else "Network: "}${state.network}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 1. Phosphor LED Status Indicators Bar
                val defaultLeds = if (state.resourceLeds.isNotEmpty()) {
                    state.resourceLeds
                } else {
                    listOf(
                        ForensicResourceLed("node", "RPC Node Feed", "نود دفترکل / نود شبکه", isOnline = true, isConnected = true),
                        ForensicResourceLed("room", "Local SQLite Room DB", "پایگاه داده آفلاین Room", isOnline = false, isLocal = true, isConnected = true),
                        ForensicResourceLed("osint", "OSINT Threat Feeds", "سرویس‌های تجمیع اوسینت", isOnline = true, isConnected = true),
                        ForensicResourceLed("rates", "Forensic Currency Engine", "مرجع نرخ تسعیر تاریخی", isOnline = true, isConnected = true)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isPersian) "وضعیت اتصال منابع برخط و محلی (LED Status):" else "Online & Local Resources Telemetry:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        defaultLeds.take(2).forEach { led ->
                            PhosphorLedBadge(
                                led = led,
                                isPersian = isPersian,
                                pulseAlpha = phosphorPulse,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (defaultLeds.size > 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            defaultLeds.drop(2).take(2).forEach { led ->
                                PhosphorLedBadge(
                                    led = led,
                                    isPersian = isPersian,
                                    pulseAlpha = phosphorPulse,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 2. Determinate Progress Bar & Percentage
                val progressVal = (state.progress ?: 0.35f).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.stepDescription.ifBlank {
                                if (isPersian) "در حال پردازش زیروظایف و استخراج داده‌ها..." else "Processing micro-tasks..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(progressVal * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progressVal },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // 3. Granular Step-by-Step Task List with Green Checkmarks and Top Fade Effect
                val subTasks = if (state.subTasks.isNotEmpty()) {
                    state.subTasks
                } else {
                    // Fallback generated steps according to progress
                    generateDefaultSubTasks(progressVal)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 96.dp, max = 160.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(subTasks) { index, task ->
                                val isTopFading = index == 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset > 20
                                val itemAlpha = if (isTopFading) 0.45f else 1.0f

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer { alpha = itemAlpha }
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (task.isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (task.isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Completed",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else if (task.isCurrent) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "Pending",
                                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Text(
                                        text = if (isPersian) task.titleFa else task.titleEn,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (task.isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            task.isCompleted -> MaterialTheme.colorScheme.onSurface
                                            task.isCurrent -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Top gradient overlay to create soft fading out effect when items reach top
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .align(Alignment.CenterHorizontally)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surfaceContainerLowest,
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }

                // Divider
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Cancel Button (Responsive & Always Centered)
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPersian) "لغو فوری عملیات" else "Cancel Operation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

@Composable
private fun PhosphorLedBadge(
    led: ForensicResourceLed,
    isPersian: Boolean,
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    val phosphorGreen = MaterialTheme.colorScheme.primary
    val phosphorAmber = Color(0xFFFFB300)
    val phosphorCyan = Color(0xFF00E5FF)
    val phosphorRed = Color(0xFFFF3366)

    val ledColor = when {
        !led.isConnected -> phosphorRed
        led.isLocal -> phosphorCyan
        led.isOnline -> phosphorGreen
        else -> phosphorAmber
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(0.7.dp, ledColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Phosphor LED Dot with glow pulse
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(ledColor.copy(alpha = if (led.isConnected) pulseAlpha else 0.4f))
                .border(0.8.dp, ledColor, CircleShape)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isPersian) led.nameFa else led.nameEn,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (led.isLocal) {
                    if (isPersian) "محلی / رمزنگاری Room" else "Local / Room DB"
                } else {
                    "${if (isPersian) "برخط" else "Online"} • ${led.pingMs}ms"
                },
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = ledColor.copy(alpha = 0.9f)
            )
        }
    }
}

private fun generateDefaultSubTasks(progress: Float): List<ForensicSubTask> {
    return listOf(
        ForensicSubTask("s1", "Validate Public Address & Checksum", "اعتبارسنجی فرمت آدرس و کدکنترل عمومی", isCompleted = progress >= 0.12f, isCurrent = progress < 0.12f),
        ForensicSubTask("s2", "Query Public Blockchain Ledger", "بررسی آدرس در بلاکچین و واکشی مانده حساب", isCompleted = progress >= 0.25f, isCurrent = progress in 0.12f..0.24f),
        ForensicSubTask("s3", "Retrieve Historical Tx Streams", "واکشی و بازیابی تراکنش‌های تاریخی", isCompleted = progress >= 0.40f, isCurrent = progress in 0.25f..0.39f),
        ForensicSubTask("s4", "Flow Direction & Amount Normalization", "تفکیک جریان‌های ورودی/خروجی و نرمال‌سازی ساتوشی", isCompleted = progress >= 0.55f, isCurrent = progress in 0.40f..0.54f),
        ForensicSubTask("s5", "Counterparty Extraction & Cluster Mapping", "استخراج ماتریس طرف‌های مقابل و خوشه‌بندی", isCompleted = progress >= 0.70f, isCurrent = progress in 0.55f..0.69f),
        ForensicSubTask("s6", "AML Crime Pattern Heuristics Matching", "تطبیق با کتابخانه الگوهای پولشویی و جرائم", isCompleted = progress >= 0.85f, isCurrent = progress in 0.70f..0.84f),
        ForensicSubTask("s7", "Diurnal Rhythm & Temporal Profiling", "تحلیل شبانه‌روزی و سازگاری جغرافیایی-زمانی", isCompleted = progress >= 0.95f, isCurrent = progress in 0.85f..0.94f),
        ForensicSubTask("s8", "Consolidate Evidence Chain & Build Graph", "تکمیل زنجیره ادله دادگاهی و ترسیم گراف تعاملی", isCompleted = progress >= 1.0f, isCurrent = progress in 0.95f..0.99f)
    )
}

@Composable
fun ForensicProgressBanner(
    state: ForensicProgressState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    isPersian: Boolean = true
) {
    if (!state.isRunning) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.operationTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.stepDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(
                    text = if (isPersian) "لغو" else "Cancel",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

