package com.deploydulupulangnanti.adblocalshellpro

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.deploydulupulangnanti.adblocalshellpro.data.CommandRecord
import com.deploydulupulangnanti.adblocalshellpro.data.LocalStore
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class LocalStoreTest {
    @Test fun historyIsBoundedPersistsAndClears() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(app.cacheDir, "store-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = folder
            override fun getSharedPreferences(name: String, mode: Int) = app.getSharedPreferences("${folder.name}-$name", Context.MODE_PRIVATE)
        }
        try {
            val store = LocalStore(context)
            val records = (0 until 110).map { i -> CommandRecord("$i", "echo $i", "x".repeat(20000), 0, i.toLong(), 5) }
            store.saveHistory(records)
            val restored = LocalStore(context).history()
            assertEquals(100, restored.size)
            assertEquals(16384, restored.first().output.length)
            store.rememberHistory = false
            assertFalse(LocalStore(context).rememberHistory)
            assertEquals(100, LocalStore(context).history().size)
            store.saveHistory(emptyList())
            assertTrue(LocalStore(context).history().isEmpty())
        } finally {
            folder.deleteRecursively()
            app.deleteSharedPreferences("${folder.name}-preferences")
        }
    }
}
