package com.deploydulupulangnanti.adblocalshellpro

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.deploydulupulangnanti.adblocalshellpro.data.starterSnippets
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Captures actual app screens using live emulator ADB; never injects fake product state. */
class StoreCaptureTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun captureStoreScreens() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("captureStore") == "true")
        val vm = ViewModelProvider(rule.activity)[ShellViewModel::class.java]
        rule.runOnUiThread { vm.clearHistory(); vm.state.value.snippets.filter { it.title.startsWith("QA identity") }.forEach { vm.deleteSnippet(it.id) } }
        rule.runOnUiThread { vm.connect(args.getString("connectPort", "5555")) }
        rule.waitUntil(30000) { vm.state.value.connected && !vm.state.value.busy }
        starterSnippets.take(5).forEach { snippet ->
            rule.runOnUiThread { vm.run(snippet.command) }
            rule.waitUntil(10000) { !vm.state.value.running && vm.state.value.currentCommand == snippet.command }
        }
        val sample = if (args.getString("capturePrefix") == "tablet7") {
            "getprop ro.build.version.sdk"
        } else {
            "dumpsys battery | grep -E '^ +(level|status|temperature|voltage):'"
        }
        rule.runOnUiThread { vm.run(sample) }
        rule.waitUntil(10000) { !vm.state.value.running && vm.state.value.currentCommand == sample }
        rule.waitUntil(10000) { vm.state.value.message == null }
        capture("shell")
        rule.onNodeWithTag("tab-Commands").performClick(); capture("commands")
        rule.onNodeWithTag("tab-History").performClick(); capture("history")
        rule.onNodeWithTag("tab-Connect").performClick(); capture("connect")
        rule.onNodeWithTag("settings").performClick(); capture("settings")
    }
    private fun capture(name: String) {
        rule.waitForIdle()
        Thread.sleep(700)
        val inst = InstrumentationRegistry.getInstrumentation()
        val bitmap = inst.uiAutomation.takeScreenshot()
        val prefix = InstrumentationRegistry.getArguments().getString("capturePrefix", "phone")
        val dir = File(rule.activity.getExternalFilesDir(null), "store-captures").apply { mkdirs() }
        File(dir, "$prefix-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
