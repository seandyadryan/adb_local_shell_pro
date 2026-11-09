package com.deploydulupulangnanti.adblocalshellpro.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    private val historyFile = AtomicFile(File(context.filesDir, "history.json"))
    private val snippetsFile = AtomicFile(File(context.filesDir, "snippets.json"))
    var rememberHistory: Boolean
        get() = prefs.getBoolean("history", true)
        set(value) { prefs.edit().putBoolean("history", value).apply() }
    var fontSize: Float
        get() = prefs.getFloat("fontSize", 13f).coerceIn(11f, 20f)
        set(value) { prefs.edit().putFloat("fontSize", value.coerceIn(11f, 20f)).apply() }
    var lastPort: String
        get() = prefs.getString("port", "") ?: ""
        set(value) { prefs.edit().putString("port", value).apply() }

    @Synchronized fun history(): List<CommandRecord> = read(historyFile).let { array ->
        (0 until array.length()).mapNotNull { i -> runCatching {
            array.getJSONObject(i).let { o -> CommandRecord(o.getString("id"), o.getString("command"), o.getString("output"),
                if (o.isNull("exit")) null else o.getInt("exit"), o.getLong("time"), o.getLong("duration"), o.optString("note")) }
        }.getOrNull() }.take(100)
    }
    @Synchronized fun saveHistory(items: List<CommandRecord>) = write(historyFile, JSONArray().apply {
        items.take(100).forEach { r -> put(JSONObject().apply {
            put("id", r.id); put("command", r.command); put("output", TextLimits.tail(r.output, 16384))
            put("exit", r.exitCode ?: JSONObject.NULL); put("time", r.timestamp); put("duration", r.durationMs); put("note", r.note)
        }) }
    })
    @Synchronized fun snippets(): List<Snippet> = read(snippetsFile).let { a ->
        (0 until a.length()).mapNotNull { i -> runCatching { a.getJSONObject(i).let {
            Snippet(it.getString("id"), it.getString("title"), it.getString("command"), "Custom", it.optString("description", "Your saved command."))
        } }.getOrNull() }.take(100)
    }
    @Synchronized fun saveSnippets(items: List<Snippet>) = write(snippetsFile, JSONArray().apply {
        items.take(100).forEach { s -> put(JSONObject().apply {
            put("id", s.id); put("title", s.title); put("command", s.command); put("description", s.description)
        }) }
    })
    private fun read(file: AtomicFile): JSONArray = runCatching {
        file.openRead().bufferedReader().use { JSONArray(it.readText()) }
    }.getOrDefault(JSONArray())
    private fun write(file: AtomicFile, data: JSONArray) {
        val stream = file.startWrite()
        try { stream.write(data.toString().toByteArray()); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
    }
}
