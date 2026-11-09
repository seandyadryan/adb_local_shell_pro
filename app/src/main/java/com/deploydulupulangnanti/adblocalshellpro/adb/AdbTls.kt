package com.deploydulupulangnanti.adblocalshellpro.adb

import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters
import org.bouncycastle.tls.AlertDescription
import org.bouncycastle.tls.Certificate
import org.bouncycastle.tls.CertificateEntry
import org.bouncycastle.tls.CertificateRequest
import org.bouncycastle.tls.DefaultTlsClient
import org.bouncycastle.tls.ProtocolVersion
import org.bouncycastle.tls.SignatureAndHashAlgorithm
import org.bouncycastle.tls.TlsAuthentication
import org.bouncycastle.tls.TlsClientProtocol
import org.bouncycastle.tls.TlsFatalAlert
import org.bouncycastle.tls.TlsServerCertificate
import org.bouncycastle.tls.crypto.TlsCryptoParameters
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto
import java.net.Socket
import java.security.SecureRandom

/** TLS restricted to the app's loopback ADB sockets. No Android provider internals. */
internal class AdbTls(socket: Socket, identity: AdbIdentity, pairing: Boolean = false) {
    val protocol = TlsClientProtocol(socket.getInputStream(), socket.getOutputStream())
    private val bcCrypto = BcTlsCrypto(SecureRandom())
    var pairingMaterial: ByteArray? = null
        private set

    init {
        protocol.connect(object : DefaultTlsClient(bcCrypto) {
            override fun getSupportedVersions() = arrayOf(ProtocolVersion.TLSv13)

            override fun notifyHandshakeComplete() {
                super.notifyHandshakeComplete()
                if (pairing) pairingMaterial = context.exportKeyingMaterial("adb-label\u0000", null, 64)
            }

            override fun getAuthentication(): TlsAuthentication = object : TlsAuthentication {
                override fun notifyServerCertificate(server: TlsServerCertificate) {
                    // adbd uses a self-signed certificate. Pairing authenticates its TLS transcript
                    // with SPAKE2. The command port is chosen from the local Android Settings screen.
                    if (server.certificate.isEmpty) throw TlsFatalAlert(AlertDescription.bad_certificate)
                }

                override fun getClientCredentials(request: CertificateRequest): BcDefaultTlsCredentialedSigner? {
                    val allowed = request.supportedSignatureAlgorithms ?: return null
                    val signature = listOf(
                        SignatureAndHashAlgorithm.rsa_pss_rsae_sha256,
                        SignatureAndHashAlgorithm.rsa_pss_rsae_sha384,
                        SignatureAndHashAlgorithm.rsa_pss_rsae_sha512
                    ).firstOrNull(allowed::contains) ?: return null
                    val key = identity.privateKey
                    val params = RSAPrivateCrtKeyParameters(
                        key.modulus, key.publicExponent, key.privateExponent,
                        key.primeP, key.primeQ, key.primeExponentP, key.primeExponentQ, key.crtCoefficient
                    )
                    val certificate = Certificate(request.certificateRequestContext, arrayOf(
                        CertificateEntry(bcCrypto.createCertificate(identity.certificate), null)
                    ))
                    return BcDefaultTlsCredentialedSigner(TlsCryptoParameters(context), bcCrypto, params, certificate, signature)
                }
            }
        })
    }
}
