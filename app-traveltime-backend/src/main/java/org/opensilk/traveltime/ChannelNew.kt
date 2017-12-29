package org.opensilk.traveltime

import dagger.Subcomponent
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.request.contentType
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
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
    post<ChannelNew> {
        call.appInjection.appComponent
                .channelNewBob().build()
                .handler().handle(call)
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

@Serializable
data class ChannelReq(
        val firebaseToken: String
)

@Serializable
data class ChannelResp(
        val channelId: String,
        val address: String,
        val token: String,
        val expiration: Long
)

class ChannelNewHandler @Inject constructor(
        @Named("base-url") private val baseUrl: String,
        private var userDAO: UserDAO
): CallHandler {

    override suspend fun handle(call: ApplicationCall) {
        if (call.request.contentType() != ContentType.Application.Json) {
            call.respond(HttpStatusCode.BadRequest, "Illegal content")
            return
        }

        val req = JSON.parse<ChannelReq>(ChannelReq.serializer(), call.receiveText())

        val userId = userDAO.newId()
        val channelId = userDAO.newId()
        val channelExpiry = System.currentTimeMillis() + 600_000_000 //one week

        val userInfo = UserInfo(req.firebaseToken, listOf(ChannelInfo(channelId, channelExpiry)))
        userDAO.saveUserInfo(userId, userInfo)

        val channelResp = ChannelResp(
                channelId = userDAO.encodeId(channelId),
                address = "$baseUrl/channel/notify/${userDAO.encodeId(userId)}/${userDAO.encodeId(channelId)}",
                token = "uid=$userId&cid=$channelId",
                expiration = channelExpiry
        )

        call.respondText(JSON.stringify(ChannelResp.serializer(), channelResp), ContentType.Application.Json, HttpStatusCode.OK)
        return
    }
}

