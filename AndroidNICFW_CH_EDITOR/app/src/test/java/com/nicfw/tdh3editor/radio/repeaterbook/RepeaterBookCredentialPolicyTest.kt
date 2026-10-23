package com.nicfw.tdh3editor.radio.repeaterbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeaterBookCredentialPolicyTest {

    private fun personalToken(): String = "rbu" + "app_" + "abcd2345efgh"

    private fun sharedToken(): String = "ap" + "p_" + "abcd2345efgh"

    @Test
    fun acceptsPersonalTokenOnly() {
        val accepted = RepeaterBookTokenPolicy.decide(personalToken())
        assertTrue(accepted is RepeaterBookTokenPolicy.Decision.Accepted)
        assertEquals(
            RepeaterBookTokenPolicy.Reason.SHARED_APP_TOKEN,
            (RepeaterBookTokenPolicy.decide(sharedToken()) as RepeaterBookTokenPolicy.Decision.Rejected).reason,
        )
        assertEquals(
            RepeaterBookTokenPolicy.Reason.URL_OR_QUERY,
            (RepeaterBookTokenPolicy.decide("https://www.repeaterbook.com/api/export.php?token=" + personalToken())
                as RepeaterBookTokenPolicy.Decision.Rejected).reason,
        )
        assertEquals(
            RepeaterBookTokenPolicy.Reason.BLANK,
            (RepeaterBookTokenPolicy.decide("  ") as RepeaterBookTokenPolicy.Decision.Rejected).reason,
        )
    }

    @Test
    fun stripsHeaderLabelBeforeCheckingTheToken() {
        val decision = RepeaterBookTokenPolicy.decide(
            RepeaterBookTokenPolicy.HEADER_NAME + ": " + personalToken(),
        )
        assertTrue(decision is RepeaterBookTokenPolicy.Decision.Accepted)
    }

    @Test
    fun sendsHeaderOnlyOnExportApi() {
        val token = personalToken()
        val api = "https://www.repeaterbook.com/api/export.php?state_id=51"
        assertEquals(token, RepeaterBookTokenPolicy.headerValueForApiRequest(api, token))
        assertNull(
            RepeaterBookTokenPolicy.headerValueForApiRequest(
                "https://www.repeaterbook.com/gmrs/prox_result.php?lat=40&long=-75",
                token,
            ),
        )
        assertNull(RepeaterBookTokenPolicy.headerValueForApiRequest(api, sharedToken()))
        assertEquals(RepeaterBookTokenPolicy.HEADER_NAME, "X-RB-App-Token")
    }

    @Test
    fun stripsCredentialQueryWithoutKeepingTheSecret() {
        val secret = personalToken()
        val cleaned = RepeaterBookTokenPolicy.stripCredentialQuery(
            "https://www.repeaterbook.com/api/export.php?country=Canada&token=$secret&state_id=51&api_key=$secret",
        )
        assertEquals(
            "https://www.repeaterbook.com/api/export.php?country=Canada&state_id=51",
            cleaned,
        )
        assertFalse(cleaned.contains(secret))
        val redacted = RepeaterBookTokenPolicy.redact("rejected $secret and " + sharedToken())
        assertFalse(redacted.contains(secret))
        assertFalse(redacted.contains(sharedToken()))
    }

    @Test
    fun honorsFullRetryAfterAndDoesNotCapIt() {
        assertEquals(5_000_000L, RepeaterBookRetryPolicy.retryAfterMillis("5000", 0L))
        assertEquals(0L, RepeaterBookRetryPolicy.retryAfterMillis("0", 0L))
        val later = RepeaterBookRetryPolicy.retryAfterMillis(
            "Sun, 06 Nov 1994 08:49:37 GMT",
            784111717000L,
        )
        assertEquals(60_000L, later)
    }

    @Test
    fun doesNotTreatAuthScopeOrUserAgentFailuresAsRetryable() {
        assertTrue(RepeaterBookRetryPolicy.isTerminalFailure(401, ""))
        assertTrue(RepeaterBookRetryPolicy.isTerminalFailure(403, "{\"error_code\":\"auth_revoked\"}"))
        assertTrue(
            RepeaterBookRetryPolicy.isTerminalFailure(
                400,
                "{\"error_code\":\"auth_scope_denied\"}",
            ),
        )
        assertTrue(RepeaterBookRetryPolicy.isTerminalFailure(400, "{\"error_code\":\"ua_mismatch\"}"))
        assertFalse(RepeaterBookRetryPolicy.isTerminalFailure(429, ""))
        assertFalse(RepeaterBookRetryPolicy.isTerminalFailure(500, "{\"error_code\":\"unavailable\"}"))
    }

    @Test
    fun rateLimitGateBlocksUntilTheDeadline() {
        RepeaterBookRateLimit.clearForTests()
        try {
            assertEquals(0L, RepeaterBookRateLimit.remainingMs(1_000L))
            RepeaterBookRateLimit.blockUntil(11_000L)
            assertEquals(10_000L, RepeaterBookRateLimit.remainingMs(1_000L))
            assertEquals(0L, RepeaterBookRateLimit.remainingMs(11_000L))
        } finally {
            RepeaterBookRateLimit.clearForTests()
        }
    }
}
