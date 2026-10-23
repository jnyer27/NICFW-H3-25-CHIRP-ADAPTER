package com.nicfw.tdh3editor.radio.repeaterbook

import com.nicfw.tdh3editor.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Query parameters for RepeaterBook JSON export endpoints ([wiki](https://www.repeaterbook.com/wiki/doku.php?id=api)):
 *
 * **export.php** (North America): `callsign`, `city`, `landmark`, `state_id`, `country`, `county`,
 * `frequency`, `mode`, `emcomm`, `stype`.
 *
 * **exportROW.php**: `callsign`, `city`, `landmark`, `country`, `region`, `frequency`, `mode` only
 * (no `state_id`, `county`, `emcomm`, or `stype` in the published API).
 */
data class RepeaterBookQuery(
    val northAmerica: Boolean = true,
    val country: String = "",
    val stateId: String = "",
    val region: String = "",
    val county: String = "",
    val city: String = "",
    val landmark: String = "",
    val callsign: String = "",
    val frequency: String = "",
    /** e.g. analog, DMR — empty = omit */
    val mode: String = "",
    val emcomm: String = "",
    /** e.g. gmrs — empty = omit */
    val stype: String = "",
)

class RepeaterBookApiException(
    val statusCode: Int,
    message: String,
    val errorBody: String? = null,
    /** Authentication, scope, User-Agent, or an unexpired Retry-After. Callers must not try again immediately. */
    val terminal: Boolean = false,
) : IOException(message)

/**
 * HTTP client for RepeaterBook JSON export.
 *
 * The personal token comes from [RepeaterBookCredentialStore] and is sent only as
 * [RepeaterBookTokenPolicy.HEADER_NAME] on `/api/` requests. Query-string authentication is not supported.
 */
object RepeaterBookHttp {

    private const val BASE = "https://www.repeaterbook.com/api"
    private const val PUBLIC_PROJECT_URL = "https://github.com/jnyer27/NICFW-H3-25-CHIRP-ADAPTER"

    fun userAgent(): String {
        val override = BuildConfig.REPEATERBOOK_USER_AGENT.trim()
        if (override.isNotEmpty()) return override
        val email = BuildConfig.REPEATERBOOK_CONTACT_EMAIL.trim()
        val url = BuildConfig.REPEATERBOOK_APP_URL.trim().ifEmpty { PUBLIC_PROJECT_URL }
        val ver = BuildConfig.VERSION_NAME
        return if (email.isNotEmpty()) {
            "NicFwChannelEditor/$ver (+$url; $email)"
        } else {
            "NicFwChannelEditor/$ver (+$url)"
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val sanitized = RepeaterBookTokenPolicy.stripCredentialQuery(original.url.toString())
        val header = RepeaterBookTokenPolicy.headerValueForApiRequest(
            sanitized,
            RepeaterBookCredentialStore.currentToken(),
        )
        val builder = original.newBuilder()
            .url(sanitized)
            .header("User-Agent", userAgent())
        if (header != null) {
            builder.header(RepeaterBookTokenPolicy.HEADER_NAME, header)
        }
        chain.proceed(builder.build())
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .build()

    /** Same stack (User-Agent + RepeaterBook token) for API or related HTTP fetches. */
    fun httpClient(): OkHttpClient = client

    /**
     * Executes GET export with [query].
     * A 429 waits out the full Retry-After before one inline retry. Authentication, scope,
     * and User-Agent failures are not retried.
     */
    @Throws(IOException::class)
    fun fetchRepeaters(query: RepeaterBookQuery): String {
        val path = if (query.northAmerica) "$BASE/export.php" else "$BASE/exportROW.php"
        val url = path.toHttpUrl().newBuilder().apply {
            fun addIfNonEmpty(name: String, value: String) {
                val v = value.trim()
                if (v.isNotEmpty()) addQueryParameter(name, v)
            }
            /** Wiki sample: `country=United States&country=Canada` — repeat the parameter per value. */
            fun addCountryParams(commaSeparated: String) {
                for (part in commaSeparated.split(',', ';')) {
                    val v = part.trim()
                    if (v.isNotEmpty()) addQueryParameter("country", v)
                }
            }
            addCountryParams(query.country)
            if (query.northAmerica) {
                addIfNonEmpty("state_id", query.stateId)
                addIfNonEmpty("county", query.county)
            } else {
                addIfNonEmpty("region", query.region)
            }
            addIfNonEmpty("city", query.city)
            addIfNonEmpty("landmark", query.landmark)
            addIfNonEmpty("callsign", query.callsign)
            addIfNonEmpty("frequency", query.frequency)
            addIfNonEmpty("mode", query.mode)
            if (query.northAmerica) {
                addIfNonEmpty("emcomm", query.emcomm)
                addIfNonEmpty("stype", query.stype)
            }
        }.build()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()
        var networkRetries = 0
        var rateLimitRetries = 0
        var skipGate = false
        while (true) {
            if (!skipGate) {
                val remaining = RepeaterBookRateLimit.remainingMs(System.currentTimeMillis())
                if (remaining > 0L) {
                    throw RepeaterBookApiException(
                        429,
                        "RepeaterBook Retry-After is still in effect. Wait ${(remaining + 999) / 1000} seconds before another request.",
                        terminal = true,
                    )
                }
            }
            skipGate = false
            try {
                var successBody: String? = null
                var inlineDelayMs: Long? = null
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    when {
                        response.code == 429 -> {
                            val now = System.currentTimeMillis()
                            val delay = RepeaterBookRetryPolicy.retryAfterMillis(
                                response.header("Retry-After"),
                                now,
                            ) ?: RepeaterBookRetryPolicy.MISSING_HEADER_BACKOFF_MS
                            RepeaterBookRateLimit.blockUntil(now + delay)
                            if (rateLimitRetries < 1 && delay <= RepeaterBookRetryPolicy.INLINE_WAIT_MAX_MS) {
                                rateLimitRetries += 1
                                inlineDelayMs = delay
                            } else {
                                throw RepeaterBookApiException(
                                    429,
                                    "RepeaterBook rate limit. Wait the full Retry-After of ${(delay + 999) / 1000} seconds before another request.",
                                    body,
                                    terminal = true,
                                )
                            }
                        }
                        !response.isSuccessful ->
                            throw RepeaterBookApiException(
                                response.code,
                                "HTTP ${response.code}: ${response.message}",
                                body,
                                terminal = RepeaterBookRetryPolicy.isTerminalFailure(response.code, body),
                            )
                        else -> successBody = body
                    }
                }
                if (successBody != null) return successBody
                val delay = inlineDelayMs
                if (delay != null) {
                    Thread.sleep(delay)
                    skipGate = true
                }
            } catch (e: RepeaterBookApiException) {
                throw e
            } catch (e: IOException) {
                networkRetries += 1
                if (networkRetries >= 3) throw e
                Thread.sleep(min(1000L * networkRetries, 4000L))
            }
        }
    }
}

object RepeaterBookJsonParser {

    /**
     * Parses JSON body into raw repeater objects. Handles API error payloads.
     */
    fun parseResults(jsonText: String): List<JSONObject> {
        val root = JSONObject(jsonText)
        if (root.has("ok") && !root.optBoolean("ok", true)) {
            val msg = root.optString("message", root.optString("error_code", "API error"))
            throw IOException(msg)
        }
        val results = root.optJSONArray("results") ?: return emptyList()
        return buildList {
            for (i in 0 until results.length()) {
                val o = results.optJSONObject(i) ?: continue
                add(o)
            }
        }
    }
}
