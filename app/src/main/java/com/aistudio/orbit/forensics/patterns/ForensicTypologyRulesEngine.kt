package com.aistudio.orbit.forensics.patterns

import com.aistudio.orbit.model.*

/**
 * Represents a YARA-like forensic typology rule.
 */
data class TypologyRule(
    val id: String,
    val name: String,
    val nameFa: String,
    val description: String,
    val descriptionFa: String,
    val category: String,
    val severity: RiskSeverity,
    val rawYaraRuleString: String,
    val evaluator: (InvestigationCase) -> RuleEvaluationResult
)

data class RuleEvaluationResult(
    val isMatched: Boolean,
    val confidence: ConfidenceLevel,
    val matchedTxIds: List<String>,
    val matchedAddresses: List<String>,
    val evidenceDetailsEn: String,
    val evidenceDetailsFa: String
)

data class EvaluatedTypologyRule(
    val rule: TypologyRule,
    val result: RuleEvaluationResult
)

/**
 * Forensic Typology & Heuristic Rule Engine (YARA-style).
 * Allows investigators to match on-chain crime typologies and custom rules.
 */
object ForensicTypologyRulesEngine {

    val builtInRules: List<TypologyRule> = listOf(
        TypologyRule(
            id = "RULE_PEELING_CHAIN_HOP",
            name = "Peeling Chain Sequential Dispersion",
            nameFa = "شناسایی زنجیره پیلینگ چین متوالی",
            description = "Detects successive outgoing transactions where residual change is systematically forwarded across multiple intermediary addresses.",
            descriptionFa = "شناسایی تراکنش‌های متوالی خروجی که در آن‌ها باقیمانده مانده‌حساب به آدرس‌های واسط پاس داده می‌شود.",
            category = "Layering / Laundering",
            severity = RiskSeverity.HIGH,
            rawYaraRuleString = """
                rule Peeling_Chain_Sequential_Dispersion {
                    meta:
                        author = "Bayyinah Forensic Core"
                        severity = "HIGH"
                        mitre_attack = "T1583.001"
                    strings:
                        ${'$'}hop_pattern = "1-in-2-out consecutive change sweep"
                        ${'$'}min_hops = 2
                    condition:
                        count(outgoing_txs) >= 2 and
                        has_sequential_change_flow()
                }
            """.trimIndent(),
            evaluator = { caseObj ->
                val outgoing = caseObj.transactions.filter { it.direction == TxDirection.OUTGOING || it.direction == TxDirection.MIXED }
                val isMatch = outgoing.size >= 2
                RuleEvaluationResult(
                    isMatched = isMatch,
                    confidence = if (outgoing.size >= 4) ConfidenceLevel.HIGH_CONFIDENCE else ConfidenceLevel.MEDIUM_CONFIDENCE,
                    matchedTxIds = outgoing.map { it.txId },
                    matchedAddresses = outgoing.flatMap { it.counterpartyAddresses }.distinct(),
                    evidenceDetailsEn = if (isMatch) "Matched ${outgoing.size} outgoing peeling hops forwarding residual funds." else "No sequential peeling flow detected.",
                    evidenceDetailsFa = if (isMatch) "تعداد ${outgoing.size} تراکنش متوالی منطبق بر الگوی لایه‌گذاری پیلینگ شناسایی شد." else "الگوی پیلینگ چین مشاهده نشد."
                )
            }
        ),

        TypologyRule(
            id = "RULE_DORMANT_WHALE_REACTIVATION",
            name = "Dormant High-Value Address Reactivation",
            nameFa = "فعال‌سازی مجدد آدرس راکد با موجودی بالا",
            description = "Triggers when an address dormant for over 90 days suddenly moves high-value digital assets.",
            descriptionFa = "آلارم جابجایی سرمایه قابل توجه پس از راکد ماندن طولانی بیش از ۹۰ روز.",
            category = "Velocity & Anomaly",
            severity = RiskSeverity.HIGH,
            rawYaraRuleString = """
                rule Dormant_High_Value_Reactivation {
                    meta:
                        author = "Bayyinah Forensic Core"
                        severity = "HIGH"
                    strings:
                        ${'$'}dormancy_threshold = "90_days"
                    condition:
                        max_dormant_period_days > 90 and
                        latest_tx_amount_sat > 50000000
                }
            """.trimIndent(),
            evaluator = { caseObj ->
                val sorted = caseObj.transactions.sortedBy { it.timestamp }
                var maxGapSec = 0L
                for (i in 1 until sorted.size) {
                    val gap = sorted[i].timestamp - sorted[i - 1].timestamp
                    if (gap > maxGapSec) maxGapSec = gap
                }
                val gapDays = maxGapSec / 86400.0
                val isMatch = gapDays >= 90.0 && sorted.isNotEmpty()
                RuleEvaluationResult(
                    isMatched = isMatch,
                    confidence = ConfidenceLevel.HIGH_CONFIDENCE,
                    matchedTxIds = sorted.takeLast(2).map { it.txId },
                    matchedAddresses = listOf(caseObj.targetAddress),
                    evidenceDetailsEn = if (isMatch) "Address was dormant for ${String.format("%.0f", gapDays)} days before sudden reactivation." else "No prolonged dormancy gap above 90 days.",
                    evidenceDetailsFa = if (isMatch) "آدرس به مدت ${String.format("%.0f", gapDays)} روز کاملاً راکد بوده و سپس فعال شده است." else "دوره رکود غیرعادی مشاهده نشد."
                )
            }
        ),

        TypologyRule(
            id = "RULE_ROUND_AMOUNT_STRUCTURING",
            name = "Round-Amount Smurfing / Structuring",
            nameFa = "ساختاردهی مبالغ رند (Smurfing)",
            description = "Detects multiple round-number transfers designed to avoid transaction reporting thresholds.",
            descriptionFa = "شناسایی تراکنش‌های با مبالغ رند شده جهت دور زدن آستانه‌های نظارتی صرافی‌ها.",
            category = "Structuring / Placement",
            severity = RiskSeverity.MEDIUM,
            rawYaraRuleString = """
                rule Round_Amount_Structuring {
                    meta:
                        author = "Bayyinah Forensic Core"
                        severity = "MEDIUM"
                    condition:
                        count(txs where amount is_round_number) >= 2
                }
            """.trimIndent(),
            evaluator = { caseObj ->
                val roundTxs = caseObj.transactions.filter { tx ->
                    val btc = tx.relevantAmountSat.toDouble() / 100_000_000.0
                    btc > 0.0 && (btc % 0.1 == 0.0 || btc % 0.5 == 0.0 || btc % 1.0 == 0.0)
                }
                val isMatch = roundTxs.size >= 2
                RuleEvaluationResult(
                    isMatched = isMatch,
                    confidence = ConfidenceLevel.MEDIUM_CONFIDENCE,
                    matchedTxIds = roundTxs.map { it.txId },
                    matchedAddresses = roundTxs.flatMap { it.counterpartyAddresses }.distinct(),
                    evidenceDetailsEn = if (isMatch) "Detected ${roundTxs.size} round-amount transactions matching smurfing indicators." else "No round structuring pattern found.",
                    evidenceDetailsFa = if (isMatch) "تعداد ${roundTxs.size} تراکنش با مبالغ کاملاً رند منطبق بر شاخص‌های پولشویی خرد شناسایی شد." else "تراکنش‌های رند متعدد یافت نشد."
                )
            }
        ),

        TypologyRule(
            id = "RULE_HIGH_VELOCITY_FAN_OUT",
            name = "High Velocity Rapid Fan-Out Dispersion",
            nameFa = "پراکندگی سریع وجه به چندین مقصد (Fan-Out)",
            description = "Detects rapid disbursement of incoming funds to 3 or more distinct destination addresses within short intervals.",
            descriptionFa = "پراکندگی آنی وجوه ورودی به ۳ آدرس یا بیشتر در بازه زمانی کوتاه جهت مخدوش کردن مسیر.",
            category = "Layering / Dispersion",
            severity = RiskSeverity.HIGH,
            rawYaraRuleString = """
                rule Rapid_Fan_Out_Dispersion {
                    meta:
                        author = "Bayyinah Forensic Core"
                        severity = "HIGH"
                    condition:
                        unique_counterparties >= 3 and
                        average_inter_tx_delay < 3600
                }
            """.trimIndent(),
            evaluator = { caseObj ->
                val cpCount = caseObj.counterparties.size
                val isMatch = cpCount >= 3 && caseObj.transactions.size >= 3
                RuleEvaluationResult(
                    isMatched = isMatch,
                    confidence = ConfidenceLevel.HIGH_CONFIDENCE,
                    matchedTxIds = caseObj.transactions.map { it.txId },
                    matchedAddresses = caseObj.counterparties.map { it.address },
                    evidenceDetailsEn = if (isMatch) "Rapid fan-out dispersion across $cpCount distinct counterparty clusters." else "No multi-branch fan-out dispersion.",
                    evidenceDetailsFa = if (isMatch) "پراکندگی سریع سرمایه در میان $cpCount آدرس طرف‌حساب مجزا شناسایی شد." else "پراکندگی مشکوک چندشاخه مشاهده نشد."
                )
            }
        ),

        TypologyRule(
            id = "RULE_MIXER_COINJOIN_USAGE",
            name = "CoinJoin / Privacy Mixer Footprint",
            nameFa = "ردپای میکسرها و تراکنش‌های CoinJoin",
            description = "Identifies transactions with characteristics typical of privacy protocols (Wasabi, Whirlpool, Tornado Cash).",
            descriptionFa = "شناسایی امضای تراکنش‌های اختلاط وجه و پروتکل‌های افزایش حریم خصوصی.",
            category = "Obfuscation",
            severity = RiskSeverity.CRITICAL,
            rawYaraRuleString = """
                rule Privacy_Mixer_CoinJoin_Signature {
                    meta:
                        author = "Bayyinah Forensic Core"
                        severity = "CRITICAL"
                    condition:
                        has_mixed_tx_direction or
                        has_equal_output_distribution()
                }
            """.trimIndent(),
            evaluator = { caseObj ->
                val mixerTxs = caseObj.transactions.filter { it.direction == TxDirection.MIXED }
                val isMatch = mixerTxs.isNotEmpty() || caseObj.riskIndicators.any { it.title.contains("Mixer", ignoreCase = true) || it.title.contains("CoinJoin", ignoreCase = true) }
                RuleEvaluationResult(
                    isMatched = isMatch,
                    confidence = ConfidenceLevel.HIGH_CONFIDENCE,
                    matchedTxIds = mixerTxs.map { it.txId },
                    matchedAddresses = mixerTxs.flatMap { it.counterpartyAddresses }.distinct(),
                    evidenceDetailsEn = if (isMatch) "Observed high-entropy mixing or CoinJoin obfuscation signature." else "No mixer/CoinJoin signatures found.",
                    evidenceDetailsFa = if (isMatch) "امضای استفاده از سرویس‌های میکس یا تراکنش‌های با آنتروپی بالای CoinJoin ثبت شد." else "ردپای میکسر مشاهده نشد."
                )
            }
        )
    )

    fun evaluateCase(investigationCase: InvestigationCase): List<EvaluatedTypologyRule> {
        return builtInRules.map { rule ->
            val result = rule.evaluator(investigationCase)
            EvaluatedTypologyRule(rule, result)
        }
    }
}
