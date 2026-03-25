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
    fun parseGmrsHtml_thTd_numericTones() {
        val html = """
            <html><body><table>
            <tr><th>Uplink Tone:</th><td>192.8</td></tr>
            <tr><th>Downlink Tone:</th><td>192.8</td></tr>
            </table></body></html>
        """.trimIndent()
        val p = RepeaterBookDetailsTones.parseGmrsHtml(html)
        assertEquals("192.8", p.uplink)
        assertEquals("192.8", p.downlink)
    }

    @Test
    fun parseGmrsHtml_loginWall_returnsNullTones() {
        val html = """
            <html><body><table>
            <tr><th>Uplink Tone:</th><td><a href="/login">LOG IN TO VIEW</a></td></tr>
            <tr><th>Downlink Tone:</th><td>LOG IN TO VIEW</td></tr>
            </table></body></html>
        """.trimIndent()
        val p = RepeaterBookDetailsTones.parseGmrsHtml(html)
        assertNull(p.uplink)
        assertNull(p.downlink)
    }

    @Test
    fun parseGmrsHtml_travelTone_ignored() {
        val html = """
            <html><body><table>
            <tr><th>Travel Tone:</th><td>Yes</td></tr>
            <tr><th>Uplink Tone:</th><td>88.5</td></tr>
            </table></body></html>
        """.trimIndent()
        val p = RepeaterBookDetailsTones.parseGmrsHtml(html)
        assertEquals("88.5", p.uplink)
        assertNull(p.downlink)
    }

    @Test
    fun parseGmrsHtml_duplicateUplink_lastWins() {
        val html = """
            <html><body><table>
            <tr><th>Uplink Tone:</th><td>100.0</td></tr>
            <tr><th>Uplink Tone:</th><td>123.0</td></tr>
            </table></body></html>
        """.trimIndent()
        val p = RepeaterBookDetailsTones.parseGmrsHtml(html)
        assertEquals("123.0", p.uplink)
    }

    @Test
    fun normalizeToneCell_rejectsLoginPhrases() {
        assertNull(RepeaterBookDetailsTones.normalizeToneCell("LOG IN TO VIEW"))
        assertNull(RepeaterBookDetailsTones.normalizeToneCell("log in to view"))
        assertNull(RepeaterBookDetailsTones.normalizeToneCell("LOGIN TO VIEW"))
    }

    @Test
    fun normalizeGmrsDetailLabel_stripsTrailingColon() {
        assertEquals(
            "uplink tone",
            RepeaterBookDetailsTones.normalizeGmrsDetailLabel("Uplink Tone:"),
        )
    }

    @Test
    fun normalizeRbNumericIdSegment_stripsLeadingZeros() {
        assertEquals("6", RepeaterBookDetailsTones.normalizeRbNumericIdSegment("06"))
        assertEquals("48", RepeaterBookDetailsTones.normalizeRbNumericIdSegment("048"))
        assertEquals("123", RepeaterBookDetailsTones.normalizeRbNumericIdSegment("0123"))
    }

    @Test
    fun gmrsCompoundRepeaterKey_normalizesNumericParts() {
        assertEquals("48-5", RepeaterBookDetailsTones.gmrsCompoundRepeaterKey("048", "005"))
    }

    @Test
    fun normalizeRbNumericIdSegment_leavesAlphanumericState() {
        assertEquals("CA01", RepeaterBookDetailsTones.normalizeRbNumericIdSegment("CA01"))
    }

    @Test
    fun gmrsExportRowMatches_stateAndRptrId_separateFields() {
        val row = JSONObject().apply {
            put("State ID", "41")
            put("Rptr ID", "2")
        }
        assertEquals(
            true,
            RepeaterBookDetailsTones.gmrsExportRowMatches(row, "41", "2", 462.55, "WXYZ"),
        )
        assertEquals(
            true,
            RepeaterBookDetailsTones.gmrsExportRowMatches(row, "041", "002", 462.55, "WXYZ"),
        )
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
