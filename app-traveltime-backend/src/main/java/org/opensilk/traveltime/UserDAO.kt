package org.opensilk.traveltime

import kotlinx.serialization.Serializable
import org.hashids.Hashids
import org.mapdb.HTreeMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by drew on 12/28/17.
 */
@Singleton
class UserDAO @Inject constructor(
        private val userMap: HTreeMap<Long, UserInfo>
) {

    private val hashId = Hashids("TODO make private", 12)
    private val idCounter = AtomicLong(1)

    fun newId(): Long {
        return makeId(System.currentTimeMillis(), idCounter.getAndIncrement())
    }

    fun encodeId(userId: Long) : String {
        return hashId.encode(userId)
    }

    fun decodeId(userId: String): Long? {
        return hashId.decode(userId)?.firstOrNull()
    }

    fun getUserInfo(userId: Long): UserInfo? {
        return userMap[userId]
    }

    fun saveUserInfo(userId: Long, userInfo: UserInfo) {
        userMap.put(userId, userInfo)
    }

}

@Serializable
data class UserInfo(
        val firebaseId: String,
        val channels: List<ChannelInfo>
)

@Serializable
data class ChannelInfo(
        val channelId: Long,
        val expiry: Long
)

fun makeId(stamp: Long, count: Long): Long {
    //based off instagrams sharding ids strategy
    return stamp.shl(24) //take bottom 39 bits
            //reserve 4
            .or(count.rem(1024).shl(10)) //10 bits for uniqueness
            .shr(10) //hashids can only handle 2^53
}
