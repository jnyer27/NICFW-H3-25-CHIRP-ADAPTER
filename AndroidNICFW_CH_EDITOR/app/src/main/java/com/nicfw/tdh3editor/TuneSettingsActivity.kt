package com.nicfw.tdh3editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.nicfw.tdh3editor.databinding.ActivityTuneSettingsBinding
import com.nicfw.tdh3editor.radio.EepromConstants
import com.nicfw.tdh3editor.radio.EepromParser

/**
 * Editor for the per-radio Tune Settings calibration block at EEPROM 0x1DFB.
 *
 * Exposes the three fields shown in nicFW Programmer's "Tuning2 → Tune Settings" tab:
 *   • VHF Power Setting Cap  — maxPowerSettingVHF at 0x1DFF (u8, 0–255)
 *   • UHF Power Setting Cap  — maxPowerSettingUHF at 0x1DFD (u8, 0–255)
 *   • XTAL 671               — xtal671            at 0x1DFB (i8, -128–127)
 *
 * Changes are written into the in-memory [EepromHolder.eeprom] and
 * [EepromHolder.tuneSettings] on save. The caller (MainActivity) re-reads
 * the EEPROM on resume so the channel-list power cap indicators refresh.
 *
 * These are per-radio values — cloning an EEPROM from another radio will
 * copy them. Set individually for each physical radio.
 */
class TuneSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTuneSettingsBinding

    // ── NumberPicker display-value arrays ─────────────────────────────────────

    /** 0 → "N/T (0)", 1–255 → "1"…"255" — matches POWERLEVEL_LIST. */
    private val capValues: Array<String> = Array(256) { i ->
        if (i == 0) "0 (N/T)" else i.toString()
    }

    /** -128…+127 as signed strings. */
    private val xtalValues: Array<String> = Array(256) { i ->
        (i - 128).toString()   // picker value 0 → -128, 128 → 0, 255 → +127
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTuneSettingsBinding.inflate(layoutInflater)
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

        setupPickers()
        populateFromHolder()
        setupButtons()
        setupProtectToggle()
        HelpSystem.init(this)
        setupHelpButtons()
    }

    private fun setupHelpButtons() {
        mapOf(
            binding.helpVhfCap      to "vhf_power_cap",
            binding.helpUhfCap      to "uhf_power_cap",
            binding.helpXtal671     to "xtal_671",
            binding.helpProtectTune to "protect_tune",
        ).forEach { (btn, key) ->
            btn.setOnClickListener { HelpSystem.show(this, key) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupPickers() {
        // VHF cap (0–255)
        binding.pickerVhfCap.minValue = 0
        binding.pickerVhfCap.maxValue = 255
        binding.pickerVhfCap.displayedValues = capValues
        binding.pickerVhfCap.wrapSelectorWheel = false
        binding.pickerVhfCap.setOnValueChangedListener { _, _, new ->
            binding.textVhfCapWatts.text = "≈ ${EepromConstants.powerToWatts(new.toString())}"
        }

        // UHF cap (0–255)
        binding.pickerUhfCap.minValue = 0
        binding.pickerUhfCap.maxValue = 255
        binding.pickerUhfCap.displayedValues = capValues
        binding.pickerUhfCap.wrapSelectorWheel = false
        binding.pickerUhfCap.setOnValueChangedListener { _, _, new ->
            binding.textUhfCapWatts.text = "≈ ${EepromConstants.powerToWatts(new.toString())}"
        }

        // XTAL 671 (-128 to +127); picker value 0..255 maps to signed -128..+127
        binding.pickerXtal.minValue = 0
        binding.pickerXtal.maxValue = 255
        binding.pickerXtal.displayedValues = xtalValues
        binding.pickerXtal.wrapSelectorWheel = false
        binding.pickerXtal.setOnValueChangedListener { _, _, new ->
            updateXtalHint(new - 128)
        }
    }

    /** Pre-seeds all pickers from [EepromHolder.tuneSettings]. */
    private fun populateFromHolder() {
        val ts = EepromHolder.tuneSettings

        // VHF cap
        binding.pickerVhfCap.value = ts.maxPowerSettingVHF.coerceIn(0, 255)
        binding.textVhfCapWatts.text =
            "≈ ${EepromConstants.powerToWatts(ts.maxPowerSettingVHF.toString())}"

        // UHF cap
        binding.pickerUhfCap.value = ts.maxPowerSettingUHF.coerceIn(0, 255)
        binding.textUhfCapWatts.text =
            "≈ ${EepromConstants.powerToWatts(ts.maxPowerSettingUHF.toString())}"

        // XTAL 671: stored as signed -128..+127; picker uses 0-based index
        val xtalSigned = ts.xtal671.coerceIn(-128, 127)
        binding.pickerXtal.value = xtalSigned + 128
        updateXtalHint(xtalSigned)
    }

    private fun setupButtons() {
        binding.btnTuneCancel.setOnClickListener { finish() }
        binding.btnTuneSave.setOnClickListener   { saveAndFinish() }
    }

    /** Reads the shared protect preference and wires the toggle in both directions. */
    private fun setupProtectToggle() {
        val prefs = getSharedPreferences("nicfw_prefs", MODE_PRIVATE)
        val protect = prefs.getBoolean("pref_protect_tune", true)
        binding.switchProtectTune.isChecked = protect
        updateProtectState(protect)

        binding.switchProtectTune.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("pref_protect_tune", checked).apply()
            updateProtectState(checked)
        }
    }

    /** Enables or disables the picker controls and Save button based on protect state. */
    private fun updateProtectState(protect: Boolean) {
        binding.pickerVhfCap.isEnabled = !protect
        binding.pickerUhfCap.isEnabled = !protect
        binding.pickerXtal.isEnabled   = !protect
        binding.btnTuneSave.isEnabled  = !protect
    }

    // ─────────────────────────────────────────────────────────────────────────
    // XTAL hint
    // ─────────────────────────────────────────────────────────────────────────

    private fun updateXtalHint(signed: Int) {
        binding.textXtalHint.text = when {
            signed > 0  -> "Radio receives HIGH — oscillator fast\nUse XTAL 671 Calculator to verify"
            signed < 0  -> "Radio receives LOW — oscillator slow\nUse XTAL 671 Calculator to verify"
            else        -> "No crystal correction applied"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save
    // ─────────────────────────────────────────────────────────────────────────

    private fun saveAndFinish() {
        // Hard guard: if protection is enabled, don't write (UI already blocks via button state,
        // but this ensures safety even if called programmatically).
        if (getSharedPreferences("nicfw_prefs", MODE_PRIVATE)
                .getBoolean("pref_protect_tune", true)) {
            Toast.makeText(this, "Tune Settings are protected \u2014 not written", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val eep = EepromHolder.eeprom ?: run {
            Toast.makeText(this, "No EEPROM data", Toast.LENGTH_SHORT).show()
            return
        }

        val vhfCap  = binding.pickerVhfCap.value          // 0–255
        val uhfCap  = binding.pickerUhfCap.value          // 0–255
        val xtalIdx = binding.pickerXtal.value             // 0–255
        val xtalSigned = xtalIdx - 128                    // -128..+127

        // Keep the watts bytes in sync with the setting bytes we are writing.
        // The radio stores watts as 0.1W units; approximate from the calibration table.
        val vhfWatts = wattsRaw(vhfCap)
        val uhfWatts = wattsRaw(uhfCap)

        val updated = TuneSettings(
            xtal671            = xtalSigned,
            maxPowerWattsUHF   = uhfWatts,
            maxPowerSettingUHF = uhfCap,
            maxPowerWattsVHF   = vhfWatts,
            maxPowerSettingVHF = vhfCap,
        )

        EepromParser.writeTuneSettings(eep, updated)
        EepromHolder.eeprom       = eep
        EepromHolder.tuneSettings = updated

        Toast.makeText(
            this,
            "Tune Settings saved — tap Save to Radio to apply",
            Toast.LENGTH_SHORT
        ).show()

        setResult(RESULT_OK)
        finish()
    }

    /**
     * Converts a raw power setting byte to the 0.1W unit value the radio stores
     * in the maxPowerWatts fields (approximate — matches calibration points).
     *
     * Calibration: 29→0.5W (5 units), 58→2.0W (20 units), 130→5.0W (50 units).
     */
    private fun wattsRaw(setting: Int): Int {
        if (setting <= 0) return 0
        val pts = arrayOf(0 to 0.0, 29 to 5.0, 58 to 20.0, 130 to 50.0)
        for (i in 0 until pts.size - 1) {
            val (v1, w1) = pts[i]
            val (v2, w2) = pts[i + 1]
            if (setting <= v2) {
                val t = (setting - v1).toDouble() / (v2 - v1)
                return (w1 + t * (w2 - w1)).toInt()
            }
        }
        val t = (setting - 58).toDouble() / (130 - 58)
        return (20.0 + t * 30.0).toInt().coerceAtMost(255)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Companion
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, TuneSettingsActivity::class.java)
    }
}

