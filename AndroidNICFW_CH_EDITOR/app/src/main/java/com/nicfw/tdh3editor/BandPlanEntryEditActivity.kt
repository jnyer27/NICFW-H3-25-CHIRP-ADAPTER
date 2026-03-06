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
import com.nicfw.tdh3editor.databinding.ActivityBandPlanEntryEditBinding
import com.nicfw.tdh3editor.radio.BandPlanEntry
import com.nicfw.tdh3editor.radio.EepromConstants
import com.nicfw.tdh3editor.radio.EepromParser

/**
 * Edits a single Band Plan slot.
 *
 * Receives [EXTRA_SLOT_INDEX] (0-based, 0–19) via Intent.
 * On Save, writes the updated entry back into [EepromHolder.eeprom] and returns
 * [Activity.RESULT_OK] with the slot index so [BandPlanEditorActivity] can refresh
 * just that row.
 */
class BandPlanEntryEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBandPlanEntryEditBinding
    private var slotIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBandPlanEntryEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }

        slotIndex = intent.getIntExtra(EXTRA_SLOT_INDEX, 0).coerceIn(0, EepromConstants.BANDPLAN_NUM_ENTRIES - 1)
        binding.toolbar.title = "Band Plan Entry #${slotIndex + 1}"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // ── Spinners ──────────────────────────────────────────────────────────
        binding.spinnerBpMaxPower.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            EepromConstants.BANDPLAN_MAXPOWER_LIST
        )
        binding.spinnerBpModulation.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            EepromConstants.BANDPLAN_MOD_LABELS
        )
        binding.spinnerBpBandwidth.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            EepromConstants.BANDPLAN_BW_LABELS
        )

        // ── Populate from current EEPROM slot ─────────────────────────────────
        val eep = EepromHolder.eeprom
        if (eep == null) {
            Toast.makeText(this, "No EEPROM data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val slots = EepromParser.readAllBandPlanSlots(eep)
        val entry: BandPlanEntry? = slots?.getOrNull(slotIndex)

        if (entry != null && !entry.isEmpty) {
            binding.editBpStartFreq.setText("%.5f".format(entry.startHz / 1_000_000.0))
            binding.editBpEndFreq.setText("%.5f".format(entry.endHz   / 1_000_000.0))
            binding.switchBpTxAllowed.isChecked = entry.txAllowed
            binding.switchBpWrap.isChecked      = entry.wrap
            binding.spinnerBpMaxPower.setSelection(entry.maxPower.coerceIn(0, 255))
            binding.spinnerBpModulation.setSelection(entry.modRaw.coerceIn(0, 7))
            binding.spinnerBpBandwidth.setSelection(entry.bwRaw.coerceIn(0, 7))
        }
        // If entry is null/empty, form stays at defaults (blanks / switches off / pos 0)

        // ── Button handlers ───────────────────────────────────────────────────
        binding.btnBpEntryCancel.setOnClickListener { finish() }
        binding.btnBpEntrySave.setOnClickListener   { saveAndFinish() }
        binding.btnBpClearEntry.setOnClickListener  { clearAndFinish() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save
    // ─────────────────────────────────────────────────────────────────────────

    private fun saveAndFinish() {
        val eep = EepromHolder.eeprom ?: return

        val startStr = binding.editBpStartFreq.text?.toString()?.trim()
        val endStr   = binding.editBpEndFreq.text?.toString()?.trim()

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

        val startHz = (startMhz * 1_000_000.0).toLong()
        val endHz   = (endMhz   * 1_000_000.0).toLong()

        // Round to nearest 10 Hz (EEPROM stores in 10 Hz units)
        val startHz10 = (startHz / 10L) * 10L
        val endHz10   = (endHz   / 10L) * 10L

        val entry = BandPlanEntry(
            startHz   = startHz10,
            endHz     = endHz10,
            txAllowed = binding.switchBpTxAllowed.isChecked,
            maxPower  = binding.spinnerBpMaxPower.selectedItemPosition.coerceIn(0, 255),
            wrap      = binding.switchBpWrap.isChecked,
            modRaw    = binding.spinnerBpModulation.selectedItemPosition.coerceIn(0, 7),
            bwRaw     = binding.spinnerBpBandwidth.selectedItemPosition.coerceIn(0, 7)
        )

        writeSingleSlot(eep, entry)
        setResult(Activity.RESULT_OK, resultIntent())
        finish()
    }

    private fun clearAndFinish() {
        val eep = EepromHolder.eeprom ?: return
        writeSingleSlot(eep, BandPlanEntry(startHz = 0L, endHz = 0L, txAllowed = false))
        setResult(Activity.RESULT_OK, resultIntent())
        finish()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Writes just one slot back into [eep] without touching the other 19.
     * Writes the magic word first in case this EEPROM image never had a Band Plan.
     */
    private fun writeSingleSlot(eep: ByteArray, entry: BandPlanEntry) {
        // Re-read all slots, substitute the edited one, write all back
        val allSlots = EepromParser.readAllBandPlanSlots(eep)
            ?: Array(EepromConstants.BANDPLAN_NUM_ENTRIES) {
                BandPlanEntry(startHz = 0L, endHz = 0L, txAllowed = false)
            }
        allSlots[slotIndex] = entry
        EepromParser.writeBandPlan(eep, allSlots)
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
            Intent(context, BandPlanEntryEditActivity::class.java)
                .putExtra(EXTRA_SLOT_INDEX, slotIndex)
    }
}
