package com.baumann.jarvis.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Guarda a chave da API criptografada (Android Keystore + EncryptedSharedPreferences).
 * A chave NUNCA fica no código-fonte: o usuário a informa dentro do app.
 */
class SecureKeyStore(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "jarvis_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getApiKey(): String? =
        prefs.getString(API_KEY, null)?.takeIf { it.isNotBlank() }

    fun saveApiKey(key: String) {
        prefs.edit().putString(API_KEY, key.trim()).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove(API_KEY).apply()
    }

    private companion object {
        const val API_KEY = "anthropic_api_key"
    }
}
