package com.aistudio.orbit.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.*
import kotlin.random.Random

/**
 * State class to capture global screen interactions and feed them into the background.
 */
class AntigravityInteractionState {
    var touchPosition by mutableStateOf<Offset?>(null)
    var lastClickPosition by mutableStateOf<Offset?>(null)
    var clickTrigger by mutableStateOf(0)
}

private class CosmicParticle(
    var baseX: Float, // 0..1 normalized
    var baseY: Float, // 0..1 normalized
    val speed: Float,
    val baseRadius: Float,
    val baseColor: Color,
    val reactiveColor: Color,
    val ribbonIndex: Int,
    val phaseOffset: Float,
    val orbitAmplitude: Float
) {
    var colorIntensity = 0f
}

private class ClickRippleParticle(
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val radius: Float,
    val maxAge: Float
) {
    var age = 0f
}

/**
 * Highly polished, professional, and luxurious animated cosmic background.
 * Inspired by Google DeepMind / Gemini "about" aesthetics.
 * Features flowing ribbons of star dust, interactive mouse/touch reaction, and click ripples.
 */
@Composable
fun AntigravityNodeBackground(
    isDark: Boolean,
    interactionState: AntigravityInteractionState,
    modifier: Modifier = Modifier
) {
    // 1. Time state updated via high-performance game-loop tick
    var timeState by remember { mutableStateOf(0f) }
    val activeClickParticles = remember { mutableStateListOf<ClickRippleParticle>() }

    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            withFrameNanos { frameTime ->
                val elapsedSec = (frameTime - lastTime) / 1_000_000_000f
                lastTime = frameTime
                timeState += elapsedSec

                // Update click burst particles
                if (activeClickParticles.isNotEmpty()) {
                    val iterator = activeClickParticles.iterator()
                    while (iterator.hasNext()) {
                        val p = iterator.next()
                        p.age += elapsedSec
                        if (p.age >= p.maxAge) {
                            iterator.remove()
                        }
                    }
                }
            }
        }
    }

    // 2. Click ripple spawn action
    LaunchedEffect(interactionState.clickTrigger) {
        val clickPos = interactionState.lastClickPosition
        if (clickPos != null) {
            val random = Random(System.currentTimeMillis())
            // Cyan/Blue spark in dark, Lavender/Indigo spark in light
            val sparkColor = if (isDark) Color(0xFF06B6D4) else Color(0xFF6366F1)
            for (i in 0 until 24) {
                val angle = random.nextFloat() * 2f * PI.toFloat()
                val speed = 60f + random.nextFloat() * 240f
                val vx = cos(angle) * speed
                val vy = sin(angle) * speed
                val radius = 0.8f + random.nextFloat() * 2.2f
                val maxAge = 0.5f + random.nextFloat() * 0.7f
                activeClickParticles.add(
                    ClickRippleParticle(
                        startX = clickPos.x,
                        startY = clickPos.y,
                        vx = vx,
                        vy = vy,
                        color = sparkColor,
                        radius = radius,
                        maxAge = maxAge
                    )
                )
            }
        }
    }

    // 3. Initialize background particles distributed across flowing rivers
    val baseParticles = remember(isDark) {
        val random = Random(2026)
        val list = ArrayList<CosmicParticle>()
        val count = 90
        
        for (i in 0 until count) {
            val ribbon = i % 3
            val baseX = random.nextFloat()
            val baseY = when (ribbon) {
                0 -> 0.20f + random.nextFloat() * 0.15f
                1 -> 0.50f + random.nextFloat() * 0.15f
                else -> 0.80f + random.nextFloat() * 0.12f
            }
            val speed = 0.015f + random.nextFloat() * 0.025f
            val radius = 0.6f + random.nextFloat() * 1.8f
            
            // Refined luxury palette: Cobalt Blue, Mystical Indigo, Soft Orchid/Rose
            val baseColor = when (ribbon) {
                0 -> if (isDark) Color(0xFF0EA5E9) else Color(0xFF0284C7)
                1 -> if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                else -> if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777)
            }
            // Transition color when touched: Warm Golden Amber
            val reactiveColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

            list.add(
                CosmicParticle(
                    baseX = baseX,
                    baseY = baseY,
                    speed = speed,
                    baseRadius = radius,
                    baseColor = baseColor,
                    reactiveColor = reactiveColor,
                    ribbonIndex = ribbon,
                    phaseOffset = random.nextFloat() * 2f * PI.toFloat(),
                    orbitAmplitude = 12f + random.nextFloat() * 28f
                )
            )
        }
        list
    }

    // 4. Color theme gradients
    val bgGradientColors = if (isDark) {
        listOf(
            Color(0xFF0B0F19), // Deep Obsidian Charcoal
            Color(0xFF070A10), // Midnight Jet Black
            Color(0xFF0A0E17)  // Deep Indigo Space
        )
    } else {
        listOf(
            Color(0xFFF4F6FA), // Cream Slate
            Color(0xFFECF0F6), // Pure Light Alabaster
            Color(0xFFF0F3F7)  // Warm Neutral Ice
        )
    }

    // Interactive soft glow spots (Nebulas) that slowly drift and hover
    val glowSpot1Center = Offset(
        x = (0.3f + sin(timeState * 0.1f) * 0.2f),
        y = (0.25f + cos(timeState * 0.08f) * 0.15f)
    )
    val glowSpot2Center = Offset(
        x = (0.7f + cos(timeState * 0.12f) * 0.18f),
        y = (0.70f + sin(timeState * 0.09f) * 0.12f)
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // Draw radial background gradient
        drawRect(
            brush = Brush.radialGradient(
                colors = bgGradientColors,
                center = Offset(width * 0.5f, height * 0.45f),
                radius = max(width, height) * 0.9f
            )
        )

        // Draw ambient glowing nebulas behind the text
        val glow1Color = if (isDark) Color(0xFF0284C7).copy(alpha = 0.05f) else Color(0xFFE0F2FE).copy(alpha = 0.40f)
        val glow2Color = if (isDark) Color(0xFF6366F1).copy(alpha = 0.04f) else Color(0xFFEEF2F6).copy(alpha = 0.35f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glow1Color, Color.Transparent),
                center = Offset(glowSpot1Center.x * width, glowSpot1Center.y * height),
                radius = width * 0.45f
            ),
            radius = width * 0.45f,
            center = Offset(glowSpot1Center.x * width, glowSpot1Center.y * height)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glow2Color, Color.Transparent),
                center = Offset(glowSpot2Center.x * width, glowSpot2Center.y * height),
                radius = width * 0.45f
            ),
            radius = width * 0.45f,
            center = Offset(glowSpot2Center.x * width, glowSpot2Center.y * height)
        )

        // Pre-allocate containers for continuous wave filaments
        val ribbon0 = ArrayList<Offset>()
        val ribbon1 = ArrayList<Offset>()
        val ribbon2 = ArrayList<Offset>()

        // Update positions & collect ribbon points
        for (particle in baseParticles) {
            val driftX = (particle.baseX + timeState * particle.speed) % 1.0f
            var px = driftX * width
            
            val ribbonBaseY = when (particle.ribbonIndex) {
                0 -> height * 0.22f
                1 -> height * 0.52f
                else -> height * 0.82f
            }

            val wavePhase = driftX * 2f * PI.toFloat() * 1.3f + timeState * 0.25f + particle.phaseOffset
            var py = ribbonBaseY + sin(wavePhase) * (particle.orbitAmplitude * density)

            // Touch interaction physics
            val touch = interactionState.touchPosition
            if (touch != null) {
                val dx = px - touch.x
                val dy = py - touch.y
                val dist = sqrt(dx * dx + dy * dy)
                val maxDist = 180f * density
                if (dist < maxDist) {
                    val force = (1f - dist / maxDist)
                    // Pull particles gently towards touch coordinate
                    px -= dx * force * 0.28f
                    py -= dy * force * 0.28f
                    particle.colorIntensity = (particle.colorIntensity + 0.12f).coerceAtMost(1f)
                } else {
                    particle.colorIntensity = (particle.colorIntensity - 0.04f).coerceAtLeast(0f)
                }
            } else {
                particle.colorIntensity = (particle.colorIntensity - 0.03f).coerceAtLeast(0f)
            }

            val finalPos = Offset(px, py)
            when (particle.ribbonIndex) {
                0 -> ribbon0.add(finalPos)
                1 -> ribbon1.add(finalPos)
                else -> ribbon2.add(finalPos)
            }

            // Interpolate color based on touch reaction state
            val finalColor = if (particle.colorIntensity > 0f) {
                Color.interpolate(particle.baseColor, particle.reactiveColor, particle.colorIntensity)
            } else {
                particle.baseColor
            }

            // Draw very soft, tiny particles to maintain complete readability
            val particleAlpha = if (isDark) 0.18f + (particle.colorIntensity * 0.4f) else 0.12f + (particle.colorIntensity * 0.3f)
            drawCircle(
                color = finalColor.copy(alpha = particleAlpha),
                radius = particle.baseRadius * density,
                center = finalPos
            )
        }

        // Draw elegant flowing wave lines connecting the particles in each ribbon (Gemini River)
        val filamentColor = if (isDark) Color(0xFF6366F1).copy(alpha = 0.05f) else Color(0xFF818CF8).copy(alpha = 0.03f)
        val maxSegmentLength = width * 0.28f // Do not connect points wrapping around boundaries

        fun drawRibbonFilaments(ribbon: List<Offset>) {
            val sorted = ribbon.sortedBy { it.x }
            for (idx in 0 until sorted.size - 1) {
                val p1 = sorted[idx]
                val p2 = sorted[idx + 1]
                if (abs(p1.x - p2.x) < maxSegmentLength) {
                    drawLine(
                        color = filamentColor,
                        start = p1,
                        end = p2,
                        strokeWidth = 1f * density
                    )
                }
            }
        }

        drawRibbonFilaments(ribbon0)
        drawRibbonFilaments(ribbon1)
        drawRibbonFilaments(ribbon2)

        // Draw active Click Burst Particles (Stardust sparkler effect)
        for (p in activeClickParticles) {
            val px = p.startX + p.vx * p.age
            val py = p.startY + p.vy * p.age
            val progress = p.age / p.maxAge
            val alpha = (1f - progress).coerceIn(0f, 1f)

            drawCircle(
                color = p.color.copy(alpha = alpha * 0.32f),
                radius = p.radius * density * (1f + progress * 0.5f),
                center = Offset(px, py)
            )
        }
    }
}

private fun Color.Companion.interpolate(from: Color, to: Color, progress: Float): Color {
    val r = from.red + (to.red - from.red) * progress
    val g = from.green + (to.green - from.green) * progress
    val b = from.blue + (to.blue - from.blue) * progress
    val a = from.alpha + (to.alpha - from.alpha) * progress
    return Color(r, g, b, a)
}
