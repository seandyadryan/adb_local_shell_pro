/*
 * AOSP pairing protocol adapter. SPAKE2 / AES-GCM sequencing adapted from Kadb
 * Copyright (c) 2024 Flyfish-Xu, Apache License 2.0.
 * Changes: bounded, cancellable sockets; app-owned identity; bounded packet reads.
 * Copyright (c) 2026 PT Deploy Dulu Pulang Nanti.
 */
package com.deploydulupulangnanti.adblocalshellpro.adb

import android.annotation.SuppressLint
import io.github.muntashirakon.crypto.spake2.Spake2Context
import io.github.muntashirakon.crypto.spake2.Spake2Role
import com.flyfishxu.kadb.cert.KadbCert
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.DataInputStream
import java.io.DataOutputStream
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.*

internal class WirelessPairing : AutoCloseable {
    private val socket = AtomicReference<Socket?>(null)
    @Volatile private var closed = false

    @SuppressLint("PrivateApi", "CustomX509TrustManager", "TrustAllX509TrustManager")
    fun pair(port: Int, code: String) {
        check(!closed) { "Pairing was cancelled." }
        val identity = KadbCert.ensureReady()
        val cert = CertificateFactory.getInstance("X.509").generateCertificate(identity.certificatePem.inputStream()) as X509Certificate
        val pem = identity.privateKeyPem.toString(Charsets.US_ASCII).replace(Regex("-----[^-]+-----|\\s"), "")
        val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)))
        val password = CharArray(0)
        val store = KeyStore.getInstance("PKCS12").apply { load(null, password); setKeyEntry("adb", key, password, arrayOf(cert)) }
        val managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply { init(store, password) }.keyManagers
        // Android's pairing server has a self-signed certificate. SPAKE2 + the
        // TLS exporter binds authentication to the user-confirmed pairing code.
        val trust = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers() = emptyArray<X509Certificate>()
        }
        val ssl = SSLContext.getInstance("TLSv1.3").apply { init(managers, arrayOf(trust), null) }
        val raw = Socket()
        socket.set(raw)
        checkOpen()
        var spake: Spake2Context? = null
        var secret: ByteArray? = null
        try {
            raw.connect(InetSocketAddress("127.0.0.1", port), 10000)
            raw.soTimeout = 10000
            val tls = ssl.socketFactory.createSocket(raw, "127.0.0.1", port, true) as SSLSocket
            socket.set(tls); checkOpen()
            tls.soTimeout = 10000; tls.enabledProtocols = arrayOf("TLSv1.3"); tls.startHandshake()
            HiddenApiBypass.addHiddenApiExemptions("Lcom/android/org/conscrypt/Conscrypt;")
            val conscrypt = Class.forName("com.android.org.conscrypt.Conscrypt")
            val exporter = conscrypt.getMethod("exportKeyingMaterial", SSLSocket::class.java, String::class.java, ByteArray::class.java, Int::class.javaPrimitiveType)
            val material = exporter.invoke(null, tls, "adb-label\u0000", null, 64) as ByteArray
            val combined = code.toByteArray() + material
            spake = Spake2Context(Spake2Role.Alice, "adb pair client\u0000".toByteArray(), "adb pair server\u0000".toByteArray())
            val message = spake.generateMessage(combined)
            combined.fill(0); material.fill(0)
            val input = DataInputStream(tls.inputStream); val output = DataOutputStream(tls.outputStream)
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
            val publicKey = encodePublicKey(cert.publicKey as RSAPublicKey)
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
    private fun encodePublicKey(key: RSAPublicKey): ByteArray {
        check(key.modulus.bitLength() == 2048)
        val n = key.modulus
        val n0 = n.modInverse(BigInteger.ONE.shiftLeft(32)).negate().toInt()
        fun little(value: BigInteger): ByteArray = value.toByteArray().reversedArray().copyOf(256)
        val payload = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN).putInt(64).putInt(n0)
            .put(little(n)).put(little(BigInteger.ONE.shiftLeft(4096).mod(n))).putInt(key.publicExponent.toInt()).array()
        return (Base64.getEncoder().encodeToString(payload) + " ADBLocalShellPro\u0000").toByteArray(Charsets.US_ASCII)
    }
}
