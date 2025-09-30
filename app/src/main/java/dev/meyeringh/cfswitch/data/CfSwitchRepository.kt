package dev.meyeringh.cfswitch.data

import android.content.SharedPreferences
import dev.meyeringh.cfswitch.network.CfSwitchApi
import dev.meyeringh.cfswitch.network.ToggleBody
import retrofit2.HttpException
import java.io.IOException

sealed class NetworkError : Exception() {
    data object Unauthorized : NetworkError()
    data object NetworkFailure : NetworkError()
    data class Unknown(override val message: String?) : NetworkError()
}

class CfSwitchRepository(private val api: CfSwitchApi, private val preferences: SharedPreferences) {
    suspend fun getState(): Result<Boolean> {
        if (!hasValidConfig()) {
            return Result.failure(NetworkError.Unknown("Configuration not set"))
        }

        return try {
            val rule = api.getRule()
            Result.success(rule.enabled)
        } catch (e: HttpException) {
            when (e.code()) {
                401, 403 -> Result.failure(NetworkError.Unauthorized)
                else -> Result.failure(NetworkError.Unknown(e.message()))
            }
        } catch (e: IOException) {
            Result.failure(NetworkError.NetworkFailure)
        } catch (e: Exception) {
            Result.failure(NetworkError.Unknown(e.message))
        }
    }

    suspend fun toggle(newState: Boolean): Result<Unit> {
        if (!hasValidConfig()) {
            return Result.failure(NetworkError.Unknown("Configuration not set"))
        }

        return try {
            val response = api.setEnabled(ToggleBody(newState))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                when (response.code()) {
                    401, 403 -> Result.failure(NetworkError.Unauthorized)
                    else -> Result.failure(NetworkError.Unknown("HTTP ${response.code()}"))
                }
            }
        } catch (e: HttpException) {
            when (e.code()) {
                401, 403 -> Result.failure(NetworkError.Unauthorized)
                else -> Result.failure(NetworkError.Unknown(e.message()))
            }
        } catch (e: IOException) {
            Result.failure(NetworkError.NetworkFailure)
        } catch (e: Exception) {
            Result.failure(NetworkError.Unknown(e.message))
        }
    }

    suspend fun refresh(): Result<Boolean> = getState()

    private fun hasValidConfig(): Boolean {
        val baseUrl = preferences.getString("base_url", "") ?: ""
        val token = preferences.getString("api_token", "") ?: ""
        return baseUrl.isNotEmpty() && token.isNotEmpty()
    }
}
