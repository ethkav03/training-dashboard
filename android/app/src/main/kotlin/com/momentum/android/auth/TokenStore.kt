package com.momentum.android.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted on-device JWT storage -- deliberately more careful than the web
 * app's plain localStorage, since a bearer token on a lost/rooted phone is a
 * real risk in a way it isn't for a browser tab.
 */
class TokenStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "momentum_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, value)
            }.apply()
        }

    companion object {
        private const val KEY_TOKEN = "jwt"
    }
}
