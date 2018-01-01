package org.opensilk.traveltime

import io.ktor.auth.UserPasswordCredential
import kotlinx.coroutines.experimental.runBlocking
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

/**
 * Created by drew on 12/31/17.
 */
class AuthTest {

    lateinit var userDAO: UserDAO

    @Before
    fun setup() {
        userDAO = makeUserDAO()
    }

    @Test
    fun passEndsWithSpace() {
        val auth = Auth(userDAO, UserPasswordCredential(userOneEncoded, "pass "))
        runBlocking {
            Assertions.assertThat(auth.validate()).isEqualTo(userDAO.getUserInfo(1))
        }
    }

    @Test
    fun passEndsWithNewline() {
        val auth = Auth(userDAO, UserPasswordCredential(userOneEncoded, """pass
"""))
        runBlocking {
            Assertions.assertThat(auth.validate()).isEqualTo(userDAO.getUserInfo(1))
        }
    }

    @Test
    fun passEndsWithTab() {
        val auth = Auth(userDAO, UserPasswordCredential(userOneEncoded, "pass\t"))
        runBlocking {
            Assertions.assertThat(auth.validate()).isEqualTo(userDAO.getUserInfo(1))
        }
    }
}