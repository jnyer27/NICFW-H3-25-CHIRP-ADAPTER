package com.nicfw.tdh3editor.radio.repeaterbook

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RepeaterBookDetailsTonesTest {

    @Test
    fun parseAmateurHtml_reads_uplink_and_downlink_tone_rows() {
        val html = """
            <html><body>
            <table>
              <tr><th scope="row">Uplink Tone</th><td>107.2</td></tr>
              <tr><th scope="row">Downlink Tone</th><td>114.8</td></tr>
            </table>
            </body></html>
        """.trimIndent()
        val t = RepeaterBookDetailsTones.parseAmateurHtml(html)
        assertEquals("107.2", t.uplink)
        assertEquals("114.8", t.downlink)
    }

    @Test
    fun parseGmrsHtml_reads_label_value_pairs() {
        val html = """
            <html><body><table>
              <tr><td class="gray">Uplink Tone:</td><td>141.3</td></tr>
              <tr><td class="gray">Downlink Tone:</td><td>141.3</td></tr>
            </table></body></html>
        """.trimIndent()
        val t = RepeaterBookDetailsTones.parseGmrsHtml(html)
        assertEquals("141.3", t.uplink)
        assertEquals("141.3", t.downlink)
    }

    @Test
    fun mergeTones_sets_pl_and_tsq() {
        val o = JSONObject()
        RepeaterBookDetailsTones.mergeTones(
            o,
            RepeaterBookDetailsTones.TonePair("100.0", "100.0"),
        )
        assertEquals("100.0", o.getString("PL"))
        assertEquals("100.0", o.getString("TSQ"))
    }

    @Test
    fun normalizeToneCell_rejects_dash_and_csq() {
        assertNull(RepeaterBookDetailsTones.normalizeToneCell("—"))
        assertNull(RepeaterBookDetailsTones.normalizeToneCell("CSQ"))
        assertEquals("107.2", RepeaterBookDetailsTones.normalizeToneCell("107.2"))
    }

    @Test
    fun stripInternalKeys_removes_routing_fields() {
        val o = JSONObject().apply {
            put(RepeaterBookDetailsTones.KEY_STATE_ID, "24")
            put(RepeaterBookDetailsTones.KEY_REPEATER_ID, "25")
            put("Callsign", "W3FT")
        }
        RepeaterBookDetailsTones.stripInternalKeys(o)
        assertFalse(o.has(RepeaterBookDetailsTones.KEY_STATE_ID))
        assertEquals("W3FT", o.getString("Callsign"))
    }
}
