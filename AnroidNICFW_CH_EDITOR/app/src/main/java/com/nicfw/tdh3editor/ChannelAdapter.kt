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

                // Groups
                val groups = channel.displayGroups()
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
    }

    object DiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(a: Channel, b: Channel) = a.number == b.number
        override fun areContentsTheSame(a: Channel, b: Channel) = a == b
    }
}
