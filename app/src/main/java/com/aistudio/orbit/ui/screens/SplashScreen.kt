package com.aistudio.orbit.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.R
import com.aistudio.orbit.localization.AppLocalization
import com.aistudio.orbit.repository.AppLanguage
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    language: AppLanguage,
    onSplashFinished: () -> Unit
) {
    val isFa = language == AppLanguage.PERSIAN
    val strings = AppLocalization.getStrings(language)
    var isVisible by remember { mutableStateOf(false) }

    // Auto-advance after 3.2 seconds
    LaunchedEffect(Unit) {
        isVisible = true
        delay(3200L)
        onSplashFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onSplashFinished()
            }
    ) {
        // Holy Shrine Background Image
        Image(
            painter = painterResource(id = R.drawable.splash_imam_reza),
            contentDescription = "Holy Shrine of Imam Reza",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient scrims for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.92f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Content
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(900)),
            exit = fadeOut(animationSpec = tween(400)),
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Salutation Tag
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFFFD54F).copy(alpha = 0.5f), Color(0xFFFFD54F), Color(0xFFFFD54F).copy(alpha = 0.5f))
                        )
                    ),
                    modifier = Modifier.padding(top = 20.dp)
                ) {
                    Text(
                        text = if (isFa) "✨ اَلسَّلامُ عَلَیْکَ یا عَلِیَّ بْنَ مُوسَی الرِّضا (ع) ✨" else "✨ In the Blessed Name of Ali ibn Musa al-Reza (AS) ✨",
                        color = Color(0xFFFFE082),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Bottom Branding & Loading
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Logo Asset Display
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(
                                width = 2.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFFFFD54F), Color(0xFF00E5FF), Color(0xFFFFD54F))
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Application Logo",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }

                    Text(
                        text = strings.appTitle,
                        color = Color(0xFFFFD54F),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            lineHeight = 40.sp,
                            letterSpacing = if (isFa) 1.5.sp else 3.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = strings.appSubtitle,
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Pulse progress line
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = alphaAnim)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF48CAE4), Color(0xFFFFD54F))
                                    )
                                )
                        )
                    }

                    Text(
                        text = if (isFa) "در حال بارگذاری سامانه‌های امنیتی..." else "Initializing forensic security modules...",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.alpha(alphaAnim)
                    )
                }
            }
        }
    }
}
