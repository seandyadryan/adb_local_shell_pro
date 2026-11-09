package com.deploydulupulangnanti.adblocalshellpro.data

data class CommandRecord(
    val id: String,
    val command: String,
    val output: String,
    val exitCode: Int?,
    val timestamp: Long,
    val durationMs: Long,
    val note: String = ""
)

data class Snippet(val id: String, val title: String, val command: String, val category: String, val description: String)

val starterSnippets = listOf(
    Snippet("identity", "Shell identity", "id", "Device", "Check the identity and groups of your ADB session."),
    Snippet("model", "Device model", "getprop ro.product.model", "Device", "Read your device's model name."),
    Snippet("android", "Android version", "getprop ro.build.version.release", "Device", "See the Android version installed on this device."),
    Snippet("battery", "Battery report", "dumpsys battery", "Diagnostics", "Inspect battery level, temperature and charging state."),
    Snippet("storage", "Storage usage", "df -h /data", "Diagnostics", "Check free and used space on the data partition."),
    Snippet("memory", "Memory summary", "cat /proc/meminfo | head -n 12", "Diagnostics", "Read a compact system memory report."),
    Snippet("display", "Display size", "wm size; wm density", "Device", "Read the current display resolution and density."),
    Snippet("packages", "User-installed packages", "pm list packages -3", "Apps", "List package names for apps installed by the user."),
    Snippet("uptime", "System uptime", "uptime", "Diagnostics", "See uptime and load averages."),
    Snippet("logs", "Recent system logs", "logcat -d -t 60", "Diagnostics", "Read the latest 60 log lines without a continuous stream.")
)

object InputRules {
    fun port(value: String): Int? = value.trim().toIntOrNull()?.takeIf { it in 1..65535 }
    fun pairingCode(value: String): Boolean = value.length == 6 && value.all { it in '0'..'9' }
    fun command(value: String): Boolean = value.isNotBlank() && value.length <= 8192 && '\u0000' !in value
    fun requiresConfirmation(value: String): Boolean =
        Regex("(?i)(^|[\\s;|&(/])(?:rm|reboot|su|dd|mkfs|shutdown|setprop|settings\\s+(?:put|delete|reset)|pm\\s+(?:uninstall|disable|clear)|am\\s+force-stop)\\b").containsMatchIn(value)
}

object TextLimits {
    /** Bound stored/displayed UTF-8 bytes without splitting a Unicode character. */
    fun tail(text: String, maxBytes: Int): String {
        if (maxBytes <= 0) return ""
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (bytes.size <= maxBytes) return text
        var start = bytes.size - maxBytes
        while (start < bytes.size && (bytes[start].toInt() and 0xC0) == 0x80) start++
        return String(bytes, start, bytes.size - start, Charsets.UTF_8)
    }
}
