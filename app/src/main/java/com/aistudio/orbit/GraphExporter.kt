package com.aistudio.orbit

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream

object GraphExporter {

    fun saveGraphToDownloads(context: Context, graph: Graph, format: String): String? {
        val extension = when (format) {
            "JSON" -> "json"
            "JS" -> "js"
            else -> "graphml"
        }
        val mimeType = when (format) {
            "JSON" -> "application/json"
            "JS" -> "application/javascript"
            else -> "text/xml"
        }
        val filename = "crypto_graph_${System.currentTimeMillis()}.$extension"

        val content = when (format) {
            "JSON" -> Json.encodeToString(graph)
            "JS" -> "var rendru = " + Json.encodeToString(graph) + ";"
            else -> generateGraphML(graph)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            return try {
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                    filename
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            return try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = java.io.File(downloadsDir, filename)
                file.outputStream().use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                filename
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun generateGraphML(graph: Graph): String {
        val sb = java.lang.StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\">\n")
        sb.append("  <key id=\"label\" for=\"node\" attr.name=\"label\" attr.type=\"string\"/>\n")
        sb.append("  <key id=\"size\" for=\"node\" attr.name=\"size\" attr.type=\"int\"/>\n")
        sb.append("  <key id=\"edge_size\" for=\"edge\" attr.name=\"size\" attr.type=\"int\"/>\n")
        sb.append("  <graph id=\"G\" edgedefault=\"directed\">\n")
        
        graph.nodes.forEach { node ->
            sb.append("    <node id=\"${node.id}\">\n")
            sb.append("      <data key=\"label\">${node.label}</data>\n")
            sb.append("      <data key=\"size\">${node.size}</data>\n")
            sb.append("    </node>\n")
        }
        
        graph.edges.forEach { edge ->
            sb.append("    <edge id=\"${edge.id}\" source=\"${edge.source}\" target=\"${edge.target}\">\n")
            sb.append("      <data key=\"edge_size\">${edge.size}</data>\n")
            sb.append("    </edge>\n")
        }
        
        sb.append("  </graph>\n")
        sb.append("</graphml>")
        return sb.toString()
    }
}
