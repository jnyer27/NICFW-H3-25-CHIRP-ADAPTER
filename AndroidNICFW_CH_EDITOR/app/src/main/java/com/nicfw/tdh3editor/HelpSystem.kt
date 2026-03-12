package com.nicfw.tdh3editor

import android.content.Context
import androidx.appcompat.app.AlertDialog
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loads help content from res/raw/help_content.json and displays per-setting
 * help dialogs throughout the app.
 *
 * Uses Android's built-in org.json parser — no external dependencies required.
 *
 * Call [init] once per Activity (it is idempotent) then call [show] from any
 * help-button click listener.
 */
object HelpSystem {

    data class HelpEntry(
        val title: String,
        val range: String?,
        val default: String?,
        val description: String,
        val notes: String?
    )

    @Volatile
    private var entries: Map<String, HelpEntry> = emptyMap()

    /**
     * Loads and parses help_content.json.  Safe to call multiple times —
     * skips reload if already loaded.
     */
    fun init(context: Context) {
        if (entries.isNotEmpty()) return
        try {
            val stream = context.resources.openRawResource(R.raw.help_content)
            val json = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(json)
            val list: JSONArray = root.getJSONArray("settings")
            val map = mutableMapOf<String, HelpEntry>()
            for (i in 0 until list.length()) {
                val m: JSONObject = list.getJSONObject(i)
                val key = m.getString("key")
                map[key] = HelpEntry(
                    title       = m.optString("title", key).ifEmpty { key },
                    range       = m.optString("range").ifEmpty { null },
                    default     = m.optString("default").ifEmpty { null },
                    description = m.optString("description", ""),
                    notes       = m.optString("notes").ifEmpty { null }
                )
            }
            entries = map
        } catch (e: Exception) {
            // Don't crash the app if help content fails to load
        }
    }

    /**
     * Shows an AlertDialog with help content for [key].
     * Silently no-ops if the key is not found or [init] hasn't been called.
     */
    fun show(context: Context, key: String) {
        val e = entries[key] ?: return
        val message = buildString {
            e.range?.let   { appendLine("Range: $it") }
            e.default?.let { appendLine("Default: $it") }
            if (e.range != null || e.default != null) appendLine()
            append(e.description)
            e.notes?.let { append("\n\n\u26a0 $it") }   // ⚠
        }
        AlertDialog.Builder(context)
            .setTitle("\uD83D\uDCA1 ${e.title}")          // 💡
            .setMessage(message.trim())
            .setPositiveButton("Got it", null)
            .show()
    }
}
