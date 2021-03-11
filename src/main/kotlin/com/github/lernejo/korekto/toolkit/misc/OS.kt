package com.github.lernejo.korekto.toolkit.misc

import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.util.Locale

enum class OS {
    LINUX, MAC, WINDOWS, OTHER;

    val isCurrentOs: Boolean
        get() = this == CURRENT_OS

    companion object {
        private val logger = LoggerFactory.getLogger(OS::class.java)
        private val CURRENT_OS = determineCurrentOs()
        private fun determineCurrentOs(): OS? {
            var osName = System.getProperty("os.name")
            if (StringUtils.isBlank(osName)) {
                logger.debug("JVM system property 'os.name' is undefined. It is therefore not possible to detect the current OS.")

                // null signals that the current OS is "unknown"
                return null
            }
            osName = osName.toLowerCase(Locale.ENGLISH)
            if (osName.contains("linux")) {
                return LINUX
            }
            if (osName.contains("mac")) {
                return MAC
            }
            return if (osName.contains("win")) {
                WINDOWS
            } else OTHER
        }
    }
}
