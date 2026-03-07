package com.nicfw.tdh3editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.nicfw.tdh3editor.databinding.ActivityScanPresetEntryEditBinding
import com.nicfw.tdh3editor.radio.EepromConstants
import com.nicfw.tdh3editor.radio.EepromParser
import com.nicfw.tdh3editor.radio.ScanPresetEntry

/**
 * Edits a single Scan Preset slot.
 *
 * Receives [EXTRA_SLOT_INDEX] (0-based, 0–19) via Intent.
 * On Save, writes the updated entry back into [EepromHolder.eeprom] and returns
 * [Activity.RESULT_OK] with the slot index so [ScanPresetEditorActivity] can
 * refresh just that row.
 */
class ScanPresetEntryEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanPresetEntryEditBinding
    private var slotIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanPresetEntryEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }

        slotIndex = intent.getIntExtra(EXTRA_SLOT_INDEX, 0)
            .coerceIn(0, EepromConstants.SCANPRESET_NUM_ENTRIES - 1)
        binding.toolbar.title = "Scan Preset #${slotIndex + 1}"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // ── Spinners ──────────────────────────────────────────────────────────
        binding.spinnerSpModulation.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            EepromConstants.SCANPRESET_MOD_LABELS
        )
        binding.spinnerSpUltrascan.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            EepromConstants.SCANPRESET_ULTRASCAN_LABELS
        )

        // ── Populate from current EEPROM slot ─────────────────────────────────
        val eep = EepromHolder.eeprom
        if (eep == null) {
            Toast.makeText(this, "No EEPROM data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val slots = EepromParser.readAllScanPresetSlots(eep)
        val entry: ScanPresetEntry = slots.getOrNull(slotIndex) ?: ScanPresetEntry()

        if (!entry.isEmpty) {
            binding.editSpStartFreq.setText("%.5f".format(entry.startHz / 1_000_000.0))
            binding.editSpEndFreq.setText("%.5f".format(entry.endHz   / 1_000_000.0))
            binding.editSpStep.setText("%.2f".format(entry.stepHz / 1000.0))
            binding.editSpResume.setText(entry.scanResume.toString())
            binding.editSpPersist.setText(entry.scanPersist.toString())
            binding.spinnerSpModulation.setSelection(entry.modRaw.coerceIn(0, 3))
            binding.spinnerSpUltrascan.setSelection(entry.ultrascan.coerceIn(0, 7))
            binding.editSpLabel.setText(entry.label)
        } else {
            // Defaults for a new empty slot
            binding.editSpStep.setText("12.50")
            binding.editSpResume.setText("0")
            binding.editSpPersist.setText("0")
            binding.spinnerSpUltrascan.setSelection(7)   // default = max speed
        }

        // ── Button handlers ───────────────────────────────────────────────────
        binding.btnSpEntryCancel.setOnClickListener { finish() }
        binding.btnSpEntrySave.setOnClickListener   { saveAndFinish() }
        binding.btnSpClearEntry.setOnClickListener  { clearAndFinish() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save
    // ─────────────────────────────────────────────────────────────────────────

    private fun saveAndFinish() {
        val eep = EepromHolder.eeprom ?: return

        val startStr = binding.editSpStartFreq.text?.toString()?.trim()
        val endStr   = binding.editSpEndFreq.text?.toString()?.trim()
        val stepStr  = binding.editSpStep.text?.toString()?.trim()

        if (startStr.isNullOrBlank() || endStr.isNullOrBlank()) {
            Toast.makeText(this, "Enter start and end frequencies", Toast.LENGTH_SHORT).show()
            return
        }
        val startMhz = startStr.toDoubleOrNull()
        val endMhz   = endStr.toDoubleOrNull()
        if (startMhz == null || endMhz == null || startMhz <= 0.0 || endMhz <= startMhz) {
            Toast.makeText(this, "Invalid frequency range", Toast.LENGTH_SHORT).show()
            return
        }

        val stepKhz = stepStr?.toDoubleOrNull() ?: 12.5
        if (stepKhz <= 0.0) {
            Toast.makeText(this, "Step must be greater than 0", Toast.LENGTH_SHORT).show()
            return
        }

        // Round to nearest 10 Hz (EEPROM stores in 10 Hz units)
        val startHz = ((startMhz * 1_000_000.0).toLong() / 10L) * 10L
        val endHz   = ((endMhz   * 1_000_000.0).toLong() / 10L) * 10L

        // Round step to nearest 10 Hz
        val stepHz  = ((stepKhz * 1000.0).toInt() / 10) * 10

        val resume  = binding.editSpResume.text?.toString()?.trim()
            ?.toIntOrNull()?.coerceIn(0, 255) ?: 0
        val persist = binding.editSpPersist.text?.toString()?.trim()
            ?.toIntOrNull()?.coerceIn(0, 255) ?: 0
        val modRaw    = binding.spinnerSpModulation.selectedItemPosition.coerceIn(0, 3)
        val ultrascan = binding.spinnerSpUltrascan.selectedItemPosition.coerceIn(0, 7)
        val label     = binding.editSpLabel.text?.toString()?.trim()?.take(8) ?: ""

        val entry = ScanPresetEntry(
            startHz     = startHz,
            endHz       = endHz,
            stepHz      = stepHz,
            scanResume  = resume,
            scanPersist = persist,
            modRaw      = modRaw,
            ultrascan   = ultrascan,
            label       = label
        )

        writeSingleSlot(eep, entry)
        setResult(Activity.RESULT_OK, resultIntent())
        finish()
    }

    private fun clearAndFinish() {
        val eep = EepromHolder.eeprom ?: return
        writeSingleSlot(eep, ScanPresetEntry())   // all zeros = empty
        setResult(Activity.RESULT_OK, resultIntent())
        finish()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Writes just one slot back into [eep] without touching the other 19.
     * Re-reads all slots first to avoid overwriting neighbouring entries.
     */
    private fun writeSingleSlot(eep: ByteArray, entry: ScanPresetEntry) {
        val allSlots = EepromParser.readAllScanPresetSlots(eep)
        allSlots[slotIndex] = entry
        EepromParser.writeScanPresets(eep, allSlots)
        EepromHolder.eeprom = eep
    }

    private fun resultIntent() =
        Intent().putExtra(EXTRA_SLOT_INDEX, slotIndex)

    // ─────────────────────────────────────────────────────────────────────────
    // Companion
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        const val EXTRA_SLOT_INDEX = "slot_index"

        fun intent(context: Context, slotIndex: Int): Intent =
            Intent(context, ScanPresetEntryEditActivity::class.java)
                .putExtra(EXTRA_SLOT_INDEX, slotIndex)
    }
}
