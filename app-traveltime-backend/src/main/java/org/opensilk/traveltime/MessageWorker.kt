package org.opensilk.traveltime

import io.ktor.client.call.call
import io.ktor.client.request
import io.ktor.client.utils.Url
import io.ktor.client.utils.url
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import javax.inject.Inject
import javax.inject.Named

/**
 * Handles dispatching messages to firebase
 *
 * Created by drew on 12/31/17.
 */
//TODO switch to new firebase api
class MessageWorker @Inject constructor(
        //private val app: Application,
        @Named("firebase-key") private val firebaseKey: String,
        @Named("firebase-endpoint") private val firebaseEndpoint: Url
) {
    val channel = Channel<MessageJob>()

    init {
        launch { work() }
    }

    suspend fun work() {
        for (job in channel) {
            sendMessage(job.message)
        }
    }

    suspend fun sendMessage(message: FirebaseMessage) {
        val req = request {
            url(firebaseEndpoint)
            method = HttpMethod.Post
            headers {
                append("Authorization", "key=$firebaseKey")
                contentType(ContentType.Application.Json)
            }
            body = JSON.stringify(message)
        }
        val client = io.ktor.client.HttpClientFactory.createDefault(
                io.ktor.client.backend.apache.ApacheBackend
                //io.ktor.client.backend.jetty.JettyHttp2Backend
        )
        val resp = client.call(req)
        if (resp.status != HttpStatusCode.OK) {
            //TODO
            //app.log.warn("Firebase errorCode=${resp.status}")
            return
        }
        val respMessage = resp.body.toString()
        //app.log.debug("Firebase resp=$respMessage")
    }

}

data class MessageJob(
        val message: FirebaseMessage
)

@Serializable
data class FirebaseMessage(
        val recipient: String,
        val priority: String = "high",
        val data: Map<String, String>
)