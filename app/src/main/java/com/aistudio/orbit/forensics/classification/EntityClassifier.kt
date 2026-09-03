package com.aistudio.orbit.forensics.classification

import com.aistudio.orbit.forensics.AddressValidator
import com.aistudio.orbit.model.*

object EntityClassifier {

    // Pre-indexed known cluster heuristic signatures and exchange prefix rules
    fun classifyAddress(
        address: String,
        network: BlockchainNetwork,
        txCount: Int,
        counterpartyCount: Int,
        hasConsolidation: Boolean,
        hasFanOut: Boolean
    ): AddressClassification {
        val validation = AddressValidator.validate(address, network)
        val lower = address.lowercase()

        // 1. Known special addresses or prefixes
        if (lower.startsWith("0x0000000000000000000000000000000000000000") || lower.startsWith("0x000000000000000000000000000000000000dead")) {
            return AddressClassification(
                address = address,
                network = network,
                classification = EntityClassificationType.SMART_CONTRACT,
                confidence = ConfidenceLevel.DEFINITIVE_FACT,
                evidenceSource = "Standard EVM Genesis / Burn Contract Signature",
                knownEntityName = "Null / Burn Address",
                attributionTags = listOf("EVM_NULL", "BURN_ADDRESS")
            )
        }

        // 2. High-volume Exchange Hot Wallet Signature Heuristics
        if (txCount > 500 && counterpartyCount > 100) {
            return AddressClassification(
                address = address,
                network = network,
                classification = EntityClassificationType.EXCHANGE_HOT_WALLET,
                confidence = ConfidenceLevel.MEDIUM_CONFIDENCE,
                evidenceSource = "High-Degree Transaction Volume & Counterparty Fan-In/Fan-Out Heuristic",
                knownEntityName = "Likely Centralized Exchange Hot Wallet / Aggregator",
                attributionTags = listOf("HIGH_VOLUME", "MULTI_COUNTERPARTY", "HOT_WALLET_CLUSTER")
            )
        }

        // 3. Mining Pool Payout Heuristic
        if (hasFanOut && txCount > 50 && counterpartyCount > 30) {
            return AddressClassification(
                address = address,
                network = network,
                classification = EntityClassificationType.MINING_POOL,
                confidence = ConfidenceLevel.MEDIUM_CONFIDENCE,
                evidenceSource = "Regular Multi-Output Reward Distribution Signature",
                knownEntityName = "Mining Pool / High-Fanout Payout System",
                attributionTags = listOf("MINING_DISTRIBUTION", "BATCH_PAYOUTS")
            )
        }

        // 4. Consolidation / Sweep Address Heuristic
        if (hasConsolidation && txCount > 20) {
            return AddressClassification(
                address = address,
                network = network,
                classification = EntityClassificationType.MERCHANT_PROCESSOR,
                confidence = ConfidenceLevel.LOW_CONFIDENCE,
                evidenceSource = "Multi-Input Aggregation Sweep Topology",
                knownEntityName = "Merchant Processor / Collection Address",
                attributionTags = listOf("SWEEP_COLLECTOR", "AGGREGATION_NODE")
            )
        }

        // 5. Individual / Personal Unhosted Wallet
        return AddressClassification(
            address = address,
            network = network,
            classification = EntityClassificationType.INDIVIDUAL_WALLET,
            confidence = ConfidenceLevel.LOW_CONFIDENCE,
            evidenceSource = "Standard Single-Party Ledger Activity Profile",
            knownEntityName = "Personal / Unhosted Wallet",
            attributionTags = listOf("UNHOSTED_WALLET", validation.addressType.name)
        )
    }
}
