package com.aistudio.orbit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Composable
fun ResponsiveLayout(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 12.dp,
    maxContentWidth: Dp = 1200.dp,
    content: @Composable (WindowWidthSizeClass) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val widthClass = when {
            maxWidth < 600.dp -> WindowWidthSizeClass.COMPACT
            maxWidth < 840.dp -> WindowWidthSizeClass.MEDIUM
            else -> WindowWidthSizeClass.EXPANDED
        }

        val effectiveHorizontalPadding = when (widthClass) {
            WindowWidthSizeClass.COMPACT -> horizontalPadding
            WindowWidthSizeClass.MEDIUM -> 24.dp
            WindowWidthSizeClass.EXPANDED -> 32.dp
        }

        Box(
            modifier = Modifier
                .widthIn(max = maxContentWidth)
                .fillMaxSize()
                .padding(
                    horizontal = effectiveHorizontalPadding,
                    vertical = verticalPadding
                )
        ) {
            content(widthClass)
        }
    }
}
