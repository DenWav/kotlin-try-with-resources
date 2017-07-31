/*
 * Copyright 2017 Kyle Wood (DemonWav)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.demonwav.kotlinutil

import java.util.concurrent.ConcurrentLinkedQueue


inline fun using(crossinline block: ResourceManager.() -> Unit): Catcher {
    val manager = ResourceManager()
    try {
        manager.use(block)
    } catch (t: Throwable) {
        manager.t = t
    }
    return manager.getCatcher()
}

class ResourceManager : AutoCloseable {
    var t: Throwable? = null
    val resourceQueue = ConcurrentLinkedQueue<AutoCloseable>()

    fun <T: AutoCloseable> T.autoClose(): T {
        resourceQueue.offer(this)
        return this
    }

    override fun close() {
        for (closeable in resourceQueue) {
            try {
                closeable.close()
            } catch (t: Throwable) {
                if (this.t == null) {
                    this.t = t
                } else {
                    this.t!!.addSuppressed(t)
                }
            }
        }
    }

    fun getCatcher(): Catcher {
        return Catcher(this)
    }
}

class Catcher(val manager: ResourceManager) {
    var t: Throwable? = null
    var thrown: Throwable? = null

    init {
        t = manager.t
    }

    inline infix fun <reified T : Throwable> catch(block: (T) -> Unit): Catcher {
        if (t is T) {
            try {
                block(t as T)
            } catch (thrown: Throwable) {
                this.thrown = thrown
            } finally {
                // It's been caught, so set it to null
                t = null
            }
        }
        return this
    }

    inline infix fun finally(block: () -> Unit) {
        try {
            block()
        } catch (thrown: Throwable) {
            if (t == null) {
                // we've caught the exception, or none was thrown
                if (this.thrown == null) {
                    // No exception was thrown in the catch blocks
                    throw thrown
                } else {
                    // An exception was thrown in the catch block
                    this.thrown!!.let {
                        it.addSuppressed(thrown)
                        throw it
                    }
                }
            } else {
                // We never caught the exception
                // So therefore this.thrown is also null
                t!!.let {
                    it.addSuppressed(thrown)
                    throw it
                }
            }
        }

        // At this point the finally block did not thrown an exception
        // We need to see if there are still any exceptions left to throw
        t?.let { t ->
            thrown?.let { t.addSuppressed(it) }
            throw t
        }
        thrown?.let { throw it }

    }
}
