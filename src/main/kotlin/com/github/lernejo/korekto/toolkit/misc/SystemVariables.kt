package com.github.lernejo.korekto.toolkit.misc

object SystemVariables {

    @JvmStatic
    operator fun get(name: String): String? {
        return System.getProperty(name) ?: System.getenv(name)
    }
}
