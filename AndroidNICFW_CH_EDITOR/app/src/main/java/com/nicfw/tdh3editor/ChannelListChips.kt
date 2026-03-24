package com.nicfw.tdh3editor

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.nicfw.tdh3editor.radio.Channel
import com.nicfw.tdh3editor.radio.EepromConstants

/**
 * Shared [Chip] styling and content for the main channel list ([ChannelAdapter])
 * and CHIRP import preview so both stay visually consistent.
 */
object ChannelListChips {

    fun makeChip(context: Context, text: String, bgColor: Int, textColor: Int): Chip =
        Chip(context).apply {
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

    fun duplexChipColor(channel: Channel): Int = when (channel.duplex) {
        "+" -> Color.parseColor("#2563EB")
        "-" -> Color.parseColor("#9333EA")
        "split" -> Color.parseColor("#0D9488")
        else -> Color.parseColor("#4B5563")
    }

    fun modeChipColor(mode: String): Int = when (mode.uppercase()) {
        "FM" -> Color.parseColor("#0EA5E9")
        "AM" -> Color.parseColor("#B45309")
        "USB" -> Color.parseColor("#7C3AED")
        "AUTO" -> Color.parseColor("#6B7280")
        else -> Color.parseColor("#4B5563")
    }

    /** Group letter chips with user labels — same as [ChannelAdapter] left column. */
    fun populateGroupChips(group: ChipGroup, channel: Channel) {
        group.removeAllViews()
        if (channel.empty) {
            group.visibility = View.GONE
            return
        }
        val labels = buildGroupLabels(channel)
        if (labels.isEmpty()) {
            group.visibility = View.GONE
            return
        }
        val ctx = group.context
        labels.forEach { label ->
            group.addView(
                makeChip(ctx, label, Color.parseColor("#4B5563"), Color.WHITE),
            )
        }
        group.visibility = View.VISIBLE
    }

    /** PWR / mode / duplex / BW / tone chips — same as [ChannelAdapter] right column. */
    fun populateDetailChips(chipGroup: ChipGroup, channel: Channel) {
        chipGroup.removeAllViews()
        if (channel.empty) return
        val ctx = chipGroup.context

        val bp = EepromHolder.bandPlan
        val txRestricted = if (bp.isNotEmpty()) {
            val entry = bp.firstOrNull { channel.freqRxHz in it.startHz..it.endHz }
            entry == null || !entry.txAllowed
        } else {
            EepromConstants.isTxRestricted(channel.freqRxHz)
        }

        val rawPower = channel.power.toIntOrNull() ?: 0
        val ts = EepromHolder.tuneSettings
        val isVhf = channel.freqRxHz < EepromConstants.VHF_UHF_BOUNDARY_HZ
        val cap = if (isVhf) ts.maxPowerSettingVHF else ts.maxPowerSettingUHF
        val exceedsCap = rawPower > 0 && rawPower > cap

        val wattsText = EepromConstants.powerToWatts(channel.power)
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
            rawPowerValue <= 0 -> Color.parseColor("#6B7280")
            rawPowerValue > 70 -> Color.parseColor("#DC2626")
            else -> Color.parseColor("#FACC15")
        }
        val powerChipText = if (rawPowerValue in 1..70) Color.BLACK else Color.WHITE
        chipGroup.addView(
            makeChip(ctx, "PWR:$powerLabel", powerChipBg, powerChipText),
        )

        chipGroup.addView(
            makeChip(ctx, channel.mode, modeChipColor(channel.mode), Color.WHITE),
        )
        chipGroup.addView(
            makeChip(
                ctx,
                channel.duplexOffsetChipLabel(),
                duplexChipColor(channel),
                Color.WHITE,
            ),
        )

        val isNarrow = channel.bandwidth == "Narrow"
        chipGroup.addView(
            makeChip(
                ctx,
                if (isNarrow) "BW:N" else "BW:W",
                if (isNarrow) Color.parseColor("#D97706") else Color.parseColor("#2563EB"),
                Color.WHITE,
            ),
        )

        addToneChip(chipGroup, channel.txToneMode, channel.txToneVal, channel.txTonePolarity, "TX")
        addToneChip(chipGroup, channel.rxToneMode, channel.rxToneVal, channel.rxTonePolarity, "RX")
    }

    private fun buildGroupLabels(channel: Channel): List<String> {
        val labels = EepromHolder.groupLabels
        return listOf(channel.group1, channel.group2, channel.group3, channel.group4)
            .filter { it != "None" }
            .map { letter ->
                val idx = EepromConstants.GROUP_LETTERS.indexOf(letter)
                val label = labels.getOrNull(idx)?.trim() ?: ""
                if (label.isEmpty()) letter else label
            }
    }

    private fun addToneChip(
        chipGroup: ChipGroup,
        mode: String?,
        value: Double?,
        polarity: String?,
        prefix: String,
    ) {
        val ctx = chipGroup.context
        val (text, bg, fg) = when (mode) {
            "Tone" -> Triple(
                "$prefix:${"%.1f".format(value ?: 0.0)}Hz",
                Color.parseColor("#06B6D4"),
                Color.WHITE,
            )
            "DTCS" -> Triple(
                "$prefix:D${"%03d".format((value ?: 0.0).toInt())}${polarity ?: "N"}",
                Color.parseColor("#C026D3"),
                Color.WHITE,
            )
            else -> return
        }
        chipGroup.addView(makeChip(ctx, text, bg, fg))
    }
}
