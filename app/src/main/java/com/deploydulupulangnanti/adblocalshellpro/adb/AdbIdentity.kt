package com.deploydulupulangnanti.adblocalshellpro.adb

import com.deploydulupulangnanti.adblocalshellpro.ShellApplication
import com.flyfishxu.kadb.cert.KadbCert
import com.flyfishxu.kadb.cert.OkioFilePrivateKeyStore
import okio.Path.Companion.toPath

internal object AdbIdentity {
    private var configured = false
    @Synchronized fun ensureConfigured() {
        if (!configured) {
            KadbCert.configure(OkioFilePrivateKeyStore(ShellApplication.identityPath.toPath()))
            configured = true
        }
    }
}
