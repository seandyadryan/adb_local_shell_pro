package com.deploydulupulangnanti.adblocalshellpro.adb

import com.flyfishxu.kadb.Kadb
import com.flyfishxu.kadb.shell.AdbShellPacket
import com.flyfishxu.kadb.shell.AdbShellStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

class AdbEngine {
    @Volatile private var client: Kadb? = null
    private val activeStream = AtomicReference<AdbShellStream?>(null)
    @Volatile private var stopped = false
    @Volatile private var connecting: Kadb? = null
    @Volatile private var pairing: WirelessPairing? = null

    suspend fun pair(port: Int, code: String) = withContext(Dispatchers.IO) {
        AdbIdentity.ensureConfigured()
        WirelessPairing().use { session ->
            pairing = session
            try { session.pair(port, code) } finally { pairing = null }
        }
    }
    suspend fun connect(port: Int): String = withContext(Dispatchers.IO) {
        AdbIdentity.ensureConfigured()
        disconnect()
        val candidate = Kadb.create("127.0.0.1", port, connectTimeout = 10000, socketTimeout = 120000)
        connecting = candidate
        try {
            val identity = candidate.shell("id")
            check(identity.exitCode == 0) { "The device did not return a shell identity." }
            val model = candidate.shell("getprop ro.product.model").output.trim()
            client = candidate
            model.ifBlank { "Android device" }
        } catch (e: Exception) { candidate.close(); throw e }
        finally { connecting = null }
    }
    suspend fun execute(command: String, onOutput: (String) -> Unit): Int {
        stopped = false
        return withContext(Dispatchers.IO) {
            val connection = checkNotNull(client) { "Connect to Wireless Debugging first." }
            val stream = SingleAttemptShell.open(connection, command)
            activeStream.set(stream)
            try {
                check(!stopped) { "Command stopped." }
                val stdout = Utf8Stream()
                val stderr = Utf8Stream()
                var bytes = 0
                while (true) {
                    val packet = stream.read()
                    if (packet !is AdbShellPacket.Exit) bytes += packet.payload.size
                    check(bytes <= 1_048_576) { "Output limit reached (1 MB). Narrow the command and try again." }
                    when (packet) {
                        is AdbShellPacket.StdOut -> onOutput(stdout.decode(packet.payload))
                        is AdbShellPacket.StdError -> onOutput(stderr.decode(packet.payload))
                        is AdbShellPacket.Exit -> {
                            onOutput(stdout.decode(byteArrayOf(), true) + stderr.decode(byteArrayOf(), true))
                            return@withContext packet.payload[0].toUByte().toInt()
                        }
                    }
                }
                @Suppress("UNREACHABLE_CODE") 0
            } finally {
                if (activeStream.compareAndSet(stream, null)) closeStream(stream)
            }
        }
    }
    private fun closeStream(stream: AdbShellStream) {
        try {
            stream.close()
        } catch (_: IOException) {
            // A closed transport cannot receive a final CLSE frame.
        }
    }
    fun stop() { stopped = true; activeStream.getAndSet(null)?.let(::closeStream) }
    fun cancelPairing() { pairing?.close() }
    fun cancelConnect() { connecting?.close() }
    fun disconnect() { stop(); cancelPairing(); connecting?.close(); connecting = null; client?.close(); client = null }
    fun connected(): Boolean = client?.connectionCheck() == true
}
