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
        if (!verifyBech32Checksum(hrp, data)) return null
        // Fix: Bech32 data contains the version byte at index 0, so convertBits should start from 1
        val decoded = convertBits(data.copyOfRange(1, data.size - 6), 5, 8, false) ?: return null
        return Pair(hrp, decoded)
    }
        
    fun decodeBech32m(str: String): Pair<String, ByteArray>? {
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
        if (!verifyBech32mChecksum(hrp, data)) return null
        // Fix: Bech32m data contains the version byte at index 0, so convertBits should start from 1
        val decoded = convertBits(data.copyOfRange(1, data.size - 6), 5, 8, false) ?: return null
        return Pair(hrp, decoded)
    }
