package com.deploydulupulangnanti.adblocalshellpro

import com.deploydulupulangnanti.adblocalshellpro.adb.Utf8Stream
import com.deploydulupulangnanti.adblocalshellpro.data.InputRules
import com.deploydulupulangnanti.adblocalshellpro.data.TextLimits
import org.junit.Assert.*
import org.junit.Test

class CoreTest {
    @Test fun portsRejectInvalidValues() {
        listOf("", "0", "65536", "-1", "5555;id", "1.2", "abc").forEach { assertNull(InputRules.port(it)) }
        assertEquals(1, InputRules.port("1")); assertEquals(65535, InputRules.port("65535")); assertEquals(5555, InputRules.port(" 5555 "))
    }
    @Test fun pairingCodeIsExactlySixAsciiDigits() {
        assertTrue(InputRules.pairingCode("012345"))
        listOf("12345", "1234567", "abcdef", "１２３４５６").forEach { assertFalse(InputRules.pairingCode(it)) }
    }
    @Test fun commandValidationPreservesShellSyntax() {
        assertTrue(InputRules.command("cd /data/local/tmp && printf '%s' hello"))
        assertFalse(InputRules.command("  ")); assertFalse(InputRules.command("id\u0000reboot")); assertFalse(InputRules.command("a".repeat(8193)))
    }
    @Test fun changingCommandsRequireReview() {
        listOf("rm -rf /data/local/tmp/test", "settings put system screen_brightness 50", "pm uninstall org.example", "echo test; reboot", "su -c id").forEach { assertTrue(it, InputRules.requiresConfirmation(it)) }
        listOf("id", "dumpsys battery", "getprop ro.product.model", "df -h /data", "pm list packages -3").forEach { assertFalse(it, InputRules.requiresConfirmation(it)) }
    }
    @Test fun unicodeSurvivesEveryPacketBoundary() {
        val expected = "Hello 世界 👋 café مرحبا\n"
        val bytes = expected.toByteArray()
        for (split in 0..bytes.size) {
            val decoder = Utf8Stream()
            val actual = decoder.decode(bytes.copyOfRange(0, split)) + decoder.decode(bytes.copyOfRange(split, bytes.size), true)
            assertEquals("split $split", expected, actual)
        }
    }
    @Test fun malformedUtf8IsReplacedWithoutCrashing() {
        assertEquals("\uFFFD", Utf8Stream().decode(byteArrayOf(0xF0.toByte()), true))
    }
    @Test fun byteLimitsPreserveWholeUnicodeCharacters() {
        assertEquals("字💻", TextLimits.tail("a汉字💻", 8))
        assertEquals("", TextLimits.tail("💻", 3))
        for (limit in 0..20) {
            val tail = TextLimits.tail("English العربية 世界 💻", limit)
            assertTrue(tail.toByteArray().size <= limit)
            assertFalse(tail.contains('\uFFFD'))
        }
    }
}
