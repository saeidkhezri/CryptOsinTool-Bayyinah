package com.aistudio.orbit.forensics.learning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

enum class MiniLessonTopic {
    BLOCKCHAIN,
    ADDRESS,
    TRANSACTION,
    UTXO,
    INPUT_OUTPUT,
    CHANGE_ADDRESS,
    FEE_CONFIRMATION,
    MEMPOOL_BLOCK,
    ADDRESS_CLUSTER,
    EXCHANGE_ATTRIBUTION,
    MIXER_TUMBLER,
    STABLECOIN_TOKEN,
    SMART_CONTRACT,
    CROSS_CHAIN,
    GRAPH_HOP,
    OSINT_INTELLIGENCE,
    SANCTIONS_LIST,
    VASP_REGULATION,
    PEELING_CHAIN,
    DIURNAL_CYCLE,
    WHY_CLUSTERED,
    CONFIDENCE_METRIC,
    BLOCK_TIMESTAMP_VS_ACTIVITY,
    ADDRESS_VS_WALLET,
    UTXO_MODEL
}

enum class LearningStatus {
    SEEN,
    UNDERSTOOD,
    USED,
    REVISITED
}

data class CryptoMiniLesson(
    val topic: MiniLessonTopic,
    val titleFa: String,
    val titleEn: String,
    val whatIsItFa: String,
    val whatIsItEn: String,
    val whyItMattersFa: String,
    val whyItMattersEn: String,
    val whatNotToInferFa: String,
    val whatNotToInferEn: String
)

object CryptoMiniLessonRegistry {
    private val lessons = mapOf(
        MiniLessonTopic.BLOCKCHAIN to CryptoMiniLesson(
            topic = MiniLessonTopic.BLOCKCHAIN,
            titleFa = "بلاکچین (شبکه داده توزیع‌شده)",
            titleEn = "Blockchain (Distributed Ledger)",
            whatIsItFa = "دفتر ثبت غیرقابل‌تغییر عمومی که تمام تراکنش‌های شبکه رمزارز را به صورت زمان‌بندی‌شده نگهداری می‌کند.",
            whatIsItEn = "An immutable public ledger recording all cryptocurrency transactions chronologically across a peer-to-peer network.",
            whyItMattersFa = "پایه اصلی تمام شواهد فارنزیک آن‌چین است؛ هر داده‌ای که روی بلاکچین ثبت شود، سندیت شفاف و دستکاری‌ناپذیر دارد.",
            whyItMattersEn = "Forms the foundational truth for all on-chain evidence; data recorded on-chain is permanent and tamper-proof.",
            whatNotToInferFa = "ثبت شدن تراکنش روی بلاکچین به معنای مشخص بودن مستقیم هویت واقعی اشخاص نیست؛ آدرس‌ها مستعار هستند.",
            whatNotToInferEn = "Recording a transaction on-chain does not directly prove real-world identities; addresses are pseudonymous."
        ),
        MiniLessonTopic.ADDRESS to CryptoMiniLesson(
            topic = MiniLessonTopic.ADDRESS,
            titleFa = "آدرس بلاکچین (شناسه مستعار)",
            titleEn = "Blockchain Address (Pseudonym)",
            whatIsItFa = "رشته‌ای متنی شامل حروف و ارقام که مانند شماره حساب جهت دریافت و ارسال ارز دیجیتال استفاده می‌شود.",
            whatIsItEn = "A string of alphanumeric characters serving as a public identifier for sending and receiving cryptocurrency.",
            whyItMattersFa = "نقطه شروع ردیابی جریان مالی است؛ آدرس‌ها گره‌های اصلی گراف مبادلات را تشکیل می‌دهند.",
            whyItMattersEn = "The starting seed for tracking financial flows; addresses constitute the primary nodes in transaction graphs.",
            whatNotToInferFa = "یک آدرس حتماً به معنی یک کیف‌پول یا یک شخص نیست؛ یک کیف‌پول می‌تواند هزاران آدرس تولید کند.",
            whatNotToInferEn = "An address does not automatically equal a single wallet or person; one wallet can generate thousands of addresses."
        ),
        MiniLessonTopic.UTXO to CryptoMiniLesson(
            topic = MiniLessonTopic.UTXO,
            titleFa = "خروجی خرج‌نشده (UTXO)",
            titleEn = "Unspent Transaction Output (UTXO)",
            whatIsItFa = "در شبکه بیت‌کوین، قطعات خروجی تراکنش‌های قبلی هستند که هنوز خرج نشده‌اند و موجودی کاربر را تشکیل می‌دهند.",
            whatIsItEn = "In Bitcoin architecture, discrete output chunks from previous transactions that remain unspent and form spendable balance.",
            whyItMattersFa = "واکشی UTXOها به تحلیل نحوه تجمیع خروجی‌ها، تفکیک آدرس باقی‌مانده و ردیابی مستقیم زنجیره خرج کمک می‌کند.",
            whyItMattersEn = "Analyzing UTXOs enables tracking fund lineage, input consolidation, and identifying change outputs.",
            whatNotToInferFa = "داشتن یک UTXO بزرگ به معنای ثروت یک فرد نیست؛ ممکن است متعلق به کیف‌پول گرم یک صرافی با هزاران کاربر باشد.",
            whatNotToInferEn = "Holding a large UTXO does not guarantee high personal net worth; it may belong to an exchange hot wallet."
        ),
        MiniLessonTopic.CHANGE_ADDRESS to CryptoMiniLesson(
            topic = MiniLessonTopic.CHANGE_ADDRESS,
            titleFa = "آدرس باقی‌مانده (Change Address)",
            titleEn = "Change Address",
            whatIsItFa = "آدرسی که باقی‌مانده ارزش یک UTXO پس از کسر مبلغ ارسالی و کارمزد، برای خود فرستنده مجدداً بازمی‌گردد.",
            whatIsItEn = "An address generated or controlled by the sender to receive the remaining balance after sending an output.",
            whyItMattersFa = "تشخیص آدرس باقی‌مانده کلیدی‌ترین قاعده تحلیلی (Heuristic) در تفکیک مقصد واقعی از بازگشت دارایی است.",
            whyItMattersEn = "Identifying change addresses is the critical heuristic for distinguishing actual payment recipients from self-transfers.",
            whatNotToInferFa = "آدرس باقی‌مانده همیشه یک آدرس جدید نیست؛ برخی کیف‌پول‌های قدیمی دارایی را به آدرس فرستنده بازمی‌گردانند.",
            whatNotToInferEn = "A change address is not guaranteed to be newly generated; older wallets may reuse sender addresses."
        ),
        MiniLessonTopic.ADDRESS_CLUSTER to CryptoMiniLesson(
            topic = MiniLessonTopic.ADDRESS_CLUSTER,
            titleFa = "خوشه آدرس‌ها (Address Cluster)",
            titleEn = "Address Cluster",
            whatIsItFa = "مجموعه‌ای از آدرس‌های مختلف که بر اساس قوانین تحلیلی (مانند ورودی مشترک CIOH) توسط یک کیف‌پول کنترل می‌شوند.",
            whatIsItEn = "A set of distinct addresses derived to be controlled by the same wallet/entity based on co-spending heuristics.",
            whyItMattersFa = "اجازه می‌دهد صدها آدرس پراکنده را به یک موجودیت یا کیف‌پول یکپارچه متصل کرده و حجم واقعی مبادلات را سنجید.",
            whyItMattersEn = "Allows combining multiple addresses into a single entity to reveal true wallet balance and transaction volume.",
            whatNotToInferFa = "خوشه‌بندی صددرصد قطعی نیست؛ تراکنش‌های CoinJoin یا میکسرها می‌توانند خوشه تحلیلی را آلوده کنند.",
            whatNotToInferEn = "Clustering is not 100% foolproof; CoinJoin or mixing transactions can intentionally taint heuristics."
        ),
        MiniLessonTopic.EXCHANGE_ATTRIBUTION to CryptoMiniLesson(
            topic = MiniLessonTopic.EXCHANGE_ATTRIBUTION,
            titleFa = "انتساب هویت صرافی (Exchange Attribution)",
            titleEn = "Exchange Attribution & Service Labeling",
            whatIsItFa = "شناسایی و برچسب‌گذاری آدرس‌ها به عنوان صرافی متمرکز، سرویس پرداخت، یا صرافی بدون احراز هویت (No-KYC).",
            whatIsItEn = "Mapping on-chain addresses to verified corporate entities, centralized exchanges, or VASP service providers.",
            whyItMattersFa = "نقطه خروج یا ورود رمزارز به سیستم بانکی و مالی رسمی را مشخص می‌سازد و امکان مکاتبه قانونی را فراهم می‌کند.",
            whyItMattersEn = "Pinpoints exit/entry ramps into traditional banking, identifying potential legal points of inquiry for KYC.",
            whatNotToInferFa = "انتساب صرافی به معنی گناهکار بودن صرافی نیست؛ صرافی تنها بستر واسط مبادلات کاربران متعدد است.",
            whatNotToInferEn = "An exchange attribution does not implicate the exchange in crime; it acts as a commercial intermediary."
        ),
        MiniLessonTopic.MIXER_TUMBLER to CryptoMiniLesson(
            topic = MiniLessonTopic.MIXER_TUMBLER,
            titleFa = "میکسر / ابزار گمنام‌سازی (Mixer)",
            titleEn = "Cryptocurrency Mixer / Tumbler",
            whatIsItFa = "سرویس یا شیوه‌ای تحلیلی که با خرد کردن و قطعه‌قطعه کردن تراکنش‌ها، ردپای ارتباط فرستنده و گیرنده را قطع می‌کند.",
            whatIsItEn = "A service or protocol designed to obscure the chain of custody by pooling and shuffling transaction outputs.",
            whyItMattersFa = "نشان‌دهنده تلاش عمدی جهت پنهان‌سازی مبدأ دارایی و افزایش ریسک شستشوی اموال است.",
            whyItMattersEn = "Indicates intentional obfuscation of fund origin and significantly increases money laundering risk score.",
            whatNotToInferFa = "تعامل با میکسر خود به خود به معنی مجرم بودن فرد نیست؛ برخی کاربران برای حفظ حریم خصوصی فردی استفاده می‌کنند.",
            whatNotToInferEn = "Mixer usage does not automatically establish criminal intent; privacy-seeking users also employ mixing protocols."
        ),
        MiniLessonTopic.PEELING_CHAIN to CryptoMiniLesson(
            topic = MiniLessonTopic.PEELING_CHAIN,
            titleFa = "زنجیره پوست‌کنی (Peeling Chain)",
            titleEn = "Peeling Chain Pattern",
            whatIsItFa = "الگویی که در آن دارایی طی تراکنش‌های پی‌درپی با کسر مبالغ کوچک به مقاصد مختلف، نهایتاً خرد می‌شود.",
            whatIsItEn = "A pattern where a large sum is repeatedly split into smaller payments while passing through a series of change addresses.",
            whyItMattersFa = "یکی از اصلی‌ترین الگوهای پولشویی خودکار برای تزریق مبالغ خرد به صرافی‌ها یا نقدسازی است.",
            whyItMattersEn = "A textbook laundering typology used to disperse large illicit funds into smaller unflagged exchange deposits.",
            whatNotToInferFa = "گاهی پردازش پرداخت صرافی‌ها یا کیف‌پول‌های تجاری نیز ظاهری شبیه به زنجیره پوست‌کنی ایجاد می‌کند.",
            whatNotToInferEn = "Automated merchant payout systems or exchange hot wallet distributions can mimic peeling chains."
        ),
        MiniLessonTopic.DIURNAL_CYCLE to CryptoMiniLesson(
            topic = MiniLessonTopic.DIURNAL_CYCLE,
            titleFa = "چرخه شبانه‌روزی (Diurnal Pattern)",
            titleEn = "Diurnal Activity Cycle & Timezone Heuristic",
            whatIsItFa = "تحلیل توزیع ۲۴ ساعته تراکنش‌ها بر اساس مناطق زمانی جهت شناسایی ساعات بیداری و فعالیت کاربر.",
            whatIsItEn = "Statistical mapping of transaction hours to evaluate local daytime working hours versus sleeping windows.",
            whyItMattersFa = "به محدود کردن مناطق جغرافیایی محتمل برای فعالیت گرداننده کیف‌پول کمک می‌کند.",
            whyItMattersEn = "Helps narrow down likely global geographic regions compatible with the operator's active hours.",
            whatNotToInferFa = "هرگز نباید ادعا کرد «مالک در کشور X است»؛ ربات‌های خودکار و استفاده از VPN الگوی زمانی را تغییر می‌دهند.",
            whatNotToInferEn = "Must NEVER be interpreted as 'Owner is in Country X'; automated bots or shifts invalidates timezone logic."
        ),
        MiniLessonTopic.OSINT_INTELLIGENCE to CryptoMiniLesson(
            topic = MiniLessonTopic.OSINT_INTELLIGENCE,
            titleFa = "هوش منابع باز (OSINT)",
            titleEn = "Open Source Intelligence (OSINT)",
            whatIsItFa = "جمع‌آوری اطلاعات عمومی از فروم‌ها، شبکه‌های اجتماعی، وب‌سایت‌ها و دامنه‌ها پیرامون آدرس رمزارز.",
            whatIsItEn = "Collecting publicly available information from forums, social networks, and domain records relating to crypto assets.",
            whyItMattersFa = "بین داده‌های آن‌چین و هویت‌های جهان واقعی، پل ارتباطی و سرنخ‌های غیررسمی ایجاد می‌کند.",
            whyItMattersEn = "Bridges abstract on-chain cryptographic addresses with real-world entities, usernames, and web footprints.",
            whatNotToInferFa = "یک متن یا ادعای موجود در وب‌سایت یا انجمن به معنی اثبات هویت نیست؛ نیاز به صحه‌گذاری مجزا دارد.",
            whatNotToInferEn = "An unverified mention or forum post does not prove identity or ownership without corroborating evidence."
        ),
        MiniLessonTopic.WHY_CLUSTERED to CryptoMiniLesson(
            topic = MiniLessonTopic.WHY_CLUSTERED,
            titleFa = "چرا این آدرس با آدرس دیگر در یک خوشه قرار گرفته است؟",
            titleEn = "Why Are These Addresses Clustered Together?",
            whatIsItFa = "قاعده تحلیلی هزینه مشترک ورودی‌ها (Common Input Ownership Heuristic): وقتی چند آدرس مختلف به عنوان ورودی (Input) یک تراکنش واحد خرج می‌شوند، معمولاً توسط یک کلید خصوصی یا یک کیف‌پول واحد کنترل می‌شوند.",
            whatIsItEn = "Common-Input Ownership Heuristic (CIOH): When multiple distinct addresses are spent together as inputs in a single transaction, they are presumed to be co-owned or controlled by the same wallet entity.",
            whyItMattersFa = "به کشف کل شبکه کیف‌پول پنهان‌شده پشت آدرس‌های یک‌بارمصرف و آدرس‌های باقیمانده (Change Addresses) کمک می‌کند.",
            whyItMattersEn = "Reveals the full underlying wallet cluster and control boundary obscured across single-use addresses.",
            whatNotToInferFa = "توجه: در تراکنش‌های کوین‌جوین (CoinJoin) یا میکسرها، چندین کاربر ناشناس ورودی‌های خود را مشترکاً در یک تراکنش می‌آورند؛ بنابراین کوین‌جوین نباید به عنوان یک خوشه تک‌مالک تلقی شود.",
            whatNotToInferEn = "Caution: CoinJoin, PayJoin, and multi-party aggregator transactions combine inputs from different owners. Clustered grouping is an inference, not an absolute cryptographic fact."
        ),
        MiniLessonTopic.CONFIDENCE_METRIC to CryptoMiniLesson(
            topic = MiniLessonTopic.CONFIDENCE_METRIC,
            titleFa = "این درصد اطمینان دقیقاً چه چیزی را اندازه می‌گیرد؟",
            titleEn = "What Does This Confidence Metric Actually Measure?",
            whatIsItFa = "میزان تاییدپذیری، استقلال منابع، سازگاری زمانی و کیفیت ادله جمع‌آوری‌شده برای یک ادعای تحلیلی خاص.",
            whatIsItEn = "A structured evidentiary index quantifying multi-source corroboration, source independence, temporal validity, and heuristic reliability.",
            whyItMattersFa = "از اشتباه گرفتن احتمال و سوءظن با مدارک اثباتی غیرقابل انکار در محاکم قضایی جلوگیری می‌کند.",
            whyItMattersEn = "Prevents conflating statistical suspicion or heuristic probability with immutable cryptographic proof.",
            whatNotToInferFa = "درصد اطمینان بالا (مثلاً ۹۰٪) به معنی حکم قطعی دادگاه یا اثبات مجرمیت شخص نیست؛ بلکه نشان‌دهنده استحکام ادله جمع‌آوری‌شده است.",
            whatNotToInferEn = "A high score measures evidence consistency and independent corroboration, not judicial culpability."
        ),
        MiniLessonTopic.BLOCK_TIMESTAMP_VS_ACTIVITY to CryptoMiniLesson(
            topic = MiniLessonTopic.BLOCK_TIMESTAMP_VS_ACTIVITY,
            titleFa = "چرا زمان بلاک معادل زمان فعالیت کاربر نیست؟",
            titleEn = "Why Block Timestamp Does Not Equal User Activity Time",
            whatIsItFa = "زمان ثبت‌شده در هدر بلاک (nTime) توسط ماینری که بلاک را استخراج کرده تعیین می‌شود و پروتکل اجازه تا ۲ ساعت انحراف با زمان واقعی شبکه را می‌دهد.",
            whatIsItEn = "The timestamp in a block header is set by the miner upon block creation, with consensus rules allowing up to 2 hours of temporal drift relative to network adjusted time.",
            whyItMattersFa = "تحلیلگران باید تاخیر انتظار در مم‌پول (Mempool) و بازه خطای ماینر را در تحلیل‌های شبانه‌روزی (Diurnal) و مناطق زمانی لحاظ کنند.",
            whyItMattersEn = "Investigators must account for mempool residency delay and miner clock skew before inferring user timezone.",
            whatNotToInferFa = "زمان بلاک را نباید به عنوان ثانیه دقیق فشردن دکمه ارسال توسط متهم در نظر گرفت؛ این زمان صرفاً زمان تقریبی استخراج بلاک توسط استخر است.",
            whatNotToInferEn = "Never use raw block timestamps as proof of the suspect's exact second of physical interaction."
        ),
        MiniLessonTopic.ADDRESS_VS_WALLET to CryptoMiniLesson(
            topic = MiniLessonTopic.ADDRESS_VS_WALLET,
            titleFa = "آدرس در برابر کیف‌پول (Address vs Wallet)",
            titleEn = "Address vs Wallet Distinction",
            whatIsItFa = "آدرس یک شناسه دریافت وجه رمزنگاری‌شده است، در حالی که کیف‌پول مجموعه‌ای از کلیدها است که می‌تواند هزاران آدرس را همزمان کنترل کند.",
            whatIsItEn = "An address is a single cryptographic receive endpoint, whereas a wallet manages hundreds or thousands of addresses under private keys.",
            whyItMattersFa = "عدم درک این تفاوت منجر به سوءتعبیر خطرناک در پرونده می‌شود؛ نباید فعالیت یک آدرس را به کل دارایی‌های سوژه تعمیم داد.",
            whyItMattersEn = "Failing to distinguish them leads to flawed forensic conclusions; one address does not reveal the subject's entire holdings.",
            whatNotToInferFa = "هیچ‌گاه فرض نکنید هر آدرس جدید یعنی یک فرد جدید یا صاحب حساب مجزا.",
            whatNotToInferEn = "Never assume every distinct address belongs to a different real-world person or entity."
        ),
        MiniLessonTopic.UTXO_MODEL to CryptoMiniLesson(
            topic = MiniLessonTopic.UTXO_MODEL,
            titleFa = "مدل خروجی خرج‌نشده (UTXO Model)",
            titleEn = "UTXO Accounting Model",
            whatIsItFa = "مدل حسابداری بیت‌کوین که دارایی را به صورت قطعات خروجی تراکنش‌های قبلی که هنوز خرج نشده‌اند نگهداری می‌کند.",
            whatIsItEn = "The Bitcoin accounting model which, unlike account-based ledgers, represents balances as discrete unspent coin outputs.",
            whyItMattersFa = "درک UTXO برای ردیابی خوشه‌بندی ورودی مشترک (CIOH) و پیلینگ‌چین حیاتی است.",
            whyItMattersEn = "Understanding UTXOs is essential for common-input clustering heuristics and peel chain tracing.",
            whatNotToInferFa = "خرج شدن یک UTXO به معنی انتقال کامل وجه به شخص ثالث نیست؛ معمولاً بخشی از آن به عنوان باقیمانده به فرستنده بازمی‌گردد.",
            whatNotToInferEn = "Spending a UTXO does not mean transferring the entire sum to a third party; change outputs return to the spender."
        )
    )

    fun getLesson(topic: MiniLessonTopic): CryptoMiniLesson {
        return lessons[topic] ?: CryptoMiniLesson(
            topic = topic,
            titleFa = topic.name,
            titleEn = topic.name,
            whatIsItFa = "مفهوم تخصصی در جرم‌یابی رمزارز.",
            whatIsItEn = "A specialized concept in cryptocurrency forensics.",
            whyItMattersFa = "به درک بهتر جریان مالی و تحلیل ادله کمک می‌کند.",
            whyItMattersEn = "Helps understand financial flow and evidence analysis.",
            whatNotToInferFa = "نباید بدون ادله کافی نتیجه‌گیری قطعی انجام داد.",
            whatNotToInferEn = "Should not be taken as definitive proof without supporting evidence."
        )
    }

    fun getAllLessons(): List<CryptoMiniLesson> = lessons.values.toList()
}

@Composable
fun LearnThisBadge(
    topic: MiniLessonTopic,
    isPersian: Boolean = true,
    onClick: () -> Unit
) {
    val lesson = remember(topic) { CryptoMiniLessonRegistry.getLesson(topic) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = if (isPersian) "آموزش: ${lesson.titleFa}" else "Learn: ${lesson.titleEn}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CryptoMiniLessonDialog(
    topic: MiniLessonTopic,
    isPersian: Boolean = true,
    onDismiss: () -> Unit,
    onMarkUnderstood: (() -> Unit)? = null
) {
    val lesson = remember(topic) { CryptoMiniLessonRegistry.getLesson(topic) }
    CryptoMiniLessonDialog(
        lesson = lesson,
        isPersian = isPersian,
        onDismiss = onDismiss,
        onMarkUnderstood = onMarkUnderstood
    )
}

@Composable
fun CryptoMiniLessonDialog(
    lesson: CryptoMiniLesson,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onMarkUnderstood: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isPersian) "مفهوم کارشناسی رمزارز" else "Crypto Forensic Concept",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isPersian) lesson.titleFa else lesson.titleEn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // What is it?
                LessonSectionBox(
                    icon = Icons.Default.HelpOutline,
                    title = if (isPersian) "چیست؟" else "What is it?",
                    content = if (isPersian) lesson.whatIsItFa else lesson.whatIsItEn,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )

                // Why it matters?
                LessonSectionBox(
                    icon = Icons.Default.SavedSearch,
                    title = if (isPersian) "در این تحقیق چرا مهم است؟" else "Why does it matter in this case?",
                    content = if (isPersian) lesson.whyItMattersFa else lesson.whyItMattersEn,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )

                // What NOT to infer?
                LessonSectionBox(
                    icon = Icons.Default.WarningAmber,
                    title = if (isPersian) "چه برداشتی نباید از آن داشت؟" else "What should NOT be inferred?",
                    content = if (isPersian) lesson.whatNotToInferFa else lesson.whatNotToInferEn,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    titleColor = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onMarkUnderstood != null) {
                        Button(
                            onClick = {
                                onMarkUnderstood()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPersian) "متوجه شدم (ثبت)" else "Understood")
                        }
                    } else {
                        OutlinedButton(onClick = onDismiss) {
                            Text(if (isPersian) "بستن" else "Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonSectionBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String,
    containerColor: Color,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
            }
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
