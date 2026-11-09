package com.deploydulupulangnanti.adblocalshellpro.adb

import com.flyfishxu.kadb.Kadb
import com.flyfishxu.kadb.shell.AdbShellStream
import com.flyfishxu.kadb.stream.AdbStream
import java.lang.reflect.InvocationTargetException

/**
 * Kadb 2.1.1's public openShell retries OPEN after transport failure. The daemon
 * may already have started the command when its acknowledgement is lost.
 * Select its existing no-retry path instead: a failed command is never replayed.
 * This small adapter is tied to the pinned dependency and protected by R8 rules.
 * If that contract changes it fails closed, without falling back to public open.
 */
internal object SingleAttemptShell {
    private val openOnce by lazy {
        Kadb::class.java.getDeclaredMethod("openStream", String::class.java, Boolean::class.javaPrimitiveType).apply {
            isAccessible = true
        }
    }

    fun open(client: Kadb, command: String): AdbShellStream {
        check(client.supportsFeature("shell_v2")) { "This device does not support shell protocol v2." }
        // Redirect before exec rather than sending a protocol CLOSE_STDIN packet.
        // A fast command can exit before acknowledging that packet, discarding
        // already queued output in Kadb's stream. Shell quoting preserves the
        // user's command exactly while stdin is EOF from its first instruction.
        val quoted = "'" + command.replace("'", "'\\''") + "'"
        val serviceCommand = "exec /system/bin/sh -c $quoted </dev/null"
        val streams = try {
            openOnce.invoke(client, "shell,v2,raw:$serviceCommand", false) as Pair<*, *>
        } catch (error: InvocationTargetException) {
            throw error.targetException
        }
        return AdbShellStream(streams.second as AdbStream)
    }
}
