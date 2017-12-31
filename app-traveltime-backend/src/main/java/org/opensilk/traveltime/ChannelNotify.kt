package org.opensilk.traveltime

import dagger.BindsInstance
import dagger.Subcomponent
import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.routing.Route
import javax.inject.Inject

/**
 * Created by drew on 12/28/17.
 */
@Location("/channel/notify/{encodedChannelId}")
data class ChannelNotify(val encodedChannelId: String)

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

): CallHandler {
    suspend override fun handle(call: ApplicationCall) {
        TODO("not implemented")
    }
}