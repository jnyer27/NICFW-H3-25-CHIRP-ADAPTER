package com.nicfw.tdh3editor.radio

/**
 * Parse and build EEPROM channel data (V2.5 big-endian). Mirrors _channel_to_memory / _memory_to_channel.
 */
object EepromParser {

    private const val CHAN_SIZE = EepromConstants.CHANNEL_STRUCT_SIZE
    private val MOD = EepromConstants.MODULATION_LIST
    private val BW = EepromConstants.BANDWIDTH_LIST
    private val GROUPS = EepromConstants.GROUPS_LIST

    fun parseChannel(eeprom: ByteArray, channelNumber: Int): Channel {
        require(channelNumber in 1..EepromConstants.NUM_CHANNELS)
        val index = channelNumber - 1
        val offset = EepromConstants.CHANNEL_BASE + index * CHAN_SIZE
        require(offset + CHAN_SIZE <= eeprom.size)
        val slice = eeprom.copyOfRange(offset, offset + CHAN_SIZE)
        return parseChannelFromSlice(slice, channelNumber)
    }

    internal fun parseChannelFromSlice(slice: ByteArray, number: Int): Channel {
        val emptyMarker = slice[0].toInt() and 0xFF == 0xFF &&
            slice[1].toInt() and 0xFF == 0xFF &&
            slice[2].toInt() and 0xFF == 0xFF &&
            slice[3].toInt() and 0xFF == 0xFF
        val rxFreq10 = readU32Be(slice, 0)
        val txFreq10 = readU32Be(slice, 4)
        if (emptyMarker || (rxFreq10 == 0L && txFreq10 == 0L)) {
            return Channel(number = number, empty = true)
        }
        val rxHz = rxFreq10 * 10
        val txHz = txFreq10 * 10
        val duplex = when {
            rxFreq10 == txFreq10 -> ""
            txFreq10 > rxFreq10 -> "+"
            else -> "-"
        }
        val offsetHz = kotlin.math.abs(rxHz - txHz)
        val txPower = slice[12].toInt() and 0xFF
        val power = if (txPower == EepromConstants.TXPower_NT) "N/T" else txPower.toString()
        val name = slice.copyOfRange(20, 32).toString(Charsets.US_ASCII)
            .replace("\u0000", "").replace("\u00FF", "").trim()
        val flags = slice[15].toInt() and 0xFF
        val modulation = (flags shr 1) and 3
        val mode = MOD.getOrElse(modulation) { "FM" }
        val bandwidthBit = flags and 1
        val bandwidth = if (bandwidthBit != 0) "Narrow" else "Wide"
        val rxSubTone = readU16Be(slice, 8)
        val txSubTone = readU16Be(slice, 10)
        val (rxMode, rxVal, rxPol) = ToneCodec.decode(rxSubTone)
        val (txMode, txVal, txPol) = ToneCodec.decode(txSubTone)
        val groups = readU16Be(slice, 13).toInt()
        val g0 = (groups shr 0) and 0xF
        val g1 = (groups shr 4) and 0xF
        val g2 = (groups shr 8) and 0xF
        val g3 = (groups shr 12) and 0xF
        return Channel(
            number = number,
            empty = false,
            freqRxHz = rxHz,
            freqTxHz = txHz,
            duplex = duplex,
            offsetHz = offsetHz,
            power = power,
            name = name,
            mode = mode,
            bandwidth = bandwidth,
            txToneMode = txMode,
            txToneVal = txVal,
            txTonePolarity = txPol,
            rxToneMode = rxMode,
            rxToneVal = rxVal,
            rxTonePolarity = rxPol,
            group1 = GROUPS.getOrElse(g0) { "None" },
            group2 = GROUPS.getOrElse(g1) { "None" },
            group3 = GROUPS.getOrElse(g2) { "None" },
            group4 = GROUPS.getOrElse(g3) { "None" },
        )
    }

    fun writeChannel(eeprom: ByteArray, channel: Channel) {
        require(channel.number in 1..EepromConstants.NUM_CHANNELS)
        val index = channel.number - 1
        val offset = EepromConstants.CHANNEL_BASE + index * CHAN_SIZE
        require(offset + CHAN_SIZE <= eeprom.size)
        val slice = ByteArray(CHAN_SIZE)
        if (channel.empty) {
            slice[0] = 0xFF.toByte()
            slice[1] = 0xFF.toByte()
            slice[2] = 0xFF.toByte()
            slice[3] = 0xFF.toByte()
            for (i in 4 until CHAN_SIZE) slice[i] = 0
        } else {
            val rxFreq10 = channel.freqRxHz / 10
            val txFreq10 = when (channel.duplex) {
                "split" -> channel.freqTxHz / 10
                "+" -> (channel.freqRxHz + channel.offsetHz) / 10
                "-" -> (channel.freqRxHz - channel.offsetHz) / 10
                else -> channel.freqRxHz / 10
            }
            writeU32Be(slice, 0, rxFreq10)
            writeU32Be(slice, 4, txFreq10)
            val txPower = if (channel.power == "N/T") EepromConstants.TXPower_NT
                else channel.power.toIntOrNull()?.coerceIn(1, 255) ?: 1
            slice[12] = txPower.toByte()
            val txSub = ToneCodec.encodeTone(channel.txToneMode, channel.txToneVal, channel.txTonePolarity)
            val rxSub = ToneCodec.encodeTone(channel.rxToneMode, channel.rxToneVal, channel.rxTonePolarity)
            writeU16Be(slice, 8, rxSub)
            writeU16Be(slice, 10, txSub)
            val g0 = GROUPS.indexOf(channel.group1).coerceIn(0, 15)
            val g1 = GROUPS.indexOf(channel.group2).coerceIn(0, 15)
            val g2 = GROUPS.indexOf(channel.group3).coerceIn(0, 15)
            val g3 = GROUPS.indexOf(channel.group4).coerceIn(0, 15)
            writeU16Be(slice, 13, g0 or (g1 shl 4) or (g2 shl 8) or (g3 shl 12))
            val modIndex = MOD.indexOf(channel.mode).coerceIn(0, 3)
            val bwBit = if (channel.bandwidth == "Narrow") 1 else 0
            slice[15] = (bwBit or (modIndex shl 1)).toByte()
            val nameBytes = channel.name.take(12).padEnd(12, ' ').map { it.code.toByte() }.toByteArray()
            nameBytes.copyInto(slice, 20)
        }
        slice.copyInto(eeprom, offset)
    }

    fun parseAllChannels(eeprom: ByteArray): List<Channel> =
        (1..EepromConstants.NUM_CHANNELS).map { parseChannel(eeprom, it) }

    /**
     * Reads the 20 Band Plan entries from 0x1A00 (magic) / 0x1A02 (entries).
     *
     * Returns an empty list when the magic word is absent or invalid — the caller
     * should fall back to the hard-coded VHF/UHF TX-band constants in that case.
     * Empty slots (startFreq=0, endFreq=0) are skipped; use [readAllBandPlanSlots]
     * to get all 20 slots including empty ones.
     */
    fun parseBandPlan(eeprom: ByteArray): List<BandPlanEntry> =
        readAllBandPlanSlots(eeprom)?.filterNot { it.isEmpty } ?: emptyList()

    /**
     * Returns all 20 Band Plan slots as an Array<BandPlanEntry> (index 0–19),
     * or null when the magic word is absent/invalid.
     *
     * Empty slots are included as [BandPlanEntry.isEmpty] == true entries so that
     * the Band Plan editor can show and modify all 20 positions.
     *
     * Entry layout (10 bytes each):
     *   u32 startFreq (10 Hz units)  |  u32 endFreq (10 Hz units)
     *   u8  maxPower                 |  u8  flags
     *      flags: bit 0 = txAllowed, bit 1 = wrap,
     *             bits 2–4 = modulation (raw 0–7), bits 5–7 = bandwidth (raw 0–7)
     */
    fun readAllBandPlanSlots(eeprom: ByteArray): Array<BandPlanEntry>? {
        val magicOff = EepromConstants.BANDPLAN_BASE
        if (magicOff + 2 > eeprom.size) return null
        val magic = readU16Be(eeprom, magicOff)
        if (magic != EepromConstants.MAGIC_BANDPLAN_V25) return null

        val entryBase = magicOff + 2
        val entrySize = EepromConstants.BANDPLAN_ENTRY_SIZE
        return Array(EepromConstants.BANDPLAN_NUM_ENTRIES) { i ->
            val off = entryBase + i * entrySize
            if (off + entrySize > eeprom.size) {
                BandPlanEntry(startHz = 0L, endHz = 0L, txAllowed = false)
            } else {
                val startFreq10 = readU32Be(eeprom, off)
                val endFreq10   = readU32Be(eeprom, off + 4)
                val maxPower    = eeprom[off + 8].toInt() and 0xFF
                val flags       = eeprom[off + 9].toInt() and 0xFF
                BandPlanEntry(
                    startHz   = startFreq10 * 10L,
                    endHz     = endFreq10   * 10L,
                    txAllowed = (flags and 0x01) != 0,
                    maxPower  = maxPower,
                    wrap      = (flags and 0x02) != 0,
                    modRaw    = (flags shr 2) and 0x07,
                    bwRaw     = (flags shr 5) and 0x07
                )
            }
        }
    }

    /**
     * Writes all 20 Band Plan slots back into [eeprom].
     *
     * Always writes the magic word (0xA46D) at 0x1A00 first so that a previously
     * unprogrammed EEPROM becomes valid after the first editor save.
     * Slots beyond the end of [entries] are zeroed out.
     */
    fun writeBandPlan(eeprom: ByteArray, entries: Array<BandPlanEntry>) {
        val magicOff  = EepromConstants.BANDPLAN_BASE
        if (magicOff + 2 > eeprom.size) return
        writeU16Be(eeprom, magicOff, EepromConstants.MAGIC_BANDPLAN_V25)

        val entryBase = magicOff + 2
        val entrySize = EepromConstants.BANDPLAN_ENTRY_SIZE

        for (i in 0 until EepromConstants.BANDPLAN_NUM_ENTRIES) {
            val off = entryBase + i * entrySize
            if (off + entrySize > eeprom.size) break

            val entry = entries.getOrNull(i)
            if (entry == null || entry.isEmpty) {
                for (j in 0 until entrySize) eeprom[off + j] = 0
            } else {
                writeU32Be(eeprom, off,     entry.startHz / 10L)
                writeU32Be(eeprom, off + 4, entry.endHz   / 10L)
                eeprom[off + 8] = (entry.maxPower and 0xFF).toByte()
                val flags = ((if (entry.txAllowed) 1 else 0)) or
                            ((if (entry.wrap)      1 else 0) shl 1) or
                            ((entry.modRaw and 0x07) shl 2) or
                            ((entry.bwRaw  and 0x07) shl 5)
                eeprom[off + 9] = flags.toByte()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scan Presets  (0x1B00, 20 entries × 20 bytes, no magic header)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns non-empty Scan Preset entries parsed from 0x1B00.
     * Empty slots (startFreq == 0) are excluded. Use [readAllScanPresetSlots]
     * to get all 20 positions including empty ones.
     */
    fun parseScanPresets(eeprom: ByteArray): List<ScanPresetEntry> =
        readAllScanPresetSlots(eeprom).filterNot { it.isEmpty }

    /**
     * Returns all 20 Scan Preset slots as an Array<ScanPresetEntry> (index 0–19).
     * Empty slots are included as [ScanPresetEntry.isEmpty] == true entries so the
     * Scan Preset editor can show and modify all 20 positions.
     *
     * Entry layout (20 bytes each):
     *   u32 startFreq (10 Hz units; 0 = empty slot)
     *   u16 span      (10 kHz units; endFreq = startFreq + span×10kHz)
     *   u16 step      (10 Hz units)
     *   u8  scanResume
     *   u8  scanPersist
     *   u8  flags     — bits[4:2]=ultrascan(0–7), bits[1:0]=modulation(0=FM,1=AM,2=USB,3=Auto)
     *   u8[8] label   — ASCII, space-padded (8 bytes)
     *   u8  null      — always 0x00
     */
    fun readAllScanPresetSlots(eeprom: ByteArray): Array<ScanPresetEntry> {
        val base      = EepromConstants.SCANPRESET_BASE
        val entrySize = EepromConstants.SCANPRESET_ENTRY_SIZE
        return Array(EepromConstants.SCANPRESET_NUM_ENTRIES) { i ->
            val off = base + i * entrySize
            if (off + entrySize > eeprom.size) {
                ScanPresetEntry()   // empty sentinel
            } else {
                val startFreq10 = readU32Be(eeprom, off)         // 10 Hz units
                val span        = readU16Be(eeprom, off + 4)     // 10 kHz units
                val step10      = readU16Be(eeprom, off + 6)     // 10 Hz units
                val resume      = eeprom[off + 8].toInt() and 0xFF
                val persist     = eeprom[off + 9].toInt() and 0xFF
                val flags       = eeprom[off + 10].toInt() and 0xFF
                val ultrascan   = (flags shr 2) and 0x07
                val modRaw      = flags and 0x03
                val labelBytes  = eeprom.copyOfRange(off + 11, off + 19)
                val label       = labelBytes.toString(Charsets.US_ASCII)
                    .replace("\u0000", " ")
                    .replace("\u00FF", " ")
                    .trim()
                ScanPresetEntry(
                    startHz     = startFreq10 * 10L,
                    endHz       = (startFreq10 + span.toLong() * 1000L) * 10L,
                    stepHz      = step10 * 10,
                    scanResume  = resume,
                    scanPersist = persist,
                    modRaw      = modRaw,
                    ultrascan   = ultrascan,
                    label       = label
                )
            }
        }
    }

    /**
     * Writes all 20 Scan Preset slots back into [eeprom] at 0x1B00.
     * There is no magic header — slots start directly at 0x1B00.
     * Null or empty entries in [entries] are written as 20 zero bytes.
     */
    fun writeScanPresets(eeprom: ByteArray, entries: Array<ScanPresetEntry>) {
        val base      = EepromConstants.SCANPRESET_BASE
        val entrySize = EepromConstants.SCANPRESET_ENTRY_SIZE

        for (i in 0 until EepromConstants.SCANPRESET_NUM_ENTRIES) {
            val off = base + i * entrySize
            if (off + entrySize > eeprom.size) break

            val entry = entries.getOrNull(i) ?: ScanPresetEntry()
            if (entry.isEmpty) {
                for (j in 0 until entrySize) eeprom[off + j] = 0
            } else {
                val startFreq10 = entry.startHz / 10L
                val endFreq10   = entry.endHz   / 10L
                val span        = ((endFreq10 - startFreq10) / 1000L)
                    .coerceIn(0L, 65535L).toInt()               // 10 kHz units, u16
                val step10      = (entry.stepHz / 10)
                    .coerceIn(0, 65535)                          // 10 Hz units, u16
                val flags       = ((entry.ultrascan and 0x07) shl 2) or (entry.modRaw and 0x03)
                val labelPadded = entry.label.take(8).padEnd(8, ' ')

                writeU32Be(eeprom, off,     startFreq10)
                writeU16Be(eeprom, off + 4, span)
                writeU16Be(eeprom, off + 6, step10)
                eeprom[off + 8]  = (entry.scanResume  and 0xFF).toByte()
                eeprom[off + 9]  = (entry.scanPersist and 0xFF).toByte()
                eeprom[off + 10] = flags.toByte()
                for (j in 0 until 8) eeprom[off + 11 + j] = labelPadded[j].code.toByte()
                eeprom[off + 19] = 0   // null terminator
            }
        }
    }

    /**
     * Reads the 15 group labels (A–O) from 0x1C90.
     * Each label is 6 bytes of null-padded ASCII. Returns a 15-element list of trimmed
     * strings; blank labels become empty string "".
     */
    fun parseGroupLabels(eeprom: ByteArray): List<String> {
        val base = EepromConstants.GROUP_LABELS_BASE
        val size = EepromConstants.GROUP_LABEL_SIZE
        return (0 until 15).map { i ->
            val off = base + i * size
            if (off + size > eeprom.size) return@map ""
            val raw = eeprom.copyOfRange(off, off + size)
            raw.toString(Charsets.US_ASCII)
                .replace("\u0000", " ")
                .replace("\u00FF", " ")
                .trim()
        }
    }

    /**
     * Writes 15 group labels back into the EEPROM buffer at 0x1C90.
     * Each label is truncated to 6 chars and null-padded to exactly 6 bytes.
     */
    fun writeGroupLabels(eeprom: ByteArray, labels: List<String>) {
        val base = EepromConstants.GROUP_LABELS_BASE
        val size = EepromConstants.GROUP_LABEL_SIZE
        for (i in 0 until 15) {
            val off = base + i * size
            if (off + size > eeprom.size) break
            val raw = (labels.getOrNull(i) ?: "").take(size).padEnd(size, '\u0000')
            for (j in 0 until size) eeprom[off + j] = raw[j].code.toByte()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tune Settings  (0x1DFB — per-radio calibration)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Reads the 5-byte Tune Settings block at 0x1DFB:
     *   [0] i8  xtal671             (-128 to 127)
     *   [1] u8  maxPowerWattsUHF    (0.1W units, display only)
     *   [2] u8  maxPowerSettingUHF  (raw cap enforced at TX time)
     *   [3] u8  maxPowerWattsVHF    (0.1W units, display only)
     *   [4] u8  maxPowerSettingVHF  (raw cap enforced at TX time)
     *
     * Returns default [TuneSettings] (all uncapped) if the offset is out of range.
     */
    fun readTuneSettings(eeprom: ByteArray): com.nicfw.tdh3editor.TuneSettings {
        val base = EepromConstants.TUNE_SETTINGS_BASE
        if (base + 5 > eeprom.size) return com.nicfw.tdh3editor.TuneSettings()
        return com.nicfw.tdh3editor.TuneSettings(
            xtal671            = eeprom[base].toInt(),          // i8 — Kotlin sign-extends
            maxPowerWattsUHF   = eeprom[base + 1].toInt() and 0xFF,
            maxPowerSettingUHF = eeprom[base + 2].toInt() and 0xFF,
            maxPowerWattsVHF   = eeprom[base + 3].toInt() and 0xFF,
            maxPowerSettingVHF = eeprom[base + 4].toInt() and 0xFF,
        )
    }

    /** Reads all fields of the nicFW 2.5 Settings block at 0x1900. */
    fun readRadioSettings(eeprom: ByteArray): com.nicfw.tdh3editor.RadioSettings {
        fun u8(off: Int)  = eeprom[off].toInt() and 0xFF
        fun u16(off: Int) = ((eeprom[off].toInt() and 0xFF) shl 8) or (eeprom[off + 1].toInt() and 0xFF)
        fun bool(off: Int) = (eeprom[off].toInt() and 0xFF) != 0
        val C = EepromConstants
        return com.nicfw.tdh3editor.RadioSettings(
            squelch         = u8(C.RS_SQUELCH),
            sqNoiseLev      = u8(C.RS_SQ_NOISE_LEV),
            noiseCeiling    = u8(C.RS_NOISE_CEILING),
            ste             = u8(C.RS_STE),
            micGain         = u8(C.RS_MIC_GAIN),
            noiseGate       = u8(C.RS_NOISE_GATE),
            rfGain          = u8(C.RS_RF_GAIN),
            lcdBrightness   = u8(C.RS_LCD_BRIGHTNESS),
            lcdTimeout      = u8(C.RS_LCD_TIMEOUT),
            breathe         = u8(C.RS_BREATHE),
            lcdDim          = u8(C.RS_LCD_DIM),
            lcdGamma        = u8(C.RS_LCD_GAMMA),
            lcdInverted     = bool(C.RS_LCD_INVERTED),
            sBarStyle       = u8(C.RS_SBAR_STYLE),
            sBarPersistent  = bool(C.RS_SBAR_PERSISTENT),
            txModMeter      = bool(C.RS_TX_MOD_METER),
            txTimeout       = u8(C.RS_TX_TIMEOUT),
            txDeviation     = u8(C.RS_TX_DEVIATION),
            pttMode         = u8(C.RS_PTT_MODE),
            txFilterTrans   = u16(C.RS_TX_FILTER_TRANS),
            showXmitCurrent = bool(C.RS_SHOW_XMIT_CURR),
            repeaterTone    = u16(C.RS_REPEATER_TONE),
            step            = u16(C.RS_STEP),
            afFilters       = u8(C.RS_AF_FILTERS),
            ifFreq          = u8(C.RS_IF_FREQ),
            rfiComp         = u8(C.RS_RFI_COMP),
            agc0            = u8(C.RS_AGC0),
            agc1            = u8(C.RS_AGC1),
            agc2            = u8(C.RS_AGC2),
            agc3            = u8(C.RS_AGC3),
            amAgcFix        = bool(C.RS_AM_AGC_FIX),
            dualWatch       = bool(C.RS_DUAL_WATCH),
            scanResume      = u8(C.RS_SCAN_RESUME),
            scanRange       = u16(C.RS_SCAN_RANGE),
            scanPersist     = u16(C.RS_SCAN_PERSIST),
            scanUpdate      = u8(C.RS_SCAN_UPDATE),
            ultraScan       = u8(C.RS_ULTRA_SCAN),
            vox             = u8(C.RS_VOX),
            voxTail         = u16(C.RS_VOX_TAIL),
            toneMonitor     = u8(C.RS_TONE_MONITOR),
            keyTones        = u8(C.RS_KEY_TONES).coerceIn(0, 3),
            subToneDev      = u8(C.RS_SUBTONE_DEV),
            dtmfDev         = u8(C.RS_DTMF_DEV),
            dtmfSpeed       = u8(C.RS_DTMF_SPEED),
            dtmfDecode      = u8(C.RS_DTMF_DECODE),
            dtmfSeqEndPause = u8(C.RS_DTMF_SEQ_PAUSE),
            battStyle       = u8(C.RS_BATT_STYLE),
            bluetooth       = bool(C.RS_BLUETOOTH),
            powerSave       = u8(C.RS_POWER_SAVE),
            dwDelay         = u8(C.RS_DW_DELAY),
            vfoLockActive   = bool(C.RS_VFO_LOCK_ACTIVE),
            asl             = u8(C.RS_ASL),
            disableFmt      = bool(C.RS_DISABLE_FMT),
            scramblerIf     = run {
                val en   = u8(C.RS_SCRAMBLER_EN)
                val freq = u8(C.RS_SCRAMBLER_FREQ)
                // Check only bit 0 for the enable flag — the radio may write other bits
                // in this byte when disabling, causing a full == 0 check to fail.
                if ((en and 0x01) == 0) 0
                else if (freq == 0) 8          // raw 0 = 3300 Hz = UI setting 8
                else freq.coerceIn(1, 10)      // raw 1-7 → UI 1-7; raw 8-9 → UI 9-10
            },
            pin             = u16(C.RS_PIN),
            pinAction       = u8(C.RS_PIN_ACTION),
        )
    }

    /** Writes all fields of [settings] back into the 0x1900 Settings block. */
    fun writeRadioSettings(eeprom: ByteArray, s: com.nicfw.tdh3editor.RadioSettings) {
        fun pu8(off: Int, v: Int)  { eeprom[off] = (v and 0xFF).toByte() }
        fun pu16(off: Int, v: Int) { eeprom[off] = ((v shr 8) and 0xFF).toByte(); eeprom[off + 1] = (v and 0xFF).toByte() }
        fun pbool(off: Int, v: Boolean) { eeprom[off] = if (v) 1 else 0 }
        val C = EepromConstants
        pu8(C.RS_SQUELCH,         s.squelch)
        pu8(C.RS_SQ_NOISE_LEV,    s.sqNoiseLev)
        pu8(C.RS_NOISE_CEILING,   s.noiseCeiling)
        pu8(C.RS_STE,             s.ste)
        pu8(C.RS_MIC_GAIN,        s.micGain)
        pu8(C.RS_NOISE_GATE,      s.noiseGate)
        pu8(C.RS_RF_GAIN,         s.rfGain)
        pu8(C.RS_LCD_BRIGHTNESS,  s.lcdBrightness)
        pu8(C.RS_LCD_TIMEOUT,     s.lcdTimeout)
        pu8(C.RS_BREATHE,         s.breathe)
        pu8(C.RS_LCD_DIM,         s.lcdDim)
        pu8(C.RS_LCD_GAMMA,       s.lcdGamma)
        pbool(C.RS_LCD_INVERTED,  s.lcdInverted)
        pu8(C.RS_SBAR_STYLE,      s.sBarStyle)
        pbool(C.RS_SBAR_PERSISTENT, s.sBarPersistent)
        pbool(C.RS_TX_MOD_METER,  s.txModMeter)
        pu8(C.RS_TX_TIMEOUT,      s.txTimeout)
        pu8(C.RS_TX_DEVIATION,    s.txDeviation)
        pu8(C.RS_PTT_MODE,        s.pttMode)
        pu16(C.RS_TX_FILTER_TRANS, s.txFilterTrans)
        pbool(C.RS_SHOW_XMIT_CURR, s.showXmitCurrent)
        pu16(C.RS_REPEATER_TONE,  s.repeaterTone)
        pu16(C.RS_STEP,           s.step)
        pu8(C.RS_AF_FILTERS,      s.afFilters)
        pu8(C.RS_IF_FREQ,         s.ifFreq)
        pu8(C.RS_RFI_COMP,        s.rfiComp)
        pu8(C.RS_AGC0,            s.agc0)
        pu8(C.RS_AGC1,            s.agc1)
        pu8(C.RS_AGC2,            s.agc2)
        pu8(C.RS_AGC3,            s.agc3)
        pbool(C.RS_AM_AGC_FIX,   s.amAgcFix)
        pbool(C.RS_DUAL_WATCH,    s.dualWatch)
        pu8(C.RS_SCAN_RESUME,     s.scanResume)
        pu16(C.RS_SCAN_RANGE,     s.scanRange)
        pu16(C.RS_SCAN_PERSIST,   s.scanPersist)
        pu8(C.RS_SCAN_UPDATE,     s.scanUpdate)
        pu8(C.RS_ULTRA_SCAN,      s.ultraScan)
        pu8(C.RS_VOX,             s.vox)
        pu16(C.RS_VOX_TAIL,       s.voxTail)
        pu8(C.RS_TONE_MONITOR,    s.toneMonitor)
        pu8(C.RS_KEY_TONES,       s.keyTones.coerceIn(0, 3))
        pu8(C.RS_SUBTONE_DEV,     s.subToneDev)
        pu8(C.RS_DTMF_DEV,        s.dtmfDev)
        pu8(C.RS_DTMF_SPEED,      s.dtmfSpeed)
        pu8(C.RS_DTMF_DECODE,     s.dtmfDecode)
        pu8(C.RS_DTMF_SEQ_PAUSE,  s.dtmfSeqEndPause)
        pu8(C.RS_BATT_STYLE,      s.battStyle)
        pbool(C.RS_BLUETOOTH,     s.bluetooth)
        pu8(C.RS_POWER_SAVE,      s.powerSave)
        pu8(C.RS_DW_DELAY,        s.dwDelay)
        pbool(C.RS_VFO_LOCK_ACTIVE, s.vfoLockActive)
        pu8(C.RS_ASL,             s.asl)
        pbool(C.RS_DISABLE_FMT,   s.disableFmt)
        // Scrambler: two fields — RS_SCRAMBLER_EN (bool) + RS_SCRAMBLER_FREQ (index).
        // Freq index: UI0=Off(en=0), UI8(3300Hz)→raw0, UI1-7→raw1-7, UI9-10→raw8-9(unverified).
        val scrEn   = if (s.scramblerIf == 0) 0 else 1
        val scrFreq = when (s.scramblerIf) {
            0       -> 0                  // Off — enable=0 so freq is irrelevant
            8       -> 0                  // 3300 Hz lives at raw index 0
            else    -> s.scramblerIf      // 1-7 and 9-10 map directly to raw index
        }
        pu8(C.RS_SCRAMBLER_EN,   scrEn)
        pu8(C.RS_SCRAMBLER_FREQ, scrFreq)
        pu16(C.RS_PIN,            s.pin)
        pu8(C.RS_PIN_ACTION,      s.pinAction)
    }

    /**
     * Writes [settings] back into the 5-byte Tune Settings block at 0x1DFB.
     */
    fun writeTuneSettings(eeprom: ByteArray, settings: com.nicfw.tdh3editor.TuneSettings) {
        val base = EepromConstants.TUNE_SETTINGS_BASE
        if (base + 5 > eeprom.size) return
        eeprom[base]     = settings.xtal671.toByte()
        eeprom[base + 1] = (settings.maxPowerWattsUHF   and 0xFF).toByte()
        eeprom[base + 2] = (settings.maxPowerSettingUHF and 0xFF).toByte()
        eeprom[base + 3] = (settings.maxPowerWattsVHF   and 0xFF).toByte()
        eeprom[base + 4] = (settings.maxPowerSettingVHF and 0xFF).toByte()
    }

    private fun readU32Be(b: ByteArray, off: Int): Long {
        return ((b[off].toInt() and 0xFF).toLong() shl 24) or
            ((b[off + 1].toInt() and 0xFF).toLong() shl 16) or
            ((b[off + 2].toInt() and 0xFF).toLong() shl 8) or
            (b[off + 3].toInt() and 0xFF).toLong()
    }

    private fun readU16Be(b: ByteArray, off: Int): Int =
        ((b[off].toInt() and 0xFF) shl 8) or (b[off + 1].toInt() and 0xFF)

    private fun writeU32Be(b: ByteArray, off: Int, v: Long) {
        b[off] = (v shr 24).toByte()
        b[off + 1] = (v shr 16).toByte()
        b[off + 2] = (v shr 8).toByte()
        b[off + 3] = v.toByte()
    }

    private fun writeU16Be(b: ByteArray, off: Int, v: Int) {
        b[off] = (v shr 8).toByte()
        b[off + 1] = v.toByte()
    }
}
