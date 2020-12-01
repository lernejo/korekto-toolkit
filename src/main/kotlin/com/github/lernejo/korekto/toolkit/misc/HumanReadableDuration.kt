package com.github.lernejo.korekto.toolkit.misc

object HumanReadableDuration {
    private const val MS_IN_SECOND = 1000
    private const val MS_IN_MINUTE = MS_IN_SECOND * 60
    private const val MS_IN_HOUR = MS_IN_MINUTE * 60

    @JvmStatic
    fun toString(ms: Long): String {
        var rest = ms
        val hours = rest / MS_IN_HOUR
        rest -= hours * MS_IN_HOUR
        val minutes = rest / MS_IN_MINUTE
        rest -= minutes * MS_IN_MINUTE
        val seconds = rest / MS_IN_SECOND
        rest -= seconds * MS_IN_SECOND
        var nbOfUnitsUsed = 0
        val sb = StringBuilder()
        if (hours > 0) {
            sb.append(hours).append("h ")
            nbOfUnitsUsed++
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ")
            nbOfUnitsUsed++
        }
        if (seconds > 0 && nbOfUnitsUsed < 2) {
            sb.append(seconds).append("s ")
            nbOfUnitsUsed++
        }
        if (rest > 0 && nbOfUnitsUsed < 2) {
            sb.append(rest).append("ms")
        }
        val lastPosition = sb.length - 1
        if (lastPosition > -1 && sb.get(lastPosition) == ' ') {
            sb.deleteCharAt(lastPosition)
        }
        return sb.toString()
    }
}
