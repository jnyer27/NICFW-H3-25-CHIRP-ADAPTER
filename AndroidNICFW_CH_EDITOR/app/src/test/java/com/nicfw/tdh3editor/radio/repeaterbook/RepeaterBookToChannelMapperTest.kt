package com.nicfw.tdh3editor.radio.repeaterbook

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RepeaterBookToChannelMapperTest {

    @Test
    fun mapsNegativeOffsetRepeater() {
        val j = JSONObject(
            "{\"Callsign\":\"W3FT\",\"Frequency\":\"146.67\",\"Input Freq\":\"146.07\"," +
                "\"PL\":\"107.2\",\"TSQ\":\"107.2\",\"Nearest City\":\"Towson\",\"State\":\"MD\"}",
        )
        val ch = RepeaterBookToChannelMapper.fromJson(j)!!
        assertEquals(146_670_000L, ch.freqRxHz)
        assertEquals(146_070_000L, ch.freqTxHz)
        assertEquals("-", ch.duplex)
        assertEquals(600_000L, ch.offsetHz)
        assertEquals("Tone", ch.txToneMode)
        assertEquals("Tone", ch.rxToneMode)
        assertEquals(107.2, ch.txToneVal!!, 0.01)
    }

    @Test
    fun mapsUhfSimpleSplit() {
        val j = JSONObject(
            """
            {"Callsign":"N0CALL","Frequency":446.0,"Input Freq":"441.0",
            "Nearest City":"X","State":"CO","FM Analog":"Yes"}
            """.trimIndent(),
        )
        val ch = RepeaterBookToChannelMapper.fromJson(j)!!
        assertEquals(446_000_000L, ch.freqRxHz)
        assertEquals(441_000_000L, ch.freqTxHz)
        assertEquals("-", ch.duplex)
        assertEquals(5_000_000L, ch.offsetHz)
    }

    @Test
    fun mapsDcsPlTsq_fromExportStrings() {
        val j = JSONObject(
            """
            {"Callsign":"TEST","Frequency":462.55,"Input Freq":"462.55",
            "PL":"DCS 023","TSQ":"DCS 023","Nearest City":"X","State":"OR"}
            """.trimIndent(),
        )
        val ch = RepeaterBookToChannelMapper.fromJson(j)!!
        assertEquals("DTCS", ch.txToneMode)
        assertEquals("DTCS", ch.rxToneMode)
        assertEquals(23.0, ch.txToneVal!!, 0.01)
        assertEquals(23.0, ch.rxToneVal!!, 0.01)
    }

    @Test
    fun commentLine_joinsLocation() {
        val j = JSONObject("{\"Nearest City\":\"Towson\",\"State\":\"MD\",\"County\":\"Baltimore\"}")
        val c = RepeaterBookToChannelMapper.commentLine(j)
        assertNotNull(c)
        assertEquals(true, c.contains("Towson"))
        assertEquals(true, c.contains("MD"))
    }
}
