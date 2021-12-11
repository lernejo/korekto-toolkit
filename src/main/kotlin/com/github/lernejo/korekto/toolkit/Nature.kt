package com.github.lernejo.korekto.toolkit

import java.util.*
import java.util.function.Consumer

interface Nature<CONTEXT : NatureContext> {
    fun <RESULT> withContext(action: (CONTEXT) -> RESULT): RESULT

    @JvmName("inContextK")
    private fun inContext(action: (CONTEXT) -> Unit) = withContext<Unit> {
        action(it)
        null
    }

    fun inContext(action: Consumer<CONTEXT>) = withContext { c -> action.accept(c) }

    fun close() {

    }
}

interface NatureContext

interface NatureFactory {
    fun getNature(exercise: Exercise): Optional<Nature<*>>

    companion object {
        @JvmStatic
        fun lookupNatures(exercise: Exercise): Map<Class<out Nature<*>>, Nature<*>> {
            val serviceLoader = ServiceLoader.load(NatureFactory::class.java)
            return serviceLoader.map { it.getNature(exercise) }
                .filter { it.isPresent }
                .map { it.get() }
                .map { p -> Pair(p.javaClass, p) }
                .toMap()
        }
    }
}
