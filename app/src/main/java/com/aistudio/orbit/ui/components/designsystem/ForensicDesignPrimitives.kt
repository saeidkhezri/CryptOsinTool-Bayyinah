package com.aistudio.orbit.ui.components.designsystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.ui.theme.*

/**
 * Standardized Forensic Card with responsive padding, refined elevation, and theme-adaptive borders.
 */
@Composable
fun ForensicCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = ForensicShapes.lg,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = borderColor?.let { CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(it)) },
        elevation = CardDefaults.cardElevation(defaultElevation = ForensicElevation.card),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(ForensicSpacing.base),
            verticalArrangement = Arrangement.spacedBy(ForensicSpacing.md),
            content = content
        )
    }
}

/**
 * Standardized Forensic Metric Card for displaying critical numbers, balances, and counts.
 */
@Composable
fun ForensicMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeText: String? = null
) {
    Card(
        shape = ForensicShapes.md,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = ForensicElevation.subtle),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(ForensicSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (badgeText != null) {
                        Surface(
                            shape = ForensicShapes.xs,
                            color = accentColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Standardized Section Header with title, subtitle, icon, and optional action.
 */
@Composable
fun ForensicSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.sm),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (action != null) {
            action()
        }
    }
}

/**
 * Monospace Address component with direction isolation (RTL-safe) and one-tap copy.
 */
@Composable
fun ForensicAddressText(
    address: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    isPersian: Boolean = false
) {
    val context = LocalContext.current
    Surface(
        shape = ForensicShapes.sm,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Address", address))
                Toast.makeText(context, if (isPersian) "آدرس در کلیپ‌بورد کپی شد" else "Address copied to clipboard", Toast.LENGTH_SHORT).show()
            }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = ForensicSpacing.md, vertical = ForensicSpacing.sm)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val formattedAddress = remember(address) {
                    com.aistudio.orbit.util.ForensicBidiUtils.formatLtrTechnicalString(address)
                }
                Text(
                    text = formattedAddress,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        textDirection = TextDirection.Ltr,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(ForensicSpacing.sm))
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Standardized Forensic Badge Types
 */
enum class ForensicBadgeType {
    PRIMARY, SUCCESS, WARNING, ERROR, INFO, MUTED
}

/**
 * Standardized Forensic Badge for labels, status, epistemic taxonomy, and indicators.
 */
@Composable
fun ForensicBadge(
    text: String,
    modifier: Modifier = Modifier,
    badgeType: ForensicBadgeType = ForensicBadgeType.PRIMARY,
    containerColor: Color? = null,
    contentColor: Color? = null,
    icon: ImageVector? = null
) {
    val effectiveContainer = containerColor ?: when (badgeType) {
        ForensicBadgeType.PRIMARY -> MaterialTheme.colorScheme.primaryContainer
        ForensicBadgeType.SUCCESS -> Color(0xFF2E7D32).copy(alpha = 0.15f)
        ForensicBadgeType.WARNING -> Color(0xFFFFA000).copy(alpha = 0.15f)
        ForensicBadgeType.ERROR -> MaterialTheme.colorScheme.errorContainer
        ForensicBadgeType.INFO -> MaterialTheme.colorScheme.secondaryContainer
        ForensicBadgeType.MUTED -> MaterialTheme.colorScheme.surfaceVariant
    }

    val effectiveContent = contentColor ?: when (badgeType) {
        ForensicBadgeType.PRIMARY -> MaterialTheme.colorScheme.onPrimaryContainer
        ForensicBadgeType.SUCCESS -> Color(0xFF2E7D32)
        ForensicBadgeType.WARNING -> Color(0xFFE65100)
        ForensicBadgeType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        ForensicBadgeType.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
        ForensicBadgeType.MUTED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = ForensicShapes.pill,
        color = effectiveContainer,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = ForensicSpacing.sm, vertical = ForensicSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.xs)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = effectiveContent,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = effectiveContent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


/**
 * Standardized Empty State.
 */
@Composable
fun ForensicEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.SearchOff,
    actionButton: @Composable (() -> Unit)? = null
) {
    Card(
        shape = ForensicShapes.md,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ForensicSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ForensicSpacing.md)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionButton != null) {
                actionButton()
            }
        }
    }
}
