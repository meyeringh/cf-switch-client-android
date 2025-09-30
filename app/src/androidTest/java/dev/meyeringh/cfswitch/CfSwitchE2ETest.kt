package dev.meyeringh.cfswitch

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.meyeringh.cfswitch.data.CfSwitchRepository
import dev.meyeringh.cfswitch.network.CfSwitchApi
import dev.meyeringh.cfswitch.ui.CfSwitchScreen
import dev.meyeringh.cfswitch.ui.theme.CfSwitchTheme
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@RunWith(AndroidJUnit4::class)
class CfSwitchE2ETest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockWebServer: MockWebServer
    private lateinit var context: Context
    private lateinit var viewModel: CfSwitchViewModel
    private lateinit var preferences: android.content.SharedPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Get preferences
        preferences = ServiceLocator.providePreferences(context)
        preferences.edit()
            .clear()
            .putString("base_url", mockWebServer.url("/").toString())
            .putString("api_token", "test-token")
            .apply()

        // Create test API and repository
        val authInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Authorization", "Bearer test-token")
                .build()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CfSwitchApi::class.java)

        val repository = CfSwitchRepository(api, preferences)
        viewModel = CfSwitchViewModel(repository)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun initialLoad_showsStateFromApi() {
        // Enqueue response for initial load
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"enabled": true}""")
        )

        // Set content and trigger load
        composeTestRule.setContent {
            CfSwitchTheme {
                CfSwitchScreen(
                    viewModel = viewModel,
                    preferences = preferences
                )
            }
        }

        // Trigger initial load
        viewModel.loadState()

        // Wait for UI to update
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onNodeWithText("Enabled", useUnmergedTree = true)
                    .assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Verify state is shown
        composeTestRule.onNodeWithText("Enabled", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Disable", useUnmergedTree = true).assertIsDisplayed()

        // Verify request was made to correct endpoint
        val request = mockWebServer.takeRequest()
        assert(request.path == "/v1/rule")
        assert(request.method == "GET")
    }

    @Test
    fun tapToggleButton_postsCorrectJson() {
        // Enqueue response for initial load (enabled = true)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"enabled": true}""")
        )

        // Set content
        composeTestRule.setContent {
            CfSwitchTheme {
                CfSwitchScreen(
                    viewModel = viewModel,
                    preferences = preferences
                )
            }
        }

        // Load initial state
        viewModel.loadState()

        // Wait for initial load
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onNodeWithText("Enabled", useUnmergedTree = true)
                    .assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Consume initial GET request
        mockWebServer.takeRequest()

        // Enqueue response for toggle
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
        )

        // Tap the toggle button
        composeTestRule.onNodeWithText("Disable", useUnmergedTree = true).performClick()

        // Wait for optimistic UI update
        composeTestRule.waitForIdle()

        // Verify UI shows disabled state (optimistic update)
        composeTestRule.onNodeWithText("Disabled", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Enable", useUnmergedTree = true).assertIsDisplayed()

        // Verify POST request was sent with correct body
        val toggleRequest = mockWebServer.takeRequest()
        assert(toggleRequest.path == "/v1/rule/enable")
        assert(toggleRequest.method == "POST")
        val body = toggleRequest.body.readUtf8()
        assert(body.contains("\"enabled\":false") || body.contains("\"enabled\": false"))
    }

    @Test
    fun refresh_refetchesState() {
        // Enqueue response for initial load (disabled)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"enabled": false}""")
        )

        // Set content
        composeTestRule.setContent {
            CfSwitchTheme {
                CfSwitchScreen(
                    viewModel = viewModel,
                    preferences = preferences
                )
            }
        }

        // Load initial state
        viewModel.loadState()

        // Wait for initial load
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onNodeWithText("Disabled", useUnmergedTree = true)
                    .assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Consume initial GET request
        mockWebServer.takeRequest()

        // Enqueue response for refresh (now enabled)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"enabled": true}""")
        )

        // Trigger refresh programmatically
        viewModel.refresh()

        // Wait for refresh to complete
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onNodeWithText("Enabled", useUnmergedTree = true)
                    .assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Verify updated state
        composeTestRule.onNodeWithText("Enabled", useUnmergedTree = true).assertIsDisplayed()

        // Verify refresh request was made
        val refreshRequest = mockWebServer.takeRequest()
        assert(refreshRequest.path == "/v1/rule")
        assert(refreshRequest.method == "GET")
    }
}
