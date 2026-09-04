package com.aistudio.orbit.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.PdfReportExporter
import com.aistudio.orbit.forensics.osint.ForensicToolController
import com.aistudio.orbit.forensics.osint.InvestigationPipelineEngine
import com.aistudio.orbit.model.*
import com.aistudio.orbit.provider.osint.ProviderHealthStatus
import com.aistudio.orbit.repository.AppLanguage
import com.aistudio.orbit.ui.InvestigationViewModel
import com.aistudio.orbit.ui.components.AdaptiveScaffold
import com.aistudio.orbit.ui.components.WindowWidthSizeClass
import com.aistudio.orbit.ui.components.designsystem.*
import com.aistudio.orbit.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OsintInvestigationView(
    viewModel: InvestigationViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.settingsRepo.language.collectAsState()
    val isFa = language == AppLanguage.PERSIAN
    val scope = rememberCoroutineScope()
    val layoutDirection = if (isFa) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Active Tab in the OSINT Subsystem:
    // 0: Seeds & Pipeline
    // 1: Resolved Entities & Control Model
    // 2: Cross-Source Correlation & Graph
    // 3: Hypotheses & Conflict Resolution
    // 4: Ranked Leads & Next Steps
    // 5: Pluggable Providers & Adapters
    // 6: Verifiable Evidence & Audit Trail
    // 7: AI Copilot & Evidence Citations
    var activeTab by remember { mutableIntStateOf(0) }

    // Multi-Seed input states
    var seedInputText by remember { mutableStateOf("") }
    val classifiedSeedPreview = remember(seedInputText) {
        if (seedInputText.isNotBlank()) {
            InvestigationPipelineEngine.classifyAndNormalizeInput(seedInputText)
        } else null
    }

    val osintSeeds by viewModel.osintSeeds.collectAsState()
    val osintSession by viewModel.osintSession.collectAsState()
    val pluggableProviders by viewModel.pluggableProviders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingMessage by viewModel.loadingMessage.collectAsState()

    // Conflict resolution dialog / state
    var selectedConflictToReview by remember { mutableStateOf<AttributionConflict?>(null) }
    var conflictAnalystNote by remember { mutableStateOf("") }

    // Evidence inspection modal / sheet
    var selectedEvidenceToInspect by remember { mutableStateOf<OsintEvidenceItem?>(null) }

    val useLuxuryBackground = viewModel.settingsRepo.useLuxuryBackground.collectAsState().value

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        AdaptiveScaffold(
            containerColor = if (useLuxuryBackground) Color.Transparent else MaterialTheme.colorScheme.background,
            topBar = {
                Surface(
                    color = if (useLuxuryBackground) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (isFa) "بازگشت" else "Back"
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isFa) "سامانه بیِّنة • موتور OSINT" else "BAYYINAH • OSINT Engine",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isFa) "تطبیق چندمنبعی • انتساب نهاد • زنجیره ادله" else "Attribution & Evidence Trail",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            // Run Full Recon Pipeline Button
                            Button(
                                onClick = { viewModel.runFullOsintPipeline() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isFa) "اجرای پویش" else "Run Pipeline",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues, widthClass ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = if (widthClass == WindowWidthSizeClass.EXPANDED) 28.dp else 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Executive Status Banner & Quick Metrics
                item {
                    val session = osintSession
                    Card(
                        shape = ForensicShapes.lg,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(ForensicSpacing.base),
                            verticalArrangement = Arrangement.spacedBy(ForensicSpacing.md)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ForensicBadge(
                                        text = if (isFa) "موتور ادله‌محور" else "EVIDENCE-DRIVEN",
                                        badgeType = ForensicBadgeType.PRIMARY
                                    )
                                    if (session != null) {
                                        val riskColor = when (session.aggregateRiskCategory) {
                                            BehavioralRiskCategory.SANCTIONS_EXPOSURE -> MaterialTheme.colorScheme.error
                                            BehavioralRiskCategory.MIXER_EXPOSURE, BehavioralRiskCategory.DARKWEB_EXPOSURE -> Color(0xFFE65100)
                                            BehavioralRiskCategory.SCAM_REPORTS -> Color(0xFFF57C00)
                                            BehavioralRiskCategory.POTENTIALLY_SUSPICIOUS -> Color(0xFFFFA000)
                                            else -> Color(0xFF2E7D32)
                                        }
                                        ForensicBadge(
                                            text = if (isFa) session.aggregateRiskCategory.displayNameFa else session.aggregateRiskCategory.displayNameEn,
                                            containerColor = riskColor.copy(alpha = 0.15f),
                                            contentColor = riskColor
                                        )
                                    }
                                }

                                Text(
                                    text = if (session != null) "${if (isFa) "ضریب اطمینان:" else "Confidence:"} ${session.aggregateConfidenceScore}% (${session.aggregateConfidenceLevel})" else if (isFa) "آماده دریافت سرنخ‌ها" else "Awaiting seeds",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Summary Grid
                            if (widthClass == WindowWidthSizeClass.EXPANDED) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ForensicMetricCard(
                                        label = if (isFa) "سرنخ‌های فعال" else "Seeds",
                                        value = "${osintSeeds.size.coerceAtLeast(1)}",
                                        icon = Icons.Default.ScatterPlot,
                                        accentColor = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ForensicMetricCard(
                                        label = if (isFa) "ادله مستند" else "Evidence",
                                        value = "${session?.collectedEvidence?.size ?: 0}",
                                        icon = Icons.Default.FactCheck,
                                        accentColor = Color(0xFF00897B),
                                        modifier = Modifier.weight(1f)
                                    )
                                    ForensicMetricCard(
                                        label = if (isFa) "موجودیت‌ها" else "Entities",
                                        value = "${session?.extractedEntities?.size ?: 0}",
                                        icon = Icons.Default.Groups,
                                        accentColor = Color(0xFF5E35B1),
                                        modifier = Modifier.weight(1f)
                                    )
                                    ForensicMetricCard(
                                        label = if (isFa) "تعارضات" else "Conflicts",
                                        value = "${session?.conflicts?.size ?: 0}",
                                        icon = Icons.Default.WarningAmber,
                                        accentColor = if ((session?.conflicts?.size ?: 0) > 0) MaterialTheme.colorScheme.error else Color(0xFF757575),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ForensicMetricCard(
                                            label = if (isFa) "سرنخ‌های فعال" else "Seeds",
                                            value = "${osintSeeds.size.coerceAtLeast(1)}",
                                            icon = Icons.Default.ScatterPlot,
                                            accentColor = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        ForensicMetricCard(
                                            label = if (isFa) "ادله مستند" else "Evidence",
                                            value = "${session?.collectedEvidence?.size ?: 0}",
                                            icon = Icons.Default.FactCheck,
                                            accentColor = Color(0xFF00897B),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ForensicMetricCard(
                                            label = if (isFa) "موجودیت‌ها" else "Entities",
                                            value = "${session?.extractedEntities?.size ?: 0}",
                                            icon = Icons.Default.Groups,
                                            accentColor = Color(0xFF5E35B1),
                                            modifier = Modifier.weight(1f)
                                        )
                                        ForensicMetricCard(
                                            label = if (isFa) "تعارضات" else "Conflicts",
                                            value = "${session?.conflicts?.size ?: 0}",
                                            icon = Icons.Default.WarningAmber,
                                            accentColor = if ((session?.conflicts?.size ?: 0) > 0) MaterialTheme.colorScheme.error else Color(0xFF757575),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Workspace Navigation - Modern Chip System (Segmented Control replacement)
                item {
                    val tabs = listOf(
                        Triple(0, if (isFa) "سرنخ‌ها" else "Seeds", Icons.Default.Input),
                        Triple(1, if (isFa) "موجودیت‌ها" else "Entities", Icons.Default.AssignmentInd),
                        Triple(2, if (isFa) "گراف" else "Graph", Icons.Default.AccountTree),
                        Triple(3, if (isFa) "فرضیات" else "Hypotheses", Icons.Default.Psychology),
                        Triple(4, if (isFa) "رتبه‌بندی" else "Ranking", Icons.Default.Lightbulb),
                        Triple(5, if (isFa) "سرویس‌ها" else "Providers", Icons.Default.Extension),
                        Triple(6, if (isFa) "ادله" else "Evidence", Icons.Default.VerifiedUser),
                        Triple(7, if (isFa) "هوش مصنوعی" else "AI Co-Pilot", Icons.Default.AutoAwesome)
                    )

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(tabs) { (idx, title, icon) ->
                            FilterChip(
                                selected = activeTab == idx,
                                onClick = { activeTab = idx },
                                label = {
                                    Text(text = title, style = MaterialTheme.typography.labelSmall)
                                },
                                leadingIcon = {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Loading Indicator
                if (isLoading) {
                    item {
                        ForensicUiStateBoundary(
                            state = ForensicUiState.Partial(Unit, loadingMessage.ifBlank { if (isFa) "در حال پردازش پایپ‌لاین OSINT..." else "Running OSINT Reconnaissance Pipeline..." }),
                            successContent = {}
                        )
                    }
                }

                // TAB 0: SEEDS & PIPELINE EXECUTION
                if (activeTab == 0) {
                    item {
                        ForensicCard {
                            ForensicSectionHeader(
                                title = if (isFa) "ورود و مدیریت سرنخ‌های چندگانه تحقیقاتی" else "Investigation Seed Manager",
                                subtitle = if (isFa) "پشتیبانی از آدرس‌های BTC/ETH/TRX، تراکنش، نام کاربری، دامنه، ایمیل، IP، ASN، گیت‌هاب و تلگرام" else "Supports Crypto Addresses, TXIDs, Usernames, Domains, Emails, IPs, ASNs, GitHub & Telegram",
                                icon = Icons.Default.AddLocationAlt
                            )

                            // Responsive seed input: stack controls on narrow screens to prevent
                            // the text field from collapsing into a one-character column.
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val compactInput = maxWidth < 520.dp
                                if (compactInput) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = seedInputText,
                                            onValueChange = { seedInputText = it },
                                            placeholder = {
                                                Text(
                                                    if (isFa) "آدرس یا شناسه عمومی سرنخ" else "Public address or identifier",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = ForensicShapes.md,
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                textDirection = TextDirection.Ltr
                                            ),
                                            trailingIcon = {
                                                if (classifiedSeedPreview != null) {
                                                    ForensicBadge(
                                                        text = if (isFa) classifiedSeedPreview.type.displayNameFa else classifiedSeedPreview.type.displayNameEn,
                                                        badgeType = ForensicBadgeType.INFO,
                                                        modifier = Modifier.padding(end = 4.dp)
                                                    )
                                                }
                                            }
                                        )
                                        Button(
                                            onClick = {
                                                if (seedInputText.isNotBlank()) {
                                                    viewModel.addOsintSeed(seedInputText)
                                                    seedInputText = ""
                                                }
                                            },
                                            shape = ForensicShapes.md,
                                            modifier = Modifier.fillMaxWidth().height(52.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isFa) "افزودن سرنخ" else "Add Seed")
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = seedInputText,
                                            onValueChange = { seedInputText = it },
                                            placeholder = {
                                                Text(
                                                    if (isFa) "آدرس یا شناسه عمومی سرنخ" else "Public address or identifier",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = ForensicShapes.md,
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                textDirection = TextDirection.Ltr
                                            ),
                                            trailingIcon = {
                                                if (classifiedSeedPreview != null) {
                                                    ForensicBadge(
                                                        text = if (isFa) classifiedSeedPreview.type.displayNameFa else classifiedSeedPreview.type.displayNameEn,
                                                        badgeType = ForensicBadgeType.INFO,
                                                        modifier = Modifier.padding(end = 4.dp)
                                                    )
                                                }
                                            }
                                        )
                                        Button(
                                            onClick = {
                                                if (seedInputText.isNotBlank()) {
                                                    viewModel.addOsintSeed(seedInputText)
                                                    seedInputText = ""
                                                }
                                            },
                                            shape = ForensicShapes.md,
                                            modifier = Modifier.height(52.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isFa) "افزودن" else "Add")
                                        }
                                    }
                                }
                            }

                            // Active Seeds List
                            if (osintSeeds.isNotEmpty()) {
                                Text(
                                    text = if (isFa) "سرنخ‌های بارگذاری‌شده در این پرونده (${osintSeeds.size}):" else "Loaded Investigation Seeds (${osintSeeds.size}):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    osintSeeds.forEach { seed ->
                                        Surface(
                                            shape = ForensicShapes.sm,
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    ForensicBadge(
                                                        text = if (isFa) seed.type.displayNameFa else seed.type.displayNameEn,
                                                        badgeType = ForensicBadgeType.PRIMARY
                                                    )
                                                    Text(
                                                        text = seed.rawValue,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontFamily = FontFamily.Monospace,
                                                            textDirection = TextDirection.Ltr
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { viewModel.removeOsintSeed(seed.seedId) },
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = if (isFa) "حذف سرنخ" else "Remove Seed",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { viewModel.clearOsintSeeds() }
                                    ) {
                                        Text(if (isFa) "پاکسازی همه سرنخ‌ها" else "Clear All Seeds", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 1: RESOLVED ENTITIES & CONTROL MODEL
                if (activeTab == 1) {
                    item {
                        ForensicCard {
                            ForensicSectionHeader(
                                title = if (isFa) "تفکیک موجودیت‌ها و مدل انتساب / کنترل" else "Entity Resolution & Ownership/Control Model",
                                subtitle = if (isFa) "تفکیک دقیق شخصیت حقیقی، حقوقی، صرافی VASP، میکسر یا سرویس دارک‌وب" else "Strict distinction of Persons, Organizations, VASPs, Mixers, and Control Types",
                                icon = Icons.Default.DomainVerification
                            )

                            val entities = osintSession?.extractedEntities ?: emptyList()
                            if (entities.isEmpty()) {
                                Text(
                                    text = if (isFa) "هیچ موجودیتی استخراج نشده است. ابتدا پایپ‌لاین را اجرا کنید." else "No resolved entities yet. Execute the investigation pipeline first.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    entities.forEach { ent ->
                                        Card(
                                            shape = ForensicShapes.md,
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = when (ent.entityClass) {
                                                                ForensicEntityClass.EXCHANGE, ForensicEntityClass.VASP -> Icons.Default.AccountBalance
                                                                ForensicEntityClass.PERSON -> Icons.Default.Person
                                                                ForensicEntityClass.ORGANIZATION -> Icons.Default.Business
                                                                ForensicEntityClass.MIXER, ForensicEntityClass.DARKWEB_SERVICE -> Icons.Default.Shield
                                                                ForensicEntityClass.RANSOMWARE, ForensicEntityClass.SCAM -> Icons.Default.Warning
                                                                else -> Icons.Default.Token
                                                            },
                                                            contentDescription = null,
                                                            tint = if (ent.isSanctioned) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Text(
                                                            text = if (isFa) ent.nameFa else ent.name,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    ForensicBadge(
                                                        text = if (isFa) ent.controlRelation.displayNameFa else ent.controlRelation.displayNameEn,
                                                        badgeType = if (ent.controlRelation == AddressControlRelation.CONTROLLED_BY) ForensicBadgeType.ERROR else ForensicBadgeType.INFO
                                                    )
                                                }

                                                Text(
                                                    text = if (isFa) ent.summaryDetailsFa else ent.summaryDetailsEn,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${if (isFa) "مدرک استناد:" else "Primary Evidence:"} ${ent.primaryEvidenceId}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = "${if (isFa) "اطمینان:" else "Confidence:"} ${ent.confidence}%",
                                                        style = MaterialTheme.typography.labelSmall,
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

                // TAB 2: CORRELATION GRAPH & OBSERVABLES
                if (activeTab == 2) {
                    item {
                        ForensicCard {
                            ForensicSectionHeader(
                                title = if (isFa) "همبستگی چندمنبعی، سیگنال‌های جغرافیایی و رفتاری" else "Cross-Source Correlation & Behavioral Signals",
                                subtitle = if (isFa) "زنجیره Domain -> IP -> ASN -> Hosting -> Email و تحلیل شبانه‌روزی Diurnal" else "Domain -> IP -> ASN -> Hosting -> Email Chain and Diurnal Temporal Patterns",
                                icon = Icons.Default.Timeline
                            )

                            // Geo Signals
                            val geoSignals = osintSession?.geoSignals ?: emptyList()
                            if (geoSignals.isNotEmpty()) {
                                Text(
                                    text = if (isFa) "سیگنال‌های موقعیت جغرافیایی و مسیریابی شبکه:" else "Geographic & Network Routing Signals:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    geoSignals.forEach { geo ->
                                        Surface(
                                            shape = ForensicShapes.md,
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = if (isFa) geo.regionNameFa else geo.regionNameEn,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    ForensicBadge(
                                                        text = if (geo.isVpnOrTorExit) (if (isFa) "سرور هاستینگ / رله VPN" else "Hosting / VPN Relay") else (if (isFa) "تطابق زبان / محتوا" else "Language / Context Match"),
                                                        badgeType = if (geo.isVpnOrTorExit) ForensicBadgeType.WARNING else ForensicBadgeType.INFO
                                                    )
                                                }
                                                Text(
                                                    text = "${if (isFa) "شاخص‌های موافق:" else "Supporting:"} ${geo.supportingIndicatorsCount} | ${if (isFa) "مخالف:" else "Contradictory:"} ${geo.contradictoryIndicatorsCount} | ${if (isFa) "ضریب:" else "Confidence:"} ${geo.confidencePercent}%",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Temporal Diurnal Profile
                            val tempProfile = osintSession?.temporalProfile
                            if (tempProfile != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isFa) "تحلیل الگوی رفتاری و ساعات فعالیت (Temporal Profile):" else "Temporal & Diurnal Behavioral Profile:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Card(
                                    shape = ForensicShapes.md,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = if (isFa) tempProfile.notesFa else tempProfile.notesEn,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "${if (isFa) "ساعات اوج UTC:" else "Peak UTC Hours:"} ${tempProfile.peakActiveHoursUtc.joinToString(", ")} | ${if (isFa) "مناطق زمانی محتمل:" else "Candidate Timezones:"} ${tempProfile.candidateTimezoneOffsets.joinToString(" • ")}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 3: HYPOTHESES & CONFLICT RESOLUTION
                if (activeTab == 3) {
                    item {
                        ForensicCard {
                            ForensicSectionHeader(
                                title = if (isFa) "فرضیات تحقیقاتی و رفع تعارضات کارشناسی" else "Investigative Hypotheses & Conflict Resolution",
                                subtitle = if (isFa) "مدیریت تعارض بین منابع متناقض و بررسی مستندات موافق/مخالف" else "Adjudication of Contradictory Sources and Evidence Weighing",
                                icon = Icons.Default.FactCheck
                            )

                            // Conflicts Section
                            val conflicts = osintSession?.conflicts ?: emptyList()
                            if (conflicts.isNotEmpty()) {
                                Text(
                                    text = if (isFa) "تعارضات انتساب کشف‌شده (نیازمند رسیدگی کارشناس):" else "Detected Attribution Conflicts (Analyst Review Required):",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    conflicts.forEach { conf ->
                                        Card(
                                            shape = ForensicShapes.md,
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "${if (isFa) "تعارض:" else "Conflict:"} ${conf.targetIndicator}",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    ForensicBadge(
                                                        text = conf.resolutionStatus,
                                                        badgeType = if (conf.resolutionStatus == "UNRESOLVED") ForensicBadgeType.ERROR else ForensicBadgeType.SUCCESS
                                                    )
                                                }

                                                Text(
                                                    text = "${if (isFa) "گزینه الف:" else "Candidate A:"} ${if (isFa) conf.candidateAFa else conf.candidateA} (${conf.confidenceA}%) vs ${if (isFa) "گزینه ب:" else "Candidate B:"} ${if (isFa) conf.candidateBFa else conf.candidateB} (${conf.confidenceB}%)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )

                                                Text(
                                                    text = conf.analystReviewNotes,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )

                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            viewModel.resolveConflictDecision(conf.conflictId, "RESOLVED_A", "Analyst affirmed Candidate A based on co-spending analysis.")
                                                            Toast.makeText(context, if (isFa) "گزینه الف تایید شد." else "Candidate A selected.", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                        modifier = Modifier.height(36.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                                    ) {
                                                        Text(if (isFa) "تایید گزینه الف" else "Affirm A", style = MaterialTheme.typography.labelSmall)
                                                    }

                                                    Button(
                                                        onClick = {
                                                            viewModel.resolveConflictDecision(conf.conflictId, "RESOLVED_B", "Analyst affirmed Candidate B based on deposit pattern.")
                                                            Toast.makeText(context, if (isFa) "گزینه ب تایید شد." else "Candidate B selected.", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                        modifier = Modifier.height(36.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                                    ) {
                                                        Text(if (isFa) "تایید گزینه ب" else "Affirm B", style = MaterialTheme.typography.labelSmall)
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.resolveConflictDecision(conf.conflictId, "REJECTED_BOTH", "Inconclusive evidence - marked as ambiguous.")
                                                            Toast.makeText(context, if (isFa) "عدم قطعیت ثبت شد." else "Marked as Inconclusive.", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.height(36.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                                    ) {
                                                        Text(if (isFa) "رد هر دو / نامشخص" else "Reject Both", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Hypotheses Section
                            val hypotheses = osintSession?.hypotheses ?: emptyList()
                            if (hypotheses.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isFa) "فرضیات آزمون‌پذیر پرونده (Hypotheses):" else "Testable Investigation Hypotheses:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    hypotheses.forEach { hyp ->
                                        Card(
                                            shape = ForensicShapes.md,
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "${hyp.hypothesisId}: ${if (isFa) hyp.titleFa else hyp.titleEn}",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    ForensicBadge(
                                                        text = "${hyp.numericConfidence}% ${hyp.confidenceLevel}",
                                                        badgeType = if (hyp.confidenceLevel == "CONTESTED") ForensicBadgeType.WARNING else ForensicBadgeType.SUCCESS
                                                    )
                                                }

                                                Text(
                                                    text = if (isFa) hyp.propositionFa else hyp.propositionEn,
                                                    style = MaterialTheme.typography.bodySmall
                                                )

                                                Text(
                                                    text = "${if (isFa) "ادله موافق:" else "Supporting:"} ${hyp.supportingEvidenceIds.joinToString(", ").ifEmpty { "None" }} | ${if (isFa) "ادله مخالف:" else "Contradicting:"} ${hyp.contradictingEvidenceIds.joinToString(", ").ifEmpty { "None" }}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
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

                // TAB 4: RANKED LEADS
                if (activeTab == 4) {
                    item {
                        ForensicCard {
                            ForensicSectionHeader(
                                title = if (isFa) "سرنخ‌های رتبه‌بندی‌شده و اقدامات پیشنهادی" else "Ranked Leads & Next Recommended Steps",
                                subtitle = if (isFa) "اولویت‌بندی اهداف با ارزش اطلاعاتی بالا برای تعمیق تحقیقات" else "Prioritization of High-Value Target Identifiers for Further Probing",
                                icon = Icons.Default.TrendingUp
                            )

                            val leads = osintSession?.rankedLeads ?: emptyList()
                            if (leads.isEmpty()) {
                                Text(
                                    text = if (isFa) "سرنخی یافت نشد." else "No ranked leads available yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    leads.forEach { lead ->
                                        Card(
                                            shape = ForensicShapes.md,
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        ForensicBadge(
                                                            text = lead.leadId,
                                                            badgeType = ForensicBadgeType.PRIMARY
                                                        )
                                                        Text(
                                                            text = lead.targetValue,
                                                            style = MaterialTheme.typography.titleSmall.copy(
                                                                fontFamily = FontFamily.Monospace,
                                                                textDirection = TextDirection.Ltr
                                                            ),
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    ForensicBadge(
                                                        text = "${if (isFa) "امتیاز:" else "Score:"} ${lead.confidenceScore}",
                                                        badgeType = ForensicBadgeType.SUCCESS
                                                    )
                                                }

                                                Text(
                                                    text = if (isFa) lead.reasonFa else lead.reasonEn,
                                                    style = MaterialTheme.typography.bodySmall
                                                )

                                                Text(
                                                    text = "${if (isFa) "اقدام بعدی پیشنهادی:" else "Next Action:"} ${if (isFa) lead.nextRecommendedStepFa else lead.nextRecommendedStepEn}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
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

                // TAB 5: PLUGGABLE PROVIDERS
                if (activeTab == 5) {
                    item {
                        ForensicCard {
                            ForensicSectionHeader(
                                title = if (isFa) "ارائه‌دهندگان و ماژول‌های اطلاعاتی" else "Pluggable OSINT Source Providers",
                                subtitle = if (isFa) "مدیریت SpiderFoot، Maigret، Sherlock، theHarvester، OpenSanctions، MISP و GraphSense" else "Manage SpiderFoot, Maigret, Sherlock, theHarvester, OpenSanctions, MISP & GraphSense",
                                icon = Icons.Default.SettingsInputComponent
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                pluggableProviders.forEach { prov ->
                                    Surface(
                                        shape = ForensicShapes.md,
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = prov.name,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    ForensicBadge(
                                                        text = if (isFa) prov.healthStatus.displayNameFa else prov.healthStatus.displayNameEn,
                                                        badgeType = if (prov.healthStatus == ProviderHealthStatus.HEALTHY) ForensicBadgeType.SUCCESS else ForensicBadgeType.WARNING
                                                    )
                                                }
                                                Text(
                                                    text = if (isFa) prov.descriptionFa else prov.descriptionEn,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Switch(
                                                checked = prov.isEnabled,
                                                onCheckedChange = { viewModel.togglePluggableProvider(prov.providerId, it) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 6: VERIFIABLE EVIDENCE CHAIN
                if (activeTab == 6) {
                    item {
                        ForensicCard {
                            ForensicSectionHeader(
                                title = if (isFa) "زنجیره ادله مستند، تفکیک معرفتی و ممیزی بازتولیدپذیر" else "Verifiable Evidence Chain & Epistemic Audit Ledger",
                                subtitle = if (isFa) "رده‌بندی دقیق کیفیت A تا E و ثبت اثر انگشت رمزنگاری SHA-256 داده خام" else "Strict Source Quality (A-E), Raw Payload Hashes & Provenance Linage",
                                icon = Icons.Default.Security
                            )

                            val evidence = osintSession?.collectedEvidence ?: emptyList()
                            if (evidence.isEmpty()) {
                                Text(
                                    text = if (isFa) "ادله‌ای ثبت نشده است." else "No evidence curated yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    evidence.forEach { ev ->
                                        Card(
                                            shape = ForensicShapes.md,
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        ForensicBadge(
                                                            text = ev.sourceQuality.grade,
                                                            badgeType = when (ev.sourceQuality) {
                                                                SourceQualityGrade.A_PRIMARY_DIRECT -> ForensicBadgeType.SUCCESS
                                                                SourceQualityGrade.B_REPUTABLE_SECONDARY -> ForensicBadgeType.PRIMARY
                                                                SourceQualityGrade.C_USEFUL_CORROBORATION -> ForensicBadgeType.INFO
                                                                SourceQualityGrade.D_WEAK_UNVERIFIED -> ForensicBadgeType.WARNING
                                                                SourceQualityGrade.E_UNRELIABLE_CONTRADICTORY -> ForensicBadgeType.ERROR
                                                            }
                                                        )
                                                        ForensicBadge(
                                                            text = if (isFa) ev.epistemicType.displayNameFa else ev.epistemicType.displayNameEn,
                                                            badgeType = ForensicBadgeType.MUTED
                                                        )
                                                    }

                                                    Text(
                                                        text = "${if (isFa) "اطمینان:" else "Confidence:"} ${ev.numericConfidence}%",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Text(
                                                    text = if (isFa) ev.titleFa else ev.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                Text(
                                                    text = "${if (isFa) "منبع:" else "Source:"} ${ev.providerName} | ${if (isFa) "شناسه:" else "ID:"} ${ev.evidenceId}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "SHA-256: ${ev.rawContentHash.take(20)}...",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontFamily = FontFamily.Monospace,
                                                            textDirection = TextDirection.Ltr
                                                        ),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )

                                                    ForensicBadge(
                                                        text = ev.verificationStatus,
                                                        badgeType = when (ev.verificationStatus) {
                                                            "VERIFIED" -> ForensicBadgeType.SUCCESS
                                                            "REJECTED" -> ForensicBadgeType.ERROR
                                                            "CONTESTED" -> ForensicBadgeType.WARNING
                                                            else -> ForensicBadgeType.MUTED
                                                        }
                                                    )
                                                }

                                                // Evidence Action Buttons for Analyst
                                                FlowRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    FilledTonalButton(
                                                        onClick = {
                                                            viewModel.updateEvidenceVerificationStatus(ev.evidenceId, "VERIFIED")
                                                            Toast.makeText(context, if (isFa) "مدرک تایید شد" else "Evidence Verified", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.height(32.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(if (isFa) "تایید" else "Verify", style = MaterialTheme.typography.labelSmall)
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.updateEvidenceVerificationStatus(ev.evidenceId, "CONTESTED")
                                                            Toast.makeText(context, if (isFa) "مورد مناقشه علامت‌گذاری شد" else "Marked Contested", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.height(32.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(if (isFa) "مناقشه‌دار" else "Contest", style = MaterialTheme.typography.labelSmall)
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.updateEvidenceVerificationStatus(ev.evidenceId, "REJECTED")
                                                            Toast.makeText(context, if (isFa) "مدرک رد شد" else "Rejected", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.height(32.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(if (isFa) "رد مدرک" else "Reject", style = MaterialTheme.typography.labelSmall)
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                            clipboard.setPrimaryClip(ClipData.newPlainText("Evidence Hash", ev.rawContentHash))
                                                            Toast.makeText(context, if (isFa) "هش کامل در حافظه کپی شد" else "Full SHA-256 Hash Copied", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Hash", modifier = Modifier.size(16.dp))
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

                // TAB 7: AI INVESTIGATION CO-PILOT
                if (activeTab == 7) {
                    item {
                        ForensicCard {
                            ForensicSectionHeader(
                                title = if (isFa) "دستیار هوش مصنوعی با استناد قطعی به ادله" else "AI Investigation Co-Pilot (Evidence-Grounded)",
                                subtitle = if (isFa) "سنتز یافته‌ها، کشف تناقضات و استناد الزامی به شناسه‌های ادله" else "Evidence Synthesis, Contradiction Detection & Strict Evidence Citations",
                                icon = Icons.Default.AutoAwesome
                            )

                            val session = osintSession
                            if (session == null || session.collectedEvidence.isEmpty()) {
                                Text(
                                    text = if (isFa) "جهت فعال‌سازی تحلیل هوشمند، ابتدا پایپ‌لاین را با سرنخ‌های معتبر اجرا کنید." else "Run the investigation pipeline with valid seeds to generate evidence-grounded AI synthesis.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Card(
                                    shape = ForensicShapes.md,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = if (isFa) "خلاصه ادله‌محور پرونده:" else "Evidence-Grounded Executive Synthesis:",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )

                                        val aiConfigs by viewModel.aiSettingsRepo.configs.collectAsState()
                                        val hasAiConfig = aiConfigs.values.any { it.enabled }
                                        
                                        if (hasAiConfig) {
                                            Button(
                                                onClick = {
                                                    viewModel.setAiCopilotPromptQueue(if (isFa) "لطفاً یافته‌های OSINT و همبستگی اطلاعاتی کشف شده در این پرونده را ارزیابی کن." else "Please evaluate the OSINT findings and intelligence correlations discovered in this case.")
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if (isFa) "ارزیابی یکپارچه با دستیار هوشمند" else "Evaluate OSINT Fusion via AI Copilot")
                                            }
                                        } else {
                                            Text(
                                                text = if (isFa) "دستیار هوشمند غیرفعال است. جهت فعال‌سازی به تنظیمات مراجعه کنید." else "AI Copilot is disabled. Enable it in Settings.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (session.aggregateConfidenceScore < 50) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                shape = ForensicShapes.sm,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = if (isFa) "هشدار: شواهد کافی جهت استنتاج قطعی وجود ندارد (INSUFFICIENT EVIDENCE)." else "WARNING: Insufficient evidence to establish definitive ownership.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(8.dp)
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
    }
}
