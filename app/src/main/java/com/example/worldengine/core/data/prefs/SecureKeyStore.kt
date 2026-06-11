package com.example.worldengine.core.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the user's NovelAI API token in EncryptedSharedPreferences so the secret is encrypted
 * at rest and never lives in source control. Keys are entered by the user in Settings.
 */
class SecureKeyStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getApiKey(): String? = prefs.getString(KEY_NOVELAI, null)

    fun setApiKey(value: String) {
        prefs.edit().putString(KEY_NOVELAI, value.trim()).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_NOVELAI).apply()
    }

    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

    companion object {
        private const val PREFS_NAME = "world_engine_secure_prefs"
        private const val KEY_NOVELAI = "novelai_api_key"
    }
}
