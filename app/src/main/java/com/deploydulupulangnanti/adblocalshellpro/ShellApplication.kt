package com.deploydulupulangnanti.adblocalshellpro

import android.app.Application
import java.io.File

class ShellApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Keep startup light. The ADB crypto provider is initialized on an I/O
        // worker only when a user starts pairing or connecting.
        identityPath = File(noBackupFilesDir, "adb-identity.pem").path
    }
    companion object {
        lateinit var identityPath: String
            private set
    }
}
