package com.devin.csuite.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SecureKeyStore"
        private const val KEYSTORE_ALIAS = "devin_csuite_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val PREFS_FILE = "devin_secure_prefs"
        private const val KEY_API_KEY = "encrypted_api_key"
        private const val KEY_API_KEY_IV = "encrypted_api_key_iv"
        private const val GCM_TAG_LENGTH = 128
    }

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences failed, falling back to standard prefs", e)
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        keyStore.getEntry(KEYSTORE_ALIAS, null)?.let { entry ->
            return (entry as KeyStore.SecretKeyEntry).secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun storeApiKey(apiKey: String) {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val encryptedBytes = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv

        sharedPreferences.edit()
            .putString(KEY_API_KEY, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
            .putString(KEY_API_KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
    }

    fun getApiKey(): String? {
        val encryptedBase64 = sharedPreferences.getString(KEY_API_KEY, null) ?: return null
        val ivBase64 = sharedPreferences.getString(KEY_API_KEY_IV, null) ?: return null

        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt API key", e)
            null
        }
    }

    fun hasApiKey(): Boolean {
        return sharedPreferences.getString(KEY_API_KEY, null) != null
    }

    fun clearApiKey() {
        sharedPreferences.edit()
            .remove(KEY_API_KEY)
            .remove(KEY_API_KEY_IV)
            .apply()
    }
}
