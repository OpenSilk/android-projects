package org.opensilk.traveltime.api

import android.util.Base64
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * The Backend API is how we communicate with our backend
 *
 * Created by drew on 10/29/17.
 */
interface BackendApi {
    /**
     *
     */
    @POST("register")
    fun register(@Body req: RegisterReq): Single<RegisterResp>

    /**
     * Backend will generate a channelId and provide the subscription url
     */
    @POST("channel/new")
    @Headers("Content-Type: application/json")
    fun channelNew(@Header("Authorization") credential: BasicCredential): Single<ChannelNewResp>
}

data class RegisterResp(
        val userName: String,
        val apiKey: String
)

data class RegisterReq(
        val firebaseToken: String
)

/**
 * Response from backend with info needed to create the event watch request.
 *
 * Created by drew on 10/30/17.
 */
data class ChannelNewResp (
        val channelId: String,
        val address: String,
        val token: String,
        val expiration: Long
)

data class BasicCredential(
        val user: String,
        val pass: String
) {
    private fun encode(): String {
        return Base64.encodeToString("$user:$pass".toByteArray(), Base64.DEFAULT)
    }
    override fun toString(): String {
        return "Basic ${encode()}"
    }
}
