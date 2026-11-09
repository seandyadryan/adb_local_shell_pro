@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.deploydulupulangnanti.adblocalshellpro.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PersistableBundle
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deploydulupulangnanti.adblocalshellpro.*
import com.deploydulupulangnanti.adblocalshellpro.R
import com.deploydulupulangnanti.adblocalshellpro.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Tab(val title: String, val icon: Int) {
    Shell("Shell", R.drawable.ic_terminal), Commands("Commands", R.drawable.ic_bookmarks),
    History("History", R.drawable.ic_history), Connect("Connect", R.drawable.ic_cable)
}

@Composable private fun Icon(icon: Int, contentDescription: String?, modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = LocalContentColor.current) {
    androidx.compose.material3.Icon(painterResource(icon), contentDescription, modifier, tint)
}

@Composable fun ShellApp(vm: ShellViewModel, onOpenSettings: () -> Unit, onExport: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    var confirmCommand by remember { mutableStateOf<String?>(null) }
    var saveCommand by remember { mutableStateOf<String?>(null) }
    var historyDetail by remember { mutableStateOf<CommandRecord?>(null) }
    val snack = remember { SnackbarHostState() }
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(state.message) { state.message?.let { snack.showSnackbar(it); vm.dismissMessage() } }
    val load: (String) -> Unit = { draft = it; tab = 0; settings = false }
    val submit = {
        focus.clearFocus()
        if (InputRules.requiresConfirmation(draft)) confirmCommand = draft else vm.run(draft)
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Ink)) {
        val wide = maxWidth >= 840.dp
        Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
            if (wide) NavigationRail(Modifier.fillMaxHeight().width(100.dp).windowInsetsPadding(WindowInsets.statusBars), containerColor = Ink) {
                Spacer(Modifier.height(20.dp)); BrandMark(); Spacer(Modifier.height(46.dp))
                Tab.entries.forEachIndexed { i, t -> NavigationRailItem(selected = tab == i && !settings,
                    onClick = { tab = i; settings = false }, modifier = Modifier.testTag("tab-${t.name}"), icon = { Icon(t.icon, t.title) }, label = { Text(t.title) },
                    colors = NavigationRailItemDefaults.colors(indicatorColor = Raised, selectedIconColor = Lime)) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { settings = true }) { Icon(R.drawable.ic_tune, "Settings") }
                Spacer(Modifier.height(20.dp))
            }
            Scaffold(Modifier.weight(1f).imePadding(), containerColor = Ink, contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snack) },
                topBar = { Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!wide) { BrandMark(); Spacer(Modifier.width(10.dp)) }
                        Text("LOCAL SHELL", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Spacer(Modifier.width(9.dp)); Badge(containerColor = Lime, contentColor = Ink) { Text("PRO", fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.weight(1f))
                        if (state.connected) Box(Modifier.size(7.dp).background(Lime, CircleShape))
                        IconButton(onClick = { settings = !settings }, modifier = Modifier.size(42.dp).testTag("settings")) { Icon(if (settings) R.drawable.ic_close else R.drawable.ic_tune, if (settings) "Close settings" else "Settings", tint = Muted, modifier = Modifier.size(21.dp)) }
                    }
                    HorizontalDivider(color = Line)
                } },
                bottomBar = { if (!wide && !keyboardVisible) NavigationBar(containerColor = Ink, tonalElevation = 0.dp) {
                    Tab.entries.forEachIndexed { i, t -> NavigationBarItem(selected = tab == i && !settings,
                        onClick = { tab = i; settings = false; focus.clearFocus() },
                        modifier = Modifier.testTag("tab-${t.name}"), icon = { Icon(t.icon, null, Modifier.size(22.dp)) }, label = { Text(t.title, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Lime, selectedTextColor = Lime, unselectedIconColor = Muted, unselectedTextColor = Muted, indicatorColor = Raised)) }
                } }
            ) { padding ->
                BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
                    if (settings) SettingsScreen(state, vm, onExport)
                    else when (Tab.entries[tab]) {
                        Tab.Shell -> TerminalScreen(state, draft, { draft = it }, submit, vm::stop, vm::clearTerminal,
                            { tab = 3 }, { saveCommand = draft.ifBlank { state.currentCommand } }, onExport, wide,
                            compact = maxHeight < 420.dp, hideOutput = maxHeight < 200.dp)
                        Tab.Commands -> CommandsScreen(state.snippets, load, { saveCommand = "" }, vm::deleteSnippet)
                        Tab.History -> HistoryScreen(state.history, { historyDetail = it }, vm::clearHistory, onExport)
                        Tab.Connect -> ConnectScreen(state, vm::pair, vm::connect, vm::disconnect, onOpenSettings)
                    }
                }
            }
        }
    }
    confirmCommand?.let { command -> AlertDialog(onDismissRequest = { confirmCommand = null }, title = { Text("Run this command?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { Text("This command may change device settings or data. Review it before continuing."); Code(command) } },
        confirmButton = { TextButton(onClick = { vm.run(command); confirmCommand = null }) { Text("Run command") } },
        dismissButton = { TextButton(onClick = { confirmCommand = null }) { Text("Cancel") } }) }
    saveCommand?.let { command -> SnippetDialog(command, { saveCommand = null }) { title, text -> vm.saveSnippet(title, text); saveCommand = null } }
    historyDetail?.let { record -> ModalBottomSheet(onDismissRequest = { historyDetail = null }, containerColor = Panel) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Eyebrow("COMMAND DETAILS"); Code(record.command)
            Text("${date(record.timestamp)}  ·  ${record.durationMs} ms  ·  ${record.exitCode?.let { "Exit $it" } ?: "Interrupted"}", color = Muted, fontSize = 12.sp)
            SelectionContainer { Text(record.output.ifBlank { "No output." }, fontFamily = FontFamily.Monospace, fontSize = state.fontSize.sp) }
            if (record.note.isNotEmpty()) Text(record.note, color = Warm)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { load(record.command); historyDetail = null }) { Text("Use command") }
                OutlinedButton(onClick = { copy(context, record.output) }) { Text("Copy output") }
            }
        }
    } }
}

@Composable private fun BrandMark() {
    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Lime), contentAlignment = Alignment.Center) {
        Icon(R.drawable.ic_terminal, null, tint = Ink, modifier = Modifier.size(24.dp))
    }
}
@Composable private fun Eyebrow(text: String, color: androidx.compose.ui.graphics.Color = Muted) { Text(text, style = MaterialTheme.typography.labelSmall, color = color) }
@Composable private fun PageTitle(kicker: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Eyebrow(kicker, Lime); Text(title, style = MaterialTheme.typography.headlineLarge); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium) }
}
@Composable private fun Code(text: String) { Text(text, modifier = Modifier.fillMaxWidth().background(Ink, RoundedCornerShape(10.dp)).padding(14.dp), fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Lime) }

@Composable private fun TerminalScreen(state: ShellState, draft: String, setDraft: (String) -> Unit, run: () -> Unit,
    stop: () -> Unit, clear: () -> Unit, connect: () -> Unit, save: () -> Unit, export: (String) -> Unit,
    wide: Boolean, compact: Boolean, hideOutput: Boolean) {
    val context = LocalContext.current
    Row(Modifier.fillMaxSize().padding(horizontal = if (wide) 32.dp else 20.dp).padding(top = if (compact) 8.dp else 22.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp)) {
            if (!compact) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Eyebrow("ON-DEVICE WORKSPACE", Lime); Spacer(Modifier.height(6.dp)); Text("Your device.\nYour command.", style = MaterialTheme.typography.headlineMedium) }
                Surface(onClick = connect, color = if (state.connected) Raised else Panel, shape = RoundedCornerShape(50), border = BorderStroke(1.dp, Line)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.size(6.dp).background(if (state.connected) Lime else Muted, CircleShape)); Text(if (state.connected) "Connected" else "Set up", fontSize = 11.sp, color = if (state.connected) Lime else Paper)
                    }
                }
            }
            if (hideOutput) Spacer(Modifier.weight(1f))
            else Surface(Modifier.weight(1f).fillMaxWidth(), color = Panel, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Line)) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(R.drawable.ic_terminal, null, tint = Muted, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                        Text("local / shell", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Muted)
                        Spacer(Modifier.weight(1f)); IconButton(onClick = { copy(context, state.output) }, enabled = state.output.isNotBlank()) { Icon(R.drawable.ic_content_copy, "Copy terminal output", Modifier.size(16.dp)) }
                        IconButton(onClick = clear, enabled = !state.running && state.currentCommand.isNotBlank()) { Icon(R.drawable.ic_delete_sweep, "Clear terminal", Modifier.size(18.dp)) }
                    }
                    HorizontalDivider(color = Line)
                    val scroll = rememberScrollState()
                    LaunchedEffect(state.output.length) { scroll.scrollTo(scroll.maxValue) }
                    SelectionContainer(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (state.currentCommand.isBlank()) {
                                Text("ADB LOCAL SHELL / 1.0", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                Text(if (state.connected) "Connection ready.\nEnter a command below to begin." else "A little closer to Android.\nA lot more control.", color = Paper, fontFamily = FontFamily.Monospace, fontSize = state.fontSize.sp, lineHeight = 23.sp)
                                Text(if (state.connected) "Try: id" else "01  Enable Wireless debugging\n02  Pair this device\n03  Make it your workspace", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 25.sp)
                                Text("${'$'} _", color = Lime, fontFamily = FontFamily.Monospace, fontSize = 20.sp)
                            } else {
                                Text("${'$'} ${state.currentCommand}", color = Lime, fontFamily = FontFamily.Monospace, fontSize = state.fontSize.sp, modifier = Modifier.testTag("last-command"))
                                if (state.output.isNotBlank()) Text(cleanOutput(state.output), color = Paper, fontFamily = FontFamily.Monospace, fontSize = state.fontSize.sp, lineHeight = (state.fontSize * 1.6).sp, modifier = Modifier.testTag("terminal-output"))
                                if (state.commandNote.isNotEmpty()) Text(state.commandNote, color = Warm, fontSize = 12.sp)
                                if (state.exitCode != null) Text("↳ exit ${state.exitCode}", color = if (state.exitCode == 0) Muted else Warm, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                if (state.running) Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.dp); Spacer(Modifier.width(8.dp)); Text("Running…", color = Muted, fontSize = 12.sp) }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().background(Ink.copy(alpha = .3f)).padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(5.dp).background(if (state.connected) Lime else Muted, CircleShape)); Spacer(Modifier.width(7.dp))
                        Text(if (state.connected) state.device else "No active connection", color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(if (state.connected) "127.0.0.1" else "LOCAL ONLY", fontFamily = FontFamily.Monospace, color = Muted, fontSize = 9.sp)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = draft, onValueChange = { if (it.length <= 8192) setDraft(it) }, modifier = Modifier.weight(1f).testTag("command-input"),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        placeholder = { Text("Enter a shell command", fontSize = 12.sp) }, leadingIcon = { Text("${'$'}", color = Lime, fontFamily = FontFamily.Monospace) },
                        maxLines = 4, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { if (!state.running && state.connected) run() }))
                    FilledIconButton(onClick = if (state.running) stop else run, enabled = state.running || (draft.isNotBlank() && state.connected && !state.busy), modifier = Modifier.size(54.dp).testTag("run-command"), shape = RoundedCornerShape(12.dp)) {
                        Icon(if (state.running) R.drawable.ic_stop else R.drawable.ic_arrow_forward, if (state.running) "Stop command" else "Run command")
                    }
                }
                if (!hideOutput) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = save, enabled = draft.isNotBlank() || state.currentCommand.isNotBlank(), contentPadding = PaddingValues(horizontal = 4.dp)) { Icon(R.drawable.ic_bookmark_add, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Save command", fontSize = 11.sp) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { export("${'$'} ${state.currentCommand}\n${state.output}\n${state.commandNote}") }, enabled = state.currentCommand.isNotBlank(), contentPadding = PaddingValues(horizontal = 4.dp)) { Text("Export", fontSize = 11.sp); Spacer(Modifier.width(5.dp)); Icon(R.drawable.ic_open_in_new, null, Modifier.size(14.dp)) }
                }
            }
        }
        if (wide && !compact) Column(Modifier.width(260.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Spacer(Modifier.height(6.dp)); Eyebrow("WORKSPACE NOTES")
            Text("Small commands.\nUseful answers.", style = MaterialTheme.typography.titleLarge)
            Text("Read system properties, inspect battery health and troubleshoot your Android device.", color = Muted)
            HorizontalDivider(color = Line)
            Eyebrow("QUICK START")
            listOf("id", "dumpsys battery", "df -h /data").forEach { command -> Surface(onClick = { setDraft(command) }, shape = RoundedCornerShape(10.dp), color = Panel) { Code(command) } }
            Text("Commands run as Android's ADB shell user. Root is not required. Each command starts a fresh shell.", color = Muted, fontSize = 12.sp)
            Text("No ads. No account.\nYour workspace stays here.", color = Lime, fontSize = 13.sp, lineHeight = 22.sp)
        }
    }
}

@Composable private fun CommandsScreen(snippets: List<Snippet>, load: (String) -> Unit, add: () -> Unit, delete: (String) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }
    val filtered = snippets.filter { (category == "All" || it.category == category) && (it.title.contains(search, true) || it.command.contains(search, true)) }
    LazyColumn(Modifier.fillMaxSize().widthIn(max = 1000.dp), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("THE COMMAND SHELF", "Less typing.\nMore doing.", "Useful starters and your own saved commands.") }
        item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(search, { search = it }, Modifier.weight(1f), placeholder = { Text("Find a command") }, leadingIcon = { Icon(R.drawable.ic_search, null) }, singleLine = true, shape = RoundedCornerShape(12.dp))
            FilledTonalIconButton(onClick = add, modifier = Modifier.size(54.dp).testTag("new-snippet")) { Icon(R.drawable.ic_add, "New saved command") }
        } }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Device", "Diagnostics", "Apps", "Custom").forEach { name -> FilterChip(selected = category == name, onClick = { category = name }, label = { Text(name, fontSize = 12.sp) }) }
        } }
        if (filtered.isEmpty()) item { EmptyState(R.drawable.ic_search_off, "Nothing here yet", "Try another search or save a command of your own.") }
        items(filtered, key = { it.id }) { snippet ->
            Surface(onClick = { load(snippet.command) }, shape = RoundedCornerShape(14.dp), color = Panel, border = BorderStroke(1.dp, Line)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(snippet.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (snippet.category == "Custom") IconButton(onClick = { delete(snippet.id) }, Modifier.size(28.dp)) { Icon(R.drawable.ic_delete, "Delete ${snippet.title}", Modifier.size(18.dp), tint = Muted) }
                        else Icon(R.drawable.ic_arrow_forward, null, tint = Muted, modifier = Modifier.size(18.dp))
                    }
                    Text(snippet.command, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Lime)
                    Text(snippet.description, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
        item { Text("Tap to load a command. You decide when to run it.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp)) }
    }
}

@Composable private fun HistoryScreen(history: List<CommandRecord>, detail: (CommandRecord) -> Unit, clear: () -> Unit, export: (String) -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().widthIn(max = 1000.dp), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("PICK UP WHERE YOU LEFT OFF", "A useful\ntrack record.", "Revisit commands and their most recent results.") }
        item { OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search command history") }, singleLine = true, leadingIcon = { Icon(R.drawable.ic_search, null) }, shape = RoundedCornerShape(12.dp)) }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("${history.size} SAVED ${if (history.size == 1) "COMMAND" else "COMMANDS"}")
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { export(history.joinToString("\n\n") { "${date(it.timestamp)}\n${'$'} ${it.command}\n${it.output}\nExit: ${it.exitCode ?: "interrupted"}\n${it.note}" }) }, enabled = history.isNotEmpty()) { Text("Export", fontSize = 12.sp) }
            IconButton(onClick = { confirm = true }, enabled = history.isNotEmpty()) { Icon(R.drawable.ic_delete, "Clear history", Modifier.size(19.dp)) }
        } }
        val filtered = history.filter { it.command.contains(search, true) }
        if (filtered.isEmpty()) item { EmptyState(R.drawable.ic_history, "A fresh start", if (history.isEmpty()) "Commands you run will appear here. History is stored only on this device." else "No commands match your search.") }
        items(filtered, key = { it.id }) { record -> Surface(onClick = { detail(record) }, shape = RoundedCornerShape(12.dp), color = Panel) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(if (record.exitCode == 0) R.drawable.ic_check_circle else R.drawable.ic_error, null, tint = if (record.exitCode == 0) Lime else Warm, modifier = Modifier.size(17.dp))
                    Text(record.command, fontFamily = FontFamily.Monospace, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Icon(R.drawable.ic_arrow_forward, null, tint = Muted, modifier = Modifier.size(16.dp))
                }
                Text("${date(record.timestamp)}  ·  ${record.durationMs} ms  ·  ${record.exitCode?.let { "Exit $it" } ?: "Interrupted"}", color = Muted, fontSize = 10.sp)
            }
        } }
        item { Text("Up to 100 commands. Up to 16 KB of output per entry.", fontSize = 11.sp, color = Muted) }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Clear command history?") }, text = { Text("This removes all saved commands and their output from this app. Export a copy first if you need it.") }, confirmButton = { TextButton(onClick = { clear(); confirm = false }) { Text("Clear history") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
}

@Composable private fun ConnectScreen(state: ShellState, pair: (String, String) -> Unit, connect: (String) -> Unit, disconnect: () -> Unit, settings: () -> Unit) {
    var pairingPort by rememberSaveable { mutableStateOf("") }
    // Deliberately not saveable: don't keep a pairing secret in saved instance state.
    var code by remember { mutableStateOf("") }
    var connectionPort by rememberSaveable { mutableStateOf(state.port) }
    Column(Modifier.fillMaxSize().widthIn(max = 820.dp).verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        PageTitle("ONE DEVICE. ZERO CABLES.", "Let's make\nthe connection.", "Connect directly to this device with Android's Wireless debugging. Android 11 or later and Wi-Fi required.")
        if (state.connected) Surface(color = Raised, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Lime.copy(alpha = .3f))) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(R.drawable.ic_check_circle, null, tint = Lime); Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(state.device, fontWeight = FontWeight.SemiBold); Text("127.0.0.1:${state.port}", fontSize = 12.sp, color = Muted, fontFamily = FontFamily.Monospace) }
                TextButton(onClick = disconnect, enabled = !state.running && !state.busy) { Text("Disconnect", fontSize = 12.sp) }
            }
        }
        Step("01", "Enable Wireless debugging", "In Developer options, turn on Wireless debugging and allow your current Wi-Fi network.") {
            OutlinedButton(onClick = settings, modifier = Modifier.fillMaxWidth()) { Text("Open Developer options"); Spacer(Modifier.width(8.dp)); Icon(R.drawable.ic_open_in_new, null, Modifier.size(16.dp)) }
            Text("Developer options hidden? In Settings → About phone, tap Build number seven times. Some manufacturers place it under Software information.", color = Muted, fontSize = 12.sp, lineHeight = 19.sp)
        }
        Step("02", "Pair this device", "Use split screen to keep this app and Settings visible. Tap Pair device with pairing code, then enter its port and six-digit code below.") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PortField(pairingPort, { pairingPort = it }, "Pairing port", Modifier.weight(1f), "pair-port")
                OutlinedTextField(code, { if (it.length <= 6 && it.all(Char::isDigit)) code = it }, Modifier.weight(1f).testTag("pair-code"), label = { Text("Pairing code", fontSize = 12.sp) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(10.dp))
            }
            Button(onClick = { pair(pairingPort, code); code = "" }, enabled = !state.busy && !state.running && InputRules.port(pairingPort) != null && InputRules.pairingCode(code), modifier = Modifier.fillMaxWidth()) {
                if (state.busy) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }; Text("Pair device")
            }
        }
        Step("03", "Open your shell", "Close the pairing dialog. On the main Wireless debugging page, find IP address & Port. Enter that port here; it is different from the pairing port.") {
            PortField(connectionPort, { connectionPort = it }, "Connection port", Modifier.fillMaxWidth(), "connection-port")
            Button(onClick = { connect(connectionPort) }, enabled = !state.busy && !state.running && InputRules.port(connectionPort) != null, modifier = Modifier.fillMaxWidth().testTag("connect-device")) { Text(if (state.busy) "Working…" else "Connect to local shell"); Spacer(Modifier.width(8.dp)); Icon(R.drawable.ic_arrow_forward, null, Modifier.size(17.dp)) }
        }
        HorizontalDivider(color = Line)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(R.drawable.ic_lock, null, tint = Muted, modifier = Modifier.size(20.dp)); Text("Your pairing key stays on this device. No account, cloud relay or external ADB app is needed. Reconnect after restarting; Android may change the port.", color = Muted, fontSize = 12.sp, lineHeight = 20.sp) }
        Text("If pairing fails, keep the system pairing dialog open, use a fresh code and check Wi-Fi. Manufacturer restrictions, VPNs or disabled debugging can prevent a connection. Turn off Wireless debugging when finished.", color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
    }
}
@Composable private fun PortField(value: String, change: (String) -> Unit, label: String, modifier: Modifier, tag: String) {
    OutlinedTextField(value, { if (it.length <= 5 && it.all(Char::isDigit)) change(it) }, modifier.testTag(tag), label = { Text(label, fontSize = 12.sp) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(10.dp))
}
@Composable private fun Step(number: String, title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(number, fontFamily = FontFamily.Monospace, color = Lime, fontSize = 12.sp, modifier = Modifier.background(Raised, RoundedCornerShape(8.dp)).padding(10.dp)); Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Text(description, color = Muted, fontSize = 13.sp, lineHeight = 21.sp)
        content()
    }
}

@Composable private fun SettingsScreen(state: ShellState, vm: ShellViewModel, export: (String) -> Unit) {
    val context = LocalContext.current
    var privacy by remember { mutableStateOf(false) }
    var licenses by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().widthIn(max = 820.dp).verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        PageTitle("MAKE ROOM FOR YOUR WORK", "Your workspace,\nyour way.", "A few thoughtful controls. Nothing to sign up for.")
        Eyebrow("TERMINAL")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Text size", Modifier.weight(1f)); Text("${state.fontSize.toInt()} sp", color = Lime, fontFamily = FontFamily.Monospace) }
        Slider(state.fontSize, vm::setFont, valueRange = 11f..20f, steps = 8)
        HorizontalDivider(color = Line)
        Eyebrow("ON THIS DEVICE")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 16.dp)) { Text("Remember command history"); Text("Keep up to 100 commands and their output locally. Turn off before running sensitive commands.", color = Muted, fontSize = 12.sp, lineHeight = 19.sp) }
            Switch(state.rememberHistory, vm::setHistory, modifier = Modifier.testTag("history-switch"))
        }
        Text("Disabling history stops future saves. Existing history remains until you clear it from the History tab. Terminal output is kept in memory until cleared or the app closes.", color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
        HorizontalDivider(color = Line)
        Eyebrow("GOOD TO KNOW")
        Text("Each command runs in a fresh, non-interactive shell. Use cd /path && command to choose a working directory. Output is limited to 1 MB; the terminal shows the latest 64 KB. Commands stop after two minutes. Interactive editors and persistent sessions are not supported.", color = Muted, fontSize = 13.sp, lineHeight = 21.sp)
        Text("Commands have the permissions of Android's ADB shell user, not root. Review commands before running them. Stopping a command does not undo changes it has already made.", color = Muted, fontSize = 13.sp, lineHeight = 21.sp)
        HorizontalDivider(color = Line)
        Eyebrow("ABOUT LOCAL SHELL")
        SettingLink(R.drawable.ic_policy, "Privacy policy") { privacy = true }
        SettingLink(R.drawable.ic_code, "Open-source licenses") { licenses = true }
        SettingLink(R.drawable.ic_mail, "Contact support") { runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:seandyadryan2@gmail.com?subject=ADB%20Local%20Shell%20PRO%20support"))) } }
        Text("ADB Local Shell PRO · ${BuildConfig.VERSION_NAME}\nPT Deploy Dulu Pulang Nanti", color = Muted, fontSize = 12.sp, lineHeight = 22.sp)
        Text("Built for the curious.", color = Lime, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
    if (privacy || licenses) AlertDialog(onDismissRequest = { privacy = false; licenses = false }, title = { Text(if (privacy) "Privacy policy" else "Open-source licenses") }, text = {
        Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) { Text(runCatching { context.assets.open(if (privacy) "privacy.txt" else "licenses.txt").bufferedReader().use { it.readText() } }.getOrDefault("Contact seandyadryan2@gmail.com for a copy."), fontSize = 13.sp, lineHeight = 21.sp) }
    }, confirmButton = { TextButton(onClick = { privacy = false; licenses = false }) { Text("Done") } })
}
@Composable private fun SettingLink(icon: Int, title: String, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = click).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Muted, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(14.dp)); Text(title, Modifier.weight(1f)); Icon(R.drawable.ic_arrow_forward, null, tint = Muted, modifier = Modifier.size(18.dp)) }
}
@Composable private fun EmptyState(icon: Int, title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 42.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = Muted, modifier = Modifier.size(36.dp)); Text(title, style = MaterialTheme.typography.titleLarge); Text(description, color = Muted, fontSize = 13.sp, modifier = Modifier.widthIn(max = 280.dp)) }
}
@Composable private fun SnippetDialog(initial: String, dismiss: () -> Unit, save: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }; var command by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Save a command") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(title, { if (it.length <= 60) title = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.testTag("snippet-title"))
        OutlinedTextField(command, { if (it.length <= 8192) command = it }, label = { Text("Command") }, minLines = 2, maxLines = 5, textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp), modifier = Modifier.testTag("snippet-command"))
    } }, confirmButton = { TextButton(onClick = { save(title, command) }, enabled = title.isNotBlank() && InputRules.command(command), modifier = Modifier.testTag("save-snippet")) { Text("Save command") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}
private fun copy(context: Context, text: String) {
    val clip = ClipData.newPlainText("Local Shell", cleanOutput(text)).apply {
        description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
    }
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
}
private fun cleanOutput(text: String): String = text.replace(Regex("\u001B\\[[0-?]*[ -/]*[@-~]"), "").replace("\u0000", "")
private fun date(time: Long): String = SimpleDateFormat("dd MMM, HH:mm", Locale.ENGLISH).format(Date(time))
