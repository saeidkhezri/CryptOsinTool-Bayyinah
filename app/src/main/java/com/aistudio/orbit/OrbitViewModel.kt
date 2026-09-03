package com.aistudio.orbit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

@Serializable
data class Node(val id: String, val label: String, val size: Int, val x: Float, val y: Float)
@Serializable
data class Edge(val id: String, val source: String, val target: String, val size: Int)
@Serializable
data class Graph(val nodes: List<Node>, val edges: List<Edge>)

class OrbitViewModel : ViewModel() {
    private val apiSemaphore = Semaphore(2) // Intelligent queueing: Limit concurrent API connections to 2
    
    val seeds = MutableStateFlow("")
    val depth = MutableStateFlow("3")
    val limit = MutableStateFlow("100")
    val topCount = MutableStateFlow("20")
    
    val isCrawling = MutableStateFlow(false)
    val progressMessage = MutableStateFlow("")
    val graphData = MutableStateFlow<Graph?>(null)

    val savedResults = MutableStateFlow<List<SavedSearchResult>>(emptyList())
    private lateinit var savedResultsRepository: SavedResultsRepository

    fun initRepository(context: Context) {
        savedResultsRepository = SavedResultsRepository(context)
        loadSavedResults()
    }

    fun loadSavedResults() {
        if (::savedResultsRepository.isInitialized) {
            savedResults.value = savedResultsRepository.loadResults()
        }
    }

    fun saveCurrentResult(note: String = "") {
        val currentGraph = graphData.value ?: return
        if (!::savedResultsRepository.isInitialized) return
        
        val timestamp = System.currentTimeMillis()
        val pDate = PersianDateUtils.getPersianDateTime(timestamp)
        val result = SavedSearchResult(
            id = "res_${timestamp}",
            timestamp = timestamp,
            persianDate = pDate,
            seeds = seeds.value,
            depth = depth.value,
            limit = limit.value,
            topCount = topCount.value,
            graph = currentGraph,
            note = note
        )
        savedResultsRepository.saveResult(result)
        loadSavedResults()
    }

    fun deleteSavedResult(id: String) {
        if (!::savedResultsRepository.isInitialized) return
        savedResultsRepository.deleteResult(id)
        loadSavedResults()
    }

    fun loadSavedGraph(savedResult: SavedSearchResult) {
        seeds.value = savedResult.seeds
        depth.value = savedResult.depth
        limit.value = savedResult.limit
        topCount.value = savedResult.topCount
        graphData.value = savedResult.graph
        progressMessage.value = "Loaded saved search from ${savedResult.persianDate}"
    }

    private fun roundMultiple(n: Int, m: Int): Int {
        val r = n % m
        return if (r + r >= m) n + m - r else n - r
    }

    private fun pageLimit(n: Int): Int {
        return (roundMultiple(n, 49) / 49) + 1
    }

    private fun ranker(database: Map<String, Map<String, Int>>, top: Int): MutableMap<String, MutableMap<String, Int>> {
        val newDatabase = mutableMapOf<String, MutableMap<String, Int>>()
        for ((node, connections) in database) {
            val topConnections = connections.entries.sortedByDescending { it.value }.take(top)
            newDatabase[node] = topConnections.associate { it.key to it.value }.toMutableMap()
        }
        return newDatabase
    }

    private fun getNew(database: Map<String, Map<String, Int>>, processed: Set<String>): Set<String> {
        val new = mutableSetOf<String>()
        for ((address, children) in database) {
            if (address !in processed) new.add(address)
            for (child in children.keys) {
                if (child !in processed) new.add(child)
            }
        }
        return new
    }

    private fun genLocation(): Pair<Float, Float> {
        var x = (1..800).random().toFloat()
        var y = (1..500).random().toFloat()
        if (Random.nextBoolean()) x = -x
        if (Random.nextBoolean()) y = -y
        return x to y
    }

    fun startCrawl(strings: AppStrings) {
        if (isCrawling.value) return
        val d = depth.value.toIntOrNull() ?: 3
        val l = limit.value.toIntOrNull() ?: 100
        val t = topCount.value.toIntOrNull() ?: 20
        val seedList = seeds.value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        if (seedList.isEmpty()) {
            progressMessage.value = strings.errorProvideSeed
            return
        }

        viewModelScope.launch {
            isCrawling.value = true
            graphData.value = null
            progressMessage.value = strings.starting
            
            try {
                var database = mutableMapOf<String, MutableMap<String, Int>>()
                val processed = mutableSetOf<String>()
                
                seedList.forEach { database[it] = mutableMapOf() }
                
                for (i in 0 until d) {
                    progressMessage.value = "Level ${i + 1}"
                    database = ranker(database, t + 1)
                    val toBeProcessed = getNew(database, processed)
                    
                    var current = 0
                    val total = toBeProcessed.size
                    
                    withContext(Dispatchers.IO) {
                        toBeProcessed.chunked(3).forEach { batch ->
                            batch.map { address ->
                                async {
                                    val newAddrs = mutableListOf<String>()
                                    var mempoolSuccess = false
                                    try {
                                        apiSemaphore.withPermit {
                                            // Dynamic stagger/jitter delay to prevent simultaneous burst requests
                                            kotlinx.coroutines.delay(Random.nextLong(150, 450))
                                            val mempoolUrl = "https://mempool.space/api/address/$address/txs"
                                            val url = URL(mempoolUrl)
                                            val connection = url.openConnection() as HttpURLConnection
                                            connection.requestMethod = "GET"
                                            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                                            connection.connectTimeout = 8000
                                            connection.readTimeout = 8000
                                            val responseCode = connection.responseCode
                                            if (responseCode == 200) {
                                                val response = connection.inputStream.bufferedReader().use { it.readText() }
                                                val regex = """"scriptpubkey_address":"(.*?)"""".toRegex()
                                                val found = regex.findAll(response).map { it.groupValues[1] }.toList()
                                                if (found.isNotEmpty()) {
                                                    newAddrs.addAll(found)
                                                    mempoolSuccess = true
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                    if (!mempoolSuccess) {
                                        var increment = 0
                                        val pages = pageLimit(l)
                                        for (p in 0 until pages) {
                                            var success = false
                                            var attempt = 0
                                            while (!success && attempt < 3) {
                                                val urlStr = "https://blockchain.info/rawaddr/$address?offset=$increment"
                                                try {
                                                    apiSemaphore.withPermit {
                                                        // Dynamic stagger/jitter delay to prevent simultaneous burst requests
                                                        kotlinx.coroutines.delay(Random.nextLong(150, 450))
                                                        
                                                        val url = URL(urlStr)
                                                        val connection = url.openConnection() as HttpURLConnection
                                                        connection.requestMethod = "GET"
                                                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                                                        connection.connectTimeout = 10000
                                                        connection.readTimeout = 10000
                                                        val responseCode = connection.responseCode
                                                        if (responseCode == 200) {
                                                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                                                            val regex = """"addr":"(.*?)"""".toRegex()
                                                            newAddrs.addAll(regex.findAll(response).map { it.groupValues[1] })
                                                            success = true
                                                        } else if (responseCode == 429) {
                                                            attempt++
                                                            // Intelligent backoff policy (e.g., 3s, 6s, 9s)
                                                            kotlinx.coroutines.delay(3000L * attempt)
                                                        } else {
                                                            attempt++
                                                            kotlinx.coroutines.delay(1500L)
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    attempt++
                                                    kotlinx.coroutines.delay(2000L * attempt)
                                                }
                                            }
                                            increment += 50
                                            kotlinx.coroutines.delay(400L) // base delay between pages
                                        }
                                    }
                                    Pair(address, newAddrs)
                                }
                            }.awaitAll().forEach { (address, newAddrs) ->
                                processed.add(address)
                                database[address] = mutableMapOf()
                                newAddrs.forEach { addr ->
                                    database[address]!![addr] = (database[address]!![addr] ?: 0) + 1
                                }
                                current++
                                progressMessage.value = "$current / $total (Level ${i + 1}/$d)"
                            }
                        }
                    }
                }
                
                progressMessage.value = "..."
                database = ranker(database, t)
                
                val doneNodes = mutableSetOf<String>()
                val doneEdges = mutableSetOf<String>()
                
                val nodes = mutableListOf<Node>()
                val edges = mutableListOf<Edge>()
                
                for ((node, children) in database) {
                    var size = children.size
                    if (size > 20) size = 20
                    if (node !in doneNodes) {
                        doneNodes.add(node)
                        val (x, y) = genLocation()
                        nodes.add(Node(node, node, size, x, y))
                    }
                    
                    for ((childNode, uniqueSizeVal) in children) {
                        var uniqueSize = uniqueSizeVal
                        if (uniqueSize > 20) uniqueSize = 20
                        if (childNode !in doneNodes) {
                            doneNodes.add(childNode)
                            val (x, y) = genLocation()
                            nodes.add(Node(childNode, childNode, uniqueSize, x, y))
                        }
                        val edgeId1 = "$node:$childNode"
                        val edgeId2 = "$childNode:$node"
                        if (edgeId1 !in doneEdges && edgeId2 !in doneEdges) {
                            doneEdges.add(edgeId1)
                            doneEdges.add(edgeId2)
                            edges.add(Edge(edgeId1, childNode, node, if (uniqueSize > 3) uniqueSize / 3 else uniqueSize))
                        }
                    }
                }
                
                graphData.value = Graph(nodes, edges)
                progressMessage.value = String.format(strings.doneWallets, nodes.size, edges.size)
            } catch (e: Exception) {
                progressMessage.value = "Error: ${e.message}"
                e.printStackTrace()
            } finally {
                isCrawling.value = false
            }
        }
    }
}
