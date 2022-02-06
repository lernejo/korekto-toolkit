package com.github.lernejo.korekto.toolkit.misc

import java.util.*
import java.util.concurrent.atomic.AtomicInteger

fun interface RandomSupplier {
    fun nextInt(bound: Int): Int

    fun nextBoolean(): Boolean {
        return nextInt(2) > 0
    }

    fun nextBytes(bytes: UByteArray) {
        for (i in bytes.indices) {
            bytes[i] = Integer.valueOf(nextInt(256) - 128).toUByte()
        }
    }

    fun nextUuid(): UUID {
        val data = UByteArray(16)
        nextBytes(data)
        data[6] = data[6] and 15u
        data[6] = data[6] or 64u
        data[8] = data[8] and 63u
        data[8] = data[8] or 128u
        var msb: Long = 0
        var lsb: Long = 0
        for (i in 0..7) msb = msb shl 8 or (data[i] and 255u).toLong()
        for (i in 8..15) lsb = lsb shl 8 or (data[i] and 255u).toLong()
        val mostSigBits = msb
        val leastSigBits = lsb
        return UUID(mostSigBits, leastSigBits)
    }

    companion object {

        @JvmStatic
        fun createRandom(): RandomSupplier {
            val r = Random()
            return RandomSupplier(r::nextInt)
        }

        @JvmStatic
        fun createDeterministic(): RandomSupplier {
            val counter = AtomicInteger()
            return RandomSupplier { i -> counter.incrementAndGet() % i }
        }
    }
}
