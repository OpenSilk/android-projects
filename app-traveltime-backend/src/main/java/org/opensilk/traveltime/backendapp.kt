package org.opensilk.traveltime

import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import io.ktor.application.*
import io.ktor.client.utils.Url
import io.ktor.client.utils.UrlBuilder
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.locations.Locations
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.JSON
import org.mapdb.*
import org.slf4j.Logger
import javax.inject.Named
import javax.inject.Singleton

@Component(modules = [
    AppModule::class
])
@Singleton
interface AppComponent {
    fun channelNewBob(): ChannelNewCmp.Builder
    fun channelNotifyBob(): ChannelNotifyCmp.Builder
    fun channelUnsubBob(): ChannelUnsubCmp.Builder

    fun registerBob(): RegisterCmp.Builder

    fun authBob(): AuthCmp.Builder

    @Component.Builder
    abstract class Builder {
        @BindsInstance abstract fun app(app: Application): Builder
        abstract fun build(): AppComponent
    }
}

@Module
object AppModule {

    @Singleton @Provides @Named("base-url") @JvmStatic
    fun baseUrl(): String = "http://localhost:8989"

    @Singleton @Provides @Named("firebase-key") @JvmStatic
    fun firebaseKey(): String = "foo"

    private val testEndpoint = UrlBuilder().apply {
        scheme = "http"
        host = "localhost"
        path = "/log"
        port = 43545
    }.build()

    private val firebaseEndpoint = UrlBuilder().apply {
        scheme = "https"
        host = "fcm.googleapis.com"
        path = "/fcm/send"
    }.build()

    @Singleton @Provides @Named("firebase-endpoint") @JvmStatic
    fun firebaseEndpoint(): Url = testEndpoint //firebaseEndpoint

    @Singleton @Provides @JvmStatic
    fun db(): DB {
        //return DBMaker.fileDB("bknd.db")
        //        .fileMmapEnable()
        return DBMaker.memoryDB()
                .transactionEnable()
                .make()
    }

    @Singleton @Provides @JvmStatic
    fun users(db: DB): HTreeMap<Long, UserInfo> =
            db.hashMap("user-info")
                .keySerializer(Serializer.LONG)
                .valueSerializer(UserInfoSerializer)
                .createOrOpen()

    @Singleton @Provides @JvmStatic
    fun channels(db: DB): HTreeMap<Long, ChannelInfo> =
            db.hashMap("channel-info")
                .keySerializer(Serializer.LONG)
                .valueSerializer(ChannelInfoSerializer)
                .createOrOpen()

    @Singleton @Provides @JvmStatic
    fun logger(app: Application): Logger = app.log

}

object UserInfoSerializer: Serializer<UserInfo> {

    override fun deserialize(input: DataInput2, available: Int): UserInfo {
        return JSON.parse(UserInfo.serializer(), Serializer.STRING.deserialize(input, available))
    }

    override fun serialize(out: DataOutput2, value: UserInfo) {
        Serializer.STRING.serialize(out, JSON.stringify(UserInfo.serializer(), value))
    }

}

object ChannelInfoSerializer: Serializer<ChannelInfo> {

    override fun deserialize(input: DataInput2, available: Int): ChannelInfo {
        return JSON.parse(ChannelInfo.serializer(), Serializer.STRING.deserialize(input, available))
    }

    override fun serialize(out: DataOutput2, value: ChannelInfo) {
        Serializer.STRING.serialize(out, JSON.stringify(ChannelInfo.serializer(), value))
    }
}

interface CallHandler {
    suspend fun handle(call: ApplicationCall)
}

val Application.appComponent: AppComponent
    get() = attributes[appComponentKey]

private val appComponentKey = AttributeKey<AppComponent>("appcomponent")

fun Application.module() {
    //attach the app component to the application
    attributes.put(appComponentKey, DaggerAppComponent.builder().app(this).build())

    install(DefaultHeaders)
    install(CallLogging)
    install(Locations)
    install(Routing) {
        register()

        channelNew()
        channelNotify()
        channelUnsub()

        get("/") {
            call.respondText("Hello There")
        }
    }
}

fun main(args: Array<String>) {
    val env = applicationEngineEnvironment {
        connector {
            port = 8989
        }
        module {
            module()
        }
    }
    embeddedServer(Jetty, env).start()
}
