package com.nicfw.tdh3editor.radio.repeaterbook

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class RepeaterBookJsonParserTest {

    @Test
    fun parseResults_readsResultsArray() {
        val json =
            """{"count":2,"results":[{"Callsign":"W3FT","Frequency":"146.67"},{"Callsign":"N0CALL","Frequency":446.0}]}"""
        val list = RepeaterBookJsonParser.parseResults(json)
        assertEquals(2, list.size)
        assertEquals("W3FT", list[0].optString("Callsign"))
    }

    @Test
    fun parseResults_throwsOnApiError() {
        val err = """{"ok":false,"error_code":"auth_missing","message":"Authorization required."}"""
        try {
            RepeaterBookJsonParser.parseResults(err)
            fail("expected IOException")
        } catch (_: IOException) {
            // ok
        }
    }
}
