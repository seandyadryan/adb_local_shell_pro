/*
 * AOSP pairing protocol adapter. SPAKE2 / AES-GCM sequencing adapted from Kadb
 * Copyright (c) 2024 Flyfish-Xu, Apache License 2.0.
 * Changes: bounded, cancellable sockets; app-owned identity; bounded packet reads.
 * Copyright (c) 2026 PT Deploy Dulu Pulang Nanti.
 */
package com.deploydulupulangnanti.adblocalshellpro.adb

import io.github.muntashirakon.crypto.spake2.Spake2Context
import io.github.muntashirakon.crypto.spake2.Spake2Role
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class WirelessPairing : AutoCloseable {
    private val socket = AtomicReference<Socket?>(null)
    @Volatile private var closed = false

    fun pair(port: Int, code: String) {
        check(!closed) { "Pairing was cancelled." }
        val identity = AdbIdentity.load()
        val raw = Socket()
        socket.set(raw)
        checkOpen()
        var spake: Spake2Context? = null
        var secret: ByteArray? = null
        try {
            raw.connect(InetSocketAddress("127.0.0.1", port), 10000)
            raw.soTimeout = 10000
            val tls = AdbTls(raw, identity, pairing = true)
            val material = checkNotNull(tls.pairingMaterial)
            val combined = code.toByteArray() + material
            val message = try {
                spake = Spake2Context(Spake2Role.Alice, "adb pair client\u0000".toByteArray(), "adb pair server\u0000".toByteArray())
                spake.generateMessage(combined)
            } finally {
                combined.fill(0)
                material.fill(0)
            }
            val input = DataInputStream(tls.protocol.inputStream)
            val output = DataOutputStream(tls.protocol.outputStream)
            write(output, 0, message)
            val peerMessage = read(input, 0)
            val keyMaterial = checkNotNull(spake.processMessage(peerMessage)) { "Pairing code was rejected." }
            val derived = ByteArray(16)
            secret = derived
            HKDFBytesGenerator(SHA256Digest()).apply {
                init(HKDFParameters(keyMaterial, null, "adb pairing_auth aes-128-gcm key".toByteArray()))
                generateBytes(derived, 0, 16)
            }
            keyMaterial.fill(0)
            val publicKey = identity.pairingPublicKey()
            val info = ByteArray(8192)
            publicKey.copyInto(info, 1)
            write(output, 1, crypt(Cipher.ENCRYPT_MODE, derived, info))
            val peer = crypt(Cipher.DECRYPT_MODE, derived, read(input, 1))
            check(peer.size == 8192 && peer[0] == 1.toByte()) { "Invalid pairing response. Try a fresh pairing code." }
        } finally {
            secret?.fill(0); spake?.destroy(); close()
        }
    }
    private fun checkOpen() { if (closed) { socket.getAndSet(null)?.close(); error("Pairing was cancelled.") } }
    override fun close() { closed = true; runCatching { socket.getAndSet(null)?.close() } }
    private fun write(out: DataOutputStream, type: Int, payload: ByteArray) {
        out.writeByte(1); out.writeByte(type); out.writeInt(payload.size); out.write(payload); out.flush()
    }
    private fun read(input: DataInputStream, type: Int): ByteArray {
        check(input.readUnsignedByte() == 1 && input.readUnsignedByte() == type) { "Invalid pairing protocol response." }
        val size = input.readInt()
        check(size in 1..16384) { "Invalid pairing packet size." }
        return ByteArray(size).also { input.readFully(it) }
    }
    private fun crypt(mode: Int, key: ByteArray, bytes: ByteArray): ByteArray = Cipher.getInstance("AES/GCM/NoPadding").run {
        init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, ByteArray(12)))
        doFinal(bytes)
    }
}
