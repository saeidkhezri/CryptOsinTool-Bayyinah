package com.aistudio.orbit.ui.components.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

sealed class ForensicUiState<out T> {
    object Loading : ForensicUiState<Nothing>()
    data class Success<out T>(val data: T) : ForensicUiState<T>()
    object Empty : ForensicUiState<Nothing>()
    data class Error(val message: String) : ForensicUiState<Nothing>()
    object Offline : ForensicUiState<Nothing>()
    data class Partial<out T>(val data: T, val message: String) : ForensicUiState<T>()
}

@Composable
fun <T> ForensicUiStateBoundary(
    state: ForensicUiState<T>,
    onRetry: (() -> Unit)? = null,
    emptyMessage: String = "اطلاعاتی یافت نشد",
    offlineMessage: String = "شما در حالت آفلاین هستید. داده‌ها از حافظه محلی لود شده‌اند.",
    loadingContent: @Composable () -> Unit = { DefaultLoadingContent() },
    emptyContent: @Composable () -> Unit = { DefaultEmptyContent(emptyMessage) },
    errorContent: @Composable (String) -> Unit = { msg -> DefaultErrorContent(msg, onRetry) },
    offlineContent: @Composable () -> Unit = { DefaultOfflineContent(offlineMessage, onRetry) },
    partialContent: @Composable (T, String) -> Unit = { data, msg -> DefaultPartialContent(data, msg) },
    successContent: @Composable (T) -> Unit
) {
    when (state) {
        is ForensicUiState.Loading -> loadingContent()
        is ForensicUiState.Success -> successContent(state.data)
        is ForensicUiState.Empty -> emptyContent()
        is ForensicUiState.Error -> errorContent(state.message)
        is ForensicUiState.Offline -> offlineContent()
        is ForensicUiState.Partial -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                partialContent(state.data, state.message)
                successContent(state.data)
            }
        }
    }
}

@Composable
fun DefaultLoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun DefaultEmptyContent(message: String) {
    ForensicEmptyState(
        message = message,
        icon = Icons.Default.HourglassEmpty
    )
}

@Composable
fun DefaultErrorContent(message: String, onRetry: (() -> Unit)?) {
    ForensicEmptyState(
        message = message,
        icon = Icons.Default.ErrorOutline,
        actionButton = onRetry?.let {
            {
                androidx.compose.material3.Button(onClick = it) {
                    Text("تلاش مجدد")
                }
            }
        }
    )
}

@Composable
fun DefaultOfflineContent(message: String, onRetry: (() -> Unit)?) {
    ForensicEmptyState(
        message = message,
        icon = Icons.Default.CloudOff,
        actionButton = onRetry?.let {
            {
                androidx.compose.material3.Button(onClick = it) {
                    Text("بروزرسانی مجدد")
                }
            }
        }
    )
}

@Composable
fun DefaultPartialContent(data: Any?, message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
            .border(1.dp, MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.small)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
