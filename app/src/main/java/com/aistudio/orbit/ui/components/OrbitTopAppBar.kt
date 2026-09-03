package com.aistudio.orbit.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.R
import com.aistudio.orbit.ui.theme.ForensicElevation
import com.aistudio.orbit.ui.theme.ForensicSpacing

// Luxury Velvet Single Color Palette for Title
val VelvetLuxuryGold = Color(0xFFE2A838)
val VelvetLuxuryAccent = Color(0xFFC99226)

@Composable
fun OrbitTopAppBar(
    title: String,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = ForensicElevation.card,
        shadowElevation = ForensicElevation.subtle,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = ForensicSpacing.sm, vertical = ForensicSpacing.xs)
                .heightIn(min = 52.dp),
            contentAlignment = Alignment.Center
        ) {
            // 1. Navigation / Logo slot on the start side
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (navigationIcon != null) {
                    Box(
                        modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        navigationIcon()
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.2f))
                            .border(
                                width = 1.dp,
                                color = VelvetLuxuryGold.copy(alpha = 0.6f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }

            // 2. Centered Velvet Luxury Title with Decorative Symbols
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val displayTitle = when (title) {
                    "بیِّنة", "بَیِّنَه", "بینه" -> "✦  بیِّنة  ✦"
                    "BAYYINAH" -> "✧  BAYYINAH  ✧"
                    else -> "✦  $title  ✦"
                }

                Text(
                    text = displayTitle,
                    color = VelvetLuxuryGold,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3. Actions slot (Search icon only) on the end side
            if (actions != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions()
                }
            }
        }
    }
}


