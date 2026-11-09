package com.deploydulupulangnanti.adblocalshellpro.adb

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket

// AOSP ADB protocol: https://android.googlesource.com/platform/packages/modules/adb/+/refs/heads/main/protocol.txt
private const val CNXN = 0x4e584e43
private const val STLS = 0x534c5453
private const val OPEN = 0x4e45504f
private const val OKAY = 0x59414b4f
private const val WRTE = 0x45545257
private const val CLSE = 0x45534c43
private const val VERSION = 0x01000001
private const val MAX_PAYLOAD = 1_048_576
private const val LOCAL_ID = 1

internal data class AdbPacket(val command: Int, val arg0: Int, val arg1: Int, val payload: ByteArray)
internal class AdbOutputLimit : IOException("Output limit reached (1 MB). Narrow the command and try again.")

internal class AdbFrames(private val input: DataInputStream, private val output: DataOutputStream) {
    var checksum = false

    fun read(): AdbPacket {
        val command = input.readLeInt()
        val arg0 = input.readLeInt()
        val arg1 = input.readLeInt()
        val length = input.readLeInt()
        val sum = input.readLeInt()
        val magic = input.readLeInt()
        if (magic != command.inv() || length !in 0..MAX_PAYLOAD) throw IOException("Invalid ADB frame.")
        val payload = ByteArray(length).also(input::readFully)
        if (checksum && sum != payload.sumOf { it.toUByte().toInt() }) throw IOException("Invalid ADB checksum.")
        return AdbPacket(command, arg0, arg1, payload)
    }

    fun write(command: Int, arg0: Int, arg1: Int, payload: ByteArray = byteArrayOf()) {
        output.writeLeInt(command)
        output.writeLeInt(arg0)
        output.writeLeInt(arg1)
        output.writeLeInt(payload.size)
        output.writeLeInt(if (checksum) payload.sumOf { it.toUByte().toInt() } else 0)
        output.writeLeInt(command.inv())
        output.write(payload)
        output.flush()
    }
}

/** One authenticated transport and one shell OPEN per command. Closing the socket cancels any I/O. */
internal class AdbWire : AutoCloseable {
    private val socket = Socket()
    val isClosed get() = socket.isClosed

    fun execute(port: Int, command: String, onOutput: (String) -> Unit): Int {
        socket.connect(InetSocketAddress("127.0.0.1", port), 10_000)
        socket.soTimeout = 10_000
        socket.tcpNoDelay = true
        val plain = AdbFrames(DataInputStream(socket.getInputStream()), DataOutputStream(socket.getOutputStream()))
        plain.checksum = true // Before CNXN, the transport starts at protocol version 1.0.
        plain.write(CNXN, VERSION, MAX_PAYLOAD, "host::features=shell_v2\u0000".toByteArray())
        if (plain.read().command != STLS) throw IOException("Use the connection port from Wireless debugging.")
        plain.write(STLS, 0x01000000, 0)
        val tls = AdbTls(socket, AdbIdentity.load()).protocol
        val frames = AdbFrames(DataInputStream(tls.inputStream), DataOutputStream(tls.outputStream))
        val response = frames.read()
        if (response.command != CNXN || response.arg1 <= 0) throw IOException("Wireless debugging did not accept this connection.")
        val features = response.payload.decodeToString().trimEnd('\u0000').substringAfter("::", "")
            .split(';').firstOrNull { it.startsWith("features=") }?.substringAfter('=')?.split(',')
        if (features?.contains("shell_v2") != true) throw IOException("Android did not offer shell protocol v2.")
        frames.checksum = response.arg0 < VERSION
        val quoted = "'" + command.replace("'", "'\\''") + "'"
        val service = "shell,v2,raw:exec /system/bin/sh -c $quoted </dev/null\u0000".toByteArray()
        if (service.size > minOf(MAX_PAYLOAD, response.arg1)) throw IOException("Command is too long for this device.")
        socket.soTimeout = 120_000
        frames.write(OPEN, LOCAL_ID, 0, service)
        val opened = frames.read()
        if (opened.command != OKAY || opened.arg0 == 0 || opened.arg1 != LOCAL_ID) {
            throw IOException("Android did not open the shell stream.")
        }
        val remoteId = opened.arg0
        return readShell(AdbStreamInput(frames, remoteId), onOutput)
    }

    override fun close() {
        runCatching { socket.close() }
    }
}

private class AdbStreamInput(private val frames: AdbFrames, private val remoteId: Int) : InputStream() {
    private var pending = byteArrayOf()
    private var offset = 0
    private var closed = false

    override fun read(): Int = if (fill()) pending[offset++].toUByte().toInt() else -1

    override fun read(target: ByteArray, start: Int, count: Int): Int {
        if (count == 0) return 0
        if (!fill()) return -1
        val size = minOf(count, pending.size - offset)
        pending.copyInto(target, start, offset, offset + size)
        offset += size
        return size
    }

    private fun fill(): Boolean {
        while (!closed && offset == pending.size) {
            val frame = frames.read()
            if (frame.arg0 != remoteId || frame.arg1 != LOCAL_ID) throw IOException("Unexpected ADB stream ID.")
            when (frame.command) {
                WRTE -> {
                    pending = frame.payload
                    offset = 0
                    frames.write(OKAY, LOCAL_ID, remoteId)
                }
                CLSE -> {
                    closed = true
                    frames.write(CLSE, LOCAL_ID, remoteId)
                }
                else -> throw IOException("Unexpected ADB stream frame.")
            }
        }
        return !closed
    }
}

internal fun readShell(stream: InputStream, onOutput: (String) -> Unit): Int {
    val input = DataInputStream(stream)
    val stdout = Utf8Stream()
    val stderr = Utf8Stream()
    var remaining = MAX_PAYLOAD
    while (true) {
        val type = input.read()
        if (type == -1) throw EOFException("Android closed the shell before returning an exit status.")
        val length = input.readLeInt()
        if (type == 3) {
            if (length != 1) throw IOException("Invalid shell exit status.")
            val exit = input.readUnsignedByte()
            onOutput(stdout.decode(byteArrayOf(), true) + stderr.decode(byteArrayOf(), true))
            return exit
        }
        val decoder = when (type) {
            1 -> stdout
            2 -> stderr
            else -> throw IOException("Unexpected shell packet type.")
        }
        if (length < 0) throw IOException("Invalid shell packet size.")
        if (length > remaining) throw AdbOutputLimit()
        remaining -= length
        onOutput(decoder.decode(ByteArray(length).also(input::readFully)))
    }
}

private fun DataInputStream.readLeInt() = Integer.reverseBytes(readInt())
private fun DataOutputStream.writeLeInt(value: Int) = writeInt(Integer.reverseBytes(value))
