package com.github.lernejo.korekto.toolkit.misc

import kotlin.math.pow

object Maths {

    @JvmStatic
    fun round(value: Double, precision: Int): Double {
        val scale = 10.0.pow(precision.toDouble()).toInt()
        return Math.round(value * scale).toDouble() / scale
    }
}
