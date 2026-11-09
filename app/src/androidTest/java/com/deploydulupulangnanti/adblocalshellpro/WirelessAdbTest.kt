package com.deploydulupulangnanti.adblocalshellpro

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.deploydulupulangnanti.adblocalshellpro.adb.AdbEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WirelessAdbTest {
    @Test fun pairsAndConnectsToRealWirelessDebugging() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.containsKey("pairPort"))
        val engine = AdbEngine()
        try {
            engine.pair(args.getString("pairPort")!!.toInt(), args.getString("pairCode")!!)
            assertTrue(engine.connect(args.getString("connectPort")!!.toInt()).isNotBlank())
            val output = StringBuilder()
            assertEquals(0, engine.execute("id") { output.append(it) })
            assertTrue(output.toString().contains("uid=2000"))
        } finally { engine.disconnect() }
    }
}
