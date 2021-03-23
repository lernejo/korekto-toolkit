package com.github.lernejo.korekto.toolkit.thirdparty.maven.invoker

import java.util.concurrent.atomic.AtomicReference
import kotlin.jvm.Volatile
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.Throws

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */ /**
 * Read from an InputStream and write the output to an OutputStream.
 *
 * @author [Trygve Laugstl](mailto:trygvis@inamo.no)
 */
internal class StreamFeeder(input: InputStream?, output: OutputStream?) : AbstractStreamHandler() {
    private val input: AtomicReference<InputStream?>
    private val output: AtomicReference<OutputStream?>

    /**
     * @since 3.2.0
     */
    @Volatile
    var exception: Throwable? = null
        private set

    override fun run() {
        try {
            feed()
        } catch (e: Throwable) {
            // Catch everything so the streams will be closed and flagged as done.
            if (exception != null) {
                exception = e
            }
        } finally {
            close()
            synchronized(this) { condition.signalAll() }
        }
    }

    fun close() {
        setDone()
        val `is` = input.getAndSet(null)
        if (`is` != null) {
            try {
                `is`.close()
            } catch (ex: IOException) {
                if (exception != null) {
                    exception = ex
                }
            }
        }
        val os = output.getAndSet(null)
        if (os != null) {
            try {
                os.close()
            } catch (ex: IOException) {
                if (exception != null) {
                    exception = ex
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun feed() {
        val `is` = input.get()
        val os = output.get()
        var flush = false
        if (`is` != null && os != null) {
            var data: Int = -1
            while (!isDone && `is`.read().also { data = it } != -1) {
                if (!isDisabled) {
                    os.write(data)
                    flush = true
                }
            }
            if (flush) {
                os.flush()
            }
        }
    }

    /**
     * Create a new StreamFeeder
     *
     * @param input Stream to read from
     * @param output Stream to write to
     */
    init {
        this.input = AtomicReference(input)
        this.output = AtomicReference(output)
    }
}
