package com.nicfw.tdh3editor.radio

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

    private fun formatTone(mode: String?, value: Double?, polarity: String?): String = when (mode) {
        "Tone" -> "%.1f Hz".format(value ?: 0.0)
        "DTCS" -> "%03d %s".format((value ?: 0.0).toInt(), polarity ?: "N")
        else   -> ""
    }
}
