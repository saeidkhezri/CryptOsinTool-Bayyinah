package com.aistudio.orbit.forensics.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.aistudio.orbit.model.ForensicEntityCategory
import com.aistudio.orbit.model.InteractiveCaseGraph
import com.aistudio.orbit.model.RiskSeverity
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Renders an interactive case graph to a high-resolution Bitmap
 * for direct embedding into PDF judicial reports and image exports.
 */
object ForensicGraphImageRenderer {

    fun renderGraphToBitmap(
        graph: InteractiveCaseGraph,
        width: Int = 1000,
        height: Int = 600,
        isDarkMode: Boolean = false
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = true }

        // Background
        val bgColor = if (isDarkMode) Color.parseColor("#0B111E") else Color.parseColor("#F8FAFC")
        canvas.drawColor(bgColor)

        // Subtle grid pattern
        paint.color = if (isDarkMode) Color.parseColor("#172033") else Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1f
        var gx = 0f
        while (gx < width) {
            canvas.drawLine(gx, 0f, gx, height.toFloat(), paint)
            gx += 50f
        }
        var gy = 0f
        while (gy < height) {
            canvas.drawLine(0f, gy, width.toFloat(), gy, paint)
            gy += 50f
        }

        if (graph.nodes.isEmpty()) {
            paint.color = Color.GRAY
            paint.textSize = 20f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("No Graph Nodes Available", width / 2f, height / 2f, paint)
            return bitmap
        }

        // Calculate bounding box of nodes
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE

        graph.nodes.forEach { n ->
            if (n.x < minX) minX = n.x
            if (n.x > maxX) maxX = n.x
            if (n.y < minY) minY = n.y
            if (n.y > maxY) maxY = n.y
        }

        val rangeX = (maxX - minX).coerceAtLeast(100f)
        val rangeY = (maxY - minY).coerceAtLeast(100f)

        val padding = 80f
        val usableWidth = width - (padding * 2)
        val usableHeight = height - (padding * 2)

        val scaleX = usableWidth / rangeX
        val scaleY = usableHeight / rangeY
        val scale = minOf(scaleX, scaleY).coerceIn(0.5f, 3.5f)

        val midWorldX = (minX + maxX) / 2f
        val midWorldY = (minY + maxY) / 2f

        fun toScreenX(wx: Float): Float = (width / 2f) + (wx - midWorldX) * scale
        fun toScreenY(wy: Float): Float = (height / 2f) + (wy - midWorldY) * scale

        // Map node IDs to screen coordinates
        val posMap = graph.nodes.associate { it.id to Pair(toScreenX(it.x), toScreenY(it.y)) }

        // 1. Draw Edges
        graph.edges.forEach { edge ->
            val src = posMap[edge.sourceId]
            val tgt = posMap[edge.targetId]
            if (src != null && tgt != null) {
                val (sx, sy) = src
                val (tx, ty) = tgt

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.0f
                paint.color = if (isDarkMode) Color.parseColor("#475569") else Color.parseColor("#94A3B8")
                canvas.drawLine(sx, sy, tx, ty, paint)

                // Draw arrow head
                val angle = atan2(ty - sy, tx - sx)
                val targetRadius = 24f
                val arrowTipX = tx - (targetRadius * cos(angle))
                val arrowTipY = ty - (targetRadius * sin(angle))

                val arrowSize = 10f
                val path = Path().apply {
                    moveTo(arrowTipX, arrowTipY)
                    lineTo(
                        arrowTipX - arrowSize * cos(angle - Math.PI / 6).toFloat(),
                        arrowTipY - arrowSize * sin(angle - Math.PI / 6).toFloat()
                    )
                    lineTo(
                        arrowTipX - arrowSize * cos(angle + Math.PI / 6).toFloat(),
                        arrowTipY - arrowSize * sin(angle + Math.PI / 6).toFloat()
                    )
                    close()
                }
                paint.style = Paint.Style.FILL
                canvas.drawPath(path, paint)

                // Draw edge volume text if available
                if (edge.volumeDisplay.isNotBlank()) {
                    val mx = (sx + tx) / 2f
                    val my = (sy + ty) / 2f
                    paint.color = if (isDarkMode) Color.parseColor("#0F172A") else Color.WHITE
                    canvas.drawCircle(mx, my, 12f, paint)
                    paint.style = Paint.Style.STROKE
                    paint.color = Color.parseColor("#CBD5E1")
                    canvas.drawCircle(mx, my, 12f, paint)

                    paint.style = Paint.Style.FILL
                    paint.color = if (isDarkMode) Color.WHITE else Color.BLACK
                    paint.textSize = 8f
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(edge.volumeDisplay.take(6), mx, my + 3f, paint)
                }
            }
        }

        // 2. Draw Nodes
        graph.nodes.forEach { node ->
            val pos = posMap[node.id] ?: return@forEach
            val (nx, ny) = pos
            val radius = (node.visualRadius * 0.9f).coerceIn(16f, 32f)

            // Halo for Target / High Risk
            if (node.isTarget || node.riskSeverity == RiskSeverity.CRITICAL) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                paint.color = if (node.isTarget) Color.parseColor("#00E676") else Color.parseColor("#FF1744")
                canvas.drawCircle(nx, ny, radius + 6f, paint)
            }

            // Fill color based on category
            val fillColor = when (node.entityCategory) {
                ForensicEntityCategory.CRYPTO_WALLET -> if (node.isTarget) Color.parseColor("#00C853") else Color.parseColor("#0284C7")
                ForensicEntityCategory.EXCHANGE_HOT_WALLET -> Color.parseColor("#F59E0B")
                ForensicEntityCategory.MIXER_OR_TUMBLER -> Color.parseColor("#DC2626")
                ForensicEntityCategory.SMART_CONTRACT -> Color.parseColor("#7C3AED")
                ForensicEntityCategory.PHONE_NUMBER, ForensicEntityCategory.PUBLIC_PHONE -> Color.parseColor("#06B6D4")
                ForensicEntityCategory.EMAIL_ADDRESS, ForensicEntityCategory.PUBLIC_EMAIL -> Color.parseColor("#8B5CF6")
                ForensicEntityCategory.IP_NETWORK_NODE, ForensicEntityCategory.PUBLIC_IP -> Color.parseColor("#10B981")
                ForensicEntityCategory.SOCIAL_ACCOUNT, ForensicEntityCategory.ALIAS_USERNAME, ForensicEntityCategory.PUBLIC_USERNAME -> Color.parseColor("#3B82F6")
                ForensicEntityCategory.DOMAIN_NAME, ForensicEntityCategory.DOMAIN, ForensicEntityCategory.ENS -> Color.parseColor("#EC4899")
                else -> Color.parseColor("#64748B")
            }

            paint.style = Paint.Style.FILL
            paint.color = fillColor

            when (node.entityCategory) {
                ForensicEntityCategory.MIXER_OR_TUMBLER -> {
                    val path = Path().apply {
                        moveTo(nx, ny - radius)
                        lineTo(nx + radius, ny)
                        lineTo(nx, ny + radius)
                        lineTo(nx - radius, ny)
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
                ForensicEntityCategory.PHONE_NUMBER, ForensicEntityCategory.PUBLIC_PHONE,
                ForensicEntityCategory.EMAIL_ADDRESS, ForensicEntityCategory.PUBLIC_EMAIL -> {
                    val half = radius * 0.85f
                    canvas.drawRoundRect(RectF(nx - half, ny - half, nx + half, ny + half), 6f, 6f, paint)
                }
                else -> {
                    canvas.drawCircle(nx, ny, radius, paint)
                }
            }

            // White border around node
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = Color.WHITE
            canvas.drawCircle(nx, ny, radius, paint)

            // Draw Node Category Symbol / Letter
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            val symbolChar = when (node.entityCategory) {
                ForensicEntityCategory.CRYPTO_WALLET -> if (node.isTarget) "★" else "₿"
                ForensicEntityCategory.EXCHANGE_HOT_WALLET -> "🏦"
                ForensicEntityCategory.MIXER_OR_TUMBLER -> "⚡"
                ForensicEntityCategory.PHONE_NUMBER, ForensicEntityCategory.PUBLIC_PHONE -> "☎"
                ForensicEntityCategory.EMAIL_ADDRESS, ForensicEntityCategory.PUBLIC_EMAIL -> "✉"
                ForensicEntityCategory.IP_NETWORK_NODE, ForensicEntityCategory.PUBLIC_IP -> "🌐"
                ForensicEntityCategory.SOCIAL_ACCOUNT, ForensicEntityCategory.ALIAS_USERNAME -> "👤"
                ForensicEntityCategory.DOMAIN_NAME, ForensicEntityCategory.DOMAIN, ForensicEntityCategory.ENS -> "🔗"
                else -> "●"
            }
            canvas.drawText(symbolChar, nx, ny + 4f, paint)

            // Draw Node Label below
            paint.color = if (isDarkMode) Color.WHITE else Color.parseColor("#0F172A")
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val displayLabel = if (node.label.length > 20) node.label.take(18) + "…" else node.label
            canvas.drawText(displayLabel, nx, ny + radius + 12f, paint)
        }

        // 3. Draw Watermark & Frame Header
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0F2042")
        canvas.drawRect(0f, 0f, width.toFloat(), 32f, paint)

        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("BAYYINAH GRAPH FORENSICS • TOPOLOGY LINK ANALYSIS DIAGRAM", 16f, 20f, paint)

        paint.color = Color.parseColor("#FFD54F")
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("${graph.nodes.size} Nodes | ${graph.edges.size} Directed Edges", width - 16f, 20f, paint)

        // Outer border
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#94A3B8")
        paint.strokeWidth = 2f
        canvas.drawRect(1f, 1f, width - 1f, height - 1f, paint)

        return bitmap
    }
}
