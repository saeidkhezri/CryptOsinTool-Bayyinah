package com.aistudio.orbit.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.orbit.Edge
import com.aistudio.orbit.Graph
import com.aistudio.orbit.Node
import com.aistudio.orbit.forensics.AddressValidator
import com.aistudio.orbit.forensics.EvidenceEngine
import com.aistudio.orbit.forensics.TransactionAnalyzer
import com.aistudio.orbit.forensics.classification.EntityClassifier
import com.aistudio.orbit.forensics.classification.LabelingModule
import com.aistudio.orbit.forensics.clustering.ClusteringEngine
import com.aistudio.orbit.forensics.currency.CurrencyConverter
import com.aistudio.orbit.forensics.export.ForensicReportExporter
import com.aistudio.orbit.forensics.graph.GraphEngine
import com.aistudio.orbit.forensics.patterns.CrimePatternEngine
import com.aistudio.orbit.forensics.relationship.PairwiseRelationshipDetail
import com.aistudio.orbit.forensics.relationship.RelationshipAnalyzer
import com.aistudio.orbit.forensics.temporal.GeographicTimeEngine
import com.aistudio.orbit.forensics.osint.OsintForensicsEngine
import com.aistudio.orbit.forensics.osint.OsintAnalysisReport
import com.aistudio.orbit.forensics.osint.OnChainToOffChainHandoffEngine
import com.aistudio.orbit.forensics.osint.OnChainEntity
import com.aistudio.orbit.forensics.osint.ExtractedIndicator
import com.aistudio.orbit.forensics.osint.AttributionScoreResult
import com.aistudio.orbit.forensics.osint.ForensicEvent
import com.aistudio.orbit.model.*
import com.aistudio.orbit.provider.ProviderManager
import com.aistudio.orbit.repository.AppLanguage
import com.aistudio.orbit.repository.InvestigationRepository
import com.aistudio.orbit.repository.SettingsRepository
import com.aistudio.orbit.security.SecureStorageManager
import com.aistudio.orbit.security.auth.AuthManager
import com.aistudio.orbit.forensics.audit.AuditOperationType
import com.aistudio.orbit.forensics.audit.AuditResultState
import com.aistudio.orbit.forensics.audit.AuditTrailService
import com.aistudio.orbit.ui.components.ForensicProgressState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

import com.aistudio.orbit.repository.AiSettingsRepo
import com.aistudio.orbit.forensics.ai.providers.*

class InvestigationViewModel(application: Application) : AndroidViewModel(application) {

    val secureStorage = SecureStorageManager(application)
    val providerManager = ProviderManager(secureStorageManager = secureStorage)
    val investigationRepo = InvestigationRepository(application)
    val settingsRepo = SettingsRepository(application)
    val aiSettingsRepo = AiSettingsRepo(application, secureStorage)

    val geminiProvider = GeminiProvider()
    val openAiProvider = OpenAiProvider()
    val deepSeekProvider = DeepSeekProvider()
    val youSearchProvider = YouSearchProvider()

    // Enterprise API, Database, Case Data and Storage Services
    val apiManagerService = com.aistudio.orbit.forensics.api.ApiManagerService(application, secureStorage)
    val databaseManager = com.aistudio.orbit.forensics.database.ForensicDatabaseManager(application, secureStorage)
    val caseDataManager = com.aistudio.orbit.forensics.cases.CaseDataManager(application, investigationRepo)

    // OSINT Database Cache, Provider & Offline-First Repository
    val osintDatabase = com.aistudio.orbit.forensics.osint.OsintDatabase.getDatabase(application)
    val osintCacheDao = osintDatabase.osintCacheDao()
    val cachedOsintReports = osintCacheDao.getAllCached()
    val osintProvider = com.aistudio.orbit.provider.DefaultOsintProvider(secureStorage, osintCacheDao)
    val osintRepo = com.aistudio.orbit.repository.OsintRepository(osintCacheDao, osintProvider)
    private val osintJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    
    private val _aiDraftedSummary = MutableStateFlow<String?>(null)
    val aiDraftedSummary: StateFlow<String?> = _aiDraftedSummary
    
    private val _aiCopilotPromptQueue = MutableStateFlow<String?>(null)
    val aiCopilotPromptQueue: StateFlow<String?> = _aiCopilotPromptQueue
    
    fun setAiCopilotPromptQueue(prompt: String?) {
        _aiCopilotPromptQueue.value = prompt
    }
    
    fun setAiDraftedSummary(summary: String?) {
        _aiDraftedSummary.value = summary
    }
    
    fun generateAiDraftedSummary(caseObj: InvestigationCase) {
        viewModelScope.launch {
            _isLoading.value = true
            val configs = aiSettingsRepo.configs.value
            val enabledProvider = configs.values.firstOrNull { it.enabled }
            if (enabledProvider != null) {
                val provider = when (enabledProvider.providerType) {
                    AiProviderType.GEMINI -> geminiProvider
                    AiProviderType.OPENAI -> openAiProvider
                    AiProviderType.DEEPSEEK -> deepSeekProvider
                    AiProviderType.YOU_COM -> youSearchProvider
                    else -> null
                }
                val apiKey = aiSettingsRepo.getApiKey(enabledProvider.providerType)
                if (provider != null && apiKey.isNotBlank()) {
                    try {
                        val contextBuilder = com.aistudio.orbit.forensics.ai.context.AiContextBuilder(com.aistudio.orbit.db.AppDatabase.getDatabase(getApplication()))
                        val osintSessionVal = osintSession.value
                        val osintContextStr = osintSessionVal?.let {
                            "Confidence: ${it.aggregateConfidenceScore}\n" +
                            "Risk Category: ${it.aggregateRiskCategory.name}\n" +
                            "Extracted Entities: ${it.extractedEntities.map { e -> "${e.name} (${e.entityClass.name})" }}\n" +
                            "Conflicts: ${it.conflicts.map { c -> "${c.conflictDomain}: ${c.candidateA} vs ${c.candidateB}" }}\n"
                        }
                        val caseContext = contextBuilder.buildCaseSummaryContext(caseObj, osintContextStr)
                        val prompt = "Context:\n$caseContext\n\nTask: Write a concise, professional executive summary of this investigation case. The summary must include an overview, key findings, and implications. Use citations [EVID-xxx] whenever referencing evidence. Keep the tone objective and forensic."
                        val result = provider.executePrompt(prompt, apiKey, enabledProvider.model, enabledProvider.endpoint)
                        if (result.output != null) {
                            _aiDraftedSummary.value = result.output
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            _isLoading.value = false
        }
    }

    init {
        com.aistudio.orbit.forensics.currency.CurrencyConverter.initialize(osintDatabase.priceRateDao())
        viewModelScope.launch {
            com.aistudio.orbit.forensics.currency.CurrencyConverter.loadRatesFromRoom()
            try {
                com.aistudio.orbit.forensics.currency.CurrencyConverter.fetchLiveRatesAndSave()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Stage Workflow Management (Stages 1 through 7)
    private val _currentStage = MutableStateFlow(1)
    val currentStage: StateFlow<Int> = _currentStage.asStateFlow()

    private val _activeCase = MutableStateFlow<InvestigationCase?>(null)
    val activeCase: StateFlow<InvestigationCase?> = _activeCase.asStateFlow()

    private val _experienceMode = MutableStateFlow(com.aistudio.orbit.model.ExperienceMode.GUIDED_INVESTIGATION)
    val experienceMode: StateFlow<com.aistudio.orbit.model.ExperienceMode> = _experienceMode.asStateFlow()

    fun setExperienceMode(mode: com.aistudio.orbit.model.ExperienceMode) {
        _experienceMode.value = mode
    }
    val allCases: StateFlow<List<InvestigationCase>> = investigationRepo.cases
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeGraph = MutableStateFlow<Graph?>(null)
    val activeGraph: StateFlow<Graph?> = _activeGraph.asStateFlow()

    private val _selectedNodeId = MutableStateFlow<String?>(null)
    val selectedNodeId: StateFlow<String?> = _selectedNodeId.asStateFlow()

    // Deep-dive forensic analytical engines state
    private val _patternMatches = MutableStateFlow<List<PatternMatchResult>>(emptyList())
    val patternMatches: StateFlow<List<PatternMatchResult>> = _patternMatches.asStateFlow()

    private val _temporalReport = MutableStateFlow<TemporalAnalysisReport?>(null)
    val temporalReport: StateFlow<TemporalAnalysisReport?> = _temporalReport.asStateFlow()

    private val _addressClassification = MutableStateFlow<AddressClassification?>(null)
    val addressClassification: StateFlow<AddressClassification?> = _addressClassification.asStateFlow()

    private val _pairwiseDetail = MutableStateFlow<PairwiseRelationshipDetail?>(null)
    val pairwiseDetail: StateFlow<PairwiseRelationshipDetail?> = _pairwiseDetail.asStateFlow()

    private val _osintReport = MutableStateFlow<OsintAnalysisReport?>(null)
    val osintReport: StateFlow<OsintAnalysisReport?> = _osintReport.asStateFlow()

    // On-Chain to Off-Chain OSINT Handoff Pipeline States
    private val _extractedOnChainEntity = MutableStateFlow<OnChainEntity?>(null)
    val extractedOnChainEntity: StateFlow<OnChainEntity?> = _extractedOnChainEntity.asStateFlow()

    private val _extractedIndicators = MutableStateFlow<List<ExtractedIndicator>>(emptyList())
    val extractedIndicators: StateFlow<List<ExtractedIndicator>> = _extractedIndicators.asStateFlow()

    private val _attributionScoreResult = MutableStateFlow<AttributionScoreResult?>(null)
    val attributionScoreResult: StateFlow<AttributionScoreResult?> = _attributionScoreResult.asStateFlow()

    private val _isHandoffRunning = MutableStateFlow(false)
    val isHandoffRunning: StateFlow<Boolean> = _isHandoffRunning.asStateFlow()

    private val _handoffProgress = MutableStateFlow(0f)
    val handoffProgress: StateFlow<Float> = _handoffProgress.asStateFlow()

    private val _handoffProgressText = MutableStateFlow("")
    val handoffProgressText: StateFlow<String> = _handoffProgressText.asStateFlow()

    val forensicEvents = OnChainToOffChainHandoffEngine.eventBus.eventLog

    // Deep Identity Reconstruction States (Prompt 2 Engine)
    private val _deepIdentityDossier = MutableStateFlow<com.aistudio.orbit.forensics.osint.identity.DeepIdentityEngine.DeepIdentityDossier?>(null)
    val deepIdentityDossier: StateFlow<com.aistudio.orbit.forensics.osint.identity.DeepIdentityEngine.DeepIdentityDossier?> = _deepIdentityDossier.asStateFlow()

    private val _isReconstructingIdentity = MutableStateFlow(false)
    val isReconstructingIdentity: StateFlow<Boolean> = _isReconstructingIdentity.asStateFlow()

    val allDeepIdentities = osintCacheDao.getAllDeepIdentitiesFlow()

    // Transaction & Flow Filter States
    private val _filterDirection = MutableStateFlow<TxDirection?>(null)
    val filterDirection: StateFlow<TxDirection?> = _filterDirection.asStateFlow()

    private val _filterMinAmountBtc = MutableStateFlow(0.0)
    val filterMinAmountBtc: StateFlow<Double> = _filterMinAmountBtc.asStateFlow()

    // Investigator Customized Intermediate Pipeline Data
    private val _customizedTransactions = MutableStateFlow<List<ForensicTransaction>?>(null)
    val customizedTransactions: StateFlow<List<ForensicTransaction>?> = _customizedTransactions.asStateFlow()

    private val _customizedCounterparties = MutableStateFlow<List<CounterpartySummary>?>(null)
    val customizedCounterparties: StateFlow<List<CounterpartySummary>?> = _customizedCounterparties.asStateFlow()

    private val _customizedPatterns = MutableStateFlow<List<PatternMatchResult>?>(null)
    val customizedPatterns: StateFlow<List<PatternMatchResult>?> = _customizedPatterns.asStateFlow()

    // UI Loading, Operation Progress & Cancellation
    private var activeOperationJob: Job? = null

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingMessage = MutableStateFlow("")
    val loadingMessage: StateFlow<String> = _loadingMessage.asStateFlow()

    private val _progressState = MutableStateFlow(ForensicProgressState())
    val progressState: StateFlow<ForensicProgressState> = _progressState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _exportedDossierText = MutableStateFlow<String?>(null)
    val exportedDossierText: StateFlow<String?> = _exportedDossierText.asStateFlow()

    fun customizeTransactionsForNextStage(filteredTxs: List<ForensicTransaction>) {
        _customizedTransactions.value = filteredTxs
        val current = _activeCase.value ?: return
        val cps = TransactionAnalyzer.extractCounterparties(current.targetAddress, filteredTxs, current.network)
        _customizedCounterparties.value = cps
        _patternMatches.value = CrimePatternEngine.matchPatterns(current.targetAddress, filteredTxs, cps)
        _temporalReport.value = GeographicTimeEngine.analyzeTemporalProfile(current.targetAddress, filteredTxs)
        _activeGraph.value = buildVisualGraph(current.targetAddress, cps)
    }

    fun customizeCounterpartiesForNextStage(filteredCps: List<CounterpartySummary>) {
        _customizedCounterparties.value = filteredCps
        val current = _activeCase.value ?: return
        val txs = _customizedTransactions.value ?: current.transactions
        _patternMatches.value = CrimePatternEngine.matchPatterns(current.targetAddress, txs, filteredCps)
        _activeGraph.value = buildVisualGraph(current.targetAddress, filteredCps)
    }

    fun customizePatternsForNextStage(patterns: List<PatternMatchResult>) {
        _customizedPatterns.value = patterns
        _patternMatches.value = patterns
    }

    fun cancelCurrentOperation() {
        activeOperationJob?.cancel()
        activeOperationJob = null
        _isLoading.value = false
        _loadingMessage.value = ""
        _progressState.value = ForensicProgressState(isRunning = false)
        val user = AuthManager.currentUser.value?.username ?: "Anonymous"
        val currentAddr = _activeCase.value?.targetAddress ?: "Current Operation"
        AuditTrailService.recordAddressLookup(
            user = user,
            address = currentAddr,
            network = "Multi-Source",
            state = AuditResultState.CANCELED,
            txCount = 0,
            notes = "Operation canceled by investigator"
        )
    }

    fun setStage(stage: Int) {
        _currentStage.value = stage.coerceIn(1, 7)
    }

    fun nextStage() {
        if (_currentStage.value < 7) {
            _currentStage.value++
        }
    }

    fun prevStage() {
        if (_currentStage.value > 1) {
            _currentStage.value--
        }
    }

    fun setDirectionFilter(direction: TxDirection?) {
        _filterDirection.value = direction
    }

    fun setMinAmountFilter(minBtc: Double) {
        _filterMinAmountBtc.value = minBtc
    }

    fun inspectPairwiseRelationship(counterpartyAddress: String) {
        val current = _activeCase.value ?: return
        val detail = RelationshipAnalyzer.analyzePairwiseRelationship(
            targetAddress = current.targetAddress,
            counterpartyAddress = counterpartyAddress,
            transactions = current.transactions
        )
        _pairwiseDetail.value = detail
    }

    fun clearPairwiseDetail() {
        _pairwiseDetail.value = null
    }

    fun validateAddress(rawAddress: String, network: BlockchainNetwork): AddressValidationResult {
        return AddressValidator.validate(rawAddress, network)
    }

    fun startNewInvestigation(
        referenceNumber: String,
        caseTitle: String,
        targetAddress: String,
        network: BlockchainNetwork,
        scopeDescription: String,
        notes: String,
        tags: List<String>,
        searchDepth: Int = 1,
        queryLimit: Int = 50
    ) {
        val trimmedAddress = targetAddress.trim()
        val validation = AddressValidator.validate(trimmedAddress, network)
        if (!validation.isValid) {
            _errorMessage.value = validation.errorReason ?: "Invalid address format"
            return
        }

        activeOperationJob?.cancel()
        activeOperationJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _customizedTransactions.value = null
            _customizedCounterparties.value = null
            _customizedPatterns.value = null
            _pairwiseDetail.value = null
            _osintSeeds.value = listOf(com.aistudio.orbit.forensics.osint.InvestigationPipelineEngine.classifyAndNormalizeInput(trimmedAddress))
            val isPersian = settingsRepo.language.value == AppLanguage.PERSIAN
            val user = AuthManager.currentUser.value?.username ?: "Anonymous"

            val leds = listOf(
                com.aistudio.orbit.ui.components.ForensicResourceLed("node", "${network.displayName} RPC Node", "نود شبکه ${network.displayName}", isOnline = true, isConnected = true, pingMs = 32),
                com.aistudio.orbit.ui.components.ForensicResourceLed("room", "Room SQLite Cache DB", "پایگاه داده محلی Room", isOnline = false, isLocal = true, isConnected = true, pingMs = 2),
                com.aistudio.orbit.ui.components.ForensicResourceLed("osint", "OSINT Threat Intel Hub", "هاب اطلاعات تهدیدات اوسینت", isOnline = true, isConnected = true, pingMs = 45),
                com.aistudio.orbit.ui.components.ForensicResourceLed("rates", "Historical Forex Rates", "مرجع تسعیر تاریخی ریال/دلار", isOnline = true, isConnected = true, pingMs = 18)
            )

            var subtasks = listOf(
                com.aistudio.orbit.ui.components.ForensicSubTask("s1", "Validate Public Address & Network", "اعتبارسنجی فرمت آدرس و شبکه", isCompleted = false, isCurrent = true),
                com.aistudio.orbit.ui.components.ForensicSubTask("s2", "Query Public Ledger & Balances", "بررسی آدرس در بلاکچین و واکشی مانده حساب", isCompleted = false, isCurrent = false),
                com.aistudio.orbit.ui.components.ForensicSubTask("s3", "Retrieve Historical Tx Streams", "واکشی و بازیابی تراکنش‌های تاریخی", isCompleted = false, isCurrent = false),
                com.aistudio.orbit.ui.components.ForensicSubTask("s4", "Flow Normalization & Satoshi Calculation", "تفکیک جریان‌های ورودی/خروجی و نرمال‌سازی ساتوشی", isCompleted = false, isCurrent = false),
                com.aistudio.orbit.ui.components.ForensicSubTask("s5", "Counterparty Extraction & Cluster Mapping", "استخراج ماتریس طرف‌های مقابل و خوشه‌بندی", isCompleted = false, isCurrent = false),
                com.aistudio.orbit.ui.components.ForensicSubTask("s6", "AML Crime Pattern Heuristics Matching", "تطبیق با کتابخانه الگوهای پولشویی و جرائم", isCompleted = false, isCurrent = false),
                com.aistudio.orbit.ui.components.ForensicSubTask("s7", "Diurnal Rhythm & Temporal Profiling", "تحلیل شبانه‌روزی و سازگاری جغرافیایی-زمانی", isCompleted = false, isCurrent = false),
                com.aistudio.orbit.ui.components.ForensicSubTask("s8", "Entity Classification & Attribution", "رده‌بندی رفتار ماهیتی آدرس و استنباط هویت", isCompleted = false, isCurrent = false),
                com.aistudio.orbit.ui.components.ForensicSubTask("s9", "Consolidate Evidence Chain & Build Graph", "تکمیل زنجیره ادله دادگاهی و ترسیم گراف تعاملی", isCompleted = false, isCurrent = false)
            )

            _progressState.value = ForensicProgressState(
                isRunning = true,
                operationTitle = if (isPersian) "بررسی آدرس در بلاکچین و واکشی مانده حساب" else "Initial Ledger Query & Balance Retrieval",
                stepDescription = if (isPersian) "اتصال به نودهای شبکه ${network.displayName}..." else "Connecting to ${network.displayName} nodes...",
                network = network.displayName,
                progress = 0.12f,
                resourceLeds = leds,
                subTasks = subtasks
            )
            _loadingMessage.value = if (isPersian) "در حال دریافت اطلاعات از بلاکچین..." else "Querying public blockchain ledger..."

            try {
                // Step 1: Address validated
                subtasks = subtasks.map { if (it.id == "s1") it.copy(isCompleted = true, isCurrent = false) else if (it.id == "s2") it.copy(isCurrent = true) else it }
                _progressState.value = _progressState.value.copy(progress = 0.22f, subTasks = subtasks)

                // 1. Fetch Address Overview
                val overviewResult = providerManager.fetchAddressOverviewWithFallback(network, trimmedAddress)
                val overview = overviewResult.getOrNull()

                val balanceSat = overview?.balanceSat ?: 0L
                val totalReceivedSat = overview?.totalReceivedSat ?: 0L
                val totalSentSat = overview?.totalSentSat ?: 0L
                val txCount = overview?.transactionCount ?: 0
                val providerName = overview?.providerName ?: "Blockchain Node"

                subtasks = subtasks.map { if (it.id == "s2") it.copy(isCompleted = true, isCurrent = false) else if (it.id == "s3") it.copy(isCurrent = true) else it }
                _progressState.value = _progressState.value.copy(
                    operationTitle = if (isPersian) "واکشی و نرمال‌سازی تراکنش‌ها" else "Discovering & Normalizing Transactions",
                    stepDescription = if (isPersian) "استخراج تراکنش‌های تاریخی و تفکیک جریان‌های ورودی/خروجی..." else "Extracting historical ledger entries and flow directions...",
                    progress = 0.35f,
                    subTasks = subtasks
                )
                _loadingMessage.value = if (isPersian) "در حال واکشی و نرمال‌سازی تراکنش‌ها..." else "Extracting and normalizing transaction flows..."

                // 2. Fetch Transactions
                val txResult = providerManager.fetchTransactionsWithFallback(network, trimmedAddress, queryLimit, 0)
                val transactions: List<ForensicTransaction> = txResult.getOrDefault(emptyList())

                subtasks = subtasks.map { if (it.id == "s3") it.copy(isCompleted = true, isCurrent = false) else if (it.id == "s4") it.copy(isCompleted = true, isCurrent = false) else if (it.id == "s5") it.copy(isCurrent = true) else it }
                _progressState.value = _progressState.value.copy(
                    operationTitle = if (isPersian) "تحلیل طرف‌های مقابل و ماتریس ریسک" else "Processing Counterparties & Risk Matrix",
                    stepDescription = if (isPersian) "شناسایی آدرس‌های فرستنده، گیرنده و شاخص‌های هشدار..." else "Identifying senders, recipients, and forensic risk flags...",
                    progress = 0.55f,
                    itemsProcessed = transactions.size,
                    totalItems = txCount.coerceAtLeast(transactions.size),
                    subTasks = subtasks
                )
                _loadingMessage.value = if (isPersian) "در حال پردازش ماتریس طرف‌های مقابل و الگوهای ریسک..." else "Processing counterparty matrix & risk indicators..."

                // 3. Counterparties & Risk Analysis
                val counterparties: List<CounterpartySummary> = TransactionAnalyzer.extractCounterparties(trimmedAddress, transactions, network)
                val riskIndicators: List<RiskIndicator> = TransactionAnalyzer.analyzeRiskIndicators(trimmedAddress, transactions, counterparties)

                subtasks = subtasks.map { if (it.id == "s5") it.copy(isCompleted = true, isCurrent = false) else if (it.id == "s6") it.copy(isCurrent = true) else it }
                _progressState.value = _progressState.value.copy(
                    operationTitle = if (isPersian) "تطبیق با الگوهای پولشویی و جرائم مالی" else "Matching Financial Crime & Laundering Typologies",
                    stepDescription = if (isPersian) "ارزیابی لایه‌بندی، تراکنش‌های حلقوی و خوشه‌های مشکوک..." else "Evaluating layering, peel chains, and structuring patterns...",
                    progress = 0.70f,
                    subTasks = subtasks
                )
                _loadingMessage.value = if (isPersian) "در حال تطبیق با کتابخانه الگوهای جرائم مالی و پولشویی..." else "Matching against crime-pattern library & typologies..."

                // 4. Run Crime Pattern Engine
                val patternMatchesList = CrimePatternEngine.matchPatterns(trimmedAddress, transactions, counterparties)
                _patternMatches.value = patternMatchesList

                subtasks = subtasks.map { if (it.id == "s6") it.copy(isCompleted = true, isCurrent = false) else if (it.id == "s7") it.copy(isCurrent = true) else it }
                _progressState.value = _progressState.value.copy(
                    operationTitle = if (isPersian) "تحلیل شبانه‌روزی و سازگاری منطقه‌ای" else "Diurnal Rhythm & Timezone Profiling",
                    stepDescription = if (isPersian) "محاسبه همبستگی زمانی فعالیت با ساعات کاری مناطق مختلف جهان..." else "Computing statistical temporal alignment with global business hours...",
                    progress = 0.82f,
                    subTasks = subtasks
                )
                _loadingMessage.value = if (isPersian) "در حال تحلیل شبانه‌روزی و سازگاری زمانی-جغرافیایی..." else "Computing diurnal rhythm & timezone compatibility..."

                // 5. Run Diurnal & Geographic-Time Inference Engine
                val temporalReportData = GeographicTimeEngine.analyzeTemporalProfile(trimmedAddress, transactions)
                _temporalReport.value = temporalReportData

                subtasks = subtasks.map { if (it.id == "s7") it.copy(isCompleted = true, isCurrent = false) else if (it.id == "s8") it.copy(isCurrent = true) else it }

                // 6. Address & Entity Classification
                val hasConsolidation = transactions.any { it.inputs.size >= 5 && it.outputs.size <= 2 }
                val hasFanOut = transactions.any { it.inputs.size <= 2 && it.outputs.size >= 5 }
                val classification = EntityClassifier.classifyAddress(
                    address = trimmedAddress,
                    network = network,
                    txCount = txCount.coerceAtLeast(transactions.size),
                    counterpartyCount = counterparties.size,
                    hasConsolidation = hasConsolidation,
                    hasFanOut = hasFanOut
                )
                _addressClassification.value = classification

                subtasks = subtasks.map { if (it.id == "s8") it.copy(isCompleted = true, isCurrent = false) else if (it.id == "s9") it.copy(isCurrent = true) else it }
                _progressState.value = _progressState.value.copy(
                    operationTitle = if (isPersian) "تکمیل زنجیره ادله و تولید گراف" else "Consolidating Evidence Chain & Graph",
                    stepDescription = if (isPersian) "ثبت زنجیره ادله تفکیک‌شده (واقعیت، محاسبه، استنتاج)..." else "Categorizing evidence chain (facts, calculations, inferences)...",
                    progress = 0.94f,
                    subTasks = subtasks
                )

                // 7. Evidence Chain Generation
                val evidenceChain: List<EvidenceItem> = EvidenceEngine.generateEvidenceChain(
                    targetAddress = trimmedAddress,
                    addressValidation = validation,
                    balanceSat = balanceSat,
                    totalReceivedSat = totalReceivedSat,
                    totalSentSat = totalSentSat,
                    transactions = transactions,
                    counterparties = counterparties,
                    riskIndicators = riskIndicators,
                    providerName = providerName
                )

                // 8. Build Visual Graph using GraphEngine Layout
                val graph = buildVisualGraph(trimmedAddress, counterparties)
                _activeGraph.value = graph
                _selectedNodeId.value = trimmedAddress

                subtasks = subtasks.map { it.copy(isCompleted = true, isCurrent = false) }
                _progressState.value = _progressState.value.copy(progress = 1.0f, subTasks = subtasks)

                val caseId = UUID.randomUUID().toString()
                val finalRef = if (referenceNumber.isNotBlank()) referenceNumber else "CASE-${System.currentTimeMillis().toString().takeLast(6)}"
                val finalTitle = if (caseTitle.isNotBlank()) caseTitle else "Forensic Audit: ${trimmedAddress.take(8)}..."

                val newCase = InvestigationCase(
                    caseId = caseId,
                    referenceNumber = finalRef,
                    caseName = finalTitle,
                    targetAddress = trimmedAddress,
                    network = network,
                    createdTimestamp = System.currentTimeMillis(),
                    updatedTimestamp = System.currentTimeMillis(),
                    status = InvestigationStatus.COMPLETED,
                    description = scopeDescription,
                    notes = notes,
                    tags = tags,
                    seedAddresses = listOf(trimmedAddress),
                    searchDepth = searchDepth,
                    txLimit = queryLimit,
                    balanceSat = balanceSat,
                    totalReceivedSat = totalReceivedSat,
                    totalSentSat = totalSentSat,
                    totalTransactionsFound = txCount.coerceAtLeast(transactions.size),
                    firstTxTimestamp = transactions.minByOrNull { it.timestamp }?.timestamp,
                    lastTxTimestamp = transactions.maxByOrNull { it.timestamp }?.timestamp,
                    counterparties = counterparties,
                    transactions = transactions,
                    evidenceLog = evidenceChain,
                    riskIndicators = riskIndicators
                )

                _activeCase.value = newCase
                _osintReport.value = OsintForensicsEngine.performOsintInvestigation(trimmedAddress, network)
                investigationRepo.saveCase(newCase)
                runOnChainToOffChainHandoff(trimmedAddress, network)

                // Record into Forensic Audit Trail Service
                AuditTrailService.recordAddressLookup(
                    user = user,
                    address = trimmedAddress,
                    network = network.displayName,
                    state = AuditResultState.SUCCESS,
                    txCount = transactions.size,
                    notes = "Case $finalRef initialized. Provider: $providerName"
                )
                patternMatchesList.forEach { match ->
                    AuditTrailService.recordPatternMatched(
                        user = user,
                        address = trimmedAddress,
                        patternId = match.patternId,
                        confidencePct = match.similarityScore.toInt()
                    )
                }
                classification?.let { cls ->
                    AuditTrailService.recordClassification(
                        user = user,
                        address = trimmedAddress,
                        classification = cls.classification.name,
                        confidencePct = (cls.confidence.weight * 100).toInt()
                    )
                }

                _progressState.value = _progressState.value.copy(progress = 1.0f)
                
                // Automatically transition to Stage 2 (Activity Discovery)
                _currentStage.value = 2

            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to perform investigation"
                AuditTrailService.recordAddressLookup(
                    user = user,
                    address = trimmedAddress,
                    network = network.displayName,
                    state = AuditResultState.FAILED,
                    txCount = 0,
                    notes = "Error: ${e.localizedMessage}"
                )
            } finally {
                _isLoading.value = false
                _progressState.value = ForensicProgressState(isRunning = false)
            }
        }
    }

    fun loadCase(investigationCase: InvestigationCase) {
        _activeCase.value = investigationCase
        _customizedTransactions.value = null
        _customizedCounterparties.value = null
        _customizedPatterns.value = null
        _pairwiseDetail.value = null
        _osintSeeds.value = listOf(com.aistudio.orbit.forensics.osint.InvestigationPipelineEngine.classifyAndNormalizeInput(investigationCase.targetAddress))
        com.aistudio.orbit.forensics.osint.InvestigationPipelineEngine.setActiveCase(investigationCase.caseId)
        
        _osintReport.value = OsintForensicsEngine.performOsintInvestigation(investigationCase.targetAddress, investigationCase.network)
        _activeGraph.value = buildVisualGraph(investigationCase.targetAddress, investigationCase.counterparties)
        _selectedNodeId.value = investigationCase.targetAddress
        runOnChainToOffChainHandoff(investigationCase.targetAddress, investigationCase.network)
        
        // Re-derive analytical modules for loaded case
        _patternMatches.value = CrimePatternEngine.matchPatterns(
            investigationCase.targetAddress,
            investigationCase.transactions,
            investigationCase.counterparties
        )
        _temporalReport.value = GeographicTimeEngine.analyzeTemporalProfile(
            investigationCase.targetAddress,
            investigationCase.transactions
        )
        _addressClassification.value = EntityClassifier.classifyAddress(
            address = investigationCase.targetAddress,
            network = investigationCase.network,
            txCount = investigationCase.transactions.size,
            counterpartyCount = investigationCase.counterparties.size,
            hasConsolidation = investigationCase.transactions.any { it.inputs.size >= 5 && it.outputs.size <= 2 },
            hasFanOut = investigationCase.transactions.any { it.inputs.size <= 2 && it.outputs.size >= 5 }
        )
        _currentStage.value = 2
    }

    fun rerunCurrentAnalysisWithFilters() {
        val current = _activeCase.value ?: return
        val rawTxs = current.transactions
        val dir = _filterDirection.value
        val minBtc = _filterMinAmountBtc.value

        val filteredTxs = rawTxs.filter { tx ->
            val matchDir = (dir == null || tx.direction == dir)
            val matchMin = (tx.relevantAmountSat.toDouble() / 100_000_000.0) >= minBtc
            matchDir && matchMin
        }

        val cps = TransactionAnalyzer.extractCounterparties(current.targetAddress, filteredTxs, current.network)
        val risks = TransactionAnalyzer.analyzeRiskIndicators(current.targetAddress, filteredTxs, cps)
        _patternMatches.value = CrimePatternEngine.matchPatterns(current.targetAddress, filteredTxs, cps)
        _temporalReport.value = GeographicTimeEngine.analyzeTemporalProfile(current.targetAddress, filteredTxs)
        _activeGraph.value = buildVisualGraph(current.targetAddress, cps)

        val user = AuthManager.currentUser.value?.username ?: "Anonymous"
        AuditTrailService.recordFilterApplied(
            user = user,
            caseId = current.caseId,
            filterSummary = "Direction=${dir?.name ?: "ALL"}, MinBtc=$minBtc",
            matchCount = filteredTxs.size
        )
    }

    fun updateCaseNotes(newNotes: String) {
        val current = _activeCase.value ?: return
        val updated = current.copy(
            notes = newNotes,
            updatedTimestamp = System.currentTimeMillis()
        )
        _activeCase.value = updated
        viewModelScope.launch {
            investigationRepo.saveCase(updated)
        }
    }

    fun addNodeAsEvidence(node: InteractiveCaseNode) {
        val current = _activeCase.value ?: return
        val isPersian = settingsRepo.language.value == AppLanguage.PERSIAN

        val now = System.currentTimeMillis()
        val category = when (node.entityCategory) {
            ForensicEntityCategory.CRYPTO_WALLET,
            ForensicEntityCategory.BLOCKCHAIN_ADDRESS -> EvidenceCategory.OBSERVED_ON_CHAIN
            ForensicEntityCategory.SMART_CONTRACT,
            ForensicEntityCategory.CONTRACT -> EvidenceCategory.ALGORITHMIC_RESULT
            ForensicEntityCategory.EXCHANGE_HOT_WALLET,
            ForensicEntityCategory.MIXER_OR_TUMBLER,
            ForensicEntityCategory.EXCHANGE -> EvidenceCategory.ATTRIBUTION
            else -> EvidenceCategory.OSINT_INTELLIGENCE
        }

        val confidence = when (node.confidencePercent) {
            100 -> ConfidenceLevel.DEFINITIVE_FACT
            in 85..99 -> ConfidenceLevel.HIGH_CONFIDENCE
            in 50..84 -> ConfidenceLevel.MEDIUM_CONFIDENCE
            else -> ConfidenceLevel.LOW_CONFIDENCE
        }

        val titleEn = "Attributed Node: ${node.label}"
        val titleFa = "گره منتسب شده: ${node.label}"
        val descEn = "Identified ${node.entityCategory.name} with risk ${node.riskSeverity.name} and epistemic status ${node.epistemicStatus.displayNameEn} in case ${current.caseName}."
        val descFa = "شناسایی ${node.entityCategory.name} با ریسک ${node.riskSeverity.name} و وضعیت معرفت‌شناختی ${node.epistemicStatus.displayNameFa} در پرونده ${current.caseName}."

        val provenance = ProvenanceRecord(
            sourceName = "BIYYENAH Graph Analyzer",
            sourceType = if (node.isOnChain) DataSourceType.ON_CHAIN_RPC else DataSourceType.OSINT_DATABASE,
            retrievalTimestamp = now,
            analystUsername = AuthManager.currentUser.value?.username ?: "Administrator",
            network = current.network.name
        )

        val newEvidenceItem = EvidenceItem(
            id = "EV_NODE_${Math.abs(node.id.hashCode())}_$now",
            timestamp = now,
            category = category,
            title = if (isPersian) titleFa else titleEn,
            description = if (isPersian) descFa else descEn,
            rawDataSource = "BIYYENAH Forensic Graph Engine",
            providerName = "BIYYENAH Suite",
            confidence = confidence,
            relatedAddress = if (node.isOnChain) node.id else current.targetAddress,
            isDirectFact = node.epistemicStatus == ForensicEpistemicStatus.UNDISPUTED_LEDGER_FACT,
            titleEn = titleEn,
            titleFa = titleFa,
            descriptionEn = descEn,
            descriptionFa = descFa,
            provenance = provenance,
            verificationStatus = if (node.epistemicStatus == ForensicEpistemicStatus.UNDISPUTED_LEDGER_FACT) VerificationStatus.VERIFIED_OFFICIAL else VerificationStatus.HEURISTIC_CLUSTER
        )

        val updatedEvidenceList = current.evidenceLog.toMutableList().apply {
            add(newEvidenceItem)
        }

        val sealedEvidenceList = com.aistudio.orbit.forensics.export.ForensicEvidenceSealer.sealEvidenceSequence(updatedEvidenceList)

        val updatedCase = current.copy(
            evidenceLog = sealedEvidenceList,
            updatedTimestamp = now
        )

        _activeCase.value = updatedCase
        viewModelScope.launch {
            investigationRepo.saveCase(updatedCase)
            AuditTrailService.recordGraphModified(
                user = AuthManager.currentUser.value?.username ?: "Administrator",
                caseId = current.caseId,
                details = "Added node ${node.label} (${node.entityCategory}) to Case Reference ${current.referenceNumber}"
            )
        }
    }

    fun filterTemporalAnalysisByDays(daysAgo: Int?) {
        val current = _activeCase.value ?: return
        val allTxs = current.transactions
        val filteredTxs = if (daysAgo != null && daysAgo > 0) {
            val cutoffEpochSec = (System.currentTimeMillis() / 1000L) - (daysAgo * 86400L)
            allTxs.filter { it.timestamp >= cutoffEpochSec }
        } else {
            allTxs
        }
        _temporalReport.value = GeographicTimeEngine.analyzeTemporalProfile(current.targetAddress, filteredTxs)
    }

    fun selectNode(nodeId: String) {
        _selectedNodeId.value = nodeId
        val current = _activeCase.value
        if (current != null && !nodeId.equals(current.targetAddress, ignoreCase = true)) {
            inspectPairwiseRelationship(nodeId)
        }
    }

    fun generateDossierText(language: AppLanguage): String {
        val current = _activeCase.value ?: return ""
        val text = ForensicReportExporter.generateTextDossier(current, language)
        _exportedDossierText.value = text
        val user = AuthManager.currentUser.value?.username ?: "Anonymous"
        AuditTrailService.recordReportExport(
            user = user,
            caseId = current.caseId,
            format = "TXT Dossier",
            destination = "Internal Memory / Clipboard"
        )
        return text
    }

    fun generateCsvData(language: AppLanguage): String {
        val current = _activeCase.value ?: return ""
        val csv = ForensicReportExporter.generateCsvDataset(current, language)
        val user = AuthManager.currentUser.value?.username ?: "Anonymous"
        AuditTrailService.recordReportExport(
            user = user,
            caseId = current.caseId,
            format = "CSV Dataset",
            destination = "Internal Memory / File System"
        )
        return csv
    }

    fun generateDossierCsv(language: AppLanguage = AppLanguage.PERSIAN): String {
        return generateCsvData(language)
    }

    fun exportCaseCsv(context: Context, language: AppLanguage = AppLanguage.PERSIAN): String? {
        val current = _activeCase.value ?: return null
        val filename = ForensicReportExporter.exportCsvToStorage(context, current, language)
        if (filename != null) {
            val user = AuthManager.currentUser.value?.username ?: "Anonymous"
            AuditTrailService.recordReportExport(
                user = user,
                caseId = current.caseId,
                format = "CSV Dataset",
                destination = "Downloads"
            )
        }
        return filename
    }

    fun exportCasePdf(
        context: Context,
        language: AppLanguage,
        options: com.aistudio.orbit.forensics.export.ForensicReportOptions = com.aistudio.orbit.forensics.export.ForensicReportOptions()
    ): String? {
        val current = _activeCase.value ?: return null
        val graph = com.aistudio.orbit.forensics.graph.ForensicCaseGraphEngine.buildCaseGraph(
            investigationCase = current,
            osintReport = _osintReport.value,
            deepIdentityCandidates = _deepIdentityDossier.value?.phoneReconstruction?.topCandidates,
            osintSession = osintSession.value
        )
        val filename = com.aistudio.orbit.PdfReportExporter.exportForensicCasePdf(context, current, language, graph, options)
        if (filename != null) {
            val user = AuthManager.currentUser.value?.username ?: "Anonymous"
            AuditTrailService.recordReportExport(
                user = user,
                caseId = current.caseId,
                format = "PDF Document",
                destination = "Downloads"
            )
        }
        return filename
    }

    fun exportCaseXlsx(context: Context, language: AppLanguage): String? {
        val current = _activeCase.value ?: return null
        val graph = com.aistudio.orbit.forensics.graph.ForensicCaseGraphEngine.buildCaseGraph(
            investigationCase = current,
            osintReport = _osintReport.value,
            deepIdentityCandidates = _deepIdentityDossier.value?.phoneReconstruction?.topCandidates,
            osintSession = osintSession.value
        )
        val filename = com.aistudio.orbit.forensics.export.ForensicExcelExporter.exportCaseToXlsx(
            context = context,
            investigationCase = current,
            graph = graph,
            isPersian = language == AppLanguage.PERSIAN
        )
        if (filename != null) {
            val user = AuthManager.currentUser.value?.username ?: "Anonymous"
            AuditTrailService.recordReportExport(
                user = user,
                caseId = current.caseId,
                format = "Excel (.xlsx) Dossier",
                destination = "Downloads"
            )
        }
        return filename
    }

    fun exportCaseTxt(
        context: Context,
        language: AppLanguage,
        options: com.aistudio.orbit.forensics.export.ForensicReportOptions = com.aistudio.orbit.forensics.export.ForensicReportOptions()
    ): String? {
        val current = _activeCase.value ?: return null
        val filename = com.aistudio.orbit.PdfReportExporter.exportTxtReport(context, current, language, options)
        if (filename != null) {
            val user = AuthManager.currentUser.value?.username ?: "Anonymous"
            AuditTrailService.recordReportExport(
                user = user,
                caseId = current.caseId,
                format = "TXT Dossier",
                destination = "Downloads"
            )
        }
        return filename
    }

    fun generateSubpoenaText(language: AppLanguage): String {
        val current = _activeCase.value ?: return ""
        return com.aistudio.orbit.forensics.export.ForensicEvidenceSealer.generateSubpoenaRequisitionText(
            investigationCase = current,
            osintReport = _osintReport.value,
            language = language
        )
    }

    fun exportSubpoenaRequisition(context: Context, language: AppLanguage): String? {
        val current = _activeCase.value ?: return null
        val text = generateSubpoenaText(language)
        val filename = com.aistudio.orbit.forensics.export.ForensicEvidenceSealer.saveSubpoenaToDownloads(
            context = context,
            textContent = text,
            caseRef = current.referenceNumber
        )
        if (filename != null) {
            val user = AuthManager.currentUser.value?.username ?: "Anonymous"
            AuditTrailService.recordReportExport(
                user = user,
                caseId = current.caseId,
                format = "Subpoena Requisition",
                destination = "Downloads"
            )
        }
        return filename
    }

    fun generateCourtArchiveJson(language: AppLanguage): String {
        val current = _activeCase.value ?: return ""
        val graph = com.aistudio.orbit.forensics.graph.ForensicCaseGraphEngine.buildCaseGraph(
            investigationCase = current,
            osintReport = _osintReport.value,
            deepIdentityCandidates = _deepIdentityDossier.value?.phoneReconstruction?.topCandidates,
            osintSession = osintSession.value
        )
        return com.aistudio.orbit.forensics.export.ForensicEvidenceSealer.generateCourtArchiveJson(
            investigationCase = current,
            graph = graph,
            osintReport = _osintReport.value,
            language = language
        )
    }

    fun exportCourtArchiveJson(context: Context, language: AppLanguage): String? {
        val current = _activeCase.value ?: return null
        val json = generateCourtArchiveJson(language)
        val filename = com.aistudio.orbit.forensics.export.ForensicEvidenceSealer.saveCourtJsonToDownloads(
            context = context,
            jsonContent = json,
            caseRef = current.referenceNumber
        )
        if (filename != null) {
            val user = AuthManager.currentUser.value?.username ?: "Anonymous"
            AuditTrailService.recordReportExport(
                user = user,
                caseId = current.caseId,
                format = "Court Archive JSON",
                destination = "Downloads"
            )
        }
        return filename
    }

    fun generateMispStixJson(): String {
        val current = _activeCase.value ?: return ""
        val graph = com.aistudio.orbit.forensics.graph.ForensicCaseGraphEngine.buildCaseGraph(
            investigationCase = current,
            osintReport = _osintReport.value,
            deepIdentityCandidates = _deepIdentityDossier.value?.phoneReconstruction?.topCandidates,
            osintSession = osintSession.value
        )
        return com.aistudio.orbit.forensics.export.ForensicEvidenceSealer.generateMispStixJson(
            investigationCase = current,
            graph = graph,
            osintReport = _osintReport.value
        )
    }

    fun exportMispStixJson(context: Context): String? {
        val current = _activeCase.value ?: return null
        val json = generateMispStixJson()
        val filename = com.aistudio.orbit.forensics.export.ForensicEvidenceSealer.saveMispJsonToDownloads(
            context = context,
            jsonContent = json,
            caseRef = current.referenceNumber
        )
        if (filename != null) {
            val user = AuthManager.currentUser.value?.username ?: "Anonymous"
            AuditTrailService.recordReportExport(
                user = user,
                caseId = current.caseId,
                format = "MISP/OpenCTI STIX2",
                destination = "Downloads"
            )
        }
        return filename
    }

    fun generateGraphMlXml(): String {
        val current = _activeCase.value ?: return ""
        val graph = com.aistudio.orbit.forensics.graph.ForensicCaseGraphEngine.buildCaseGraph(
            investigationCase = current,
            osintReport = _osintReport.value,
            deepIdentityCandidates = _deepIdentityDossier.value?.phoneReconstruction?.topCandidates,
            osintSession = osintSession.value
        )
        return com.aistudio.orbit.forensics.export.ForensicEvidenceSealer.generateGraphMlXml(
            graph = graph,
            caseRef = current.referenceNumber
        )
    }

    fun exportGraphMl(context: Context): String? {
        val current = _activeCase.value ?: return null
        val xml = generateGraphMlXml()
        val filename = com.aistudio.orbit.forensics.export.ForensicEvidenceSealer.saveGraphMlToDownloads(
            context = context,
            xmlContent = xml,
            caseRef = current.referenceNumber
        )
        if (filename != null) {
            val user = AuthManager.currentUser.value?.username ?: "Anonymous"
            AuditTrailService.recordReportExport(
                user = user,
                caseId = current.caseId,
                format = "GraphML XML",
                destination = "Downloads"
            )
        }
        return filename
    }

    fun runOnChainToOffChainHandoff(targetAddress: String? = null, network: BlockchainNetwork? = null) {
        val addr = targetAddress ?: _activeCase.value?.targetAddress ?: return
        val net = network ?: _activeCase.value?.network ?: BlockchainNetwork.BITCOIN
        val currentCase = _activeCase.value

        viewModelScope.launch {
            _isHandoffRunning.value = true
            _handoffProgress.value = 0.05f
            _handoffProgressText.value = "Extracting On-Chain Entities (Memos, ENS, IP footprints)..."

            val txs = currentCase?.transactions ?: emptyList()
            val cps = currentCase?.counterparties?.map { it.address } ?: emptyList()

            // 1. Extract On-Chain Entities & Normalized Indicators
            val (entity, indicators) = OnChainToOffChainHandoffEngine.extractIndicatorsFromOnChain(
                targetAddress = addr,
                network = net,
                transactions = txs,
                counterparties = cps
            )
            _extractedOnChainEntity.value = entity
            _extractedIndicators.value = indicators

            // 2. Dispatch across asynchronous Pub/Sub bus
            val result = OnChainToOffChainHandoffEngine.executeHandoffPipeline(
                targetAddress = addr,
                network = net,
                indicators = indicators,
                onProgress = { progress, text ->
                    _handoffProgress.value = progress
                    _handoffProgressText.value = text
                }
            )

            _attributionScoreResult.value = result
            _isHandoffRunning.value = false
            _handoffProgress.value = 1.0f
            _handoffProgressText.value = "Handoff Pipeline Completed & Evidence Sealed."

            val user = AuthManager.currentUser.value?.username ?: "Anonymous"
            AuditTrailService.recordClassification(
                user = user,
                address = addr,
                classification = "OSINT_HANDOFF_${result.dominantActor}",
                confidencePct = (result.overallConfidenceScore * 100).toInt()
            )
        }
    }

    fun runDeepIdentityReconstruction(
        targetInput: String,
        associatedCrypto: String? = null,
        onComplete: (com.aistudio.orbit.forensics.osint.identity.DeepIdentityEngine.DeepIdentityDossier) -> Unit = {}
    ) {
        if (targetInput.isBlank()) return
        viewModelScope.launch {
            _isReconstructingIdentity.value = true
            try {
                val dossier = com.aistudio.orbit.forensics.osint.identity.DeepIdentityEngine.reconstructIdentity(
                    targetInput = targetInput,
                    associatedCryptoAddress = associatedCrypto,
                    osintCacheDao = osintCacheDao
                )
                _deepIdentityDossier.value = dossier
                onComplete(dossier)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isReconstructingIdentity.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun buildVisualGraph(
        targetAddress: String,
        counterparties: List<CounterpartySummary>
    ): Graph {
        val nodes = mutableListOf<Node>()
        val edges = mutableListOf<Edge>()

        // Target / Seed Node in the center
        nodes.add(
            Node(
                id = targetAddress,
                label = targetAddress.take(8) + "...",
                size = 28,
                x = 0f,
                y = 0f
            )
        )

        // Counterparty Ring
        val topCounterparties = counterparties.take(18)
        val count = topCounterparties.size
        val radius = 220f

        for ((index, cp) in topCounterparties.withIndex()) {
            val angle = (2 * Math.PI * index / count.coerceAtLeast(1)).toFloat()
            val x = (radius * cos(angle)) + (Math.random().toFloat() * 16f - 8f)
            val y = (radius * sin(angle)) + (Math.random().toFloat() * 16f - 8f)
            val nodeSize = (12 + (cp.txCount * 2)).coerceIn(14, 24)

            nodes.add(
                Node(
                    id = cp.address,
                    label = cp.address.take(8) + "...",
                    size = nodeSize,
                    x = x,
                    y = y
                )
            )

            edges.add(
                Edge(
                    id = "edge_${targetAddress.take(4)}_${cp.address.take(4)}_$index",
                    source = targetAddress,
                    target = cp.address,
                    size = cp.txCount.coerceIn(1, 10)
                )
            )
        }

        val rawGraph = Graph(nodes = nodes, edges = edges)
        return GraphEngine.stepForceDirectedLayout(rawGraph, iterations = 30)
    }

    fun saveOsintReportToRoom(report: com.aistudio.orbit.forensics.osint.OsintAnalysisReport) {
        viewModelScope.launch {
            try {
                val jsonStr = osintJson.encodeToString(com.aistudio.orbit.forensics.osint.OsintAnalysisReport.serializer(), report)
                val entity = com.aistudio.orbit.forensics.osint.OsintCacheEntity(
                    address = report.address,
                    network = report.network.name,
                    jsonReport = jsonStr
                )
                osintCacheDao.insertReport(entity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteOsintReportFromRoom(address: String) {
        viewModelScope.launch {
            try {
                osintCacheDao.deleteReport(address)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadOsintReportFromRoom(address: String, onLoaded: (com.aistudio.orbit.forensics.osint.OsintAnalysisReport?) -> Unit) {
        viewModelScope.launch {
            try {
                val entity = osintCacheDao.getReport(address)
                if (entity != null) {
                    val report = osintJson.decodeFromString(com.aistudio.orbit.forensics.osint.OsintAnalysisReport.serializer(), entity.jsonReport)
                    onLoaded(report)
                } else {
                    onLoaded(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onLoaded(null)
            }
        }
    }

    // Advanced OSINT Multi-Seed Pipeline & Intelligence Engine
    val osintSession: StateFlow<com.aistudio.orbit.forensics.osint.InvestigationExecutionSession?> = com.aistudio.orbit.forensics.osint.InvestigationPipelineEngine.currentSession
    val pluggableProviders: StateFlow<List<com.aistudio.orbit.provider.osint.PluggableProviderInfo>> = com.aistudio.orbit.provider.osint.PluggableOsintOrchestrator.providersState

    private val _osintSeeds = MutableStateFlow<List<com.aistudio.orbit.model.InvestigationSeed>>(emptyList())
    val osintSeeds: StateFlow<List<com.aistudio.orbit.model.InvestigationSeed>> = _osintSeeds.asStateFlow()

    fun addOsintSeed(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return
        val seed = com.aistudio.orbit.forensics.osint.InvestigationPipelineEngine.classifyAndNormalizeInput(trimmed)
        _osintSeeds.value = _osintSeeds.value + seed
    }

    fun removeOsintSeed(seedId: String) {
        _osintSeeds.value = _osintSeeds.value.filter { it.seedId != seedId }
    }

    fun clearOsintSeeds() {
        _osintSeeds.value = emptyList()
    }

    fun runFullOsintPipeline(customCaseId: String? = null) {
        val currentCaseId = customCaseId ?: _activeCase.value?.caseId ?: _activeCase.value?.referenceNumber ?: "CASE_LIVE_${System.currentTimeMillis()}"
        var seedsToInvestigate = _osintSeeds.value
        if (seedsToInvestigate.isEmpty()) {
            val targetAddr = _activeCase.value?.targetAddress ?: "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"
            val defaultSeed = com.aistudio.orbit.forensics.osint.InvestigationPipelineEngine.classifyAndNormalizeInput(targetAddr)
            seedsToInvestigate = listOf(defaultSeed)
            _osintSeeds.value = seedsToInvestigate
        }

        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Executing Multi-Source OSINT Pipeline..."
            try {
                com.aistudio.orbit.forensics.osint.InvestigationPipelineEngine.runCompleteInvestigation(
                    seeds = seedsToInvestigate,
                    caseId = currentCaseId,
                    onProgress = { pct, msgEn, msgFa ->
                        _loadingMessage.value = msgEn
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun togglePluggableProvider(providerId: String, isEnabled: Boolean) {
        com.aistudio.orbit.provider.osint.PluggableOsintOrchestrator.toggleProvider(providerId, isEnabled)
    }

    fun resolveConflictDecision(conflictId: String, decision: String, reviewNotes: String) {
        com.aistudio.orbit.forensics.osint.InvestigationPipelineEngine.updateConflictResolution(conflictId, decision, reviewNotes)
    }

    fun updateEvidenceVerificationStatus(evidenceId: String, newStatus: String, analystNote: String? = null) {
        com.aistudio.orbit.forensics.osint.InvestigationPipelineEngine.updateEvidenceVerification(evidenceId, newStatus, analystNote)
    }

    /**
     * Exports a cryptographically sealed Court-Ready JSON dossier containing on-chain facts,
     * OSINT transforms, graph topology, and SHA-256 seal.
     */
    fun exportCourtReadyDossier(context: Context, investigationCase: InvestigationCase, isPersian: Boolean): String? {
        val currentOsint = _osintReport.value ?: OsintForensicsEngine.performOsintInvestigation(investigationCase.targetAddress, investigationCase.network)
        val deepCands = _deepIdentityDossier.value?.phoneReconstruction?.topCandidates
        val caseGraph = com.aistudio.orbit.forensics.graph.ForensicCaseGraphEngine.buildCaseGraph(
            investigationCase = investigationCase,
            osintReport = currentOsint,
            deepIdentityCandidates = deepCands
        )
        val language = if (isPersian) AppLanguage.PERSIAN else AppLanguage.ENGLISH
        val jsonArchive = com.aistudio.orbit.forensics.export.ForensicEvidenceSealer.generateCourtArchiveJson(
            investigationCase = investigationCase,
            graph = caseGraph,
            osintReport = currentOsint,
            language = language
        )
        return com.aistudio.orbit.forensics.export.ForensicEvidenceSealer.saveCourtJsonToDownloads(
            context = context,
            jsonContent = jsonArchive,
            caseRef = investigationCase.referenceNumber
        )
    }

    /**
     * Generates formal subpoena requisition text for judicial / law-enforcement submission.
     */
    fun exportSubpoenaRequisition(context: Context, investigationCase: InvestigationCase, isPersian: Boolean): String {
        val currentOsint = _osintReport.value ?: OsintForensicsEngine.performOsintInvestigation(investigationCase.targetAddress, investigationCase.network)
        val language = if (isPersian) AppLanguage.PERSIAN else AppLanguage.ENGLISH
        return com.aistudio.orbit.forensics.export.ForensicEvidenceSealer.generateSubpoenaRequisitionText(
            investigationCase = investigationCase,
            osintReport = currentOsint,
            language = language
        )
    }
}

