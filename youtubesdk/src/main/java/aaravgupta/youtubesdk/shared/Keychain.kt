package aaravgupta.youtubesdk.shared

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.concurrent.ConcurrentHashMap

/**
 * Android-backed Keychain equivalent used by OAuth helpers.
 */
object Keychain {
    private const val SERVICE_NAME: String = "aaravgupta.youtubesdk.security"
    private const val PREFS_NAME: String = "aaravgupta.youtubesdk.security.prefs"

    @Volatile
    private var appContext: Context? = null

    private val inMemoryValues: MutableMap<String, String> = ConcurrentHashMap()

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun save(value: String, key: String) {
        encryptedPrefsOrNull()?.edit()?.putString(namespacedKey(key), value)?.apply()
        inMemoryValues[key] = value
    }

    fun load(key: String): String? {
        val encrypted = encryptedPrefsOrNull()?.getString(namespacedKey(key), null)
        if (!encrypted.isNullOrBlank()) {
            inMemoryValues[key] = encrypted
            return encrypted
        }
        return inMemoryValues[key]
    }

    fun delete(key: String) {
        encryptedPrefsOrNull()?.edit()?.remove(namespacedKey(key))?.apply()
        inMemoryValues.remove(key)
    }

    private fun namespacedKey(key: String): String = "$SERVICE_NAME.$key"

    private fun encryptedPrefsOrNull(): SharedPreferences? {
        val context = appContext ?: return null
        return runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrNull()
    }
}