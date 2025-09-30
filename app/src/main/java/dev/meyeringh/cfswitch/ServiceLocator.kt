package dev.meyeringh.cfswitch

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.meyeringh.cfswitch.data.CfSwitchRepository
import dev.meyeringh.cfswitch.network.CfSwitchApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ServiceLocator {
    private var repository: CfSwitchRepository? = null
    private var preferences: SharedPreferences? = null

    fun provideRepository(context: Context): CfSwitchRepository = repository ?: synchronized(this) {
        repository ?: CfSwitchRepository(
            api = provideApi(context),
            preferences = providePreferences(context)
        ).also { repository = it }
    }

    fun providePreferences(context: Context): SharedPreferences = preferences ?: synchronized(this) {
        preferences ?: createEncryptedPreferences(context).also { preferences = it }
    }

    private fun createEncryptedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "cf_switch_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun provideApi(context: Context): CfSwitchApi {
        val prefs = providePreferences(context)

        val authInterceptor = Interceptor { chain ->
            val token = prefs.getString("api_token", "") ?: ""
            val request = if (token.isNotEmpty()) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            clientBuilder.addInterceptor(loggingInterceptor)
        }

        val client = clientBuilder.build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val baseUrl = prefs.getString("base_url", "https://example.com/") ?: "https://example.com/"
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(CfSwitchApi::class.java)
    }

    // For testing - allows replacing repository
    fun setRepository(repo: CfSwitchRepository) {
        repository = repo
    }

    // Reset repository to force recreation with new settings
    fun resetRepository() {
        repository = null
    }

    fun resetForTesting() {
        repository = null
        preferences = null
    }
}
