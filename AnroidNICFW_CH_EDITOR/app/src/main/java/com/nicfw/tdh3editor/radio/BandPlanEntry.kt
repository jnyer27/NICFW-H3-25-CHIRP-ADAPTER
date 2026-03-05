package com.nicfw.tdh3editor.radio

/**
 * A single entry from the nicFW 2.5 Band Plan table stored at 0x1A02 in EEPROM.
 *
 * Each of the 20 entries defines a frequency range the radio may operate in,
 * and whether transmission is permitted within that range.
 *
 * @param startHz   Lower bound of the frequency range (Hz).
 * @param endHz     Upper bound of the frequency range (Hz).
 * @param txAllowed Whether the radio permits transmission in this range.
 *                  False means the Band Plan forces N/T regardless of channel power.
 */
data class BandPlanEntry(
    val startHz: Long,
    val endHz: Long,
    val txAllowed: Boolean
)
