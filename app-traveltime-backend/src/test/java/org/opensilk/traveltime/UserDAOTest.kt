package org.opensilk.traveltime

import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.mapdb.Atomic
import org.mapdb.HTreeMap
import org.mapdb.Serializer

/**
 * Created by drew on 12/28/17.
 */
class UserDAOTest {

    @Test
    fun testUserIdGen() {
        val stamp = 1514514297877L
        val id = 2000L
        val want = stamp.shl(14).or(id.rem(1024)).and(0x1fffffffffffff)
        val hexStamp = java.lang.Long.toHexString(stamp)
        val hexWant = java.lang.Long.toHexString(want)
        Assertions.assertThat(java.lang.Long.toHexString(makeId(stamp, id))).isEqualTo(hexWant)
    }

}

fun makeUserDAO(): UserDAO {
    return  UserDAO(
            HTreeMap.make(keySerializer = Serializer.LONG, valueSerializer = UserInfoSerializer),
            HTreeMap.make(keySerializer = Serializer.LONG, valueSerializer = ChannelInfoSerializer)
    )
}

val userOneEncoded = "pmnyadz0B75G"
