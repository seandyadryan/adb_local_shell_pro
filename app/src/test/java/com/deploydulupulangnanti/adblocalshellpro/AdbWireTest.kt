package com.deploydulupulangnanti.adblocalshellpro

import com.deploydulupulangnanti.adblocalshellpro.adb.AdbFrames
import com.deploydulupulangnanti.adblocalshellpro.adb.readShell
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AdbWireTest {
    @Test fun rejectsOversizedOrCorruptFramesBeforeReadingPayload() {
        val command = 0x45545257
        fun read(length: Int, magic: Int) {
            val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(command).putInt(9).putInt(1).putInt(length).putInt(0).putInt(magic).array()
            AdbFrames(DataInputStream(header.inputStream()), DataOutputStream(ByteArrayOutputStream())).read()
        }
        assertThrows(IOException::class.java) { read(1_048_577, command.inv()) }
        assertThrows(IOException::class.java) { read(-1, command.inv()) }
        assertThrows(IOException::class.java) { read(0, command) }
    }

    @Test fun splitUtf8AndStderrKeepTheirOwnDecodersAndReturnExitStatus() {
        val utf8 = "界".toByteArray()
        val bytes = packet(1, utf8.copyOfRange(0, 1)) + packet(2, "error ".toByteArray()) +
            packet(1, utf8.copyOfRange(1, utf8.size)) + packet(3, byteArrayOf(7))
        val output = StringBuilder()
        assertEquals(7, readShell(bytes.inputStream()) { output.append(it) })
        assertEquals("error 界", output.toString())
    }

    @Test fun exitPacketDoesNotCountAgainstOutputBudget() {
        val bytes = packet(1, ByteArray(1_048_576) { 'a'.code.toByte() }) + packet(3, byteArrayOf(0))
        var length = 0
        assertEquals(0, readShell(bytes.inputStream()) { length += it.length })
        assertEquals(1_048_576, length)
        assertThrows(EOFException::class.java) { readShell(packet(1, byteArrayOf(42)).inputStream()) {} }
        assertThrows(IOException::class.java) { readShell(packet(3, byteArrayOf()).inputStream()) {} }
    }

    private fun packet(type: Int, data: ByteArray): ByteArray =
        ByteBuffer.allocate(5 + data.size).order(ByteOrder.LITTLE_ENDIAN)
            .put(type.toByte()).putInt(data.size).put(data).array()
}
