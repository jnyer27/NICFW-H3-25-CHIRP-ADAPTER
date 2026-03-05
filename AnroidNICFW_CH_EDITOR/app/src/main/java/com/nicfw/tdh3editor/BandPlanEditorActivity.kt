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
import com.nicfw.tdh3editor.databinding.ActivityBandPlanEditorBinding
import com.nicfw.tdh3editor.radio.BandPlanEntry
import com.nicfw.tdh3editor.radio.EepromConstants
import com.nicfw.tdh3editor.radio.EepromParser

/**
 * Displays all 20 Band Plan slots and allows the user to edit each one.
 *
 * Entries are written back to [EepromHolder.eeprom] when the user taps Save.
 * [EepromHolder.bandPlan] is refreshed after each save so validation and the
 * channel list "(BP)" display reflect the updated Band Plan immediately.
 */
class BandPlanEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBandPlanEditorBinding
    private val slots = arrayOfNulls<BandPlanEntry>(EepromConstants.BANDPLAN_NUM_ENTRIES)

    // ── Activity result launcher for editing a single entry ───────────────────

    private val entryEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val idx = result.data?.getIntExtra(BandPlanEntryEditActivity.EXTRA_SLOT_INDEX, -1) ?: -1
            if (idx in 0 until EepromConstants.BANDPLAN_NUM_ENTRIES) {
                // Re-read the updated slot from EepromHolder.eeprom
                val eep = EepromHolder.eeprom ?: return@registerForActivityResult
                val all = EepromParser.readAllBandPlanSlots(eep)
                if (all != null) {
                    slots[idx] = all[idx]
                    binding.recyclerBandPlan.adapter?.notifyItemChanged(idx)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBandPlanEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Load current band plan from EEPROM (or empty slots if no magic yet)
        val eep = EepromHolder.eeprom
        if (eep == null) {
            Toast.makeText(this, "No EEPROM data loaded", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val loaded = EepromParser.readAllBandPlanSlots(eep)
        if (loaded != null) {
            loaded.forEachIndexed { i, entry -> slots[i] = entry }
        }
        // If no magic (null), all slots stay null → treated as empty

        // RecyclerView
        binding.recyclerBandPlan.layoutManager = LinearLayoutManager(this)
        binding.recyclerBandPlan.adapter = BandPlanSlotsAdapter()

        // Buttons
        binding.btnBpCancel.setOnClickListener { finish() }
        binding.btnBpSave.setOnClickListener { saveAndFinish() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save
    // ─────────────────────────────────────────────────────────────────────────

    private fun saveAndFinish() {
        val eep = EepromHolder.eeprom ?: return
        // Build a full 20-element Array<BandPlanEntry>; empty slots become zeros
        val toWrite = Array(EepromConstants.BANDPLAN_NUM_ENTRIES) { i ->
            slots[i] ?: BandPlanEntry(startHz = 0L, endHz = 0L, txAllowed = false)
        }
        EepromParser.writeBandPlan(eep, toWrite)
        EepromHolder.eeprom   = eep
        EepromHolder.bandPlan = EepromParser.parseBandPlan(eep)
        Toast.makeText(this, "Band Plan saved to EEPROM buffer", Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Adapter
    // ─────────────────────────────────────────────────────────────────────────

    inner class BandPlanSlotsAdapter :
        RecyclerView.Adapter<BandPlanSlotsAdapter.SlotViewHolder>() {

        inner class SlotViewHolder(val card: MaterialCardView) : RecyclerView.ViewHolder(card) {
            val tvNumber:    TextView = card.findViewById(R.id.bpSlotNumber)
            val tvFreqRange: TextView = card.findViewById(R.id.bpFreqRange)
            val tvDetails:   TextView = card.findViewById(R.id.bpDetails)
        }

        override fun getItemCount() = EepromConstants.BANDPLAN_NUM_ENTRIES

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_band_plan_entry, parent, false) as MaterialCardView
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
                holder.tvFreqRange.text = "%.3f – %.3f MHz".format(startMhz, endMhz)

                val txLabel  = if (entry.txAllowed) "TX: ✓" else "TX: ✗"
                val modLabel = EepromConstants.BANDPLAN_MOD_LABELS.getOrElse(entry.modRaw) { "Mod ${entry.modRaw}" }
                val bwLabel  = EepromConstants.BANDPLAN_BW_LABELS.getOrElse(entry.bwRaw)  { "BW ${entry.bwRaw}" }
                val pwrLabel = if (entry.maxPower == 0) "Pwr: Any"
                               else "Pwr: ${EepromConstants.powerToWatts(entry.maxPower.toString())}"
                holder.tvDetails.text = "$txLabel  Mod: $modLabel  BW: $bwLabel  $pwrLabel"
            }

            holder.card.setOnClickListener {
                entryEditLauncher.launch(
                    BandPlanEntryEditActivity.intent(this@BandPlanEditorActivity, position)
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Companion
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, BandPlanEditorActivity::class.java)
    }
}
