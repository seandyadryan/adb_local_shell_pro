package com.deploydulupulangnanti.adblocalshellpro

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.deploydulupulangnanti.adblocalshellpro.adb.AdbEngine
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Optional live integration: adb tcpip 5555, then pass -e liveAdb true to the test runner. */
@RunWith(AndroidJUnit4::class)
class LocalAdbTest {
    @Test fun realShellExitUnicodeAndCancellation() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveAdb") == "true")
        val engine = AdbEngine()
        try {
            val port = InstrumentationRegistry.getArguments().getString("connectPort", "5555").toInt()
            assertTrue(engine.connect(port).isNotBlank())
            val output = StringBuilder()
            assertEquals(0, engine.execute("id") { output.append(it) })
            assertTrue(output.toString().contains("uid="))
            output.clear()
            assertEquals(7, engine.execute("printf 'Hello 世界'; exit 7") { output.append(it) })
            assertEquals("Hello 世界", output.toString())
            // Fast exits must not lose queued output; stdin is EOF immediately.
            repeat(12) { iteration ->
                output.clear()
                assertEquals(0, engine.execute("printf 'fast-$iteration'") { output.append(it) })
                assertEquals("fast-$iteration", output.toString())
            }
            output.clear()
            assertEquals(0, engine.execute("cat; printf '%s' \"a'b\"") { output.append(it) })
            assertEquals("a'b", output.toString())
            val work = async(Dispatchers.IO) { runCatching { engine.execute("sleep 60") {} } }
            delay(1000); engine.stop()
            withTimeout(5000) { assertTrue(work.await().isFailure) }
            output.clear()
            assertEquals(0, engine.execute("echo ready") { output.append(it) })
            assertEquals("ready", output.toString().trim())
        } finally { engine.disconnect() }
    }
}
