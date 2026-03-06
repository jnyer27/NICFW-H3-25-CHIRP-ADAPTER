package com.nicfw.tdh3editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.nicfw.tdh3editor.databinding.ActivityXtal671CalculatorBinding
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Standalone XTAL 671 correction-value calculator for nicFW-equipped radios.
 *
 * Formula (matches the Google Sheets reference spreadsheet):
 *
 *   XTAL = ROUND( ((actualHz − targetHz) × 100_000) ÷ targetHz × 671.08864,  0 )
 *
 * where actualHz and targetHz are both in MHz.
 *
 * A positive result means the radio receives HIGH — increase the XTAL value.
 * A negative result means the radio receives LOW  — decrease the XTAL value.
 *
 * The activity is entirely standalone and does not require an EEPROM image.
 */
class Xtal671CalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityXtal671CalculatorBinding

    /** Last successfully computed correction value, or null when inputs are invalid. */
    private var lastResult: Int? = null

    // ── Preset target frequencies (MHz) ──────────────────────────────────────
    private data class Preset(val label: String, val freqMhz: String)
    private val PRESETS = listOf(
        Preset("GMRS22 462.725",  "462.7250"),
        Preset("GMRS1 462.5625",  "462.5625"),
        Preset("MURS1 151.820",   "151.8200"),
        Preset("WX1 162.400",     "162.4000"),
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityXtal671CalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left   = bars.left,
                top    = bars.top,
                right  = bars.right,
                bottom = bars.bottom
            )
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupPresetChips()
        setupTextWatchers()
        setupCopyButton()

        // Trigger an initial calculation using the default target frequency
        // so the UI is not blank on first open.
        recalculate()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Wire each frequency preset chip to fill the target frequency field. */
    private fun setupPresetChips() {
        val chips = listOf(
            binding.chipGmrs22 to PRESETS[0],
            binding.chipGmrs1  to PRESETS[1],
            binding.chipMurs1  to PRESETS[2],
            binding.chipWx1    to PRESETS[3],
        )
        for ((chip, preset) in chips) {
            chip.setOnClickListener {
                binding.editTargetFreq.setText(preset.freqMhz)
                // Move cursor to end so the field is ready for editing
                binding.editTargetFreq.setSelection(preset.freqMhz.length)
                recalculate()
            }
        }
    }

    /** Attach TextWatchers so the result updates as the user types. */
    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = recalculate()
        }
        binding.editTargetFreq.addTextChangedListener(watcher)
        binding.editActualFreq.addTextChangedListener(watcher)
    }

    private fun setupCopyButton() {
        binding.btnCopyResult.setOnClickListener {
            val value = lastResult ?: return@setOnClickListener
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("XTAL671", value.toString()))
            Toast.makeText(this, "Copied: $value", Toast.LENGTH_SHORT).show()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Calculation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Re-computes the XTAL 671 correction value from the current field contents
     * and updates all result views.
     *
     * Formula:
     *   xtal = ROUND( ((actual − target) × 100_000) ÷ target × 671.08864,  0 )
     */
    private fun recalculate() {
        val targetStr = binding.editTargetFreq.text?.toString()?.trim()
        val actualStr = binding.editActualFreq.text?.toString()?.trim()

        val target = targetStr?.toDoubleOrNull()
        val actual = actualStr?.toDoubleOrNull()

        // ── Validation ────────────────────────────────────────────────────────
        binding.layoutTargetFreq.error = when {
            target == null && !targetStr.isNullOrEmpty() -> "Enter a valid frequency in MHz"
            target != null && target <= 0.0              -> "Frequency must be positive"
            else                                         -> null
        }
        binding.layoutActualFreq.error = when {
            actual == null && !actualStr.isNullOrEmpty() -> "Enter a valid frequency in MHz"
            actual != null && actual <= 0.0              -> "Frequency must be positive"
            else                                         -> null
        }

        if (target == null || target <= 0.0 || actual == null || actual <= 0.0) {
            showNoResult()
            return
        }

        // ── Core formula ──────────────────────────────────────────────────────
        // ROUND(((actual − target) × 100_000) ÷ target × 671.08864, 0)
        val rawValue = ((actual - target) * 100_000.0) / target * 671.08864
        val result   = rawValue.roundToInt()
        lastResult   = result

        // ── Update result display ─────────────────────────────────────────────
        binding.textResult.text = result.toString()

        // Frequency delta in kHz (3 decimal places)
        val deltaKhz = (actual - target) * 1000.0
        val sign     = if (deltaKhz >= 0) "+" else ""
        binding.textDelta.text = "Δ = ${sign}%.3f kHz  (${actual} − ${target} MHz)".format(deltaKhz)

        // Direction hint — always remind the user that XTAL must be 0 during measurement
        val prerequisiteNote = "\n\nℹ  Measurement must be taken with XTAL 671 set to 0 on the radio."
        binding.textDirection.text = when {
            result > 0  -> "▲ Radio receives HIGH\nSet XTAL 671 to +$result$prerequisiteNote"
            result < 0  -> "▼ Radio receives LOW\nSet XTAL 671 to $result$prerequisiteNote"
            else        -> "✓ Within calibration tolerance\nXTAL 671 = 0 (no change needed)$prerequisiteNote"
        }

        binding.btnCopyResult.isEnabled = true
    }

    /** Resets the result panel to its empty/waiting state. */
    private fun showNoResult() {
        lastResult = null
        binding.textResult.text    = "—"
        binding.textDelta.text     = ""
        binding.textDirection.text = ""
        binding.btnCopyResult.isEnabled = false
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Companion
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, Xtal671CalculatorActivity::class.java)
    }
}
