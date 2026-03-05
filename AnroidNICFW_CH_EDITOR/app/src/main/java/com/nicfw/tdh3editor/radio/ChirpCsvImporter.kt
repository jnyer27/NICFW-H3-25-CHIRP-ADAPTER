package com.nicfw.tdh3editor.radio

/**
 * Parses a CHIRP-format CSV export (e.g. from RepeaterBook) into a list of [ChirpEntry]
 * objects ready to be mapped into empty EEPROM slots.
 *
 * CHIRP CSV columns used:
 *   Location, Name, Frequency, Duplex, Offset, Tone, rToneFreq, cToneFreq,
 *   DtcsCode, DtcsPolarity, Mode, Power
 *
 * Tone mode mapping:
 *   ""     → no TX or RX tone
 *   "Tone" → TX CTCSS only (rToneFreq)
 *   "TSQL" → TX + RX CTCSS (cToneFreq for both)
 *   "DTCS" → TX + RX DCS  (DtcsCode as integer label, DtcsPolarity "NN"/"NR"/"RN"/"RR")
 *   Other  → no tone (best-effort fallback)
 *
 * DCS codes: CHIRP "023" → parseInt = 23 → matches EepromConstants.DCS_CODES entries.
 *
 * Each parsed [Channel] has number = 0; the caller assigns the actual slot number.
 * Groups are left as "None"; the caller applies group assignments from the UI.
 */
object ChirpCsvImporter {

    data class ChirpEntry(
        val csvLocation: Int,   // original CSV "Location" value (for display only)
        val channel: Channel    // parsed channel; number=0 until slot is assigned
    )

    fun parse(csvText: String): List<ChirpEntry> {
        val lines = csvText.lines()

        // Locate the header row (first row starting with "Location")
        val headerIdx = lines.indexOfFirst {
            it.trimStart().startsWith("Location", ignoreCase = true)
        }
        if (headerIdx < 0) return emptyList()

        val headers = parseCsvLine(lines[headerIdx]).map { it.trim().lowercase().trim('"') }

        fun colOf(cols: List<String>, name: String): String {
            val idx = headers.indexOf(name)
            return if (idx in cols.indices) cols[idx].trim().trim('"') else ""
        }

        val results = mutableListOf<ChirpEntry>()

        for (lineIdx in (headerIdx + 1) until lines.size) {
            val raw = lines[lineIdx].trim()
            if (raw.isEmpty()) continue
            val cols = parseCsvLine(raw)

            val location = colOf(cols, "location").toIntOrNull() ?: continue
            val freqMhz  = colOf(cols, "frequency").toDoubleOrNull() ?: continue
            val freqHz   = (freqMhz * 1_000_000).toLong()
            val name     = colOf(cols, "name").take(12)

            // ── Duplex / offset ───────────────────────────────────────────────
            val duplexRaw = colOf(cols, "duplex").lowercase()
            val offsetMhz = colOf(cols, "offset").toDoubleOrNull() ?: 0.0
            val duplex: String
            val freqTxHz: Long
            val offsetHz: Long
            when (duplexRaw) {
                "+"      -> { duplex = "+";     offsetHz = (offsetMhz * 1_000_000).toLong(); freqTxHz = freqHz + offsetHz }
                "-"      -> { duplex = "-";     offsetHz = (offsetMhz * 1_000_000).toLong(); freqTxHz = freqHz - offsetHz }
                "split"  -> { duplex = "split"; offsetHz = 0L; freqTxHz = (offsetMhz * 1_000_000).toLong() }
                else     -> {
                    // Issue #3: if the duplex field is empty/missing but a non-zero
                    // offset is present, assume positive (+) — common in RepeaterBook
                    // exports where the duplex column is left blank for repeaters.
                    if (offsetMhz != 0.0) {
                        duplex   = "+"
                        offsetHz = (offsetMhz * 1_000_000).toLong()
                        freqTxHz = freqHz + offsetHz
                    } else {
                        duplex   = ""
                        offsetHz = 0L
                        freqTxHz = freqHz
                    }
                }
            }

            // ── Tone ──────────────────────────────────────────────────────────
            val toneMode  = colOf(cols, "tone").uppercase()
            val rToneFreq = colOf(cols, "rtonefreq").toDoubleOrNull() ?: 88.5
            val cToneFreq = colOf(cols, "ctonefreq").toDoubleOrNull() ?: 88.5
            // CHIRP DtcsCode is the octal-label integer string, e.g. "023" → 23
            val dtcsCode  = colOf(cols, "dtcscode").toIntOrNull() ?: 0
            val dtcsPol   = colOf(cols, "dtcspolarity")  // e.g. "NN", "NR", "RN", "RR"
            val txDtcsPol = dtcsPol.getOrNull(0)?.toString() ?: "N"
            val rxDtcsPol = dtcsPol.getOrNull(1)?.toString() ?: "N"

            var txToneMode: String? = null
            var txToneVal:  Double? = null
            var txPol:      String? = null
            var rxToneMode: String? = null
            var rxToneVal:  Double? = null
            var rxPol:      String? = null

            when (toneMode) {
                "TONE" -> {
                    // TX CTCSS only; no RX squelch
                    txToneMode = "Tone"; txToneVal = rToneFreq
                }
                "TSQL" -> {
                    // TX + RX CTCSS (carrier-operated squelch, both use cToneFreq)
                    txToneMode = "Tone"; txToneVal = cToneFreq
                    rxToneMode = "Tone"; rxToneVal = cToneFreq
                }
                "DTCS" -> {
                    // TX + RX DCS
                    txToneMode = "DTCS"; txToneVal = dtcsCode.toDouble(); txPol = txDtcsPol
                    rxToneMode = "DTCS"; rxToneVal = dtcsCode.toDouble(); rxPol = rxDtcsPol
                }
                // "Cross", "" and anything else → no tone
            }

            // ── Power (Issue #4) ──────────────────────────────────────────────
            // The TD-H3 stores txPower as a raw byte 1–255 (0 = N/T).
            // CHIRP exports from this radio use the numeric string directly.
            // Exports from other radios use named levels; map them to reasonable
            // raw values. Default to "1" (minimum transmit) when absent.
            val powerRaw = colOf(cols, "power")
            val power = parsePower(powerRaw)

            // ── Mode ──────────────────────────────────────────────────────────
            val modeStr = when (colOf(cols, "mode").uppercase()) {
                "AM"  -> "AM"
                "USB" -> "USB"
                else  -> "FM"
            }

            results.add(
                ChirpEntry(
                    csvLocation = location,
                    channel = Channel(
                        number         = 0,          // slot assigned by caller
                        empty          = false,
                        freqRxHz       = freqHz,
                        freqTxHz       = freqTxHz,
                        duplex         = duplex,
                        offsetHz       = offsetHz,
                        power          = power,
                        name           = name,
                        mode           = modeStr,
                        bandwidth      = "Wide",
                        txToneMode     = txToneMode,
                        txToneVal      = txToneVal,
                        txTonePolarity = txPol,
                        rxToneMode     = rxToneMode,
                        rxToneVal      = rxToneVal,
                        rxTonePolarity = rxPol,
                        // Groups left as "None" — user assigns them in the import UI
                        group1 = "None", group2 = "None",
                        group3 = "None", group4 = "None",
                    )
                )
            )
        }

        return results
    }

    /**
     * Converts a CHIRP power string to the TD-H3's raw byte power value (1–255).
     *
     * TD-H3 / nicFW stores txPower as a byte: 0 = N/T, 1..255 = transmit level.
     * Exports from this radio contain the numeric string directly (e.g. "130").
     * Exports from other radios or RepeaterBook may use named levels; map them
     * to useful starting points (user can fine-tune in the channel editor):
     *   High / Hi   → 130  (typical TD-H3 max)
     *   Medium / Mid→ 65   (mid-range)
     *   Low / Lo    → 1    (minimum transmit)
     * Anything unrecognised or blank defaults to "1".
     */
    private fun parsePower(raw: String): String {
        val s = raw.trim()
        if (s.isBlank()) return "1"
        // Numeric value from the same radio type
        s.toIntOrNull()?.let { if (it in 1..255) return it.toString() }
        // Named levels from other CHIRP-supported radios
        return when (s.lowercase()) {
            "high", "hi"              -> "130"
            "medium", "mid", "med"    -> "65"
            "low", "lo"               -> "1"
            else                      -> "1"   // unrecognised → safe minimum
        }
    }

    /** Parses one CSV line respecting double-quoted fields that may contain commas. */
    private fun parseCsvLine(line: String): List<String> {
        val result   = mutableListOf<String>()
        val current  = StringBuilder()
        var inQuotes = false
        for (c in line) {
            when {
                c == '"'           -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { result += current.toString(); current.clear() }
                else               -> current.append(c)
            }
        }
        result += current.toString()
        return result
    }
}
