package com.nicfw.tdh3editor.radio.repeaterbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeaterBookGmrsProxTest {

    @Test
    fun parseHtml_extracts_row_like_prox_result_table() {
        val html = """
            <html><body><table>
            <tr>
              <td><a href="details.php?state%5Fid=24&ID=380">462.600</a></td>
              <td></td><td></td>
              <td><a href="owner_details.php?call=WRJY601">WRJY601</a></td>
              <td>Towson</td><td>MD</td><td>OPEN</td><td>3.13</td><td>NNW</td>
            </tr>
            </table></body></html>
        """.trimIndent()

        val rows = RepeaterBookGmrsProx.parseHtml(html).onEach {
            RepeaterBookDetailsTones.stripInternalKeys(it)
        }
        assertEquals(1, rows.size)
        val o = rows[0]
        assertEquals(462.600, o.getDouble("Frequency"), 0.001)
        assertEquals(467.600, o.getDouble("Input Freq"), 0.001)
        assertEquals("WRJY601", o.getString("Callsign"))
        assertEquals("Towson", o.getString("Nearest City"))
        assertEquals("MD", o.getString("State"))
        assertEquals("OPEN", o.getString("Use"))
        assertEquals("On-air", o.getString("Operational Status"))
        assertTrue(o.getString("Notes").contains("state_id=24"))
        assertTrue(o.getString("Notes").contains("ID=380"))
    }

    @Test
    fun typicalGmrsInputMhz_adds_five_in_462_band() {
        assertEquals(467.55, RepeaterBookGmrsProx.typicalGmrsInputMhz(462.55), 0.01)
    }

    @Test
    fun typicalGmrsInputMhz_outside_band_unchanged() {
        assertEquals(446.0, RepeaterBookGmrsProx.typicalGmrsInputMhz(446.0), 0.01)
    }
}
