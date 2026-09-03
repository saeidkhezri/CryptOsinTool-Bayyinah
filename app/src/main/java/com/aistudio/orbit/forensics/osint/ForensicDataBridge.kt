package com.aistudio.orbit.forensics.osint

import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.ForensicTransaction
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable

/**
 * Event-Driven Forensic Data Bridge & Pub/Sub Task Dispatcher.
 * Pipes on-chain transaction metadata (OP_RETURN, memos, RPC node IPs, ENS domains)
 * into modular OSINT pipelines inspired by GraphSense TagPacks and SpiderFoot event architecture.
 */
object ForensicDataBridge {

    private val _eventStream = MutableSharedFlow<ForensicEvent>(replay = 50, extraBufferCapacity = 100)
    val eventStream: SharedFlow<ForensicEvent> = _eventStream.asSharedFlow()

    private val _activePipelines = MutableStateFlow<List<PipelineStatus>>(emptyList())
    val activePipelines: StateFlow<List<PipelineStatus>> = _activePipelines.asStateFlow()

    @Serializable
    data class PipelineStatus(
        val pipelineId: String,
        val targetAddress: String,
        val stageName: String,
        val stageNameFa: String,
        val progressPercent: Int,
        val isCompleted: Boolean,
        val indicatorsDiscovered: Int,
        val lastEventDescription: String
    )

    /**
     * Publishes a forensic event into the centralized event stream.
     */
    suspend fun publishEvent(event: ForensicEvent) {
        _eventStream.emit(event)
        OnChainToOffChainHandoffEngine.eventBus.publish(event)
    }

    /**
     * Dispatches an automated forensic pipe for a given address and transaction history.
     */
    fun pipeBlockchainMetadataToOsint(
        scope: CoroutineScope,
        targetAddress: String,
        network: BlockchainNetwork,
        transactions: List<ForensicTransaction>,
        osintCacheDao: OsintCacheDao? = null,
        onProgress: (Float, String) -> Unit = { _, _ -> },
        onComplete: (AttributionScoreResult) -> Unit = {}
    ) {
        val pipelineId = "PIPE_${targetAddress.takeLast(6).uppercase()}_${System.currentTimeMillis()}"

        scope.launch(Dispatchers.IO) {
            updatePipeline(pipelineId, targetAddress, "Ingesting Ledger Blocks", "دریافت و تحلیل بلاک‌های دفترکل", 15, 0, "Ingesting on-chain records")
            onProgress(0.15f, "Ingesting ledger transactions and OP_RETURN memos...")

            publishEvent(
                ForensicEvent(
                    eventId = "EVT_PIPE_START_${System.currentTimeMillis()}",
                    type = ForensicEventType.INDICATOR_DISCOVERED,
                    indicatorType = IndicatorType.CRYPTO_ADDRESS,
                    data = targetAddress,
                    confidenceScore = 1.0f,
                    emittingModule = "ForensicDataBridge",
                    targetAddress = targetAddress,
                    descriptionEn = "Initiated automated on-chain to off-chain data pipe for $targetAddress",
                    descriptionFa = "آغاز خط لوله پردازش خودکار آن‌چین به آف‌چین برای آدرس $targetAddress"
                )
            )

            val (entity, indicators) = OnChainToOffChainHandoffEngine.extractIndicatorsFromOnChain(
                targetAddress = targetAddress,
                network = network,
                transactions = transactions
            )

            delay(300)
            updatePipeline(pipelineId, targetAddress, "GraphSense Clustering", "خوشه‌بندی تگ‌پک‌های گراف‌سنس", 45, indicators.size, "Extracting ENS & TagPack entities")
            onProgress(0.45f, "Matching GraphSense TagPacks and cluster entities...")

            delay(300)
            updatePipeline(pipelineId, targetAddress, "SpiderFoot Threat Correlator", "انطباق تهدیدات سایبری اسپایدر‌فوت", 75, indicators.size, "Correlating breach databases")
            onProgress(0.75f, "Correlating darknet breach intelligence and IP geolocation...")

            // Run the main handoff engine
            val attribution = OnChainToOffChainHandoffEngine.executeHandoffPipeline(
                targetAddress = targetAddress,
                network = network,
                indicators = indicators,
                onProgress = onProgress
            )

            // Persist to Room if DAO is present
            if (osintCacheDao != null) {
                try {
                    val indicatorEntities = attribution.osintIndicators.map { ind ->
                        ExtractedIndicatorEntity(
                            targetAddress = targetAddress,
                            network = network.symbol,
                            indicatorType = ind.type.name,
                            rawValue = ind.rawValue,
                            normalizedValue = ind.normalizedValue,
                            extractionSource = ind.extractionSource,
                            extractionConfidence = ind.extractionConfidence,
                            category = ind.type.name,
                            firstObserved = ind.timestamp,
                            lastObserved = ind.timestamp
                        )
                    }
                    osintCacheDao.insertExtractedIndicators(indicatorEntities)

                    val attrEntity = AttributionConfidenceEntity(
                        targetAddress = targetAddress,
                        overallConfidenceScore = attribution.overallConfidenceScore,
                        classificationLabelEn = attribution.classificationLabelEn,
                        classificationLabelFa = attribution.classificationLabelFa,
                        dominantActor = attribution.dominantActor,
                        supportingEvidenceCount = attribution.supportingEvidenceCount,
                        tagPackMatchesJson = kotlinx.serialization.json.Json.encodeToString(attribution.tagPackMatches),
                        osintIndicatorsJson = kotlinx.serialization.json.Json.encodeToString(attribution.osintIndicators),
                        caseReference = attribution.chainOfCustodySeal.caseReference,
                        sha256Fingerprint = attribution.chainOfCustodySeal.sha256Fingerprint,
                        evidenceMerkleRoot = attribution.chainOfCustodySeal.evidenceMerkleRoot,
                        investigatorSignature = attribution.chainOfCustodySeal.investigatorSignature
                    )
                    osintCacheDao.insertAttributionConfidence(attrEntity)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            updatePipeline(pipelineId, targetAddress, "Pipeline Complete", "پایان خط لوله و پلمپ ادله", 100, attribution.supportingEvidenceCount, "Evidence sealed")
            onProgress(1.0f, "Forensic data pipe complete. Chain-of-custody seal generated.")

            withContext(Dispatchers.Main) {
                onComplete(attribution)
            }
        }
    }

    private fun updatePipeline(
        id: String,
        addr: String,
        stageEn: String,
        stageFa: String,
        progress: Int,
        indicators: Int,
        lastDesc: String
    ) {
        val current = _activePipelines.value.toMutableList()
        val index = current.indexOfFirst { it.pipelineId == id }
        val updated = PipelineStatus(
            pipelineId = id,
            targetAddress = addr,
            stageName = stageEn,
            stageNameFa = stageFa,
            progressPercent = progress,
            isCompleted = progress >= 100,
            indicatorsDiscovered = indicators,
            lastEventDescription = lastDesc
        )
        if (index >= 0) {
            current[index] = updated
        } else {
            current.add(0, updated)
        }
        _activePipelines.value = current
    }
}
