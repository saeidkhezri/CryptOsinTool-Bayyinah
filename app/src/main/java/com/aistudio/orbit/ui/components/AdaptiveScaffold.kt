package com.aistudio.orbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Adaptive Scaffold supporting responsive layout breakpoints:
 * Compact (Phone portrait), Medium (Tablet / Foldable), and Expanded (Desktop / Wide screen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    contentWindowInsets: WindowInsets = WindowInsets(0, 0, 0, 0),
    content: @Composable (PaddingValues, WindowWidthSizeClass) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthClass = when {
            maxWidth < 600.dp -> WindowWidthSizeClass.COMPACT
            maxWidth < 840.dp -> WindowWidthSizeClass.MEDIUM
            else -> WindowWidthSizeClass.EXPANDED
        }

        Scaffold(
            topBar = topBar,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = contentWindowInsets
        ) { paddingValues ->
            content(paddingValues, widthClass)
        }
    }
}
