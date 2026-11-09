package com.deploydulupulangnanti.adblocalshellpro.adb

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction

/** Preserves Unicode characters when a shell packet ends partway through UTF-8. */
class Utf8Stream {
    private val decoder = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE)
    private var pending = byteArrayOf()
    fun decode(bytes: ByteArray, end: Boolean = false): String {
        val input = ByteBuffer.wrap(pending + bytes)
        val chars = CharBuffer.allocate(input.remaining() + 4)
        decoder.decode(input, chars, end)
        pending = ByteArray(input.remaining()).also { input.get(it) }
        if (end) decoder.flush(chars)
        return chars.flip().toString()
    }
}
