package com.aistudio.orbit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GraphView(
    graph: Graph, 
    modifier: Modifier = Modifier,
    onSetAsSeed: ((String) -> Unit)? = null
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var hasAutoFit by remember(graph) { mutableStateOf(false) }
    var selectedNode by remember(graph) { mutableStateOf<Node?>(null) }
    
    val nodeColor = MaterialTheme.colorScheme.primary
    val edgeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val highlightColor = Color(0xFFF57C00) // Amber / orange contrast

    val nodesMap = remember(graph.nodes) { graph.nodes.associateBy { it.id } }

    LaunchedEffect(graph, viewSize, hasAutoFit) {
        if (!hasAutoFit && viewSize.width > 0 && viewSize.height > 0 && graph.nodes.isNotEmpty()) {
            var minX = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var minY = Float.MAX_VALUE
            var maxY = Float.MIN_VALUE
            for (node in graph.nodes) {
                if (node.x < minX) minX = node.x
                if (node.x > maxX) maxX = node.x
                if (node.y < minY) minY = node.y
                if (node.y > maxY) maxY = node.y
            }
            val graphWidth = (maxX - minX).coerceAtLeast(1f)
            val graphHeight = (maxY - minY).coerceAtLeast(1f)
            val scaleX = viewSize.width * 0.8f / graphWidth
            val scaleY = viewSize.height * 0.8f / graphHeight
            scale = minOf(scaleX, scaleY).coerceIn(0.1f, 5f)
            
            val graphCenterX = (minX + maxX) / 2f
            val graphCenterY = (minY + maxY) / 2f
            offset = Offset(-graphCenterX * scale, -graphCenterY * scale)
            hasAutoFit = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewSize = it }
                .pointerInput(graph) {
                    detectTapGestures { tapOffset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        var foundNode: Node? = null
                        for (node in graph.nodes.asReversed()) {
                            val nodeCenter = Offset(
                                center.x + offset.x + node.x * scale,
                                center.y + offset.y + node.y * scale
                            )
                            val distance = (tapOffset - nodeCenter).getDistance()
                            val touchRadius = (node.size * 3f * scale).coerceAtLeast(30f)
                            if (distance <= touchRadius + 20f) {
                                foundNode = node
                                break
                            }
                        }
                        if (foundNode != null) {
                            selectedNode = foundNode
                            offset = Offset(-foundNode.x * scale, -foundNode.y * scale)
                        } else {
                            selectedNode = null
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val newScale = (scale * zoom).coerceIn(0.01f, 50f)
                        val factor = newScale / oldScale
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val zoomOffset = (centroid - center - offset) * (1 - factor)
                        offset = offset + pan + zoomOffset
                        scale = newScale
                    }
                }
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            
            // Draw Edges
            for (edge in graph.edges) {
                val sourceNode = nodesMap[edge.source]
                val targetNode = nodesMap[edge.target]
                if (sourceNode != null && targetNode != null) {
                    val start = Offset(
                        center.x + offset.x + sourceNode.x * scale,
                        center.y + offset.y + sourceNode.y * scale
                    )
                    val end = Offset(
                        center.x + offset.x + targetNode.x * scale,
                        center.y + offset.y + targetNode.y * scale
                    )
                    drawLine(
                        color = edgeColor,
                        start = start,
                        end = end,
                        strokeWidth = (edge.size * scale).coerceAtLeast(1f)
                    )
                }
            }
            
            // Draw Nodes
            for (node in graph.nodes) {
                val nodeCenter = Offset(
                    center.x + offset.x + node.x * scale,
                    center.y + offset.y + node.y * scale
                )
                val isSelected = node == selectedNode
                
                if (isSelected) {
                    drawCircle(
                        color = highlightColor,
                        radius = (node.size * 3f * scale).coerceAtLeast(4f) + 12f * scale.coerceAtMost(2f),
                        center = nodeCenter,
                        style = Stroke(width = 3f * scale.coerceAtMost(2f))
                    )
                }
                
                drawCircle(
                    color = if (isSelected) highlightColor else nodeColor,
                    radius = (node.size * 3f * scale).coerceAtLeast(4f),
                    center = nodeCenter
                )
            }
        }

        // On-screen Navigation & Zoom Controls
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { scale = (scale * 1.3f).coerceIn(0.01f, 50f) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("➕", fontSize = 16.sp)
                }
                IconButton(
                    onClick = { scale = (scale / 1.3f).coerceIn(0.01f, 50f) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("➖", fontSize = 16.sp)
                }
                IconButton(
                    onClick = { 
                        hasAutoFit = false // Re-triggers auto-fit LaunchedEffect
                        selectedNode = null
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("🎯", fontSize = 16.sp)
                }
            }
        }

        // Interactive Selected Node Details Card
        selectedNode?.let { node ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "جزئیات کیف پول انتخاب شده",
                            style = MaterialTheme.typography.titleSmall,
                            color = highlightColor,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { selectedNode = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "آدرس: ${node.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "تعداد تراکنش‌ها/اتصالات شناسایی شده: ${node.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (onSetAsSeed != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { onSetAsSeed(node.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = highlightColor)
                            ) {
                                Text("انتخاب به عنوان آدرس پایه جدید", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
