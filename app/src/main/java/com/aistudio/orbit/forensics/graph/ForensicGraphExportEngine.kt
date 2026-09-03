package com.aistudio.orbit.forensics.graph

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.aistudio.orbit.model.InvestigationCase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Forensic Graph Exporter supporting GraphML, GEXF, CSV, and JSON formats.
 */
object ForensicGraphExportEngine {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    /**
     * Exports graph data to Downloads folder and returns the filename.
     */
    fun exportGraph(
        context: Context,
        investigationCase: InvestigationCase,
        nodes: List<VisualInvestigationNode>,
        edges: List<VisualInvestigationEdge>,
        format: String
    ): String? {
        val timestamp = System.currentTimeMillis()
        val caseRef = investigationCase.referenceNumber.ifBlank { "CASE" }.replace("/", "_")
        val filename = when (format.uppercase()) {
            "GRAPHML" -> "Biyena_Graph_${caseRef}_$timestamp.graphml"
            "GEXF" -> "Biyena_Network_${caseRef}_$timestamp.gexf"
            "CSV_NODES" -> "Biyena_Nodes_${caseRef}_$timestamp.csv"
            "CSV_EDGES" -> "Biyena_Edges_${caseRef}_$timestamp.csv"
            else -> "Biyena_Investigation_${caseRef}_$timestamp.json"
        }

        val mimeType = when (format.uppercase()) {
            "GRAPHML", "GEXF" -> "application/xml"
            "CSV_NODES", "CSV_EDGES" -> "text/csv"
            else -> "application/json"
        }

        val content = when (format.uppercase()) {
            "GRAPHML" -> generateGraphML(investigationCase, nodes, edges)
            "GEXF" -> generateGEXF(investigationCase, nodes, edges)
            "CSV_NODES" -> generateNodesCSV(nodes)
            "CSV_EDGES" -> generateEdgesCSV(edges)
            else -> generateInvestigationJSON(investigationCase, nodes, edges)
        }

        return writeToStorage(context, filename, mimeType, content)
    }

    private fun writeToStorage(context: Context, filename: String, mimeType: String, content: String): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            return try {
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(content.toByteArray(Charsets.UTF_8))
                    }
                    filename
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            return try {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, filename)
                file.outputStream().use { os ->
                    os.write(content.toByteArray(Charsets.UTF_8))
                }
                filename
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun generateGraphML(
        investigationCase: InvestigationCase,
        nodes: List<VisualInvestigationNode>,
        edges: List<VisualInvestigationEdge>
    ): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"\n")
        sb.append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
        sb.append("         xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">\n")
        sb.append("  <!-- Biyena Forensic Evidence Graph Schema -->\n")
        sb.append("  <key id=\"label\" for=\"node\" attr.name=\"label\" attr.type=\"string\"/>\n")
        sb.append("  <key id=\"node_type\" for=\"node\" attr.name=\"node_type\" attr.type=\"string\"/>\n")
        sb.append("  <key id=\"risk_severity\" for=\"node\" attr.name=\"risk_severity\" attr.type=\"string\"/>\n")
        sb.append("  <key id=\"confidence\" for=\"node\" attr.name=\"confidence\" attr.type=\"string\"/>\n")
        sb.append("  <key id=\"balance\" for=\"node\" attr.name=\"balance\" attr.type=\"string\"/>\n")
        sb.append("  <key id=\"tx_count\" for=\"node\" attr.name=\"tx_count\" attr.type=\"int\"/>\n")
        sb.append("  <key id=\"evidence_ids\" for=\"node\" attr.name=\"evidence_ids\" attr.type=\"string\"/>\n")
        sb.append("  <key id=\"relationship\" for=\"edge\" attr.name=\"relationship\" attr.type=\"string\"/>\n")
        sb.append("  <key id=\"amount\" for=\"edge\" attr.name=\"amount\" attr.type=\"string\"/>\n")
        sb.append("  <key id=\"edge_confidence\" for=\"edge\" attr.name=\"confidence\" attr.type=\"string\"/>\n")
        sb.append("  <key id=\"epistemic_style\" for=\"edge\" attr.name=\"epistemic_style\" attr.type=\"string\"/>\n")
        sb.append("  <key id=\"edge_evidence\" for=\"edge\" attr.name=\"evidence_ids\" attr.type=\"string\"/>\n")
        sb.append("  <graph id=\"${investigationCase.id}\" edgedefault=\"directed\">\n")

        nodes.forEach { node ->
            val safeLabel = escapeXml(node.label)
            val safeEv = escapeXml(node.evidenceIds.joinToString(","))
            sb.append("    <node id=\"${escapeXml(node.id)}\">\n")
            sb.append("      <data key=\"label\">$safeLabel</data>\n")
            sb.append("      <data key=\"node_type\">${node.nodeType.name}</data>\n")
            sb.append("      <data key=\"risk_severity\">${node.riskSeverity.name}</data>\n")
            sb.append("      <data key=\"confidence\">${node.confidence.name}</data>\n")
            sb.append("      <data key=\"balance\">${escapeXml(node.balanceDisplay)}</data>\n")
            sb.append("      <data key=\"tx_count\">${node.txCount}</data>\n")
            sb.append("      <data key=\"evidence_ids\">$safeEv</data>\n")
            sb.append("    </node>\n")
        }

        edges.forEach { edge ->
            val safeRel = escapeXml(edge.relationshipType.name)
            val safeAmt = escapeXml(edge.amountDisplay)
            val safeEdgeEv = escapeXml(edge.evidenceIds.joinToString(","))
            sb.append("    <edge id=\"${escapeXml(edge.id)}\" source=\"${escapeXml(edge.sourceId)}\" target=\"${escapeXml(edge.targetId)}\">\n")
            sb.append("      <data key=\"relationship\">$safeRel</data>\n")
            sb.append("      <data key=\"amount\">$safeAmt</data>\n")
            sb.append("      <data key=\"edge_confidence\">${edge.confidence.name}</data>\n")
            sb.append("      <data key=\"epistemic_style\">${edge.epistemicStyle.name}</data>\n")
            sb.append("      <data key=\"edge_evidence\">$safeEdgeEv</data>\n")
            sb.append("    </edge>\n")
        }

        sb.append("  </graph>\n")
        sb.append("</graphml>")
        return sb.toString()
    }

    fun generateGEXF(
        investigationCase: InvestigationCase,
        nodes: List<VisualInvestigationNode>,
        edges: List<VisualInvestigationEdge>
    ): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gexf xmlns=\"http://www.gexf.net/1.2draft\" version=\"1.2\">\n")
        sb.append("  <meta lastmodifieddate=\"${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}\">\n")
        sb.append("    <creator>Biyena Forensic Intelligence Platform</creator>\n")
        sb.append("    <description>${escapeXml(investigationCase.caseName)}</description>\n")
        sb.append("  </meta>\n")
        sb.append("  <graph defaultedgetype=\"directed\" mode=\"static\">\n")
        sb.append("    <nodes>\n")
        nodes.forEach { node ->
            sb.append("      <node id=\"${escapeXml(node.id)}\" label=\"${escapeXml(node.label)}\" />\n")
        }
        sb.append("    </nodes>\n")
        sb.append("    <edges>\n")
        edges.forEach { edge ->
            sb.append("      <edge id=\"${escapeXml(edge.id)}\" source=\"${escapeXml(edge.sourceId)}\" target=\"${escapeXml(edge.targetId)}\" label=\"${escapeXml(edge.relationshipType.name)}\" />\n")
        }
        sb.append("    </edges>\n")
        sb.append("  </graph>\n")
        sb.append("</gexf>")
        return sb.toString()
    }

    fun generateNodesCSV(nodes: List<VisualInvestigationNode>): String {
        val sb = StringBuilder()
        sb.appendLine("NodeId,Label,Type,RiskSeverity,Confidence,Balance,TxCount,FirstSeen,LastSeen,EvidenceIds")
        nodes.forEach { n ->
            sb.appendLine("\"${n.id}\",\"${n.label}\",\"${n.nodeType.name}\",\"${n.riskSeverity.name}\",\"${n.confidence.name}\",\"${n.balanceDisplay}\",${n.txCount},${n.firstSeenTimestamp},${n.lastSeenTimestamp},\"${n.evidenceIds.joinToString(";")}\"")
        }
        return sb.toString()
    }

    fun generateEdgesCSV(edges: List<VisualInvestigationEdge>): String {
        val sb = StringBuilder()
        sb.appendLine("EdgeId,SourceId,TargetId,Relationship,EpistemicStyle,AmountDisplay,AmountSat,Confidence,Source,EvidenceIds")
        edges.forEach { e ->
            sb.appendLine("\"${e.id}\",\"${e.sourceId}\",\"${e.targetId}\",\"${e.relationshipType.name}\",\"${e.epistemicStyle.name}\",\"${e.amountDisplay}\",${e.amountSat},\"${e.confidence.name}\",\"${e.source}\",\"${e.evidenceIds.joinToString(";")}\"")
        }
        return sb.toString()
    }

    private fun generateInvestigationJSON(
        investigationCase: InvestigationCase,
        nodes: List<VisualInvestigationNode>,
        edges: List<VisualInvestigationEdge>
    ): String {
        val payload = mapOf(
            "caseId" to investigationCase.id,
            "referenceNumber" to investigationCase.referenceNumber,
            "caseName" to investigationCase.caseName,
            "network" to investigationCase.network.symbol,
            "exportedAt" to System.currentTimeMillis(),
            "nodeCount" to nodes.size,
            "edgeCount" to edges.size,
            "nodes" to nodes,
            "edges" to edges
        )
        return json.encodeToString(payload)
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
