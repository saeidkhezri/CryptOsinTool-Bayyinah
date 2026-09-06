package com.aistudio.orbit.forensics

import org.junit.Test
import org.junit.Assert.*

class TestCrypto {
    @Test
    fun testBase58() {
        val btcSegwit = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"
        val lower = btcSegwit.lowercase()
        val pos = lower.lastIndexOf('1')
        val hrp = lower.substring(0, pos)
        val dataStr = lower.substring(pos + 1)
        val data = ByteArray(dataStr.length)
        for (i in dataStr.indices) {
            val c = dataStr[i]
            val value = "qpzry9x8gf2tvdw0s3jn54khce6mua7l".indexOf(c)
            data[i] = value.toByte()
        }
        val decoded1 = convertBits(data.copyOfRange(0, data.size - 6), 5, 8, false)
        val decoded2 = convertBits(data.copyOfRange(1, data.size - 6), 5, 8, false)
        println("convertBits from 0: $decoded1")
        println("convertBits from 1: $decoded2")
    }

    private fun convertBits(data: ByteArray, fromBits: Int, toBits: Int, pad: Boolean): ByteArray? {
        var acc = 0
        var bits = 0
        val maxv = (1 shl toBits) - 1
        val max_acc = (1 shl (fromBits + toBits - 1)) - 1
        val ret = mutableListOf<Byte>()
        for (value in data) {
            val v = value.toInt() and 0xff
            if (v ushr fromBits != 0) return null
            acc = ((acc shl fromBits) or v) and max_acc
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                ret.add(((acc ushr bits) and maxv).toByte())
            }
        }
        if (pad) {
            if (bits > 0) ret.add(((acc shl (toBits - bits)) and maxv).toByte())
        } else if (bits >= fromBits || ((acc shl (toBits - bits)) and maxv) != 0) {
            return null
        }
        return ret.toByteArray()
    }
}
