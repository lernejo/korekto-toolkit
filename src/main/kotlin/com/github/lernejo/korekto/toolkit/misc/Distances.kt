package com.github.lernejo.korekto.toolkit.misc

import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.math.min

object Distances {
    private fun minimum(a: Int, b: Int, c: Int): Int {
        return min(min(a, b), c)
    }

    fun levenshteinDistance(lhs: CharSequence?, rhs: CharSequence?): Int {
        val distance = Array(lhs!!.length + 1) {
            IntArray(
                rhs!!.length + 1
            )
        }
        for (i in 0..lhs.length) distance[i][0] = i
        for (j in 1..rhs!!.length) distance[0][j] = j
        for (i in 1..lhs.length) for (j in 1..rhs.length) distance[i][j] = minimum(
            distance[i - 1][j] + 1,
            distance[i][j - 1] + 1,
            distance[i - 1][j - 1] + if (lhs[i - 1] == rhs[j - 1]) 0 else 1
        )
        return distance[lhs.length][rhs.length]
    }

    fun isWordsIncludedApprox(left: String, right: String, wordLevenshteinDistance: Int): Boolean {
        val leftWords = words(left).collect(Collectors.toList())
        val rightWords = words(right).collect(Collectors.toList())
        val shorterList = if (rightWords.size > leftWords.size) leftWords else rightWords
        val longerList: MutableList<String?> = if (rightWords.size > leftWords.size) rightWords else leftWords
        for (shortListWord in shorterList) {
            if (!contains(longerList, shortListWord, wordLevenshteinDistance)) {
                return false
            }
        }
        return true
    }

    private fun contains(words: MutableList<String?>, match: String?, maxLevenshteinDistance: Int): Boolean {
        var contained = false
        var matched: String? = null
        for (word in words) {
            if (levenshteinDistance(word, match) <= maxLevenshteinDistance) {
                contained = true
                matched = word
                break
            }
        }
        if (contained) {
            words.remove(matched)
        }
        return contained
    }

    fun countWords(text: String): Int {
        return words(text).count().toInt()
    }

    private fun words(sentence: String): Stream<String?> {
        return Arrays.stream(sentence.split("\\s+".toRegex()).toTypedArray())
            .filter { s: String? -> s!!.trim { it <= ' ' }.isNotEmpty() }
    }

    fun longestCommonSubSequence(a: String, b: String): Int {
        return lCSubStr(a.toCharArray(), b.toCharArray(), a.length, b.length)
    }

    private fun lCSubStr(X: CharArray, Y: CharArray, m: Int, n: Int): Int {
        // Create a table to store lengths of longest common suffixes of 
        // substrings. Note that LCSuff[i][j] contains length of longest 
        // common suffix of X[0..i-1] and Y[0..j-1]. The first row and 
        // first column entries have no logical meaning, they are used only 
        // for simplicity of program 
        val lCStuff = Array(m + 1) { IntArray(n + 1) }
        var result = 0 // To store length of the longest common substring 

        // Following steps build LCSuff[m+1][n+1] in bottom up fashion 
        for (i in 0..m) {
            for (j in 0..n) {
                if (i == 0 || j == 0) lCStuff[i][j] = 0 else if (X[i - 1] == Y[j - 1]) {
                    lCStuff[i][j] = lCStuff[i - 1][j - 1] + 1
                    result = Integer.max(result, lCStuff[i][j])
                } else lCStuff[i][j] = 0
            }
        }
        return result
    }
}
