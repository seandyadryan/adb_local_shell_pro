package com.deploydulupulangnanti.adblocalshellpro

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deploydulupulangnanti.adblocalshellpro.adb.AdbEngine
import com.deploydulupulangnanti.adblocalshellpro.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class ShellState(
    val connected: Boolean = false, val busy: Boolean = false, val running: Boolean = false,
    val device: String = "This Android device", val port: String = "", val message: String? = null,
    val currentCommand: String = "", val output: String = "", val exitCode: Int? = null,
    val commandNote: String = "", val history: List<CommandRecord> = emptyList(),
    val snippets: List<Snippet> = starterSnippets, val rememberHistory: Boolean = true, val fontSize: Float = 13f
)

class ShellViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)
    private val engine = AdbEngine()
    private val persistence = Mutex()
    private val mutable = MutableStateFlow(ShellState(port = store.lastPort, rememberHistory = store.rememberHistory, fontSize = store.fontSize))
    val state = mutable.asStateFlow()
    private var stopReason: String? = null
    init { viewModelScope.launch(Dispatchers.IO) {
        val history = store.history(); val snippets = store.snippets()
        mutable.update { it.copy(history = history, snippets = starterSnippets + snippets) }
    } }
    fun dismissMessage() { mutable.update { it.copy(message = null) } }
    fun pair(port: String, code: String) {
        if (state.value.busy || state.value.running) return
        val parsed = InputRules.port(port)
        if (parsed == null || !InputRules.pairingCode(code)) { message("Enter a pairing port (1–65535) and a six-digit code."); return }
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            val timeout = launch { delay(30000); withContext(Dispatchers.IO) { engine.cancelPairing() } }
            try { engine.pair(parsed, code); message("Paired. Enter the connection port from the main Wireless debugging screen.") }
            catch (e: Exception) { message("Pairing failed. Keep the pairing dialog open in split screen, check the code and port, then try again. ${e.message.orEmpty().take(100)}") }
            finally { timeout.cancel(); mutable.update { it.copy(busy = false) } }
        }
    }
    fun connect(port: String) {
        if (state.value.busy || state.value.running) return
        val parsed = InputRules.port(port)
        if (parsed == null) { message("Enter the connection port shown below IP address & Port in Wireless debugging."); return }
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            val timeout = launch { delay(20000); withContext(Dispatchers.IO) { engine.cancelConnect() } }
            try {
                val name = engine.connect(parsed)
                store.lastPort = port.trim()
                mutable.update { it.copy(connected = true, device = name, port = port.trim(), message = "Connected to $name.") }
            } catch (e: Exception) {
                mutable.update { it.copy(connected = false, message = "Could not connect. Check Wireless debugging is on and use its connection port, not the pairing port. ${e.message.orEmpty().take(100)}") }
            } finally { timeout.cancel(); mutable.update { it.copy(busy = false) } }
        }
    }
    fun disconnect() { engine.disconnect(); mutable.update { it.copy(connected = false) } }
    fun run(command: String) {
        val cmd = command.trim()
        if (state.value.running || state.value.busy) return
        if (!InputRules.command(cmd)) { message("Enter a command up to 8,192 characters."); return }
        if (!state.value.connected) { message("Connect this device from the Connect tab first."); return }
        stopReason = null
        mutable.update { it.copy(running = true, currentCommand = cmd, output = "", exitCode = null, commandNote = "") }
        val start = System.currentTimeMillis()
        val elapsedStart = SystemClock.elapsedRealtime()
        viewModelScope.launch {
            val watchdog = launch { delay(120000); stopReason = "Stopped after the 2-minute time limit."; withContext(Dispatchers.IO) { engine.stop() } }
            var exit: Int? = null
            var note = ""
            try {
                exit = engine.execute(cmd) { text -> mutable.update { it.copy(output = TextLimits.tail(it.output + text, 65536)) } }
            } catch (e: Exception) { note = stopReason ?: "Session interrupted: ${e.message.orEmpty().take(180)}" }
            finally { watchdog.cancel() }
            if (stopReason != null) note = stopReason!!
            val result = CommandRecord(UUID.randomUUID().toString(), cmd, TextLimits.tail(state.value.output, 16384), exit, start, SystemClock.elapsedRealtime() - elapsedStart, note)
            mutable.update {
                it.copy(running = false, exitCode = exit, commandNote = note, connected = engine.connected(),
                    history = if (it.rememberHistory) (listOf(result) + it.history).take(100) else it.history)
            }
            if (state.value.rememberHistory) persistHistory()
        }
    }
    fun stop() { stopReason = "Stopped by you. A command may already have changed device state."; viewModelScope.launch(Dispatchers.IO) { engine.stop() } }
    fun clearTerminal() { if (!state.value.running) mutable.update { it.copy(currentCommand = "", output = "", exitCode = null, commandNote = "") } }
    fun clearHistory() { mutable.update { it.copy(history = emptyList()) }; persistHistory() }
    fun setHistory(enabled: Boolean) { store.rememberHistory = enabled; mutable.update { it.copy(rememberHistory = enabled) } }
    fun setFont(size: Float) { store.fontSize = size; mutable.update { it.copy(fontSize = store.fontSize) } }
    fun saveSnippet(title: String, command: String) {
        if (title.isBlank() || title.length > 60 || !InputRules.command(command)) { message("Use a title of 1–60 characters and a valid command."); return }
        if (state.value.snippets.count { it.category == "Custom" } >= 100) { message("You can save up to 100 custom commands."); return }
        val snippet = Snippet(UUID.randomUUID().toString(), title.trim(), command.trim(), "Custom", "Your saved command.")
        mutable.update { it.copy(snippets = it.snippets + snippet) }; persistSnippets()
    }
    fun deleteSnippet(id: String) { mutable.update { it.copy(snippets = it.snippets.filterNot { s -> s.id == id && s.category == "Custom" }) }; persistSnippets() }
    private fun persistSnippets() { viewModelScope.launch(Dispatchers.IO) {
        runCatching { persistence.withLock { store.saveSnippets(state.value.snippets.filter { it.category == "Custom" }) } }
            .onFailure { message("Could not save commands to device storage. Check available space.") }
    } }
    private fun persistHistory() { viewModelScope.launch(Dispatchers.IO) {
        runCatching { persistence.withLock { store.saveHistory(state.value.history) } }
            .onFailure { message("Could not save history to device storage. Export any results you need to keep.") }
    } }
    private fun message(text: String) { mutable.update { it.copy(message = text) } }
    override fun onCleared() { engine.disconnect(); super.onCleared() }
}
