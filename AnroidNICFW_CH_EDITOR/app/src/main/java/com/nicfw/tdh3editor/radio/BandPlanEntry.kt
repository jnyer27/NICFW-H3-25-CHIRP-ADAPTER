package com.nicfw.tdh3editor.radio

/**
 * A single entry from the nicFW 2.5 Band Plan table stored at 0x1A02 in EEPROM.
 *
 * Layout (10 bytes per entry):
 *   u32 startFreq  — 10 Hz units
 *   u32 endFreq    — 10 Hz units
 *   u8  maxPower   — 0 = any/ignore, 1–255 = max power raw value
 *   u8  flags      — bit 0: txAllowed, bit 1: wrap, bits 2–4: modRaw, bits 5–7: bwRaw
 *
 * An entry with both startHz=0 and endHz=0 is considered empty (unused slot).
 *
 * @param startHz   Lower bound of the frequency range (Hz).
 * @param endHz     Upper bound of the frequency range (Hz).
 * @param txAllowed Whether the radio permits transmission in this range.
 *                  False means the Band Plan forces N/T regardless of channel power.
 * @param maxPower  Maximum transmit power enforced in this range (0 = no limit / ignore).
 * @param wrap      Whether scan wraps at the boundary of this band plan range.
 * @param modRaw    Raw 3-bit modulation override (0=Auto, 1=FM, 2=USB, 3=AM,
 *                  4=Ignore, 5=Enforce FM, 6=Enforce AM, 7=Enforce USB).
 * @param bwRaw     Raw 3-bit bandwidth override (0=Ignore, 1=Wide, 2=Narrow,
 *                  3=FM Tuner, 4=Enforce Wide, 5=Enforce Narrow).
 */
data class BandPlanEntry(
    val startHz:   Long,
    val endHz:     Long,
    val txAllowed: Boolean,
    val maxPower:  Int     = 0,
    val wrap:      Boolean = false,
    val modRaw:    Int     = 0,
    val bwRaw:     Int     = 0
) {
    /** True when this entry is an unused (empty) EEPROM slot. */
    val isEmpty: Boolean get() = startHz == 0L && endHz == 0L
}
