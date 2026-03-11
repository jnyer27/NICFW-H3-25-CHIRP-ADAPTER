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

    // ── Radio Settings offsets (nicFW 2.5, block at 0x1900) ──────────────────
    // Main struct (0x1900–0x1966):
    const val RS_SQUELCH         = 0x1902  // u8  0-9
    const val RS_DUAL_WATCH      = 0x1903  // u8  bool
    const val RS_SCRAMBLER_EN    = 0x1905  // u8  bool enable flag (0=Off, 1=enabled)  ✓ confirmed
    const val RS_STEP            = 0x1906  // u16 BE, 10 Hz units (500 = 5.0 kHz)
    const val RS_PTT_MODE        = 0x190C  // u8  0=Dual, 1=Single
    const val RS_TX_MOD_METER    = 0x190D  // u8  bool
    const val RS_MIC_GAIN        = 0x190E  // u8  0-31
    const val RS_TX_DEVIATION    = 0x190F  // u8
    const val RS_BATT_STYLE      = 0x1911  // u8  0=Off,1=Icon,2=Percentage,3=Voltage
    const val RS_SCAN_RANGE      = 0x1912  // u16 BE, × 10 kHz (100 = 1.00 MHz)
    const val RS_SCAN_PERSIST    = 0x1914  // u16 BE, × 0.1 s
    const val RS_SCAN_RESUME     = 0x1916  // u8  seconds
    const val RS_ULTRA_SCAN      = 0x1917  // u8  0-20 (verified on radio)
    const val RS_TONE_MONITOR    = 0x1918  // u8  0=Off,1=On,2=Clone
    const val RS_LCD_BRIGHTNESS  = 0x1919  // u8  0-28
    const val RS_LCD_TIMEOUT     = 0x191A  // u8  seconds, 0=Off
    const val RS_BREATHE         = 0x191B  // u8  seconds, 0=Off
    const val RS_DTMF_DEV        = 0x191C  // u8  DTMF deviation
    const val RS_LCD_GAMMA       = 0x191D  // u8
    const val RS_REPEATER_TONE   = 0x191E  // u16 BE, Hz
    const val RS_BLUETOOTH       = 0x1947  // u8  bool
    const val RS_POWER_SAVE      = 0x1948  // u8  0=Off
    const val RS_KEY_TONES       = 0x1949  // u8  bool
    const val RS_STE             = 0x194A  // u8  SquelchTail Elim, 0=Off
    const val RS_RF_GAIN         = 0x194B  // u8  0=AGC, 1-42
    const val RS_SBAR_STYLE      = 0x194C  // u8  0=Segment
    const val RS_SQ_NOISE_LEV    = 0x194D  // u8  Squelch Noise Level
    const val RS_VOX             = 0x1952  // u8  0=Off, 1-15
    const val RS_VOX_TAIL        = 0x1953  // u16 BE, × 0.1 s (20 = 2.0 s)
    const val RS_TX_TIMEOUT      = 0x1955  // u8  seconds, 0=Off
    const val RS_LCD_DIM         = 0x1956  // u8  0=Off
    const val RS_DTMF_SPEED      = 0x1957  // u8
    const val RS_NOISE_GATE      = 0x1958  // u8
    const val RS_SCAN_UPDATE     = 0x1959  // u8  × 0.1 s
    const val RS_ASL             = 0x195A  // u8  AllStarLink, 0=Off
    const val RS_DISABLE_FMT     = 0x195B  // u8  bool, Disable FM Tuner
    const val RS_PIN             = 0x195C  // u16 BE, 0-9999
    const val RS_PIN_ACTION      = 0x195E  // u8  0=Off,1=Lock,2=Unlock
    const val RS_LCD_INVERTED    = 0x195F  // u8  bool
    const val RS_AF_FILTERS      = 0x1960  // u8  0-7 (see RS_AF_FILTERS_LIST)
    const val RS_IF_FREQ         = 0x1961  // u8  0=default (8.46 kHz display)
    const val RS_SBAR_PERSISTENT = 0x1962  // u8  bool  Persistent S-Bar
    const val RS_VFO_LOCK_ACTIVE = 0x1964  // u8  bool  DualWatch VFO Lock
    const val RS_DW_DELAY        = 0x1965  // u8  DualWatch Start Delay, seconds
    const val RS_SUBTONE_DEV     = 0x1966  // u8  Squelch Tone Deviation
    // Filler area (0x1967–0x197F) — offsets derived from EEPROM analysis:
    const val RS_SHOW_XMIT_CURR  = 0x1967  // u8  bool  ✓ confirmed (0→1 when Show Xmit Current=On)
    const val RS_AGC0            = 0x1968  // u8  AGC Table 0  ✓ confirmed (value=24)
    const val RS_AGC1            = 0x1969  // u8  AGC Table 1  ✓ confirmed (value=32)
    const val RS_AGC2            = 0x196A  // u8  AGC Table 2  ✓ confirmed (value=37)
    const val RS_AGC3            = 0x196B  // u8  AGC Table 3  ✓ confirmed (value=40)
    const val RS_RFI_COMP        = 0x196C  // u8  0=Off  ✓ confirmed (set to 1, observed in EEPROM)
    const val RS_SCRAMBLER_FREQ  = 0x196D  // u8  freq index: 0=3300Hz(UI8), 1-7=2600-3200Hz(UI1-7)  ✓ confirmed; UI9-10 encoding unverified
    const val RS_DTMF_DECODE     = 0x196E  // u8  0=Off,1=Always,2=Squelched  ✓ confirmed (0→1 when DTMF Decode=Always)
    // 0x196F = unknown (1 byte gap); RS_RX_FILTER_TRANS location unconfirmed — removed from UI pending new dump
    const val RS_TX_FILTER_TRANS = 0x1970  // u16 BE, 0=default (280 MHz)  ⚠ unconfirmed
    const val RS_DTMF_SEQ_PAUSE  = 0x1972  // u8  × 0.1 s  ✓ confirmed (10=1.0 s)
    const val RS_NOISE_CEILING   = 0x1973  // u8  ✓ confirmed (value=55)

    // Radio Settings enum lists ─────────────────────────────────────────────
    // Scrambler: index == scramblerIf field (0=Off, 1=2600Hz … 10=3500Hz).
    // EEPROM encoding uses two fields: RS_SCRAMBLER_EN (enable) + RS_SCRAMBLER_FREQ (index).
    // Freq index mapping: UI1-7 → raw1-7; UI8(3300Hz) → raw0; UI9-10 → raw8-9 (unverified).
    val RS_SCRAMBLER_LABELS  = listOf(
        "Off",
        "2600 Hz", "2700 Hz", "2800 Hz", "2900 Hz",
        "3000 Hz", "3100 Hz", "3200 Hz",
        "3300 Hz", "3400 Hz", "3500 Hz"
    )
    val RS_DTMF_DECODE_LIST  = listOf("Off", "Always", "Squelched")
    val RS_BATT_STYLE_LIST   = listOf("Off", "Icon", "Percent", "Volts")
    val RS_TONE_MONITOR_LIST = listOf("Off", "On", "Clone")
    val RS_PTT_MODE_LIST     = listOf("Dual", "Single", "Hybrid")
    val RS_PIN_ACTION_LIST   = listOf("Off", "On", "Power On")
    val RS_AF_FILTERS_LIST   = listOf(
        "All", "Band Pass Only", "De-Emph + HP", "High Pass",
        "De-Emph + LP", "Low Pass", "De-Emph Only", "None", "No RX/TX Filter"
    )
    val RS_STE_LIST          = listOf("Off", "RX", "TX", "Both")
    val RS_SBAR_STYLE_LIST   = listOf("Segment", "Stepped", "Solid")
    val RS_IF_FREQ_LIST      = listOf(
        "8.46 kHz", "7.25 kHz", "6.35 kHz", "5.64 kHz",
        "5.08 kHz", "4.62 kHz", "4.23 kHz"
    )
    val RS_ASL_LIST          = listOf("Off", "COS", "USB", "I-COS")
    val RS_KEY_TONES_LIST    = listOf("Off", "On", "Differential", "Voice")
    // RFi Comp: 0 = Off, 1–30 = numeric level. Spinner index == raw EEPROM value.
    val RS_RFI_COMP_LIST     = listOf("Off") + (1..30).map { it.toString() }
    // RF Gain: 0 = AGC (automatic), 1–42 = manual gain. Spinner index == raw EEPROM value.
    val RS_RF_GAIN_LIST      = listOf("AGC") + (1..42).map { it.toString() }
    // Power Save: 0 = Off, 1–20 = level. Spinner index == raw EEPROM value.
    val RS_POWER_SAVE_LIST   = listOf("Off") + (1..20).map { it.toString() }
    // Step: display label → raw u16 value in 10 Hz units
    val RS_STEP_LABELS = listOf("2.5 kHz", "5.0 kHz", "6.25 kHz", "12.5 kHz", "25.0 kHz", "50.0 kHz")
    val RS_STEP_RAW    = listOf(250, 500, 625, 1250, 2500, 5000)
    val RS_SQUELCH_LIST      = (0..9).map { it.toString() }

    // Group label table: 15 entries (A–O) × 6 bytes ASCII at 0x1C90
    const val GROUP_LABELS_BASE = 0x1C90
    const val GROUP_LABEL_SIZE  = 6

    // ── Tune Settings (per-radio calibration) at 0x1DFB ──────────────────────
    // Struct layout (5 bytes total; matches nicFW Programmer "Tuning2" tab):
    //   0x1DFB  i8  xtal671             Crystal calibration (-128 to 127)
    //   0x1DFC  u8  maxPowerWattsUHF    Max UHF power in 0.1W units (display only)
    //   0x1DFD  u8  maxPowerSettingUHF  Max UHF raw power setting (enforced by radio)
    //   0x1DFE  u8  maxPowerWattsVHF    Max VHF power in 0.1W units (display only)
    //   0x1DFF  u8  maxPowerSettingVHF  Max VHF raw power setting (enforced by radio)
    //
    // These are per-radio calibration values; cloning an EEPROM from another
    // radio copies them. The radio silently clamps channel TX power to the cap
    // at transmit time — this does NOT change the stored channel power byte.
    const val TUNE_SETTINGS_BASE = 0x1DFB

    /** Frequency boundary (Hz) separating VHF from UHF for power cap checks. */
    const val VHF_UHF_BOUNDARY_HZ = 300_000_000L

    // VHF/UHF TX-capable bands (Hz) — fallback when no band plan is loaded
    const val VHF_LOW  = 136_000_000
    const val VHF_HIGH = 174_000_000
    const val UHF_LOW  = 400_000_000
    const val UHF_HIGH = 480_000_000

    // ── Scan Preset memory layout (nicFW 2.5) ──────────────────────────────
    // 0x1B00 : scanPresets[20], each entry is 20 bytes (NO magic header):
    //            u32 startFreq (10 Hz units; 0 = empty slot)
    //            u16 span      (10 kHz units; endFreq = startFreq + span×10kHz)
    //            u16 step      (10 Hz units)
    //            u8  scanResume
    //            u8  scanPersist
    //            u8  flags     (bits[4:2]=ultrascan 0–7, bits[1:0]=modulation)
    //                          modulation: 0=FM, 1=AM, 2=USB, 3=Auto
    //            u8[8] label   (ASCII, space-padded, 8 chars)
    //            u8  null      (always 0x00)
    const val SCANPRESET_BASE        = 0x1B00
    const val SCANPRESET_ENTRY_SIZE  = 20
    const val SCANPRESET_NUM_ENTRIES = 20

    // Modulation encoding for scan presets (different from channel list).
    // Index == raw EEPROM value: 0=FM, 1=AM, 2=USB, 3=Auto.
    // Verified against live EEPROM dump: FM entries raw=0, AM entries raw=1.
    val SCANPRESET_MOD_LABELS = listOf("FM", "AM", "USB", "Auto")

    // Ultrascan speed labels (0–7); spinner index == raw EEPROM value.
    val SCANPRESET_ULTRASCAN_LABELS = (0..7).map { it.toString() }

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

    // Max power: spinner position == raw power byte.  Position 0 → 0 = "Ignore".
    val BANDPLAN_MAXPOWER_LIST: List<String> = listOf("Ignore") + (1..255).map { it.toString() }

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
