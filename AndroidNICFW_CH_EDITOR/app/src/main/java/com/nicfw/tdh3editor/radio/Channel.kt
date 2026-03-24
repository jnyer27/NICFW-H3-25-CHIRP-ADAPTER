package com.nicfw.tdh3editor.radio

import kotlin.math.abs

/**
 * One memory channel (1–198). Mirrors chirp_common.Memory / _channel_to_memory.
 */
data class Channel(
    val number: Int,           // 1..198
    var empty: Boolean = true,
    var freqRxHz: Long = 0,
    var freqTxHz: Long = 0,
    var duplex: String = "",    // "", "+", "-", "split"
    var offsetHz: Long = 0,
    var power: String = "1",    // "N/T" or "1".."255"
    var name: String = "",
    var mode: String = "FM",    // Auto, FM, AM, USB
    var bandwidth: String = "Wide",
    var txToneMode: String? = null,  // "Tone", "DTCS", null
    var txToneVal: Double? = null,
    var txTonePolarity: String? = null,
    var rxToneMode: String? = null,
    var rxToneVal: Double? = null,
    var rxTonePolarity: String? = null,
    var group1: String = "None",
    var group2: String = "None",
    var group3: String = "None",
    var group4: String = "None",
    var busyLock: Boolean = false,
    /** Raw flags byte (byte 15 of channel struct). Preserves bits 3-6 (position, pttID, reversed)
     *  so a round-trip write doesn't clobber per-channel flags set by the radio firmware. */
    var flagsRaw: Int = 0,
) {
    fun displayFreq(): String = if (empty) "" else "%.4f".format(freqRxHz / 1_000_000.0)
    fun displayDuplex(): String = when {
        empty -> ""
        duplex == "+" -> "+${offsetHz / 1000}kHz"
        duplex == "-" -> "-${offsetHz / 1000}kHz"
        duplex == "split" -> "Split"
        else -> ""
    }

    fun displayTxTone(): String = formatTone(txToneMode, txToneVal, txTonePolarity)
    fun displayRxTone(): String = formatTone(rxToneMode, rxToneVal, rxTonePolarity)

    /**
     * Same text as the main channel list duplex chip: signed offset in kHz with no unit
     * (e.g. `+5000`, `-600`), or `Split` / `Simplex`.
     */
    fun duplexOffsetChipLabel(): String = when (duplex) {
        "+" -> "+${abs(offsetHz) / 1_000L}"
        "-" -> "-${abs(offsetHz) / 1_000L}"
        "split" -> "Split"
        else -> "Simplex"
    }

    /** Returns active group letters space-separated, e.g. "A B" — empty string if all None. */
    fun displayGroups(): String = listOf(group1, group2, group3, group4)
        .filter { it != "None" }
        .joinToString("  ")

    private fun formatTone(mode: String?, value: Double?, polarity: String?): String = when (mode) {
        "Tone" -> "%.1f Hz".format(value ?: 0.0)
        "DTCS" -> "%03d %s".format((value ?: 0.0).toInt(), polarity ?: "N")
        else   -> ""
    }
}
