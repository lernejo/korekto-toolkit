package com.github.lernejo.korekto.toolkit.misc

import java.beans.IntrospectionException
import java.beans.Introspector
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.net.HttpURLConnection
import java.net.URL

class Equalator(private val permissiveness: Int) {

    @SuppressWarnings("unchecked")
    fun <T> equals(o1: T?, o2: T?): Boolean {
        if (o1 === o2) {
            return true
        }
        if (o1 == null || o2 == null) {
            return false
        }
        when (o1) {
            is String -> {
                return compareCharSequence(o1.toLowerCase(), (o2 as String).toLowerCase())
            }
            is Comparable<*> -> {
                return compareComparable<Any>(o1 as Comparable<Any>, o2)
            }
            is MutableCollection<*> -> {
                return compareCollection(o1 as Collection<Any>, o2 as Collection<Any>)
            }
            is Boolean -> {
                return compareBoolean(o1, o2 as Boolean)
            }
            else -> return try {
                val beanInfo = Introspector.getBeanInfo(o1.javaClass)
                var equals = true
                for (propDesc in beanInfo.propertyDescriptors) {
                    if ("class" == propDesc.name) {
                        continue
                    }
                    val readMethod = propDesc.readMethod
                    val val1 = readMethod.invoke(o1)
                    val val2 = readMethod.invoke(o2)
                    equals = equals && equals(val1, val2)
                }
                equals
            } catch (e: IntrospectionException) {
                throw RuntimeException(e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            } catch (e: InvocationTargetException) {
                throw RuntimeException(e)
            }
        }
    }

    private fun compareCollection(o1: Collection<Any>, o2: Collection<Any>): Boolean {
        if (o1.size != o2.size) {
            return false
        }
        val it1 = o1.iterator()
        val it2 = o2.iterator()
        for (i in o1.indices) {
            val item1 = it1.next()
            val item2 = it2.next()
            if (!equals(item1, item2)) {
                return false
            }
        }
        return true
    }

    private fun compareBoolean(val1: Boolean, val2: Boolean): Boolean {
        return val1 == val2
    }

    private fun compareCharSequence(val1: CharSequence, val2: CharSequence): Boolean {
        if (Distances.levenshteinDistance(val1, val2) <= permissiveness) {
            return true
        }
        if (val1.toString().startsWith("http") && val2.toString().startsWith("http")) {
            try {
                val resolvedUrl1 = resolveUrl(val1.toString())
                val resolvedUrl2 = resolveUrl(val2.toString())
                if (resolvedUrl1 == resolvedUrl2) {
                    return true
                }
            } catch (e: IOException) {
                // continue
            }
        }
        return false
    }

    @Throws(IOException::class)
    private fun resolveUrl(rawUrl: String): String {
        val url = URL(rawUrl)
        val con1 = url.openConnection() as HttpURLConnection
        con1.requestMethod = "GET"
        con1.responseCode
        var resolvedUrl = con1.url.toString()
        if (resolvedUrl.endsWith("/")) {
            resolvedUrl = resolvedUrl.substring(0, resolvedUrl.length - 1)
        }
        return resolvedUrl
    }

    private fun <T> compareComparable(val1: Comparable<T>, val2: T): Boolean {
        return val1.compareTo(val2) == 0
    }
}
