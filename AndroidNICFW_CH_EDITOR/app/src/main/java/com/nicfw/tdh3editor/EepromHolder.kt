package com.nicfw.tdh3editor

import com.nicfw.tdh3editor.radio.BandPlanEntry
import com.nicfw.tdh3editor.radio.ScanPresetEntry

/**
 * General radio settings stored at EEPROM offset 0x1900 (nicFW 2.5).
 * Mirrors the "Settings" tab in the nicFW Programmer.
 *
 * Units and storage notes:
 *  - scanRange:       raw × 10 kHz  (100 = 1.00 MHz)
 *  - scanPersist:     raw × 0.1 s
 *  - voxTail:         raw × 0.1 s  (20 = 2.0 s)
 *  - dtmfSeqEndPause: raw × 0.1 s  (10 = 1.0 s)
 *  - step:            raw u16 in 10 Hz units; see RS_STEP_RAW in EepromConstants
 *  - repeaterTone:    raw u16 in Hz (1750 = 1750 Hz)
 *  - pin:             raw u16, 0-9999
 *  - txFilterTrans: raw u16; 0 = use firmware default (280 MHz)
 *  - ifFreq:          raw u8;  0 = firmware default (displayed as 8.46 kHz)
 */
data class RadioSettings(
    // Squelch & Audio
    val squelch:         Int = 0,
    val sqNoiseLev:      Int = 0,
    val noiseCeiling:    Int = 0,
    val ste:             Int = 0,
    val micGain:         Int = 25,
    val noiseGate:       Int = 0,
    val rfGain:          Int = 0,
    // Display
    val lcdBrightness:   Int = 28,
    val lcdTimeout:      Int = 0,
    val breathe:         Int = 0,
    val lcdDim:          Int = 0,
    val lcdGamma:        Int = 0,
    val lcdInverted:     Boolean = false,
    val sBarStyle:       Int = 0,
    val sBarPersistent:  Boolean = false,
    // TX
    val txModMeter:      Boolean = true,
    val txTimeout:       Int = 120,
    val txDeviation:     Int = 64,
    val pttMode:         Int = 0,
    val txFilterTrans:   Int = 0,
    val showXmitCurrent: Boolean = false,
    val repeaterTone:    Int = 1750,
    // RX & Tuning
    val step:            Int = 500,
    val afFilters:       Int = 0,
    val ifFreq:          Int = 0,
    val rfiComp:         Int = 0,
    val agc0:            Int = 24,
    val agc1:            Int = 32,
    val agc2:            Int = 37,
    val agc3:            Int = 40,
    // Scan
    val dualWatch:       Boolean = true,
    val scanResume:      Int = 10,
    val scanRange:       Int = 100,
    val scanPersist:     Int = 0,
    val scanUpdate:      Int = 0,
    val ultraScan:       Int = 7,
    // VOX
    val vox:             Int = 0,
    val voxTail:         Int = 20,
    // Tones & Keys
    val toneMonitor:     Int = 1,
    val keyTones:        Int     = 0,      // 0=Off, 1=On, 2=Differential, 3=Voice
    val subToneDev:      Int = 74,
    // DTMF
    val dtmfDev:         Int = 80,
    val dtmfSpeed:       Int = 11,
    val dtmfDecode:      Int = 0,
    val dtmfSeqEndPause: Int = 10,
    // System
    val battStyle:       Int = 2,
    val bluetooth:       Boolean = true,
    val powerSave:       Int = 0,
    val dwDelay:         Int = 5,
    val vfoLockActive:   Boolean = false,
    val asl:             Int = 0,
    val disableFmt:      Boolean = false,
    val scramblerIf:     Int = 0,
    // Security
    val pin:             Int = 1234,
    val pinAction:       Int = 0,
)

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

    /**
     * General radio settings parsed from 0x1900 (nicFW 2.5 Settings block).
     * Mirrors the "Settings" tab in the nicFW Programmer.
     * Populated by MainActivity after each EEPROM load.
     */
    var radioSettings: RadioSettings = RadioSettings()
}
