@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.aistudio.orbit.ui.screens

import com.aistudio.orbit.model.ExperienceMode

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.forensics.ai.copilot.AiCopilotView
import com.aistudio.orbit.forensics.graph.ForensicCaseGraphEngine
import com.aistudio.orbit.localization.AppLocalization
import com.aistudio.orbit.model.InteractiveCaseNode
import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.repository.AppLanguage
import com.aistudio.orbit.ui.InvestigationViewModel
import com.aistudio.orbit.ui.components.InteractiveCaseGraphVisualizer
import com.aistudio.orbit.ui.components.designsystem.ForensicEpistemicBadge
import com.aistudio.orbit.ui.components.designsystem.ForensicEpistemicType
import kotlinx.coroutines.launch

/**
 * Top-level sections of the Forensic Investigation Workspace.
 */
enum class WorkspaceDomain {
    BLOCKCHAIN_ANALYTICS,
    OSINT_INTELLIGENCE,
    CASE_GRAPH,
    TYPOLOGY_RULES,
    EVIDENCE_CHAIN,
    AI_COPILOT,
    REPORTS_EXPORT
}

/**
 * Sub-tabs within the Blockchain Analytics Domain.
 */
enum class BlockchainSubTab {
    OVERVIEW,
    TEMPORAL_ANALYSIS,
    TRANSACTIONS_LEDGER,
    COUNTERPARTY_MATRIX,
    PEELING_CHAIN_TRACKER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestigationWorkspaceView(
    viewModel: InvestigationViewModel,
    investigationCase: InvestigationCase,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val language by viewModel.settingsRepo.language.collectAsState()
    val isFa = language == AppLanguage.PERSIAN
    val strings = AppLocalization.getStrings(language)

    val osintReport by viewModel.osintReport.collectAsState()
    val deepIdentityDossier by viewModel.deepIdentityDossier.collectAsState()
    val deepCandidates = deepIdentityDossier?.phoneReconstruction?.topCandidates ?: emptyList()
    val osintSession by viewModel.osintSession.collectAsState()

    val experienceMode by viewModel.experienceMode.collectAsState()

    var stageStatuses by remember(investigationCase, osintReport, deepCandidates) {
        val hasTransactions = investigationCase.transactions.isNotEmpty()
        val hasCounterparties = investigationCase.counterparties.isNotEmpty()
        val hasPatterns = investigationCase.matchedPatterns.isNotEmpty()
        val hasOsint = osintReport != null
        val hasRisks = investigationCase.riskIndicators.isNotEmpty()
        val hasEvidence = investigationCase.evidenceLog.isNotEmpty()
        val hasHypotheses = investigationCase.hypotheses.isNotEmpty()

        mutableStateOf(mapOf(
            InvestigationStage.START_CASE to StageStatus.COMPLETED,
            InvestigationStage.INITIAL_LEAD to StageStatus.COMPLETED,
            InvestigationStage.BLOCKCHAIN_DISCOVERY to if (hasTransactions) StageStatus.COMPLETED else StageStatus.CURRENT,
            InvestigationStage.TRANSACTIONS_LEDGER to when {
                hasCounterparties -> StageStatus.COMPLETED
                hasTransactions -> StageStatus.CURRENT
                else -> StageStatus.LOCKED
            },
            InvestigationStage.RELATED_ADDRESSES to when {
                hasPatterns -> StageStatus.COMPLETED
                hasCounterparties -> StageStatus.CURRENT
                hasTransactions -> StageStatus.AVAILABLE
                else -> StageStatus.LOCKED
            },
            InvestigationStage.PATTERN_ANALYSIS to when {
                hasOsint -> StageStatus.COMPLETED
                hasPatterns -> StageStatus.CURRENT
                hasCounterparties -> StageStatus.AVAILABLE
                else -> StageStatus.LOCKED
            },
            InvestigationStage.OSINT_REVIEW to when {
                hasRisks -> StageStatus.COMPLETED
                hasOsint -> StageStatus.CURRENT
                hasPatterns -> StageStatus.AVAILABLE
                else -> StageStatus.LOCKED
            },
            InvestigationStage.RISK_REVIEW to when {
                hasEvidence -> StageStatus.COMPLETED
                hasRisks -> StageStatus.CURRENT
                hasOsint -> StageStatus.AVAILABLE
                else -> StageStatus.LOCKED
            },
            InvestigationStage.EVIDENCE_REVIEW to when {
                hasHypotheses -> StageStatus.COMPLETED
                hasEvidence -> StageStatus.CURRENT
                hasRisks -> StageStatus.AVAILABLE
                else -> StageStatus.LOCKED
            },
            InvestigationStage.CONCLUSION to when {
                hasHypotheses -> StageStatus.CURRENT
                hasEvidence -> StageStatus.AVAILABLE
                else -> StageStatus.LOCKED
            },
            InvestigationStage.REPORT to when {
                hasEvidence -> StageStatus.AVAILABLE
                else -> StageStatus.LOCKED
            }
        ))
    }

    val recommendedStage = stageStatuses.entries.firstOrNull { it.value == StageStatus.CURRENT }?.key ?: InvestigationStage.BLOCKCHAIN_DISCOVERY
    var currentStage by remember(investigationCase.caseId) { mutableStateOf(recommendedStage) }


    var activeDomain by remember(investigationCase.caseId) { mutableStateOf(WorkspaceDomain.BLOCKCHAIN_ANALYTICS) }
    var activeBlockchainSubTab by remember(investigationCase.caseId) { mutableStateOf(BlockchainSubTab.OVERVIEW) }

    val syncStageToDomain: (InvestigationStage) -> Unit = { stage ->
        currentStage = stage
        val updated = stageStatuses.toMutableMap()
        InvestigationStage.values().forEach { st ->
            if (st.id < stage.id) {
                if (updated[st] != StageStatus.SKIPPED) {
                    updated[st] = StageStatus.COMPLETED
                }
            } else if (st == stage) {
                if (updated[st] != StageStatus.SKIPPED) {
                    updated[st] = StageStatus.CURRENT
                }
            } else {
                if (updated[st] != StageStatus.SKIPPED && updated[st] != StageStatus.COMPLETED) {
                    updated[st] = StageStatus.AVAILABLE
                }
            }
        }
        stageStatuses = updated

        when (stage) {
            InvestigationStage.START_CASE -> {
                activeDomain = WorkspaceDomain.BLOCKCHAIN_ANALYTICS
                activeBlockchainSubTab = BlockchainSubTab.OVERVIEW
            }
            InvestigationStage.INITIAL_LEAD -> {
                activeDomain = WorkspaceDomain.BLOCKCHAIN_ANALYTICS
                activeBlockchainSubTab = BlockchainSubTab.OVERVIEW
            }
            InvestigationStage.BLOCKCHAIN_DISCOVERY -> {
                activeDomain = WorkspaceDomain.BLOCKCHAIN_ANALYTICS
                activeBlockchainSubTab = BlockchainSubTab.OVERVIEW
            }
            InvestigationStage.TRANSACTIONS_LEDGER -> {
                activeDomain = WorkspaceDomain.BLOCKCHAIN_ANALYTICS
                activeBlockchainSubTab = BlockchainSubTab.TRANSACTIONS_LEDGER
            }
            InvestigationStage.RELATED_ADDRESSES -> {
                activeDomain = WorkspaceDomain.CASE_GRAPH
            }
            InvestigationStage.PATTERN_ANALYSIS -> {
                activeDomain = WorkspaceDomain.TYPOLOGY_RULES
            }
            InvestigationStage.OSINT_REVIEW -> {
                activeDomain = WorkspaceDomain.OSINT_INTELLIGENCE
            }
            InvestigationStage.RISK_REVIEW -> {
                activeDomain = WorkspaceDomain.TYPOLOGY_RULES
            }
            InvestigationStage.EVIDENCE_REVIEW -> {
                activeDomain = WorkspaceDomain.EVIDENCE_CHAIN
            }
            InvestigationStage.CONCLUSION -> {
                activeDomain = WorkspaceDomain.AI_COPILOT
            }
            InvestigationStage.REPORT -> {
                activeDomain = WorkspaceDomain.REPORTS_EXPORT
            }
        }
    }

    var selectedNode by remember { mutableStateOf<InteractiveCaseNode?>(null) }
    var isNodeDetailsOpen by remember { mutableStateOf(false) }

    val promptQueue by viewModel.aiCopilotPromptQueue.collectAsState()
    LaunchedEffect(promptQueue) {
        if (promptQueue != null) {
            activeDomain = WorkspaceDomain.AI_COPILOT
        }
    }

    val interactiveGraph = remember(investigationCase, osintReport, deepCandidates, osintSession) {
        ForensicCaseGraphEngine.buildCaseGraph(
            investigationCase = investigationCase,
            osintReport = osintReport,
            deepIdentityCandidates = deepCandidates,
            osintSession = osintSession
        )
    }

    if (experienceMode == ExperienceMode.QUICK_CHECK) {
        QuickCheckView(
            viewModel = viewModel,
            investigationCase = investigationCase,
            isPersian = isFa,
            onEscalateToGuided = { viewModel.setExperienceMode(ExperienceMode.GUIDED_INVESTIGATION) },
            onEscalateToAnalyst = { viewModel.setExperienceMode(ExperienceMode.ANALYST_WORKSPACE) }
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthClass = when {
            maxWidth < 600.dp -> com.aistudio.orbit.ui.components.WindowWidthSizeClass.COMPACT
            maxWidth < 840.dp -> com.aistudio.orbit.ui.components.WindowWidthSizeClass.MEDIUM
            else -> com.aistudio.orbit.ui.components.WindowWidthSizeClass.EXPANDED
        }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight().width(300.dp)
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (isFa) "ناوبری پرونده" else "Case Navigation",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                WorkspaceDomain.values().forEach { domain ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = when (domain) {
                                    WorkspaceDomain.BLOCKCHAIN_ANALYTICS -> if (isFa) "تحلیل بلاکچین" else "Blockchain"
                                    WorkspaceDomain.OSINT_INTELLIGENCE -> if (isFa) "جستجوی اوسینت" else "OSINT"
                                    WorkspaceDomain.CASE_GRAPH -> if (isFa) "گراف ارتباطات" else "Case Graph"
                                    WorkspaceDomain.TYPOLOGY_RULES -> if (isFa) "قوانین و الگوها" else "Typologies"
                                    WorkspaceDomain.EVIDENCE_CHAIN -> if (isFa) "زنجیره ادله" else "Evidence"
                                    WorkspaceDomain.AI_COPILOT -> if (isFa) "دستیار هوشمند" else "AI Copilot"
                                    WorkspaceDomain.REPORTS_EXPORT -> if (isFa) "گزارشات و خروجی" else "Reports"
                                }
                            )
                        },
                        selected = activeDomain == domain,
                        onClick = {
                            activeDomain = domain
                            coroutineScope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                imageVector = when (domain) {
                                    WorkspaceDomain.BLOCKCHAIN_ANALYTICS -> Icons.Default.CurrencyBitcoin
                                    WorkspaceDomain.OSINT_INTELLIGENCE -> Icons.Default.Language
                                    WorkspaceDomain.CASE_GRAPH -> Icons.Default.Hub
                                    WorkspaceDomain.TYPOLOGY_RULES -> Icons.Default.Security
                                    WorkspaceDomain.EVIDENCE_CHAIN -> Icons.Default.Policy
                                    WorkspaceDomain.AI_COPILOT -> Icons.Default.SmartToy
                                    WorkspaceDomain.REPORTS_EXPORT -> Icons.Default.Assessment
                                },
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    // Nested Sub-navigation for Blockchain Analytics
                    if (domain == WorkspaceDomain.BLOCKCHAIN_ANALYTICS && activeDomain == WorkspaceDomain.BLOCKCHAIN_ANALYTICS) {
                        BlockchainSubTab.values().forEach { subTab ->
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = when (subTab) {
                                            BlockchainSubTab.OVERVIEW -> if (isFa) "خلاصه" else "Overview"
                                            BlockchainSubTab.TEMPORAL_ANALYSIS -> if (isFa) "زمانی" else "Temporal"
                                            BlockchainSubTab.TRANSACTIONS_LEDGER -> if (isFa) "دفترکل" else "Ledger"
                                            BlockchainSubTab.COUNTERPARTY_MATRIX -> if (isFa) "ماتریس" else "Matrix"
                                            BlockchainSubTab.PEELING_CHAIN_TRACKER -> if (isFa) "رهگیری" else "Tracker"
                                        },
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                },
                                selected = activeBlockchainSubTab == subTab,
                                onClick = {
                                    activeBlockchainSubTab = subTab
                                    coroutineScope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).padding(start = 16.dp)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.weight(1f))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp))
                Text(
                    text = if (isFa) "بیِّنة - نسخه ۱.۰.۰" else "Bayyinah v1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(28.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (isFa) "پرونده: ${investigationCase.id.take(8)}" else "Case: ${investigationCase.id.take(8)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = investigationCase.targetAddress,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, textDirection = TextDirection.Ltr),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Collapsible Roadmap & Experience Mode Header
                CollapsibleRoadmapHeader(
                    activeMode = experienceMode,
                    onModeChange = { viewModel.setExperienceMode(it) },
                    currentStage = currentStage,
                    stageStatuses = stageStatuses,
                    onStageSelect = { syncStageToDomain(it) },
                    isPersian = isFa
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val currentContent = @Composable {
                        when (activeDomain) {
                            WorkspaceDomain.BLOCKCHAIN_ANALYTICS -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        when (activeBlockchainSubTab) {
                                            BlockchainSubTab.OVERVIEW -> OverviewTab(investigationCase, isFa, strings)
                                            BlockchainSubTab.TEMPORAL_ANALYSIS -> TemporalAnalysisTab(investigationCase, isFa, strings)
                                            BlockchainSubTab.TRANSACTIONS_LEDGER -> TransactionsTab(investigationCase, isFa, strings, viewModel)
                                            BlockchainSubTab.COUNTERPARTY_MATRIX -> CounterpartyMatrixTab(investigationCase, isFa, strings)
                                            BlockchainSubTab.PEELING_CHAIN_TRACKER -> PeelingChainAndFlowTab(investigationCase, isFa, strings)
                                        }
                                    }
                                }
                            }

                                WorkspaceDomain.CASE_GRAPH -> {
                                    InteractiveCaseGraphVisualizer(
                                        initialGraph = interactiveGraph,
                                        isPersian = isFa,
                                        modifier = Modifier.fillMaxSize(),
                                        onRunDeepReconForNode = { node ->
                                            val cleanTarget = node.label.replace("Email: ", "").replace("Phone: ", "").trim()
                                            viewModel.runDeepIdentityReconstruction(cleanTarget, investigationCase.targetAddress)
                                            Toast.makeText(context, if (isFa) "واکاوی عمیق هویت فعال شد" else "Deep Recon Triggered", Toast.LENGTH_SHORT).show()
                                        },
                                        onAddNodeToEvidence = { node ->
                                            viewModel.addNodeAsEvidence(node)
                                            Toast.makeText(context, if (isFa) "گره به زنجیره ادله اضافه شد" else "Node added to evidence", Toast.LENGTH_SHORT).show()
                                        },
                                        onOpenProvenance = { node ->
                                            selectedNode = node
                                            isNodeDetailsOpen = true
                                        }
                                    )
                                }

                                WorkspaceDomain.OSINT_INTELLIGENCE -> {
                                    OsintInvestigationView(viewModel = viewModel, onBack = { activeDomain = WorkspaceDomain.BLOCKCHAIN_ANALYTICS })
                                }

                                WorkspaceDomain.TYPOLOGY_RULES -> {
                                    var typologyTab by remember { mutableStateOf(0) }
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        SingleChoiceSegmentedButtonRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        ) {
                                            SegmentedButton(
                                                selected = typologyTab == 0,
                                                onClick = { typologyTab = 0 },
                                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                            ) {
                                                Text(
                                                    text = if (isFa) "قوانین جرم‌یابی" else "Typology Rules",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                            SegmentedButton(
                                                selected = typologyTab == 1,
                                                onClick = { typologyTab = 1 },
                                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                            ) {
                                                Text(
                                                    text = if (isFa) "فرضیات کارشناسی" else "Hypothesis Workspace",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            if (typologyTab == 0) {
                                                TypologyRulesTab(investigationCase, isFa, strings)
                                            } else {
                                                HypothesisWorkspaceView(viewModel = viewModel, investigationCase = investigationCase, isFa = isFa)
                                            }
                                        }
                                    }
                                }

                                WorkspaceDomain.EVIDENCE_CHAIN -> {
                                    EvidencePanel(investigationCase, isFa, strings)
                                }

                                WorkspaceDomain.AI_COPILOT -> {
                                    AiCopilotView(
                                        viewModel = viewModel,
                                        investigationCase = investigationCase,
                                        onClose = { activeDomain = WorkspaceDomain.BLOCKCHAIN_ANALYTICS }
                                    )
                                }

                                WorkspaceDomain.REPORTS_EXPORT -> {
                                    ReportsPanel(investigationCase, isFa, viewModel, language)
                                }
                            }
                        }

                        if (experienceMode == ExperienceMode.GUIDED_INVESTIGATION) {
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    GuideStageTemplate(
                                        stage = currentStage,
                                        case = investigationCase,
                                        isPersian = isFa,
                                        onNavigateNext = { nextStage ->
                                            syncStageToDomain(nextStage)
                                        },
                                        onSkipStage = { reason ->
                                            val updated = stageStatuses.toMutableMap()
                                            updated[currentStage] = StageStatus.SKIPPED
                                            stageStatuses = updated
                                            Toast.makeText(context, if (isFa) "مرحله با موفقیت رد شد: $reason" else "Stage skipped: $reason", Toast.LENGTH_LONG).show()
                                        }
                                    ) {
                                        // Include dead-end checks dynamically
                                        val hasNoData = when (currentStage) {
                                            InvestigationStage.BLOCKCHAIN_DISCOVERY -> investigationCase.balanceSat == 0L
                                            InvestigationStage.TRANSACTIONS_LEDGER -> investigationCase.transactions.isEmpty()
                                            InvestigationStage.RELATED_ADDRESSES -> investigationCase.counterparties.isEmpty()
                                            else -> false
                                        }

                                        if (hasNoData) {
                                            DeadEndHandlingView(
                                                isPersian = isFa,
                                                stageTitle = if (isFa) currentStage.titleFa else currentStage.titleEn,
                                                onRerun = {
                                                    Toast.makeText(context, if (isFa) "بازخوانی مجدد اطلاعات آغاز شد" else "Rerunning data lookup...", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                                                currentContent()
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Analyst Mode: raw display
                            currentContent()
                        }
                    }

                    // Node Provenance Detail Modal Slide-in
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isNodeDetailsOpen && selectedNode != null,
                        enter = slideInHorizontally(initialOffsetX = { if (isFa) -it else it }, animationSpec = tween(300)),
                        exit = slideOutHorizontally(targetOffsetX = { if (isFa) -it else it }, animationSpec = tween(300)),
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.85f)
                            .widthIn(max = 480.dp)
                            .align(if (isFa) Alignment.Start else Alignment.End)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 8.dp
                        ) {
                            NodeDetailsPanel(selectedNode, isFa, onClose = { isNodeDetailsOpen = false })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EvidencePanel(case: InvestigationCase, isFa: Boolean, strings: com.aistudio.orbit.localization.ForensicStrings) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(16.dp)) {
            Text(if (isFa) "زنجیره ادله دیجیتال و شناسه یکپارچگی" else "Digital Evidence Chain & Provenance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        EvidenceChainTab(case, isFa, strings)
    }
}

@Composable
fun ReportsPanel(case: InvestigationCase, isFa: Boolean, viewModel: InvestigationViewModel, language: AppLanguage) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(16.dp)) {
            Text(if (isFa) "مدیریت گزارشات، مستندسازی و صدور" else "Reports, Export & Subpoena Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Stage7ReportExportView(
            viewModel = viewModel,
            investigationCase = case,
            language = language,
            onNavigatePrev = {}
        )
    }
}

@Composable
fun NodeDetailsPanel(node: InteractiveCaseNode?, isFa: Boolean, onClose: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (isFa) "جزئیات ادله (Provenance)" else "Evidence Provenance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        if (node != null) {
            Text(if (isFa) "موجودیت مورد بررسی:" else "Entity Under Investigation:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("• ID: ${node.id}")
            Text("• Label: ${node.label}")
            Text("• Category: ${node.entityCategory.name}")
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(if (isFa) "⚖️ منبع انتساب (Source of Truth):" else "⚖️ Source of Truth:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val epistemicType = when (node.epistemicStatus) {
                        com.aistudio.orbit.model.ForensicEpistemicStatus.UNDISPUTED_LEDGER_FACT -> ForensicEpistemicType.OBSERVED_FACT
                        com.aistudio.orbit.model.ForensicEpistemicStatus.EXTERNAL_SOURCE -> ForensicEpistemicType.EXTERNAL_SOURCE
                        com.aistudio.orbit.model.ForensicEpistemicStatus.ALGORITHMIC_CALCULATION -> ForensicEpistemicType.CALCULATED
                        com.aistudio.orbit.model.ForensicEpistemicStatus.DERIVED_OSINT_INFERENCE -> ForensicEpistemicType.INFERENCE
                        com.aistudio.orbit.model.ForensicEpistemicStatus.WORKING_HYPOTHESIS -> ForensicEpistemicType.HYPOTHESIS
                        com.aistudio.orbit.model.ForensicEpistemicStatus.INVESTIGATOR_ASSESSMENT -> ForensicEpistemicType.INVESTIGATOR_ASSESSMENT
                        com.aistudio.orbit.model.ForensicEpistemicStatus.UNKNOWN -> ForensicEpistemicType.UNKNOWN
                    }
                    ForensicEpistemicBadge(
                        type = epistemicType,
                        isPersian = isFa,
                        source = node.network.name,
                        confidencePercent = node.confidencePercent,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (node.tags.isNotEmpty()) {
                        Text(if (isFa) "تگ‌های منتسب: ${node.tags.joinToString()}" else "Attributed Tags: ${node.tags.joinToString()}")
                    }
                    
                    Text(
                        if (isFa) "«این داده بر اساس الگوریتم‌های اکتشافی درون‌زنجیره‌ای و ارائه‌دهندگان OSINT در زمان کشف اعتبارسنجی شده است. برای پیگیری قانونی، زنجیره ادله را استخراج کنید.»"
                        else "«This data is validated against on-chain heuristics and OSINT providers at discovery time. Export the Evidence Chain for legal pursuit.»",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
