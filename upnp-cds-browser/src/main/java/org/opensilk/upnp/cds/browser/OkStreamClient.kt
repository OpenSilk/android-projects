package org.opensilk.upnp.cds.browser

import okhttp3.*
import okio.ByteString
import org.fourthline.cling.model.message.*
import org.fourthline.cling.model.message.header.ContentTypeHeader
import org.fourthline.cling.model.message.header.UpnpHeader
import org.fourthline.cling.transport.spi.AbstractStreamClient
import org.fourthline.cling.transport.spi.StreamClient
import java.io.UnsupportedEncodingException
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

private val log: Logger = Logger.getLogger(StreamClient::class.java.name)

/**
 * Created by drew on 8/5/17.
 */
class OkStreamClient
constructor(
        okClient: OkHttpClient,
        private val mConfiguration: OkStreamClientConfig
): AbstractStreamClient<OkStreamClientConfig, Call>() {

    private val mOkClient = okClient.newBuilder()
            .connectTimeout(configuration.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    override fun stop() {
        //nothing
    }

    override fun getConfiguration(): OkStreamClientConfig {
        return mConfiguration
    }

    override fun createRequest(message: StreamRequestMessage?): Call? {
        if (message == null) {
            return null
        }
        val builder = Request.Builder()
        if (log.isLoggable(Level.FINE)) {
            log.fine("Preparing HTTP request message with method " +
                    "${message.operation.httpMethodName}: $message")
        }
        builder.url(message.operation.uri.toURL())
        builder.method(message.operation.httpMethodName, createRequestBody(message))

        if (log.isLoggable(Level.FINE)) {
            log.fine("Writing headers on Request: ${message.headers.size}")
        }
        if (!message.headers.containsKey(UpnpHeader.Type.USER_AGENT)) {
            builder.addHeader(UpnpHeader.Type.USER_AGENT.httpName,
                    configuration.getUserAgentValue(message.udaMajorVersion, message.udaMinorVersion))
        }
        for ((key,value) in message.headers.entries) {
            for (v in value) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine("Setting header '$key': $v")
                }
                builder.addHeader(key, v)
            }
        }
        return mOkClient.newCall(builder.build())
    }

    private fun createRequestBody(message: StreamRequestMessage): RequestBody? {
        if (!message.hasBody()) {
            return null
        }
        when (message.bodyType) {
            UpnpMessage.BodyType.STRING -> {
                if (log.isLoggable(Level.FINE)) {
                    log.fine("Writing textual request body: $message")
                }

                val contentType = message.contentTypeHeader?.value ?:
                        ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8

                val contentCharset = message.contentTypeCharset ?: "UTF-8"

                return RequestBody.create(MediaType.parse(contentType.toString()),
                        ByteString.encodeString(message.bodyString, charset(contentCharset)))
            }
            UpnpMessage.BodyType.BYTES -> {
                if (log.isLoggable(Level.FINE)) {
                    log.fine("Writing binary request body: $message")
                }
                if (message.contentTypeHeader == null) {
                    throw RuntimeException("Missing content type header in request message: $message")
                }
                val contentType = message.contentTypeHeader.value

                return RequestBody.create(MediaType.parse(contentType.toString()),
                        message.bodyBytes)
            }
            else -> return null //java nullability madness
        }
    }

    override fun createCallable(message: StreamRequestMessage, request: Call): Callable<StreamResponseMessage?> {
        return Callable {
            if (log.isLoggable(Level.FINE)) {
                log.fine("Sending HTTP request: $message")
            }
            val response = request.execute()
            if (response.isSuccessful) {
                return@Callable createResponse(response)
            } else {
                log.warning("Unsuccessful HTTP response: $response")
                return@Callable null
            }
        }
    }

    override fun abort(request: Call?) {
        request?.cancel()
    }

    override fun logExecutionException(t: Throwable?): Boolean {
        return true
    }

    private fun createResponse(response: Response): StreamResponseMessage {
        // Status
        val responseOperation = UpnpResponse(response.code(), response.message())

        if (log.isLoggable(Level.FINE))
            log.fine("Received response: " + responseOperation)

        val responseMessage = StreamResponseMessage(responseOperation)

        // Headers
        val headers = UpnpHeaders()
        val responseFields = response.headers()
        for (name in responseFields.names()) {
            for (value in responseFields.values(name)) {
                headers.add(name, value)
            }
        }
        responseMessage.headers = headers

        // Body
        val bytes = response.body()?.bytes()
        if (bytes != null && bytes.isNotEmpty() && responseMessage.isContentTypeMissingOrText) {

            if (log.isLoggable(Level.FINE)) {
                log.fine("Response contains textual entity body, converting then setting string on message")
            }
            try {
                responseMessage.setBodyCharacters(bytes)
            } catch (ex: UnsupportedEncodingException) {
                throw RuntimeException("Unsupported character encoding: " + ex, ex)
            }

        } else if (bytes != null && bytes.isNotEmpty()) {

            if (log.isLoggable(Level.FINE)) {
                log.fine("Response contains binary entity body, setting bytes on message")
            }
            responseMessage.setBody(UpnpMessage.BodyType.BYTES, bytes)

        } else {
            if (log.isLoggable(Level.FINE)) {
                log.fine("Response did not contain entity body")
            }
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine("Response message complete: " + responseMessage)
        }
        return responseMessage
    }

}