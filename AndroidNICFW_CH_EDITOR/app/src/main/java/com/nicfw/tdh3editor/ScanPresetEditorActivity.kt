package com.nicfw.tdh3editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.nicfw.tdh3editor.databinding.ActivityScanPresetEditorBinding
import com.nicfw.tdh3editor.radio.EepromConstants
import com.nicfw.tdh3editor.radio.EepromParser
import com.nicfw.tdh3editor.radio.ScanPresetEntry

/**
 * Displays all 20 Scan Preset slots and allows the user to edit each one.
 *
 * Entries are written back to [EepromHolder.eeprom] when the user taps Save.
 * [EepromHolder.scanPresets] is refreshed after each save so the updated
 * presets are immediately available to the rest of the app.
 */
class ScanPresetEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanPresetEditorBinding
    private val slots = arrayOfNulls<ScanPresetEntry>(EepromConstants.SCANPRESET_NUM_ENTRIES)

    // ── Activity result launcher for editing a single entry ───────────────────

    private val entryEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val idx = result.data?.getIntExtra(ScanPresetEntryEditActivity.EXTRA_SLOT_INDEX, -1) ?: -1
            if (idx in 0 until EepromConstants.SCANPRESET_NUM_ENTRIES) {
                // Re-read the updated slot from EepromHolder.eeprom
                val eep = EepromHolder.eeprom ?: return@registerForActivityResult
                val all = EepromParser.readAllScanPresetSlots(eep)
                slots[idx] = all[idx]
                binding.recyclerScanPresets.adapter?.notifyItemChanged(idx)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanPresetEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Load current scan presets from EEPROM
        val eep = EepromHolder.eeprom
        if (eep == null) {
            Toast.makeText(this, "No EEPROM data loaded", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val loaded = EepromParser.readAllScanPresetSlots(eep)
        loaded.forEachIndexed { i, entry -> slots[i] = entry }

        // RecyclerView
        binding.recyclerScanPresets.layoutManager = LinearLayoutManager(this)
        binding.recyclerScanPresets.adapter = ScanPresetSlotsAdapter()

        // Buttons
        binding.btnSpCancel.setOnClickListener { finish() }
        binding.btnSpSave.setOnClickListener   { saveAndFinish() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save
    // ─────────────────────────────────────────────────────────────────────────

    private fun saveAndFinish() {
        val eep = EepromHolder.eeprom ?: return
        // Build a full 20-element Array<ScanPresetEntry>; null slots become empty entries
        val toWrite = Array(EepromConstants.SCANPRESET_NUM_ENTRIES) { i ->
            slots[i] ?: ScanPresetEntry()
        }
        EepromParser.writeScanPresets(eep, toWrite)
        EepromHolder.eeprom       = eep
        EepromHolder.scanPresets  = EepromParser.parseScanPresets(eep)
        Toast.makeText(this, "Scan Presets saved to EEPROM buffer", Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Adapter
    // ─────────────────────────────────────────────────────────────────────────

    inner class ScanPresetSlotsAdapter :
        RecyclerView.Adapter<ScanPresetSlotsAdapter.SlotViewHolder>() {

        inner class SlotViewHolder(val card: MaterialCardView) : RecyclerView.ViewHolder(card) {
            val tvNumber:    TextView = card.findViewById(R.id.spSlotNumber)
            val tvFreqRange: TextView = card.findViewById(R.id.spFreqRange)
            val tvDetails:   TextView = card.findViewById(R.id.spDetails)
        }

        override fun getItemCount() = EepromConstants.SCANPRESET_NUM_ENTRIES

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_scan_preset_entry, parent, false) as MaterialCardView
            v.isClickable = true
            return SlotViewHolder(v)
        }

        override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
            val entry = slots[position]
            holder.tvNumber.text = "#${position + 1}"

            if (entry == null || entry.isEmpty) {
                holder.tvFreqRange.text = "Empty"
                holder.tvDetails.text   = "Tap to configure this slot"
            } else {
                val startMhz = entry.startHz / 1_000_000.0
                val endMhz   = entry.endHz   / 1_000_000.0
                val label    = if (entry.label.isNotBlank()) "  \"${entry.label}\"" else ""
                holder.tvFreqRange.text = "%.5f – %.5f MHz$label".format(startMhz, endMhz)

                val stepKhz  = entry.stepHz / 1000.0
                val modLabel = EepromConstants.SCANPRESET_MOD_LABELS.getOrElse(entry.modRaw) { "Mod ${entry.modRaw}" }
                holder.tvDetails.text =
                    "Step: %.2f kHz  Resume: %d  Persist: %d  %s  Ultra: %d".format(
                        stepKhz, entry.scanResume, entry.scanPersist, modLabel, entry.ultrascan
                    )
            }

            holder.card.setOnClickListener {
                entryEditLauncher.launch(
                    ScanPresetEntryEditActivity.intent(this@ScanPresetEditorActivity, position)
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Companion
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, ScanPresetEditorActivity::class.java)
    }
}
