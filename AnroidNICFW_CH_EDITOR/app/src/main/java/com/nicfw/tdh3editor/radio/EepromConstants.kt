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

    // VHF/UHF TX-capable bands (Hz) — fallback when no band plan is loaded
    const val VHF_LOW  = 136_000_000
    const val VHF_HIGH = 174_000_000
    const val UHF_LOW  = 400_000_000
    const val UHF_HIGH = 480_000_000

    // Band Plan memory layout (nicFW 2.5)
    // 0x1A00 : u16 magic (must equal 0xA46D)
    // 0x1A02 : bandPlans[20], each entry is 10 bytes:
    //            u32 startFreq (10 Hz units) | u32 endFreq (10 Hz units)
    //            u8  maxPower  | u8 flags  (bit 0 = txAllowed, bit 1 = wrap,
    //                                      bits 2-4 = modulation, bits 5-7 = bandwidth)
    const val BANDPLAN_BASE        = 0x1A00
    const val BANDPLAN_ENTRY_SIZE  = 10          // 4 + 4 + 1 + 1 bytes
    const val BANDPLAN_NUM_ENTRIES = 20
    const val MAGIC_BANDPLAN_V25   = 0xA46D

    // Band Plan editor spinner labels — index == raw value stored in EEPROM.
    // Verified against an actual TD-H3 nicFW 2.5 EEPROM dump + nicFW Programmer screenshot:
    //   • raw 0 = "Ignore"  (entries 9, 20 confirmed)
    //   • raw 1 = "FM"      (entries 1, 2, 4, 5, 6, 7, 8 confirmed)
    //   • raw 2 = "AM"      (entry 3, Air Band, confirmed)
    // Remaining values are logical extrapolations — not seen in sample data.
    //
    // Modulation: bits 2-4 of the flags byte  ((flags shr 2) and 0x07).
    val BANDPLAN_MOD_LABELS = listOf(
        "Ignore",       // raw 0 ✓ confirmed
        "FM",           // raw 1 ✓ confirmed
        "AM",           // raw 2 ✓ confirmed
        "USB",          // raw 3 (unconfirmed — logical)
        "Auto",         // raw 4 (unconfirmed — logical)
        "Enforce FM",   // raw 5 (unconfirmed — logical)
        "Enforce AM",   // raw 6 (unconfirmed — logical)
        "Enforce USB"   // raw 7 (unconfirmed — logical)
    )

    // Bandwidth: bits 5-7 of the flags byte  ((flags shr 5) and 0x07).
    // Verified values:
    //   • raw 0 = "Ignore"   (entry 4 MURS, entry 9 FM broadcast, entry 20 catch-all)
    //   • raw 1 = "Wide"     (entries 1, 2, 5, 6, 7, 8 confirmed)
    //   • raw 2 = "Narrow"   (entry 3 Air Band confirmed)
    //   • raw 5 = "FM Tuner" (entry 9 FM broadcast 88–108 MHz confirmed)
    val BANDPLAN_BW_LABELS = listOf(
        "Ignore",    // raw 0 ✓ confirmed
        "Wide",      // raw 1 ✓ confirmed
        "Narrow",    // raw 2 ✓ confirmed
        "BW (3)",    // raw 3 (unconfirmed — not seen in sample data)
        "BW (4)",    // raw 4 (unconfirmed — not seen in sample data)
        "FM Tuner",  // raw 5 ✓ confirmed
        "BW (6)",    // raw 6 (unconfirmed)
        "BW (7)"     // raw 7 (unconfirmed)
    )

    // Max power: spinner position == raw power byte.  Position 0 → 0 = "Any / no limit".
    val BANDPLAN_MAXPOWER_LIST: List<String> = listOf("Any (0)") + (1..255).map { it.toString() }

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
     * Returns true when [freqHz] falls outside the TD-H3 / nicFW 2.5 transmit-capable bands.
     *
     * The radio supports TX only on VHF 136–174 MHz and UHF 400–480 MHz.
     * Frequencies outside those ranges — FM broadcast (88–108 MHz), aviation (108–136 MHz),
     * etc. — are RX-only regardless of a channel's stored power setting; the radio's
     * Band Plan overrides the power value to N/T on those frequencies.
     */
    fun isTxRestricted(freqHz: Long): Boolean =
        !(freqHz in VHF_LOW..VHF_HIGH || freqHz in UHF_LOW..UHF_HIGH)

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
