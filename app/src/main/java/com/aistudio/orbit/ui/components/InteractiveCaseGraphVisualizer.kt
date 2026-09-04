package com.aistudio.orbit.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.forensics.graph.EntityFilterMode
import com.aistudio.orbit.forensics.graph.ForensicCaseGraphEngine
import com.aistudio.orbit.model.*
import com.aistudio.orbit.ui.components.designsystem.ForensicEpistemicBadge
import com.aistudio.orbit.ui.components.designsystem.ForensicEpistemicType
import kotlin.math.*

/**
 * High-performance, Canvas-based interactive case graph visualizer.
 * Integrates GraphSense node-link network topologies, Maltego/Epieos transform links,
 * and SpiderFoot visual risk correlations.
 */
@Composable
fun InteractiveCaseGraphVisualizer(
    initialGraph: InteractiveCaseGraph,
    isPersian: Boolean,
    modifier: Modifier = Modifier,
    onRunDeepReconForNode: ((InteractiveCaseNode) -> Unit)? = null,
    onAddNodeToEvidence: ((InteractiveCaseNode) -> Unit)? = null,
    onOpenProvenance: ((InteractiveCaseNode) -> Unit)? = null
) {
    val context = LocalContext.current

    // Viewport transform state
    var scale by remember { mutableStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Animated Path Flow State (Breadcrumbs.app inspired)
    var isPathAnimationEnabled by remember { mutableStateOf(true) }
    val infiniteTransition = rememberInfiniteTransition(label = "graph_flow_anim")
    val animationPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "path_phase"
    )

    // Graph data & filter state
    var filterMode by remember { mutableStateOf(EntityFilterMode.ALL) }
    val displayGraph = remember(initialGraph, filterMode) {
        ForensicCaseGraphEngine.filterGraph(initialGraph, filterMode)
    }

    // Selected node state for detailed inspection
    var selectedNode by remember { mutableStateOf<InteractiveCaseNode?>(null) }

    // Analytical highlighted paths (flow trace, shortest path, cycles)
    var highlightedNodeIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var highlightedEdgeIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var activeToolStatus by remember { mutableStateOf<String?>(null) }

    // Secondary selection for shortest-path routing
    var secondaryNodeForPath by remember { mutableStateOf<InteractiveCaseNode?>(null) }

    // Draggable card offset for the selected node inspector popup
    var dragOffset by remember(selectedNode) { mutableStateOf(Offset.Zero) }

    // Canvas coordinate translation helpers
    fun toScreenX(worldX: Float, canvasWidth: Float): Float = (canvasWidth / 2f) + (worldX * scale) + offset.x
    fun toScreenY(worldY: Float, canvasHeight: Float): Float = (canvasHeight / 2f) + (worldY * scale) + offset.y
    fun toWorldX(screenX: Float, canvasWidth: Float): Float = (screenX - (canvasWidth / 2f) - offset.x) / scale
    fun toWorldY(screenY: Float, canvasHeight: Float): Float = (screenY - (canvasHeight / 2f) - offset.y) / scale

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080C14))
            .testTag("interactive_case_graph_container")
    ) {
        // Main Interactive Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("forensic_graph_canvas")
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.25f, 4.0f)
                        offset += pan
                    }
                }
                .pointerInput(displayGraph, scale, offset) {
                    detectTapGestures { tapOffset ->
                        val canvasW = size.width.toFloat()
                        val canvasH = size.height.toFloat()
                        val worldTapX = toWorldX(tapOffset.x, canvasW)
                        val worldTapY = toWorldY(tapOffset.y, canvasH)

                        // Proximity hit-testing for nodes
                        val hitNode = displayGraph.nodes.firstOrNull { node ->
                            val dx = node.x - worldTapX
                            val dy = node.y - worldTapY
                            sqrt(dx * dx + dy * dy) <= (node.visualRadius + 14f)
                        }

                        if (hitNode != null) {
                            if (activeToolStatus?.startsWith("Shortest Path") == true && selectedNode != null && hitNode.id != selectedNode?.id) {
                                // Execute Shortest Path
                                secondaryNodeForPath = hitNode
                                val selected = selectedNode
                                if (selected == null) return@detectTapGestures
                                val path = ForensicCaseGraphEngine.findShortestPath(displayGraph, selected.id, hitNode.id)
                                if (path.isNotEmpty()) {
                                    highlightedNodeIds = path.toSet()
                                    val edgeIds = mutableSetOf<String>()
                                    for (i in 0 until (path.size - 1)) {
                                        val u = path[i]
                                        val v = path[i + 1]
                                        displayGraph.edges.firstOrNull {
                                            (it.sourceId == u && it.targetId == v) || (it.sourceId == v && it.targetId == u)
                                        }?.let { edgeIds.add(it.id) }
                                    }
                                    highlightedEdgeIds = edgeIds
                                    activeToolStatus = if (isPersian) "کوتاه‌ترین مسیر: ${path.size} گام" else "Shortest Path: ${path.size} hops"
                                } else {
                                    Toast.makeText(context, if (isPersian) "هیچ مسیری یافت نشد" else "No path between nodes", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                selectedNode = hitNode
                            }
                        } else {
                            // Tap outside: clear secondary selection if not in active tool
                            if (activeToolStatus == null) {
                                selectedNode = null
                                highlightedNodeIds = emptySet()
                                highlightedEdgeIds = emptySet()
                            }
                        }
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw Subtle Tech Grid Background
            drawForensicGrid(canvasW, canvasH, scale, offset)

            // 2. Draw Edges (Transfers & OSINT Transforms)
            val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

            displayGraph.edges.forEach { edge ->
                val srcNode = displayGraph.nodes.find { it.id == edge.sourceId }
                val tgtNode = displayGraph.nodes.find { it.id == edge.targetId }
                if (srcNode != null && tgtNode != null) {
                    val srcX = toScreenX(srcNode.x, canvasW)
                    val srcY = toScreenY(srcNode.y, canvasH)
                    val tgtX = toScreenX(tgtNode.x, canvasW)
                    val tgtY = toScreenY(tgtNode.y, canvasH)

                    // Culling: If both endpoints are outside the screen bounds on the same side, skip drawing
                    if ((srcX < -150f && tgtX < -150f) ||
                        (srcX > canvasW + 150f && tgtX > canvasW + 150f) ||
                        (srcY < -150f && tgtY < -150f) ||
                        (srcY > canvasH + 150f && tgtY > canvasH + 150f)
                    ) {
                        return@forEach
                    }

                    val selectedId = selectedNode?.id
                    val isHighlighted = edge.id in highlightedEdgeIds ||
                            (selectedId != null && (edge.sourceId == selectedId || edge.targetId == selectedId))

                    val (edgeColor, edgeStroke) = when (edge.category) {
                        ForensicEdgeCategory.ON_CHAIN_TRANSFER -> {
                            val color = if (isHighlighted) Color(0xFF00E5FF) else Color(0xFF37474F)
                            Pair(color, (edge.strokeWidth * scale).coerceIn(1.5f, 8f))
                        }
                        ForensicEdgeCategory.TRANSFORM_ATTRIBUTION -> {
                            val color = if (isHighlighted) Color(0xFFFFD54F) else Color(0xFF8E24AA)
                            Pair(color, 2.0f * scale)
                        }
                        ForensicEdgeCategory.NETWORK_PROPAGATION -> {
                            val color = if (isHighlighted) Color(0xFF00E676) else Color(0xFF00838F)
                            Pair(color, 1.8f * scale)
                        }
                        else -> Pair(Color(0xFF546E7A), 1.5f * scale)
                    }

                    val isDashed = edge.category != ForensicEdgeCategory.ON_CHAIN_TRANSFER

                    // Draw Edge Line
                    drawLine(
                        color = edgeColor,
                        start = Offset(srcX, srcY),
                        end = Offset(tgtX, tgtY),
                        strokeWidth = edgeStroke,
                        pathEffect = if (isDashed) dashedEffect else null
                    )

                    // Draw Directional Arrowhead
                    drawArrowHead(
                        srcX, srcY, tgtX, tgtY,
                        targetRadius = (tgtNode.visualRadius * scale),
                        color = edgeColor,
                        size = 12f * scale.coerceAtLeast(0.6f)
                    )

                    // Animated Flow Particle (Breadcrumbs.app flow effect)
                    if (isPathAnimationEnabled) {
                        val basePhase = (animationPhase + ((edge.id.hashCode().absoluteValue % 100) / 100f)) % 1f
                        val photonX = srcX + (tgtX - srcX) * basePhase
                        val photonY = srcY + (tgtY - srcY) * basePhase
                        val particleColor = when {
                            isHighlighted -> Color(0xFF00E5FF)
                            edge.category == ForensicEdgeCategory.ON_CHAIN_TRANSFER -> Color(0xFF00E676)
                            else -> Color(0xFFFFD54F)
                        }
                        drawCircle(
                            color = particleColor.copy(alpha = 0.85f),
                            radius = (3.5f * scale).coerceIn(2.5f, 6.5f),
                            center = Offset(photonX, photonY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = (1.5f * scale).coerceIn(1.2f, 3.0f),
                            center = Offset(photonX, photonY)
                        )
                    }

                    // Draw Edge Label Capsule (Progressive Detail: scale >= 0.95f or highlighted)
                    if (scale >= 0.95f || isHighlighted) {
                        val midX = (srcX + tgtX) / 2f
                        val midY = (srcY + tgtY) / 2f
                        val labelText = when {
                            edge.volumeDisplay.isNotBlank() -> edge.volumeDisplay
                            edge.transformLabel.isNotBlank() -> edge.transformLabel
                            else -> ""
                        }
                        if (labelText.isNotBlank()) {
                            drawEdgeLabelCapsule(
                                text = labelText,
                                cx = midX,
                                cy = midY,
                                scale = scale,
                                isHighlighted = isHighlighted
                            )
                        }
                    }
                }
            }

            // 3. Draw Nodes (On-Chain & Off-Chain)
            displayGraph.nodes.forEach { node ->
                val screenX = toScreenX(node.x, canvasW)
                val screenY = toScreenY(node.y, canvasH)

                // Viewport Culling Optimization: Skip nodes far outside visible canvas
                if (screenX < -120f || screenX > canvasW + 120f || screenY < -120f || screenY > canvasH + 120f) {
                    return@forEach
                }

                val r = (node.visualRadius * scale).coerceAtLeast(10f)

                val isSelected = selectedNode?.id == node.id
                val isHighlighted = node.id in highlightedNodeIds

                // Draw Outer Halo Ring if Target or Selected
                if (node.isTarget || isSelected || isHighlighted) {
                    val haloColor = when {
                        isSelected -> Color(0xFF00E5FF)
                        isHighlighted -> Color(0xFFFFD54F)
                        node.riskSeverity == RiskSeverity.CRITICAL -> Color(0xFFFF1744)
                        else -> Color(0xFF00E676)
                    }
                    drawCircle(
                        color = haloColor.copy(alpha = 0.28f),
                        radius = r + 14f * scale,
                        center = Offset(screenX, screenY)
                    )
                    drawCircle(
                        color = haloColor,
                        radius = r + 4f * scale,
                        center = Offset(screenX, screenY),
                        style = Stroke(width = 2.5f * scale)
                    )
                }

                // Determine Node Primary Color
                val nodeFillColor = when (node.entityCategory) {
                    ForensicEntityCategory.CRYPTO_WALLET -> if (node.isTarget) Color(0xFF00C853) else Color(0xFF1E88E5)
                    ForensicEntityCategory.EXCHANGE_HOT_WALLET -> Color(0xFFFB8C00)
                    ForensicEntityCategory.MIXER_OR_TUMBLER -> Color(0xFFD50000)
                    ForensicEntityCategory.SMART_CONTRACT -> Color(0xFF7E57C2)
                    ForensicEntityCategory.PHONE_NUMBER -> Color(0xFF00B0FF)
                    ForensicEntityCategory.EMAIL_ADDRESS -> Color(0xFFAB47BC)
                    ForensicEntityCategory.IP_NETWORK_NODE -> Color(0xFF26A69A)
                    ForensicEntityCategory.SOCIAL_ACCOUNT -> Color(0xFF3F51B5)
                    else -> Color(0xFF78909C)
                }

                // Draw Node Shape
                when (node.entityCategory) {
                    ForensicEntityCategory.MIXER_OR_TUMBLER -> {
                        drawDiamond(screenX, screenY, r, nodeFillColor)
                    }
                    ForensicEntityCategory.PHONE_NUMBER, ForensicEntityCategory.EMAIL_ADDRESS -> {
                        val halfSide = r * 0.9f
                        drawRoundRect(
                            color = nodeFillColor,
                            topLeft = Offset(screenX - halfSide, screenY - halfSide),
                            size = Size(halfSide * 2, halfSide * 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale, 6f * scale)
                        )
                    }
                    else -> {
                        drawCircle(
                            color = nodeFillColor,
                            radius = r,
                            center = Offset(screenX, screenY)
                        )
                    }
                }

                // Draw Distinct Entity Symbol inside node (GraphSense / Maltego transform glyphs)
                drawEntityGlyph(
                    cx = screenX,
                    cy = screenY,
                    radius = r,
                    category = node.entityCategory,
                    scale = scale
                )

                // Risk Dot Badge (Top-Right)
                if (node.riskSeverity in listOf(RiskSeverity.CRITICAL, RiskSeverity.HIGH)) {
                    drawCircle(
                        color = Color(0xFFFF1744),
                        radius = (4.5f * scale).coerceAtLeast(3f),
                        center = Offset(screenX + r * 0.7f, screenY - r * 0.7f)
                    )
                }

                // Progressive Zoom Level of Detail (LOD)
                // At scale >= 1.15f, render detailed node label pill & confidence
                if (scale >= 1.15f || isSelected) {
                    drawNodeDetailPill(
                        node = node,
                        cx = screenX,
                        cy = screenY + r + 6f * scale,
                        scale = scale,
                        isPersian = isPersian,
                        isSelected = isSelected
                    )
                }
            }
        }

        // Top Filter Bar & Quick Stats
        TopForensicFilterBar(
            displayGraph = displayGraph,
            currentFilter = filterMode,
            isPersian = isPersian,
            onFilterChange = { filterMode = it },
            activeStatus = activeToolStatus,
            onClearStatus = {
                activeToolStatus = null
                highlightedNodeIds = emptySet()
                highlightedEdgeIds = emptySet()
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp, start = 12.dp, end = 12.dp)
        )

        // Floating Interactive Toolbar (GraphSense & Maltego Analysis Tools)
        InteractiveGraphToolbar(
            isPersian = isPersian,
            hasSelection = selectedNode != null,
            isPathAnimationEnabled = isPathAnimationEnabled,
            onTogglePathAnimation = { isPathAnimationEnabled = !isPathAnimationEnabled },
            onZoomIn = { scale = (scale * 1.25f).coerceAtMost(4.0f) },
            onZoomOut = { scale = (scale / 1.25f).coerceAtLeast(0.25f) },
            onResetView = {
                scale = 1.0f
                offset = Offset.Zero
            },
            onTraceFlow = {
                selectedNode?.let { sel ->
                    val flows = ForensicCaseGraphEngine.traceFundFlow(displayGraph, sel.id, maxHops = 3)
                    if (flows.isNotEmpty()) {
                        val nodeSet = flows.flatten().toSet()
                        highlightedNodeIds = nodeSet
                        val edgeSet = mutableSetOf<String>()
                        flows.forEach { flow ->
                            for (i in 0 until (flow.size - 1)) {
                                displayGraph.edges.firstOrNull {
                                    it.sourceId == flow[i] && it.targetId == flow[i + 1]
                                }?.let { edgeSet.add(it.id) }
                            }
                        }
                        highlightedEdgeIds = edgeSet
                        activeToolStatus = if (isPersian) "ردیابی چندگامی: ${flows.size} مسیر" else "Multi-Hop Flow: ${flows.size} paths"
                    } else {
                        Toast.makeText(context, if (isPersian) "مسیر جریان خروجی یافت نشد" else "No outgoing flow found", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onShortestPathMode = {
                if (selectedNode != null) {
                    activeToolStatus = if (isPersian) "روی گره مقصد برای کوتاه‌ترین مسیر ضربه بزنید" else "Tap destination node for Shortest Path"
                } else {
                    Toast.makeText(context, if (isPersian) "ابتدا یک گره مبدا انتخاب کنید" else "Select source node first", Toast.LENGTH_SHORT).show()
                }
            },
            onDetectPeelChains = {
                val chains = ForensicCaseGraphEngine.detectPeelChains(displayGraph)
                if (chains.isNotEmpty()) {
                    highlightedNodeIds = chains.flatten().toSet()
                    activeToolStatus = if (isPersian) "زنجیره لایه‌برداری (Peel-Chain): ${chains.size} مورد" else "Peel Chains: ${chains.size} detected"
                } else {
                    Toast.makeText(context, if (isPersian) "الگوی لایه‌برداری یافت نشد" else "No peel chains detected", Toast.LENGTH_SHORT).show()
                }
            },
            onDetectCycles = {
                val cycles = ForensicCaseGraphEngine.detectCycles(displayGraph)
                if (cycles.isNotEmpty()) {
                    highlightedNodeIds = cycles.flatten().toSet()
                    activeToolStatus = if (isPersian) "گردش مالی دایره‌ای (Cycle): ${cycles.size} حلقه" else "Circular Loops: ${cycles.size} cycles"
                } else {
                    Toast.makeText(context, if (isPersian) "حلقه بازگشتی یافت نشد" else "No circular loops detected", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        )

        // Bottom Node Inspector Card (GraphSense / Maltego tap-to-inspect)
        AnimatedVisibility(
            visible = selectedNode != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .offset {
                    androidx.compose.ui.unit.IntOffset(
                        dragOffset.x.toInt(),
                        dragOffset.y.toInt()
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    }
                }
        ) {
            selectedNode?.let { node ->
                NodeInspectorCard(
                    node = node,
                    isPersian = isPersian,
                    onClose = { selectedNode = null },
                    onCenterNode = {
                        offset = Offset(-node.x * scale, -node.y * scale)
                    },
                    onRunDeepRecon = {
                        onRunDeepReconForNode?.invoke(node)
                    },
                    onAddToEvidence = {
                        onAddNodeToEvidence?.invoke(node)
                    },
                    onOpenProvenance = {
                        onOpenProvenance?.invoke(node)
                    }
                )
            }
        }
    }
}

/**
 * Top Filter Bar with dynamic counts & analytical filter chips.
 */
@Composable
private fun TopForensicFilterBar(
    displayGraph: InteractiveCaseGraph,
    currentFilter: EntityFilterMode,
    isPersian: Boolean,
    onFilterChange: (EntityFilterMode) -> Unit,
    activeStatus: String?,
    onClearStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xEB111827),
        tonalElevation = 6.dp,
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFF1E293B))),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Hub, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    Text(
                        text = if (isPersian) "گراف ارتباطات فارنزیک و تحلیل پیوند" else "Forensic Link-Analysis Graph",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Text(
                        text = "${displayGraph.nodes.size} ${if (isPersian) "گره" else "nodes"} | ${displayGraph.edges.size} ${if (isPersian) "یال" else "edges"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Filter Chips Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EntityFilterMode.values().forEach { mode ->
                    FilterChip(
                        selected = currentFilter == mode,
                        onClick = { onFilterChange(mode) },
                        label = {
                            Text(
                                text = if (isPersian) mode.displayNameFa else mode.displayNameEn,
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            }

            // Active Tool Alert Banner
            if (activeStatus != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F766E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearStatus, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Floating vertical toolbar providing zoom controls and graph algorithms.
 */
@Composable
private fun InteractiveGraphToolbar(
    isPersian: Boolean,
    hasSelection: Boolean,
    isPathAnimationEnabled: Boolean,
    onTogglePathAnimation: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetView: () -> Unit,
    onTraceFlow: () -> Unit,
    onShortestPathMode: () -> Unit,
    onDetectPeelChains: () -> Unit,
    onDetectCycles: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xEB111827),
        tonalElevation = 8.dp,
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFF1E293B))),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom In
            IconButton(onClick = onZoomIn, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color(0xFFE2E8F0))
            }
            // Zoom Out
            IconButton(onClick = onZoomOut, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color(0xFFE2E8F0))
            }
            // Center View
            IconButton(onClick = onResetView, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset View", tint = Color(0xFF38BDF8))
            }

            // Path Animation Toggle (Breadcrumbs.app flow particles)
            IconButton(onClick = onTogglePathAnimation, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (isPathAnimationEnabled) Icons.Default.PlayCircleFilled else Icons.Default.PauseCircleFilled,
                    contentDescription = "Toggle Flow Animation",
                    tint = if (isPathAnimationEnabled) Color(0xFF00E676) else Color(0xFF64748B)
                )
            }

            HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.width(24.dp))

            // Multi-Hop Flow Tracing
            IconButton(
                onClick = onTraceFlow,
                enabled = hasSelection,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Timeline,
                    contentDescription = "Trace Flow",
                    tint = if (hasSelection) Color(0xFF00E5FF) else Color(0xFF475569)
                )
            }

            // Shortest Path
            IconButton(
                onClick = onShortestPathMode,
                enabled = hasSelection,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.AltRoute,
                    contentDescription = "Shortest Path",
                    tint = if (hasSelection) Color(0xFFFFD54F) else Color(0xFF475569)
                )
            }

            // Peel-Chain Detection
            IconButton(onClick = onDetectPeelChains, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.CallSplit, contentDescription = "Detect Peel Chains", tint = Color(0xFFFB923C))
            }

            // Cycle Detection
            IconButton(onClick = onDetectCycles, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChangeCircle, contentDescription = "Detect Cycles", tint = Color(0xFFE879F9))
            }
        }
    }
}

/**
 * Detailed Node Inspector Card that pops up when tapping any entity node.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NodeInspectorCard(
    node: InteractiveCaseNode,
    isPersian: Boolean,
    onClose: () -> Unit,
    onCenterNode: () -> Unit,
    onRunDeepRecon: () -> Unit,
    onAddToEvidence: () -> Unit,
    onOpenProvenance: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xF50F172A),
        tonalElevation = 10.dp,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(
                when (node.riskSeverity) {
                    RiskSeverity.CRITICAL, RiskSeverity.HIGH -> Color(0xFFE11D48)
                    RiskSeverity.MEDIUM -> Color(0xFFD97706)
                    else -> Color(0xFF0284C7)
                }
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 620.dp)
            .heightIn(max = 280.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Bar: Category, Risk Badge, Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (node.entityCategory) {
                            ForensicEntityCategory.CRYPTO_WALLET -> Color(0xFF0284C7)
                            ForensicEntityCategory.EXCHANGE_HOT_WALLET -> Color(0xFFD97706)
                            ForensicEntityCategory.MIXER_OR_TUMBLER -> Color(0xFFDC2626)
                            ForensicEntityCategory.PHONE_NUMBER -> Color(0xFF059669)
                            ForensicEntityCategory.EMAIL_ADDRESS -> Color(0xFF7C3AED)
                            ForensicEntityCategory.IP_NETWORK_NODE -> Color(0xFF0D9488)
                            else -> Color(0xFF475569)
                        }
                    ) {
                        Text(
                            text = node.entityCategory.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (node.riskSeverity) {
                            RiskSeverity.CRITICAL, RiskSeverity.HIGH -> Color(0xFFDC2626)
                            RiskSeverity.MEDIUM -> Color(0xFFD97706)
                            else -> Color(0xFF15803D)
                        }
                    ) {
                        Text(
                            text = node.riskSeverity.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onClose, 
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Entity Identifier & Copy Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Entity ID", node.id))
                        Toast.makeText(context, if (isPersian) "کپی شد" else "Copied", Toast.LENGTH_SHORT).show()
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = node.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF334155),
                            modifier = Modifier.clickable { onOpenProvenance() }
                        ) {
                            Text("⚖️", modifier = Modifier.padding(4.dp), fontSize = 12.sp)
                        }
                    }
                    Text(
                        text = node.id,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
            }

            // Epistemic Demarcation Badge
            val epistemicType = when (node.epistemicStatus) {
                ForensicEpistemicStatus.UNDISPUTED_LEDGER_FACT -> ForensicEpistemicType.OBSERVED_FACT
                ForensicEpistemicStatus.EXTERNAL_SOURCE -> ForensicEpistemicType.EXTERNAL_SOURCE
                ForensicEpistemicStatus.ALGORITHMIC_CALCULATION -> ForensicEpistemicType.CALCULATED
                ForensicEpistemicStatus.DERIVED_OSINT_INFERENCE -> ForensicEpistemicType.INFERENCE
                ForensicEpistemicStatus.WORKING_HYPOTHESIS -> ForensicEpistemicType.HYPOTHESIS
                ForensicEpistemicStatus.INVESTIGATOR_ASSESSMENT -> ForensicEpistemicType.INVESTIGATOR_ASSESSMENT
                ForensicEpistemicStatus.UNKNOWN -> ForensicEpistemicType.UNKNOWN
            }
            ForensicEpistemicBadge(
                type = epistemicType,
                isPersian = isPersian,
                source = node.network.name,
                confidencePercent = node.confidencePercent,
                modifier = Modifier.fillMaxWidth()
            )

            // Metrics: Balance / Transactions / Degree
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (node.balanceDisplay.isNotBlank()) {
                    MetricMiniBox(
                        title = if (isPersian) "موجودی / حجم" else "Balance / Vol",
                        value = node.balanceDisplay,
                        modifier = Modifier.weight(1f)
                    )
                }
                MetricMiniBox(
                    title = if (isPersian) "تراکنش‌ها" else "Tx Count",
                    value = node.txCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    title = if (isPersian) "ضریب اطمینان" else "Confidence",
                    value = "${node.confidencePercent}%",
                    modifier = Modifier.weight(1f)
                )
            }

            // Tags
            if (node.tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    node.tags.forEach { tag ->
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF1E293B)) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Action Buttons
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isNarrow = maxWidth < 400.dp
                if (isNarrow) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = onRunDeepRecon,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isPersian) "هویت‌یابی عمیق" else "Deep Recon",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = onAddToEvidence,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isPersian) "ثبت در ادله" else "Add Evidence",
                                    fontSize = 11.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onCenterNode,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.FilterCenterFocus, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "مرکز کردن دید روی این گره" else "Center View on Node",
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onRunDeepRecon,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isPersian) "هویت‌یابی عمیق" else "Deep Recon",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onAddToEvidence,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isPersian) "ثبت در ادله" else "Add Evidence",
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = onCenterNode,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.FilterCenterFocus, contentDescription = "Center", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniBox(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF1E293B),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 9.sp)
            Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

/**
 * Draws subtle technical grid dots on the canvas.
 */
private fun DrawScope.drawForensicGrid(canvasW: Float, canvasH: Float, scale: Float, offset: Offset) {
    val gridSize = 40f * scale
    if (gridSize < 12f) return

    val startX = (offset.x % gridSize)
    val startY = (offset.y % gridSize)

    var curX = startX
    while (curX < canvasW) {
        var curY = startY
        while (curY < canvasH) {
            drawCircle(
                color = Color(0xFF1E293B).copy(alpha = 0.45f),
                radius = 1.0f,
                center = Offset(curX, curY)
            )
            curY += gridSize
        }
        curX += gridSize
    }
}

/**
 * Draws an arrowhead on an edge pointing towards target radius.
 */
private fun DrawScope.drawArrowHead(
    srcX: Float,
    srcY: Float,
    tgtX: Float,
    tgtY: Float,
    targetRadius: Float,
    color: Color,
    size: Float
) {
    val dx = tgtX - srcX
    val dy = tgtY - srcY
    val dist = sqrt(dx * dx + dy * dy)
    if (dist < targetRadius + size) return

    // Position tip at circumference of target node
    val unitX = dx / dist
    val unitY = dy / dist
    val tipX = tgtX - unitX * targetRadius
    val tipY = tgtY - unitY * targetRadius

    val angle = atan2(dy, dx)
    val arrowAngle = PI / 6 // 30 degrees

    val leftX = tipX - size * cos(angle - arrowAngle).toFloat()
    val leftY = tipY - size * sin(angle - arrowAngle).toFloat()
    val rightX = tipX - size * cos(angle + arrowAngle).toFloat()
    val rightY = tipY - size * sin(angle + arrowAngle).toFloat()

    val path = Path().apply {
        moveTo(tipX, tipY)
        lineTo(leftX, leftY)
        lineTo(rightX, rightY)
        close()
    }

    drawPath(path, color = color)
}

/**
 * Helper to draw a diamond shape for high-risk / mixer nodes.
 */
private fun DrawScope.drawDiamond(cx: Float, cy: Float, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy - radius * 1.2f)
        lineTo(cx + radius * 1.2f, cy)
        lineTo(cx, cy + radius * 1.2f)
        lineTo(cx - radius * 1.2f, cy)
        close()
    }
    drawPath(path, color = color)
}

/**
 * Draws crisp entity category glyphs inside the node center (GraphSense & Maltego transform style).
 */
private fun DrawScope.drawEntityGlyph(
    cx: Float,
    cy: Float,
    radius: Float,
    category: ForensicEntityCategory,
    scale: Float
) {
    if (radius < 8f) return
    val glyph = when (category) {
        ForensicEntityCategory.CRYPTO_WALLET, ForensicEntityCategory.BLOCKCHAIN_ADDRESS -> "₿"
        ForensicEntityCategory.EXCHANGE_HOT_WALLET, ForensicEntityCategory.EXCHANGE -> "🏦"
        ForensicEntityCategory.MIXER_OR_TUMBLER -> "⚡"
        ForensicEntityCategory.SMART_CONTRACT, ForensicEntityCategory.CONTRACT -> "⚙"
        ForensicEntityCategory.PHONE_NUMBER, ForensicEntityCategory.PUBLIC_PHONE -> "📞"
        ForensicEntityCategory.EMAIL_ADDRESS, ForensicEntityCategory.PUBLIC_EMAIL -> "@"
        ForensicEntityCategory.IP_NETWORK_NODE, ForensicEntityCategory.PUBLIC_IP -> "🌐"
        ForensicEntityCategory.SOCIAL_ACCOUNT, ForensicEntityCategory.PUBLIC_PROFILE -> "👤"
        ForensicEntityCategory.ALIAS_USERNAME, ForensicEntityCategory.PUBLIC_USERNAME -> "ID"
        ForensicEntityCategory.DOMAIN_NAME, ForensicEntityCategory.DOMAIN, ForensicEntityCategory.ENS -> "🔗"
        ForensicEntityCategory.MINING_POOL -> "⛏"
        ForensicEntityCategory.TRANSACTION -> "⇄"
        ForensicEntityCategory.BLOCK -> "⬡"
        ForensicEntityCategory.TOKEN -> "🪙"
        ForensicEntityCategory.CLUSTER -> "❖"
        ForensicEntityCategory.EVIDENCE -> "📜"
        ForensicEntityCategory.INVESTIGATION_CASE -> "📁"
        ForensicEntityCategory.PATTERN -> "🔍"
        ForensicEntityCategory.PUBLIC_ORGANIZATION -> "🏢"
        ForensicEntityCategory.PUBLIC_SERVICE -> "🛠"
        else -> "?"
    }

    val paint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = (radius * 0.95f).coerceIn(10f, 26f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    val bounds = Rect()
    paint.getTextBounds(glyph, 0, glyph.length, bounds)
    val textY = cy + bounds.height() / 2f - bounds.bottom

    drawContext.canvas.nativeCanvas.drawText(glyph, cx, textY, paint)
}

/**
 * Draws edge volume/transform label capsule with rounded dark background.
 */
private fun DrawScope.drawEdgeLabelCapsule(
    text: String,
    cx: Float,
    cy: Float,
    scale: Float,
    isHighlighted: Boolean
) {
    val cleanText = if (text.length > 20) text.take(19) + "…" else text
    val paint = Paint().apply {
        color = if (isHighlighted) android.graphics.Color.parseColor("#00E5FF") else android.graphics.Color.parseColor("#E2E8F0")
        textSize = (10f * scale).coerceIn(9f, 15f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    val textBounds = Rect()
    paint.getTextBounds(cleanText, 0, cleanText.length, textBounds)
    val textW = textBounds.width().toFloat()
    val textH = textBounds.height().toFloat()

    val padX = 8f * scale
    val padY = 4f * scale
    val bgW = textW + padX * 2
    val bgH = textH + padY * 2

    // Background pill
    drawRoundRect(
        color = Color(0xF00B132B),
        topLeft = Offset(cx - bgW / 2f, cy - bgH / 2f),
        size = Size(bgW, bgH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale, 6f * scale)
    )
    drawRoundRect(
        color = if (isHighlighted) Color(0xFF00E5FF) else Color(0xFF334155),
        topLeft = Offset(cx - bgW / 2f, cy - bgH / 2f),
        size = Size(bgW, bgH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale, 6f * scale),
        style = Stroke(width = 1f * scale)
    )

    val textY = cy + textH / 2f - textBounds.bottom
    drawContext.canvas.nativeCanvas.drawText(cleanText, cx, textY, paint)
}

/**
 * Progressive Zoom: Detailed Node Label Pill and Confidence under node.
 */
private fun DrawScope.drawNodeDetailPill(
    node: InteractiveCaseNode,
    cx: Float,
    cy: Float,
    scale: Float,
    isPersian: Boolean,
    isSelected: Boolean
) {
    val label = if (node.label.length > 16) "${node.label.take(15)}…" else node.label
    val subText = when {
        node.confidencePercent > 0 -> "${node.confidencePercent}%"
        node.balanceDisplay.isNotBlank() -> node.balanceDisplay
        else -> ""
    }
    val fullText = if (subText.isNotBlank()) "$label ($subText)" else label

    val paint = Paint().apply {
        color = if (isSelected) android.graphics.Color.parseColor("#00E5FF") else android.graphics.Color.WHITE
        textSize = (9f * scale).coerceIn(8.5f, 14f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
        isAntiAlias = true
    }

    val bounds = Rect()
    paint.getTextBounds(fullText, 0, fullText.length, bounds)
    val textW = bounds.width().toFloat()
    val textH = bounds.height().toFloat()

    val padX = 6f * scale
    val padY = 3f * scale
    val bgW = textW + padX * 2
    val bgH = textH + padY * 2

    drawRoundRect(
        color = Color(0xEE0F172A),
        topLeft = Offset(cx - bgW / 2f, cy),
        size = Size(bgW, bgH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * scale, 4f * scale)
    )
    drawRoundRect(
        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF334155),
        topLeft = Offset(cx - bgW / 2f, cy),
        size = Size(bgW, bgH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * scale, 4f * scale),
        style = Stroke(width = 0.8f * scale)
    )

    val textY = cy + padY + textH - bounds.bottom
    drawContext.canvas.nativeCanvas.drawText(fullText, cx, textY, paint)
}
