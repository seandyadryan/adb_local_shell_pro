package com.deploydulupulangnanti.adblocalshellpro.adb

import android.util.AtomicFile
import com.deploydulupulangnanti.adblocalshellpro.ShellApplication
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileNotFoundException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import java.util.Date
import java.util.concurrent.TimeUnit

internal class AdbIdentity private constructor(val privateKey: RSAPrivateCrtKey) {
    private val publicKey = KeyFactory.getInstance("RSA").generatePublic(
        RSAPublicKeySpec(privateKey.modulus, privateKey.publicExponent)
    )

    val certificate: ByteArray = run {
        val name = X500Name("CN=ADB Local Shell PRO")
        val now = System.currentTimeMillis()
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(privateKey)
        JcaX509v3CertificateBuilder(
            name, BigInteger(128, SecureRandom()).setBit(0),
            Date(now - TimeUnit.DAYS.toMillis(1)), Date(now + TimeUnit.DAYS.toMillis(365)),
            name, publicKey
        ).build(signer).encoded
    }

    fun pairingPublicKey(): ByteArray {
        val modulus = privateKey.modulus
        val n0 = modulus.modInverse(BigInteger.ONE.shiftLeft(32)).negate().toInt()
        fun little(value: BigInteger) = value.toByteArray().reversedArray().copyOf(256)
        val payload = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(64).putInt(n0).put(little(modulus))
            .put(little(BigInteger.ONE.shiftLeft(4096).mod(modulus)))
            .putInt(privateKey.publicExponent.toInt()).array()
        return (Base64.getEncoder().encodeToString(payload) + " ADBLocalShellPro\u0000").toByteArray(Charsets.US_ASCII)
    }

    companion object {
        private var cached: AdbIdentity? = null

        @Synchronized fun load(): AdbIdentity = cached ?: readIdentity().also { cached = it }

        private fun readIdentity(): AdbIdentity {
            // Reuse the PKCS#8 PEM from v1.0.0 so existing system pairings remain valid.
            val store = AtomicFile(File(ShellApplication.identityPath))
            val pem = try { store.readFully() } catch (_: FileNotFoundException) { null }
            val key = if (pem == null) {
                KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair().private
            } else {
                val encoded = pem.toString(Charsets.US_ASCII)
                    .removePrefix("-----BEGIN PRIVATE KEY-----")
                    .trim().removeSuffix("-----END PRIVATE KEY-----").filterNot(Char::isWhitespace)
                KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)))
            }
            check(key is RSAPrivateCrtKey && key.modulus.bitLength() == 2048) { "Invalid local ADB identity." }
            val identity = AdbIdentity(key)
            if (pem == null) {
                val data = "-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(key.encoded) +
                    "\n-----END PRIVATE KEY-----\n"
                val file = store.startWrite()
                try {
                    file.write(data.toByteArray(Charsets.US_ASCII))
                    store.finishWrite(file)
                } catch (error: Exception) {
                    store.failWrite(file)
                    throw error
                }
            }
            return identity
        }
    }
}
