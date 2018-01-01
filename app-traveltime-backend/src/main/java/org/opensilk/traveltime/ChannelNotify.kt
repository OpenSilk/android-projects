package org.opensilk.traveltime

import dagger.BindsInstance
import dagger.Subcomponent
import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.routing.Route
import javax.inject.Inject

/**
 * Created by drew on 12/28/17.
 */
@Location("/channel/notify/{encodedUserId}")
data class ChannelNotify(val encodedUserId: String)

fun Route.channelNotify() {
    post<ChannelNotify> { channelNotify ->
        application.appComponent
                .channelNotifyBob().channelNotify(channelNotify).build()
                .handler().handle(call)
    }
}

@Subcomponent
interface ChannelNotifyCmp {
    fun handler(): ChannelNotifyHandler

    @Subcomponent.Builder
    abstract class Builder {
        @BindsInstance abstract fun channelNotify(channelNotify: ChannelNotify): Builder
        abstract fun build(): ChannelNotifyCmp
    }
}

class ChannelNotifyHandler @Inject constructor(
        private val userDAO: UserDAO,
        private val channelNotify: ChannelNotify,
        private val messageWorker: MessageWorker
): CallHandler {
    suspend override fun handle(call: ApplicationCall) {
        val channel = call.request.header("X-Goog-Channel-ID")
        if (channel == null) {
            call.respondOK()
            return
        }
        if (channel.startsWith("TEST")) {
            call.application.log.info("Received test notification")
            call.respondOK()
            return
        }

        val channelInfo = userDAO.decodeId(channel)?.let { userDAO.getChannelInfo(it) }
        val userInfo = userDAO.decodeId(channelNotify.encodedUserId)?.let { userDAO.getUserInfo(it) }

        if (userInfo == null || channelInfo == null || !userInfo.channels.contains(channelInfo.id)) {
            call.respondOK()
            return
        }

        val resource = call.request.header("X-Goog-Resource-ID") ?: ""
        val state = call.request.header("X-Goog-Resource-State") ?: ""
        if (state == "sync") {
            //TODO attach resource to channel for unsubscribe
            call.respondOK()
            return
        }

        val expiry = call.request.header("X-Goog-Channel-Expiration") ?: ""

        call.application.log.debug("Received notification on $channelInfo for $userInfo")

        messageWorker.channel.send(MessageJob(FirebaseMessage(
                recipient = userInfo.firebaseToken,
                data = mapOf("resource" to resource,
                        "state" to state,
                        "expire" to expiry
                )
        )))

        call.respondOK()
    }

    private suspend fun ApplicationCall.respondOK() {
        respond(HttpStatusCode.OK, "")
    }
}
