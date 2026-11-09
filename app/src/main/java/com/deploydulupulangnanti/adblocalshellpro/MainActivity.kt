package com.deploydulupulangnanti.adblocalshellpro

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.deploydulupulangnanti.adblocalshellpro.ui.LocalShellTheme
import com.deploydulupulangnanti.adblocalshellpro.ui.ShellApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast

class MainActivity : ComponentActivity() {
    private val vm: ShellViewModel by viewModels()
    private var exportText = ""
    private val export = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) { runCatching {
                checkNotNull(contentResolver.openOutputStream(uri)).bufferedWriter().use { it.write(exportText) }
            }.isSuccess }
            Toast.makeText(this@MainActivity, if (success) "Export saved." else "Could not save the export.", Toast.LENGTH_LONG).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exportText = savedInstanceState?.getString("exportText").orEmpty()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            LocalShellTheme {
                ShellApp(vm, onOpenSettings = {
                    try { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    catch (_: ActivityNotFoundException) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
                }, onExport = { text ->
                    exportText = text
                    export.launch("local-shell-${System.currentTimeMillis()}.txt")
                })
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("exportText", exportText.take(200000))
        super.onSaveInstanceState(outState)
    }
}
