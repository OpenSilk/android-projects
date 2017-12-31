package org.opensilk.traveltime

import kotlinx.serialization.Serializable

/**
 * Created by drew on 12/28/17.
 */
@Serializable
data class FirebaseMessage(
        val recipient: String,
        val priority: String = "high",
        val data: Map<String, String>
)

data class NotifyJob(val firebaseMessage: FirebaseMessage)
