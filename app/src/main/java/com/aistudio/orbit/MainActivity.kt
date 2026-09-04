package com.aistudio.orbit

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.localization.AppLocalization
import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.repository.AppLanguage
import com.aistudio.orbit.ui.InvestigationViewModel
import com.aistudio.orbit.ui.components.ForensicOperationProgressDialog
import com.aistudio.orbit.ui.components.GlobalSearchComponent
import com.aistudio.orbit.ui.components.PartialResultBanner
import com.aistudio.orbit.ui.components.PermissionConsentDialog
import com.aistudio.orbit.ui.components.OrbitTopAppBar
import com.aistudio.orbit.ui.components.WindowWidthSizeClass
import com.aistudio.orbit.ui.screens.*
import com.aistudio.orbit.ui.theme.OrbitForensicsTheme
import com.aistudio.orbit.repository.ThemeMode
import com.aistudio.orbit.ui.theme.ForensicDarkColorScheme
import com.aistudio.orbit.ui.theme.ForensicLightColorScheme
import com.aistudio.orbit.ui.theme.ForensicSpacing
import com.aistudio.orbit.ui.theme.ForensicTouchTarget

import com.aistudio.orbit.util.SecureStorageManager

class MainActivity : ComponentActivity() {

    private val investigationViewModel: InvestigationViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SecureStorageManager.init(applicationContext)
        com.aistudio.orbit.security.auth.AuthManager.ensureLocalOwnerSession()

        setContent {
            val language by investigationViewModel.settingsRepo.language.collectAsState()
            val themeMode by investigationViewModel.settingsRepo.themeMode.collectAsState()
            val useDynamicColor by investigationViewModel.settingsRepo.useDynamicColor.collectAsState()
            
            val isPersian = language == AppLanguage.PERSIAN
            val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr
            val strings = AppLocalization.getStrings(language)

            val sharedPrefs = remember { getSharedPreferences("theme_prefs", Context.MODE_PRIVATE) }
            var showSplash by remember { mutableStateOf(true) }

            // First run permission consent
            var hasConfiguredPermissions by remember {
                mutableStateOf(sharedPrefs.getBoolean("has_configured_permissions", false))
            }
            var showPermissionDialog by remember { mutableStateOf(false) }
            var showGlobalSearch by remember { mutableStateOf(false) }

            var selectedNavIndex by remember { mutableIntStateOf(0) }
            var detailedCase by remember { mutableStateOf<InvestigationCase?>(null) }
            var showOsintInvestigationHub by remember { mutableStateOf(false) }
            var showAiSettings by remember { mutableStateOf(false) }

            // Progress state for cancellable blockchain queries
            val progressState by investigationViewModel.progressState.collectAsState()
            val activeCase by investigationViewModel.activeCase.collectAsState()
            val allCases by investigationViewModel.allCases.collectAsState()
            val useLuxuryBackground by investigationViewModel.settingsRepo.useLuxuryBackground.collectAsState()

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                OrbitForensicsTheme(
                    themeMode = themeMode,
                    useDynamicColor = useDynamicColor
                ) {
                    val backgroundInteractionState = remember { com.aistudio.orbit.ui.components.AntigravityInteractionState() }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val changes = event.changes
                                        if (changes.isNotEmpty()) {
                                            val lastChange = changes.last()
                                            if (lastChange.pressed) {
                                                backgroundInteractionState.touchPosition = lastChange.position
                                                if (lastChange.previousPressed == false) {
                                                    backgroundInteractionState.lastClickPosition = lastChange.position
                                                    backgroundInteractionState.clickTrigger = (backgroundInteractionState.clickTrigger + 1) % 1000
                                                }
                                            } else {
                                                backgroundInteractionState.touchPosition = null
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        if (useLuxuryBackground) {
                            com.aistudio.orbit.ui.components.AntigravityNodeBackground(
                                isDark = (themeMode == ThemeMode.DARK),
                                interactionState = backgroundInteractionState
                            )
                        }
                        
                        if (showSplash) {
                        SplashScreen(
                            language = language,
                            onSplashFinished = { showSplash = false }
                        )
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val widthClass = when {
                                maxWidth < 600.dp -> WindowWidthSizeClass.COMPACT
                                maxWidth < 840.dp -> WindowWidthSizeClass.MEDIUM
                                else -> WindowWidthSizeClass.EXPANDED
                            }
                            val isCompact = widthClass == WindowWidthSizeClass.COMPACT
                            val showBottomBar = isCompact && detailedCase == null && !showOsintInvestigationHub
                            val showSideRail = !isCompact && detailedCase == null && !showOsintInvestigationHub

                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = if (useLuxuryBackground) Color.Transparent else MaterialTheme.colorScheme.background,
                                topBar = {
                                    OrbitTopAppBar(
                                        title = strings.appTitle,
                                        subtitle = null,
                                        navigationIcon = {
                                            if (detailedCase != null || showOsintInvestigationHub || showAiSettings) {
                                                IconButton(
                                                    onClick = { 
                                                        detailedCase = null 
                                                        showOsintInvestigationHub = false
                                                        showAiSettings = false
                                                    },
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = if (isPersian) "بازگشت" else "Back"
                                                    )
                                                }
                                            }
                                        },
                                        actions = {
                                            // Global Search Trigger only
                                            IconButton(
                                                onClick = { showGlobalSearch = true },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Search,
                                                    contentDescription = "Search",
                                                    tint = Color(0xFFE2A838),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    )
                                },
                                bottomBar = {
                                    if (showBottomBar) {
                                        NavigationBar(
                                            modifier = Modifier.navigationBarsPadding(),
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            tonalElevation = 6.dp
                                        ) {
                                            NavigationBarItem(
                                                selected = selectedNavIndex == 0,
                                                onClick = { selectedNavIndex = 0 },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.Dashboard,
                                                        contentDescription = null,
                                                        tint = if (selectedNavIndex == 0) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = strings.tabDashboard,
                                                        fontSize = 10.sp,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            )
                                            NavigationBarItem(
                                                selected = selectedNavIndex == 1,
                                                onClick = { selectedNavIndex = 1 },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.AddCircleOutline,
                                                        contentDescription = null,
                                                        tint = if (selectedNavIndex == 1) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = strings.tabNewInvestigation,
                                                        fontSize = 10.sp,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            )
                                            NavigationBarItem(
                                                selected = selectedNavIndex == 2,
                                                onClick = { selectedNavIndex = 2 },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.Folder,
                                                        contentDescription = null,
                                                        tint = if (selectedNavIndex == 2) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = strings.tabCases,
                                                        fontSize = 10.sp,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            )
                                            NavigationBarItem(
                                                selected = selectedNavIndex == 3,
                                                onClick = { selectedNavIndex = 3 },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.TravelExplore,
                                                        contentDescription = "OSINT",
                                                        tint = if (selectedNavIndex == 3) Color(0xFFE040FB) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = "OSINT",
                                                        fontSize = 10.sp,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            )
                                            NavigationBarItem(
                                                selected = selectedNavIndex == 4,
                                                onClick = { selectedNavIndex = 4 },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.Settings,
                                                        contentDescription = null,
                                                        tint = if (selectedNavIndex == 4) Color(0xFFFF7043) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = strings.tabApiSettings,
                                                        fontSize = 10.sp,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    if (showSideRail) {
                                        NavigationRail(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier.fillMaxHeight()
                                        ) {
                                            Spacer(modifier = Modifier.height(ForensicSpacing.md))
                                            NavigationRailItem(
                                                selected = selectedNavIndex == 0,
                                                onClick = { selectedNavIndex = 0 },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.Dashboard,
                                                        contentDescription = strings.tabDashboard,
                                                        tint = if (selectedNavIndex == 0) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                label = { Text(strings.tabDashboard, fontSize = 11.sp, maxLines = 1) }
                                            )
                                            NavigationRailItem(
                                                selected = selectedNavIndex == 1,
                                                onClick = { selectedNavIndex = 1 },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.AddCircleOutline,
                                                        contentDescription = strings.tabNewInvestigation,
                                                        tint = if (selectedNavIndex == 1) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                label = { Text(strings.tabNewInvestigation, fontSize = 11.sp, maxLines = 1) }
                                            )
                                            NavigationRailItem(
                                                selected = selectedNavIndex == 2,
                                                onClick = { selectedNavIndex = 2 },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.Folder,
                                                        contentDescription = strings.tabCases,
                                                        tint = if (selectedNavIndex == 2) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                label = { Text(strings.tabCases, fontSize = 11.sp, maxLines = 1) }
                                            )
                                            NavigationRailItem(
                                                selected = selectedNavIndex == 3,
                                                onClick = { selectedNavIndex = 3 },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.TravelExplore,
                                                        contentDescription = "OSINT",
                                                        tint = if (selectedNavIndex == 3) Color(0xFFE040FB) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                label = { Text("OSINT", fontSize = 11.sp, maxLines = 1) }
                                            )
                                            NavigationRailItem(
                                                selected = selectedNavIndex == 4,
                                                onClick = { selectedNavIndex = 4 },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.Settings,
                                                        contentDescription = strings.tabApiSettings,
                                                        tint = if (selectedNavIndex == 4) Color(0xFFFF7043) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                label = { Text(strings.tabApiSettings, fontSize = 11.sp, maxLines = 1) }
                                            )
                                        }
                                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        PartialResultBanner(
                                            isPersian = isPersian,
                                            onOpenApiSettings = {
                                                showAiSettings = true
                                                detailedCase = null
                                                showOsintInvestigationHub = false
                                            }
                                        )

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                        ) {
                                         val displayedCase = if (detailedCase != null) activeCase ?: detailedCase else null
                                         if (displayedCase != null) {
                                             InvestigationWorkspaceView(
                                                 viewModel = investigationViewModel,
                                                 investigationCase = displayedCase,
                                                 onBack = { detailedCase = null }
                                             )
                                         } else if (showAiSettings) {
                                             AiSettingsView(
                                                 viewModel = investigationViewModel
                                             )
                                         } else if (showOsintInvestigationHub) {
                                             OsintInvestigationView(
                                                 viewModel = investigationViewModel,
                                                 onBack = { showOsintInvestigationHub = false }
                                             )
                                         } else {
                                             when (selectedNavIndex) {
                                                 0 -> DashboardView(
                                                     viewModel = investigationViewModel,
                                                     onNavigateToNew = { selectedNavIndex = 1 },
                                                     onNavigateToCase = { caseObj ->
                                                         investigationViewModel.loadCase(caseObj)
                                                         detailedCase = caseObj
                                                     },
                                                     onNavigateToSettings = { selectedNavIndex = 4 },
                                                     onNavigateToOsint = { selectedNavIndex = 3 }
                                                 )
                                                 1 -> NewInvestigationView(
                                                     viewModel = investigationViewModel,
                                                     onInvestigationStarted = {
                                                         detailedCase = investigationViewModel.activeCase.value
                                                     }
                                                 )
                                                 2 -> CasesHistoryView(
                                                     viewModel = investigationViewModel,
                                                     onSelectCase = { caseObj ->
                                                         investigationViewModel.loadCase(caseObj)
                                                         detailedCase = caseObj
                                                     }
                                                 )
                                                 3 -> OsintInvestigationView(
                                                     viewModel = investigationViewModel,
                                                     onBack = { selectedNavIndex = 0 }
                                                 )
                                                 4 -> ApiSettingsView(
                                                     viewModel = investigationViewModel,
                                                     onRequestPermissionsDialog = { showPermissionDialog = true },
                                                     onNavigateToAiSettings = { showAiSettings = true }
                                                 )
                                             }
                                         }

                                        // Auto-navigate to detail view when a new case completes
                                        LaunchedEffect(activeCase?.caseId, progressState.isRunning) {
                                            // Do not mount the heavy investigation workspace while the
                                            // acquisition/analysis pipeline is still running. This avoids
                                            // concurrent graph/OSINT rendering and makes the progress dialog
                                            // the single owner of the running-operation surface.
                                            if (!progressState.isRunning && activeCase != null && detailedCase == null && selectedNavIndex == 1) {
                                                detailedCase = activeCase
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Cancellable Forensic Progress Dialog
                        if (progressState.isRunning) {
                            ForensicOperationProgressDialog(
                                state = progressState,
                                onCancel = {
                                    investigationViewModel.cancelCurrentOperation()
                                },
                                isPersian = isPersian
                            )
                        }

                        // Global Search Dialog
                        if (showGlobalSearch) {
                            GlobalSearchComponent(
                                cases = allCases,
                                onSelectCase = { caseObj ->
                                    investigationViewModel.loadCase(caseObj)
                                    detailedCase = caseObj
                                    showGlobalSearch = false
                                },
                                onStartNewInvestigation = { addr, net ->
                                    showGlobalSearch = false
                                    selectedNavIndex = 1
                                    investigationViewModel.startNewInvestigation(
                                        referenceNumber = "",
                                        caseTitle = "Search: ${addr.take(8)}...",
                                        targetAddress = addr,
                                        network = net,
                                        scopeDescription = "Initiated from Global Search",
                                        notes = "",
                                        tags = listOf("Search")
                                    )
                                },
                                onDismiss = { showGlobalSearch = false },
                                isPersian = isPersian
                            )
                        }

                        // Permission Consent Dialog
                        if (showPermissionDialog || (!hasConfiguredPermissions && !showSplash)) {
                            PermissionConsentDialog(
                                language = language,
                                onDismiss = {
                                    showPermissionDialog = false
                                    hasConfiguredPermissions = true
                                    sharedPrefs.edit()
                                        .putBoolean("has_configured_permissions", true)
                                        .putBoolean("perm_internet", true)
                                        .putBoolean("perm_network_state", true)
                                        .apply()
                                },
                                onPermissionsConfirmed = { permMap ->
                                    showPermissionDialog = false
                                    hasConfiguredPermissions = true
                                    val editor = sharedPrefs.edit()
                                    editor.putBoolean("has_configured_permissions", true)
                                    permMap.forEach { (key, value) ->
                                        editor.putBoolean(key, value)
                                        if (!key.startsWith("perm_")) {
                                            editor.putBoolean("perm_$key", value)
                                        }
                                    }
                                    editor.apply()
                                }
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
