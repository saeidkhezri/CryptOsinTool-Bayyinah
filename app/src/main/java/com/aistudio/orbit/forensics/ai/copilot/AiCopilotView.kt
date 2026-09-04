package com.aistudio.orbit.forensics.ai.copilot

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.forensics.ai.context.AiContextBuilder
import com.aistudio.orbit.forensics.ai.providers.AiProviderType
import com.aistudio.orbit.forensics.learning.*
import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.ui.InvestigationViewModel
import com.aistudio.orbit.ui.theme.ForensicSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCopilotView(
    viewModel: InvestigationViewModel,
    investigationCase: InvestigationCase,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val aiConfigs by viewModel.aiSettingsRepo.configs.collectAsState()
    val localOnly by viewModel.aiSettingsRepo.privacyModeLocalOnly.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var isLoading by remember { mutableStateOf(false) }
    
    var activeExplanationData by remember { mutableStateOf<ExplanationCardData?>(null) }
    var activeMiniLessonTopic by remember { mutableStateOf<MiniLessonTopic?>(null) }
    
    val promptQueue by viewModel.aiCopilotPromptQueue.collectAsState()
    
    LaunchedEffect(promptQueue) {
        promptQueue?.let { queuedPrompt ->
            inputText = queuedPrompt
            viewModel.setAiCopilotPromptQueue(null)
        }
    }

    // Dialog for "Why am I seeing this?"
    activeExplanationData?.let { data ->
        WhyAmISeeingThisDialog(
            data = data,
            isPersian = true,
            onDismiss = { activeExplanationData = null }
        )
    }

    // Dialog for Crypto Mini Lesson
    activeMiniLessonTopic?.let { topic ->
        CryptoMiniLessonDialog(
            lesson = CryptoMiniLessonRegistry.getLesson(topic),
            isPersian = true,
            onDismiss = { activeMiniLessonTopic = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ForensicSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("دستیار هوشمند کارشناس (AI Copilot)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("پاسخ‌های متکی بر شواهد و تحلیل مستند پرونده", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        HorizontalDivider()

        if (localOnly) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "ارتباط با هوش مصنوعی بیرونی به دلیل فعال بودن حالت حریم خصوصی (Privacy Mode - فقط پردازش محلی) متوقف است.",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "جهت استفاده از دستیار AI Copilot، می‌توانید ارسال داده به سرویس‌های هوش مصنوعی بیرونی را مستقیماً از کلید زیر یا بخش تنظیمات AI فعال کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.aiSettingsRepo.setPrivacyModeLocalOnly(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فعال‌سازی ارتباط با AI بیرونی (غیرفعال کردن Privacy Mode)")
                        }
                    }
                }
            }
        } else {
            // Quick Suggestion Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ForensicSpacing.md, vertical = ForensicSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    AssistChip(
                        onClick = { inputText = "شواهد و فرضیات موجود در این پرونده را خلاصه کن و ادله پشتیبان را ذکر کن." },
                        label = { Text("خلاصه ادله پرونده", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
                item {
                    AssistChip(
                        onClick = { inputText = "آیا شواهد کافی برای ارتباط این آدرس با صرافی یا میکسر وجود دارد؟" },
                        label = { Text("بررسی کفایت شواهد انتساب", fontSize = 11.sp) }
                    )
                }
                item {
                    AssistChip(
                        onClick = { activeMiniLessonTopic = MiniLessonTopic.UTXO },
                        label = { Text("آموزش: UTXO چیست؟", fontSize = 11.sp) }
                    )
                }
            }

            // Chat history
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(ForensicSpacing.md),
                verticalArrangement = Arrangement.spacedBy(ForensicSpacing.md)
            ) {
                items(chatHistory) { message ->
                    if (message.first == "user") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp),
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Text(
                                    message.second,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("AI Copilot", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }

                                        CertaintyBadge(level = ForensicCertaintyLevel.INFERENCE, isPersian = true)
                                    }

                                    val aiText = message.second
                                    val isInsufficient = aiText.contains("INSUFFICIENT EVIDENCE", ignoreCase = true) || aiText.contains("شواهد کافی نیست", ignoreCase = true)

                                    if (isInsufficient) {
                                        InsufficientEvidenceNotice(
                                            missingDataFa = "شواهد یا داده‌های آن‌چین موجود جهت ارائه پاسخ قطعی کامل نیست.",
                                            missingDataEn = "Insufficient evidence or missing on-chain data to confirm claim.",
                                            recommendedActionFa = "واکشی عمیق‌تر تراکنش‌ها، اجرای استعلام OSINT یا تحلیل گراف ارتباطی.",
                                            recommendedActionEn = "Perform deeper transaction fetch, run OSINT queries, or expand graph hop depth.",
                                            isPersian = true
                                        )
                                    }

                                    Text(
                                        aiText,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    // Why am I seeing this? Button
                                    WhyAmISeeingThisButton(
                                        isPersian = true,
                                        onClick = {
                                            activeExplanationData = ExplanationCardData(
                                                titleFa = "تحلیل هوش مصنوعی دستیار (AI Copilot)",
                                                titleEn = "AI Copilot Response Derivation",
                                                certaintyLevel = ForensicCertaintyLevel.INFERENCE,
                                                inputDataFa = "خلاصه ادله ثبت‌شده پرونده، گراف ارتباطات و پرسش کارشناس",
                                                inputDataEn = "Case evidence log, graph topology, and investigator query",
                                                analysisMethodFa = "پردازش متن و تطبیق ادله دیجیتال با قوانین انضباطی بیّنه",
                                                analysisMethodEn = "LLM text processing grounded with evidence citations",
                                                resultSummaryFa = "پاسخ ساختاریافته شامل ارجاعات مستقیم به کد ادله [EVID-xxx]",
                                                resultSummaryEn = "Grounded response referencing evidence IDs",
                                                evidenceIds = investigationCase.evidenceLog.map { it.id }.take(3),
                                                sources = listOf("داده‌های بلاکچین", "مدیریت ادله محلی"),
                                                limitationsFa = listOf("پاسخ هوش مصنوعی تنها جنبه دستیار تحلیلی داشته و جایگزین نظر رسمی کارشناس نیست."),
                                                limitationsEn = listOf("AI copilot output is advisory and requires human expert review.")
                                            )
                                        },
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input area
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ForensicSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("پرسش درباره شواهد، فرضیات یا تراکنش‌ها...") },
                        maxLines = 3,
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.width(ForensicSpacing.sm))
                    FilledIconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val userText = inputText
                                inputText = ""
                                chatHistory = chatHistory + ("user" to userText)
                                isLoading = true
                                
                                coroutineScope.launch {
                                    val enabledProvider = aiConfigs.values.firstOrNull { it.enabled }
                                    if (enabledProvider != null) {
                                        val provider = when (enabledProvider.providerType) {
                                            AiProviderType.GEMINI -> viewModel.geminiProvider
                                            AiProviderType.OPENAI -> viewModel.openAiProvider
                                            AiProviderType.DEEPSEEK -> viewModel.deepSeekProvider
                                            AiProviderType.YOU_COM -> viewModel.youSearchProvider
                                            else -> null
                                        }
                                        
                                        val apiKey = viewModel.aiSettingsRepo.getApiKey(enabledProvider.providerType)
                                        
                                        if (provider != null && apiKey.isNotBlank()) {
                                            val contextBuilder = AiContextBuilder(com.aistudio.orbit.db.AppDatabase.getDatabase(viewModel.getApplication()))
                                            
                                            // Serialize OSINT context if available
                                            val osintSession = viewModel.osintSession.value
                                            val osintContextStr = osintSession?.let {
                                                "Confidence: ${it.aggregateConfidenceScore}\n" +
                                                "Risk Category: ${it.aggregateRiskCategory.name}\n" +
                                                "Extracted Entities: ${it.extractedEntities.map { e -> "${e.name} (${e.entityClass.name})" }}\n" +
                                                "Conflicts: ${it.conflicts.map { c -> "${c.conflictDomain}: ${c.candidateA} vs ${c.candidateB}" }}\n"
                                            }
                                            
                                            val caseContext = contextBuilder.buildCaseSummaryContext(investigationCase, osintContextStr)
                                            val prompt = """
                                                You are an AI Investigation Assistant within the Bayyinah platform.
                                                Your primary role is to assist the investigator by summarizing, comparing, explaining, identifying inconsistencies, suggesting actions, proposing hypotheses, explaining graphs, summarizing OSINT, or drafting reports.

                                                CRITICAL RULES:
                                                1. Every important statement or conclusion you make MUST reference evidence IDs (e.g. [EVID-xxxx]) from the provided context.
                                                2. DO NOT output meaningless or fabricated confidence numbers. If you provide a confidence assessment, it must be explainable based on: Evidence Strength, Source Quality, Source Independence, Temporal Validity, Contradictory Evidence, and Analytical Method.
                                                3. If the evidence is insufficient to answer the user's question, you MUST explicitly state "INSUFFICIENT EVIDENCE" and explain what is missing.
                                                4. You may propose hypotheses but you may NOT finalize them as facts. State clearly that it is a hypothesis.

                                                Context:
                                                $caseContext

                                                Investigator Question: $userText

                                                Answer:
                                            """.trimIndent()
                                            
                                            val result = provider.executePrompt(
                                                prompt = prompt,
                                                apiKey = apiKey,
                                                model = enabledProvider.model,
                                                endpoint = enabledProvider.endpoint
                                            )
                                            
                                            if (result.output != null) {
                                                chatHistory = chatHistory + ("ai" to result.output)
                                            } else {
                                                chatHistory = chatHistory + ("ai" to "Error: ${result.errorMessage}")
                                            }
                                        } else {
                                            chatHistory = chatHistory + ("ai" to "خطا: کلید API مربوطه در تنظیمات پیکربندی نشده است.")
                                        }
                                    } else {
                                        chatHistory = chatHistory + ("ai" to "خطا: هیچ سرویس هوش مصنوعی در تنظیمات فعال نشده است.")
                                    }
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading && inputText.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
}

