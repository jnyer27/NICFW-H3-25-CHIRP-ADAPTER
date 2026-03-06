package com.nicfw.tdh3editor

import com.nicfw.tdh3editor.radio.BandPlanEntry
import com.nicfw.tdh3editor.radio.ScanPresetEntry

/**
 * Per-radio calibration values stored at EEPROM offset 0x1DFB.
 *
 * @param xtal671              Crystal calibration offset (-128 to 127).
 * @param maxPowerWattsUHF     Max UHF output in 0.1W units (display/info only).
 * @param maxPowerSettingUHF   Max UHF raw power byte the radio enforces at TX time.
 * @param maxPowerWattsVHF     Max VHF output in 0.1W units (display/info only).
 * @param maxPowerSettingVHF   Max VHF raw power byte the radio enforces at TX time.
 */
data class TuneSettings(
    val xtal671:            Int = 0,
    val maxPowerWattsUHF:   Int = 255,
    val maxPowerSettingUHF: Int = 255,
    val maxPowerWattsVHF:   Int = 255,
    val maxPowerSettingVHF: Int = 255,
)

/**
 * Application-level holder for the current EEPROM image so that ChannelEditActivity
 * and GroupLabelEditActivity can read and write the same buffer without passing it
 * through Intent.
 */
object EepromHolder {
    var eeprom: ByteArray? = null

    /**
     * Decoded group labels (A–O, 15 items) parsed from 0x1C90.
     * Empty string means the label is blank in the radio.
     * Populated by MainActivity after each EEPROM load.
     */
    var groupLabels: List<String> = List(15) { "" }

    /**
     * Decoded Band Plan entries parsed from 0x1A00 (nicFW 2.5).
     * Each entry covers a frequency range and indicates whether TX is permitted.
     * Empty when the EEPROM has not been loaded or the band plan magic is absent.
     * Populated by MainActivity after each EEPROM load.
     */
    var bandPlan: List<BandPlanEntry> = emptyList()

    /**
     * Decoded Scan Preset entries parsed from 0x1B00 (nicFW 2.5).
     * Only non-empty entries (startHz != 0) are included.
     * Populated by MainActivity after each EEPROM load.
     */
    var scanPresets: List<ScanPresetEntry> = emptyList()

    /**
     * Per-radio calibration values parsed from 0x1DFB (nicFW 2.5).
     * Includes XTAL 671 crystal calibration and VHF/UHF max power caps.
     * Populated by MainActivity after each EEPROM load.
     * Defaults to uncapped (255) until an EEPROM is loaded.
     */
    var tuneSettings: TuneSettings = TuneSettings()
}
