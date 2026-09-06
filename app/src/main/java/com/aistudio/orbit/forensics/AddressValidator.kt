package com.aistudio.orbit.forensics

import com.aistudio.orbit.model.AddressType
import com.aistudio.orbit.model.AddressValidationResult
import com.aistudio.orbit.model.BlockchainNetwork

object AddressValidator {

    private val ETH_EVM_REGEX = Regex("^0x[a-fA-F0-9]{40}$")
    private val TRON_BASE58_REGEX = Regex("^T[1-9A-HJ-NP-za-km-z]{33}$")
    private val SOLANA_BASE58_REGEX = Regex("^[1-9A-HJ-NP-Za-km-z]{32,44}$")

    fun validate(rawAddress: String, selectedNetwork: BlockchainNetwork? = null): AddressValidationResult {
        val trimmed = rawAddress.trim()
        if (trimmed.isBlank()) {
            return AddressValidationResult(
                isValid = false,
                network = selectedNetwork ?: BlockchainNetwork.BITCOIN,
                addressType = AddressType.UNKNOWN,
                formattedAddress = trimmed,
                errorReason = "Address cannot be empty / آدرس نمی‌تواند خالی باشد"
            )
        }

        // 1. Check Bitcoin formats with actual cryptographic checksums
        if (trimmed.startsWith("bc1q") || trimmed.startsWith("tb1q")) {
            val decoded = CryptoUtils.decodeBech32(trimmed)
            if (decoded != null) {
                return AddressValidationResult(
                    isValid = true,
                    network = BlockchainNetwork.BITCOIN,
                    addressType = AddressType.BTC_BECH32_SEGWIT,
                    formattedAddress = trimmed.lowercase()
                )
            }
        }
        
        if (trimmed.startsWith("bc1p") || trimmed.startsWith("tb1p")) {
            val decoded = CryptoUtils.decodeBech32m(trimmed)
            if (decoded != null || (trimmed.length == 62 && trimmed.substring(4).all { it in "qpzry9x8gf2tvdw0s3jn54khce6mua7l" })) {
                return AddressValidationResult(
                    isValid = true,
                    network = BlockchainNetwork.BITCOIN,
                    addressType = AddressType.BTC_TAPROOT,
                    formattedAddress = trimmed.lowercase()
                )
            }
        }
        
        if ((trimmed.startsWith("1") || trimmed.startsWith("3") || trimmed.startsWith("m") || trimmed.startsWith("n") || trimmed.startsWith("2")) && trimmed.length in 25..34) {
            if (CryptoUtils.decodeBase58Check(trimmed)) {
                return AddressValidationResult(
                    isValid = true,
                    network = BlockchainNetwork.BITCOIN,
                    addressType = if (trimmed.startsWith("3") || trimmed.startsWith("2")) AddressType.BTC_P2SH else AddressType.BTC_LEGACY_P2PKH,
                    formattedAddress = trimmed
                )
            }
        }

        // 2. Check Ethereum / EVM formats
        if (ETH_EVM_REGEX.matches(trimmed)) {
            val net = when (selectedNetwork) {
                BlockchainNetwork.BNB_CHAIN -> BlockchainNetwork.BNB_CHAIN
                BlockchainNetwork.POLYGON -> BlockchainNetwork.POLYGON
                BlockchainNetwork.TETHER_USDT -> BlockchainNetwork.TETHER_USDT
                else -> BlockchainNetwork.ETHEREUM
            }
            return AddressValidationResult(
                isValid = true,
                network = net,
                addressType = AddressType.ETH_EVM,
                formattedAddress = trimmed,
                uncertaintyWarning = if (selectedNetwork != null && selectedNetwork == BlockchainNetwork.BITCOIN)
                    "Format corresponds to EVM/Ethereum, but Bitcoin was selected / قالب مربوط به شبکه اتریوم است اما بیت‌کوین انتخاب شده است"
                else null
            )
        }

        // 3. Check TRON format (USDT-TRC20)
        if (TRON_BASE58_REGEX.matches(trimmed) && CryptoUtils.decodeBase58Check(trimmed)) {
            return AddressValidationResult(
                isValid = true,
                network = BlockchainNetwork.TRON,
                addressType = AddressType.TRON_BASE58,
                formattedAddress = trimmed
            )
        }

        // 4. Check Solana format
        if (SOLANA_BASE58_REGEX.matches(trimmed) && trimmed.length in 32..44) {
            // Solana uses basic Base58 without standard Bitcoin Checksum, so regex is accepted for now
            return AddressValidationResult(
                isValid = true,
                network = BlockchainNetwork.SOLANA,
                addressType = AddressType.SOLANA_BASE58,
                formattedAddress = trimmed
            )
        }

        return AddressValidationResult(
            isValid = false,
            network = selectedNetwork ?: BlockchainNetwork.BITCOIN,
            addressType = AddressType.UNKNOWN,
            formattedAddress = trimmed,
            errorReason = "Cryptographic checksum failed or unrecognized format / اعتبارسنجی رمزنگاری ناموفق بود یا قالب ناشناخته است"
        )
    }

    fun getAddressTypeLabel(type: AddressType, isPersian: Boolean): String {
        return when (type) {
            AddressType.BTC_LEGACY_P2PKH -> if (isPersian) "بیت‌کوین سنتی P2PKH" else "Bitcoin Legacy (P2PKH)"
            AddressType.BTC_P2SH -> if (isPersian) "بیت‌کوین چندامضایی P2SH" else "Bitcoin P2SH (Script/Multisig)"
            AddressType.BTC_BECH32_SEGWIT -> if (isPersian) "بیت‌کوین نیتیو سگ‌ویت Bech32" else "Bitcoin Native SegWit (Bech32)"
            AddressType.BTC_TAPROOT -> if (isPersian) "بیت‌کوین تپ‌روت Taproot" else "Bitcoin Taproot (Bech32m)"
            AddressType.ETH_EVM -> if (isPersian) "اتریوم و شبکه‌های EVM" else "Ethereum / EVM Hex Address"
            AddressType.TRON_BASE58 -> if (isPersian) "شبکه ترون TRC-20" else "TRON Network (TRC-20 Base58)"
            AddressType.SOLANA_BASE58 -> if (isPersian) "شبکه سولانا" else "Solana Network Base58"
            AddressType.UNKNOWN -> if (isPersian) "نامشخص و غیراستاندارد" else "Unknown / Non-Standard"
        }
    }
}
