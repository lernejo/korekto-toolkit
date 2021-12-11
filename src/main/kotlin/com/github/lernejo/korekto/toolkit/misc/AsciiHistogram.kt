package com.github.lernejo.korekto.toolkit.misc

import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.roundToInt

class AsciiHistogram {
    fun asciiHistogram(values: List<Double?>): String {
        val occurrencesByGrade: MutableMap<Int, AtomicInteger> = TreeMap()
        values.forEach(Consumer { value: Double? ->
            occurrencesByGrade.computeIfAbsent(
                (value!!).roundToInt()
            ) { AtomicInteger() }.incrementAndGet()
        })
        val maxOccurrences =
            occurrencesByGrade.values.stream().mapToInt { ai: AtomicInteger -> ai.get() }.max().orElse(0)
        val maxGrade = values.stream().mapToInt { d: Double? -> (d!!).roundToInt() }.max().orElse(0)
        if (maxOccurrences == 0) {
            return "no results."
        }
        val columnWidth = max(maxOccurrences.toString().length, maxGrade.toString().length) + 1
        val histogramBuilder = StringBuilder()
        for (i in 10 downTo 0) {
            histogramBuilder.append(String.format("%1$" + 2 + "s", (i * 10 * maxOccurrences) / values.size))
                .append("% │")
            val previousUpPercentage = (i + 1) * 0.1
            val upPercentage = i * 0.1
            val lowPercentage = (i - 1) * 0.1
            for (valueColumn in 0..maxGrade) {
                val occurrences = occurrencesByGrade.getOrDefault(valueColumn, AtomicInteger())
                if (occurrences.get() > lowPercentage * maxOccurrences) {
                    if (occurrences.get() <= upPercentage * maxOccurrences) {
                        histogramBuilder.append(pad(occurrences.get().toString(), columnWidth))
                    } else if (occurrences.get() <= previousUpPercentage * maxOccurrences) {
                        histogramBuilder.append(pad("┬", columnWidth))
                    } else {
                        histogramBuilder.append(pad("║", columnWidth))
                    }
                } else {
                    histogramBuilder.append(" ".repeat(columnWidth))
                }
            }
            histogramBuilder.append('\n')
        }
        histogramBuilder.append("    └")
        for (valueColumn in 0..maxGrade) {
            histogramBuilder.append("─".repeat(columnWidth))
        }
        histogramBuilder.append('\n')
        histogramBuilder.append("     ")
        for (valueColumn in 0..maxGrade) {
            histogramBuilder.append(pad(valueColumn.toString(), columnWidth))
        }
        return histogramBuilder.toString()
    }

    private fun pad(value: String, columnWidth: Int) =
        if (value.length > 2) value else (if (value.length > 1) " ".repeat(columnWidth - 2) + value else " ".repeat(
            columnWidth - 1
        ) + value)
}
