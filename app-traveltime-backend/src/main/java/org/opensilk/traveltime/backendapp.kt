package org.opensilk.traveltime

import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
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
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.serialization.json.JSON
import org.mapdb.*
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

    @Singleton @Provides @JvmStatic
    fun provideJobChannel(): Channel<NotifyJob> {
        return Channel()
    }

    @Singleton @Provides @Named("base-url") @JvmStatic
    fun baseUrl(): String {
        return "http://localhost:8989"
    }

    @Singleton @Provides @JvmStatic
    fun db(): DB {
        //return DBMaker.fileDB("bknd.db")
        //        .fileMmapEnable()
        return DBMaker.memoryDB()
                .transactionEnable()
                .make()
    }

    @Singleton @Provides @JvmStatic
    fun users(db: DB): HTreeMap<Long, UserInfo> {
        return db.hashMap("user-info")
                .keySerializer(Serializer.LONG)
                .valueSerializer(UserInfoSerializer)
                .createOrOpen()
    }

    @Singleton @Provides @JvmStatic
    fun channels(db: DB): HTreeMap<Long, ChannelInfo> {
        return db.hashMap("channel-info")
                .keySerializer(Serializer.LONG)
                .valueSerializer(ChannelInfoSerializer)
                .createOrOpen()
    }

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
