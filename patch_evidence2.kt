                // 7. Evidence Chain Generation
                val generatedEvidenceChain: List<EvidenceItem> = EvidenceEngine.generateEvidenceChain(
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
                
                val evidenceChain = generatedEvidenceChain.toMutableList()
                if (overviewResult.isFailure) {
                    evidenceChain.add(0, EvidenceItem(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        category = com.aistudio.orbit.model.EvidenceCategory.EXTERNAL_SOURCE,
                        title = if (isPersian) "نقص در واکشی وضعیت کلی آدرس" else "Address Overview Fetch Failed",
                        description = overviewResult.exceptionOrNull()?.localizedMessage ?: "Unknown provider error",
                        rawDataSource = providerName,
                        providerName = providerName,
                        confidence = com.aistudio.orbit.model.ConfidenceLevel.UNCERTAIN,
                        isDirectFact = false,
                        polarity = com.aistudio.orbit.model.EvidencePolarity.NEGATIVE_FINDING
                    ))
                }
                if (txResult.isFailure) {
                    evidenceChain.add(0, EvidenceItem(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        category = com.aistudio.orbit.model.EvidenceCategory.EXTERNAL_SOURCE,
                        title = if (isPersian) "نقص در واکشی تراکنش‌های تاریخی" else "Historical Transactions Fetch Failed",
                        description = txResult.exceptionOrNull()?.localizedMessage ?: "Unknown provider error",
                        rawDataSource = providerName,
                        providerName = providerName,
                        confidence = com.aistudio.orbit.model.ConfidenceLevel.UNCERTAIN,
                        isDirectFact = false,
                        polarity = com.aistudio.orbit.model.EvidencePolarity.NEGATIVE_FINDING
                    ))
                }
