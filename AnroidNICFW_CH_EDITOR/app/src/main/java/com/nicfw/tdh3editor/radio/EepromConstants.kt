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

    // Group label table: 15 entries (A–O) × 6 bytes ASCII at 0x1C90
    const val GROUP_LABELS_BASE = 0x1C90
    const val GROUP_LABEL_SIZE  = 6

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
     * Converts a stored [Channel.power] string to a human-readable wattage label
     * for display in the channel list.  Storage is unchanged (raw byte 0–255 / "N/T").
     *
     * Calibration points provided by the radio hardware:
     *   raw   0  →  N/T  (no transmit)
     *   raw  29  →  0.5 W
     *   raw  58  →  2.0 W
     *   raw 130  →  5.0 W
     *
     * Values between calibration points are linearly interpolated within the
     * appropriate segment.  Values above 130 are extrapolated from the top segment.
     * Result is formatted to one decimal place (e.g. "2.3W").
     */
    fun powerToWatts(powerStr: String): String {
        if (powerStr == "N/T") return "N/T"
        val raw = powerStr.toIntOrNull() ?: return powerStr
        if (raw <= 0) return "N/T"

        // Calibration segments as (rawValue, watts) pairs
        val pts = arrayOf(0 to 0.0, 29 to 0.5, 58 to 2.0, 130 to 5.0)

        for (i in 0 until pts.size - 1) {
            val (v1, w1) = pts[i]
            val (v2, w2) = pts[i + 1]
            if (raw <= v2) {
                val t = (raw - v1).toDouble() / (v2 - v1)
                return "%.1fW".format(w1 + t * (w2 - w1))
            }
        }

        // Above 130: extrapolate from the 58→130 segment (2W→5W over 72 units)
        val t = (raw - 58).toDouble() / (130 - 58)
        return "%.1fW".format((2.0 + t * 3.0).coerceAtMost(99.9))
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
