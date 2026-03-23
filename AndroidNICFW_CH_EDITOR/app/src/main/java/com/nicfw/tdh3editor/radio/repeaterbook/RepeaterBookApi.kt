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
 * Query parameters for [export.php] (North America) or [exportROW.php] (rest of world).
 * See https://www.repeaterbook.com/wiki/doku.php?id=api
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
) : IOException(message)

/**
 * HTTP client for RepeaterBook JSON export. Sets User-Agent and Bearer token from [BuildConfig].
 * If RepeaterBook documents a different auth scheme, adjust [authInterceptor] only.
 */
object RepeaterBookHttp {

    private const val BASE = "https://www.repeaterbook.com/api"

    fun userAgent(): String {
        val email = BuildConfig.REPEATERBOOK_CONTACT_EMAIL.ifBlank { "configure@local.properties" }
        val url = BuildConfig.REPEATERBOOK_APP_URL.trim()
        val ver = BuildConfig.VERSION_NAME
        return if (url.isNotEmpty()) {
            "NicFwChannelEditor/$ver (+$url; $email)"
        } else {
            "NicFwChannelEditor/$ver ($email)"
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .header("User-Agent", userAgent())
            .apply {
                val token = BuildConfig.REPEATERBOOK_APP_TOKEN.trim()
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                }
            }
            .build()
        chain.proceed(req)
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .build()

    /**
     * Executes GET export with [query]. Retries on HTTP 429 with simple backoff.
     */
    @Throws(IOException::class)
    fun fetchRepeaters(query: RepeaterBookQuery): String {
        val path = if (query.northAmerica) "$BASE/export.php" else "$BASE/exportROW.php"
        val url = path.toHttpUrl().newBuilder().apply {
            fun addIfNonEmpty(name: String, value: String) {
                val v = value.trim()
                if (v.isNotEmpty()) addQueryParameter(name, v)
            }
            addIfNonEmpty("country", query.country)
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
            addIfNonEmpty("emcomm", query.emcomm)
            addIfNonEmpty("stype", query.stype)
        }.build()

        val request = Request.Builder().url(url).get().build()
        var attempt = 0
        while (attempt < 4) {
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    when {
                        response.code == 429 -> {
                            val waitSec = (response.header("Retry-After")?.toLongOrNull()
                                ?: (2L shl attempt)).coerceIn(1L, 60L)
                            Thread.sleep(TimeUnit.SECONDS.toMillis(waitSec))
                            attempt++
                        }
                        !response.isSuccessful ->
                            throw RepeaterBookApiException(
                                response.code,
                                "HTTP ${response.code}: ${response.message}",
                                body,
                            )
                        else -> return body
                    }
                }
            } catch (e: RepeaterBookApiException) {
                throw e
            } catch (e: IOException) {
                attempt++
                if (attempt >= 4) throw e
                Thread.sleep(min(2000L * attempt, 8000L))
            }
        }
        throw IOException("RepeaterBook: too many retries")
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
