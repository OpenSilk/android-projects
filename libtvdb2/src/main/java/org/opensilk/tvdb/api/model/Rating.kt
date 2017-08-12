package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class Rating @JvmOverloads constructor (
        val average: Float = 0F,
        val count: Int = 0
)