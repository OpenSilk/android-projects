package org.opensilk.traveltime

import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test

/**
 * Created by drew on 12/31/17.
 */
class MessageWorkerTest {

    @Test
    fun testNotifyWorker() {
        val worker = MessageWorker("fookey")
        runBlocking {
            worker.sendMessage(FirebaseMessage("recipient", "high", mapOf("foo" to "bar")))
        }
    }
}