package com.nicfw.tdh3editor

import com.nicfw.tdh3editor.radio.Protocol
import org.junit.Assert.assertEquals
import org.junit.Test

class ProtocolTest {

    @Test
    fun checksum() {
        val data = byteArrayOf(0x12, 0x34, 0x56)
        assertEquals((0x12 + 0x34 + 0x56) % 256, Protocol.checksum(data))
    }

    @Test
    fun constants() {
        assertEquals(32, Protocol.BLOCK_SIZE)
        assertEquals(8192, Protocol.EEPROM_SIZE)
        assertEquals(256, Protocol.NUM_BLOCKS)
    }
}
