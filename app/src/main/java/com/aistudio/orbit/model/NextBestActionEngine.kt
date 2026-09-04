package com.aistudio.orbit.model

object NextBestActionEngine {
    fun determineNextBestAction(case: InvestigationCase?, hasOsint:Boolean, hasPatterns:Boolean, hasRisks:Boolean, hasHypotheses:Boolean): NextBestAction {
        if (case == null) return NextBestAction(
            "Start New Investigation or Quick Check", "No active investigation is loaded.", 0, "High", "Target Address", "START_CASE", 100, "None", "START",
            false, "شروع کاوش جدید یا بررسی سریع", "هیچ پرونده فعالی بارگذاری نشده است.", "زیاد", "آدرس هدف", "هیچ"
        )
        val evidence = case.evidenceLog.size
        val candidates = buildList {
            if (case.targetAddress.isBlank()) add(NextBestAction("Validate target address", "The case has no usable target address.", evidence, "Very High", "Target Address + Network", "INITIAL_LEAD", 100, "None", "VALIDATE_INPUT", false, "اعتبارسنجی آدرس هدف", "پرونده فاقد آدرس هدف معتبر است.", "بسیار زیاد", "آدرس هدف + شبکه", "هیچ"))
            if (case.transactions.isEmpty() && case.totalTransactionsFound == 0) add(NextBestAction("Run or retry ledger discovery", "No confirmed transaction-discovery result is recorded. Zero balance alone does not prove an empty history.", evidence, "Very High", "None", "BLOCKCHAIN_DISCOVERY", 95, "Provider quota", "DISCOVER_TRANSACTIONS", false, "واکشی بلاکچین و تراکنش‌ها", "هیچ نتیجه تایید شدهای از تراکنش‌ها ثبت نشده است. موجودی صفر به تنهایی تاریخچه خالی را اثبات نمی‌کند.", "بسیار زیاد", "هیچ", "سهمیه ارائه‌دهنده"))
            if (case.counterparties.isEmpty() && case.transactions.isNotEmpty()) add(NextBestAction("Extract related addresses", "Transactions exist but the relationship set has not been derived.", evidence, "High", "None", "RELATED_ADDRESSES", 90, "Local compute / provider expansion", "EXTRACT_COUNTERPARTIES", false, "استخراج آدرس‌های مرتبط", "تراکنش‌ها واکشی شده‌اند اما مجموعه روابط استخراج نشده است.", "زیاد", "هیچ", "محاسبات محلی"))
            if (!hasPatterns && case.counterparties.isNotEmpty()) add(NextBestAction("Evaluate approved typologies", "Compare observed structure against versioned indicators and counter-indicators.", evidence, "High", "None", "PATTERN_ANALYSIS", 85, "Local compute", "RUN_PATTERNS", false, "ارزیابی الگوها و تایپولوژی‌ها", "ساختار کشف‌شده با شاخص‌های تایید شده مقایسه و ارزیابی میشود.", "زیاد", "هیچ", "محاسبات محلی"))
            if (!hasOsint && (hasPatterns || case.counterparties.isNotEmpty())) add(NextBestAction("Review public OSINT sources", "Seek corroborating or contradictory public information without treating search hits as identity proof.", evidence, "High", "None", "OSINT_REVIEW", 80, "Search quota", "RUN_OSINT", false, "بررسی منابع اطلاعات آشکار (OSINT)", "جستجوی اطلاعات عمومی تاییدکننده یا متناقض بدون فرض اثبات هویت.", "زیاد", "هیچ", "سهمیه جستجو"))
            if (!hasRisks && hasOsint) add(NextBestAction("Run risk and sanctions review", "Cross-check observations against applicable risk datasets and preserve effective dates.", evidence, "High", "None", "RISK_REVIEW", 82, "Dataset/provider quota", "RUN_RISK", false, "ارزیابی ریسک و تحریم‌ها", "بررسی شواهد با لیست‌های ریسک و تحریم با حفظ تاریخ اثر.", "زیاد", "هیچ", "سهمیه ارائه‌دهنده"))
            if (evidence == 0 && (hasRisks || hasOsint || hasPatterns)) add(NextBestAction("Admit reviewed evidence", "Analytical outputs must be tied to source-backed evidence before conclusion drafting.", evidence, "Very High", "Analyst review", "EVIDENCE_REVIEW", 95, "None", "REVIEW_EVIDENCE", false, "تایید و ثبت شواهد", "خروجی‌های تحلیلی باید پیش از نتیجه‌گیری به عنوان شواهد مستند ثبت شوند.", "بسیار زیاد", "بررسی تحلیلگر", "هیچ"))
            if (!hasHypotheses && evidence > 0) add(NextBestAction("Formulate and review a hypothesis", "State what is being tested and link supporting and contradictory evidence.", evidence, "Very High", "Analyst input", "CONCLUSION", 75, "None", "CREATE_HYPOTHESIS", false, "طرح و بررسی فرضیه", "تعیین دقیق فرضیه و مرتبط کردن شواهد تایید و نقض‌کننده.", "بسیار زیاد", "ورودی تحلیلگر", "هیچ"))
        }
        return candidates.maxWithOrNull(compareBy<NextBestAction> { it.confidence }.thenBy { it.supportingEvidenceCount }) ?: NextBestAction(
            "Review conclusion and generate report", "Core investigative artifacts are present; final conclusions still require analyst adjudication.", evidence, "Very High", "Analyst review", "REPORT", 70, "None", "REPORT", false, "بررسی نهایی و صدور گزارش", "شواهد اصلی مهیاست؛ نتایج نهایی به قضاوت تحلیلگر نیاز دارند.", "بسیار زیاد", "بررسی تحلیلگر", "هیچ"
        )
    }
}
