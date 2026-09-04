package com.aistudio.orbit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.aistudio.orbit.forensics.ai.providers.AiProvider
import com.aistudio.orbit.forensics.ai.providers.AiProviderType
import com.aistudio.orbit.repository.AiProviderConfig
import com.aistudio.orbit.repository.AppLanguage
import com.aistudio.orbit.ui.InvestigationViewModel
import com.aistudio.orbit.ui.theme.ForensicSpacing
import kotlinx.coroutines.launch

@Composable
fun AiSettingsView(
    viewModel: InvestigationViewModel
) {
    val configs by viewModel.aiSettingsRepo.configs.collectAsState()
    val localOnly by viewModel.aiSettingsRepo.privacyModeLocalOnly.collectAsState()
    val language by viewModel.settingsRepo.language.collectAsState()
    val isFa = language == AppLanguage.PERSIAN

    val providers = listOf(
        viewModel.geminiProvider,
        viewModel.youSearchProvider,
        viewModel.openAiProvider,
        viewModel.deepSeekProvider
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(ForensicSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ForensicSpacing.lg)
    ) {
        item {
            Text("AI Investigator Copilot", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(ForensicSpacing.sm))
            Text("Configure AI providers for forensic assistance, hypothesis generation, and pattern recognition. The AI acts as an assistant and never replaces factual blockchain evidence.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (localOnly) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(ForensicSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (localOnly) Icons.Default.Security else Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = if (localOnly) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(ForensicSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFa) "حالت حریم خصوصی فقط محلی" else "Privacy Mode (Local Only)",
                            fontWeight = FontWeight.Bold,
                            color = if (localOnly) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (localOnly) {
                                if (isFa) "فعال (فقط محلی): هیچ داده‌ای به سرویس‌های هوش مصنوعی بیرونی ارسال نمی‌شود."
                                else "ACTIVE (Local Only): No case data sent to external AI services."
                            } else {
                                if (isFa) "غیرفعال (ارسال مجاز): ارتباط با سرویس‌های هوش مصنوعی بیرونی مجاز است."
                                else "INACTIVE (External Allowed): Case queries allowed for configured AI providers."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (localOnly) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = localOnly,
                        onCheckedChange = { viewModel.aiSettingsRepo.setPrivacyModeLocalOnly(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.error,
                            checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                }
            }
        }

        items(providers) { provider ->
            AiProviderCard(
                provider = provider,
                config = configs[provider.providerType] ?: AiProviderConfig(provider.providerType),
                apiKey = viewModel.aiSettingsRepo.getApiKey(provider.providerType),
                onSave = { updatedConfig, newApiKey ->
                    viewModel.aiSettingsRepo.updateConfig(updatedConfig, newApiKey)
                },
                onTestConnection = { key, endpoint ->
                    provider.testConnection(key, endpoint)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProviderCard(
    provider: AiProvider,
    config: AiProviderConfig,
    apiKey: String,
    onSave: (AiProviderConfig, String?) -> Unit,
    onTestConnection: suspend (String, String?) -> Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isEnabled by remember(config.enabled) { mutableStateOf(config.enabled) }
    var inputKey by remember { mutableStateOf(if (apiKey.isNotBlank()) "********" else "") }
    var selectedModel by remember(config.model) { mutableStateOf(if (config.model.isNotBlank()) config.model else provider.defaultModels.first()) }
    var inputEndpoint by remember(config.endpoint) { mutableStateOf(config.endpoint) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember(config.lastTestSuccess) { mutableStateOf(config.lastTestSuccess) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(ForensicSpacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(ForensicSpacing.md))
                Text(provider.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { 
                        isEnabled = it
                        onSave(config.copy(enabled = it, model = selectedModel, endpoint = inputEndpoint), null)
                    }
                )
            }
            
            Text("Version: ${provider.version} • Cost: ${provider.costCategory}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(ForensicSpacing.md))

            if (isEnabled) {
                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    label = { Text("API Key") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(ForensicSpacing.sm))

                var modelDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = modelDropdownExpanded,
                    onExpandedChange = { modelDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = { selectedModel = it },
                        label = { Text("Model") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        readOnly = false
                    )
                    ExposedDropdownMenu(
                        expanded = modelDropdownExpanded,
                        onDismissRequest = { modelDropdownExpanded = false }
                    ) {
                        provider.defaultModels.forEach { modelName ->
                            DropdownMenuItem(
                                text = { Text(modelName) },
                                onClick = {
                                    selectedModel = modelName
                                    modelDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (provider.providerType != AiProviderType.GEMINI) {
                    Spacer(modifier = Modifier.height(ForensicSpacing.sm))
                    OutlinedTextField(
                        value = inputEndpoint,
                        onValueChange = { inputEndpoint = it },
                        label = { Text("Custom Endpoint (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(ForensicSpacing.md))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            val keyToSave = if (inputKey == "********") null else inputKey
                            onSave(config.copy(enabled = isEnabled, model = selectedModel, endpoint = inputEndpoint), keyToSave)
                            Toast.makeText(context, "Configuration saved", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Save Configuration")
                    }
                    
                    Spacer(modifier = Modifier.width(ForensicSpacing.sm))
                    
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isTesting = true
                                val actualKey = if (inputKey == "********") apiKey else inputKey
                                val success = onTestConnection(actualKey, inputEndpoint.takeIf { it.isNotBlank() })
                                testResult = success
                                onSave(config.copy(
                                    enabled = true, 
                                    model = selectedModel, 
                                    endpoint = inputEndpoint, 
                                    lastTestSuccess = success,
                                    lastTestTimestamp = System.currentTimeMillis()
                                ), null)
                                isTesting = false
                            }
                        },
                        enabled = !isTesting
                    ) {
                        Text(if (isTesting) "Testing..." else "Test Connection")
                    }
                }

                if (testResult != null) {
                    Spacer(modifier = Modifier.height(ForensicSpacing.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (testResult == true) Icons.Default.Security else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (testResult == true) Color(0xFF00E676) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (testResult == true) "Connection successful" else "Connection failed. Check API Key.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (testResult == true) Color(0xFF00E676) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Text("Provider is disabled.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
