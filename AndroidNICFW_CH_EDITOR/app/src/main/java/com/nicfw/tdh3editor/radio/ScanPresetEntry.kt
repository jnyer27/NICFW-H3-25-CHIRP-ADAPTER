package com.nicfw.tdh3editor.radio

/**
 * A single entry from the nicFW 2.5 Scan Preset table stored at 0x1B00 in EEPROM.
 *
 * Layout (20 bytes per entry, no magic header):
 *   u32 startFreq  — 10 Hz units  (0 = empty/unused slot)
 *   u16 span       — 10 kHz units; endFreq = startFreq + span × 10 kHz
 *   u16 step       — 10 Hz units
 *   u8  scanResume — scan resume count
 *   u8  scanPersist — scan persist count
 *   u8  flags      — bits[6:2] = ultrascan (0–20), bits[1:0] = modulation
 *                    modulation: 0=FM, 1=AM, 2=USB, 3=Auto
 *   u8[8] label    — ASCII, space-padded to 8 bytes
 *   u8  null       — always 0x00 (null terminator)
 *
 * An entry with startHz == 0 is considered empty (unused slot).
 *
 * @param startHz    Lower bound of the frequency range (Hz).
 * @param endHz      Upper bound of the frequency range (Hz).
 * @param stepHz     Scan step size in Hz.
 * @param scanResume Number of scan resume cycles (0 = don't resume).
 * @param scanPersist Number of scan persist cycles.
 * @param modRaw     Raw 2-bit modulation value: 0=FM, 1=AM, 2=USB, 3=Auto.
 * @param ultrascan  Ultrascan speed (0–20; 20 = fastest).
 * @param label      Display label, 8 chars max.
 */
data class ScanPresetEntry(
    val startHz:     Long   = 0L,
    val endHz:       Long   = 0L,
    val stepHz:      Int    = 0,
    val scanResume:  Int    = 0,
    val scanPersist: Int    = 0,
    val modRaw:      Int    = 0,
    val ultrascan:   Int    = 20,
    val label:       String = ""
) {
    /** True when this entry is an unused (empty) EEPROM slot. */
    val isEmpty: Boolean get() = startHz == 0L
}
