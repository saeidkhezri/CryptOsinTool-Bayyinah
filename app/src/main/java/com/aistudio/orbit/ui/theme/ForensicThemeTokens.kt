package com.aistudio.orbit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Global Design Tokens for the Bayyinah (بیِّنة) Blockchain Forensic Investigation Platform.
 * Enforces strict consistency across spacing, radius, elevation, accessibility touch targets,
 * and forensic evidence color semantics.
 */
object ForensicSpacing {
    val none: Dp = 0.dp
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val base: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp
}

object ForensicShapes {
    val xs = RoundedCornerShape(4.dp)
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(20.dp)
    val pill = RoundedCornerShape(999.dp)
}

object ForensicElevation {
    val none: Dp = 0.dp
    val subtle: Dp = 1.dp
    val card: Dp = 2.dp
    val elevated: Dp = 4.dp
    val dialog: Dp = 8.dp
}

object ForensicTouchTarget {
    val minSize: Dp = 48.dp
    val iconButtonSize: Dp = 48.dp
}

/**
 * Forensic Evidence Category Colors:
 * Strict visual demarcation between Deterministic Facts, Algorithmic Calculations,
 * OSINT Attributions, and Behavioral Hypotheses / Risk Patterns.
 */
object ForensicEvidenceColors {
    // 1. Observed On-Chain Fact (100% Deterministic)
    val observedFact = Color(0xFF10B981) // Emerald Green
    val observedFactContainer = Color(0xFFD1FAE5)
    val onObservedFactContainer = Color(0xFF065F46)

    // 2. Algorithmic Calculation (Derived)
    val derivedCalculation = Color(0xFF0284C7) // Sky Blue
    val derivedCalculationContainer = Color(0xFFE0F2FE)
    val onDerivedCalculationContainer = Color(0xFF0369A1)

    // 3. Entity Attribution & OSINT Intelligence (Off-Chain Correlation)
    val osintAttribution = Color(0xFF8B5CF6) // Purple / Violet
    val osintAttributionContainer = Color(0xFFEDE9FE)
    val onOsintAttributionContainer = Color(0xFF5B21B6)

    // 4. Behavioral Pattern / Crime Typology (Heuristic)
    val behavioralPattern = Color(0xFFF59E0B) // Amber / Warm Orange
    val behavioralPatternContainer = Color(0xFFFEF3C7)
    val onBehavioralPatternContainer = Color(0xFF92400E)

    // 5. High-Risk / Suspicious Alert (Critical Forensics)
    val riskCritical = Color(0xFFEF4444) // Crisp Red
    val riskCriticalContainer = Color(0xFFFEE2E2)
    val onRiskCriticalContainer = Color(0xFF991B1B)
}
