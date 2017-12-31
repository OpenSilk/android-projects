package org.opensilk.traveltime

import dagger.BindsInstance
import dagger.Subcomponent
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.Authentication
import io.ktor.auth.UserPasswordCredential
import io.ktor.auth.basicAuthentication
import io.ktor.routing.Route
import io.ktor.routing.application
import javax.inject.Inject

/**
 * Created by drew on 12/30/17.
 */
fun Route.restricted() {
    install(Authentication) {
        basicAuthentication("You should know") {
            application.appComponent.authBob().credential(it).build().auth().validate()
        }
    }
}

@Subcomponent
interface AuthCmp {
    fun auth(): Auth

    @Subcomponent.Builder
    abstract class Builder {
        @BindsInstance abstract fun credential(credential: UserPasswordCredential): Builder
        abstract fun build(): AuthCmp
    }
}

/**
 * Auth checks if user name matches api key
 */
class Auth @Inject constructor(
        private val userDAO: UserDAO,
        private val credential: UserPasswordCredential
        //private val app: Application
) {
    suspend fun validate(): UserInfo? {
        val info = userDAO.decodeId(credential.name)?.let {
            userDAO.getUserInfo(it)
        }
        //app.log.debug("User info for ${credential.name} :: ${credential.password} = $info")
        val normalizedPass = credential.password.trim(' ', '\n', '\t')
        return if (normalizedPass == info?.apiKey) info else null
    }
}
