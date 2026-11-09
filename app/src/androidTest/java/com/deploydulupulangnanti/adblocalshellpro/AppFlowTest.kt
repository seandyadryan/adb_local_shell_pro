package com.deploydulupulangnanti.adblocalshellpro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun savedCommandCanBeLoadedWithoutExecuting() {
        val title = "QA identity ${System.nanoTime()}"
        rule.onNodeWithTag("tab-Commands").performClick()
        rule.onNodeWithTag("new-snippet").performClick()
        rule.onNodeWithTag("snippet-title").performTextInput(title)
        rule.onNodeWithTag("snippet-command").performTextInput("id")
        rule.onNodeWithTag("save-snippet").performClick()
        rule.onNodeWithText("Custom", useUnmergedTree = true).performClick()
        rule.onNodeWithText(title).performScrollTo().performClick()
        rule.onNodeWithTag("command-input").assertTextContains("id")
        rule.onNodeWithTag("run-command").assertIsNotEnabled()
        rule.onNodeWithTag("tab-Commands").performClick()
        rule.onNodeWithText("Custom", useUnmergedTree = true).performClick()
        rule.onNodeWithContentDescription("Delete $title").performScrollTo().performClick()
    }

    @Test fun connectionAndSettingsAreAccessible() {
        rule.onNodeWithTag("tab-Connect").performClick()
        rule.onNodeWithText("Pair this device").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("settings").performClick()
        rule.onNodeWithTag("history-switch").performScrollTo().assertIsDisplayed()
    }
    @Test fun terminalComposerRemainsVisibleInLandscape() {
        try {
            rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            rule.waitUntil(15000) { rule.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
            rule.onNodeWithTag("command-input").assertIsDisplayed()
            rule.onNodeWithTag("run-command").assertIsDisplayed()
        } finally { rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }
}
