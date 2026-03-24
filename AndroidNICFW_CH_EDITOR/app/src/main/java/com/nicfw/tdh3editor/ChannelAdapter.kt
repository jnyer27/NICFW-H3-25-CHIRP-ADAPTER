package com.nicfw.tdh3editor

import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.nicfw.tdh3editor.radio.Channel
import com.nicfw.tdh3editor.radio.EepromConstants

/**
 * RecyclerView adapter for the 198-channel list on [MainActivity].
 *
 * Normal mode — tap a card to open the channel editor.
 * Selection mode — long-press a card to enter selection mode; subsequent taps
 *   toggle selection; the in-app selection bar in MainActivity handles
 *   Move Up, Move Down, Clear, and Done operations.
 *   A drag handle appears on each selected card — touching it starts an
 *   [ItemTouchHelper] drag so the user can physically reposition that channel.
 *
 * Selection state is intentionally kept inside the adapter (not in MainActivity)
 * so it survives [submitList] updates triggered by move/clear operations.
 */
class ChannelAdapter(
    private val onChannelClick:      (Channel) -> Unit,
    private val onLongClick:         (Channel) -> Unit,
    private val onSelectionChanged:  (count: Int) -> Unit,
    private val onDragStart:         (RecyclerView.ViewHolder) -> Unit
) : ListAdapter<Channel, ChannelAdapter.ViewHolder>(DiffCallback) {

    // ── Selection state ───────────────────────────────────────────────────────

    /** True while one or more channels are selected via long-press. */
    var isSelectionMode: Boolean = false
        private set

    /** Channel slot numbers currently selected (1-based). */
    private val selectedNumbers = mutableSetOf<Int>()

    /** Snapshot of [selectedNumbers] for callers (MainActivity move/clear). */
    val selectedChannelNumbers: Set<Int> get() = selectedNumbers.toSet()

    /** Number of currently selected channels. */
    val selectedCount: Int get() = selectedNumbers.size

    // ── Selection API (called from MainActivity) ───────────────────────────────

    /**
     * Enters selection mode with [number] as the first selected channel.
     * Notifies all items so cards can show/hide the check state and drag handle.
     */
    fun enterSelectionMode(number: Int) {
        isSelectionMode = true
        selectedNumbers.clear()
        selectedNumbers.add(number)
        notifyDataSetChanged()
        onSelectionChanged(selectedNumbers.size)
    }

    /**
     * Exits selection mode and clears all selections.
     * Called when the Done button or back navigation dismisses the selection bar.
     */
    fun exitSelectionMode() {
        isSelectionMode = false
        selectedNumbers.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    /**
     * Toggles selection for [number]. Notifies the affected item.
     * Returns the new selection count.
     */
    fun toggleSelection(number: Int): Int {
        if (number in selectedNumbers) selectedNumbers.remove(number)
        else selectedNumbers.add(number)

        val pos = currentList.indexOfFirst { it.number == number }
        if (pos >= 0) notifyItemChanged(pos)

        onSelectionChanged(selectedNumbers.size)
        return selectedNumbers.size
    }

    /**
     * Selects all non-empty channels in [currentList] and enters selection mode.
     * Used by the search bar's "Select All" button to bulk-select all visible matches.
     */
    fun selectAllVisible() {
        val targets = currentList.filter { !it.empty }
        if (targets.isEmpty()) return
        isSelectionMode = true
        selectedNumbers.clear()
        selectedNumbers.addAll(targets.map { it.number })
        notifyDataSetChanged()
        onSelectionChanged(selectedNumbers.size)
    }

    /**
     * Replaces the selection set (called after move operations so the same
     * logical channels remain highlighted at their new slot positions).
     */
    fun updateSelection(numbers: Set<Int>) {
        selectedNumbers.clear()
        selectedNumbers.addAll(numbers)
        notifyDataSetChanged()
        onSelectionChanged(selectedNumbers.size)
    }

    // ── Adapter overrides ──────────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        val card = v as MaterialCardView
        // Enable the Material checked-state overlay (shows a checkmark when isChecked = true)
        card.isCheckable = true
        return ViewHolder(card)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    inner class ViewHolder(private val card: MaterialCardView) : RecyclerView.ViewHolder(card) {

        private val channelNumber:    TextView     = card.findViewById(R.id.channelNumber)
        private val channelFreq:      TextView     = card.findViewById(R.id.channelFreq)
        private val channelGroups:    ChipGroup    = card.findViewById(R.id.channelGroups)
        private val channelName:      TextView     = card.findViewById(R.id.channelName)
        private val channelChips:     ChipGroup    = card.findViewById(R.id.channelChips)
        private val channelDragHandle: ImageView    = card.findViewById(R.id.channelDragHandle)

        @Suppress("ClickableViewAccessibility")
        fun bind(channel: Channel) {
            channelNumber.text = card.context.getString(R.string.channel_number, channel.number)
            channelGroups.removeAllViews()
            channelChips.removeAllViews()

            if (channel.empty) {
                channelFreq.text   = card.context.getString(R.string.empty_channel)
                channelGroups.visibility = View.GONE
                channelName.text   = ""
            } else {
                channelFreq.text   = channel.displayFreq()
                val groupNames = buildGroupsList(channel)
                if (groupNames.isEmpty()) {
                    channelGroups.visibility = View.GONE
                } else {
                    groupNames.forEach { label ->
                        channelGroups.addView(
                            makeChip(
                                text = label,
                                bgColor = Color.parseColor("#4B5563"),
                                textColor = Color.WHITE
                            )
                        )
                    }
                    channelGroups.visibility = View.VISIBLE
                }
                channelName.text   = channel.name.ifEmpty { "-" }
                val wattsText = EepromConstants.powerToWatts(channel.power)
                // TX restricted when the frequency's Band Plan entry has txAllowed=false,
                // or when the frequency doesn't fall in any Band Plan entry at all.
                // Falls back to the hard-coded VHF/UHF check when no band plan is loaded.
                val bp = EepromHolder.bandPlan
                val txRestricted = if (bp.isNotEmpty()) {
                    val entry = bp.firstOrNull { channel.freqRxHz in it.startHz..it.endHz }
                    entry == null || !entry.txAllowed
                } else {
                    EepromConstants.isTxRestricted(channel.freqRxHz)
                }

                // Issue #5: warn when stored channel power exceeds the radio's
                // VHF/UHF power setting cap (Tune Settings at 0x1DFB).
                // The radio silently clamps at TX time; the stored byte is unchanged.
                val rawPower = channel.power.toIntOrNull() ?: 0
                val ts = EepromHolder.tuneSettings
                val isVhf = channel.freqRxHz < EepromConstants.VHF_UHF_BOUNDARY_HZ
                val cap = if (isVhf) ts.maxPowerSettingVHF else ts.maxPowerSettingUHF
                val exceedsCap = rawPower > 0 && rawPower > cap

                val powerLabel = when {
                    wattsText != "N/T" && txRestricted && exceedsCap ->
                        "$wattsText (BP) ⚠"
                    wattsText != "N/T" && txRestricted ->
                        "$wattsText (BP)"
                    exceedsCap ->
                        "$wattsText ⚠"
                    else ->
                        wattsText
                }

                val rawPowerValue = channel.power.toIntOrNull() ?: 0
                val powerChipBg = when {
                    rawPowerValue <= 0 -> Color.parseColor("#6B7280")   // gray for N/T or 0
                    rawPowerValue > 70 -> Color.parseColor("#DC2626")   // red
                    else -> Color.parseColor("#FACC15")                  // yellow
                }
                val powerChipText = if (rawPowerValue in 1..70) Color.BLACK else Color.WHITE
                channelChips.addView(
                    makeChip(
                        text = "PWR:$powerLabel",
                        bgColor = powerChipBg,
                        textColor = powerChipText
                    )
                )

                channelChips.addView(
                    makeChip(
                        text = channel.mode,
                        bgColor = modeChipColor(channel.mode),
                        textColor = Color.WHITE
                    )
                )
                channelChips.addView(
                    makeChip(
                        text = duplexChipText(channel),
                        bgColor = duplexChipColor(channel),
                        textColor = Color.WHITE
                    )
                )

                val isNarrow = channel.bandwidth == "Narrow"
                channelChips.addView(
                    makeChip(
                        text = if (isNarrow) "BW:N" else "BW:W",
                        bgColor = if (isNarrow) Color.parseColor("#D97706") else Color.parseColor("#2563EB"),
                        textColor = Color.WHITE
                    )
                )

                addToneChip(channel.txToneMode, channel.txToneVal, channel.txTonePolarity, prefix = "TX")
                addToneChip(channel.rxToneMode, channel.rxToneVal, channel.rxTonePolarity, prefix = "RX")
            }

            // Selection check state (drives the MaterialCardView checked-icon overlay)
            val isSelected = isSelectionMode && channel.number in selectedNumbers
            card.isChecked = isSelected

            // Drag handle: visible only on selected cards while in selection mode
            channelDragHandle.visibility = if (isSelected) View.VISIBLE else View.GONE
            if (isSelected) {
                channelDragHandle.setOnTouchListener { _, e ->
                    if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                        onDragStart(this@ViewHolder)
                    }
                    false
                }
            } else {
                channelDragHandle.setOnTouchListener(null)
            }

            // Touch behaviour differs by mode
            card.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(channel.number)
                } else {
                    onChannelClick(channel)
                }
            }
            card.setOnLongClickListener {
                if (!isSelectionMode) {
                    onLongClick(channel)
                }
                true
            }
        }

        /**
         * Builds the group chips list resolving group letters to user-defined labels.
         */
        private fun buildGroupsList(channel: Channel): List<String> {
            val labels = EepromHolder.groupLabels
            return listOf(channel.group1, channel.group2, channel.group3, channel.group4)
                .filter { it != "None" }
                .map { letter ->
                    val idx   = EepromConstants.GROUP_LETTERS.indexOf(letter)
                    val label = labels.getOrNull(idx)?.trim() ?: ""
                    if (label.isEmpty()) letter else label
                }
        }

        private fun addToneChip(mode: String?, value: Double?, polarity: String?, prefix: String) {
            val (text, bg, fg) = when (mode) {
                "Tone" -> Triple(
                    "$prefix:${"%.1f".format(value ?: 0.0)}Hz",
                    Color.parseColor("#06B6D4"),
                    Color.WHITE
                )
                "DTCS" -> Triple(
                    "$prefix:D${"%03d".format((value ?: 0.0).toInt())}${polarity ?: "N"}",
                    Color.parseColor("#C026D3"),
                    Color.WHITE
                )
                else -> return
            }
            channelChips.addView(makeChip(text, bg, fg))
        }

        private fun duplexChipText(channel: Channel): String = when (channel.duplex) {
            "+" -> "+${channel.offsetHz / 1_000_000L}MHz"
            "-" -> "-${channel.offsetHz / 1_000_000L}MHz"
            "split" -> "Split"
            else -> "Simplex"
        }

        private fun duplexChipColor(channel: Channel): Int = when (channel.duplex) {
            "+" -> Color.parseColor("#2563EB")      // blue
            "-" -> Color.parseColor("#9333EA")      // purple
            "split" -> Color.parseColor("#0D9488")  // teal
            else -> Color.parseColor("#4B5563")     // neutral gray (simplex)
        }

        private fun modeChipColor(mode: String): Int = when (mode.uppercase()) {
            "FM" -> Color.parseColor("#0EA5E9")    // sky
            "AM" -> Color.parseColor("#B45309")    // amber-brown
            "USB" -> Color.parseColor("#7C3AED")   // violet
            "AUTO" -> Color.parseColor("#6B7280")  // neutral gray
            else -> Color.parseColor("#4B5563")
        }

        private fun makeChip(text: String, bgColor: Int, textColor: Int): Chip {
            return Chip(card.context).apply {
                this.text = text
                isClickable = false
                isCheckable = false
                chipCornerRadius = 999f
                chipBackgroundColor = ColorStateList.valueOf(bgColor)
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                chipStrokeWidth = 0f
                chipStrokeColor = ColorStateList.valueOf(Color.TRANSPARENT)
                elevation = 0f
                setEnsureMinTouchTargetSize(false)
                minHeight = 0
                chipMinHeight = 0f
                chipStartPadding = 8f
                chipEndPadding = 8f
                textStartPadding = 0f
                textEndPadding = 0f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                gravity = android.view.Gravity.CENTER
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(a: Channel, b: Channel) = a.number == b.number
        override fun areContentsTheSame(a: Channel, b: Channel) = a == b
    }
}
