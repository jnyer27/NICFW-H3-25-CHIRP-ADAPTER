package com.nicfw.tdh3editor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.nicfw.tdh3editor.radio.Channel
import com.nicfw.tdh3editor.radio.EepromConstants

class ChannelAdapter(
    private val onChannelClick: (Channel) -> Unit
) : ListAdapter<Channel, ChannelAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return ViewHolder(v as MaterialCardView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onChannelClick)
    }

    class ViewHolder(private val card: MaterialCardView) : RecyclerView.ViewHolder(card) {
        private val channelNumber:    TextView     = card.findViewById(R.id.channelNumber)
        private val channelFreq:      TextView     = card.findViewById(R.id.channelFreq)
        private val channelName:      TextView     = card.findViewById(R.id.channelName)
        private val channelGroups:    TextView     = card.findViewById(R.id.channelGroups)
        private val channelToneGroup: LinearLayout = card.findViewById(R.id.channelToneGroup)
        private val channelTxTone:    TextView     = card.findViewById(R.id.channelTxTone)
        private val channelRxTone:    TextView     = card.findViewById(R.id.channelRxTone)
        private val channelDuplex:    TextView     = card.findViewById(R.id.channelDuplex)

        fun bind(channel: Channel, onChannelClick: (Channel) -> Unit) {
            channelNumber.text = card.context.getString(R.string.channel_number, channel.number)
            if (channel.empty) {
                channelFreq.text   = card.context.getString(R.string.empty_channel)
                channelName.text   = ""
                channelGroups.text = ""
                channelGroups.visibility = View.GONE
                channelDuplex.text = ""
                channelTxTone.text = ""
                channelRxTone.text = ""
                channelToneGroup.visibility = View.GONE
            } else {
                channelFreq.text   = channel.displayFreq()
                channelName.text   = channel.name.ifEmpty { "-" }
                channelDuplex.text = channel.displayDuplex()

                // Groups — show label text (e.g. "All", "MURS") when available;
                // fall back to the letter code when the label is blank.
                val groups = buildGroupsDisplay(channel)
                channelGroups.text = groups
                channelGroups.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE

                // Tones
                val tx = channel.displayTxTone()
                val rx = channel.displayRxTone()
                channelTxTone.text = if (tx.isNotEmpty()) "T: $tx" else ""
                channelRxTone.text = if (rx.isNotEmpty()) "R: $rx" else ""
                channelToneGroup.visibility = if (tx.isEmpty() && rx.isEmpty()) View.GONE else View.VISIBLE
            }
            card.setOnClickListener { onChannelClick(channel) }
        }

        /**
         * Builds the groups display string for the card, preferring the user-defined
         * label (e.g. "All", "MURS") over the raw letter code.
         * Falls back to the letter when the label is blank or EEPROM hasn't been loaded.
         */
        private fun buildGroupsDisplay(channel: Channel): String {
            val labels = EepromHolder.groupLabels
            return listOf(channel.group1, channel.group2, channel.group3, channel.group4)
                .filter { it != "None" }
                .joinToString("  ") { letter ->
                    val idx   = EepromConstants.GROUP_LETTERS.indexOf(letter)
                    val label = labels.getOrNull(idx)?.trim() ?: ""
                    if (label.isEmpty()) letter else label
                }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(a: Channel, b: Channel) = a.number == b.number
        override fun areContentsTheSame(a: Channel, b: Channel) = a == b
    }
}
