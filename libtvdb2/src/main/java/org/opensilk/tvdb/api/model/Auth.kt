package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class Auth @JvmOverloads constructor (
        val apiKey: String = "",
        val userKey: String = "",
        val username: String = ""
)