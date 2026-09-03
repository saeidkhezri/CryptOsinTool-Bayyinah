package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

@Serializable
enum class BlockchainNetwork(val symbol: String, val displayName: String, val isUtxoBased: Boolean) {
    BITCOIN("BTC", "Bitcoin Mainnet", true),
    ETHEREUM("ETH", "Ethereum Mainnet", false),
    TETHER_USDT("USDT", "Tether USD (Multi-Network)", false),
    BNB_CHAIN("BNB", "BNB Smart Chain", false),
    POLYGON("POL", "Polygon PoS", false),
    TRON("TRX", "TRON Network", false),
    SOLANA("SOL", "Solana", false)
}

@Serializable
enum class AddressType {
    BTC_LEGACY_P2PKH,      // 1...
    BTC_P2SH,              // 3...
    BTC_BECH32_SEGWIT,     // bc1q...
    BTC_TAPROOT,           // bc1p...
    ETH_EVM,               // 0x...
    TRON_BASE58,           // T...
    SOLANA_BASE58,         // 32-44 base58 chars
    UNKNOWN
}

@Serializable
enum class TxDirection {
    INCOMING,              // Value transferred into target address
    OUTGOING,              // Value transferred out of target address
    SELF_TRANSFER,         // Consolidation or change to same address
    MIXED,                 // Multi-input/output containing both incoming and outgoing
    CONTRACT_CALL;         // Smart contract interaction

    companion object {
        val INBOUND = INCOMING
        val OUTBOUND = OUTGOING
    }
}

@Serializable
enum class RiskSeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Serializable
data class AddressValidationResult(
    val isValid: Boolean,
    val network: BlockchainNetwork,
    val addressType: AddressType,
    val formattedAddress: String,
    val errorReason: String? = null,
    val uncertaintyWarning: String? = null
)

@Serializable
data class TxInput(
    val txId: String = "",
    val vout: Int = 0,
    val prevOutAddress: String? = null,
    val prevOutValueSat: Long = 0,
    val isTargetAddress: Boolean = false
) {
    val txid: String get() = txId
    val address: String? get() = prevOutAddress
    val amountSat: Long get() = prevOutValueSat
    val prevOutValueBtc: Double get() = prevOutValueSat.toDouble() / 100_000_000.0
}

@Serializable
data class TxOutput(
    val n: Int = 0,
    val scriptPubKey: String? = null,
    val address: String? = null,
    val valueSat: Long = 0,
    val isTargetAddress: Boolean = false
) {
    val amountSat: Long get() = valueSat
    val valueBtc: Double get() = valueSat.toDouble() / 100_000_000.0
}

@Serializable
data class ForensicTransaction(
    val txId: String,
    val network: BlockchainNetwork = BlockchainNetwork.BITCOIN,
    val timestamp: Long,               // UNIX timestamp in seconds
    val blockHeight: Long? = null,
    val confirmations: Int = 1,
    val isConfirmed: Boolean = true,
    val feeSat: Long = 0,
    val inputs: List<TxInput> = emptyList(),
    val outputs: List<TxOutput> = emptyList(),
    val direction: TxDirection = TxDirection.INCOMING,
    val relevantAmountSat: Long = 0,
    val counterpartyAddresses: List<String> = emptyList(),
    val isDirectFact: Boolean = true,
    val rawSize: Int = 0,
    val notes: String = "",
    val assetSymbol: String = "BTC",
    val isTokenTransfer: Boolean = false,
    val tokenContract: String? = null,
    val tokenStandard: String? = null,
    val rawMetadata: Map<String, String> = emptyMap(),
    val provenance: ProvenanceRecord? = null,
    val totalInputValueSat: Long = 0,
    val totalOutputValueSat: Long = 0
) {
    val txid: String get() = txId
    val feeBtc: Double get() = feeSat.toDouble() / 100_000_000.0
    val relevantAmountBtc: Double get() = relevantAmountSat.toDouble() / 100_000_000.0
}

@Serializable
data class CounterpartySummary(
    val address: String,
    val network: BlockchainNetwork = BlockchainNetwork.BITCOIN,
    val addressType: AddressType = AddressType.UNKNOWN,
    val txCount: Int,
    val totalReceivedSatFromCounterparty: Long = 0,
    val totalSentSatToCounterparty: Long = 0,
    val netVolumeSat: Long = 0,
    val firstSeenTimestamp: Long = 0,
    val lastSeenTimestamp: Long = 0,
    val interactionDirection: TxDirection = TxDirection.INCOMING,
    val label: String? = null,
    val entityType: String? = null,
    val riskSeverity: RiskSeverity = RiskSeverity.INFO,
    val riskTags: List<String> = emptyList()
) {
    val totalReceivedBtc: Double get() = totalReceivedSatFromCounterparty.toDouble() / 100_000_000.0
    val totalSentBtc: Double get() = totalSentSatToCounterparty.toDouble() / 100_000_000.0
    val netVolumeBtc: Double get() = netVolumeSat.toDouble() / 100_000_000.0
}

@Serializable
data class RiskIndicator(
    val id: String,
    val code: String,
    val title: String,
    val severity: RiskSeverity,
    val category: String,
    val description: String,
    val matchingScore: Double,          // 0.0 to 100.0
    val confidence: ConfidenceLevel,
    val relatedAddresses: List<String> = emptyList(),
    val relatedTxHashes: List<String> = emptyList(),
    val recommendedAction: String = ""
)

@Serializable
enum class InvestigationStatus {
    DRAFT,
    INITIALIZING,
    FETCHING_DATA,
    ANALYZING_FLOWS,
    COMPLETED,
    PARTIALLY_COMPLETED,
    FAILED,
    ARCHIVED
}

@Serializable
@androidx.room.Entity(tableName = "investigation_cases")
data class InvestigationCase(
    @androidx.room.PrimaryKey 
    val caseId: String,
    val referenceNumber: String,
    val caseName: String,
    val targetAddress: String,
    val network: BlockchainNetwork = BlockchainNetwork.BITCOIN,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis(),
    val status: InvestigationStatus = InvestigationStatus.DRAFT,
    val description: String = "",
    val investigatorName: String = "Lead Forensic Analyst",
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val seedAddresses: List<String> = emptyList(),
    val searchDepth: Int = 1,
    val txLimit: Int = 50,
    val balanceSat: Long = 0,
    val totalReceivedSat: Long = 0,
    val totalSentSat: Long = 0,
    val totalTransactionsFound: Int = 0,
    val firstTxTimestamp: Long? = null,
    val lastTxTimestamp: Long? = null,
    val counterparties: List<CounterpartySummary> = emptyList(),
    val transactions: List<ForensicTransaction> = emptyList(),
    val evidenceLog: List<EvidenceItem> = emptyList(),
    val riskIndicators: List<RiskIndicator> = emptyList()
) {
    val balanceBtc: Double get() = balanceSat.toDouble() / 100_000_000.0
    val totalReceivedBtc: Double get() = totalReceivedSat.toDouble() / 100_000_000.0
    val totalSentBtc: Double get() = totalSentSat.toDouble() / 100_000_000.0

    val id: String get() = caseId
    val title: String get() = caseName
    val caseReferenceNumber: String get() = referenceNumber
    val blockchainNetwork: BlockchainNetwork get() = network
}
