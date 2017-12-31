package org.opensilk.traveltime

import dagger.BindsInstance
import dagger.Subcomponent
import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.auth.authentication
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.locations.handle
import io.ktor.locations.location
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.method
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import javax.inject.Inject

/**
 * Created by drew on 12/30/17.
 */
@Location("/channel/unsub/{encodedChannelId}")
data class ChannelUnsub(val encodedChannelId: String)

fun Route.channelUnsub() {
    location<ChannelUnsub> {
        restricted()
        method(HttpMethod.Post) {
            handle<ChannelUnsub> { channelUnsub ->
                application.appComponent.channelUnsubBob()
                        .channelUnsub(channelUnsub).build()
                        .handler().handle(call)
            }
        }
    }
}

@Subcomponent
interface ChannelUnsubCmp {
    fun handler(): ChannelUnsubHandler

    @Subcomponent.Builder
    abstract class Builder {
        @BindsInstance abstract fun channelUnsub(channelUnsub: ChannelUnsub): Builder
        abstract fun build(): ChannelUnsubCmp
    }
}

class ChannelUnsubHandler @Inject constructor(
    private val userDAO: UserDAO,
    private val channelUnsub: ChannelUnsub
): CallHandler {
    suspend override fun handle(call: ApplicationCall) {
        val userInfo = call.authentication.principal<UserInfo>()!!

        val channelId = userDAO.decodeId(channelUnsub.encodedChannelId)

        if (!userInfo.channels.contains(channelId)) {
            call.respondText(JSON.stringify(ChannelUnsubResp(false, "Unregistered channel")),
                    ContentType.Application.Json, HttpStatusCode.OK)
            return
        }

        //TODO unregister

        call.respondText(JSON.stringify(ChannelUnsubResp(true, "")),
                ContentType.Application.Json, HttpStatusCode.OK)
    }
}

@Serializable
data class ChannelUnsubResp(
        val success: Boolean,
        val message: String
)
