package com.aistudio.orbit.util

import androidx.core.text.BidiFormatter

object ForensicBidiUtils {
    private val formatter: BidiFormatter by lazy { BidiFormatter.getInstance() }

    /**
     * Formats a technical string (like a Bitcoin address, txid, hash, or IP) so that it remains
     * strictly Left-To-Right (LTR) even when embedded inside a Right-To-Left (RTL) Persian sentence,
     * preventing punctuation or characters from shifting.
     */
    fun formatLtrTechnicalString(text: String): String {
        if (text.isBlank()) return ""
        return formatter.unicodeWrap(text)
    }

    /**
     * Surrounds text with explicit LTR marks (Left-to-Right Mark \u200E) to prevent visual direction distortion.
     */
    fun wrapLtr(text: String): String {
        return "\u200E$text\u200E"
    }

    /**
     * Surrounds text with explicit RTL marks (Right-to-Left Mark \u200F).
     */
    fun wrapRtl(text: String): String {
        return "\u200F$text\u200F"
    }
}
