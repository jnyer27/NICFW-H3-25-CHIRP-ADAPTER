package com.nicfw.tdh3editor.radio.repeaterbook

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * RepeaterBook requires the full Retry-After delay, and no retries of authentication,
 * scope, or User-Agent failures.
 */
object RepeaterBookRetryPolicy {

    /** In-call waits longer than this are reported to the user instead of blocking the search. */
    const val INLINE_WAIT_MAX_MS = 120_000L

    /** Used only when a 429 arrives without a parseable Retry-After. Not a cap on a real header. */
    const val MISSING_HEADER_BACKOFF_MS = 60_000L

    private val TERMINAL_ERROR_CODES = setOf(
        "auth_missing",
        "auth_invalid",
        "auth_inactive",
        "auth_revoked",
        "auth_scope_denied",
        "ua_mismatch",
    )

    private val HTTP_DATE_PATTERNS = listOf(
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEEE, dd-MMM-yy HH:mm:ss zzz",
        "EEE MMM d HH:mm:ss yyyy",
    )

    fun isTerminalFailure(statusCode: Int, body: String?): Boolean {
        if (statusCode == 401 || statusCode == 403) return true
        val code = errorCode(body) ?: return false
        return code in TERMINAL_ERROR_CODES
    }

    fun errorCode(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            JSONObject(body).optString("error_code", "").trim().lowercase().ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Delay in milliseconds with no upper cap.
     * Accepts delta-seconds and an HTTP-date. Returns null when the header is absent or unparseable.
     */
    fun retryAfterMillis(headerValue: String?, nowEpochMs: Long): Long? {
        val raw = headerValue?.trim()?.trim('"').orEmpty()
        if (raw.isEmpty()) return null
        raw.toLongOrNull()?.let { return secondsToMillis(it.toDouble()) }
        raw.toDoubleOrNull()?.let { return secondsToMillis(it) }
        val dateMs = parseHttpDate(raw) ?: return null
        return (dateMs - nowEpochMs).coerceAtLeast(0L)
    }

    private fun secondsToMillis(seconds: Double): Long {
        if (seconds.isNaN() || seconds.isInfinite() || seconds <= 0.0) return 0L
        val millis = seconds * 1000.0
        if (millis >= Long.MAX_VALUE) return Long.MAX_VALUE
        return millis.toLong()
    }

    private fun parseHttpDate(value: String): Long? {
        for (pattern in HTTP_DATE_PATTERNS) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                format.isLenient = false
                format.timeZone = TimeZone.getTimeZone("GMT")
                return format.parse(value)?.time
            } catch (_: Exception) {
                // try the next HTTP-date variant
            }
        }
        return null
    }
}

/** Process-wide gate so a later call cannot go out before Retry-After has elapsed. */
object RepeaterBookRateLimit {

    @Volatile
    private var notBeforeEpochMs: Long = 0L

    fun remainingMs(nowEpochMs: Long): Long =
        (notBeforeEpochMs - nowEpochMs).coerceAtLeast(0L)

    fun blockUntil(epochMs: Long) {
        synchronized(this) {
            if (epochMs > notBeforeEpochMs) notBeforeEpochMs = epochMs
        }
    }

    internal fun clearForTests() {
        notBeforeEpochMs = 0L
    }
}
