package com.nicfw.tdh3editor

import com.nicfw.tdh3editor.radio.EepromConstants
import com.nicfw.tdh3editor.radio.EepromParser
import com.nicfw.tdh3editor.radio.Protocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * EEPROM parsing tests. Sample layout matches tests/build_sample_image.py:
 * Settings magic 0xD82F at 0x1900; Channel 1 at 0x0040: 146.52 MHz simplex, name "CH0".
 */
class EepromParserTest {

    private fun buildMinimalEeprom(): ByteArray {
        val data = ByteArray(Protocol.EEPROM_SIZE)
        // Settings magic at 0x1900 (big-endian)
        data[0x1900] = (EepromConstants.MAGIC_SETTINGS_V25 shr 8).toByte()
        data[0x1901] = (EepromConstants.MAGIC_SETTINGS_V25 and 0xFF).toByte()
        // Channel 0 (display channel 1): 146.52 MHz = 14652000 in 10 Hz units (big-endian u32)
        val freq10 = 14652000L
        data[0x0040] = (freq10 shr 24).toByte()
        data[0x0041] = (freq10 shr 16).toByte()
        data[0x0042] = (freq10 shr 8).toByte()
        data[0x0043] = freq10.toByte()
        data[0x0044] = (freq10 shr 24).toByte()
        data[0x0045] = (freq10 shr 16).toByte()
        data[0x0046] = (freq10 shr 8).toByte()
        data[0x0047] = freq10.toByte()
        data[0x004C] = 1 // txPower
        data[0x0054] = 'C'.code.toByte()
        data[0x0055] = 'H'.code.toByte()
        data[0x0056] = '0'.code.toByte()
        return data
    }

    @Test
    fun parseChannel1_fromMinimalImage() {
        val eeprom = buildMinimalEeprom()
        val ch = EepromParser.parseChannel(eeprom, 1)
        assertFalse(ch.empty)
        assertEquals(146_520_000L, ch.freqRxHz)
        assertEquals(146_520_000L, ch.freqTxHz)
        assertEquals("", ch.duplex)
        assertEquals("1", ch.power)
        assertEquals("CH0", ch.name)
    }

    @Test
    fun parseEmptyChannel() {
        val eeprom = ByteArray(Protocol.EEPROM_SIZE)
        // Channel 2 (index 1) is all zeros -> empty
        val ch = EepromParser.parseChannel(eeprom, 2)
        assertTrue(ch.empty)
    }

    @Test
    fun roundtripChannel() {
        val eeprom = buildMinimalEeprom()
        val ch = EepromParser.parseChannel(eeprom, 1)
        ch.name = "TEST"
        ch.bandwidth = "Narrow"
        EepromParser.writeChannel(eeprom, ch)
        val ch2 = EepromParser.parseChannel(eeprom, 1)
        assertEquals("TEST", ch2.name)
        assertEquals("Narrow", ch2.bandwidth)
    }
}
