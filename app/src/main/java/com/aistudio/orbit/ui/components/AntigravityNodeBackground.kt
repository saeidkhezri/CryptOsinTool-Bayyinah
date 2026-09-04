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
 * State container capturing touch and click events across the application to drive the
 * interactive Gemini-inspired cosmic background.
 */
class AntigravityInteractionState {
    var touchPosition by mutableStateOf<Offset?>(null)
    var lastClickPosition by mutableStateOf<Offset?>(null)
    var clickTrigger by mutableStateOf(0)
}

/**
 * A single stardust particle in the 3D cosmic stream.
 */
private class GeminiStardustParticle(
    var normX: Float,
    var normY: Float,
    val depthZ: Float, // 0.3f (distant, small, slow) to 1.0f (foreground, luminous)
    val orbitSpeed: Float,
    val orbitRadius: Float,
    val phaseOffset: Float,
    val baseRadius: Float,
    val baseColor: Color,
    val activeColor: Color,
    val streamIndex: Int
) {
    var currentX: Float = 0f
    var currentY: Float = 0f
    var colorShiftProgress: Float = 0f // 0f = baseColor, 1f = activeColor
    var impulseOffsetX: Float = 0f
    var impulseOffsetY: Float = 0f
}

/**
 * Expanding circular shockwave caused by a tap or click.
 */
private class ShockwaveWave(
    val centerX: Float,
    val centerY: Float,
    val maxRadius: Float,
    val duration: Float
) {
    var age: Float = 0f
    val currentRadius: Float
        get() = (age / duration).coerceIn(0f, 1f) * maxRadius
    val isExpired: Boolean
        get() = age >= duration
}

/**
 * Sparkle burst particles that radiate outward on tap/click.
 */
private class StardustSpark(
    val originX: Float,
    val originY: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val radius: Float,
    val maxAge: Float
) {
    var age: Float = 0f
}

/**
 * Luxury Animated Background inspired by Google DeepMind / Gemini "About" page (gemini.google/us/about).
 *
 * Visual Highlights:
 * 1. 3D swirling river of stardust particles with dual-pass glow halos.
 * 2. Real physical reaction: Dragging/touching pulls particles into a swirling vortex with dynamic color transformation.
 * 3. Tapping/clicking emits an expanding radial shockwave that displaces particles and flashes their colors into starlight.
 * 4. Calibrated opacity ensuring the background remains subordinate to foreground forensic cards and text.
 */
@Composable
fun AntigravityNodeBackground(
    isDark: Boolean,
    interactionState: AntigravityInteractionState,
    modifier: Modifier = Modifier,
    dimFactor: Float = 0.85f // Calibrated softness: dimmer than web as requested
) {
    var timeState by remember { mutableStateOf(0f) }
    val activeShockwaves = remember { mutableStateListOf<ShockwaveWave>() }
    val activeSparks = remember { mutableStateListOf<StardustSpark>() }

    // Game-loop frame ticker for fluid 60/120fps animation
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            withFrameNanos { frameTime ->
                val elapsedSec = (frameTime - lastTime) / 1_000_000_000f
                lastTime = frameTime
                timeState += elapsedSec

                // Update shockwaves
                if (activeShockwaves.isNotEmpty()) {
                    val it = activeShockwaves.iterator()
                    while (it.hasNext()) {
                        val sw = it.next()
                        sw.age += elapsedSec
                        if (sw.isExpired) it.remove()
                    }
                }

                // Update sparks
                if (activeSparks.isNotEmpty()) {
                    val it = activeSparks.iterator()
                    while (it.hasNext()) {
                        val spark = it.next()
                        spark.age += elapsedSec
                        if (spark.age >= spark.maxAge) it.remove()
                    }
                }
            }
        }
    }

    // Tap / Click Handler: triggers shockwave pulse and stardust sparks
    LaunchedEffect(interactionState.clickTrigger) {
        val clickPos = interactionState.lastClickPosition
        if (clickPos != null && interactionState.clickTrigger > 0) {
            // Add expanding shockwave
            activeShockwaves.add(
                ShockwaveWave(
                    centerX = clickPos.x,
                    centerY = clickPos.y,
                    maxRadius = 600f,
                    duration = 0.9f
                )
            )

            // Add stardust sparklers
            val random = Random(System.currentTimeMillis())
            val sparkColor = if (isDark) Color(0xFF00E5FF) else Color(0xFF2563EB)
            for (i in 0 until 18) {
                val angle = random.nextFloat() * 2f * PI.toFloat()
                val speed = 80f + random.nextFloat() * 260f
                activeSparks.add(
                    StardustSpark(
                        originX = clickPos.x,
                        originY = clickPos.y,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        color = sparkColor,
                        radius = 0.8f + random.nextFloat() * 2.2f,
                        maxAge = 0.45f + random.nextFloat() * 0.55f
                    )
                )
            }
        }
    }

    // Particles system definition (Gemini color spectrum)
    val particles = remember(isDark) {
        val random = Random(42)
        val list = ArrayList<GeminiStardustParticle>()
        val particleCount = 110

        for (i in 0 until particleCount) {
            val stream = i % 3
            val normX = random.nextFloat()
            val normY = when (stream) {
                0 -> 0.15f + random.nextFloat() * 0.22f // Top header/hero stream
                1 -> 0.42f + random.nextFloat() * 0.20f // Mid ambient stream
                else -> 0.70f + random.nextFloat() * 0.22f // Lower subtle stream
            }
            val depthZ = 0.35f + random.nextFloat() * 0.65f
            val orbitSpeed = (0.012f + random.nextFloat() * 0.024f) * (0.8f + depthZ * 0.4f)
            val orbitRadius = 14f + random.nextFloat() * 28f
            val baseRadius = (0.8f + random.nextFloat() * 2.0f) * depthZ

            // Gemini signature colors:
            // Dark: Electric Cyan, Cosmic Blue, Neon Violet, Starlight Purple
            // Light: Sapphire Blue, Ethereal Cerulean, Lavender Mist
            val baseColor = if (isDark) {
                when (stream) {
                    0 -> Color(0xFF00E5FF) // Electric Cyan
                    1 -> Color(0xFF3B82F6) // Celestial Blue
                    else -> Color(0xFFA855F7) // Cosmic Violet
                }
            } else {
                when (stream) {
                    0 -> Color(0xFF0284C7) // Sky Blue
                    1 -> Color(0xFF2563EB) // Royal Sapphire
                    else -> Color(0xFF7C3AED) // Muted Violet
                }
            }

            // Reactive Color when touched or shocked:
            // Dark: Radiant Amber-Gold, Solar Coral, Luminous Magenta
            // Light: Warm Sunset Orange, Coral Rose
            val activeColor = if (isDark) {
                when (stream) {
                    0 -> Color(0xFFF59E0B) // Amber Gold
                    1 -> Color(0xFFF97316) // Solar Orange
                    else -> Color(0xFFEC4899) // Hot Magenta
                }
            } else {
                when (stream) {
                    0 -> Color(0xFFEA580C) // Warm Coral
                    1 -> Color(0xFFE11D48) // Rose
                    else -> Color(0xFFD97706) // Golden Bronze
                }
            }

            list.add(
                GeminiStardustParticle(
                    normX = normX,
                    normY = normY,
                    depthZ = depthZ,
                    orbitSpeed = orbitSpeed,
                    orbitRadius = orbitRadius,
                    phaseOffset = random.nextFloat() * 2f * PI.toFloat(),
                    baseRadius = baseRadius,
                    baseColor = baseColor,
                    activeColor = activeColor,
                    streamIndex = stream
                )
            )
        }
        list
    }

    // Base canvas colors
    val baseCanvasColor = if (isDark) Color(0xFF080C14) else Color(0xFFF1F5F9)
    val canvasFadeBottom = if (isDark) Color(0xFF06090F) else Color(0xFFEEF2F6)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // 1. Base subtle canvas gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(baseCanvasColor, canvasFadeBottom)
            )
        )

        // 2. Ambient drifting cosmic glow nebulae (very soft and subtle)
        val nebula1Center = Offset(
            x = width * (0.35f + sin(timeState * 0.08f) * 0.18f),
            y = height * (0.22f + cos(timeState * 0.07f) * 0.12f)
        )
        val nebulaColor1 = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.08f * dimFactor) else Color(0xFFDBEAFE).copy(alpha = 0.35f * dimFactor)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(nebulaColor1, Color.Transparent),
                center = nebula1Center,
                radius = width * 0.55f
            ),
            radius = width * 0.55f,
            center = nebula1Center
        )

        val nebula2Center = Offset(
            x = width * (0.72f + cos(timeState * 0.09f) * 0.15f),
            y = height * (0.48f + sin(timeState * 0.06f) * 0.14f)
        )
        val nebulaColor2 = if (isDark) Color(0xFF581C87).copy(alpha = 0.06f * dimFactor) else Color(0xFFF3E8FF).copy(alpha = 0.30f * dimFactor)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(nebulaColor2, Color.Transparent),
                center = nebula2Center,
                radius = width * 0.50f
            ),
            radius = width * 0.50f,
            center = nebula2Center
        )

        // 3. Process each stardust particle
        val touch = interactionState.touchPosition
        val touchInfluence = 180f * density

        for (p in particles) {
            // Calculate orbital drift along fluid wave
            val driftX = (p.normX + timeState * p.orbitSpeed) % 1.0f
            var px = driftX * width

            val streamBaseY = when (p.streamIndex) {
                0 -> height * 0.20f
                1 -> height * 0.48f
                else -> height * 0.76f
            }
            val wavePhase = driftX * 2.2f * PI.toFloat() + timeState * 0.3f + p.phaseOffset
            var py = streamBaseY + sin(wavePhase) * (p.orbitRadius * density)

            // Touch interaction physics: whirlpool attraction and color shift
            var touchInfluenceRatio = 0f
            if (touch != null) {
                val dx = px - touch.x
                val dy = py - touch.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < touchInfluence && dist > 1f) {
                    touchInfluenceRatio = (1f - dist / touchInfluence)
                    // Vortex attraction force
                    val pull = touchInfluenceRatio * 0.32f
                    px -= dx * pull
                    py -= dy * pull
                    // Add subtle vortex swirl perpendicular to vector
                    px += -dy * (pull * 0.2f)
                    py += dx * (pull * 0.2f)
                }
            }

            // Shockwave interaction physics: radial push + instant color ignition
            for (sw in activeShockwaves) {
                val dx = px - sw.centerX
                val dy = py - sw.centerY
                val dist = sqrt(dx * dx + dy * dy)
                val waveDist = abs(dist - sw.currentRadius)
                val waveWidth = 50f * density
                if (waveDist < waveWidth && dist > 1f) {
                    val strength = (1f - waveDist / waveWidth) * (1f - sw.age / sw.duration)
                    // Push particle outward along shockwave normal
                    val nx = dx / dist
                    val ny = dy / dist
                    p.impulseOffsetX += nx * strength * 24f * density
                    p.impulseOffsetY += ny * strength * 24f * density
                    touchInfluenceRatio = max(touchInfluenceRatio, strength)
                }
            }

            // Apply and decay impulse offsets smoothly
            px += p.impulseOffsetX
            py += p.impulseOffsetY
            p.impulseOffsetX *= 0.88f
            p.impulseOffsetY *= 0.88f

            // Smooth color transition
            p.colorShiftProgress = if (touchInfluenceRatio > p.colorShiftProgress) {
                (p.colorShiftProgress + 0.18f).coerceAtMost(touchInfluenceRatio)
            } else {
                (p.colorShiftProgress - 0.04f).coerceAtLeast(0f)
            }

            p.currentX = px
            p.currentY = py

            // Interpolate color between cool base and radiant active
            val drawColor = if (p.colorShiftProgress > 0.01f) {
                Color.interpolate(p.baseColor, p.activeColor, p.colorShiftProgress)
            } else {
                p.baseColor
            }

            // Vertical position fade: upper header area is brightest, fading toward lower screen
            val verticalFade = (1.0f - (py / height) * 0.65f).coerceIn(0.25f, 1.0f)
            val baseAlpha = if (isDark) {
                (0.18f + p.depthZ * 0.22f + p.colorShiftProgress * 0.40f) * dimFactor * verticalFade
            } else {
                (0.14f + p.depthZ * 0.18f + p.colorShiftProgress * 0.35f) * dimFactor * verticalFade
            }

            val particleCenter = Offset(px, py)
            val particleRadius = p.baseRadius * density

            // Dual-Pass Rendering:
            // Pass 1: Luminous bloom halo
            drawCircle(
                color = drawColor.copy(alpha = baseAlpha * 0.35f),
                radius = particleRadius * 2.8f,
                center = particleCenter
            )

            // Pass 2: Crisp starlight core
            drawCircle(
                color = drawColor.copy(alpha = baseAlpha.coerceAtMost(1f)),
                radius = particleRadius,
                center = particleCenter
            )
        }

        // 4. Render active sparklers from clicks/taps
        for (spark in activeSparks) {
            val progress = spark.age / spark.maxAge
            val sx = spark.originX + spark.vx * spark.age
            val sy = spark.originY + spark.vy * spark.age
            val alpha = ((1f - progress) * 0.45f * dimFactor).coerceIn(0f, 1f)

            drawCircle(
                color = spark.color.copy(alpha = alpha),
                radius = spark.radius * density * (1f + progress * 0.6f),
                center = Offset(sx, sy)
            )
        }
    }
}

/**
 * Dedicated Header-Specific Animated Gemini Stardust Background.
 * Designed specifically for inclusion inside [OrbitTopAppBar] so that every single screen
 * header features the flowing, animated stardust stream.
 */
@Composable
fun AntigravityHeaderBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    var timeState by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            withFrameNanos { frameTime ->
                val elapsedSec = (frameTime - lastTime) / 1_000_000_000f
                lastTime = frameTime
                timeState += elapsedSec
            }
        }
    }

    val headerParticles = remember(isDark) {
        val random = Random(99)
        val list = ArrayList<GeminiStardustParticle>()
        for (i in 0 until 40) {
            val normX = random.nextFloat()
            val normY = 0.15f + random.nextFloat() * 0.70f
            val depthZ = 0.4f + random.nextFloat() * 0.6f
            val orbitSpeed = 0.025f + random.nextFloat() * 0.035f
            val baseRadius = 0.7f + random.nextFloat() * 1.5f
            val baseColor = if (isDark) {
                if (i % 2 == 0) Color(0xFF00E5FF) else Color(0xFF818CF8)
            } else {
                if (i % 2 == 0) Color(0xFF0284C7) else Color(0xFF6366F1)
            }
            list.add(
                GeminiStardustParticle(
                    normX = normX,
                    normY = normY,
                    depthZ = depthZ,
                    orbitSpeed = orbitSpeed,
                    orbitRadius = 8f + random.nextFloat() * 12f,
                    phaseOffset = random.nextFloat() * 2f * PI.toFloat(),
                    baseRadius = baseRadius,
                    baseColor = baseColor,
                    activeColor = Color(0xFFF59E0B),
                    streamIndex = 0
                )
            )
        }
        list
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        for (p in headerParticles) {
            val driftX = (p.normX + timeState * p.orbitSpeed) % 1.0f
            val px = driftX * width
            val py = p.normY * height + sin(driftX * 2.5f * PI.toFloat() + timeState * 0.4f + p.phaseOffset) * (p.orbitRadius * density)
            val center = Offset(px, py)
            val radius = p.baseRadius * density
            val alpha = if (isDark) 0.28f else 0.20f

            drawCircle(
                color = p.baseColor.copy(alpha = alpha * 0.4f),
                radius = radius * 2.2f,
                center = center
            )
            drawCircle(
                color = p.baseColor.copy(alpha = alpha),
                radius = radius,
                center = center
            )
        }
    }
}

private fun Color.Companion.interpolate(from: Color, to: Color, progress: Float): Color {
    val p = progress.coerceIn(0f, 1f)
    val r = from.red + (to.red - from.red) * p
    val g = from.green + (to.green - from.green) * p
    val b = from.blue + (to.blue - from.blue) * p
    val a = from.alpha + (to.alpha - from.alpha) * p
    return Color(r, g, b, a)
}
