package com.nicfw.tdh3editor.radio.repeaterbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeaterBookProx2Test {

    @Test
    fun parseHtml_extracts_prox2_style_row() {
        val html = """
            <html><body><table>
            <tr>
              <td><a href="details.php?state_id=24&ID=25">146.6700</a></td>
              <td>-0.6 MHz</td>
              <td>107.2</td>
              <td>W3FT</td>
              <td>Towson</td>
              <td>MD</td>
              <td>OPEN</td>
              <td>FM EchoLink</td>
              <td>1.93 NNW</td>
              <td>&#x1F7E2;</td>
            </tr>
            </table></body></html>
        """.trimIndent()

        val rows = RepeaterBookProx2.parseHtml(html).onEach {
            RepeaterBookDetailsTones.stripInternalKeys(it)
        }
        assertEquals(1, rows.size)
        val o = rows[0]
        assertEquals(146.67, o.getDouble("Frequency"), 0.001)
        assertEquals(146.07, o.getDouble("Input Freq"), 0.001)
        assertEquals("107.2", o.getString("PL"))
        assertEquals("W3FT", o.getString("Callsign"))
        assertEquals("Towson", o.getString("Nearest City"))
        assertEquals("MD", o.getString("State"))
        assertEquals("OPEN", o.getString("Use"))
        assertEquals("On-air", o.getString("Operational Status"))
        assertTrue(o.getString("Notes").contains("state_id=24"))
        assertTrue(o.getString("Notes").contains("ID=25"))
    }

    @Test
    fun parseHtml_yellow_status_is_testing_reduced() {
        val html = """
            <html><body><table>
            <tr>
              <td><a href="details.php?state_id=24&ID=23542">145.3900</a></td>
              <td>-0.6 MHz</td>
              <td>&#x2014;</td>
              <td>W4ATN</td>
              <td>Laurel</td>
              <td>MD</td>
              <td>OPEN</td>
              <td>FM</td>
              <td>23.55 SW</td>
              <td>&#x1F7E1;</td>
            </tr>
            </table></body></html>
        """.trimIndent()

        val o = RepeaterBookProx2.parseHtml(html).single().also {
            RepeaterBookDetailsTones.stripInternalKeys(it)
        }
        assertEquals("Testing/Reduced", o.getString("Operational Status"))
        assertFalse(o.has("PL"))
    }

    @Test
    fun inputMhzFromOffset_simplex_on_dash() {
        assertEquals(446.0, RepeaterBookProx2.inputMhzFromOffset(446.0, "—"), 0.0)
    }

    @Test
    fun inputMhzFromOffset_positive_split() {
        assertEquals(147.63, RepeaterBookProx2.inputMhzFromOffset(147.03, "+0.6 MHz"), 0.01)
    }

    @Test
    fun operationalStatusFromCell_text() {
        assertEquals("Off-air", RepeaterBookProx2.operationalStatusFromCell("Off-Air"))
        assertEquals("Testing/Reduced", RepeaterBookProx2.operationalStatusFromCell("Testing"))
    }
}
