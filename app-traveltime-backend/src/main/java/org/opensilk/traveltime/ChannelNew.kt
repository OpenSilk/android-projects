package org.opensilk.traveltime

import dagger.Subcomponent
import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.auth.authentication
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.*
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.method
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import javax.inject.Inject
import javax.inject.Named

/**
 * Created by drew on 12/28/17.
 */
@Location("/channel/new")
class ChannelNew

fun Route.channelNew() {
    location<ChannelNew> {
        restricted()
        method(HttpMethod.Post) {
            handle<ChannelNew> {
                application.appComponent.channelNewBob().build().handler().handle(call)
            }
        }
    }
}

@Subcomponent
interface ChannelNewCmp {
    fun handler(): ChannelNewHandler

    @Subcomponent.Builder
    abstract class Builder {
        abstract fun build(): ChannelNewCmp
    }
}

/**
 * Handles creating new users
 */
class ChannelNewHandler @Inject constructor(
        @Named("base-url") private val baseUrl: String,
        private var userDAO: UserDAO
): CallHandler {

    override suspend fun handle(call: ApplicationCall) {
        val userInfo = call.authentication.principal<UserInfo>()!!
        //create new channel
        val channelInfo = userDAO.makeChannel()
        val newUserInfo = userInfo.copy(channels = userInfo.channels.toMutableList().apply { add(channelInfo.id) })
        userDAO.saveUserInfo(newUserInfo)

        //build response
        val encodedChannel = userDAO.encodeId(channelInfo.id)
        val channelResp = ChannelNewResp(
                channelId = encodedChannel,
                address = "$baseUrl/channel/notify/$encodedChannel",
                token = "uid=${userInfo.id}&cid=${channelInfo.id}", //TODO remove
                expiration = channelInfo.expiry
        )

        call.respondText(JSON.stringify(channelResp), ContentType.Application.Json, HttpStatusCode.OK)
    }
}

/**
 * Information required by client to setup the notification channel with gcal
 */
@Serializable
data class ChannelNewResp(
        val channelId: String,
        val address: String,
        val token: String,
        val expiration: Long
)

