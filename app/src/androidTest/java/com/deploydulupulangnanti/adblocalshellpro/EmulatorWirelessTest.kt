package com.deploydulupulangnanti.adblocalshellpro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry
import com.deploydulupulangnanti.adblocalshellpro.adb.AdbEngine
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** System-service fixture for isolated emulators, independent of Settings UI speed.
 * Shell privileges configure the OS fixture only, and are dropped before app code runs.
 * Protocol source: AOSP AdbDebuggingManager / android.debug.IAdbManager.
 */
class EmulatorWirelessTest {
    @Test fun realSystemPairingAndShell() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("wirelessFixture") == "true")
        val inst = InstrumentationRegistry.getInstrumentation()
        val ui = inst.uiAutomation
        fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(ui.executeShellCommand(command)).bufferedReader().use { it.readText().trim() }
        check(Build.VERSION.SDK_INT == 36 && shell("getprop ro.boot.qemu") == "1" &&
            shell("getprop ro.adb.secure") == "1") { "This fixture requires a secure API 36 emulator." }
        val context = inst.targetContext
        // Android 16 IAdbManager.aidl transaction order. These shell calls prepare
        // the emulator only. The app uses its own unprivileged sockets afterwards.
        fun call(transaction: Int, arguments: String = "") = shell("service call adb $transaction $arguments")
        fun wirelessPort(): Int? {
            val words = Regex("\\b[0-9a-fA-F]{8}\\b").findAll(call(10)).map { it.value }.toList()
            return words.getOrNull(1)?.toLongOrNull(16)?.toInt()?.takeIf { it in 1..65535 }
        }
        val pairingCode = CompletableDeferred<String>()
        val pairingPort = CompletableDeferred<Int>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent) {
                intent.getStringExtra("pairing_code")?.let { pairingCode.complete(it) }
                val port = intent.getIntExtra("adb_port", -1)
                if (port > 0) pairingPort.complete(port)
            }
        }
        val engine = AdbEngine()
        ui.adoptShellPermissionIdentity()
        var registered = false
        try {
            Settings.Global.putInt(context.contentResolver, "development_settings_enabled", 1)
            val bssidPattern = Regex("BSSID: ([0-9a-fA-F:]{17})")
            val bssid = withTimeout(30000) {
                var address = bssidPattern.find(shell("cmd wifi status"))?.groupValues?.get(1)
                while (address == null) {
                    delay(500)
                    address = bssidPattern.find(shell("cmd wifi status"))?.groupValues?.get(1)
                }
                address
            }
            call(4, "i32 1 s16 $bssid")
            Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 1)
            val filter = IntentFilter("com.android.server.adb.WIRELESS_DEBUG_PAIRING_RESULT")
            if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
            registered = true
            call(11)
            call(8)
            val code = withTimeout(30000) { pairingCode.await() }
            val port = withTimeout(30000) { pairingPort.await() }
            val tlsPort = withTimeout(20000) {
                var discovered = wirelessPort()
                while (discovered == null) {
                    delay(200)
                    discovered = wirelessPort()
                }
                discovered
            }
            ui.dropShellPermissionIdentity()
            assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission("android.permission.MANAGE_DEBUGGING"))
            // These are the exact production client methods, without adopted privileges.
            engine.pair(port, code)
            delay(500) // Android persists authorization asynchronously after pairing.
            assertTrue(engine.connect(tlsPort).isNotBlank())
            val output = StringBuilder()
            assertEquals(0, engine.execute("id") { output.append(it) })
            assertTrue(output.toString().contains("uid=2000"))
            inst.sendStatus(2, Bundle().apply {
                putString("adbTransport", "PAIRED_TLS")
                putInt("adbTlsPort", tlsPort)
            })
        } finally {
            engine.disconnect()
            ui.adoptShellPermissionIdentity()
            runCatching { call(11) }
            if (registered) context.unregisterReceiver(receiver)
            ui.dropShellPermissionIdentity()
        }
    }
}
