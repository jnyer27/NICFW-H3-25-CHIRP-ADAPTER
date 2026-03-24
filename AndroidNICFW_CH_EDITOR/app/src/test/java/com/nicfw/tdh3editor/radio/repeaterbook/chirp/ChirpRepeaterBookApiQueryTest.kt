package com.nicfw.tdh3editor.radio.repeaterbook.chirp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChirpRepeaterBookApiQueryTest {

    @Test
    fun us_amateur_oregon_uses_numeric_state_id() {
        val q = ChirpRepeaterBookApiQuery.toRepeaterBookQuery(
            country = "United States",
            stateUi = "Oregon",
            serviceGmrs = false,
        )
        assertTrue(q.northAmerica)
        assertEquals("United States", q.country)
        assertEquals("41", q.stateId)
        assertEquals("", q.stype)
    }

    @Test
    fun us_gmrs_all_states_uses_stype_only() {
        val q = ChirpRepeaterBookApiQuery.toRepeaterBookQuery(
            country = "United States",
            stateUi = ChirpRepeaterBookData.stateAll,
            serviceGmrs = true,
        )
        assertTrue(q.northAmerica)
        assertEquals("", q.stateId)
        assertEquals("gmrs", q.stype)
    }

    @Test
    fun us_amateur_does_not_set_stype_even_when_checked() {
        val q = ChirpRepeaterBookApiQuery.toRepeaterBookQuery(
            country = "United States",
            stateUi = ChirpRepeaterBookData.stateAll,
            serviceGmrs = false,
        )
        assertEquals("", q.stype)
    }

    @Test
    fun canada_ontario_uses_ca_prefix() {
        val q = ChirpRepeaterBookApiQuery.toRepeaterBookQuery(
            country = "Canada",
            stateUi = "Ontario",
            serviceGmrs = false,
        )
        assertTrue(q.northAmerica)
        assertEquals("CA08", q.stateId)
    }

    @Test
    fun row_australia_uses_export_row() {
        val q = ChirpRepeaterBookApiQuery.toRepeaterBookQuery(
            country = "Australia",
            stateUi = ChirpRepeaterBookData.stateAll,
            serviceGmrs = false,
        )
        assertFalse(q.northAmerica)
        assertEquals("Australia", q.country)
        assertEquals("", q.region)
    }
}
