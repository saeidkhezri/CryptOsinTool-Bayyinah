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
    val observedFact = Color(0xFF3D7A57)
    val observedFactContainer = Color(0xFFDCEBE1)
    val onObservedFactContainer = Color(0xFF28563C)

    // 2. Algorithmic Calculation (Derived)
    val derivedCalculation = Color(0xFF3D6F86) // Sky Blue
    val derivedCalculationContainer = Color(0xFFDCEAF0)
    val onDerivedCalculationContainer = Color(0xFF2D5365)

    // 3. Entity Attribution & OSINT Intelligence (Off-Chain Correlation)
    val osintAttribution = Color(0xFF6F6387) // Purple / Violet
    val osintAttributionContainer = Color(0xFFE9E5EF)
    val onOsintAttributionContainer = Color(0xFF4F465F)

    // 4. Behavioral Pattern / Crime Typology (Heuristic)
    val behavioralPattern = Color(0xFFB66A1B) // Amber / Warm Orange
    val behavioralPatternContainer = Color(0xFFF4E8D1)
    val onBehavioralPatternContainer = Color(0xFF6E4614)

    // 5. High-Risk / Suspicious Alert (Critical Forensics)
    val riskCritical = Color(0xFFC53B32) // Crisp Red
    val riskCriticalContainer = Color(0xFFF4DEDC)
    val onRiskCriticalContainer = Color(0xFF7B2621)
}
