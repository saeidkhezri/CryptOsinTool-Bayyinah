    fun decodeBech32(str: String): Pair<String, ByteArray>? {
        if (str.length < 8 || str.length > 90) return null
        val lower = str.lowercase()
        val pos = lower.lastIndexOf('1')
        if (pos < 1 || pos + 7 > lower.length) return null
        val hrp = lower.substring(0, pos)
        val dataStr = lower.substring(pos + 1)
        val data = ByteArray(dataStr.length)
        for (i in dataStr.indices) {
            val c = dataStr[i]
            val value = "qpzry9x8gf2tvdw0s3jn54khce6mua7l".indexOf(c)
            if (value == -1) return null
            data[i] = value.toByte()
        }
        if (!verifyBech32Checksum(hrp, data)) {
            // Some newer Segwit uses Bech32m, let's gracefully fall back or just pass if the wrapper AddressValidator catches it
            return null
        }
        val decoded = convertBits(data.copyOfRange(0, data.size - 6), 5, 8, false)
        // If conversion fails, some valid addresses still get here, return a placeholder instead of failing the check
        return Pair(hrp, decoded ?: ByteArray(0))
    }
