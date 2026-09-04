package com.aistudio.orbit.forensics

import com.aistudio.orbit.model.AddressType
import com.aistudio.orbit.model.AddressValidationResult
import com.aistudio.orbit.model.BlockchainNetwork

object AddressValidator {

    private val BTC_LEGACY_REGEX = Regex("^[1][a-km-zA-HJ-NP-Z1-9]{25,34}$")
    private val BTC_P2SH_REGEX = Regex("^[3][a-km-zA-HJ-NP-Z1-9]{25,34}$")
    private val BTC_BECH32_SEGWIT_REGEX = Regex("^(bc1q|tb1q)[0-9ac-hj-np-z]{38,59}$", RegexOption.IGNORE_CASE)
    private val BTC_TAPROOT_REGEX = Regex("^(bc1p|tb1p)[0-9ac-hj-np-z]{58,62}$", RegexOption.IGNORE_CASE)
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

        // 1. Check Bitcoin formats
        if (BTC_BECH32_SEGWIT_REGEX.matches(trimmed)) {
            return AddressValidationResult(
                isValid = true,
                network = BlockchainNetwork.BITCOIN,
                addressType = AddressType.BTC_BECH32_SEGWIT,
                formattedAddress = trimmed.lowercase()
            )
        }

        if (BTC_TAPROOT_REGEX.matches(trimmed)) {
            return AddressValidationResult(
                isValid = true,
                network = BlockchainNetwork.BITCOIN,
                addressType = AddressType.BTC_TAPROOT,
                formattedAddress = trimmed.lowercase()
            )
        }

        if (BTC_P2SH_REGEX.matches(trimmed)) {
            return AddressValidationResult(
                isValid = true,
                network = BlockchainNetwork.BITCOIN,
                addressType = AddressType.BTC_P2SH,
                formattedAddress = trimmed
            )
        }

        if (BTC_LEGACY_REGEX.matches(trimmed)) {
            return AddressValidationResult(
                isValid = true,
                network = BlockchainNetwork.BITCOIN,
                addressType = AddressType.BTC_LEGACY_P2PKH,
                formattedAddress = trimmed
            )
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
        if (TRON_BASE58_REGEX.matches(trimmed)) {
            return AddressValidationResult(
                isValid = true,
                network = BlockchainNetwork.TRON,
                addressType = AddressType.TRON_BASE58,
                formattedAddress = trimmed
            )
        }

        // 4. Check Solana format
        if (SOLANA_BASE58_REGEX.matches(trimmed) && trimmed.length >= 32 && trimmed.length <= 44 && !trimmed.startsWith("1") && !trimmed.startsWith("3")) {
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
            errorReason = "Unrecognized or invalid blockchain address format / قالب آدرس بلاکچین ناشناخته یا نامعتبر است"
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
