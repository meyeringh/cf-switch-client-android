package dev.meyeringh.cfswitch

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.meyeringh.cfswitch.data.CfSwitchRepository
import dev.meyeringh.cfswitch.data.NetworkError
import dev.meyeringh.cfswitch.network.CfSwitchApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class CfSwitchRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: CfSwitchApi
    private lateinit var preferences: SharedPreferences
    private lateinit var repository: CfSwitchRepository

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CfSwitchApi::class.java)

        preferences = mock(SharedPreferences::class.java)
        `when`(preferences.getString("base_url", "")).thenReturn("http://test/")
        `when`(preferences.getString("api_token", "")).thenReturn("test-token")

        repository = CfSwitchRepository(api, preferences)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getState returns enabled true when API returns enabled true`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"enabled": true}""")
        )

        val result = repository.getState()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(true)
    }

    @Test
    fun `getState returns enabled false when API returns enabled false`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"enabled": false}""")
        )

        val result = repository.getState()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(false)
    }

    @Test
    fun `getState returns Unauthorized error on 401`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": "Unauthorized"}""")
        )

        val result = repository.getState()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NetworkError.Unauthorized::class.java)
    }

    @Test
    fun `getState returns Unauthorized error on 403`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("""{"error": "Forbidden"}""")
        )

        val result = repository.getState()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NetworkError.Unauthorized::class.java)
    }

    @Test
    fun `toggle succeeds with 200 response`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
        )

        val result = repository.toggle(true)

        assertThat(result.isSuccess).isTrue()

        val request = mockWebServer.takeRequest()
        assertThat(request.path).isEqualTo("/v1/rule/enable")
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.body.readUtf8()).contains("\"enabled\":true")
    }

    @Test
    fun `toggle returns Unauthorized error on 401`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
        )

        val result = repository.toggle(false)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NetworkError.Unauthorized::class.java)
    }

    @Test
    fun `refresh calls getState and returns result`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"enabled": true}""")
        )

        val result = repository.refresh()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(true)
    }

    @Test
    fun `getState fails when config is missing`() = runTest {
        `when`(preferences.getString("base_url", "")).thenReturn("")
        `when`(preferences.getString("api_token", "")).thenReturn("")

        val result = repository.getState()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NetworkError.Unknown::class.java)
    }
}
