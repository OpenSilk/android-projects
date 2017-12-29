package org.opensilk.traveltime

import dagger.Component
import dagger.Module
import dagger.Provides
import io.ktor.application.*
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.locations.Locations
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.engine.*
import io.ktor.server.jetty.Jetty
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
    fun channelReupBob(): ChannelReupCmp.Builder
    fun channelNotifyBob(): ChannelNotifyCmp.Builder

    @Component.Builder
    abstract class Builder {
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

}

object UserInfoSerializer: Serializer<UserInfo> {

    override fun deserialize(input: DataInput2, available: Int): UserInfo {
        return JSON.parse(UserInfo.serializer(), Serializer.STRING.deserialize(input, available))
    }

    override fun serialize(out: DataOutput2, value: UserInfo) {
        Serializer.STRING.serialize(out, JSON.stringify(UserInfo.serializer(), value))
    }

}

class AppInjection(val appComponent: AppComponent): Injection

val ApplicationCall.appInjection: AppInjection
        get() = injection as AppInjection

interface CallHandler {
    suspend fun handle(call: ApplicationCall)
}

fun Application.module() {
    install(DaggerFeature) {
        injection = AppInjection(DaggerAppComponent.builder().build())
    }
    install(DefaultHeaders)
    install(CallLogging)
    install(Locations)
    install(Routing) {
        channelNew()
        channelReup()
        channelNotify()

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
