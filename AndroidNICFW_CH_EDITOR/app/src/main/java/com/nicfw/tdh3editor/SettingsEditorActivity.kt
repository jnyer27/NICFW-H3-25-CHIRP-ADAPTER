package com.nicfw.tdh3editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.nicfw.tdh3editor.databinding.ActivitySettingsEditorBinding
import com.nicfw.tdh3editor.radio.EepromConstants
import com.nicfw.tdh3editor.radio.EepromParser
import kotlin.math.roundToInt

/**
 * Editor for the nicFW 2.5 Radio Settings block at EEPROM 0x1900.
 *
 * Exposes all 56 fields shown in the nicFW Programmer "Settings" tab, grouped
 * into 10 sections:
 *   Squelch & Audio · Display · TX Settings · RX & Tuning · Scan ·
 *   VOX · Tones & Keys · DTMF · System · Security
 *
 * Fractional display conversions (human-readable ↔ raw EEPROM):
 *   Scan Range      display MHz  = raw / 100.0   (raw ×10 kHz)
 *   Scan Persist    display s    = raw / 10.0    (raw ×0.1 s)
 *   VOX Tail        display s    = raw / 10.0    (raw ×0.1 s)
 *   DTMF Seq Pause  display s    = raw / 10.0    (raw ×0.1 s)
 *
 * Changes are written into [EepromHolder.eeprom] and [EepromHolder.radioSettings]
 * on save. The user must then use "Save to Radio" in MainActivity to upload.
 *
 * Fields marked ⚠ in the UI use EEPROM offsets derived by cross-referencing the
 * EEPROM dump against the programmer screenshot — not confirmed in firmware source.
 */
class SettingsEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsEditorBinding

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        if (EepromHolder.eeprom == null) {
            Toast.makeText(this, "No EEPROM data — load from radio first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupSpinners()
        populateFromHolder()
        setupButtons()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupSpinners() {
        fun <T> spin(spinner: android.widget.Spinner, items: List<T>) {
            spinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                items
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }

        val C = EepromConstants
        spin(binding.spinnerSquelch,     C.RS_SQUELCH_LIST)
        spin(binding.spinnerSte,         C.RS_STE_LIST)
        spin(binding.spinnerPttMode,     C.RS_PTT_MODE_LIST)
        spin(binding.spinnerStep,        C.RS_STEP_LABELS)
        spin(binding.spinnerAfFilters,   C.RS_AF_FILTERS_LIST)
        spin(binding.spinnerIfFreq,      C.RS_IF_FREQ_LIST)
        spin(binding.spinnerRfiComp,     C.RS_RFI_COMP_LIST)
        spin(binding.spinnerSBarStyle,   C.RS_SBAR_STYLE_LIST)
        spin(binding.spinnerKeyTones,    C.RS_KEY_TONES_LIST)
        spin(binding.spinnerAsl,         C.RS_ASL_LIST)
        spin(binding.spinnerToneMonitor, C.RS_TONE_MONITOR_LIST)
        spin(binding.spinnerBattStyle,   C.RS_BATT_STYLE_LIST)
        spin(binding.spinnerPinAction,   C.RS_PIN_ACTION_LIST)
        spin(binding.spinnerDtmfDecode,  C.RS_DTMF_DECODE_LIST)
        spin(binding.spinnerScramblerIf, C.RS_SCRAMBLER_LABELS)
        spin(binding.spinnerRfGain,      C.RS_RF_GAIN_LIST)
        spin(binding.spinnerPowerSave,   C.RS_POWER_SAVE_LIST)
    }

    /** Populates all UI widgets by re-parsing the live EEPROM buffer. */
    private fun populateFromHolder() {
        val eep = EepromHolder.eeprom ?: return
        val s = EepromParser.readRadioSettings(eep)
        val C = EepromConstants

        // ── Spinners ──────────────────────────────────────────────────────────
        binding.spinnerSquelch.setSelection(s.squelch.coerceIn(0, C.RS_SQUELCH_LIST.lastIndex))
        binding.spinnerSte.setSelection(s.ste.coerceIn(0, C.RS_STE_LIST.lastIndex))
        binding.spinnerPttMode.setSelection(s.pttMode.coerceIn(0, C.RS_PTT_MODE_LIST.lastIndex))
        // Step: find matching raw value in RS_STEP_RAW; default to index 1 (5.0 kHz)
        binding.spinnerStep.setSelection(
            C.RS_STEP_RAW.indexOf(s.step).takeIf { it >= 0 } ?: 1
        )
        binding.spinnerAfFilters.setSelection(s.afFilters.coerceIn(0, C.RS_AF_FILTERS_LIST.lastIndex))
        binding.spinnerIfFreq.setSelection(s.ifFreq.coerceIn(0, C.RS_IF_FREQ_LIST.lastIndex))
        binding.spinnerRfiComp.setSelection(s.rfiComp.coerceIn(0, C.RS_RFI_COMP_LIST.lastIndex))
        binding.spinnerSBarStyle.setSelection(s.sBarStyle.coerceIn(0, C.RS_SBAR_STYLE_LIST.lastIndex))
        binding.spinnerKeyTones.setSelection(s.keyTones.coerceIn(0, C.RS_KEY_TONES_LIST.lastIndex))
        binding.spinnerAsl.setSelection(s.asl.coerceIn(0, C.RS_ASL_LIST.lastIndex))
        binding.spinnerToneMonitor.setSelection(s.toneMonitor.coerceIn(0, C.RS_TONE_MONITOR_LIST.lastIndex))
        binding.spinnerBattStyle.setSelection(s.battStyle.coerceIn(0, C.RS_BATT_STYLE_LIST.lastIndex))
        binding.spinnerPinAction.setSelection(s.pinAction.coerceIn(0, C.RS_PIN_ACTION_LIST.lastIndex))

        // ── Switches ──────────────────────────────────────────────────────────
        binding.switchLcdInverted.isChecked     = s.lcdInverted
        binding.switchSBarPersistent.isChecked  = s.sBarPersistent
        binding.switchTxModMeter.isChecked      = s.txModMeter
        binding.switchShowXmitCurrent.isChecked = s.showXmitCurrent
        binding.switchDualWatch.isChecked       = s.dualWatch
        binding.switchNoiseGate.isChecked       = s.noiseGate != 0
        binding.switchBluetooth.isChecked       = s.bluetooth
        binding.switchVfoLockActive.isChecked   = s.vfoLockActive
        binding.switchDisableFmt.isChecked      = s.disableFmt

        // ── EditTexts — raw integers ──────────────────────────────────────────
        fun ei(v: Int) = v.toString()
        binding.editSqNoiseLev.setText(ei(s.sqNoiseLev))
        binding.editNoiseCeiling.setText(ei(s.noiseCeiling))
        binding.editMicGain.setText(ei(s.micGain))
        binding.spinnerRfGain.setSelection(s.rfGain.coerceIn(0, C.RS_RF_GAIN_LIST.lastIndex))

        binding.editLcdBrightness.setText(ei(s.lcdBrightness))
        binding.editLcdTimeout.setText(ei(s.lcdTimeout))
        binding.editBreathe.setText(ei(s.breathe))
        binding.editLcdDim.setText(ei(s.lcdDim))
        binding.editLcdGamma.setText(ei(s.lcdGamma))

        binding.editTxTimeout.setText(ei(s.txTimeout))
        binding.editTxDeviation.setText(ei(s.txDeviation))
        // TX Filter Trans: raw 0 means "use default (280.0 MHz)"; display as "280.0" so the
        // user sees the actual frequency rather than a confusing zero.
        binding.editTxFilterTrans.setText(if (s.txFilterTrans == 0) "280.0" else s.txFilterTrans.toString())
        binding.editRepeaterTone.setText(ei(s.repeaterTone))

        binding.editAgc0.setText(ei(s.agc0))
        binding.editAgc1.setText(ei(s.agc1))
        binding.editAgc2.setText(ei(s.agc2))
        binding.editAgc3.setText(ei(s.agc3))

        binding.editScanResume.setText(ei(s.scanResume))
        binding.editScanUpdate.setText(ei(s.scanUpdate))
        binding.editUltraScan.setText(ei(s.ultraScan))

        binding.editVox.setText(ei(s.vox))

        binding.editSubToneDev.setText(ei(s.subToneDev))

        binding.editDtmfDev.setText(ei(s.dtmfDev))
        binding.editDtmfSpeed.setText(ei(s.dtmfSpeed))
        binding.spinnerDtmfDecode.setSelection(s.dtmfDecode.coerceIn(0, C.RS_DTMF_DECODE_LIST.lastIndex))

        binding.spinnerPowerSave.setSelection(s.powerSave.coerceIn(0, C.RS_POWER_SAVE_LIST.lastIndex))
        binding.editDwDelay.setText(ei(s.dwDelay))
        binding.spinnerScramblerIf.setSelection(s.scramblerIf.coerceIn(0, C.RS_SCRAMBLER_LABELS.lastIndex))

        binding.editPin.setText(ei(s.pin))

        // ── EditTexts — fractional (display in human-readable units) ──────────
        // scanRange: raw ×10 kHz → display as MHz (÷100)
        binding.editScanRange.setText("%.2f".format(s.scanRange / 100.0))
        // scanPersist / voxTail / dtmfSeqEndPause: raw ×0.1 s → display as s (÷10)
        binding.editScanPersist.setText("%.1f".format(s.scanPersist / 10.0))
        binding.editVoxTail.setText("%.1f".format(s.voxTail / 10.0))
        binding.editDtmfSeqEndPause.setText("%.1f".format(s.dtmfSeqEndPause / 10.0))
    }

    private fun setupButtons() {
        binding.btnSettingsCancel.setOnClickListener { finish() }
        binding.btnSettingsSave.setOnClickListener   { saveAndFinish() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save
    // ─────────────────────────────────────────────────────────────────────────

    private fun saveAndFinish() {
        val eep = EepromHolder.eeprom ?: run {
            Toast.makeText(this, "No EEPROM data", Toast.LENGTH_SHORT).show()
            return
        }

        fun readInt(text: String, default: Int): Int = text.trim().toIntOrNull() ?: default
        fun readFlt(text: String, default: Float): Float = text.trim().toFloatOrNull() ?: default

        fun String.asInt(def: Int) = readInt(this, def)
        fun String.asFlt(def: Float) = readFlt(this, def)

        val C = EepromConstants

        val updated = RadioSettings(
            // ── Spinners ─────────────────────────────────────────────────────
            squelch     = binding.spinnerSquelch.selectedItemPosition,
            ste         = binding.spinnerSte.selectedItemPosition,
            pttMode     = binding.spinnerPttMode.selectedItemPosition,
            step        = C.RS_STEP_RAW[binding.spinnerStep.selectedItemPosition],
            afFilters   = binding.spinnerAfFilters.selectedItemPosition,
            ifFreq      = binding.spinnerIfFreq.selectedItemPosition,
            rfiComp     = binding.spinnerRfiComp.selectedItemPosition,
            sBarStyle   = binding.spinnerSBarStyle.selectedItemPosition,
            keyTones    = binding.spinnerKeyTones.selectedItemPosition,
            asl         = binding.spinnerAsl.selectedItemPosition,
            toneMonitor = binding.spinnerToneMonitor.selectedItemPosition,
            battStyle   = binding.spinnerBattStyle.selectedItemPosition,
            pinAction   = binding.spinnerPinAction.selectedItemPosition,

            // ── Switches ─────────────────────────────────────────────────────
            lcdInverted     = binding.switchLcdInverted.isChecked,
            sBarPersistent  = binding.switchSBarPersistent.isChecked,
            txModMeter      = binding.switchTxModMeter.isChecked,
            showXmitCurrent = binding.switchShowXmitCurrent.isChecked,
            dualWatch       = binding.switchDualWatch.isChecked,
            noiseGate       = if (binding.switchNoiseGate.isChecked) 1 else 0,
            bluetooth       = binding.switchBluetooth.isChecked,
            vfoLockActive   = binding.switchVfoLockActive.isChecked,
            disableFmt      = binding.switchDisableFmt.isChecked,

            // ── EditTexts — raw integers ──────────────────────────────────────
            sqNoiseLev    = binding.editSqNoiseLev.text.toString().asInt(0).coerceIn(0, 255),
            noiseCeiling  = binding.editNoiseCeiling.text.toString().asInt(0).coerceIn(0, 255),
            micGain       = binding.editMicGain.text.toString().asInt(25).coerceIn(0, 31),
            rfGain        = binding.spinnerRfGain.selectedItemPosition,   // 0=AGC, 1–42 direct

            lcdBrightness = binding.editLcdBrightness.text.toString().asInt(28).coerceIn(0, 35),
            lcdTimeout    = binding.editLcdTimeout.text.toString().asInt(0).coerceIn(0, 255),
            breathe       = binding.editBreathe.text.toString().asInt(0).coerceIn(0, 30),
            lcdDim        = binding.editLcdDim.text.toString().asInt(0).coerceIn(0, 14),
            lcdGamma      = binding.editLcdGamma.text.toString().asInt(0).coerceIn(0, 255),

            txTimeout     = binding.editTxTimeout.text.toString().asInt(120).coerceIn(0, 255),
            txDeviation   = binding.editTxDeviation.text.toString().asInt(64).coerceIn(0, 127),
            // "280.0" (displayed default) maps back to raw 0; any other numeric value is stored directly.
            txFilterTrans = binding.editTxFilterTrans.text.toString().trim().let { txt ->
                if (txt == "280.0" || txt == "0") 0
                else txt.toIntOrNull()?.coerceIn(0, 65535) ?: 0
            },
            repeaterTone  = binding.editRepeaterTone.text.toString().asInt(1750).coerceIn(0, 65535),

            agc0          = binding.editAgc0.text.toString().asInt(24).coerceIn(0, 63),
            agc1          = binding.editAgc1.text.toString().asInt(32).coerceIn(0, 63),
            agc2          = binding.editAgc2.text.toString().asInt(37).coerceIn(0, 63),
            agc3          = binding.editAgc3.text.toString().asInt(40).coerceIn(0, 63),

            scanResume    = binding.editScanResume.text.toString().asInt(10).coerceIn(0, 30),
            scanUpdate    = binding.editScanUpdate.text.toString().asInt(0).coerceIn(0, 255),
            ultraScan     = binding.editUltraScan.text.toString().asInt(7).coerceIn(0, 7),

            vox           = binding.editVox.text.toString().asInt(0).coerceIn(0, 15),

            subToneDev    = binding.editSubToneDev.text.toString().asInt(74).coerceIn(0, 255),

            dtmfDev       = binding.editDtmfDev.text.toString().asInt(80).coerceIn(0, 255),
            dtmfSpeed     = binding.editDtmfSpeed.text.toString().asInt(11).coerceIn(0, 15),
            dtmfDecode    = binding.spinnerDtmfDecode.selectedItemPosition,

            powerSave     = binding.spinnerPowerSave.selectedItemPosition,   // 0=Off, 1–20 direct
            dwDelay       = binding.editDwDelay.text.toString().asInt(5).coerceIn(0, 15),
            scramblerIf   = binding.spinnerScramblerIf.selectedItemPosition,

            pin           = binding.editPin.text.toString().asInt(0).coerceIn(0, 9999),

            // ── EditTexts — fractional (convert display units back to raw) ────
            // Scan Range displayed as MHz → raw ×10 kHz (×100)
            scanRange     = (binding.editScanRange.text.toString().asFlt(1.0f) * 100f)
                                .roundToInt().coerceIn(0, 65535),
            // Remaining displayed as seconds → raw ×0.1 s (×10)
            scanPersist    = (binding.editScanPersist.text.toString().asFlt(0.0f) * 10f)
                                .roundToInt().coerceIn(0, 65535),
            voxTail        = (binding.editVoxTail.text.toString().asFlt(2.0f) * 10f)
                                .roundToInt().coerceIn(0, 65535),
            dtmfSeqEndPause = (binding.editDtmfSeqEndPause.text.toString().asFlt(1.0f) * 10f)
                                .roundToInt().coerceIn(0, 255),
        )

        EepromParser.writeRadioSettings(eep, updated)
        EepromHolder.eeprom        = eep
        EepromHolder.radioSettings = updated

        Toast.makeText(
            this,
            "Settings saved — tap Save to Radio to apply",
            Toast.LENGTH_SHORT
        ).show()

        setResult(RESULT_OK)
        finish()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Companion
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, SettingsEditorActivity::class.java)
    }
}
