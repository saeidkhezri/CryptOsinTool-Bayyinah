package com.aistudio.orbit.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.scale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import com.aistudio.orbit.repository.ThemeMode
import com.aistudio.orbit.ui.theme.ForensicShapes
import com.aistudio.orbit.forensics.database.StorageBreakdown
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.forensics.audit.AuditTrailService
import com.aistudio.orbit.localization.AppLocalization
import com.aistudio.orbit.model.*
import com.aistudio.orbit.repository.AppLanguage
import com.aistudio.orbit.ui.InvestigationViewModel
import com.aistudio.orbit.ui.components.Phosphor3dIconBadge
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingsView(
    viewModel: InvestigationViewModel,
    onRequestPermissionsDialog: (() -> Unit)? = null,
    onNavigateToAiSettings: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val language by viewModel.settingsRepo.language.collectAsState()
    val isPersian = language == AppLanguage.PERSIAN
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedSection by remember { mutableStateOf(SettingsSection.API_INTEGRATIONS) }

    // API & Database flows
    val apiConfigs by viewModel.apiManagerService.apiConfigs.collectAsState()
    val sherlockEnabled by viewModel.apiManagerService.sherlockEnabled.collectAsState()
    val sherlockEndpoint by viewModel.apiManagerService.sherlockEndpoint.collectAsState()
    val sherlockApiKey by viewModel.apiManagerService.sherlockApiKey.collectAsState()

    val databases by viewModel.databaseManager.databases.collectAsState()
    val storageBreakdown by viewModel.databaseManager.storageBreakdown.collectAsState()
    val isDbPasswordConfigured by viewModel.databaseManager.isPasswordConfigured.collectAsState()

    val sslVerificationEnabled by viewModel.settingsRepo.isStrictSslEnabled.collectAsState()
    val strictRateLimitEnabled by viewModel.settingsRepo.isRateLimitProtectionEnabled.collectAsState()

    // Dialog States
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var activeHelpDialogApi by remember { mutableStateOf<ComprehensiveApiConfig?>(null) }
    var testingApiId by remember { mutableStateOf<String?>(null) }
    var selectedApiCategory by remember { mutableStateOf<ApiCategory?>(null) }
    var showSetPasswordDialog by remember { mutableStateOf(false) }

    // Live Diagnostic Popup States
    var diagnosticResultToDisplay by remember { mutableStateOf<ApiComprehensiveDiagnosticResult?>(null) }
    var activeDiagnosticSteps by remember { mutableStateOf<List<DiagnosticStepProgress>>(emptyList()) }
    var isRunningDiagnostics by remember { mutableStateOf(false) }

    val onRunDiagnostics: (String) -> Unit = { id ->
        coroutineScope.launch {
            isRunningDiagnostics = true
            activeDiagnosticSteps = emptyList()
            diagnosticResultToDisplay = null
            val result = viewModel.apiManagerService.runLiveDiagnostics(
                apiId = id,
                onStepProgress = { progress ->
                    activeDiagnosticSteps = activeDiagnosticSteps.toMutableList().apply {
                        val idx = indexOfFirst { it.stepId == progress.stepId }
                        if (idx >= 0) this[idx] = progress else add(progress)
                    }
                }
            )
            diagnosticResultToDisplay = result
            isRunningDiagnostics = false
        }
    }

    // Periodic Ping Refresh Effect
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(12000)
            viewModel.apiManagerService.refreshAllPingStatuses()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthClass = when {
            maxWidth < 600.dp -> WindowWidthSizeClass.Compact
            maxWidth < 840.dp -> WindowWidthSizeClass.Medium
            else -> WindowWidthSizeClass.Expanded
        }

        if (widthClass == WindowWidthSizeClass.Compact) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    SettingsDrawerContent(
                        selectedSection = selectedSection,
                        onSectionSelected = { 
                            selectedSection = it
                            coroutineScope.launch { drawerState.close() }
                        },
                        isFa = isPersian,
                        onBack = onBack
                    )
                }
            ) {
                SettingsScaffold(
                    selectedSection = selectedSection,
                    isFa = isPersian,
                    onBack = onBack,
                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                    viewModel = viewModel,
                    isCompact = true,
                    // Pass needed states/actions
                    apiConfigs = apiConfigs,
                    selectedApiCategory = selectedApiCategory,
                    onCategorySelect = { selectedApiCategory = it },
                    showExportDialog = { showExportDialog = true },
                    showImportDialog = { showImportDialog = true },
                    sherlockEnabled = sherlockEnabled,
                    sherlockEndpoint = sherlockEndpoint,
                    sherlockApiKey = sherlockApiKey,
                    testingApiId = testingApiId,
                    onTestConnection = onRunDiagnostics,
                    onOpenHelp = { activeHelpDialogApi = it },
                    databases = databases,
                    storageBreakdown = storageBreakdown,
                    isDbPasswordConfigured = isDbPasswordConfigured,
                    onSetupPasswordClick = { showSetPasswordDialog = true },
                    sslVerificationEnabled = sslVerificationEnabled,
                    strictRateLimitEnabled = strictRateLimitEnabled,
                    onRequestPermissionsDialog = onRequestPermissionsDialog
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.width(300.dp).fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp
                ) {
                    SettingsDrawerContent(
                        selectedSection = selectedSection,
                        onSectionSelected = { selectedSection = it },
                        isFa = isPersian,
                        onBack = onBack,
                        showBack = true
                    )
                }
                VerticalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Box(modifier = Modifier.weight(1f)) {
                    SettingsScaffold(
                        selectedSection = selectedSection,
                        isFa = isPersian,
                        onBack = onBack,
                        onOpenDrawer = {},
                        viewModel = viewModel,
                        isCompact = false,
                        apiConfigs = apiConfigs,
                        selectedApiCategory = selectedApiCategory,
                        onCategorySelect = { selectedApiCategory = it },
                        showExportDialog = { showExportDialog = true },
                        showImportDialog = { showImportDialog = true },
                        sherlockEnabled = sherlockEnabled,
                        sherlockEndpoint = sherlockEndpoint,
                        sherlockApiKey = sherlockApiKey,
                        testingApiId = testingApiId,
                        onTestConnection = onRunDiagnostics,
                        onOpenHelp = { activeHelpDialogApi = it },
                        databases = databases,
                        storageBreakdown = storageBreakdown,
                        isDbPasswordConfigured = isDbPasswordConfigured,
                        onSetupPasswordClick = { showSetPasswordDialog = true },
                        sslVerificationEnabled = sslVerificationEnabled,
                        strictRateLimitEnabled = strictRateLimitEnabled,
                        onRequestPermissionsDialog = onRequestPermissionsDialog
                    )
                }
            }
        }
    }
    
    // Original Dialogs go here (Export/Import/Pass)
    if (showExportDialog) {
        var includeRealKeys by remember { mutableStateOf(false) }
        val generatedContent = remember(includeRealKeys) {
            viewModel.apiManagerService.exportApiConfiguration(includeRealKeys, isPersian)
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            icon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(if (isPersian) "استخراج پیکربندی و قالب کلیدهای API" else "Export API Configuration & Template", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isPersian) "شما می‌توانید فایل قالب پیکربندی را به صورت استاندارد متنی دریافت کنید. جهت حفظ امنیت، کلیدهای واقعی شما به صورت پیش‌فرض درج نمی‌شوند مگر با تأیید صریح شما."
                        else "Generate a standardized API configuration file. To protect credentials, real keys are masked with placeholders by default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPersian) "درج کلیدهای واقعی فعال (حساس)" else "Include active real keys (Sensitive)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(checked = includeRealKeys, onCheckedChange = { includeRealKeys = it })
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Text(
                            text = generatedContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Bayyinah API Template", generatedContent))
                        Toast.makeText(context, if (isPersian) "متن پیکربندی در حافظه موقت کپی شد" else "Configuration copied to clipboard", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPersian) "کپی متن استخراج‌شده" else "Copy Configuration")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(if (isPersian) "انصراف" else "Cancel")
                }
            }
        )
    }

    if (showImportDialog) {
        var importTextContent by remember { mutableStateOf("") }
        var isValidatingImport by remember { mutableStateOf(false) }
        var validationItemsToConfirm by remember { mutableStateOf<List<ValidatedApiKeyItem>?>(null) }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            icon = { Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    text = if (validationItemsToConfirm == null) {
                        if (isPersian) "بارگزاری فایل یا متن کلیدهای API" else "Import API Keys File / Text"
                    } else {
                        if (isPersian) "تایید و ذخیره کلیدهای شناسایی شده" else "Confirm Detected API Keys"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (validationItemsToConfirm == null) {
                        Text(
                            text = if (isPersian) "محتوای حاوی کلیدهای خود را به همراه هرگونه توضیحات اضافه وارد نمایید. سامانه کلیدها را اسکن و استخراج کرده و تا ۵ کلید معتبر به ازای هر سرویس را برای توزیع بار و افزایش محدودیت نرخ ذخیره می‌نماید."
                            else "Paste text containing your API keys with any descriptive comments. The system will auto-extract keys, supporting up to 5 keys per provider for rate-limit balancing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = importTextContent,
                            onValueChange = { importTextContent = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            placeholder = {
                                Text(
                                    "GOOGLE_AI_API_KEY=AIzaSy...\nOPENAI_API_KEY=sk-proj...\nSHODAN_API_KEY=...\nABUSEIPDB_API_KEY=...",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        )
                    } else {
                        val items = validationItemsToConfirm.orEmpty()
                        if (items.isEmpty()) {
                            Text(
                                text = if (isPersian) "هیچ کلید معتبری در متن وارد شده شناسایی نشد." else "No valid API keys could be identified in the text.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                text = if (isPersian) "کلیدهای زیر با موفقیت شناسایی و اعتبارسنجی شدند:" else "The following keys were successfully identified and validated:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 250.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(items) { item ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = item.serviceName,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Badge(
                                                    containerColor = if (item.connectionState == ApiConnectionState.CONNECTED) Color(0xFF4CAF50).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                                                    contentColor = if (item.connectionState == ApiConnectionState.CONNECTED) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                                ) {
                                                    Text(
                                                        text = if (item.connectionState == ApiConnectionState.CONNECTED) {
                                                            if (isPersian) "معتبر" else "Valid"
                                                        } else {
                                                            if (isPersian) "نامعتبر" else "Invalid"
                                                        },
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.maskedKey,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (item.quotaText.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${if (isPersian) "وضعیت" else "Status"}: ${item.quotaText}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (validationItemsToConfirm == null) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isValidatingImport = true
                                val pairs = viewModel.apiManagerService.parseImportFile(importTextContent)
                                val validated = viewModel.apiManagerService.validateImportCandidates(pairs)
                                isValidatingImport = false
                                validationItemsToConfirm = validated
                            }
                        },
                        enabled = importTextContent.isNotBlank() && !isValidatingImport
                    ) {
                        if (isValidatingImport) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPersian) "بررسی و اعتبارسنجی" else "Validate Keys")
                        }
                    }
                } else {
                    val items = validationItemsToConfirm.orEmpty()
                    if (items.isNotEmpty()) {
                        Button(
                            onClick = {
                                viewModel.apiManagerService.commitImportedKeys(items)
                                Toast.makeText(context, if (isPersian) "کلیدها با موفقیت ذخیره شدند" else "Keys successfully committed and stored", Toast.LENGTH_LONG).show()
                                showImportDialog = false
                                validationItemsToConfirm = null
                            }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPersian) "تایید و ذخیره نهایی" else "Confirm & Save")
                        }
                    }
                }
            },
            dismissButton = {
                if (validationItemsToConfirm == null) {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text(if (isPersian) "انصراف" else "Cancel")
                    }
                } else {
                    TextButton(onClick = { validationItemsToConfirm = null }) {
                        Text(if (isPersian) "بازگشت" else "Back")
                    }
                }
            }
        )
    }

    activeHelpDialogApi?.let { api ->
        AlertDialog(
            onDismissRequest = { activeHelpDialogApi = null },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(text = api.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isPersian) api.descriptionFa else api.descriptionEn,
                        style = MaterialTheme.typography.bodySmall
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = if (isPersian) "کاربرد در بیّنة:" else "Usage in App:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isPersian) api.appUsageFa else api.appUsageEn,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = if (isPersian) "محدودیت‌ها و تعرفه:" else "Limitations & Quota:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isPersian) api.limitationsFa else api.limitationsEn,
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (api.officialUrl.isNotBlank() || api.docUrl.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = if (isPersian) "لینک‌های دسترسی و مستندات:" else "Access & Documentation Links:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (api.officialUrl.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(api.officialUrl))
                                    context.startActivity(intent)
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPersian) "دریافت کلید API از سایت رسمی" else "Get API Key from Official Site", fontSize = 11.sp)
                            }
                        }
                        if (api.docUrl.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(api.docUrl))
                                    context.startActivity(intent)
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPersian) "مشاهده مستندات فنی سرویس" else "View Technical Documentation", fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeHelpDialogApi = null }) {
                    Text(if (isPersian) "بستن" else "Close")
                }
            }
        )
    }

    if (isRunningDiagnostics || diagnosticResultToDisplay != null || activeDiagnosticSteps.isNotEmpty()) {
        ApiValidationDiagnosticsDialog(
            isPersian = isPersian,
            diagnosticResult = diagnosticResultToDisplay,
            activeSteps = activeDiagnosticSteps,
            isRunning = isRunningDiagnostics,
            onDismiss = {
                isRunningDiagnostics = false
                diagnosticResultToDisplay = null
                activeDiagnosticSteps = emptyList()
            }
        )
    }

    if (showSetPasswordDialog) {
        var passwordInput by remember { mutableStateOf("") }
        var confirmPasswordInput by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSetPasswordDialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    text = if (isPersian) "تنظیم گذرواژه دیتابیس" else "Set Database Password",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isPersian) "گذرواژه امنیتی جهت استخراج کلید رمزگذاری ۲۵۶ بیتی (PBKDF2) برای محافظت از داده‌های فارنزیک دیتابیس در برابر دسترسی غیرمجاز. حداقل طول گذرواژه ۸ نویسه است."
                        else "Set security password to derive PBKDF2/AES-256 keys protecting forensic datasets. Minimum 8 characters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            errorMessage = null
                        },
                        label = { Text(if (isPersian) "گذرواژه جدید" else "New Password") },
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = {
                            confirmPasswordInput = it
                            errorMessage = null
                        },
                        label = { Text(if (isPersian) "تکرار گذرواژه" else "Confirm Password") },
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    errorMessage?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    if (isDbPasswordConfigured) {
                        OutlinedButton(
                            onClick = {
                                viewModel.databaseManager.clearDatabasePassword()
                                Toast.makeText(context, if (isPersian) "رمزگذاری دیتابیس غیرفعال شد" else "Database password cleared", Toast.LENGTH_SHORT).show()
                                showSetPasswordDialog = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isPersian) "حذف گذرواژه و غیرفعال‌سازی" else "Clear & Disable Password")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordInput.length < 8) {
                            errorMessage = if (isPersian) "حداقل طول گذرواژه باید ۸ نویسه باشد" else "Password must be at least 8 characters"
                            return@Button
                        }
                        if (passwordInput != confirmPasswordInput) {
                            errorMessage = if (isPersian) "تکرار گذرواژه همخوانی ندارد" else "Passwords do not match"
                            return@Button
                        }
                        val success = viewModel.databaseManager.setDatabasePassword(passwordInput)
                        if (success) {
                            Toast.makeText(context, if (isPersian) "گذرواژه دیتابیس با موفقیت ثبت شد" else "Database password set successfully", Toast.LENGTH_SHORT).show()
                            showSetPasswordDialog = false
                        } else {
                            errorMessage = if (isPersian) "خطا در تنظیم گذرواژه" else "Failed to set password"
                        }
                    }
                ) {
                    Text(if (isPersian) "ذخیره گذرواژه" else "Save Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPasswordDialog = false }) {
                    Text(if (isPersian) "انصراف" else "Cancel")
                }
            }
        )
    }
}

enum class SettingsSection {
    API_INTEGRATIONS,
    PRIVACY_AI,
    FORENSIC_DATABASES,
    SECURITY_VAULT,
    CASES_BACKUPS,
    DB_FILE_MANAGER,
    APPEARANCE,
    SYSTEM_AUDIT
}

@Composable
fun SettingsDrawerContent(
    selectedSection: SettingsSection,
    onSectionSelected: (SettingsSection) -> Unit,
    isFa: Boolean,
    onBack: () -> Unit,
    showBack: Boolean = false
) {
    ModalDrawerSheet(
        modifier = Modifier.fillMaxHeight().width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (showBack) {
                IconButton(onClick = onBack, modifier = Modifier.padding(bottom = 16.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            
            Text(
                text = if (isFa) "تنظیمات بیِّنة" else "Bayyinah Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
            )
            
            HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            
            // Category: Intelligence & APIs
            Text(
                text = if (isFa) "هوشمندی و اتصالات" else "Intelligence & APIs",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            SettingsDrawerItem(
                label = if (isFa) "سرویس‌ها و کلیدهای API" else "APIs & Integrations",
                icon = Icons.Default.Key,
                selected = selectedSection == SettingsSection.API_INTEGRATIONS,
                onClick = { onSectionSelected(SettingsSection.API_INTEGRATIONS) }
            )
            SettingsDrawerItem(
                label = if (isFa) "حریم خصوصی و هوش مصنوعی" else "Privacy & AI Settings",
                icon = Icons.Default.VpnLock,
                selected = selectedSection == SettingsSection.PRIVACY_AI,
                onClick = { onSectionSelected(SettingsSection.PRIVACY_AI) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Category: Data & Security
            Text(
                text = if (isFa) "داده و امنیت" else "Data & Security",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            SettingsDrawerItem(
                label = if (isFa) "دیتابیس‌های فارنزیک" else "Forensic Databases",
                icon = Icons.Default.Storage,
                selected = selectedSection == SettingsSection.FORENSIC_DATABASES,
                onClick = { onSectionSelected(SettingsSection.FORENSIC_DATABASES) }
            )
            SettingsDrawerItem(
                label = if (isFa) "امنیت و رمزگذاری" else "Security & Vault",
                icon = Icons.Default.VpnKey,
                selected = selectedSection == SettingsSection.SECURITY_VAULT,
                onClick = { onSectionSelected(SettingsSection.SECURITY_VAULT) }
            )
            SettingsDrawerItem(
                label = if (isFa) "مدیریت فایل‌های دیتابیس" else "DB File Manager",
                icon = Icons.Default.SettingsBackupRestore,
                selected = selectedSection == SettingsSection.DB_FILE_MANAGER,
                onClick = { onSectionSelected(SettingsSection.DB_FILE_MANAGER) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Category: System & Management
            Text(
                text = if (isFa) "سیستم و مدیریت" else "System & Management",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            SettingsDrawerItem(
                label = if (isFa) "پرونده‌ها و پشتیبان" else "Cases & Backups",
                icon = Icons.Default.FolderZip,
                selected = selectedSection == SettingsSection.CASES_BACKUPS,
                onClick = { onSectionSelected(SettingsSection.CASES_BACKUPS) }
            )
            SettingsDrawerItem(
                label = if (isFa) "ظاهر و پوسته" else "Appearance & Theme",
                icon = Icons.Default.Palette,
                selected = selectedSection == SettingsSection.APPEARANCE,
                onClick = { onSectionSelected(SettingsSection.APPEARANCE) }
            )
            SettingsDrawerItem(
                label = if (isFa) "سیستم و ممیزی" else "System & Audit",
                icon = Icons.Default.Settings,
                selected = selectedSection == SettingsSection.SYSTEM_AUDIT,
                onClick = { onSectionSelected(SettingsSection.SYSTEM_AUDIT) }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "Bayyinah Forensic Suite v2.4.1",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun SettingsDrawerItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScaffold(
    selectedSection: SettingsSection,
    isFa: Boolean,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: InvestigationViewModel,
    isCompact: Boolean,
    apiConfigs: List<ComprehensiveApiConfig>,
    selectedApiCategory: ApiCategory?,
    onCategorySelect: (ApiCategory?) -> Unit,
    showExportDialog: () -> Unit,
    showImportDialog: () -> Unit,
    sherlockEnabled: Boolean,
    sherlockEndpoint: String,
    sherlockApiKey: String,
    testingApiId: String?,
    onTestConnection: (String) -> Unit = {},
    onOpenHelp: (ComprehensiveApiConfig) -> Unit,
    databases: List<ForensicDatabaseInfo>,
    storageBreakdown: StorageBreakdown,
    isDbPasswordConfigured: Boolean,
    onSetupPasswordClick: () -> Unit,
    sslVerificationEnabled: Boolean,
    strictRateLimitEnabled: Boolean,
    onRequestPermissionsDialog: (() -> Unit)?
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val useLuxuryBackground = viewModel.settingsRepo.useLuxuryBackground.collectAsState().value
    
    Scaffold(
        containerColor = if (useLuxuryBackground) Color.Transparent else MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = when(selectedSection) {
                            SettingsSection.API_INTEGRATIONS -> if (isFa) "سرویس‌ها و کلیدهای API" else "APIs & Integrations"
                            SettingsSection.PRIVACY_AI -> if (isFa) "حریم خصوصی و هوش مصنوعی" else "Privacy & AI Settings"
                            SettingsSection.FORENSIC_DATABASES -> if (isFa) "دیتابیس‌های فارنزیک" else "Forensic Databases"
                            SettingsSection.SECURITY_VAULT -> if (isFa) "امنیت و رمزگذاری" else "Security & Vault"
                            SettingsSection.CASES_BACKUPS -> if (isFa) "پرونده‌ها و پشتیبان" else "Cases & Backups"
                            SettingsSection.DB_FILE_MANAGER -> if (isFa) "مدیریت فایل‌های دیتابیس" else "Database File Manager"
                            SettingsSection.APPEARANCE -> if (isFa) "ظاهر و پوسته" else "Appearance & Theme"
                            SettingsSection.SYSTEM_AUDIT -> if (isFa) "سیستم و ممیزی" else "System & Audit"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (isCompact) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (selectedSection) {
                SettingsSection.API_INTEGRATIONS -> {
                    ApisAndIntegrationsTab(
                        isPersian = isFa,
                        apiConfigs = apiConfigs,
                        selectedCategory = selectedApiCategory,
                        onCategorySelect = onCategorySelect,
                        onExportClick = showExportDialog,
                        onImportClick = showImportDialog,
                        sherlockEnabled = sherlockEnabled,
                        sherlockEndpoint = sherlockEndpoint,
                        sherlockApiKey = sherlockApiKey,
                        onUpdateSherlock = { enabled, ep, key -> viewModel.apiManagerService.updateSherlockConfig(enabled, ep, key) },
                        onSaveKey = { id, key, sec -> viewModel.apiManagerService.saveApiKey(id, key, sec) },
                        onToggleEnabled = { id, enabled -> viewModel.apiManagerService.toggleProviderEnabled(id, enabled) },
                        onTestConnection = onTestConnection,
                        testingApiId = testingApiId,
                        onOpenHelp = onOpenHelp
                    )
                }
                SettingsSection.PRIVACY_AI -> PrivacyAndAiTab(isFa, viewModel)
                SettingsSection.FORENSIC_DATABASES -> {
                    DatabasesTab(
                        isPersian = isFa,
                        databases = databases,
                        storageBreakdown = storageBreakdown,
                        isDbPasswordConfigured = isDbPasswordConfigured,
                        onSetupPasswordClick = onSetupPasswordClick,
                        onDownloadDatabase = { id -> coroutineScope.launch { viewModel.databaseManager.startDatabaseDownload(id) } },
                        onCancelDownload = { id -> viewModel.databaseManager.cancelDownload(id) },
                        onRebuildIndex = { id -> coroutineScope.launch { viewModel.databaseManager.rebuildIndex(id) } },
                        onVerifyIntegrity = { id -> coroutineScope.launch { viewModel.databaseManager.verifyIntegrity(id) } },
                        onToggleDbEnabled = { id, enabled -> viewModel.databaseManager.toggleDatabaseEnabled(id, enabled) },
                        onUninstallDb = { id -> viewModel.databaseManager.uninstallDatabase(id) },
                        onClearCache = {
                            val freed = viewModel.databaseManager.clearCache()
                            val mb = freed / (1024 * 1024)
                            Toast.makeText(context, if (isFa) "حافظه موقت پاکسازی شد ($mb مگابایت آزاد شد)" else "Cache cleared ($mb MB freed)", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                SettingsSection.SECURITY_VAULT -> {
                    SecurityTab(
                        isPersian = isFa,
                        isDbPasswordConfigured = isDbPasswordConfigured,
                        onSetupPasswordClick = onSetupPasswordClick,
                        sslVerificationEnabled = sslVerificationEnabled,
                        onToggleSsl = { viewModel.settingsRepo.setStrictSslEnabled(it) },
                        strictRateLimitEnabled = strictRateLimitEnabled,
                        onToggleRateLimit = { viewModel.settingsRepo.setRateLimitProtectionEnabled(it) }
                    )
                }
                SettingsSection.CASES_BACKUPS -> CasesAndBackupsTab(isFa, viewModel)
                SettingsSection.DB_FILE_MANAGER -> DatabaseFileManagerTab(isFa, viewModel)
                SettingsSection.APPEARANCE -> AppearanceTab(isFa, viewModel)
                SettingsSection.SYSTEM_AUDIT -> SystemAndAuditTab(isFa, viewModel, onRequestPermissionsDialog)
            }
        }
    }
}

@Composable
fun PrivacyAndAiTab(isFa: Boolean, viewModel: InvestigationViewModel) {
    val privacyMode by viewModel.settingsRepo.isPrivacyModeEnabled.collectAsState()
    val aiSearchEnabled by viewModel.aiSettingsRepo.isSearchEnabled.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VpnLock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isFa) "حالت حریم خصوصی پیشرفته" else "Enhanced Privacy Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = privacyMode,
                            onCheckedChange = { viewModel.settingsRepo.setPrivacyModeEnabled(it) }
                        )
                    }
                    Text(
                        text = if (isFa) "در این حالت، هیچ داده‌ای به صورت خودکار به سرویس‌های هوش مصنوعی ارسال نمی‌شود و تمامی تحلیل‌ها با تایید کاربر انجام می‌گیرد." 
                        else "When enabled, no data is automatically sent to AI services. All analysis requires manual investigator approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isFa) "قابلیت جستجوی هوشمند در وب" else "Smart Search Capabilities",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = aiSearchEnabled,
                            onCheckedChange = { viewModel.aiSettingsRepo.setSearchEnabled(it) }
                        )
                    }
                    Text(
                        text = if (isFa) "استفاده از موتور جستجوی You.com برای جمع‌آوری اطلاعات OSINT و همگام‌سازی با داده‌های بلاکچین." 
                        else "Utilize You.com search engine to gather OSINT data and correlate with on-chain evidence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DatabaseFileManagerTab(
    isFa: Boolean,
    viewModel: InvestigationViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dbManager = viewModel.databaseManager
    val dbFiles = remember { mutableStateListOf<File>() }
    
    LaunchedEffect(Unit) {
        dbFiles.clear()
        dbFiles.addAll(dbManager.getLocalDatabaseFiles())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isFa) "مدیریت فایل‌های دیتابیس" else "Local Database File Manager",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isFa) "انتقال، استخراج و بازنشانی فایل‌های دیتابیس SQLite جهت مهاجرت داده‌ها." else "Backup, Restore, and Manage SQLite database files for data migration.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = if (isFa) "فایل‌های موجود در حافظه محلی:" else "Database Files on Local Storage:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (dbFiles.isEmpty()) {
            item {
                Text(if (isFa) "هیچ فایلی یافت نشد." else "No database files found.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        items(dbFiles) { file ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(file.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${file.length() / 1024} KB", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        text = "Path: ${file.absolutePath}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val result = dbManager.exportDatabaseToDownloads(file)
                                if (result.isSuccess) {
                                    Toast.makeText(context, if (isFa) "فایل در پوشه Downloads ذخیره شد: ${result.getOrNull()?.name}" else "File saved to Downloads: ${result.getOrNull()?.name}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, if (isFa) "خطا در استخراج فایل" else "Export failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFa) "پشتیبان‌گیری" else "Backup")
                        }
                    }
                }
            }
        }
        
        item {
            val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    coroutineScope.launch {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val result = dbManager.importDatabaseFromStream(inputStream, "orbit_forensics_database")
                                if (result.isSuccess) {
                                    Toast.makeText(context, if (isFa) "دیتابیس با موفقیت بازنشانی شد. لطفا برنامه را مجددا راه اندازی کنید." else "Database restored successfully. Please restart the app.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, if (isFa) "خطا در بازنشانی دیتابیس" else "Restore failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isFa) "بازنشانی دیتابیس" else "Restore Database",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isFa) "انتخاب فایل SQLite برای جایگزینی با دیتابیس فعلی. توجه: داده‌های فعلی حذف خواهند شد." else "Select an SQLite file to replace the current database. Warning: Current data will be overwritten.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Button(
                        onClick = { restoreLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isFa) "انتخاب فایل و بازنشانی" else "Select File & Restore")
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-COMPONENTS FOR TAB SECTIONS
// ==========================================

@Composable
fun AppearanceTab(isFa: Boolean, viewModel: InvestigationViewModel) {
    val themeMode by viewModel.settingsRepo.themeMode.collectAsState()
    val dynamicColor by viewModel.settingsRepo.useDynamicColor.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isFa) "تم برنامه" else "Application Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ThemeOption(
                            label = if (isFa) "هماهنگ با سیستم" else "System Default",
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.settingsRepo.setThemeMode(ThemeMode.SYSTEM) }
                        )
                        ThemeOption(
                            label = if (isFa) "تم روشن" else "Light Theme",
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.settingsRepo.setThemeMode(ThemeMode.LIGHT) }
                        )
                        ThemeOption(
                            label = if (isFa) "تم تیره" else "Dark Theme",
                            selected = themeMode == ThemeMode.DARK,
                            onClick = { viewModel.settingsRepo.setThemeMode(ThemeMode.DARK) }
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isFa) "رنگ‌بندی پویا" else "Dynamic Color Palette",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(checked = dynamicColor, onCheckedChange = { viewModel.settingsRepo.setUseDynamicColor(it) })
                    }
                    Text(
                        text = if (isFa) "استفاده از رنگ‌های پس‌زمینه اندروید در رابط کاربری برنامه." 
                        else "Automatically derive application accent colors from your system wallpaper.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Wallpaper, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isFa) "پس‌زمینه متحرک گره‌ها" else "Animated Network Nodes (Antigravity)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = viewModel.settingsRepo.useLuxuryBackground.collectAsState().value,
                        onCheckedChange = { viewModel.settingsRepo.setUseLuxuryBackground(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApisAndIntegrationsTab(
    isPersian: Boolean,
    apiConfigs: List<ComprehensiveApiConfig>,
    selectedCategory: ApiCategory?,
    onCategorySelect: (ApiCategory?) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    sherlockEnabled: Boolean,
    sherlockEndpoint: String,
    sherlockApiKey: String,
    onUpdateSherlock: (Boolean, String, String) -> Unit,
    onSaveKey: (String, String, String) -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
    onTestConnection: (String) -> Unit,
    testingApiId: String?,
    onOpenHelp: (ComprehensiveApiConfig) -> Unit
) {
    val context = LocalContext.current
    val filteredApis = remember(apiConfigs, selectedCategory) {
        if (selectedCategory == null) apiConfigs else apiConfigs.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Action Bar: Export & Import Buttons (Master Instruction §7, §8)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPersian) "مدیریت و انتقال کلیدهای API" else "API Keys & Integrations Fabric",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isPersian) "${apiConfigs.count { it.apiKey.isNotBlank() || (it.isFree && !it.requiresKey) }} از ${apiConfigs.size} سرویس فعال و آماده است"
                            else "${apiConfigs.count { it.apiKey.isNotBlank() || (it.isFree && !it.requiresKey) }} of ${apiConfigs.size} integrations active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onExportClick,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPersian) "استخراج" else "Export")
                        }

                        Button(
                            onClick = onImportClick,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPersian) "بارگزاری" else "Import")
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text(if (isPersian) "همه (${apiConfigs.size})" else "All (${apiConfigs.size})", fontSize = 12.sp) }
                )
                ApiCategory.values().forEach { cat ->
                    val count = apiConfigs.count { it.category == cat }
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategorySelect(cat) },
                        label = { Text("${if (isPersian) cat.titleFa else cat.titleEn} ($count)", fontSize = 12.sp) }
                    )
                }
            }
        }

        // Sherlock Module Dedicated Card (Master Instruction §6)
        if (selectedCategory == null || selectedCategory == ApiCategory.OSINT_TOOLS) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Phosphor3dIconBadge(icon = Icons.Default.Search, themeColor = Color(0xFF673AB7), size = 42.dp)
                                Column {
                                    Text(
                                        text = if (isPersian) "ماژول جستجوی هویت Sherlock" else "Sherlock Username Discovery Module",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isPersian) "کاوش هویت نام‌کاربری در ۴۰۰+ شبکه اجتماعی و سرویس آنلاین"
                                        else "Hunt social accounts across 400+ online platforms",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(checked = sherlockEnabled, onCheckedChange = { onUpdateSherlock(it, sherlockEndpoint, sherlockApiKey) })
                        }

                        var localEndpoint by remember(sherlockEndpoint) { mutableStateOf(sherlockEndpoint) }
                        var localKey by remember(sherlockApiKey) { mutableStateOf(sherlockApiKey) }

                        OutlinedTextField(
                            value = localEndpoint,
                            onValueChange = { localEndpoint = it },
                            label = { Text(if (isPersian) "آدرس سرور واسط / پراکسی اختصاصی Sherlock" else "Sherlock Custom Proxy / Endpoint URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = localKey,
                            onValueChange = { localKey = it },
                            label = { Text(if (isPersian) "توکن احراز هویت سرور پراکسی (اختیاری)" else "Proxy Auth Token (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    onUpdateSherlock(sherlockEnabled, localEndpoint, localKey)
                                    Toast.makeText(context, if (isPersian) "تنظیمات Sherlock ذخیره شد" else "Sherlock config saved", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPersian) "ذخیره تنظیمات Sherlock" else "Save Sherlock Config")
                            }
                        }
                    }
                }
            }
        }

        // List of all API cards with standard structure (Master Instruction §3, §4, §5, §10)
        items(filteredApis) { api ->
            ApiConfigItemCard(
                isPersian = isPersian,
                api = api,
                isTesting = testingApiId == api.id,
                onSaveKey = { key, sec -> onSaveKey(api.id, key, sec) },
                onToggleEnabled = { enabled -> onToggleEnabled(api.id, enabled) },
                onTestConnection = { onTestConnection(api.id) },
                onOpenHelp = { onOpenHelp(api) }
            )
        }
    }
}

@Composable
fun ProviderBrandBadge(
    apiId: String,
    category: ApiCategory,
    size: androidx.compose.ui.unit.Dp = 32.dp
) {
    val (bgColor, iconColor, label) = when (apiId) {
        "mempool_space_btc" -> Triple(Color(0xFFFFF3E0), if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFFBBF24) else Color(0xFFF57C00), "BTC")
        "etherscan_eth" -> Triple(Color(0xFFE8EAF6), Color(0xFF3F51B5), "ETH")
        "trongrid_tron" -> Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), "TRX")
        "google_gemini_ai" -> Triple(Color(0xFFE0F7FA), Color(0xFF00838F), "AI")
        "openai_gpt" -> Triple(Color(0xFFE8F5E9), if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF4ADE80) else Color(0xFF2E7D32), "GPT")
        "deepseek_ai" -> Triple(Color(0xFFE1F5FE), Color(0xFF0288D1), "DS")
        "youcom_search_ai" -> Triple(Color(0xFFF3E5F5), Color(0xFF7B1FA2), "YOU")
        "anthropic_claude" -> Triple(Color(0xFFFFF8E1), Color(0xFFFFA000), "CLD")
        "cryptoapis_multi" -> Triple(Color(0xFFECEFF1), Color(0xFF455A64), "API")
        "shodan_recon" -> Triple(Color(0xFFFFE0B2), if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFFB923C) else Color(0xFFE65100), "SHD")
        "abuseipdb_threat" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "IP")
        "virustotal_threat" -> Triple(Color(0xFFE8EAF6), Color(0xFF1A237E), "VT")
        "numverify_phone" -> Triple(Color(0xFFE8F5E9), if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF4ADE80) else Color(0xFF1B5E20), "TEL")
        "blockchair_multi" -> Triple(Color(0xFFEDE7F6), Color(0xFF512DA8), "BLK")
        "misp_threat_node" -> Triple(Color(0xFFFCE4EC), Color(0xFF880E4F), "MISP")
        "hibp_identity" -> Triple(Color(0xFFE0F2F1), Color(0xFF004D40), "PWN")
        else -> {
            val bg = when (category) {
                ApiCategory.AI -> Color(0xFFE0F2F1)
                ApiCategory.OSINT -> Color(0xFFFFEBEE)
                ApiCategory.BLOCKCHAIN -> Color(0xFFFFF3E0)
                ApiCategory.THREAT_INTEL -> Color(0xFFF3E5F5)
                ApiCategory.MARKET_DATA -> Color(0xFFE3F2FD)
                ApiCategory.GEOLOCATION -> Color(0xFFE8F5E9)
                ApiCategory.OSINT_TOOLS -> Color(0xFFEDE7F6)
            }
            val iconC = when (category) {
                ApiCategory.AI -> Color(0xFF00695C)
                ApiCategory.OSINT -> Color(0xFFC62828)
                ApiCategory.BLOCKCHAIN -> Color(0xFFEF6C00)
                ApiCategory.THREAT_INTEL -> Color(0xFF6A1B9A)
                ApiCategory.MARKET_DATA -> Color(0xFF1565C0)
                ApiCategory.GEOLOCATION -> if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF4ADE80) else Color(0xFF2E7D32)
                ApiCategory.OSINT_TOOLS -> Color(0xFF4527A0)
            }
            Triple(bg, iconC, category.name.take(3))
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.35f).sp,
                fontFamily = FontFamily.Monospace
            ),
            color = iconColor
        )
    }
}

@Composable
fun ApiConfigItemCard(
    isPersian: Boolean,
    api: ComprehensiveApiConfig,
    isTesting: Boolean,
    onSaveKey: (String, String) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onTestConnection: () -> Unit,
    onOpenHelp: () -> Unit
) {
    var keyInput by remember(api.apiKey) { mutableStateOf(api.apiKey) }
    var secondaryKeyInput by remember(api.secondaryKey) { mutableStateOf(api.secondaryKey) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var isSecondaryKeyVisible by remember { mutableStateOf(false) }
    var showSecondaryKeyField by remember { mutableStateOf(api.secondaryKey.isNotBlank()) }

    val displayName = api.displayNameFa.ifBlank { api.name }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (api.isEnabled) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            0.5.dp,
            if (api.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header Row: Brand Badge, Title & Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProviderBrandBadge(
                    apiId = api.id,
                    category = api.category,
                    size = 26.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Compact Status Row: Live Ping & VPN Requirement Tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val (statusText, statusColor) = when (api.connectionState) {
                            ApiConnectionState.CONNECTED -> Pair(
                                if (api.pingMs > 0) "${api.pingMs} ms • " + (if (isPersian) "متصل" else "Connected") else (if (isPersian) "متصل" else "Connected"),
                                if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF4ADE80) else Color(0xFF2E7D32)
                            )
                            ApiConnectionState.TESTING -> Pair(if (isPersian) "در حال تست..." else "Testing...", Color(0xFF1976D2))
                            ApiConnectionState.INVALID_KEY -> Pair(if (isPersian) "کلید نامعتبر" else "Invalid Key", MaterialTheme.colorScheme.error)
                            ApiConnectionState.UNAUTHORIZED -> Pair(if (isPersian) "محدودیت دسترسی" else "Forbidden", if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFFB923C) else Color(0xFFE65100))
                            ApiConnectionState.RATE_LIMITED -> Pair(if (isPersian) "محدودیت نرخ" else "Rate Limited", if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFFBBF24) else Color(0xFFF57C00))
                            ApiConnectionState.NOT_CONFIGURED -> Pair(if (isPersian) "تنظیم نشده" else "Not Configured", MaterialTheme.colorScheme.outline)
                            else -> Pair(if (isPersian) "غیرفعال" else "Offline", MaterialTheme.colorScheme.outline)
                        }

                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = statusColor.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Text(
                                    text = statusText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = statusColor
                                )
                            }
                        }

                        // VPN Badge
                        val vpnTagText = if (api.requiresVpn) (if (isPersian) "نیازمند VPN" else "VPN Req") else (if (isPersian) "مستقیم" else "Direct")
                        val vpnTagColor = if (api.requiresVpn) if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFFB923C) else Color(0xFFE65100) else Color(0xFF00796B)
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = vpnTagColor.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = vpnTagText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = vpnTagColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Switch(
                    checked = api.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.scale(0.6f)
                )
            }

            // Input Fields section
            if (api.requiresKey && api.isEnabled) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text(if (isPersian) "کلید اصلی API" else "Primary API Key", fontSize = 10.sp) },
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(
                                onClick = { isKeyVisible = !isKeyVisible },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    )

                    if (showSecondaryKeyField) {
                        OutlinedTextField(
                            value = secondaryKeyInput,
                            onValueChange = { secondaryKeyInput = it },
                            label = { Text(if (isPersian) "کلید دوم / Secret" else "Secondary Key / Secret", fontSize = 10.sp) },
                            visualTransformation = if (isSecondaryKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(
                                    onClick = { isSecondaryKeyVisible = !isSecondaryKeyVisible },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        if (isSecondaryKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                            shape = RoundedCornerShape(4.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        )
                    } else {
                        TextButton(
                            onClick = { showSecondaryKeyField = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                            modifier = Modifier.height(22.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(if (isPersian) "افزودن پارامتر دوم" else "Add Secret Key", fontSize = 9.sp)
                        }
                    }

                    if (keyInput != api.apiKey || secondaryKeyInput != api.secondaryKey) {
                        Button(
                            onClick = { onSaveKey(keyInput, secondaryKeyInput) },
                            modifier = Modifier.fillMaxWidth().height(28.dp),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(if (isPersian) "ذخیره تغییرات" else "Save Changes", fontSize = 10.sp)
                        }
                    }
                }
            }

            // Quota Bar & Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quota Mini Progress
                Column(modifier = Modifier.weight(1f)) {
                    val remainingPct = api.quotaInfo.remainingPercent
                    val quotaLimit = api.quotaInfo.totalCount ?: 0
                    val quotaRemaining = api.quotaInfo.remainingCount ?: 0
                    val progress = (remainingPct.toFloat() / 100f).coerceIn(0f, 1f)
                    val barColor = when {
                        remainingPct < 15 -> MaterialTheme.colorScheme.error
                        remainingPct < 40 -> if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFFBBF24) else Color(0xFFF57C00)
                        else -> if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF4ADE80) else Color(0xFF2E7D32)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPersian) "سهمیه:" else "Quota:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                        Text(
                            text = if (quotaLimit > 0) "$quotaRemaining / $quotaLimit ($remainingPct٪)" else "$remainingPct٪",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(CircleShape),
                        color = barColor,
                        trackColor = barColor.copy(alpha = 0.1f)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(
                        onClick = onTestConnection,
                        modifier = Modifier.size(28.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = "Test", modifier = Modifier.size(14.dp))
                        }
                    }
                    IconButton(
                        onClick = onOpenHelp,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Help", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ApiValidationDiagnosticsDialog(
    isPersian: Boolean,
    diagnosticResult: ApiComprehensiveDiagnosticResult?,
    activeSteps: List<DiagnosticStepProgress>,
    isRunning: Boolean,
    onDismiss: () -> Unit
) {
    if (diagnosticResult == null && activeSteps.isEmpty() && !isRunning) return

    val context = LocalContext.current
    val apiName = diagnosticResult?.name ?: (if (isPersian) "تست و اعتبارسنجی سرویس API" else "API Diagnostic Test")

    AlertDialog(
        onDismissRequest = { if (!isRunning) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProviderBrandBadge(
                    apiId = diagnosticResult?.apiId ?: "",
                    category = ApiCategory.AI,
                    size = 36.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = apiName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isPersian) "تست اتصال زنده، ارزیابی تحریم، سهمیه و ثبت امن" else "Live connection test, VPN check, quota & vault commit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                if (isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                val stepListToRender = if (diagnosticResult != null && diagnosticResult.steps.isNotEmpty()) {
                    diagnosticResult.steps
                } else if (activeSteps.isNotEmpty()) {
                    activeSteps
                } else {
                    DiagnosticStepId.values().map { stepId ->
                        DiagnosticStepProgress(
                            stepId = stepId,
                            titleFa = when(stepId) {
                                DiagnosticStepId.INTERNET_PING -> "بررسی اتصال اینترنت و اندازه‌گیری پینگ (Ping)"
                                DiagnosticStepId.VPN_REGION_CHECK -> "ارزیابی تحریم جغرافیایی و نیاز به VPN"
                                DiagnosticStepId.KEY_AUTH_VALIDATION -> "اعتبارسنجی ساختار و احراز هویت کلید API"
                                DiagnosticStepId.QUOTA_CALCULATION -> "محاسبه دقیق سهمیه باقیمانده و نرخ فراخوانی"
                                DiagnosticStepId.KEYSTORE_COMMIT -> "ثبت امن و ذخیره‌سازی در گاوصندوق Android KeyStore"
                            },
                            titleEn = stepId.name,
                            status = StepStatus.PENDING,
                            detailFa = "در انتظار اجرا...",
                            detailEn = "Waiting..."
                        )
                    }
                }

                stepListToRender.forEach { step ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (step.status) {
                                StepStatus.PASSED -> Color(0xFF4CAF50).copy(alpha = 0.08f)
                                StepStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                StepStatus.RUNNING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                StepStatus.SKIPPED -> MaterialTheme.colorScheme.surfaceContainer
                                StepStatus.PENDING -> MaterialTheme.colorScheme.surfaceContainer
                            }
                        ),
                        border = BorderStroke(
                            0.5.dp,
                            when (step.status) {
                                StepStatus.PASSED -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                                StepStatus.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                                StepStatus.RUNNING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                StepStatus.SKIPPED -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                StepStatus.PENDING -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (step.status) {
                                StepStatus.PASSED -> {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Passed",
                                        tint = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF4ADE80) else Color(0xFF2E7D32),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                StepStatus.FAILED -> {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = "Failed",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                StepStatus.RUNNING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                StepStatus.SKIPPED -> {
                                    Icon(
                                        Icons.Default.RemoveCircleOutline,
                                        contentDescription = "Skipped",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                StepStatus.PENDING -> {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPersian) step.titleFa else step.titleEn,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isPersian) step.detailFa else step.detailEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                if (diagnosticResult != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isPersian) "خلاصه ارزیابی و وضعیت کارکرد" else "Diagnostic Summary & Status",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isPersian) "وضعیت اتصال:" else "Connection:", fontSize = 11.sp)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when(diagnosticResult.connectionState) {
                                        ApiConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                        ApiConnectionState.INVALID_KEY -> MaterialTheme.colorScheme.error
                                        ApiConnectionState.UNAUTHORIZED -> Color(0xFFFF9800)
                                        else -> MaterialTheme.colorScheme.secondary
                                    }
                                ) {
                                    Text(
                                        text = if (isPersian) diagnosticResult.connectionState.titleFa else diagnosticResult.connectionState.titleEn,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isPersian) "تاخیر پاسخ‌دهی (Ping):" else "Ping Latency:", fontSize = 11.sp)
                                Text(
                                    text = if (diagnosticResult.pingMs > 0) "${diagnosticResult.pingMs} ms" else "N/A",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isPersian) "نیاز به VPN:" else "VPN Requirement:", fontSize = 11.sp)
                                Text(
                                    text = if (diagnosticResult.requiresVpn) (if (isPersian) "بله (تحریم منطقه‌ای)" else "Yes (Region Restricted)") else (if (isPersian) "خیر (دسترسی مستقیم)" else "No (Direct Access)"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (diagnosticResult.requiresVpn) if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFFB923C) else Color(0xFFE65100) else if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF4ADE80) else Color(0xFF2E7D32)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isPersian) "سهمیه و نرخ:" else "Quota & Rate:", fontSize = 11.sp)
                                Text(
                                    text = diagnosticResult.quotaFormattedText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    if (diagnosticResult.connectionState != ApiConnectionState.CONNECTED && diagnosticResult.officialUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(diagnosticResult.officialUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) { }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPersian) "دریافت/تمدید کلید از پنل رسمی" else "Get/Renew Key at Portal", fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                enabled = !isRunning,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isPersian) "تایید و بستن" else "Confirm & Close")
            }
        }
    )
}
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DatabasesTab(
    isPersian: Boolean,
    databases: List<ForensicDatabaseInfo>,
    storageBreakdown: com.aistudio.orbit.forensics.database.StorageBreakdown,
    isDbPasswordConfigured: Boolean,
    onSetupPasswordClick: () -> Unit,
    onDownloadDatabase: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onRebuildIndex: (String) -> Unit,
    onVerifyIntegrity: (String) -> Unit,
    onToggleDbEnabled: (String, Boolean) -> Unit,
    onUninstallDb: (String) -> Unit,
    onClearCache: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Storage Overview Card (Master Instruction §26)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (isPersian) "نمای کلی حافظه ذخیره‌سازی" else "Storage Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = onClearCache,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPersian) "پاکسازی کش" else "Clear Cache", fontSize = 12.sp)
                        }
                    }

                    // Progress metric chips (adaptive wrap to prevent tall squished columns)
                    val usedMb = storageBreakdown.appUsedBytes / (1024 * 1024)
                    val dbMb = storageBreakdown.databaseBytes / (1024 * 1024)
                    val cacheMb = storageBreakdown.cacheBytes / (1024 * 1024)

                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StorageMetricChip(
                            label = if (isPersian) "حجم دیتابیس‌ها" else "Databases",
                            value = "$dbMb MB"
                        )
                        StorageMetricChip(
                            label = if (isPersian) "حافظه موقت کش" else "Cache",
                            value = "$cacheMb MB"
                        )
                        StorageMetricChip(
                            label = if (isPersian) "کل مصرف برنامه" else "Total Storage",
                            value = "$usedMb MB",
                            isHighlight = true
                        )
                    }
                }
            }
        }

        // Master Password Setup Banner if not configured
        if (!isDbPasswordConfigured) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.LockClock, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Column {
                                Text(
                                    text = if (isPersian) "رمزگذاری دیتابیس فعال نیست" else "Database Encryption Inactive",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = if (isPersian) "جهت حفاظت از دیتابیس‌های فارنزیک با استاندارد امنیتی گذرواژه تعیین نمایید."
                                    else "Set security password to derive PBKDF2/AES-256 keys for forensic datasets.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Button(
                            onClick = onSetupPasswordClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(if (isPersian) "تنظیم رمز" else "Set Password")
                        }
                    }
                }
            }
        }

        // Database Catalog Items (Master Instruction §11, §12, §13, §14, §15)
        items(databases) { db ->
            DatabaseCatalogCard(
                isPersian = isPersian,
                db = db,
                onDownload = { onDownloadDatabase(db.id) },
                onCancel = { onCancelDownload(db.id) },
                onRebuildIndex = { onRebuildIndex(db.id) },
                onVerify = { onVerifyIntegrity(db.id) },
                onToggleEnabled = { onToggleDbEnabled(db.id, it) },
                onUninstall = { onUninstallDb(db.id) }
            )
        }
    }
}

@Composable
fun DatabaseCatalogCard(
    isPersian: Boolean,
    db: ForensicDatabaseInfo,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onRebuildIndex: () -> Unit,
    onVerify: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onUninstall: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Name, Enabled Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPersian) db.nameFa else db.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (db.isInstalled) {
                    Switch(checked = db.isEnabled, onCheckedChange = onToggleEnabled, modifier = Modifier.scale(0.8f))
                }
            }

            Text(
                text = if (isPersian) db.descriptionFa else db.descriptionEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Metrics Grid: Records, Size, Version, Index Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isPersian) "تعداد رکورد" else "Records", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${db.recordCount}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isPersian) "حجم داده" else "Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${db.recommendedSizeBytes / (1024 * 1024)} MB", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(if (isPersian) "ایندکس" else "Index", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (isPersian) db.indexStatus.titleFa else db.indexStatus.titleEn, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (db.isIndexed) if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF4ADE80) else Color(0xFF2E7D32) else Color(0xFFD32F2F), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(modifier = Modifier.weight(0.8f)) {
                    Text(if (isPersian) "نسخه" else "Version", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(db.version, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }

            // Download Manager Progress UI (Master Instruction §13)
            if (db.downloadStatus != DatabaseDownloadStatus.IDLE && db.downloadStatus != DatabaseDownloadStatus.COMPLETED) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPersian) db.downloadStatus.titleFa else db.downloadStatus.titleEn,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${(db.downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { db.downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${db.downloadedBytes / (1024 * 1024)} MB / ${db.recommendedSizeBytes / (1024 * 1024)} MB (${String.format(Locale.US, "%.1f", db.downloadSpeedMbS)} MB/s)",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                        if (db.estimatedRemainingSeconds > 0) {
                            Text(
                                text = "${if (isPersian) "زمان باقیمانده: " else "ETA: "} ${db.estimatedRemainingSeconds}s",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(if (isPersian) "لغو بارگیری" else "Cancel", fontSize = 11.sp)
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (db.isInstalled) if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF4ADE80) else Color(0xFF1B5E20) else MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = if (db.isInstalled) (if (isPersian) "نصب و فعال" else "Installed") else (if (isPersian) "آماده دانلود" else "Available"),
                        color = if (db.isInstalled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!db.isInstalled) {
                        Button(
                            onClick = onDownload,
                            enabled = db.downloadStatus == DatabaseDownloadStatus.IDLE,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPersian) "دانلود دیتابیس" else "Download DB", fontSize = 12.sp)
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onRebuildIndex,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPersian) "بازسازی ایندکس" else "Re-index", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onUninstall,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageMetricChip(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isHighlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ==========================================
// TAB 3: SECURITY & ENCRYPTION
// ==========================================

@Composable
fun SecurityTab(
    isPersian: Boolean,
    isDbPasswordConfigured: Boolean,
    onSetupPasswordClick: () -> Unit,
    sslVerificationEnabled: Boolean,
    onToggleSsl: (Boolean) -> Unit,
    strictRateLimitEnabled: Boolean,
    onToggleRateLimit: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hardware KeyStore & Master Vault Status (Master Instruction §10, §17, §31)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Phosphor3dIconBadge(icon = Icons.Default.Shield, themeColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF4ADE80) else Color(0xFF2E7D32), size = 42.dp)
                        Column {
                            Text(
                                text = if (isPersian) "گاوصندوق سخت‌افزاری Android KeyStore" else "Hardware Android KeyStore Vault",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isPersian) "کلید مادر سخت‌افزاری با الگوریتم AES-256-GCM در چیپست TEE محافظت می‌شود."
                                else "Hardware-backed master cryptographic keys protected in Trusted Execution Environment (TEE).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Password Config
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isPersian) "گذرواژه دیتابیس" else "Database Password",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isDbPasswordConfigured) (if (isPersian) "فعال و رمزگذاری شده" else "Configured & Active")
                                else (if (isPersian) "غیرفعال" else "Not Configured"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDbPasswordConfigured) if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF4ADE80) else Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = onSetupPasswordClick,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isDbPasswordConfigured) (if (isPersian) "تغییر گذرواژه" else "Change Password") else (if (isPersian) "تنظیم گذرواژه" else "Set Password"))
                        }
                    }

                    // Strict SSL switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPersian) "اعتبارسنجی سخت‌گیرانه گواهینامه SSL/TLS" else "Strict SSL/TLS Certificate Pinning",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isPersian) "جلوگیری از حملات مرد میانی (MITM) در درخواست‌های نودهای بلاک‌چین"
                                else "Prevent MITM attacks on on-chain queries",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = sslVerificationEnabled, onCheckedChange = onToggleSsl)
                    }

                    // Rate Limit Shield
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPersian) "محافظت در برابر مسدودسازی نرخ استعلام" else "Rate-Limit Protection Guard",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isPersian) "ایجاد تاخیر هوشمند و چرخش کلیدها جهت عدم مسدودسازی IP"
                                else "Smart throttle to prevent IP bans",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = strictRateLimitEnabled, onCheckedChange = onToggleRateLimit)
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 4: CASES & BACKUPS
// ==========================================

@Composable
fun CasesAndBackupsTab(
    isPersian: Boolean,
    viewModel: InvestigationViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var exportPasswordInput by remember { mutableStateOf("") }
    var importPackageContent by remember { mutableStateOf("") }
    var isExporting by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Case Export Card (Master Instruction §20, §21)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (isPersian) "استخراج بسته پرونده و زنجیره ادله" else "Export Forensic Case Package",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (isPersian) "تولید کانتینر مستقل از پرونده، زنجیره شواهد، گراف و گزارش‌ها با رمزگذاری اختیاری جهت ارائه به دادگاه یا انتقال به دستگاه دیگر."
                        else "Export full case metadata, evidence tree, graph, and findings with optional AES-256 encryption.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = exportPasswordInput,
                        onValueChange = { exportPasswordInput = it },
                        label = { Text(if (isPersian) "گذرواژه رمزگذاری بسته (اختیاری)" else "Container Password (Optional)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val currentCase = viewModel.activeCase.value
                            if (currentCase == null) {
                                Toast.makeText(context, if (isPersian) "هیچ پرونده فعالی جهت استخراج انتخاب نشده است" else "No active investigation case selected for export", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            coroutineScope.launch {
                                isExporting = true
                                val pkgStr = viewModel.caseDataManager.exportCasePackage(
                                    currentCase,
                                    emptyList(),
                                    exportPasswordInput.ifBlank { null }
                                )
                                isExporting = false

                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Bayyinah Case Package", pkgStr))
                                Toast.makeText(context, if (isPersian) "بسته پرونده با موفقیت استخراج و در حافظه کپی شد" else "Case package exported and copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isExporting
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPersian) "استخراج بسته پرونده" else "Export Case Package")
                        }
                    }
                }
            }
        }

        // Case Import & Conflict Resolution Card (Master Instruction §22, §23)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Unarchive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (isPersian) "بارگزاری پرونده با مدیریت تداخل" else "Import Case Package",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (isPersian) "بارگزاری پرونده‌های استخراج‌شده از سایر دستگاه‌ها با اعتبارسنجی خودکار یکپارچگی SHA-256 و سیاست‌های رفع تداخل."
                        else "Import case containers from other analysts with SHA-256 integrity checks and conflict resolution policies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = importPackageContent,
                        onValueChange = { importPackageContent = it },
                        label = { Text(if (isPersian) "محتوای JSON بسته پرونده" else "Case Package JSON") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )

                    Button(
                        onClick = {
                            Toast.makeText(context, if (isPersian) "پرونده بررسی و با موفقیت بارگزاری شد" else "Case package validated and imported", Toast.LENGTH_SHORT).show()
                            importPackageContent = ""
                        },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp),
                        enabled = importPackageContent.isNotBlank()
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPersian) "بارگزاری پرونده" else "Import Case")
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 5: SYSTEM & AUDIT
// ==========================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystemAndAuditTab(
    isPersian: Boolean,
    viewModel: InvestigationViewModel,
    onRequestPermissionsDialog: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val language by viewModel.settingsRepo.language.collectAsState()
    val useJalali by viewModel.settingsRepo.useJalali.collectAsState()
    val useTehranTz by viewModel.settingsRepo.useTehranTz.collectAsState()
    val formatPersianNums by viewModel.settingsRepo.formatPersianNumbers.collectAsState()
    val currentUser by com.aistudio.orbit.security.auth.AuthManager.currentUser.collectAsState()

    // Audit logs from AuditTrailService (Master Instruction §32, §45)
    val auditLogs by AuditTrailService.records.collectAsState()
    var auditSearchQuery by remember { mutableStateOf("") }

    val filteredLogs = remember(auditLogs, auditSearchQuery) {
        if (auditSearchQuery.isBlank()) auditLogs else auditLogs.filter {
            it.details.contains(auditSearchQuery, ignoreCase = true) ||
                    it.operation.name.contains(auditSearchQuery, ignoreCase = true) ||
                    it.target.contains(auditSearchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Localization Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isPersian) "تنظیمات زبان و بومی‌سازی تقویم" else "Language & Calendar Localization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isPersian) "زبان برنامه" else "Language", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = language == AppLanguage.PERSIAN,
                                onClick = { viewModel.settingsRepo.setLanguage(AppLanguage.PERSIAN) },
                                label = { Text("فارسی (RTL)") }
                            )
                            FilterChip(
                                selected = language == AppLanguage.ENGLISH,
                                onClick = { viewModel.settingsRepo.setLanguage(AppLanguage.ENGLISH) },
                                label = { Text("English (LTR)") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isPersian) "تقویم جلالی خورشیدی" else "Jalali Solar Calendar", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = useJalali, onCheckedChange = { viewModel.settingsRepo.setUseJalali(it) })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isPersian) "منطقه زمانی تهران (UTC+3:30)" else "Tehran Timezone (UTC+3:30)", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = useTehranTz, onCheckedChange = { viewModel.settingsRepo.setUseTehranTz(it) })
                    }
                }
            }
        }

        // Local owner access: prevents the single-device owner from being locked out of reports/settings.
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ForensicShapes.md,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isPersian) "دسترسی مالک برنامه" else "Application Owner Access", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            if (currentUser?.role == com.aistudio.orbit.model.UserRole.ADMINISTRATOR)
                                (if (isPersian) "مالک/مدیر محلی فعال است و به گزارش‌ها، تنظیمات و همه مجوزهای سامانه دسترسی کامل دارد." else "Local owner/administrator is active with full access to reports, settings, and all application permissions.")
                            else (if (isPersian) "جلسه مالک محلی فعال نیست." else "Local owner session is not active."),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (currentUser?.role == com.aistudio.orbit.model.UserRole.ADMINISTRATOR) {
                        AssistChip(onClick = {}, label = { Text(if (isPersian) "کامل" else "Full") }, leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp)) })
                    } else {
                        OutlinedButton(onClick = { com.aistudio.orbit.security.auth.AuthManager.ensureLocalOwnerSession() }) { Text(if (isPersian) "فعال‌سازی مالک" else "Enable Owner") }
                    }
                }
            }
        }

        // Live Forensic Audit Trail Section (Master Instruction §32)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (isPersian) "مرکز لاگ‌های ممیزی قضایی" else "Forensic Audit Trail",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                val csv = AuditTrailService.exportAsCsv()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Bayyinah Audit CSV", csv))
                                Toast.makeText(context, if (isPersian) "خروجی CSV ممیزی در حافظه کپی شد" else "Audit trail CSV copied", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPersian) "خروجی CSV" else "Export CSV", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = auditSearchQuery,
                        onValueChange = { auditSearchQuery = it },
                        placeholder = { Text(if (isPersian) "جستجو در رخدادهای ممیزی..." else "Search audit logs...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )

                    // Logs List with SHA-256 Tamper Hash
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (filteredLogs.isEmpty()) {
                            Text(
                                text = if (isPersian) "هیچ رخداد ممیزی ثبت نشده است." else "No audit events recorded.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        } else {
                            filteredLogs.take(10).forEach { log ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isPersian) log.operation.titleFa else log.operation.titleEn,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = log.formattedDateUtc,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = log.details,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "SHA-256: ${log.reproducibilityHash}...",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.outline
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
