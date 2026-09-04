@file:OptIn(ExperimentalLayoutApi::class)
package com.aistudio.orbit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.forensics.TemporalUtils
import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.model.RiskSeverity
import com.aistudio.orbit.ui.components.designsystem.*
import com.aistudio.orbit.ui.theme.*

import androidx.compose.animation.AnimatedVisibility
import com.aistudio.orbit.model.ExperienceMode
import com.aistudio.orbit.model.InvestigationState
import com.aistudio.orbit.model.InvestigationStateMachine
import com.aistudio.orbit.forensics.learning.CryptoMiniLessonDialog
import com.aistudio.orbit.forensics.learning.CryptoMiniLessonRegistry
import com.aistudio.orbit.forensics.learning.LearnThisBadge
import com.aistudio.orbit.forensics.learning.MiniLessonTopic
import com.aistudio.orbit.repository.AppLanguage
import com.aistudio.orbit.localization.AppLocalization

enum class StageStatus(val displayNameFa: String, val displayNameEn: String, val color: Color) {
    COMPLETED("تکمیل شده", "Completed", Color(0xFF10B981)), // Emerald Green
    CURRENT("در حال انجام", "Current", Color(0xFF00E5FF)), // Electric Cyan
    AVAILABLE("قابل بررسی", "Available", Color(0xFF38BDF8)), // Sky Blue
    LOCKED("قفل شده", "Locked", Color(0xFF64748B)), // Slate Gray
    SKIPPED("رد شده", "Skipped", Color(0xFFF59E0B)), // Amber
    WARNING("دارای هشدار", "Warning", Color(0xFFEF4444)), // Red
    FAILED("ناموفق", "Failed", Color(0xFFDC2626)), // Crimson
    PARTIAL("نیمه‌کاره", "Partial", Color(0xFF8B5CF6)) // Purple
}

enum class InvestigationStage(
    val id: Int,
    val titleFa: String,
    val titleEn: String,
    val objectiveFa: String,
    val objectiveEn: String
) {
    START_CASE(1, "شروع تحقیق", "Start Investigation", "ایجاد پرونده، ثبت کلاسه و تعیین محدوده ردیابی", "Initiating the forensic case, assigning reference number, and setting scope boundaries"),
    INITIAL_LEAD(2, "سرنخ اولیه", "Initial Lead", "اعتبارسنجی الگوهای نوشتاری آدرس ورودی و ساختار شبکه صادرکننده", "Validating starting address string schemas, checking checksums, and auto-detecting base ledger"),
    BLOCKCHAIN_DISCOVERY(3, "بررسی بلاکچین", "Blockchain Discovery", "استعلام اطلاعات پایه‌ای بلاکچین، بررسی وضعیت خروجی‌های خرج‌نشده (UTXO) و تراز مالی زنده", "Querying ledger state, inspecting unspent output (UTXO) counts, and checking real-time on-chain balance"),
    TRANSACTIONS_LEDGER(4, "بررسی تراکنش‌ها", "Transactions Ledger", "تحلیل تفصیلی جریان دفترکل تراکنش‌های ورودی و خروجی یکسان‌سازی‌شده", "Detailed investigation of normalized incoming and outgoing ledger transactions and transaction values"),
    RELATED_ADDRESSES(5, "بررسی ارتباط‌ها", "Related Addresses", "کشف روابط تراکنشی مستقیم، خوشه‌بندی کیف‌پول‌ها و تبارشناسی انتقال‌ها در عمق گره‌ها", "Mapping transaction flows, wallet clustering heuristic calculations, and determining hop-depth counterparties"),
    PATTERN_ANALYSIS(6, "بررسی الگوها", "Pattern Analysis", "تطبیق رفتار جریان با کتابخانه الگوهای شناخته‌شده جرایم مالی و پیلینگ‌چین", "Matching flow behaviors against known forensic crime typologies, peeling chains, and money-laundering models"),
    OSINT_REVIEW(7, "بررسی OSINT", "OSINT Review", "ردیابی هویت خارج‌زنجیره‌ای، سوابق وب‌تاریک، حساب‌های اجتماعی و منبع‌باز", "Investigating off-chain identities, darknet leaks, forums, and open-source blockchain intelligence databases"),
    RISK_REVIEW(8, "بررسی ریسک", "Risk Review", "ارزیابی شاخص‌های نظارتی، کیف‌پول‌های تحت تحریم و سناریوهای پویای فرضیاتی پرونده", "Assessing compliance alerts, sanctioned address interactions, and structuring active investigative hypotheses"),
    EVIDENCE_REVIEW(9, "مرور شواهد", "Evidence Review", "ممیزی، سازمان‌دهی و تایید اصالت زنجیره ادله دیجیتال و بررسی کلاسه پرونده", "Auditing and finalizing the digital chain of custody, sealed findings, and overall forensic records"),
    CONCLUSION(10, "نتیجه‌گیری", "Conclusion", "فرضیه‌سازی نهایی کارشناس، تایید هویت و پاسخ به مراجع قضایی بر اساس ارزیابی فنی", "Formulating final expert assessments, matching identities, and responding to judicial mandates"),
    REPORT(11, "گزارش پرونده", "Report", "تولید، صدور و مهر و موم امن گزارش پرونده فارنزیک با فرمت‌های استاندارد", "Generating and secure-sealing finalized forensic PDF / CSV case reports for legal and judicial proceedings")
}

@Composable
fun CollapsibleRoadmapHeader(
    activeMode: ExperienceMode,
    onModeChange: (ExperienceMode) -> Unit,
    currentStage: InvestigationStage,
    stageStatuses: Map<InvestigationStage, StageStatus>,
    onStageSelect: (InvestigationStage) -> Unit,
    isPersian: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val stages = InvestigationStage.values()
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        shape = ForensicShapes.md,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(shape = ForensicShapes.sm, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        if (isPersian) activeMode.displayNameFa else activeMode.displayNameEn,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPersian) "نقشه مسیر تحقیق" else "Investigation Roadmap",
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${currentStage.id} / ${stages.size} • ${if (isPersian) currentStage.titleFa else currentStage.titleEn}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }
            if (expanded) {
                HorizontalDivider()
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxItemsInEachRow = 4
                ) {
                    stages.forEach { stage ->
                        val status = stageStatuses[stage] ?: StageStatus.AVAILABLE
                        val selected = stage == currentStage
                        FilterChip(
                            selected = selected,
                            onClick = { onStageSelect(stage) },
                            label = { Text(if (isPersian) stage.titleFa else stage.titleEn, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(if (selected) MaterialTheme.colorScheme.primary else status.color))
                            }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExperienceMode.values().forEach { mode ->
                        FilterChip(
                            selected = activeMode == mode,
                            onClick = { onModeChange(mode) },
                            label = { Text(if (isPersian) mode.displayNameFa else mode.displayNameEn, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExperienceModeSelector(
    activeMode: ExperienceMode,
    onModeChange: (ExperienceMode) -> Unit,
    isPersian: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Mode",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isPersian) "حالت ناوبری و کاربری:" else "Navigation Mode:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ExperienceMode.values().forEach { mode ->
                    val isSelected = mode == activeMode
                    Button(
                        onClick = { onModeChange(mode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isPersian) mode.displayNameFa else mode.displayNameEn,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VisualRoadmapStrip(
    currentStage: InvestigationStage,
    stageStatuses: Map<InvestigationStage, StageStatus>,
    onStageSelect: (InvestigationStage) -> Unit,
    isPersian: Boolean
) {
    val scrollState = rememberScrollState()

    // Auto scroll to current stage
    LaunchedEffect(currentStage) {
        // Simple heuristic for scrolling to show the active stage
        val scrollPosition = (currentStage.ordinal * 120).dp.value.toInt()
        scrollState.animateScrollTo(scrollPosition)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = if (isPersian) "نقشه راه تحقیقات دیجیتال" else "Visual Investigation Roadmap",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InvestigationStage.values().forEachIndexed { index, stage ->
                val status = stageStatuses[stage] ?: StageStatus.LOCKED
                val isCurrent = stage == currentStage

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .border(
                            width = 1.dp,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = status != StageStatus.LOCKED) {
                            onStageSelect(stage)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Custom Icon Indicator with Color
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(status.color.copy(alpha = 0.15f))
                            .border(width = 1.5.dp, color = status.color, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (status == StageStatus.COMPLETED) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = status.color, modifier = Modifier.size(12.dp))
                        } else if (status == StageStatus.CURRENT) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = status.color, modifier = Modifier.size(12.dp))
                        } else if (status == StageStatus.SKIPPED) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, tint = status.color, modifier = Modifier.size(12.dp))
                        } else if (status == StageStatus.WARNING) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = status.color, modifier = Modifier.size(12.dp))
                        } else {
                            Text(
                                text = stage.id.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = status.color
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (isPersian) stage.titleFa else stage.titleEn,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isPersian) status.displayNameFa else status.displayNameEn,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = status.color,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (index < InvestigationStage.values().size - 1) {
                        Icon(
                            imageVector = if (isPersian) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

data class NextBestAction(
    val proposalFa: String,
    val proposalEn: String,
    val reasonFa: String,
    val reasonEn: String,
    val evidenceFa: String,
    val evidenceEn: String,
    val expectedValueFa: String,
    val expectedValueEn: String,
    val targetStage: InvestigationStage
)

fun calculateNextBestAction(
    stage: InvestigationStage,
    case: InvestigationCase
): NextBestAction {
    return when (stage) {
        InvestigationStage.START_CASE -> NextBestAction(
            proposalFa = "اعتبارسنجی آدرس هدف و تعیین شبکه بلاکچین",
            proposalEn = "Validate target address and detect blockchain network",
            reasonFa = "آدرس ورودی باید از نظر ساختاری بررسی شده و شبکه بلاکچین آن (مانند بیت‌کوین، اتریوم، ترون) تایید شود.",
            reasonEn = "The starting lead address must be validated structurally and its network (Bitcoin, Ethereum, TRON) confirmed.",
            evidenceFa = "آدرس ثبت‌شده: ${case.targetAddress.take(8)}...",
            evidenceEn = "Registered address: ${case.targetAddress.take(8)}...",
            expectedValueFa = "تایید صحت آدرس و فعال‌سازی مراحل واکاوی تراکنش‌ها بدون نقص فنی.",
            expectedValueEn = "Confirming address format sanity and unlocking transaction queries without failures.",
            targetStage = InvestigationStage.INITIAL_LEAD
        )
        InvestigationStage.INITIAL_LEAD -> NextBestAction(
            proposalFa = "استعلام اطلاعات پایه بلاکچین و وضعیت دارایی‌ها",
            proposalEn = "Query blockchain details and asset balances",
            reasonFa = "پس از تایید قالب آدرس، بررسی وضعیت موجودی زنده، توکن‌های تحت مالکیت و تعداد UTXOها گام منطقی است.",
            reasonEn = "Following format validation, retrieving live balance, held tokens, and UTXO counts is the next logical step.",
            evidenceFa = "شبکه شناسایی شده: ${case.network.symbol}",
            evidenceEn = "Detected Network: ${case.network.symbol}",
            expectedValueFa = "به دست آوردن تراز مالی دقیق و تشخیص حجم انتقال‌ها برای تعیین فرضیات اولیه پرونده.",
            expectedValueEn = "Establishing precise financial status and volumes to structure starting hypotheses.",
            targetStage = InvestigationStage.BLOCKCHAIN_DISCOVERY
        )
        InvestigationStage.BLOCKCHAIN_DISCOVERY -> NextBestAction(
            proposalFa = "واکاوی تفصیلی دفترکل تراکنش‌های پرونده",
            proposalEn = "Analyze detailed transaction ledger",
            reasonFa = "آدرس هدف دارای ${case.transactions.size} تراکنش است. برای بررسی توالی انتقال‌ها، جریان وجوه تاریخی باید استخراج و دسته‌بندی شود.",
            reasonEn = "Target address has ${case.transactions.size} transactions. Investigating chronologies, flows, and values is essential.",
            evidenceFa = "تعداد تراکنش‌های خام ثبت‌شده: ${case.transactions.size}",
            evidenceEn = "Raw transactions found: ${case.transactions.size}",
            expectedValueFa = "دستیابی به توالی زمانی تراکنش‌ها، مبالغ دقیق ورودی/خروجی و تفکیک جریان‌های نقدی مشکوک.",
            expectedValueEn = "Obtaining temporal chronology, clean fiat values, and isolating suspicious fund transfers.",
            targetStage = InvestigationStage.TRANSACTIONS_LEDGER
        )
        InvestigationStage.TRANSACTIONS_LEDGER -> NextBestAction(
            proposalFa = "ترسیم گراف ارتباطات و خوشه‌بندی کیف‌پول‌ها",
            proposalEn = "Map transaction flow graph and counterparties",
            reasonFa = "با وجود ${case.counterparties.size} طرف حساب مستقیم، ساخت گراف روابط برای رهگیری تراکنش‌های تجمیعی و خرد الزامی است.",
            reasonEn = "With ${case.counterparties.size} direct counterparties, building the link graph is crucial to spot peel chains or pooling.",
            evidenceFa = "تعداد طرف حساب‌های ثبت‌شده: ${case.counterparties.size}",
            evidenceEn = "Identified direct counterparties: ${case.counterparties.size}",
            expectedValueFa = "شناسایی نهادهای میزبان (صرافی‌ها، میکسرها) و خوشه‌بندی آدرس‌های زنجیره‌ای تحت کنترل یک واحد.",
            expectedValueEn = "Detecting custodian hosts (exchanges, mixers) and grouping multi-addresses owned by same entity.",
            targetStage = InvestigationStage.RELATED_ADDRESSES
        )
        InvestigationStage.RELATED_ADDRESSES -> NextBestAction(
            proposalFa = "تطبیق رفتاری با قوانین و سناریوهای جرم‌شناختی مالی",
            proposalEn = "Match flow behaviors against crime typologies",
            reasonFa = "کیف‌پول مورد بررسی روابط عمیقی دارد. اجرای تطبیق رفتاری به ردیابی الگوهایی چون Peeling Chain و انتقال‌های متوالی کمک می‌کند.",
            reasonEn = "The target address exhibits dense flow networks. Matching behaviors identifies patterns like Peeling Chains and nested layering.",
            evidenceFa = "تراکنش‌های فیلترشده آماده تطبیق: ${case.transactions.size} عدد",
            evidenceEn = "Filtered transactions ready for pattern matching: ${case.transactions.size}",
            expectedValueFa = "کشف آدرس‌های انتقال تدریجی وجوه (پیلینگ)، ساختار هرمی و تکنیک‌های تطهیر وجه دیجیتال.",
            expectedValueEn = "Isolating progressive peeling addresses, pyramid hierarchies, and digital layering structures.",
            targetStage = InvestigationStage.PATTERN_ANALYSIS
        )
        InvestigationStage.PATTERN_ANALYSIS -> NextBestAction(
            proposalFa = "اجرای ردیابی اطلاعات منبع‌باز و اوسینت (OSINT)",
            proposalEn = "Perform open-source intelligence (OSINT) searches",
            reasonFa = "تطبیق الگوها شاخص‌هایی را نمایان کرد. با جستجو در منابع وب، انجمن‌ها و سوابق نشت داده، هویت‌های خارج‌زنجیره‌ای را کشف کنید.",
            reasonEn = "Pattern analysis revealed suspicious clusters. OSINT queries will seek off-chain email/forum/leak metadata correlations.",
            evidenceFa = "الگوهای فعال و همبستگی رفتاری هم‌راستا",
            evidenceEn = "Active behaviors and matching correlation matrices",
            expectedValueFa = "انتساب احتمالی آدرس به ایمیل، تلفن، شناسه مستعار یا وب‌سایت تجاری غیرقانونی.",
            expectedValueEn = "Attributing address candidates to emails, phone prefixes, aliases, or active darknet illicit portals.",
            targetStage = InvestigationStage.OSINT_REVIEW
        )
        InvestigationStage.OSINT_REVIEW -> NextBestAction(
            proposalFa = "ارزیابی یکپارچه شاخص‌های ریسک و فرضیه‌سازی",
            proposalEn = "Assess integrated risk indicators and structure hypotheses",
            reasonFa = "باید شواهد بلاکچینی را با یافته‌های هویتی اوسینت تلفیق کنیم تا سطح خطر، درگیری در تحریم‌ها و سناریوهای پرونده مشخص شود.",
            reasonEn = "We must merge on-chain evidence with off-chain OSINT discoveries to quantify regulatory risk and sanction levels.",
            evidenceFa = "گزارش OSINT و کاندیداهای هویتی همگام‌سازی شده",
            evidenceEn = "Synchronized OSINT report and candidate identity datasets",
            expectedValueFa = "رتبه‌بندی دقیق ریسک پرونده و تعریف فرضیه‌های مستند برای بازپرس قضایی.",
            expectedValueEn = "Formulating precise risk ranking metrics and formalizing forensic hypotheses.",
            targetStage = InvestigationStage.RISK_REVIEW
        )
        InvestigationStage.RISK_REVIEW -> NextBestAction(
            proposalFa = "مرور جامع و بستن زنجیره ادله دیجیتال",
            proposalEn = "Audit and lock the digital chain of custody",
            reasonFa = "برای اثبات نهایی پرونده، تک‌تک مشاهدات (تراکنش‌ها، انتساب‌های OSINT، تگ‌های ریسک) باید بازبینی، ممهور و تایید شوند.",
            reasonEn = "To guarantee judicial admissibility, each digital finding must be audited, annotated, and added to the custody chain.",
            evidenceFa = "تعداد ادله ثبت‌شده در پرونده: ${case.evidenceLog.size}",
            evidenceEn = "Total items in the evidence log: ${case.evidenceLog.size}",
            expectedValueFa = "ایجاد زنجیره ادله غیرقابل دستکاری و مستدل با مهر اصالت دیجیتال.",
            expectedValueEn = "Ensuring a tamper-proof and admissibility-ready evidence log with cryptographically sealed integrity.",
            targetStage = InvestigationStage.EVIDENCE_REVIEW
        )
        InvestigationStage.EVIDENCE_REVIEW -> NextBestAction(
            proposalFa = "ثبت استنباط نهایی و ارزیابی کارشناس فارنزیک",
            proposalEn = "Record final expert forensic assessment & conclusion",
            reasonFa = "با آماده بودن زنجیره ادله، ثبت نتیجه کارشناسی رسمی، پاسخ به پرسش‌های مرجع قضایی و فرضیه برگزیده الزامی است.",
            reasonEn = "With a sealed evidence log, entering the formal expert opinion and judicial summary answers is required.",
            evidenceFa = "شواهد قطعی ثبت‌شده: ${case.evidenceLog.count { it.confidence == com.aistudio.orbit.model.ConfidenceLevel.DEFINITIVE_FACT }} مورد",
            evidenceEn = "Definitive evidence items: ${case.evidenceLog.count { it.confidence == com.aistudio.orbit.model.ConfidenceLevel.DEFINITIVE_FACT }}",
            expectedValueFa = "مستندسازی فرضیه اثبات‌شده کارشناس و آماده‌سازی برای تولید گزارش حقوقی نهایی.",
            expectedValueEn = "Documenting the proven expert hypothesis to lay the foundation for legal reporting.",
            targetStage = InvestigationStage.CONCLUSION
        )
        InvestigationStage.CONCLUSION -> NextBestAction(
            proposalFa = "صدور و استخراج گزارش فنی رسمی (PDF/CSV)",
            proposalEn = "Generate and export official forensic report (PDF/CSV)",
            reasonFa = "پرونده تکمیل شده است. زمان تولید گزارش مکتوب فارسی/انگلیسی با فرمت استاندارد به همراه فلوچارت‌های تراکنشی و گواهینامه است.",
            reasonEn = "Investigation is completed. The final step is exporting the standardized bilingual court-ready PDF report with charts and seals.",
            evidenceFa = "خلاصه پرونده، ارزیابی کارشناس و زنجیره ادله ممهور",
            evidenceEn = "Validated summaries, expert assessments, and sealed custody records",
            expectedValueFa = "دریافت سند مکتوب قابل ارائه به مراجع قانونی جهت پیگیری حقوقی و توقیف دارایی‌ها.",
            expectedValueEn = "Obtaining a bilingual signed export dossier suitable for judicial asset-recovery mandates.",
            targetStage = InvestigationStage.REPORT
        )
        InvestigationStage.REPORT -> NextBestAction(
            proposalFa = "آرشیو پرونده و ذخیره‌سازی سوابق",
            proposalEn = "Archive case and lock evidence",
            reasonFa = "گزارش صادر شده است. برای حفظ حریم خصوصی کارفرما و رعایت اصول فارنزیک، اطلاعات پرونده را در پایگاه داده محلی تثبیت کنید.",
            reasonEn = "Reports are generated. For privacy and compliance, seal the investigation record in the local database.",
            evidenceFa = "گزارش صادر شده پرونده در حافظه محلی",
            evidenceEn = "Generated report file metadata located in local storage",
            expectedValueFa = "آرشیو پرونده با قابلیت استعلام و بازیابی مجدد سوابق ردیابی.",
            expectedValueEn = "Safe archival of the case dossier, preserving historic timelines for quick subpoena lookups.",
            targetStage = InvestigationStage.REPORT
        )
    }
}

fun getStageLessonTopic(stage: InvestigationStage): MiniLessonTopic {
    return when (stage) {
        InvestigationStage.START_CASE, InvestigationStage.INITIAL_LEAD -> MiniLessonTopic.ADDRESS_VS_WALLET
        InvestigationStage.BLOCKCHAIN_DISCOVERY -> MiniLessonTopic.UTXO_MODEL
        InvestigationStage.TRANSACTIONS_LEDGER -> MiniLessonTopic.CHANGE_ADDRESS
        InvestigationStage.RELATED_ADDRESSES -> MiniLessonTopic.WHY_CLUSTERED
        InvestigationStage.PATTERN_ANALYSIS -> MiniLessonTopic.PEELING_CHAIN
        InvestigationStage.OSINT_REVIEW -> MiniLessonTopic.OSINT_INTELLIGENCE
        InvestigationStage.RISK_REVIEW -> MiniLessonTopic.SANCTIONS_LIST
        InvestigationStage.EVIDENCE_REVIEW -> MiniLessonTopic.CONFIDENCE_METRIC
        InvestigationStage.CONCLUSION -> MiniLessonTopic.BLOCK_TIMESTAMP_VS_ACTIVITY
        InvestigationStage.REPORT -> MiniLessonTopic.VASP_REGULATION
    }
}

fun getStageForensicMeaning(stage: InvestigationStage, case: InvestigationCase, isPersian: Boolean): String {
    return when (stage) {
        InvestigationStage.START_CASE, InvestigationStage.INITIAL_LEAD -> if (isPersian)
            "تایید فرمت آدرس نشان‌دهنده ساختار رمزارزی معتبر است، اما به معنی مالکیت یک شخص واحد نیست؛ هر آدرس می‌تواند متعلق به یک والت خصوصی یا یک والت چندامضایی صرافی باشد."
        else
            "Address validation confirms structural correctness, but does not indicate single-entity ownership. The address may belong to a personal wallet, multi-sig contract, or custodian pool."

        InvestigationStage.BLOCKCHAIN_DISCOVERY -> if (isPersian)
            "موجودی ثبت‌شده در بلاکچین یک حقیقت قطعی غیرقابل انکار است. توجه داشته باشید که موجودی صفر به معنی عدم فعالیت نیست؛ بسیاری از شبکه‌های پولشویی وجوه را فوراً خالی می‌کنند."
        else
            "On-chain balance is an undisputed ledger fact. Note that a zero balance does NOT mean no activity; illicit schemes often drain funds immediately after receipt."

        InvestigationStage.TRANSACTIONS_LEDGER -> if (isPersian)
            "جریان وجوه در بیت‌کوین بر مبنای UTXO است؛ خروجی‌های یک تراکنش معمولاً شامل مقصد اصلی به همراه یک آدرس باقیمانده (Change Address) جدید هستند. همه خروجی‌ها دریافت‌کننده وجه نیستند."
        else
            "Bitcoin flows follow the UTXO model where outputs typically split between destination payment and a fresh change address. Not all transaction outputs represent beneficiaries."

        InvestigationStage.RELATED_ADDRESSES -> if (isPersian)
            "خوشه‌بندی آدرس‌ها بر اساس قاعده هزینه مشترک ورودی‌ها (CIOH) استوار است. در صورتی که تراکنش از نوع کوین‌جوین نباشد، آدرس‌های ورودی مشترک معمولاً متعلق به یک شخص یا کیف‌پول هستند."
        else
            "Address clustering relies on the Common-Input Ownership Heuristic. Unless CoinJoin mixing is present, co-spent inputs are analytically inferred to share common control."

        InvestigationStage.PATTERN_ANALYSIS -> if (isPersian)
            "انطباق با الگوهای رفتاری (نظیر Peeling Chain یا خردسازی) شاخص تحلیلی برای کشف پولشویی است؛ این انطباق سوءظن فنی ایجاد می‌کند اما به تنهایی اثبات‌کننده جرم نیست."
        else
            "Pattern matches (like Peeling Chains or Structuring) provide analytical suspicion and behavioral indicators, but do not constitute standalone proof of criminal conduct."

        InvestigationStage.OSINT_REVIEW -> if (isPersian)
            "یافته‌های منابع باز (فروم‌ها، دامنه‌ها، پایگاه‌های نشت داده) پل میان آدرس و دنیای واقعی هستند. هر داده OSINT باید تا زمان ارزیابی مستقل به عنوان فرضیه کاری تلقی شود."
        else
            "Open-source findings bridge on-chain hashes with real-world identities. All OSINT data must be classified as unverified hypotheses until independently corroborated."

        InvestigationStage.RISK_REVIEW -> if (isPersian)
            "شاخص‌های ریسک بر اساس تعامل با نهادهای پرخطر، میکسرها یا لیست‌های تحریمی محاسبه می‌شوند. تعامل با میکسر به معنی مجرمیت قطعی نیست اما اولویت بررسی را افزایش می‌دهد."
        else
            "Risk scores quantify exposure to high-risk services, mixers, or sanctions. Mixer exposure is an investigative indicator, not automatic judicial culpability."

        InvestigationStage.EVIDENCE_REVIEW -> if (isPersian)
            "زنجیره ادله باید حقایق قطعی بلاکچین را از استنتاجات و فرضیه‌ها تفکیک کند. این تفکیک شرط لازم برای پذیرش گزارش در محاکم قضایی است."
        else
            "The evidence chain must isolate immutable ledger facts from analytical inferences and working hypotheses to ensure courtroom admissibility."

        InvestigationStage.CONCLUSION -> if (isPersian)
            "ارزیابی نهایی کارشناس حاصل تلفیق ادله معتبر است؛ در این مرحله تناقضات برطرف شده و فرضیه برگزیده با ضریب اطمینان مشخص بیان می‌شود."
        else
            "The investigator's conclusion synthesizes verified evidence, resolving contradictions and articulating the favored hypothesis with measured confidence."

        InvestigationStage.REPORT -> if (isPersian)
            "گزارش نهایی سند قانونی پرونده است که شامل امضای دیجیتال، ارجاعات ادله و سلب مسئولیت‌های فارنزیک می‌باشد."
        else
            "The final dossier is a sealed legal instrument containing cryptographic hashes, evidence references, and forensic limitation disclaimers."
    }
}

fun getStageDeadEndAnalysis(stage: InvestigationStage, case: InvestigationCase, isPersian: Boolean): Pair<Boolean, String>? {
    return when {
        case.transactions.isEmpty() && case.balanceBtc == 0.0 -> {
            val message = if (isPersian)
                "هشدار بن‌بست ظاهری: هیچ تراکنش یا موجودی در این آدرس ثبت نشده است.\n• علت: آدرس ممکن است جدید باشد، یا تراکنش‌ها در شبکه دیگری صورت گرفته باشند.\n• اقدام جایگزین: شبکه بلاکچین را تغییر دهید یا منتظر ثبت تراکنش در مم‌پول بمانید."
            else
                "Potential Dead-End: No transactions or balance recorded on this address.\n• Cause: The address may be newly generated, or activity occurred on another network.\n• Alternative: Switch network or monitor the mempool for unconfirmed transactions."
            Pair(true, message)
        }
        case.balanceBtc == 0.0 && case.transactions.isNotEmpty() -> {
            val message = if (isPersian)
                "توجه: موجودی فعلی صفر است، اما این وضعیت بن‌بست نیست!\n• علت: کل وجوه به آدرس‌های دیگر منتقل شده است.\n• اقدام جایگزین: سوابق ${case.transactions.size} تراکنش و ${case.counterparties.size} طرف تراکنش را در گراف دنبال کنید."
            else
                "Note: Current balance is zero, but this is NOT a dead-end!\n• Cause: All funds have been transferred out to secondary wallets.\n• Alternative: Follow the historical trail across ${case.transactions.size} transactions and ${case.counterparties.size} counterparties."
            Pair(false, message)
        }
        else -> null
    }
}

@Composable
fun GuideStageTemplate(
    stage: InvestigationStage,
    case: InvestigationCase,
    isPersian: Boolean,
    onNavigateNext: (InvestigationStage) -> Unit,
    onSkipStage: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val strings = AppLocalization.getStrings(if (isPersian) AppLanguage.PERSIAN else AppLanguage.ENGLISH)
    val nextAction = remember(stage, case) { calculateNextBestAction(stage, case) }
    var skipReasonText by remember { mutableStateOf("") }
    var showSkipDialog by remember { mutableStateOf(false) }
    var isFullDetailExpanded by remember { mutableStateOf(false) }
    var activeLessonTopic by remember { mutableStateOf<MiniLessonTopic?>(null) }

    val stageLesson = remember(stage) { getStageLessonTopic(stage) }
    val forensicMeaning = remember(stage, case, isPersian) { getStageForensicMeaning(stage, case, isPersian) }
    val deadEndInfo = remember(stage, case, isPersian) { getStageDeadEndAnalysis(stage, case, isPersian) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. OBJECTIVE (Stage Objective + Context)
        ForensicCard(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${stage.id}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Text(
                            text = if (isPersian) stage.titleFa else stage.titleEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LearnThisBadge(
                        topic = stageLesson,
                        onClick = { activeLessonTopic = stageLesson }
                    )
                }

                Text(
                    text = if (isPersian) stage.objectiveFa else stage.objectiveEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. REQUIRED INPUTS
        ForensicCard(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Input,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = strings.requiredInputs,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.leadAddressLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = case.targetAddress.take(14) + "..." + case.targetAddress.takeLast(8),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = strings.blockchainNetwork,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        ForensicBadge(
                            text = "${case.network.symbol} (${case.network.name})",
                            badgeType = ForensicBadgeType.PRIMARY
                        )
                    }
                }
            }
        }

        // 3. CURRENT FINDINGS (Summarized with on-demand technical detail)
        ForensicCard(
            containerColor = MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.outlineVariant
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = strings.currentFindings,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    TextButton(
                        onClick = { isFullDetailExpanded = !isFullDetailExpanded },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullDetailExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isFullDetailExpanded) {
                                if (isPersian) "بستن جزئیات فنی" else "Hide Details"
                            } else {
                                if (isPersian) "مشاهده جزئیات کامل فنی" else "Show Full Details"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Summary Row (Always visible)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ForensicMetricCard(
                        label = strings.balance,
                        value = "${String.format(java.util.Locale.US, "%.4f", case.balanceBtc)} BTC",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    ForensicMetricCard(
                        label = strings.totalTransactions,
                        value = "${case.transactions.size}",
                        icon = Icons.Default.ReceiptLong,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    ForensicMetricCard(
                        label = strings.uniqueCounterparties,
                        value = "${case.counterparties.size}",
                        icon = Icons.Default.Group,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Expandable Full Live Sub-view Content
                AnimatedVisibility(visible = isFullDetailExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(8.dp))
                        content()
                    }
                }
            }
        }

        // 4. EVIDENCE COLLECTED (Categorized with Epistemic Badges)
        ForensicCard(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            borderColor = MaterialTheme.colorScheme.outlineVariant
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FactCheck,
                        contentDescription = null,
                        tint = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF4ADE80) else Color(0xFF2E7D32),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${strings.evidenceCollected} (${case.evidenceLog.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF4ADE80) else Color(0xFF2E7D32)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ForensicEpistemicBadge(
                        type = ForensicEpistemicType.OBSERVED_FACT,
                        isPersian = isPersian,
                        source = "Ledger RPC",
                        confidencePercent = 100,
                        compact = true,
                        modifier = Modifier.weight(1f)
                    )
                    ForensicEpistemicBadge(
                        type = ForensicEpistemicType.CALCULATED,
                        isPersian = isPersian,
                        source = "Engine",
                        confidencePercent = 95,
                        compact = true,
                        modifier = Modifier.weight(1f)
                    )
                    ForensicEpistemicBadge(
                        type = ForensicEpistemicType.INFERENCE,
                        isPersian = isPersian,
                        source = "Heuristics",
                        confidencePercent = 80,
                        compact = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. WHAT THIS MEANS (Forensic Translation)
        ForensicCard(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            borderColor = MaterialTheme.colorScheme.outlineVariant
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFFBBF24) else Color(0xFFFFA000),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = strings.whatThisMeans,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFFB923C) else Color(0xFFE65100)
                    )
                }
                Text(
                    text = forensicMeaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 6. RECOMMENDED NEXT ACTION (Next Best Action Engine)
        ForensicCard(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            borderColor = MaterialTheme.colorScheme.primary
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = strings.recommendedNextAction,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    ForensicBadge(
                        text = "AI RECOMMENDED",
                        badgeType = ForensicBadgeType.PRIMARY
                    )
                }

                Text(
                    text = if (isPersian) nextAction.proposalFa else nextAction.proposalEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isPersian) nextAction.reasonFa else nextAction.reasonEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPersian) "ادله حامی مادی:" else "Supporting Findings:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (isPersian) nextAction.evidenceFa else nextAction.evidenceEn,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPersian) "اثر تحلیلی پیش‌بینی‌شده:" else "Expected Analytical Impact:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (isPersian) nextAction.expectedValueFa else nextAction.expectedValueEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = { onNavigateNext(nextAction.targetStage) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isPersian) "شروع و اجرای اقدام پیشنهادی بعدی" else "Execute Recommended Step",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isPersian) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 7. ALTERNATIVE ACTIONS
        ForensicCard(
            containerColor = MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.outlineVariant
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.alternativeActions,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onNavigateNext(InvestigationStage.RELATED_ADDRESSES) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPersian) "گراف ارتباط" else "Graph", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { onNavigateNext(InvestigationStage.OSINT_REVIEW) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPersian) "منابع OSINT" else "OSINT", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { onNavigateNext(InvestigationStage.REPORT) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPersian) "گزارش نهایی" else "Report", style = MaterialTheme.typography.labelSmall)
                    }
                }

                TextButton(
                    onClick = { showSkipDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPersian) "عبور از این مرحله (با ثبت علت کارشناسی)" else "Skip Stage with Reason")
                }
            }
        }

        // 8. DEAD ENDS / BLOCKERS (Evidence-Aware Analysis)
        if (deadEndInfo != null) {
            val (isDeadEnd, explanation) = deadEndInfo
            ForensicCard(
                containerColor = if (isDeadEnd) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
                borderColor = if (isDeadEnd) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isDeadEnd) Icons.Default.ErrorOutline else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDeadEnd) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = strings.deadEndConditions,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeadEnd) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 9. LEARN ABOUT THIS CONCEPT (Contextual Education)
        val lesson = remember(stageLesson) { CryptoMiniLessonRegistry.getLesson(stageLesson) }
        ForensicCard(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            borderColor = MaterialTheme.colorScheme.outlineVariant
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isPersian) lesson.titleFa else lesson.titleEn,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextButton(onClick = { activeLessonTopic = stageLesson }) {
                        Text(
                            text = if (isPersian) "مطالعه توضیحات تکمیلی" else "Read Full Guide",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Text(
                    text = if (isPersian) lesson.whatIsItFa else lesson.whatIsItEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }

    // Contextual Educational Dialog
    activeLessonTopic?.let { topic ->
        CryptoMiniLessonDialog(
            topic = topic,
            isPersian = isPersian,
            onDismiss = { activeLessonTopic = null }
        )
    }

    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text(if (isPersian) "عدم نیاز به این مرحله تحلیلی" else "Skip Forensic Stage") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (isPersian) 
                            "لطفاً دلیل کارشناسی عدم انجام این مرحله را ذکر کنید. این علت در گزارش پرونده حقوقی درج خواهد شد."
                            else "Provide the forensic rationale for skipping this analysis. This explanation will be embedded in the official report."
                    )
                    OutlinedTextField(
                        value = skipReasonText,
                        onValueChange = { skipReasonText = it },
                        label = { Text(if (isPersian) "علت کارشناسی عدم نیاز" else "Expert Rationale") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = skipReasonText.ifBlank { 
                            if (isPersian) "برای این کلاسه پرونده نیاز به داده‌های این مرحله ارزیابی نگردید." 
                            else "No analytical requirements detected for this case scope." 
                        }
                        onSkipStage(reason)
                        showSkipDialog = false
                    }
                ) {
                    Text(if (isPersian) "تایید و ثبت عبور" else "Confirm Skip")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) {
                    Text(if (isPersian) "انصراف" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun DeadEndHandlingView(
    isPersian: Boolean,
    stageTitle: String,
    onRerun: () -> Unit
) {
    ForensicCard(
        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
        borderColor = MaterialTheme.colorScheme.error
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isPersian) "اطلاعات کافی برای واکاوی در مرحله وجود ندارد" else "Insufficient Evidence in Active Stage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = if (isPersian) 
                    "سیستم فارنزیک به بن‌بست رسید؛ هیچ داده، تراکنش یا موجودیتی در محدوده فیلترهای فعلی یافت نشد."
                    else "Forensic query reached a dead end; no transactions or entities found in current active filters.",
                style = MaterialTheme.typography.bodySmall
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))

            Text(
                text = if (isPersian) "🔍 چه چیزی مفقود است؟" else "🔍 What is missing?",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = if (isPersian) 
                    "تراکنش تاریخی در بلاکچین مبدا یا انتساب‌های معتبر هویتی در خارج از زنجیره."
                    else "Historic ledger transactions on selected network, or valid off-chain identity mappings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (isPersian) "💡 چرا مهم است؟" else "💡 Why does it matter?",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = if (isPersian) 
                    "برای شروع خوشه‌بندی و رهگیری جریان، وجود حداقل یک تراکنش با مقدار معتبر الزامی است."
                    else "To calculate clusters and peeling flow models, at least one validated transaction is required.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (isPersian) "🛠️ اقدامات و راه‌حل‌های پیشنهادی" else "🛠️ Suggested Actions",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isPersian) 
                    "۱. فعال‌سازی ارائه‌دهنده‌های جایگزین (بلاکچین آفلاین)\n۲. بازنشانی و تعریض فیلتر دوره زمانی ردیابی در کنترل‌های تحلیل کارشناسی\n۳. استفاده از آدرس مرتبط دیگر به عنوان سرنخ ورودی جدید\n۴. فشردن دکمه عبور موقت با ذکر علت نبود داده"
                    else "1. Enable backup local offline databases.\n2. Extend the observation date range in the expert settings.\n3. Input an alternative sibling address as a new starting lead.\n4. Skip this stage with an official rationale record.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onRerun,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isPersian) "تلاش مجدد و به‌روزرسانی شاخص‌های تحلیلی" else "Retry Query Heuristics")
            }
        }
    }
}

@Composable
fun ProviderFailureView(
    isPersian: Boolean,
    providerName: String,
    onRetry: () -> Unit
) {
    ForensicCard(
        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
                Text(
                    text = if (isPersian) "ارائه‌دهنده سرویس غیرفعال یا خارج از دسترس است" else "Service Provider Unavailable",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = if (isPersian) 
                    "ارائه‌دهنده خارجی $providerName در فاز فعلی با شکست روبرو شد. این موضوع مانع دسترسی زنده به داده‌ها است."
                    else "Service provider $providerName returned a connection failure. Live lookups are currently affected.",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = if (isPersian) "اثر بر پرونده: کاهش موقت سرعت واکشی تایید‌شده‌ها و نمایش داده‌های حافظه پنهان محلی." else "Impact: Temporary reliance on offline database and cached entries.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isPersian) "تلاش مجدد" else "Retry")
                }

                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isPersian) "سرویس آفلاین محلی" else "Use Local Offline")
                }
            }
        }
    }
}
