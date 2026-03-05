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
