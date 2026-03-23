package com.nicfw.tdh3editor

import android.content.Context
import com.nicfw.tdh3editor.radio.Channel
import com.nicfw.tdh3editor.radio.EepromParser

/**
 * Bulk-editable channel fields (multi-select on the main list). Each maps to a
 * “setting type” in the UI: list pickers vs checkbox for bool.
 *
 * TX power and groups stay on their dedicated toolbar buttons.
 */
enum class ChannelBulkField {
    MODULATION,
    BANDWIDTH,
    BUSY_LOCK;

    fun title(context: Context): String = when (this) {
        MODULATION -> context.getString(R.string.bulk_edit_field_modulation)
        BANDWIDTH -> context.getString(R.string.bulk_edit_field_bandwidth)
        BUSY_LOCK -> context.getString(R.string.bulk_edit_field_busy_lock)
    }
}

object ChannelBulkEdit {

    /** Busy lock is disallowed when repeater offset or split TX is configured. */
    fun channelHasDuplexOffset(ch: Channel): Boolean =
        ch.duplex == "+" || ch.duplex == "-" || ch.duplex.equals("split", ignoreCase = true)

    fun applyModulation(eeprom: ByteArray, selected: Set<Int>, mode: String): Int {
        var count = 0
        val channels = EepromParser.parseAllChannels(eeprom)
        for (ch in channels) {
            if (ch.number in selected && !ch.empty) {
                EepromParser.writeChannel(eeprom, ch.copy(mode = mode))
                count++
            }
        }
        return count
    }

    fun applyBandwidth(eeprom: ByteArray, selected: Set<Int>, bandwidth: String): Int {
        var count = 0
        val channels = EepromParser.parseAllChannels(eeprom)
        for (ch in channels) {
            if (ch.number in selected && !ch.empty) {
                EepromParser.writeChannel(eeprom, ch.copy(bandwidth = bandwidth))
                count++
            }
        }
        return count
    }

    /**
     * @return pair of (channels updated, skipped — only when [enabled] is true and channel has duplex offset)
     */
    fun applyBusyLock(eeprom: ByteArray, selected: Set<Int>, enabled: Boolean): Pair<Int, Int> {
        var updated = 0
        var skipped = 0
        val channels = EepromParser.parseAllChannels(eeprom)
        for (ch in channels) {
            if (ch.number !in selected || ch.empty) continue
            if (enabled && channelHasDuplexOffset(ch)) {
                skipped++
                continue
            }
            EepromParser.writeChannel(eeprom, ch.copy(busyLock = enabled))
            updated++
        }
        return updated to skipped
    }
}
