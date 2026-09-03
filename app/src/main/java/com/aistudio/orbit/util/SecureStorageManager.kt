package com.aistudio.orbit.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecureStorageManager {
    private const val PREFS_FILE_NAME = "orbit_secure_prefs"
    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        if (sharedPreferences == null) {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            sharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    fun saveSecret(key: String, value: String) {
        sharedPreferences?.edit()?.putString(key, value)?.apply()
    }

    fun getSecret(key: String): String? {
        return sharedPreferences?.getString(key, null)
    }

    fun removeSecret(key: String) {
        sharedPreferences?.edit()?.remove(key)?.apply()
    }
    
    fun clearAllSecrets() {
        sharedPreferences?.edit()?.clear()?.apply()
    }
}
