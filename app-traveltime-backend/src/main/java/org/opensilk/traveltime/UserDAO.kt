package org.opensilk.traveltime

import io.ktor.auth.Principal
import kotlinx.serialization.Serializable
import org.hashids.Hashids
import org.mapdb.HTreeMap
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by drew on 12/28/17.
 */
@Singleton
class UserDAO @Inject constructor(
        private val userMap: HTreeMap<Long, UserInfo>,
        private val channelMap: HTreeMap<Long, ChannelInfo>
) {

    private val hashId = Hashids("TODO make private", 12)
    private val idCounter = AtomicLong(1)

    init {
        val info = UserInfo(
                id = 1,
                apiKey = "pass",
                firebaseToken = "token",
                channels = emptyList()
        )
        saveUserInfo(info)
    }

    fun newId(): Long {
        return makeId(System.currentTimeMillis(), idCounter.getAndIncrement())
    }

    fun encodeId(userId: Long) : String {
        return hashId.encode(userId)
    }

    fun decodeId(userId: String): Long? {
        return hashId.decode(userId)?.firstOrNull()
    }

    fun makeUser(firebaseToken: String): UserInfo {
        val userInfo = UserInfo(
                id = newId(),
                apiKey = UUID.randomUUID().toString(),
                firebaseToken =  firebaseToken,
                channels = emptyList()
        )
        userMap.put(userInfo.id, userInfo)
        return userInfo
    }

    fun getUserInfo(userId: Long): UserInfo? {
        return userMap[userId]
    }

    fun saveUserInfo(userInfo: UserInfo) {
        userMap.put(userInfo.id, userInfo)
    }

    fun makeChannel(): ChannelInfo {
        val channel = ChannelInfo(newId(), System.currentTimeMillis() + 600_000_000) //one week
        channelMap.put(channel.id, channel)
        return channel
    }
}

@Serializable
data class UserInfo(
        val id: Long,
        val apiKey: String,
        val firebaseToken: String,
        val channels: List<Long>
): Principal

@Serializable
data class ChannelInfo(
        val id: Long,
        val expiry: Long
)

fun makeId(stamp: Long, count: Long): Long {
    //based off instagrams sharding ids strategy
    return stamp.shl(24) //take bottom 39 bits
            //reserve 4
            .or(count.rem(1024).shl(10)) //10 bits for uniqueness
            .shr(10) //hashids can only handle 2^53
}
