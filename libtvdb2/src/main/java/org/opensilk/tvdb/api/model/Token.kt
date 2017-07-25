package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class Token(val token: String) {
    override fun toString(): String {
        return "Bearer $token"
    }
}