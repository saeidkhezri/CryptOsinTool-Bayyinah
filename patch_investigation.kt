                // Step 1: Address validated
                subtasks = subtasks.map { if (it.id == "s1") it.copy(isCompleted = true, isCurrent = false) else if (it.id == "s2") it.copy(isCurrent = true) else it }
                _progressState.value = _progressState.value.copy(
                    progress = 0.20f,
                    operationTitle = if (isPersian) "بررسی آدرس در بلاکچین و واکشی مانده حساب" else "Initial Ledger Query",
                    stepDescription = if (isPersian) "دریافت اطلاعات تراکنش‌ها از نودهای بلاکچین..." else "Querying network RPC feeds...",
                    subTasks = subtasks
                )
                kotlinx.coroutines.delay(150L)

                // 1. Fetch Address Overview (Immediate Real Data Fetch)
                val overviewResult = providerManager.fetchAddressOverviewWithFallback(network, trimmedAddress)
                val overview = overviewResult.getOrNull()

                val balanceSat = if (overviewResult.isFailure) -1L else (overview?.balanceSat ?: 0L)
                val totalReceivedSat = if (overviewResult.isFailure) -1L else (overview?.totalReceivedSat ?: 0L)
                val totalSentSat = if (overviewResult.isFailure) -1L else (overview?.totalSentSat ?: 0L)
                val txCount = if (overviewResult.isFailure) -1 else (overview?.transactionCount ?: 0)
                val providerName = overview?.providerName ?: "Blockchain Node"

                subtasks = subtasks.map { if (it.id == "s2") it.copy(isCompleted = true, isCurrent = false) else if (it.id == "s3") it.copy(isCurrent = true) else it }
                _progressState.value = _progressState.value.copy(
                    progress = 0.35f,
                    operationTitle = if (isPersian) "واکشی و بازیابی تراکنش‌های تاریخی" else "Retrieving Transactions",
                    stepDescription = if (isPersian) "تجزیه تاریخی سوابق و جریان خروجی..." else "Parsing historical ledger logs...",
                    subTasks = subtasks
                )
                kotlinx.coroutines.delay(150L)

                // 2. Fetch Transactions (Immediate Real Data Fetch)
                val txResult = providerManager.fetchTransactionsWithFallback(network, trimmedAddress, queryLimit, 0)
                val transactions: List<ForensicTransaction> = txResult.getOrDefault(emptyList())

                subtasks = subtasks.map { if (it.id == "s3") it.copy(isCompleted = true, isCurrent = false) else if (it.id == "s4") it.copy(isCurrent = true) else it }
                _progressState.value = _progressState.value.copy(
                    progress = 0.50f,
                    operationTitle = if (isPersian) "تفکیک جریان‌های ورودی/خروجی و ساتوشی" else "Normalizing Flows",
                    stepDescription = if (isPersian) "محاسبه ارزش ریالی و تفکیک جریان‌های ساتوشی..." else "Normalizing Satoshi amounts...",
                    subTasks = subtasks,
                    foundTransactionsCount = transactions.size
                )
                kotlinx.coroutines.delay(150L)

                // 3. Counterparties & Risk Analysis
                val counterparties: List<CounterpartySummary> = TransactionAnalyzer.extractCounterparties(trimmedAddress, transactions, network)
                
                subtasks = subtasks.map { if (it.id == "s4") it.copy(isCompleted = true, isCurrent = false) else if (it.id == "s5") it.copy(isCurrent = true) else it }
                _progressState.value = _progressState.value.copy(
                    progress = 0.65f,
                    operationTitle = if (isPersian) "استخراج ماتریس طرف‌های مقابل" else "Mapping Counterparties",
                    stepDescription = if (isPersian) "تحلیل ساختار آدرس‌ها و خوشه‌بندی طرفین..." else "Generating counterparty list...",
                    subTasks = subtasks,
                    foundAddressesCount = counterparties.size,
                    foundRelationsCount = (counterparties.size * 1.5).toInt()
                )
                kotlinx.coroutines.delay(150L)
                
                val riskIndicators: List<RiskIndicator> = TransactionAnalyzer.analyzeRiskIndicators(trimmedAddress, transactions, counterparties)

                // 4. Run Crime Pattern Engine
                val patternMatchesList = CrimePatternEngine.matchPatterns(trimmedAddress, transactions, counterparties)
                _patternMatches.value = patternMatchesList
                
                subtasks = subtasks.map { if (it.id == "s5") it.copy(isCompleted = true, isCurrent = false) else if (it.id == "s6") it.copy(isCurrent = true) else it }
                _progressState.value = _progressState.value.copy(
                    progress = 0.75f,
                    operationTitle = if (isPersian) "تطبیق با الگوهای پولشویی و جرائم" else "Matching AML Patterns",
                    stepDescription = if (isPersian) "ارزیابی الگوهای لایه‌بندی و پیل‌چین..." else "Running pattern heuristic rules...",
                    subTasks = subtasks
                )
                kotlinx.coroutines.delay(150L)

                // 5. Run Diurnal & Geographic-Time Inference Engine
                val temporalReportData = GeographicTimeEngine.analyzeTemporalProfile(trimmedAddress, transactions)
                _temporalReport.value = temporalReportData
                
                subtasks = subtasks.map { if (it.id == "s6") it.copy(isCompleted = true, isCurrent = false) else if (it.id == "s7") it.copy(isCurrent = true) else it }
                _progressState.value = _progressState.value.copy(
                    progress = 0.85f,
                    operationTitle = if (isPersian) "تحلیل شبانه‌روزی و زمانی-جغرافیایی" else "Temporal Profiling",
                    stepDescription = if (isPersian) "محاسبه همبستگی زمانی فعالیت روزانه..." else "Analyzing diurnal activity hours...",
                    subTasks = subtasks
                )
                kotlinx.coroutines.delay(150L)

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
                
                subtasks = subtasks.map { if (it.id == "s7") it.copy(isCompleted = true, isCurrent = false) else if (it.id == "s8") it.copy(isCurrent = true) else it }
                _progressState.value = _progressState.value.copy(
                    progress = 0.92f,
                    operationTitle = if (isPersian) "رده‌بندی رفتار ماهیتی آدرس" else "Classifying Entity",
                    stepDescription = if (isPersian) "تطبیق رفتار تراکنشی با ماهیت‌های شناخته‌شده..." else "Inferring entity behavior...",
                    subTasks = subtasks
                )
                kotlinx.coroutines.delay(150L)

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
                
                subtasks = subtasks.map { if (it.id == "s8") it.copy(isCompleted = true, isCurrent = false) else if (it.id == "s9") it.copy(isCurrent = true) else it }
                _progressState.value = _progressState.value.copy(
                    progress = 0.97f,
                    operationTitle = if (isPersian) "تکمیل زنجیره ادله و ساخت گراف" else "Building Evidence Graph",
                    stepDescription = if (isPersian) "ترسیم نهایی ارتباطات در بوم کارشناسی..." else "Rendering active force network...",
                    subTasks = subtasks
                )
                kotlinx.coroutines.delay(150L)

                // 8. Build Visual Graph using GraphEngine Layout
                val graph = buildVisualGraph(trimmedAddress, counterparties)
                _activeGraph.value = graph
                _selectedNodeId.value = trimmedAddress

                subtasks = subtasks.map { if (it.id == "s9") it.copy(isCompleted = true, isCurrent = false) else it }
