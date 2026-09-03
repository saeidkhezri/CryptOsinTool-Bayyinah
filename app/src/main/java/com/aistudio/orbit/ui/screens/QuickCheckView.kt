package com.aistudio.orbit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.model.ExperienceMode
import com.aistudio.orbit.ui.InvestigationViewModel

@Composable
fun QuickCheckView(
    viewModel: InvestigationViewModel,
    investigationCase: InvestigationCase,
    isPersian: Boolean,
    onEscalateToGuided: () -> Unit,
    onEscalateToAnalyst: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (isPersian) "بررسی سریع (Quick Check)" else "Quick Check",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isPersian) "آدرس هدف: ${investigationCase.targetAddress}" else "Target Address: ${investigationCase.targetAddress}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isPersian) "موجودی: ${investigationCase.balanceBtc} BTC" else "Balance: ${investigationCase.balanceBtc} BTC",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (isPersian) "تعداد تراکنش‌ها: ${investigationCase.transactions.size}" else "Transactions: ${investigationCase.transactions.size}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val riskIndicators = investigationCase.riskIndicators
        if (riskIndicators.isNotEmpty()) {
            Text(
                text = if (isPersian) "شاخص‌های ریسک مهم" else "Key Risk Indicators",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            riskIndicators.take(3).forEach { risk ->
                Text("• ${risk.title} (${risk.severity})", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        val counterparties = investigationCase.counterparties
        if (counterparties.isNotEmpty()) {
            Text(
                text = if (isPersian) "طرف‌های تعامل اصلی" else "Key Counterparties",
                style = MaterialTheme.typography.titleMedium
            )
            counterparties.sortedByDescending { it.txCount }.take(3).forEach { cp ->
                Text("• ${cp.address.take(10)}... (Tx: ${cp.txCount})", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onEscalateToGuided) {
                Icon(Icons.Default.Explore, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isPersian) "انتقال به بررسی هدایت‌شده" else "Escalate to Guided")
            }
            FilledTonalButton(onClick = onEscalateToAnalyst) {
                Icon(Icons.Default.Analytics, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isPersian) "محیط کارشناس" else "Analyst Workspace")
            }
        }
    }
}
