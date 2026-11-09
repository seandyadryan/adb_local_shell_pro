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
import org.lsposed.hiddenapibypass.HiddenApiBypass

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
        check(shell("getprop ro.boot.qemu") == "1" || shell("getprop ro.kernel.qemu") == "1") { "This fixture is restricted to emulators." }
        val context = inst.targetContext
        HiddenApiBypass.addHiddenApiExemptions("Landroid/debug/")
        val stub = Class.forName("android.debug.IAdbManager\$Stub")
        fun call(name: String, arguments: String = ""): String {
            val field = stub.getDeclaredField("TRANSACTION_$name").apply { isAccessible = true }
            // SELinux permits the shell to reach this system service; delegating
            // Android permissions alone does not change an app's SELinux domain.
            return shell("service call adb ${field.getInt(null)} $arguments")
        }
        fun wirelessPort(): Int? {
            val words = Regex("\\b[0-9a-fA-F]{8}\\b").findAll(call("getAdbWirelessPort")).map { it.value }.toList()
            return words.getOrNull(1)?.toLongOrNull(16)?.toInt()?.takeIf { it in 1..65535 }
        }
        val constants = Class.forName("android.debug.AdbManager")
        fun key(name: String) = constants.getField(name).get(null) as String
        val codeKey = key("WIRELESS_PAIRING_CODE_EXTRA")
        val portKey = key("WIRELESS_DEBUG_PORT_EXTRA")
        val pairingCode = CompletableDeferred<String>()
        val pairingPort = CompletableDeferred<Int>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent) {
                intent.getStringExtra(codeKey)?.let { pairingCode.complete(it) }
                val port = intent.getIntExtra(portKey, -1)
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
            call("allowWirelessDebugging", "i32 1 s16 $bssid")
            Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 1)
            val filter = IntentFilter(key("WIRELESS_DEBUG_PAIRING_RESULT_ACTION"))
            if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
            registered = true
            call("disablePairing")
            call("enablePairingByPairingCode")
            val code = withTimeout(30000) { pairingCode.await() }
            val port = withTimeout(30000) { pairingPort.await() }
            val secure = shell("getprop ro.adb.secure") == "1"
            var tlsPort = wirelessPort()
            if (secure) withTimeout(20000) {
                while (tlsPort == null || tlsPort!! <= 0) {
                    delay(200)
                    tlsPort = wirelessPort()
                }
            }
            ui.dropShellPermissionIdentity()
            assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission("android.permission.MANAGE_DEBUGGING"))
            // These are the exact production client methods, without adopted privileges.
            engine.pair(port, code)
            delay(500) // Android persists authorization asynchronously after pairing.
            assertTrue(engine.connect(tlsPort ?: 5555).isNotBlank())
            val output = StringBuilder()
            assertEquals(0, engine.execute("id") { output.append(it) })
            assertTrue(output.toString().contains("uid=2000"))
            inst.sendStatus(2, Bundle().apply {
                putString("adbTransport", if (secure) "PAIRED_TLS" else "INSECURE_EMULATOR_TCP")
                putInt("adbTlsPort", tlsPort ?: 5555)
            })
        } finally {
            engine.disconnect()
            ui.adoptShellPermissionIdentity()
            runCatching { call("disablePairing") }
            if (registered) context.unregisterReceiver(receiver)
            ui.dropShellPermissionIdentity()
        }
    }
}
