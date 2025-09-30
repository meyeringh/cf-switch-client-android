package dev.meyeringh.cfswitch.network

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CfSwitchApi {
    @GET("v1/rule")
    suspend fun getRule(): RuleDto

    @POST("v1/rule/enable")
    suspend fun setEnabled(@Body body: ToggleBody): Response<Unit>
}

@JsonClass(generateAdapter = true)
data class RuleDto(val enabled: Boolean)

@JsonClass(generateAdapter = true)
data class ToggleBody(val enabled: Boolean)
