package com.nicfw.tdh3editor

import android.content.Context
import androidx.appcompat.app.AlertDialog
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/**
 * Loads help content from res/raw/help_content.yaml and displays per-setting
 * help dialogs throughout the app.
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
     * Loads and parses help_content.yaml.  Safe to call multiple times —
     * skips reload if already loaded.
     */
    fun init(context: Context) {
        if (entries.isNotEmpty()) return
        try {
            val opts = LoaderOptions()
            val yaml = Yaml(SafeConstructor(opts))
            val stream = context.resources.openRawResource(R.raw.help_content)
            @Suppress("UNCHECKED_CAST")
            val root = yaml.load<Map<String, Any>>(stream)
            @Suppress("UNCHECKED_CAST")
            val list = root["settings"] as? List<Map<String, Any>> ?: return
            entries = list.associate { m ->
                val key = m["key"] as String
                key to HelpEntry(
                    title       = m["title"] as? String ?: key,
                    range       = m["range"] as? String,
                    default     = m["default"] as? String,
                    description = m["description"] as? String ?: "",
                    notes       = m["notes"] as? String
                )
            }
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
