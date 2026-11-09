package com.deploydulupulangnanti.adblocalshellpro.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdbEngine {
    private val lock = Any()
    private var endpoint: Int? = null
    private var running: AdbWire? = null
    private var connecting: AdbWire? = null
    private var pairing: WirelessPairing? = null

    suspend fun pair(port: Int, code: String) {
        val session = WirelessPairing()
        synchronized(lock) {
            check(pairing == null) { "Pairing already in progress." }
            pairing = session
        }
        try {
            withContext(Dispatchers.IO) { session.pair(port, code) }
        } finally {
            session.close()
            synchronized(lock) { if (pairing === session) pairing = null }
        }
    }

    suspend fun connect(port: Int): String {
        val wire = synchronized(lock) {
            check(running == null && connecting == null) { "ADB is busy." }
            endpoint = null
            AdbWire().also { connecting = it }
        }
        try {
            val model = withContext(Dispatchers.IO) {
                val output = StringBuilder()
                val exit = wire.execute(port, "getprop ro.product.model") { output.append(it) }
                check(exit == 0) { "Android did not return a device model." }
                output.toString().trim().ifBlank { "Android device" }
            }
            synchronized(lock) {
                check(connecting === wire && !wire.isClosed) { "Connection cancelled." }
                endpoint = port
            }
            return model
        } finally {
            wire.close()
            synchronized(lock) { if (connecting === wire) connecting = null }
        }
    }

    suspend fun execute(command: String, onOutput: (String) -> Unit): Int {
        val (port, wire) = synchronized(lock) {
            check(running == null && connecting == null) { "ADB is busy." }
            val port = checkNotNull(endpoint) { "Connect to Wireless Debugging first." }
            port to AdbWire().also { running = it }
        }
        try {
            return withContext(Dispatchers.IO) { wire.execute(port, command, onOutput) }
        } catch (error: Exception) {
            synchronized(lock) {
                // A deliberate stop closes only this command. A transport failure needs a reconnect.
                if (running === wire && !wire.isClosed && error !is AdbOutputLimit) endpoint = null
            }
            throw error
        } finally {
            wire.close()
            synchronized(lock) { if (running === wire) running = null }
        }
    }

    fun stop() = synchronized(lock) { running?.close(); Unit }
    fun cancelPairing() = synchronized(lock) { pairing?.close(); Unit }
    fun cancelConnect() = synchronized(lock) { connecting?.close(); Unit }

    fun disconnect() = synchronized(lock) {
        endpoint = null
        running?.close()
        connecting?.close()
        pairing?.close()
    }

    fun connected(): Boolean = synchronized(lock) { endpoint != null }
}
