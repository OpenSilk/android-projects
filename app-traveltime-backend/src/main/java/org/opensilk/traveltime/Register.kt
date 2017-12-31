package org.opensilk.traveltime

import dagger.Subcomponent
import io.ktor.application.ApplicationCall
import io.ktor.application.application
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

/**
 * Created by drew on 12/30/17.
 */
@Location("/register")
class Register

fun Route.register() {
    post<Register> {
        application.appComponent.registerBob().build().handler().handle(call)
    }
}

@Subcomponent interface RegisterCmp {
    fun handler(): RegisterHandler

    @Subcomponent.Builder
    abstract class Builder {
        abstract fun build(): RegisterCmp
    }
}

/**
 * Handles creating a new user and sending info back to client
 */
class RegisterHandler @Inject constructor(
    private val userDAO: UserDAO
): CallHandler {
    suspend override fun handle(call: ApplicationCall) {
        //TODO this could probably be installed as a 'Feature'
        if (call.request.contentType() != ContentType.Application.Json) {
            call.respond(HttpStatusCode.BadRequest, "Illegal content")
            return
        }

        //parse request, this will throw if malformed
        val req = JSON.parse<RegisterReq>(call.receiveText())

        //create new user
        val userInfo = userDAO.makeUser(firebaseToken = req.firebaseToken)

        //build response
        val encodedUser = userDAO.encodeId(userInfo.id)
        val channelResp = RegisterResp(
                userName = encodedUser,
                apiKey = userInfo.apiKey
        )

        call.respondText(JSON.stringify(channelResp), ContentType.Application.Json, HttpStatusCode.OK)
    }
}

/**
 * Information sent in the request
 */
@Serializable
data class RegisterReq(
        val firebaseToken: String
)

/**
 * Information needed to call restricted endpoints
 */
@Serializable
data class RegisterResp(
        val userName: String,
        val apiKey: String
)