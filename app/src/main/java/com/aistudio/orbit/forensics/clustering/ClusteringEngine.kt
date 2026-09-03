package com.aistudio.orbit.forensics.clustering

import com.aistudio.orbit.model.ForensicTransaction
import kotlinx.serialization.Serializable

/**
 * Union-Find (Disjoint-Set Union) Data Structure with path compression and union by rank.
 */
class UnionFind {
    private val parent = mutableMapOf<String, String>()
    private val rank = mutableMapOf<String, Int>()

    fun find(address: String): String {
        val root = parent.getOrPut(address) { address }
        if (root != address) {
            parent[address] = find(root) // Path compression
        }
        return parent[address]!!
    }

    fun union(addr1: String, addr2: String) {
        val root1 = find(addr1)
        val root2 = find(addr2)

        if (root1 != root2) {
            val rank1 = rank.getOrPut(root1) { 0 }
            val rank2 = rank.getOrPut(root2) { 0 }

            if (rank1 < rank2) {
                parent[root1] = root2
            } else if (rank1 > rank2) {
                parent[root2] = root1
            } else {
                parent[root2] = root1
                rank[root1] = rank1 + 1
            }
        }
    }

    fun getAllAddresses(): Set<String> = parent.keys
}

@Serializable
data class AddressCluster(
    val clusterId: String,
    val rootAddress: String,
    val memberAddresses: List<String>,
    val evidenceCount: Int,
    val confidenceScore: Float, // min(1.0, count / 5.0)
    val clusterNameEn: String = "Multi-Input Co-Spend Cluster",
    val clusterNameFa: String = "خوشه مالکان ورودی مشترک (CIOH)"
)

/**
 * ClusteringEngine
 * Implements Common Input Ownership Heuristic (CIOH) and CoinJoin round detection.
 */
object ClusteringEngine {

    /**
     * Clusters addresses using CIOH (Common Input Ownership Heuristic).
     * When multiple addresses co-spend inputs in the same transaction, they belong to the same entity.
     */
    fun clusterCIOH(transactions: List<ForensicTransaction>): List<AddressCluster> {
        val uf = UnionFind()
        val evidenceCounts = mutableMapOf<String, Int>()

        for (tx in transactions) {
            val inputAddresses = tx.inputs.mapNotNull { it.prevOutAddress }.filter { it.isNotBlank() }.distinct()
            if (inputAddresses.size < 2) continue

            // Pairwise union of all inputs in this transaction
            for (i in inputAddresses.indices) {
                for (j in (i + 1) until inputAddresses.size) {
                    uf.union(inputAddresses[i], inputAddresses[j])
                }
            }

            val root = uf.find(inputAddresses[0])
            evidenceCounts[root] = (evidenceCounts[root] ?: 0) + 1
        }

        val rawClusters = mutableMapOf<String, MutableList<String>>()
        for (address in uf.getAllAddresses()) {
            val root = uf.find(address)
            rawClusters.getOrPut(root) { mutableListOf() }.add(address)
        }

        val finalClusters = mutableListOf<AddressCluster>()
        for ((root, members) in rawClusters) {
            var totalEvidence = 0
            for (m in members) {
                totalEvidence += (evidenceCounts[m] ?: 0)
            }
            // Linear confidence score: min(1.0, evidence / 5.0)
            val score = minOf(1.0f, (totalEvidence.coerceAtLeast(1) / 5.0f))

            finalClusters.add(
                AddressCluster(
                    clusterId = "CLUSTER_${root.take(6)}",
                    rootAddress = root,
                    memberAddresses = members.sorted(),
                    evidenceCount = totalEvidence.coerceAtLeast(1),
                    confidenceScore = score
                )
            )
        }

        return finalClusters.sortedByDescending { it.memberAddresses.size }
    }

    /**
     * Checks if a transaction matches the CoinJoin / Wasabi / Whirlpool signature:
     * - Minimum 3 inputs and 3 outputs
     * - At least 3 outputs have identical values (equal denomination mixing)
     */
    fun isCoinJoinTransaction(tx: ForensicTransaction): Boolean {
        if (tx.inputs.size < 3 || tx.outputs.size < 3) return false

        val outputValueCounts = tx.outputs.groupingBy { it.valueSat }.eachCount()
        return outputValueCounts.any { it.value >= 3 && it.key > 0 }
    }
}
