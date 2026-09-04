package com.aistudio.orbit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthSizeClass { COMPACT, MEDIUM, EXPANDED }

data class ResponsiveLayoutContract(
    val widthClass: WindowWidthSizeClass,
    val horizontalPadding: Dp,
    val maxContentWidth: Dp,
    val columns: Int,
    val useMasterDetail: Boolean
)

fun responsiveContract(width: Dp, maxContentWidth: Dp = 1200.dp): ResponsiveLayoutContract = when {
    width < 600.dp -> ResponsiveLayoutContract(WindowWidthSizeClass.COMPACT, 16.dp, maxContentWidth, 1, false)
    width < 840.dp -> ResponsiveLayoutContract(WindowWidthSizeClass.MEDIUM, 24.dp, maxContentWidth, 2, true)
    else -> ResponsiveLayoutContract(WindowWidthSizeClass.EXPANDED, 32.dp, maxContentWidth, 2, true)
}

@Composable
fun ResponsiveLayout(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 12.dp,
    maxContentWidth: Dp = 1200.dp,
    content: @Composable (WindowWidthSizeClass) -> Unit
) {
    BoxWithConstraints(modifier=modifier, contentAlignment=Alignment.TopCenter) {
        val contract=responsiveContract(maxWidth,maxContentWidth)
        Box(
            modifier=Modifier.fillMaxWidth().widthIn(max=contract.maxContentWidth).padding(horizontal=contract.horizontalPadding,vertical=verticalPadding),
            contentAlignment=Alignment.TopCenter
        ) { content(contract.widthClass) }
    }
}
