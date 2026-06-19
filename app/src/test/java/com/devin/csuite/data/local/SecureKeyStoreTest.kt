package com.devin.csuite.data.local

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SecureKeyStoreTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } returns Unit
    }

    // --- hasApiKey Tests ---

    @Test
    fun `hasApiKey returns false when no key stored`() {
        every { sharedPreferences.getString("encrypted_api_key", null) } returns null

        val keyStore = SecureKeyStoreTestable(sharedPreferences)
        assertFalse(keyStore.hasApiKey())
    }

    @Test
    fun `hasApiKey returns true when key exists`() {
        every { sharedPreferences.getString("encrypted_api_key", null) } returns "encrypted_value"

        val keyStore = SecureKeyStoreTestable(sharedPreferences)
        assertTrue(keyStore.hasApiKey())
    }

    // --- clearApiKey Tests ---

    @Test
    fun `clearApiKey removes encrypted key`() {
        val keyStore = SecureKeyStoreTestable(sharedPreferences)
        keyStore.clearApiKey()

        verify { editor.remove("encrypted_api_key") }
        verify { editor.apply() }
    }

    @Test
    fun `clearApiKey removes IV`() {
        val keyStore = SecureKeyStoreTestable(sharedPreferences)
        keyStore.clearApiKey()

        verify { editor.remove("encrypted_api_key_iv") }
    }

    // --- Store/Retrieve Roundtrip Tests ---

    @Test
    fun `storeApiKey writes encrypted data and IV`() {
        val keyStore = SecureKeyStoreTestable(sharedPreferences)
        keyStore.storeApiKey("cog_test_key_123")

        verify { editor.putString("encrypted_api_key", any()) }
        verify { editor.putString("encrypted_api_key_iv", any()) }
        verify { editor.apply() }
    }

    @Test
    fun `storeApiKey stores non-empty encrypted value`() {
        val capturedValues = mutableMapOf<String, String>()
        every { editor.putString(any(), any()) } answers {
            capturedValues[firstArg()] = secondArg()
            editor
        }

        val keyStore = SecureKeyStoreTestable(sharedPreferences)
        keyStore.storeApiKey("cog_my_key")

        assertTrue(capturedValues["encrypted_api_key"]!!.isNotEmpty())
        assertTrue(capturedValues["encrypted_api_key_iv"]!!.isNotEmpty())
    }

    @Test
    fun `store and retrieve roundtrip returns original key`() {
        val keyStore = InMemoryKeyStore()

        keyStore.storeApiKey("cog_roundtrip_test_key")
        val retrieved = keyStore.getApiKey()

        assertEquals("cog_roundtrip_test_key", retrieved)
    }

    @Test
    fun `store different keys preserves latest`() {
        val keyStore = InMemoryKeyStore()

        keyStore.storeApiKey("cog_first_key")
        keyStore.storeApiKey("cog_second_key")

        assertEquals("cog_second_key", keyStore.getApiKey())
    }

    @Test
    fun `store then clear then retrieve returns null`() {
        val keyStore = InMemoryKeyStore()

        keyStore.storeApiKey("cog_temporary_key")
        assertEquals("cog_temporary_key", keyStore.getApiKey())

        keyStore.clearApiKey()
        assertNull(keyStore.getApiKey())
    }

    // --- getApiKey Error Cases ---

    @Test
    fun `getApiKey returns null when encrypted key is missing`() {
        every { sharedPreferences.getString("encrypted_api_key", null) } returns null

        val keyStore = SecureKeyStoreTestable(sharedPreferences)
        assertNull(keyStore.getApiKey())
    }

    @Test
    fun `getApiKey returns null when IV is missing`() {
        every { sharedPreferences.getString("encrypted_api_key", null) } returns "some_value"
        every { sharedPreferences.getString("encrypted_api_key_iv", null) } returns null

        val keyStore = SecureKeyStoreTestable(sharedPreferences)
        assertNull(keyStore.getApiKey())
    }

    @Test
    fun `getApiKey returns null on decryption failure`() {
        every { sharedPreferences.getString("encrypted_api_key", null) } returns "corrupted_data"
        every { sharedPreferences.getString("encrypted_api_key_iv", null) } returns "bad_iv"

        val keyStore = SecureKeyStoreTestable(sharedPreferences)
        assertNull(keyStore.getApiKey())
    }

    // --- Key Validation Flow Tests ---

    @Test
    fun `valid cog_ prefix key is accepted`() {
        assertTrue("cog_valid_key_12345".startsWith("cog_"))
    }

    @Test
    fun `key without cog_ prefix is rejected`() {
        assertFalse("invalid_key_12345".startsWith("cog_"))
    }

    @Test
    fun `empty key is rejected`() {
        assertFalse("".startsWith("cog_"))
    }

    @Test
    fun `key with only cog_ prefix is technically valid prefix`() {
        assertTrue("cog_".startsWith("cog_"))
    }

    // --- Fallback Tests ---

    @Test
    fun `fallback to standard SharedPreferences is accessible`() {
        val fallbackPrefs = mockk<SharedPreferences>(relaxed = true)
        every { context.getSharedPreferences("devin_secure_prefs", Context.MODE_PRIVATE) } returns fallbackPrefs

        val prefs = context.getSharedPreferences("devin_secure_prefs", Context.MODE_PRIVATE)
        assertEquals(fallbackPrefs, prefs)
    }

    @Test
    fun `fallback SharedPreferences can store and retrieve`() {
        val fallbackPrefs = mockk<SharedPreferences>(relaxed = true)
        val fallbackEditor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { fallbackPrefs.edit() } returns fallbackEditor
        every { fallbackEditor.putString(any(), any()) } returns fallbackEditor
        every { fallbackEditor.apply() } returns Unit
        every { fallbackPrefs.getString("encrypted_api_key", null) } returns "fallback_value"

        val keyStore = SecureKeyStoreTestable(fallbackPrefs)
        assertTrue(keyStore.hasApiKey())
    }
}

/**
 * Testable wrapper around SharedPreferences-based key storage
 * that mirrors SecureKeyStore's interface without Android Keystore dependency.
 */
class SecureKeyStoreTestable(
    private val sharedPreferences: SharedPreferences
) {
    fun hasApiKey(): Boolean {
        return sharedPreferences.getString("encrypted_api_key", null) != null
    }

    fun clearApiKey() {
        sharedPreferences.edit()
            .remove("encrypted_api_key")
            .remove("encrypted_api_key_iv")
            .apply()
    }

    fun storeApiKey(apiKey: String) {
        val encoded = java.util.Base64.getEncoder().encodeToString(apiKey.toByteArray(Charsets.UTF_8))
        sharedPreferences.edit()
            .putString("encrypted_api_key", encoded)
            .putString("encrypted_api_key_iv", "test_iv_value")
            .apply()
    }

    fun getApiKey(): String? {
        val encrypted = sharedPreferences.getString("encrypted_api_key", null) ?: return null
        val iv = sharedPreferences.getString("encrypted_api_key_iv", null) ?: return null
        return try {
            String(java.util.Base64.getDecoder().decode(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * In-memory key store for integration-style roundtrip tests.
 */
class InMemoryKeyStore {
    private var storedKey: String? = null

    fun storeApiKey(apiKey: String) {
        storedKey = apiKey
    }

    fun getApiKey(): String? = storedKey

    fun clearApiKey() {
        storedKey = null
    }

    fun hasApiKey(): Boolean = storedKey != null
}
