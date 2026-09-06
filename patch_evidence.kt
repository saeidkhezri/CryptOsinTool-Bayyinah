                // 7. Evidence Chain Generation
                val generatedEvidenceChain: List<EvidenceItem> = EvidenceEngine.generateEvidenceChain(
                    targetAddress = trimmedAddress,
                    addressValidation = validation,
                    balanceSat = balanceSat,
                    totalReceivedSat = totalReceivedSat,
                    totalSentSat = totalSentSat,
                    txCount = txCount.coerceAtLeast(transactions.size),
                    counterparties = counterparties,
                    transactions = transactions,
                    riskIndicators = riskIndicators,
                    patternMatches = patternMatchesList,
                    classification = classification
                )
                
                val evidenceChain = generatedEvidenceChain.toMutableList()
                if (overviewResult.isFailure) {
                    evidenceChain.add(0, EvidenceItem(
                        id = UUID.randomUUID().toString(),
                        category = com.aistudio.orbit.model.EvidenceCategory.NETWORK_METADATA,
                        title = if (isPersian) "نقص در واکشی وضعیت کلی آدرس" else "Address Overview Fetch Failed",
                        description = overviewResult.exceptionOrNull()?.localizedMessage ?: "Unknown provider error",
                        source = providerName,
                        confidence = com.aistudio.orbit.model.EvidenceConfidence.LOW,
                        epistemicStatus = com.aistudio.orbit.model.EpistemicStatus.UNKNOWN
                    ))
                }
                if (txResult.isFailure) {
                    evidenceChain.add(0, EvidenceItem(
                        id = UUID.randomUUID().toString(),
                        category = com.aistudio.orbit.model.EvidenceCategory.NETWORK_METADATA,
                        title = if (isPersian) "نقص در واکشی تراکنش‌های تاریخی" else "Historical Transactions Fetch Failed",
                        description = txResult.exceptionOrNull()?.localizedMessage ?: "Unknown provider error",
                        source = providerName,
                        confidence = com.aistudio.orbit.model.EvidenceConfidence.LOW,
                        epistemicStatus = com.aistudio.orbit.model.EpistemicStatus.UNKNOWN
                    ))
                }
