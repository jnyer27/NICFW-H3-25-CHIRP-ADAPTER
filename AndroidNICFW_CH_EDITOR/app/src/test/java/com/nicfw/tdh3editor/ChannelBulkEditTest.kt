package com.nicfw.tdh3editor

import com.nicfw.tdh3editor.radio.Channel
import com.nicfw.tdh3editor.radio.EepromParser
import com.nicfw.tdh3editor.radio.Protocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelBulkEditTest {

    @Test
    fun channelHasDuplexOffset_simplex_false() {
        assertFalse(ChannelBulkEdit.channelHasDuplexOffset(Channel(number = 1, duplex = "")))
    }

    @Test
    fun channelHasDuplexOffset_plus_true() {
        assertTrue(ChannelBulkEdit.channelHasDuplexOffset(Channel(number = 1, duplex = "+")))
    }

    @Test
    fun channelHasDuplexOffset_split_caseInsensitive() {
        assertTrue(ChannelBulkEdit.channelHasDuplexOffset(Channel(number = 1, duplex = "Split")))
    }

    @Test
    fun applyModulation_updatesSelectedNonEmpty() {
        val eeprom = ByteArray(Protocol.EEPROM_SIZE)
        val ch = Channel(
            number = 3,
            empty = false,
            freqRxHz = 146_520_000L,
            freqTxHz = 146_520_000L,
            mode = "FM",
        )
        EepromParser.writeChannel(eeprom, ch)
        val count = ChannelBulkEdit.applyModulation(eeprom, setOf(3), "AM")
        assertEquals(1, count)
        assertEquals("AM", EepromParser.parseChannel(eeprom, 3).mode)
    }

    @Test
    fun applyBusyLock_on_skipsRepeaterChannels() {
        val eeprom = ByteArray(Protocol.EEPROM_SIZE)
        val simplex = Channel(
            number = 1,
            empty = false,
            freqRxHz = 146_520_000L,
            freqTxHz = 146_520_000L,
            duplex = "",
            busyLock = false,
        )
        val repeater = Channel(
            number = 2,
            empty = false,
            freqRxHz = 146_520_000L,
            freqTxHz = 147_000_000L,
            duplex = "+",
            offsetHz = 480_000L,
            busyLock = false,
        )
        EepromParser.writeChannel(eeprom, simplex)
        EepromParser.writeChannel(eeprom, repeater)
        val (updated, skipped) = ChannelBulkEdit.applyBusyLock(eeprom, setOf(1, 2), enabled = true)
        assertEquals(1, updated)
        assertEquals(1, skipped)
        assertTrue(EepromParser.parseChannel(eeprom, 1).busyLock)
        assertFalse(EepromParser.parseChannel(eeprom, 2).busyLock)
    }
}
