package com.nicfw.tdh3editor.radio.repeaterbook

/**
 * Credential rules for a distributed RepeaterBook client.
 *
 * Personal app-bound tokens (`rbuapp_…`) may be sent only in the approved header.
 * Shared `app_…` tokens are refused. Tokens are never placed in a URL.
 */
object RepeaterBookTokenPolicy {

    const val HEADER_NAME = "X-RB-App-Token"

    private val USER_TOKEN = Regex("^rbuapp_[A-Za-z0-9_\\-]{8,}$")
    private val CREDENTIAL_QUERY_KEYS = setOf("key", "token", "api_key", "apikey", "api-key")
    private val TOKEN_SHAPE = Regex("(?i)(?<![A-Za-z0-9])(?:rbuapp_|app_)[A-Za-z0-9_\\-]{8,}")

    sealed class Decision {
        data class Accepted(val token: String) : Decision()
        data class Rejected(val reason: Reason) : Decision()
    }

    enum class Reason {
        BLANK,
        SHARED_APP_TOKEN,
        URL_OR_QUERY,
        MALFORMED,
        STORAGE,
    }

    fun decide(raw: String): Decision {
        var trimmed = raw.trim()
        if (trimmed.startsWith(HEADER_NAME)) {
            trimmed = trimmed.removePrefix(HEADER_NAME).trim().removePrefix(":").trim()
        }
        if (trimmed.isEmpty()) return Decision.Rejected(Reason.BLANK)
        if (trimmed.any { it.isWhitespace() } || trimmed.contains("://") ||
            trimmed.contains('?') || trimmed.contains('&') || trimmed.contains('=')
        ) {
            return Decision.Rejected(Reason.URL_OR_QUERY)
        }
        if (trimmed.lowercase().startsWith("app_")) {
            return Decision.Rejected(Reason.SHARED_APP_TOKEN)
        }
        if (!USER_TOKEN.matches(trimmed)) return Decision.Rejected(Reason.MALFORMED)
        return Decision.Accepted(trimmed)
    }

    /** Header value for an export API URL, or null when the token must not be sent. */
    fun headerValueForApiRequest(url: String, token: String): String? {
        if (!isExportApiUrl(url)) return null
        return (decide(token) as? Decision.Accepted)?.token
    }

    fun isExportApiUrl(url: String): Boolean {
        val normalized = url.trim().lowercase()
        return normalized.startsWith("https://www.repeaterbook.com/api/") ||
            normalized.startsWith("https://repeaterbook.com/api/")
    }

    /**
     * Drops credential query parameters. Search parameters such as `country` and `state_id` stay.
     */
    fun stripCredentialQuery(url: String): String {
        val hash = url.indexOf('#')
        val withoutFragment = if (hash >= 0) url.substring(0, hash) else url
        val fragment = if (hash >= 0) url.substring(hash) else ""
        val queryAt = withoutFragment.indexOf('?')
        if (queryAt < 0) return url
        val base = withoutFragment.substring(0, queryAt)
        val kept = withoutFragment.substring(queryAt + 1)
            .split('&')
            .filter { part ->
                val name = part.substringBefore('=').lowercase()
                name !in CREDENTIAL_QUERY_KEYS
            }
        return if (kept.isEmpty()) base + fragment else base + "?" + kept.joinToString("&") + fragment
    }

    fun redact(text: String): String = TOKEN_SHAPE.replace(text) { match ->
        val value = match.value
        val prefix = if (value.lowercase().startsWith("rbuapp_")) "rbuapp_" else "app_"
        prefix + "[redacted]"
    }
}
