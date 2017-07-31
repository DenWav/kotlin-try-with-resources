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

package com.demonwav.kotlinutil.test

import com.demonwav.kotlinutil.using
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("using {} tests")
class UsingTest {
    @Test
    @DisplayName("Simple using {} test")
    fun testBasicUsing() {
        val queue = using {
            val connection = getConnection().autoClose()
            val statement = connection.prepareStatement().autoClose()
            val rs = statement.executeQuery().autoClose()
            rs.next()
        }.manager.resourceQueue

        for (closeable in queue) {
            assertTrue((closeable as? Closed)?.closed == true)
        }
    }

    @Test
    @DisplayName("Close all with catch test")
    fun testWithCatch() {
        val queue = (using {
            val connection = getConnection().autoClose()
            val statement = connection.prepareStatement().autoClose()
            val rs = statement.executeQuery().autoClose()
            rs.next()
        } catch { _: Exception ->
        }).manager.resourceQueue

        for (closeable in queue) {
            assertTrue((closeable as? Closed)?.closed == true)
        }
    }

    @Test
    @DisplayName("Close all with caught exception test")
    fun testWithCaught() {
        val queue = (using {
            val connection = getConnection().autoClose()
            val statement = connection.prepareStatement().autoClose()
            val rs = statement.executeQuery().autoClose()
            rs.next()
            throw SuperSecretException
        } catch { _: SuperSecretException ->
        }).manager.resourceQueue

        for (closeable in queue) {
            assertTrue((closeable as? Closed)?.closed == true)
        }
    }

    @Test
    @DisplayName("Close all with uncaught exception test")
    fun testWithUncaught() {
        val catcher = (using {
            val connection = getConnection().autoClose()
            val statement = connection.prepareStatement().autoClose()
            val rs = statement.executeQuery().autoClose()
            rs.next()
            throw SuperSecretException
        } catch { _: AnotherSecretException ->
        })

        assertThrows(SuperSecretException::class.java) {
            catcher finally {}
        }

        for (closeable in catcher.manager.resourceQueue) {
            assertTrue((closeable as? Closed)?.closed == true)
        }
    }

    @Test
    @DisplayName("Exception in using {} block test")
    fun testExceptionInBlock() {
        assertThrows(SuperSecretException::class.java) {
            using {
                val connection = getConnection().autoClose()
                val statement = connection.prepareStatement().autoClose()
                val rs = statement.executeQuery().autoClose()
                rs.next()
                throw SuperSecretException
            } finally {}
        }
    }

    @Test
    @DisplayName("Exception in using {} block with catch test")
    fun testCatch() {
        using {
            val connection = getConnection().autoClose()
            val statement = connection.prepareStatement().autoClose()
            val rs = statement.executeQuery().autoClose()
            rs.next()
            throw SuperSecretException
        } catch { _: SuperSecretException ->
        } finally {}
    }

    @Test
    @DisplayName("Exception in using {} block with catch of different type test")
    fun testCatchWrongType() {
        assertThrows(SuperSecretException::class.java) {
            using {
                val connection = getConnection().autoClose()
                val statement = connection.prepareStatement().autoClose()
                val rs = statement.executeQuery().autoClose()
                rs.next()
                throw SuperSecretException
            } catch { _: RuntimeException ->
            } finally {}
        }
    }

    @Test
    @DisplayName("Exception in using {} block with catch of super type test")
    fun testCatchSuperType() {
        using {
            val connection = getConnection().autoClose()
            val statement = connection.prepareStatement().autoClose()
            val rs = statement.executeQuery().autoClose()
            rs.next()
            throw SuperSecretException
        } catch { _: Exception ->
        } finally {}
    }

    @Test
    @DisplayName("Exception in using {} block with multi-catch test")
    fun testMultiCatch() {
        using {
            val connection = getConnection().autoClose()
            val statement = connection.prepareStatement().autoClose()
            val rs = statement.executeQuery().autoClose()
            rs.next()
            throw SuperSecretException
        } catch { _: RuntimeException ->
        } catch { _: NumberFormatException ->
        } catch { _: SuperSecretException ->
        } finally {}
    }

    @Test
    @DisplayName("Exception in using {} block with multi-catch super type test")
    fun testMultiCatchSuper() {
        using {
            val connection = getConnection().autoClose()
            val statement = connection.prepareStatement().autoClose()
            val rs = statement.executeQuery().autoClose()
            rs.next()
            throw SuperSecretException
        } catch { _: RuntimeException ->
        } catch { _: NumberFormatException ->
        } catch { _: Exception ->
        } finally {}
    }

    @Test
    @DisplayName("Exception in using {} block with multi-catch wrong type test")
    fun testMultiCatchWrongType() {
        assertThrows(SuperSecretException::class.java) {
            using {
                val connection = getConnection().autoClose()
                val statement = connection.prepareStatement().autoClose()
                val rs = statement.executeQuery().autoClose()
                rs.next()
                throw SuperSecretException
            } catch { _: RuntimeException ->
            } catch { _: NumberFormatException ->
            } catch { _: ArrayIndexOutOfBoundsException ->
            } finally {}
        }
    }

    @Test
    @DisplayName("Exception in catch {} block test")
    fun testExceptionInCatch() {
        assertThrows(AnotherSecretException::class.java) {
            using {
                val connection = getConnection().autoClose()
                val statement = connection.prepareStatement().autoClose()
                val rs = statement.executeQuery().autoClose()
                rs.next()
                throw SuperSecretException
            } catch { _: SuperSecretException ->
                throw AnotherSecretException
            } finally {}
        }
    }

    @Test
    @DisplayName("Exception in catch {} block with another catch block of the same type test")
    fun testExceptionInCatchWithSameTypeCatch() {
        assertThrows(AnotherSecretException::class.java) {
            using {
                val connection = getConnection().autoClose()
                val statement = connection.prepareStatement().autoClose()
                val rs = statement.executeQuery().autoClose()
                rs.next()
                throw SuperSecretException
            } catch { _: SuperSecretException ->
                throw AnotherSecretException
            } catch { _: AnotherSecretException ->
            } finally {}
        }
    }

    @Test
    @DisplayName("Exception in catch {} block with multi-catch test")
    fun testExceptionInCatchWithMultiCatch() {
        assertThrows(AnotherSecretException::class.java) {
            using {
                val connection = getConnection().autoClose()
                val statement = connection.prepareStatement().autoClose()
                val rs = statement.executeQuery().autoClose()
                rs.next()
                throw SuperSecretException
            } catch { _: RuntimeException ->
            } catch { _: NumberFormatException ->
            } catch { _: SuperSecretException ->
                throw AnotherSecretException
            } finally {}
        }
    }

    @Test
    @DisplayName("Exception in catch {} block with multi-catch super type test")
    fun testExceptionInCatchWithMultiCatchSuperType() {
        assertThrows(AnotherSecretException::class.java) {
            using {
                val connection = getConnection().autoClose()
                val statement = connection.prepareStatement().autoClose()
                val rs = statement.executeQuery().autoClose()
                rs.next()
                throw SuperSecretException
            } catch { _: RuntimeException ->
            } catch { _: NumberFormatException ->
            } catch { _: Exception ->
                throw AnotherSecretException
            } finally {}
        }
    }

    @Test
    @DisplayName("Exception in finally {} block test")
    fun testExceptionInFinally() {
        assertThrows(AnotherSecretException::class.java) {
            using {
                val connection = getConnection().autoClose()
                val statement = connection.prepareStatement().autoClose()
                val rs = statement.executeQuery().autoClose()
                rs.next()
                throw SuperSecretException
            } catch { _: SuperSecretException ->
            } finally {
                throw AnotherSecretException
            }
        }
    }

    @Test
    @DisplayName("Exception in finally {} block with multi-catch test")
    fun testExceptionInFinallyWithMutliCatch() {
        assertThrows(AnotherSecretException::class.java) {
            using {
                val connection = getConnection().autoClose()
                val statement = connection.prepareStatement().autoClose()
                val rs = statement.executeQuery().autoClose()
                rs.next()
                throw SuperSecretException
            } catch { _: RuntimeException ->
            } catch { _: SuperSecretException ->
            } finally {
                throw AnotherSecretException
            }
        }
    }

    @Test
    @DisplayName("finally {} block with no exception test")
    fun testNoExceptionFinally() {
        var b = false
        using {
            val connection = getConnection().autoClose()
            val statement = connection.prepareStatement().autoClose()
            val rs = statement.executeQuery().autoClose()
            rs.next()
        } finally {
            b = true
        }

        assertTrue(b)
    }

    @Test
    @DisplayName("Exception in close() test")
    fun testExceptionInClose() {
        assertThrows(SuperSecretException::class.java) {
            using {
                CloseWithException().autoClose()
            } finally {}
        }
    }

    @Test
    @DisplayName("Caught exception in close() test")
    fun testExceptionInCloseCaught() {
        using {
            CloseWithException().autoClose()
        } catch { _: SuperSecretException ->
        } finally {}
    }

    private object SuperSecretException : Exception()
    private object AnotherSecretException: Exception()

    // Below is a simple mock-up of the general SQL resource path in Java, for visual purposes
    fun getConnection(): Connection {
        return Connection()
    }

    open class Closed : AutoCloseable {
        var closed = false
        override fun close() {
            closed = true
        }
    }

    class Connection : Closed() {
        fun prepareStatement(): PreparedStatement {
            return PreparedStatement()
        }
    }

    class PreparedStatement : Closed() {
        fun executeQuery(): ResultSet {
            return ResultSet()
        }
    }

    class ResultSet : Closed() {
        fun next() {}
    }

    class CloseWithException : AutoCloseable {
        override fun close() {
            throw SuperSecretException
        }
    }
}
