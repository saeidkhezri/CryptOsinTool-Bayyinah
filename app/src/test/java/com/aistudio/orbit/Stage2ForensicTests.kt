package com.aistudio.orbit

import com.aistudio.orbit.forensics.AddressValidator
import com.aistudio.orbit.forensics.currency.CurrencyConverter
import com.aistudio.orbit.model.AddressType
import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.CurrencyPair
import com.aistudio.orbit.model.TxDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage2ForensicTests {

    @Test
    fun testBitcoinAddressValidation() {
        // Legacy P2PKH (1...)
        val btcLegacy = AddressValidator.validate("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa", BlockchainNetwork.BITCOIN)
        assertTrue("BTC Legacy should be valid", btcLegacy.isValid)
        assertEquals(AddressType.BTC_LEGACY_P2PKH, btcLegacy.addressType)

        // P2SH (3...)
        val btcP2sh = AddressValidator.validate("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy", BlockchainNetwork.BITCOIN)
        assertTrue("BTC P2SH should be valid", btcP2sh.isValid)
        assertEquals(AddressType.BTC_P2SH, btcP2sh.addressType)

        // Native SegWit (bc1q...)
        val btcSegwit = AddressValidator.validate("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq", BlockchainNetwork.BITCOIN)
        assertTrue("BTC Native SegWit should be valid", btcSegwit.isValid)
        assertEquals(AddressType.BTC_BECH32_SEGWIT, btcSegwit.addressType)

        // Taproot (bc1p...)
        val btcTaproot = AddressValidator.validate("bc1p5d7rjq7g6rdk2yhzks9s2uma66dn3x5afy9vqdaxy3w7xr9f8wnqf6ujfa", BlockchainNetwork.BITCOIN)
        assertTrue("BTC Taproot should be valid", btcTaproot.isValid)
        assertEquals(AddressType.BTC_TAPROOT, btcTaproot.addressType)
    }

    @Test
    fun testEthereumAndEvmAddressValidation() {
        val ethAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
        val result = AddressValidator.validate(ethAddress, BlockchainNetwork.ETHEREUM)
        assertTrue("EVM address should be valid", result.isValid)
        assertEquals(AddressType.ETH_EVM, result.addressType)
    }

    @Test
    fun testTronAddressValidation() {
        val tronAddress = "TR7NHqJEKQxGTCi8q8ZY4pL8otSzgjLj6t"
        val result = AddressValidator.validate(tronAddress, BlockchainNetwork.TRON)
        assertTrue("TRON Base58 address should be valid", result.isValid)
        assertEquals(AddressType.TRON_BASE58, result.addressType)
    }

    @Test
    fun testInvalidAddressRejection() {
        val invalid = AddressValidator.validate("XYZ12345InvalidBlockchainAddress", BlockchainNetwork.BITCOIN)
        assertFalse("Invalid address should fail validation", invalid.isValid)
        assertEquals(AddressType.UNKNOWN, invalid.addressType)
        assertNotNull(invalid.errorReason)
    }

    @Test
    fun testCurrencyConversionAndTomanCalculation() {
        // 1 BTC (100,000,000 Satoshis)
        val conversion = CurrencyConverter.convertSatoshi(100_000_000L, BlockchainNetwork.BITCOIN)
        assertEquals(1.0, conversion.cryptoAmount, 0.0001)
        assertTrue("USD value should be greater than 0", conversion.usdValue > 0)
        assertTrue("Toman value should be calculated", conversion.tomanValue > 0)

        // Test Toman and Rial formatters
        val formattedToman = CurrencyConverter.formatToman(1000000.0, false)
        assertTrue("Toman format should contain unit", formattedToman.contains("تومان"))
        
        val formattedRial = CurrencyConverter.formatRial(1000000.0, false)
        assertTrue("Rial format should contain unit", formattedRial.contains("ریال"))
    }

    @Test
    fun testTokenAmountConversion() {
        // 500 USDT
        val tokenConv = CurrencyConverter.convertTokenAmount(500.0, "USDT")
        assertEquals(500.0, tokenConv.cryptoAmount, 0.001)
        assertEquals(500.0, tokenConv.usdValue, 0.001)
        assertTrue("Toman equivalent of USDT should be computed", tokenConv.tomanValue > 0)
    }

    @Test
    fun testPersianDateFormatting() {
        val epochMs = 1704067200000L // 2024-01-01 00:00:00 UTC
        val persianDate = PersianDateUtils.formatTimestampToPersian(epochMs, false)
        assertNotNull(persianDate)
        assertTrue("Persian date should not be empty", persianDate.isNotBlank())
    }
}
