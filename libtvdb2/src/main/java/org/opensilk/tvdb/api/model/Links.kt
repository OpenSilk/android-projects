package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class Links(
        val first: Int = 0,
        val last: Int = 0,
        val next: Int? = null,
        val previous: Int? = null
)