package com.aistudio.orbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.model.BlockchainNetwork

/**
 * 3D Pastel & Phosphor Glowing Icon Badge for general UI components.
 */
@Composable
fun Phosphor3dIconBadge(
    icon: ImageVector,
    themeColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = (size.value * 0.55f).dp
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            themeColor.copy(alpha = 0.95f),
            themeColor.copy(alpha = 0.65f)
        )
    )

    Surface(
        modifier = modifier
            .size(size)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp), ambientColor = themeColor, spotColor = themeColor),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Custom 3D Styled Blockchain Badge for networks (BTC, ETH, TRX, SOL, BSC, POL, USDT).
 */
@Composable
fun Blockchain3dBadge(
    network: BlockchainNetwork,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    val (colors, labelText, iconVector) = when (network) {
        BlockchainNetwork.BITCOIN -> Triple(
            listOf(Color(0xFFFFB300), Color(0xFFFF6F00)),
            "₿",
            Icons.Default.CurrencyBitcoin
        )
        BlockchainNetwork.ETHEREUM -> Triple(
            listOf(Color(0xFF7C4DFF), Color(0xFF304FFE)),
            "Ξ",
            Icons.Default.Diamond
        )
        BlockchainNetwork.TETHER_USDT -> Triple(
            listOf(Color(0xFF00E676), Color(0xFF00B0FF)),
            "₮",
            Icons.Default.AttachMoney
        )
        BlockchainNetwork.BNB_CHAIN -> Triple(
            listOf(Color(0xFFFFD600), Color(0xFFFF9100)),
            "BNB",
            Icons.Default.Layers
        )
        BlockchainNetwork.POLYGON -> Triple(
            listOf(Color(0xFFAA00FF), Color(0xFF651FFF)),
            "POL",
            Icons.Default.Hexagon
        )
        BlockchainNetwork.TRON -> Triple(
            listOf(Color(0xFFFF1744), Color(0xFFD50000)),
            "TRX",
            Icons.Default.FlashOn
        )
        BlockchainNetwork.SOLANA -> Triple(
            listOf(Color(0xFF00E5FF), Color(0xFFD500F9)),
            "SOL",
            Icons.Default.AutoAwesome
        )
    }

    val gradientBrush = Brush.linearGradient(colors = colors)

    Surface(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = colors.first(),
                spotColor = colors.last()
            ),
        shape = CircleShape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .border(width = 1.5.dp, color = Color.White.copy(alpha = 0.5f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = labelText,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.42f).sp,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
