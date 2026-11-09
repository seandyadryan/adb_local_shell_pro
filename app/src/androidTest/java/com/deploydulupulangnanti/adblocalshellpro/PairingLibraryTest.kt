package com.deploydulupulangnanti.adblocalshellpro

import io.github.muntashirakon.crypto.spake2.Spake2Context
import io.github.muntashirakon.crypto.spake2.Spake2Role
import org.junit.Assert.*
import org.junit.Test

class PairingLibraryTest {
    @Test fun nativeExchangeAuthenticatesMatchingPasswords() {
        repeat(12) {
            val alice = Spake2Context(Spake2Role.Alice, "adb pair client\u0000".toByteArray(), "adb pair server\u0000".toByteArray())
            val bob = Spake2Context(Spake2Role.Bob, "adb pair server\u0000".toByteArray(), "adb pair client\u0000".toByteArray())
            try {
                val a = alice.generateMessage("012345-exporter-material".toByteArray())
                val b = bob.generateMessage("012345-exporter-material".toByteArray())
                assertEquals(32, a.size); assertEquals(32, b.size)
                assertArrayEquals(alice.processMessage(b), bob.processMessage(a))
            } finally { alice.destroy(); bob.destroy() }
        }
    }
    @Test fun differentPasswordsDoNotAuthenticate() {
        val alice = Spake2Context(Spake2Role.Alice, "client".toByteArray(), "server".toByteArray())
        val bob = Spake2Context(Spake2Role.Bob, "server".toByteArray(), "client".toByteArray())
        try {
            val a = alice.generateMessage("123456".toByteArray())
            val b = bob.generateMessage("654321".toByteArray())
            assertFalse(alice.processMessage(b).contentEquals(bob.processMessage(a)))
        } finally { alice.destroy(); bob.destroy() }
    }
    @Test fun malformedPeerIsRejectedAndCleanupIsIdempotent() {
        val alice = Spake2Context(Spake2Role.Alice, "client".toByteArray(), "server".toByteArray())
        try {
            alice.generateMessage("123456".toByteArray())
            assertTrue(runCatching { alice.processMessage(ByteArray(31)) }.isFailure)
        } finally { alice.destroy(); alice.destroy() }
        assertTrue(alice.isDestroyed)
    }
}
