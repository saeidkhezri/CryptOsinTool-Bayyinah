package com.aistudio.orbit.forensics.osint.identity

import kotlinx.serialization.Serializable

/**
 * PhoneInfoga-inspired International Phone Number Analysis and Pruning Engine.
 * Handles E.164 normalization, carrier classification, line-type verification (Mobile vs. VoIP),
 * and prefix/NDC filtering for candidate reconstruction pools.
 */
object PhoneInfogaEngine {

    @Serializable
    data class ParsedPhoneNumber(
        val rawInput: String,
        val e164Format: String,
        val internationalFormat: String,
        val nationalFormat: String,
        val countryCode: String,
        val countryIso2: String,
        val countryNameEn: String,
        val countryNameFa: String,
        val carrierName: String,
        val carrierNameFa: String,
        val lineType: PhoneLineType,
        val isValidStructure: Boolean,
        val isPossibleNumber: Boolean,
        val areaOrOperatorPrefix: String
    )

    enum class PhoneLineType(val displayNameEn: String, val displayNameFa: String) {
        MOBILE("Mobile Cellular", "تلفن همراه سلولار"),
        FIXED_LINE("Landline / Fixed Line", "تلفن ثابت زمینی"),
        VOIP("VoIP / Virtual Number", "شماره مجازی / اینترنتی (VoIP)"),
        TOLL_FREE("Toll-Free / Service", "شماره رایگان / سازمانی"),
        UNKNOWN("Unclassified Line", "نامشخص")
    }

    /**
     * Parses, normalizes, and classifies a phone number string.
     */
    fun parse(input: String): ParsedPhoneNumber {
        val digitsOnly = input.replace(Regex("[^0-9+]"), "")
        val normalized = when {
            digitsOnly.startsWith("00") -> "+" + digitsOnly.substring(2)
            digitsOnly.startsWith("09") && digitsOnly.length == 11 -> "+98" + digitsOnly.substring(1)
            digitsOnly.startsWith("989") && digitsOnly.length == 12 -> "+$digitsOnly"
            !digitsOnly.startsWith("+") && digitsOnly.length == 10 -> "+1$digitsOnly" // Default to US if 10 digits
            else -> if (!digitsOnly.startsWith("+")) "+$digitsOnly" else digitsOnly
        }

        return when {
            // Iran (+98)
            normalized.startsWith("+98") -> parseIranNumber(normalized)
            // United States / Canada (+1)
            normalized.startsWith("+1") -> parseNorthAmericaNumber(normalized)
            // United Kingdom (+44)
            normalized.startsWith("+44") -> parseUkNumber(normalized)
            // United Arab Emirates (+971)
            normalized.startsWith("+971") -> parseUaeNumber(normalized)
            // Turkey (+90)
            normalized.startsWith("+90") -> parseTurkeyNumber(normalized)
            // Germany (+49)
            normalized.startsWith("+49") -> parseGenericE164(normalized, "49", "DE", "Germany", "آلمان")
            // France (+33)
            normalized.startsWith("+33") -> parseGenericE164(normalized, "33", "FR", "France", "فرانسه")
            // Russia (+7)
            normalized.startsWith("+7") -> parseGenericE164(normalized, "7", "RU", "Russia", "روسیه")
            else -> parseGenericE164(normalized, "0", "XX", "International", "بین‌المللی")
        }
    }

    private fun parseIranNumber(e164: String): ParsedPhoneNumber {
        val national = if (e164.startsWith("+98")) e164.substring(3) else e164
        val formattedNational = "0$national"
        val prefix = formattedNational.take(4)

        val (carrier, carrierFa, lineType) = when {
            // MCI (Hamrah-e Aval)
            prefix in listOf("0910", "0911", "0912", "0913", "0914", "0915", "0916", "0917", "0918", "0919", "0990", "0991", "0992", "0993", "0994") ->
                Triple("Mobile Telecommunication Co. (MCI / Hamrah Aval)", "همراه اول (ارتباطات سیار ایران)", PhoneLineType.MOBILE)
            // MTN Irancell
            prefix in listOf("0930", "0933", "0935", "0936", "0937", "0938", "0939", "0901", "0902", "0903", "0904", "0905", "0941") ->
                Triple("MTN Irancell", "ایرانسل (MTN)", PhoneLineType.MOBILE)
            // Rightel
            prefix in listOf("0920", "0921", "0922", "0923") ->
                Triple("RighTel Mobile", "رایتل", PhoneLineType.MOBILE)
            // Shatel Mobile
            prefix == "0998" ->
                Triple("Shatel Mobile (MVNO)", "شاتل موبایل", PhoneLineType.MOBILE)
            // SamanTel
            prefix == "0999" ->
                Triple("SamanTel (MVNO)", "سامان‌تل", PhoneLineType.MOBILE)
            // Tehran Landlines (021)
            prefix.startsWith("021") || formattedNational.startsWith("021") ->
                Triple("Telecommunication Company of Iran (Tehran Fixed Line)", "مخابرات استان تهران (تلفن ثابت)", PhoneLineType.FIXED_LINE)
            // Other provinces landline
            formattedNational.startsWith("031") || formattedNational.startsWith("051") || formattedNational.startsWith("071") || formattedNational.startsWith("041") ->
                Triple("Telecommunication Company of Iran (Provincial Landline)", "تلفن ثابت مخابرات ایران", PhoneLineType.FIXED_LINE)
            else -> Triple("Unknown Iranian Carrier", "اپراتور نامشخص ایرانی", PhoneLineType.UNKNOWN)
        }

        val isValid = formattedNational.length == 11 && (lineType == PhoneLineType.MOBILE || lineType == PhoneLineType.FIXED_LINE)

        return ParsedPhoneNumber(
            rawInput = e164,
            e164Format = e164,
            internationalFormat = "+98 ${national.take(3)} ${national.drop(3).take(3)} ${national.drop(6)}",
            nationalFormat = formattedNational,
            countryCode = "98",
            countryIso2 = "IR",
            countryNameEn = "Iran",
            countryNameFa = "ایران",
            carrierName = carrier,
            carrierNameFa = carrierFa,
            lineType = lineType,
            isValidStructure = isValid,
            isPossibleNumber = formattedNational.length in 10..11,
            areaOrOperatorPrefix = prefix
        )
    }

    private fun parseNorthAmericaNumber(e164: String): ParsedPhoneNumber {
        val national = e164.removePrefix("+1")
        val areaCode = national.take(3)
        val prefix = national.drop(3).take(3)
        val line = national.drop(6)

        // Basic line-type heuristics
        val isVoipArea = areaCode in listOf("800", "888", "877", "866", "855", "844", "833")
        val lineType = if (isVoipArea) PhoneLineType.TOLL_FREE else PhoneLineType.MOBILE

        return ParsedPhoneNumber(
            rawInput = e164,
            e164Format = e164,
            internationalFormat = "+1 ($areaCode) $prefix-$line",
            nationalFormat = "($areaCode) $prefix-$line",
            countryCode = "1",
            countryIso2 = "US",
            countryNameEn = "United States / Canada",
            countryNameFa = "ایالات متحده / کانادا",
            carrierName = if (isVoipArea) "Toll-Free Telephony" else "North American NANPA Carrier",
            carrierNameFa = if (isVoipArea) "شماره رایگان بین‌المللی" else "اپراتور مخابراتی آمریکای شمالی",
            lineType = lineType,
            isValidStructure = national.length == 10,
            isPossibleNumber = national.length == 10,
            areaOrOperatorPrefix = areaCode
        )
    }

    private fun parseUkNumber(e164: String): ParsedPhoneNumber {
        val national = e164.removePrefix("+44")
        val isMobile = national.startsWith("7")
        return ParsedPhoneNumber(
            rawInput = e164,
            e164Format = e164,
            internationalFormat = "+44 $national",
            nationalFormat = "0$national",
            countryCode = "44",
            countryIso2 = "GB",
            countryNameEn = "United Kingdom",
            countryNameFa = "بریتانیا",
            carrierName = if (isMobile) "UK Mobile Network (EE / Vodafone / O2 / Three)" else "BT Fixed Telephony",
            carrierNameFa = if (isMobile) "شبکه همراه بریتانیا" else "تلفن ثابت انگلستان",
            lineType = if (isMobile) PhoneLineType.MOBILE else PhoneLineType.FIXED_LINE,
            isValidStructure = national.length in 10..11,
            isPossibleNumber = true,
            areaOrOperatorPrefix = national.take(4)
        )
    }

    private fun parseUaeNumber(e164: String): ParsedPhoneNumber {
        val national = e164.removePrefix("+971")
        val isMobile = national.startsWith("5")
        val carrier = if (national.startsWith("50") || national.startsWith("54") || national.startsWith("56")) "e& (Etisalat)" else "du Telecom"
        val carrierFa = if (national.startsWith("50") || national.startsWith("54") || national.startsWith("56")) "اتصالات امارات" else "دو تلکام"
        return ParsedPhoneNumber(
            rawInput = e164,
            e164Format = e164,
            internationalFormat = "+971 $national",
            nationalFormat = "0$national",
            countryCode = "971",
            countryIso2 = "AE",
            countryNameEn = "United Arab Emirates",
            countryNameFa = "امارات متحده عربی",
            carrierName = carrier,
            carrierNameFa = carrierFa,
            lineType = if (isMobile) PhoneLineType.MOBILE else PhoneLineType.FIXED_LINE,
            isValidStructure = national.length == 9,
            isPossibleNumber = true,
            areaOrOperatorPrefix = national.take(2)
        )
    }

    private fun parseTurkeyNumber(e164: String): ParsedPhoneNumber {
        val national = e164.removePrefix("+90")
        val prefix = national.take(3)
        val carrier = when (prefix) {
            "530", "531", "532", "533", "534", "535", "536", "537", "538", "539" -> "Turkcell"
            "540", "541", "542", "543", "544", "545", "546", "547", "548", "549" -> "Vodafone Turkey"
            "501", "505", "506", "507", "551", "552", "553", "554", "555", "559" -> "Türk Telekom Mobile"
            else -> "Turkish Telecom Operator"
        }
        return ParsedPhoneNumber(
            rawInput = e164,
            e164Format = e164,
            internationalFormat = "+90 $national",
            nationalFormat = "0$national",
            countryCode = "90",
            countryIso2 = "TR",
            countryNameEn = "Turkey",
            countryNameFa = "ترکیه",
            carrierName = carrier,
            carrierNameFa = carrier,
            lineType = PhoneLineType.MOBILE,
            isValidStructure = national.length == 10,
            isPossibleNumber = true,
            areaOrOperatorPrefix = prefix
        )
    }

    private fun parseGenericE164(e164: String, cc: String, iso: String, nameEn: String, nameFa: String): ParsedPhoneNumber {
        val national = e164.removePrefix("+$cc")
        return ParsedPhoneNumber(
            rawInput = e164,
            e164Format = e164,
            internationalFormat = e164,
            nationalFormat = national,
            countryCode = cc,
            countryIso2 = iso,
            countryNameEn = nameEn,
            countryNameFa = nameFa,
            carrierName = "Standard Telephony Operator",
            carrierNameFa = "اپراتور استاندارد مخابراتی",
            lineType = PhoneLineType.UNKNOWN,
            isValidStructure = e164.length in 8..16,
            isPossibleNumber = true,
            areaOrOperatorPrefix = national.take(3)
        )
    }

    /**
     * Pruning rule: verifies if a generated candidate phone number conforms to real-world carrier allocation tables.
     */
    fun isCandidateNumberValid(candidateE164: String): Boolean {
        val parsed = parse(candidateE164)
        if (!parsed.isValidStructure) return false
        if (parsed.countryIso2 == "IR") {
            val nat = parsed.nationalFormat
            if (!nat.startsWith("09")) return false
            val prefix = nat.take(4)
            val validPrefixes = listOf(
                "0910", "0911", "0912", "0913", "0914", "0915", "0916", "0917", "0918", "0919",
                "0920", "0921", "0922", "0923",
                "0930", "0933", "0935", "0936", "0937", "0938", "0939",
                "0901", "0902", "0903", "0904", "0905",
                "0990", "0991", "0992", "0993", "0994", "0998", "0999"
            )
            return prefix in validPrefixes
        }
        return true
    }
}
