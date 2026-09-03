import json

typologies = [
    ("A", "PAT_AML_FAN_IN", "AML_FAN_IN", "Fan-In (Consolidation)", "تجمیع و ادغام (Fan-In)", "MONEY_LAUNDERING", "Many source addresses send funds to one destination.", "ارسال وجه از آدرس‌های متعدد به یک مقصد واحد."),
    ("B", "PAT_AML_FAN_OUT", "AML_FAN_OUT", "Fan-Out (Dispersion)", "خردسازی و توزیع (Fan-Out)", "MONEY_LAUNDERING", "One address distributes funds to many destinations.", "توزیع وجه از یک آدرس به چندین مقصد مجزا."),
    ("C", "PAT_AML_STRUCTURING", "AML_STRUCTURING", "Structuring / Smurfing Indicators", "شاخص‌های خردسازی (Structuring/Smurfing)", "STRUCTURING_SMURFING", "Repeated transfers that may indicate deliberate fragmentation.", "تراکنش‌های مکرر که نشان‌دهنده خردسازی عمدی مبالغ است."),
    ("D", "PAT_AML_LAYERING", "AML_LAYERING", "Layering", "لایه‌بندی مالی (Layering)", "MONEY_LAUNDERING", "Multi-hop movement through intermediary addresses.", "حرکت وجوه از طریق چندین آدرس واسطه (هاپ‌های متعدد)."),
    ("E", "PAT_AML_PEEL_CHAIN", "AML_PEEL_CHAIN", "Peel Chain", "زنجیره پوست‌کنی (Peel Chain)", "MONEY_LAUNDERING", "Repeated partial transfers where one address continues moving a remainder.", "انتقال‌های جزئی مکرر که در آن یک آدرس مابقی وجه را مدام به جلو می‌راند."),
    ("F", "PAT_AML_RAPID_PASS_THROUGH", "AML_RAPID_PASS_THROUGH", "Rapid Pass-Through", "انتقال سریع و بی‌درنگ", "HIGH_VELOCITY_TRANSIT", "Addresses that receive funds and move most or all of them shortly afterward.", "آدرس‌هایی که وجوه را دریافت کرده و بلافاصله بیشتر یا تمام آن را منتقل می‌کنند."),
    ("G", "PAT_AML_DORMANT_ACTIVATION", "AML_DORMANT_ACTIVATION", "Dormant Activation", "فعال‌سازی حساب خوابیده", "BEHAVIORAL_ANOMALY", "Addresses with long inactivity followed by unusual activity.", "آدرس‌هایی با عدم فعالیت طولانی که به ناگهان فعالیت غیرعادی نشان می‌دهند."),
    ("H", "PAT_AML_RAPID_SUCCESSION", "AML_RAPID_SUCCESSION", "Rapid Succession", "توالی سریع تراکنش‌ها", "HIGH_VELOCITY_TRANSIT", "Multiple transfers occurring within unusually short intervals.", "تراکنش‌های متعددی که در فواصل زمانی بسیار کوتاه رخ می‌دهند."),
    ("I", "PAT_AML_CYCLIC_FLOW", "AML_CYCLIC_FLOW", "Cyclic Flow", "جریان مدور", "MONEY_LAUNDERING", "Funds returning through a path to a previous address/entity.", "بازگشت وجوه از طریق یک مسیر به آدرس یا موجودیت قبلی."),
    ("J", "PAT_AML_ROUND_TRIPPING", "AML_ROUND_TRIPPING", "Round-Tripping / Wash-Like Cycle", "چرخه شستشوی دارایی (Round-Tripping)", "MONEY_LAUNDERING", "Repeated circulation of assets through related addresses.", "چرخش مکرر دارایی‌ها در میان آدرس‌های مرتبط."),
    ("K", "PAT_AML_EXCHANGE_HOPPING", "AML_EXCHANGE_HOPPING", "Exchange Hopping", "پرش میان صرافی‌ها", "MONEY_LAUNDERING", "Rapid movement across multiple exchange/service labels.", "انتقال سریع وجوه در میان چندین صرافی یا سرویس مختلف."),
    ("L", "PAT_AML_MIXER_EXPOSURE", "AML_MIXER_EXPOSURE", "Mixer / Tumbler Exposure", "ارتباط با میکسر/تامبلر", "MIXER_OBFUSCATION", "Interaction with known or externally tagged mixer/tumbler services.", "تعامل با سرویس‌های شناخته‌شده میکسر یا تامبلر جهت ناشناس‌سازی."),
    ("M", "PAT_AML_BRIDGE_HOPPING", "AML_BRIDGE_HOPPING", "Bridge Hopping", "پرش میان پل‌های بلاکچینی", "MONEY_LAUNDERING", "Movement across blockchain bridges.", "جابه‌جایی دارایی‌ها از طریق پل‌های بلاکچینی میان شبکه‌های مختلف."),
    ("N", "PAT_AML_MULTI_ASSET_CONVERSION", "AML_MULTI_ASSET_CONVERSION", "Multi-Asset Conversion", "تبدیل چندگانه دارایی‌ها", "MONEY_LAUNDERING", "Rapid switching between assets.", "تغییر و تبدیل سریع میان دارایی‌های رمزنگاری‌شده مختلف."),
    ("O", "PAT_AML_CONSOLIDATION", "AML_CONSOLIDATION", "Consolidation", "تجمیع نهایی", "MONEY_LAUNDERING", "Many addresses eventually transferring funds into a smaller number of destinations.", "انتقال وجوه از آدرس‌های متعدد به تعداد محدودی آدرس مقصد."),
    ("P", "PAT_AML_DISPERSION", "AML_DISPERSION", "Dispersion", "پراکندگی", "MONEY_LAUNDERING", "One source distributing funds across many addresses.", "توزیع وجوه از یک منبع به تعداد زیادی آدرس دیگر."),
    ("Q", "PAT_AML_BURST_ACTIVITY", "AML_BURST_ACTIVITY", "Burst Activity", "فعالیت انفجاری", "BEHAVIORAL_ANOMALY", "Abnormal concentration of transactions within a short time period.", "تمرکز غیرعادی تراکنش‌ها در یک بازه زمانی کوتاه."),
    ("R", "PAT_AML_HIGH_VELOCITY", "AML_HIGH_VELOCITY", "High Velocity", "سرعت گردش بالا", "HIGH_VELOCITY_TRANSIT", "Unusually rapid movement of value.", "جابه‌جایی غیرعادی و سریع ارزش مالی."),
    ("S", "PAT_AML_ADDRESS_REUSE", "AML_ADDRESS_REUSE", "Address Reuse / Operational Reuse", "استفاده مجدد و عملیاتی از آدرس", "BEHAVIORAL_ANOMALY", "Recurring operational relationships.", "روابط عملیاتی تکرارشونده و استفاده مجدد از آدرس‌ها."),
    ("T", "PAT_AML_HIGH_RISK_EXPOSURE", "AML_HIGH_RISK_EXPOSURE", "High-Risk Service Exposure", "مواجهه با سرویس‌های پرخطر", "EXTERNAL_EXPOSURE", "Interaction with addresses/services carrying verified external risk labels.", "تعامل با آدرس‌ها یا سرویس‌هایی که برچسب ریسک خارجی تایید شده دارند."),
    ("U", "PAT_AML_SCAM_COLLECTION", "AML_SCAM_COLLECTION", "Possible Scam Collection Pattern", "الگوی احتمالی جمع‌آوری وجوه کلاهبرداری", "FRAUD_SCAM", "Patterns potentially consistent with scam collection behavior.", "الگوهایی که به طور بالقوه با رفتار جمع‌آوری وجوه کلاهبرداری سازگارند."),
    ("V", "PAT_AML_RANSOM_PAYMENT", "AML_RANSOM_PAYMENT", "Possible Ransom Payment Pattern", "الگوی احتمالی پرداخت باج‌افزار", "RANSOMWARE", "Patterns potentially consistent with ransom-related collection and subsequent movement.", "الگوهایی که به طور بالقوه با جمع‌آوری باج و انتقال بعدی آن سازگارند."),
    ("W", "PAT_AML_EXTORTION", "AML_EXTORTION", "Possible Extortion / Coercive Payment Pattern", "الگوی احتمالی پرداخت اجباری/اخاذی", "EXTORTION_COERCION", "Create a pattern framework but do not claim legal conclusions.", "الگوهای مرتبط با باج‌گیری و پرداخت‌های اجباری (بدون نتیجه‌گیری قطعی حقوقی)."),
    ("X", "PAT_AML_ILLICIT_SERVICE", "AML_ILLICIT_SERVICE", "Possible Illicit-Service Payment", "پرداخت احتمالی به سرویس‌های غیرقانونی", "THEFT_EXPLOIT", "Behavioral patterns potentially associated with payments to known illicit-service entities.", "الگوهای رفتاری مرتبط با پرداخت به نهادهای سرویس‌دهنده غیرقانونی."),
    ("Y", "PAT_AML_SANCTIONS_EXPOSURE", "AML_SANCTIONS_EXPOSURE", "Sanctions Exposure", "مواجهه با نهادهای تحریم‌شده", "SANCTIONS_EVASION", "Direct or indirect interaction with sanctioned entities/addresses where reliable external data exists.", "تعامل مستقیم یا غیرمستقیم با نهادها/آدرس‌های تحریم‌شده بر اساس داده‌های موثق خارجی."),
    ("Z", "PAT_AML_PEP_EXPOSURE", "AML_PEP_EXPOSURE", "PEP / High-Profile Entity Exposure", "ارتباط با اشخاص مهم سیاسی (PEP)", "EXTERNAL_EXPOSURE", "External intelligence relationships while maintaining strict separation between identity information and blockchain evidence.", "ارتباطات اطلاعاتی خارجی مرتبط با افراد دارای ریسک سیاسی بالا.")
]

code = "package com.aistudio.orbit.forensics.patterns\n\nimport com.aistudio.orbit.model.*\n\nobject CrimePatternLibrary {\n\n    val patterns: List<CrimePattern> = listOf(\n"

for p in typologies:
    code += f"""        CrimePattern(
            id = "{p[1]}",
            code = "{p[2]}",
            nameEn = "{p[0]}. {p[3]}",
            nameFa = "{p[0]}. {p[4]}",
            category = CrimeCategory.{p[5]},
            descriptionEn = "{p[6]}",
            descriptionFa = "{p[7]}",
            behavioralIndicatorsEn = listOf("Indicator 1 for {p[3]}", "Indicator 2 for {p[3]}"),
            behavioralIndicatorsFa = listOf("شاخص ۱ برای {p[4]}", "شاخص ۲ برای {p[4]}"),
            detectionRules = "Rule: Matches definition of {p[3]}",
            scoringModel = "Deterministic rule logic + Graph analytics",
            confidenceInterpretationEn = "Analytical hypothesis requiring contextual corroboration.",
            confidenceInterpretationFa = "فرضیه تحلیلی که نیازمند تایید شواهد زمینه‌ای است.",
            references = listOf("FATF Red Flags for Virtual Assets", "Internal Typology Library")
        ),
"""

code = code[:-2] + "\n    )\n\n"
code += "    val referenceCases: List<ReferenceCase> = listOf()\n}\n"

with open("generate_typologies_out.kt", "w") as f:
    f.write(code)

