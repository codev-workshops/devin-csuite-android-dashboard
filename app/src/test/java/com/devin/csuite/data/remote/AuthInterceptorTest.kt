package com.devin.csuite.data.remote

import com.devin.csuite.data.local.SecureKeyStore
import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var secureKeyStore: SecureKeyStore

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        secureKeyStore = mockk()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `interceptor adds Authorization header when key exists`() {
        every { secureKeyStore.getApiKey() } returns "cog_test_key_123"

        val interceptor = AuthInterceptor(secureKeyStore)
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()
        client.newCall(request).execute()

        val recorded = mockWebServer.takeRequest()
        assertEquals("Bearer cog_test_key_123", recorded.getHeader("Authorization"))
    }

    @Test
    fun `interceptor adds Content-Type header`() {
        every { secureKeyStore.getApiKey() } returns "cog_test_key_123"

        val interceptor = AuthInterceptor(secureKeyStore)
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()
        client.newCall(request).execute()

        val recorded = mockWebServer.takeRequest()
        assertEquals("application/json", recorded.getHeader("Content-Type"))
    }

    @Test
    fun `interceptor does not add Authorization when no key`() {
        every { secureKeyStore.getApiKey() } returns null

        val interceptor = AuthInterceptor(secureKeyStore)
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()
        client.newCall(request).execute()

        val recorded = mockWebServer.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun `interceptor preserves original request URL`() {
        every { secureKeyStore.getApiKey() } returns "cog_test"

        val interceptor = AuthInterceptor(secureKeyStore)
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/v3/enterprise/organizations"))
            .build()
        client.newCall(request).execute()

        val recorded = mockWebServer.takeRequest()
        assertEquals("/v3/enterprise/organizations", recorded.path)
    }

    @Test
    fun `interceptor uses Bearer token format`() {
        every { secureKeyStore.getApiKey() } returns "cog_my_enterprise_key"

        val interceptor = AuthInterceptor(secureKeyStore)
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()
        client.newCall(request).execute()

        val recorded = mockWebServer.takeRequest()
        val auth = recorded.getHeader("Authorization")!!
        assertTrue(auth.startsWith("Bearer "))
    }
}
