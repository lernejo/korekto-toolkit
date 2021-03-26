package com.github.lernejo.korekto.toolkit.misc

import java.util.function.Consumer
import java.util.function.Function

interface ThrowingConsumer<T> {

    @Throws(Exception::class)
    fun accept(t: T?)

    companion object {

        @JvmStatic
        fun <T> sneaky(throwingConsumer: ThrowingConsumer<T>): Consumer<T?> {
            return Consumer<T?>(throwingConsumer::accept)
        }
    }
}

interface ThrowingFunction<I, O> {

    @Throws(Exception::class)
    fun apply(t: I?): O?

    companion object {

        @JvmStatic
        fun <I, O> sneaky(throwingFunction: ThrowingFunction<I, O>): Function<I?, O?> {
            return Function<I?, O?>(throwingFunction::apply)
        }
    }
}
