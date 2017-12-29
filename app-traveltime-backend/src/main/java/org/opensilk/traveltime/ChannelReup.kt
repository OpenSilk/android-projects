package org.opensilk.traveltime

import dagger.BindsInstance
import dagger.Subcomponent
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.response.respondText
import io.ktor.routing.Route
import javax.inject.Inject

/**
 * Created by drew on 12/28/17.
 */
@Location("/channel/reup/{encodedUserId}")
data class ChannelReup(val encodedUserId: String)

//installs the reeup route
fun Route.channelReup() {
    post<ChannelReup> { channelReup ->
        call.appInjection.appComponent
                .channelReupBob().channelReup(channelReup).build()
                .handler().handle(call)
    }
}

@Subcomponent
interface ChannelReupCmp {
    fun handler(): ChannelReupHandler

    @Subcomponent.Builder
    abstract class Builder {
        @BindsInstance abstract fun channelReup(channelReup: ChannelReup): Builder
        abstract fun build(): ChannelReupCmp
    }
}

class ChannelReupHandler @Inject constructor(
    private val channelReup: ChannelReup,
    private val userDAO: UserDAO
): CallHandler {
    suspend override fun handle(call: ApplicationCall) {
        val userId = userDAO.decodeId(channelReup.encodedUserId)
        val userInfo = userId?.let { userDAO.getUserInfo(it) }
        if (userInfo == null) {
            call.respondText("Unknown user", ContentType.Text.Plain, HttpStatusCode.BadRequest)
            return
        }
        val newChannel = ChannelInfo(userDAO.newId(), System.currentTimeMillis() + 600_000_000)
        val newUserInfo = userInfo.copy(channels = userInfo.channels.toMutableList().apply { add(newChannel) })

        userDAO.saveUserInfo(userId, newUserInfo)


        call.respondText("This is it: ${newChannel.channelId}", ContentType.Text.Plain, HttpStatusCode.OK)
    }
}


