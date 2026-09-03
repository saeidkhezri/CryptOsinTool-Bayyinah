package com.aistudio.orbit.ui.components.graph

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.forensics.graph.*
import com.aistudio.orbit.model.RiskSeverity
import kotlin.math.*

private fun computeNodeDepths(
    nodes: List<VisualInvestigationNode>,
    edges: List<VisualInvestigationEdge>
): Map<String, Int> {
    val targetIds = nodes.filter { it.isTarget || it.isSeed }.map { it.id }.toSet()
    val depths = mutableMapOf<String, Int>()
    val queue = ArrayDeque<String>()

    targetIds.forEach { id ->
        depths[id] = 1
        queue.add(id)
    }

    // Build adjacency list
    val adj = mutableMapOf<String, MutableList<String>>()
    edges.forEach { edge ->
        adj.getOrPut(edge.sourceId) { mutableListOf() }.add(edge.targetId)
        adj.getOrPut(edge.targetId) { mutableListOf() }.add(edge.sourceId)
    }

    while (queue.isNotEmpty()) {
        val curr = queue.removeFirst()
        val currDepth = depths[curr] ?: 1
        adj[curr]?.forEach { neighbor ->
            if (!depths.containsKey(neighbor)) {
                depths[neighbor] = currDepth + 1
                queue.add(neighbor)
            }
        }
    }

    return depths
}

/**
 * 2D & Large-Graph Interactive Forensic Canvas inspired by Cytoscape.js and Sigma.js.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun ForensicCytoscapeCanvas(
    nodes: List<VisualInvestigationNode>,
    edges: List<VisualInvestigationEdge>,
    selectedNodeId: String?,
    selectedEdgeId: String?,
    highlightedPathNodeIds: List<String>,
    highlightedPathEdgeIds: List<String>,
    filterState: VisualFilterState,
    isLargeGraphMode: Boolean,
    onNodeSelected: (VisualInvestigationNode) -> Unit,
    onEdgeSelected: (VisualInvestigationEdge) -> Unit,
    onBackgroundClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFlowAnimationEnabled: Boolean = true,
    maxHopDepth: Int = 3
) {
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val textMeasurer = rememberTextMeasurer()

    // Infinite animation transition for continuous fund flow looping
    val infiniteTransition = rememberInfiniteTransition(label = "EdgeFlow")
    val animFraction = if (isFlowAnimationEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "FlowFraction"
        ).value
    } else {
        0f
    }

    // Local mutable state for node positions for dragging
    val nodePositions = remember(nodes) {
        nodes.associate { it.id to mutableStateOf(Offset(it.x, it.y)) }
    }

    var draggedNodeId by remember { mutableStateOf<String?>(null) }

    // Compute hop-depths dynamically starting from seeds/targets
    val nodeDepths = remember(nodes, edges) { computeNodeDepths(nodes, edges) }

    // Color definitions
    val bgColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    val textPrimaryColor = MaterialTheme.colorScheme.onSurface
    val textSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.25f, 4.0f)
                    offset += pan
                }
            }
            .pointerInput(nodes, scale, offset) {
                detectTapGestures { tapOffset ->
                    val worldX = (tapOffset.x - size.width / 2f - offset.x) / scale
                    val worldY = (tapOffset.y - size.height / 2f - offset.y) / scale

                    // Hit test nodes (front-to-back)
                    val clickedNode = nodes.lastOrNull { node ->
                        val pos = nodePositions[node.id]?.value ?: Offset(node.x, node.y)
                        val dx = worldX - pos.x
                        val dy = worldY - pos.y
                        sqrt(dx * dx + dy * dy) <= (node.radius + 10f)
                    }

                    if (clickedNode != null) {
                        onNodeSelected(clickedNode)
                    } else {
                        // Check edge hit-test
                        val clickedEdge = edges.lastOrNull { edge ->
                            val p1 = nodePositions[edge.sourceId]?.value ?: Offset.Zero
                            val p2 = nodePositions[edge.targetId]?.value ?: Offset.Zero
                            distancePointToSegment(worldX, worldY, p1.x, p1.y, p2.x, p2.y) <= 15f
                        }
                        if (clickedEdge != null) {
                            onEdgeSelected(clickedEdge)
                        } else {
                            onBackgroundClick()
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f) + offset

            // 1. Draw Forensic Grid Background
            drawForensicGrid(center, scale, size, gridColor)

            // 2. Filter nodes and edges based on state and depth
            val visibleNodes = if (isLargeGraphMode) {
                // In large graph mode (Sigma.js style), prioritize degree and high risk
                nodes.filter { it.txCount > 0 || it.isTarget || it.isSeed || it.riskSeverity != RiskSeverity.INFO }
            } else {
                nodes.filter { node ->
                    val depth = nodeDepths[node.id] ?: 3
                    depth <= maxHopDepth &&
                    filterState.selectedNodeTypes.contains(node.nodeType) &&
                    (!filterState.showHighRiskOnly || node.riskSeverity in listOf(RiskSeverity.HIGH, RiskSeverity.CRITICAL)) &&
                    (!filterState.showSanctionedOnly || node.sanctionsMatch) &&
                    (!filterState.showOnChainOnly || !node.isOffChain) &&
                    (!filterState.showOffChainOnly || node.isOffChain)
                }
            }
            val visibleNodeIds = visibleNodes.map { it.id }.toSet()

            val visibleEdges = edges.filter { edge ->
                visibleNodeIds.contains(edge.sourceId) && visibleNodeIds.contains(edge.targetId) &&
                filterState.selectedEdgeTypes.contains(edge.relationshipType)
            }

            // 3. Draw Edges
            visibleEdges.forEach { edge ->
                val p1 = (nodePositions[edge.sourceId]?.value ?: Offset.Zero) * scale + center
                val p2 = (nodePositions[edge.targetId]?.value ?: Offset.Zero) * scale + center

                val isHighlighted = highlightedPathEdgeIds.contains(edge.id) || selectedEdgeId == edge.id
                val isNeighbor = selectedNodeId != null && (edge.sourceId == selectedNodeId || edge.targetId == selectedNodeId)

                drawForensicEdge(
                    edge = edge,
                    p1 = p1,
                    p2 = p2,
                    scale = scale,
                    isHighlighted = isHighlighted,
                    isNeighbor = isNeighbor,
                    dimmed = selectedNodeId != null && !isNeighbor && !isHighlighted,
                    isFlowAnimationEnabled = isFlowAnimationEnabled,
                    animFraction = animFraction
                )
            }

            // 4. Draw Nodes
            visibleNodes.forEach { node ->
                val pos = (nodePositions[node.id]?.value ?: Offset(node.x, node.y)) * scale + center
                val isSelected = node.id == selectedNodeId
                val isPathHighlighted = highlightedPathNodeIds.contains(node.id)
                val isNeighbor = selectedNodeId != null && visibleEdges.any {
                    (it.sourceId == selectedNodeId && it.targetId == node.id) ||
                    (it.targetId == selectedNodeId && it.sourceId == node.id)
                }

                drawForensicNode(
                    node = node,
                    center = pos,
                    scale = scale,
                    isSelected = isSelected,
                    isHighlighted = isPathHighlighted,
                    isNeighbor = isNeighbor,
                    dimmed = selectedNodeId != null && !isSelected && !isNeighbor && !isPathHighlighted,
                    textMeasurer = textMeasurer,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor
                )
            }
        }
    }
}

private fun DrawScope.drawForensicGrid(center: Offset, scale: Float, size: Size, color: Color) {
    val gridSize = 50f * scale
    val startX = (center.x % gridSize)
    val startY = (center.y % gridSize)

    var x = startX
    while (x < size.width) {
        drawLine(color = color, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
        x += gridSize
    }

    var y = startY
    while (y < size.height) {
        drawLine(color = color, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
        y += gridSize
    }
}

private fun DrawScope.drawForensicEdge(
    edge: VisualInvestigationEdge,
    p1: Offset,
    p2: Offset,
    scale: Float,
    isHighlighted: Boolean,
    isNeighbor: Boolean,
    dimmed: Boolean,
    isFlowAnimationEnabled: Boolean,
    animFraction: Float
) {
    val baseColor = when (edge.epistemicStyle) {
        EdgeEpistemicStyle.FACT -> Color(0xFF1976D2)       // Solid blue
        EdgeEpistemicStyle.DERIVED -> Color(0xFF00897B)    // Teal
        EdgeEpistemicStyle.INFERENCE -> Color(0xFF7E57C2)  // Purple
        EdgeEpistemicStyle.HYPOTHESIS -> Color(0xFFE65100) // Amber / Orange
        EdgeEpistemicStyle.CONTESTED -> Color(0xFFD32F2F)   // Red warning
        EdgeEpistemicStyle.REJECTED -> Color(0xFF757575)   // Muted gray
    }

    val edgeColor = when {
        isHighlighted -> Color(0xFFFF9100) // Glowing amber
        isNeighbor -> Color(0xFF29B6F6)
        dimmed -> baseColor.copy(alpha = 0.15f)
        else -> baseColor.copy(alpha = 0.75f)
    }

    val strokeWidth = (if (isHighlighted) 4.5f else if (isNeighbor) 3.5f else edge.strokeWidth) * scale.coerceIn(0.6f, 1.8f)

    val pathEffect = when (edge.epistemicStyle) {
        EdgeEpistemicStyle.FACT -> null
        EdgeEpistemicStyle.DERIVED -> PathEffect.dashPathEffect(floatArrayOf(15f, 6f), 0f)
        EdgeEpistemicStyle.INFERENCE -> PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
        EdgeEpistemicStyle.HYPOTHESIS -> PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        EdgeEpistemicStyle.CONTESTED -> PathEffect.dashPathEffect(floatArrayOf(10f, 4f, 2f, 4f), 0f)
        EdgeEpistemicStyle.REJECTED -> PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
    }

    // Draw main line
    drawLine(
        color = edgeColor,
        start = p1,
        end = p2,
        strokeWidth = strokeWidth,
        pathEffect = pathEffect
    )

    // Draw directional arrowhead
    if (edge.direction && scale > 0.4f) {
        val angle = atan2(p2.y - p1.y, p2.x - p1.x)
        val arrowLength = (14f * scale).coerceIn(8f, 22f)
        val arrowAngle = PI / 6

        // Position arrow slightly before destination node
        val midX = (p1.x + p2.x * 2) / 3f
        val midY = (p1.y + p2.y * 2) / 3f
        val tip = Offset(midX, midY)

        val arrowP1 = Offset(
            tip.x - arrowLength * cos(angle - arrowAngle).toFloat(),
            tip.y - arrowLength * sin(angle - arrowAngle).toFloat()
        )
        val arrowP2 = Offset(
            tip.x - arrowLength * cos(angle + arrowAngle).toFloat(),
            tip.y - arrowLength * sin(angle + arrowAngle).toFloat()
        )

        val arrowPath = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(arrowP1.x, arrowP1.y)
            lineTo(arrowP2.x, arrowP2.y)
            close()
        }

        drawPath(path = arrowPath, color = edgeColor)
    }

    // Continuous flow animation: small moving circles/arrows representing transaction currency transfer
    if (isFlowAnimationEnabled && !dimmed && scale > 0.35f) {
        val flowCount = 3
        for (i in 0 until flowCount) {
            val progress = (animFraction + (i.toFloat() / flowCount)) % 1.0f
            val px = p1.x + (p2.x - p1.x) * progress
            val py = p1.y + (p2.y - p1.y) * progress
            val flowPos = Offset(px, py)

            // Flowing particle
            drawCircle(
                color = if (isHighlighted) Color(0xFFFFD700) else Color(0xFF00E5FF),
                radius = (5f * scale).coerceIn(3f, 11f),
                center = flowPos
            )

            // Tiny directional arrowhead inside flow particle
            val angle = atan2(p2.y - p1.y, p2.x - p1.x)
            val flowArrowLen = (8f * scale).coerceIn(4f, 15f)
            val arrowAngle = PI / 6
            val fArrowP1 = Offset(
                flowPos.x - flowArrowLen * cos(angle - arrowAngle).toFloat(),
                flowPos.y - flowArrowLen * sin(angle - arrowAngle).toFloat()
            )
            val fArrowP2 = Offset(
                flowPos.x - flowArrowLen * cos(angle + arrowAngle).toFloat(),
                flowPos.y - flowArrowLen * sin(angle + arrowAngle).toFloat()
            )
            val fArrowPath = Path().apply {
                moveTo(flowPos.x, flowPos.y)
                lineTo(fArrowP1.x, fArrowP1.y)
                lineTo(fArrowP2.x, fArrowP2.y)
                close()
            }
            drawPath(
                path = fArrowPath,
                color = if (isHighlighted) Color(0xFFBF360C) else Color(0xFF0D47A1)
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawForensicNode(
    node: VisualInvestigationNode,
    center: Offset,
    scale: Float,
    isSelected: Boolean,
    isHighlighted: Boolean,
    isNeighbor: Boolean,
    dimmed: Boolean,
    textMeasurer: TextMeasurer,
    textPrimaryColor: Color,
    textSecondaryColor: Color
) {
    val nodeRadius = (node.radius * scale).coerceIn(12f, 75f)

    // Risk Border / Accent Color (Orthogonal from Confidence)
    val riskColor = when (node.riskSeverity) {
        RiskSeverity.CRITICAL -> Color(0xFFD32F2F) // Red
        RiskSeverity.HIGH -> Color(0xFFE65100)     // Deep Orange
        RiskSeverity.MEDIUM -> Color(0xFFFFA000)   // Amber
        RiskSeverity.LOW -> Color(0xFF388E3C)      // Green
        RiskSeverity.INFO -> Color(0xFF1976D2)     // Blue
    }

    // Node Container Background Fill
    val containerColor = when (node.nodeType) {
        VisualNodeType.ADDRESS -> Color(0xFF263238)
        VisualNodeType.TRANSACTION -> Color(0xFF37474F)
        VisualNodeType.BLOCK -> Color(0xFF455A64)
        VisualNodeType.CLUSTER -> Color(0xFF1B5E20)
        VisualNodeType.ENTITY, VisualNodeType.PERSON, VisualNodeType.ORGANIZATION -> Color(0xFF4A148C)
        VisualNodeType.EXCHANGE, VisualNodeType.VASP -> Color(0xFF0D47A1)
        VisualNodeType.MIXER -> Color(0xFFB71C1C)
        VisualNodeType.DOMAIN, VisualNodeType.URL -> Color(0xFF004D40)
        VisualNodeType.EMAIL, VisualNodeType.USERNAME -> Color(0xFF311B92)
        VisualNodeType.IP, VisualNodeType.ASN -> Color(0xFF880E4F)
        VisualNodeType.EVIDENCE -> Color(0xFFE65100)
        VisualNodeType.HYPOTHESIS -> Color(0xFFF57F17)
        VisualNodeType.FINDING, VisualNodeType.ALERT -> Color(0xFFBF360C)
        else -> Color(0xFF424242)
    }

    val alpha = if (dimmed) 0.25f else 1.0f

    // 1. Selection / Highlight Halo
    if (isSelected || isHighlighted || isNeighbor) {
        val haloColor = when {
            isSelected -> Color(0xFF00E5FF).copy(alpha = 0.4f * alpha)
            isHighlighted -> Color(0xFFFFD700).copy(alpha = 0.4f * alpha)
            else -> Color(0xFF29B6F6).copy(alpha = 0.25f * alpha)
        }
        drawCircle(
            color = haloColor,
            radius = nodeRadius + 10f * scale,
            center = center
        )
    }

    // 2. Draw Geometry based on Node Shape
    when (node.nodeType) {
        VisualNodeType.ADDRESS -> {
            // Circle
            drawCircle(color = containerColor.copy(alpha = alpha), radius = nodeRadius, center = center)
            drawCircle(
                color = riskColor.copy(alpha = alpha),
                radius = nodeRadius,
                center = center,
                style = Stroke(width = if (isSelected) 4f * scale else 2.5f * scale)
            )
        }

        VisualNodeType.TRANSACTION, VisualNodeType.BLOCK -> {
            // Square
            val size = nodeRadius * 1.8f
            val topLeft = Offset(center.x - size / 2, center.y - size / 2)
            drawRect(color = containerColor.copy(alpha = alpha), topLeft = topLeft, size = Size(size, size))
            drawRect(
                color = riskColor.copy(alpha = alpha),
                topLeft = topLeft,
                size = Size(size, size),
                style = Stroke(width = if (isSelected) 4f * scale else 2.5f * scale)
            )
        }

        VisualNodeType.ENTITY, VisualNodeType.PERSON, VisualNodeType.ORGANIZATION -> {
            // Rounded Rectangle
            val width = nodeRadius * 2.2f
            val height = nodeRadius * 1.5f
            val topLeft = Offset(center.x - width / 2, center.y - height / 2)
            drawRoundRect(
                color = containerColor.copy(alpha = alpha),
                topLeft = topLeft,
                size = Size(width, height),
                cornerRadius = CornerRadius(8f * scale, 8f * scale)
            )
            drawRoundRect(
                color = riskColor.copy(alpha = alpha),
                topLeft = topLeft,
                size = Size(width, height),
                cornerRadius = CornerRadius(8f * scale, 8f * scale),
                style = Stroke(width = if (isSelected) 4f * scale else 2.5f * scale)
            )
        }

        VisualNodeType.EXCHANGE, VisualNodeType.VASP -> {
            // Hexagon / Diamond
            val path = Path().apply {
                moveTo(center.x, center.y - nodeRadius * 1.1f)
                lineTo(center.x + nodeRadius * 1.1f, center.y)
                lineTo(center.x, center.y + nodeRadius * 1.1f)
                lineTo(center.x - nodeRadius * 1.1f, center.y)
                close()
            }
            drawPath(path = path, color = containerColor.copy(alpha = alpha))
            drawPath(
                path = path,
                color = riskColor.copy(alpha = alpha),
                style = Stroke(width = if (isSelected) 4f * scale else 2.5f * scale)
            )
        }

        VisualNodeType.HYPOTHESIS -> {
            // Dashed outline shape
            drawCircle(color = containerColor.copy(alpha = alpha * 0.7f), radius = nodeRadius, center = center)
            drawCircle(
                color = riskColor.copy(alpha = alpha),
                radius = nodeRadius,
                center = center,
                style = Stroke(width = 3f * scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f))
            )
        }

        else -> {
            // General Pill / Rounded Shape
            drawCircle(color = containerColor.copy(alpha = alpha), radius = nodeRadius, center = center)
            drawCircle(
                color = riskColor.copy(alpha = alpha),
                radius = nodeRadius,
                center = center,
                style = Stroke(width = 2.5f * scale)
            )
        }
    }

    // 3. Draw Labels if zoomed in sufficiently
    if (scale > 0.45f && !dimmed) {
        val labelText = if (node.label.length > 16) node.label.take(14) + ".." else node.label
        val textLayout = textMeasurer.measure(
            text = AnnotatedString(labelText),
            style = TextStyle(
                fontSize = (11 * scale).coerceIn(9f, 15f).sp,
                fontWeight = if (node.isSeed || node.isTarget || isSelected) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                color = textPrimaryColor
            )
        )

        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(center.x - textLayout.size.width / 2f, center.y + nodeRadius + 4f * scale)
        )

        // Subtitle (Balance or Volume)
        if (node.balanceDisplay.isNotBlank() && scale > 0.7f) {
            val subLayout = textMeasurer.measure(
                text = AnnotatedString(node.balanceDisplay),
                style = TextStyle(
                    fontSize = (9 * scale).coerceIn(8f, 13f).sp,
                    fontFamily = FontFamily.Monospace,
                    color = textSecondaryColor
                )
            )
            drawText(
                textLayoutResult = subLayout,
                topLeft = Offset(center.x - subLayout.size.width / 2f, center.y + nodeRadius + textLayout.size.height + 4f * scale)
            )
        }
    }
}

private fun distancePointToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    if (dx == 0f && dy == 0f) {
        val dpx = px - x1
        val dpy = py - y1
        return sqrt(dpx * dpx + dpy * dpy)
    }
    val t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)
    val clampedT = t.coerceIn(0f, 1f)
    val projX = x1 + clampedT * dx
    val projY = y1 + clampedT * dy
    val diffX = px - projX
    val diffY = py - projY
    return sqrt(diffX * diffX + diffY * diffY)
}
