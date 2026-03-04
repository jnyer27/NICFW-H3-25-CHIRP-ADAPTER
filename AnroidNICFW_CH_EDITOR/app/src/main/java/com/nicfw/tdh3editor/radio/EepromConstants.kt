package com.nicfw.tdh3editor.radio

/**
 * Constants matching tidradio_h3_nicfw25.py (MODULATION_LIST, BANDWIDTH_LIST, etc.)
 */
object EepromConstants {

    val MODULATION_LIST = listOf("Auto", "FM", "AM", "USB")
    val BANDWIDTH_LIST = listOf("Wide", "Narrow")
    val GROUPS_LIST = listOf("None", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O")
    val GROUP_LETTERS = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O")

    const val TXPower_NT = 0
    val POWERLEVEL_LIST = listOf("N/T") + (1..255).map { it.toString() }

    const val MAGIC_SETTINGS_V25 = 0xD82F
    const val CHANNEL_BASE = 0x0040
    const val CHANNEL_STRUCT_SIZE = 32
    const val NUM_CHANNELS = 198
    const val SETTINGS_BASE = 0x1900

    // VHF/UHF bands (Hz)
    const val VHF_LOW = 136_000_000
    const val VHF_HIGH = 174_000_000
    const val UHF_LOW = 400_000_000
    const val UHF_HIGH = 480_000_000

    // Tone mode selector
    val TONE_MODE_LIST = listOf("None", "Tone", "DTCS")
    val DCS_POLARITY_LIST = listOf("N", "R")

    // Standard CTCSS tones (Hz) — matches chirp_common.CHIRP_TONES
    val CTCSS_TONES = listOf(
        67.0,  71.9,  74.4,  77.0,  79.7,  82.5,  85.4,  88.5,
        91.5,  94.8,  97.4, 100.0, 103.5, 107.2, 110.9, 114.8,
       118.8, 123.0, 127.3, 131.8, 136.5, 141.3, 146.2, 151.4,
       156.7, 162.2, 167.9, 173.8, 179.9, 186.2, 192.8, 203.5,
       210.7, 218.1, 225.7, 233.6, 241.8, 250.3
    )
    val CTCSS_TONE_LABELS: List<String> = CTCSS_TONES.map { "%.1f".format(it) }

    // Standard DCS codes — matches chirp_common.DTCS_CODES
    val DCS_CODES = listOf(
         23,  25,  26,  31,  32,  33,  35,  36,  43,  47,  51,  53,  54,
         65,  71,  72,  73,  74, 114, 115, 116, 122, 125, 131, 132, 134,
        143, 145, 152, 155, 156, 162, 165, 172, 174, 205, 212, 223, 225,
        226, 243, 244, 245, 246, 251, 252, 255, 261, 263, 265, 266, 271,
        274, 306, 311, 315, 325, 331, 332, 343, 346, 351, 356, 364, 365,
        371, 411, 412, 413, 423, 431, 432, 445, 446, 452, 454, 455, 462,
        464, 465, 466, 503, 506, 516, 523, 526, 532, 546, 565, 606, 612,
        624, 627, 631, 632, 654, 662, 664, 703, 712, 723, 731, 732, 734,
        743, 754
    )
    val DCS_CODE_LABELS: List<String> = DCS_CODES.map { "%03d".format(it) }

    // ── Flat tone picker ─────────────────────────────────────────────────────
    // Layout: [0] None  [1..38] CTCSS  [39..142] DCS-N  [143..246] DCS-R
    // Total: 1 + 38 + 104 + 104 = 247 items
    val TONE_LABELS: List<String> = buildList {
        add("None")
        CTCSS_TONES.forEach { add("CTCSS %.1f Hz".format(it)) }
        DCS_CODES.forEach  { add("DCS %03d N".format(it)) }
        DCS_CODES.forEach  { add("DCS %03d R".format(it)) }
    }

    /**
     * Convert a decoded (mode, value, polarity) triple to a flat TONE_LABELS index.
     * Returns 0 (None) if the tone is unrecognised.
     */
    fun toneToIndex(mode: String?, value: Double?, polarity: String?): Int = when (mode) {
        "Tone" -> {
            val i = CTCSS_TONES.indexOfFirst { kotlin.math.abs(it - (value ?: 0.0)) < 0.05 }
            if (i < 0) 0 else 1 + i
        }
        "DTCS" -> {
            val code = value?.toInt() ?: 0
            val i = DCS_CODES.indexOf(code)
            if (i < 0) 0
            else if (polarity == "R") 1 + CTCSS_TONES.size + DCS_CODES.size + i
            else 1 + CTCSS_TONES.size + i
        }
        else -> 0
    }

    /**
     * Convert a flat TONE_LABELS index back to (mode, value, polarity).
     * Returns (null, null, null) for index 0 (None).
     */
    fun indexToTone(idx: Int): Triple<String?, Double?, String?> {
        val ctcssEnd = 1 + CTCSS_TONES.size          // 39
        val dcsNEnd  = ctcssEnd + DCS_CODES.size      // 143
        val dcsREnd  = dcsNEnd  + DCS_CODES.size      // 247
        return when {
            idx <= 0       -> Triple(null, null, null)
            idx < ctcssEnd -> Triple("Tone", CTCSS_TONES[idx - 1], null)
            idx < dcsNEnd  -> Triple("DTCS", DCS_CODES[idx - ctcssEnd].toDouble(), "N")
            idx < dcsREnd  -> Triple("DTCS", DCS_CODES[idx - dcsNEnd].toDouble(),  "R")
            else           -> Triple(null, null, null)
        }
    }
}
