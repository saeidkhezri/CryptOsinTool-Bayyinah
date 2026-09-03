package com.aistudio.orbit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.provider.ProviderErrorBoundary
import com.aistudio.orbit.provider.ProviderFailureState

@Composable
fun PartialResultBanner(
    isPersian: Boolean,
    onOpenApiSettings: () -> Unit = {},
    onDismiss: () -> Unit = { ProviderErrorBoundary.clearFailures() }
) {
    val failures by ProviderErrorBoundary.providerFailures.collectAsState()
    val isPartial by ProviderErrorBoundary.isPartialResult.collectAsState()

    AnimatedVisibility(
        visible = isPartial && failures.isNotEmpty(),
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF2D1F10), // Warm warning amber background
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isPersian) "نتیجه به‌صورت جزئی (Partial Result) استخراج شد" else "Partial Result Received",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFDE68A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("×", color = Color(0xFFFDE68A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isPersian)
                        "ارتباط با برخی سرویس‌دهندگان خارجی برقرار نشد. سایر بخش‌های تحلیلی و ادله به صورت کاملاً ایزوله اجرا گردیدند."
                    else
                        "Some external service queries timed out. Local analysis and ledger verification proceeded without interruption.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFEF3C7),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    failures.take(3).forEach { failure ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF3B2712), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isPersian) "• ${failure.providerNameFa}: ${failure.errorMessage}" else "• ${failure.providerNameEn}: ${failure.errorMessage}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFCD34D),
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (isPersian) "خطای غیربحرانی" else "Non-blocking",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9CA3AF),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onOpenApiSettings,
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPersian) "تنظیمات Providerها" else "Provider Settings",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isPersian) "متوجه شدم" else "Acknowledge",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
