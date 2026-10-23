package com.nicfw.tdh3editor.radio.repeaterbook

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores one personal RepeaterBook token in encrypted preferences.
 * There is no plaintext fallback. A shared app token is never written.
 */
object RepeaterBookCredentialStore {

    const val PREF_FILE = "repeaterbook_user_token"
    private const val KEY = "user_app_token"

    @Volatile
    private var appContext: Context? = null

    fun install(context: Context) {
        appContext = context.applicationContext
    }

    fun hasToken(): Boolean = currentToken().isNotEmpty()

    fun currentToken(): String {
        val context = appContext ?: return ""
        val stored = try {
            prefs(context).getString(KEY, "").orEmpty()
        } catch (_: Exception) {
            return ""
        }
        return when (val decision = RepeaterBookTokenPolicy.decide(stored)) {
            is RepeaterBookTokenPolicy.Decision.Accepted -> decision.token
            else -> ""
        }
    }

    fun save(context: Context, raw: String): RepeaterBookTokenPolicy.Decision {
        val decision = RepeaterBookTokenPolicy.decide(raw)
        if (decision !is RepeaterBookTokenPolicy.Decision.Accepted) return decision
        val app = context.applicationContext
        install(app)
        return try {
            prefs(app).edit().putString(KEY, decision.token).commit()
            decision
        } catch (_: Exception) {
            RepeaterBookTokenPolicy.Decision.Rejected(RepeaterBookTokenPolicy.Reason.STORAGE)
        }
    }

    fun clear(context: Context) {
        val app = context.applicationContext
        install(app)
        try {
            prefs(app).edit().remove(KEY).commit()
        } catch (_: Exception) {
            // The next read already fails closed when encrypted storage cannot be opened.
        }
    }

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}
