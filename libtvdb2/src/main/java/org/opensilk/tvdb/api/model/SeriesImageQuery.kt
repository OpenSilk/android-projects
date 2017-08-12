package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
class SeriesImageQuery @JvmOverloads constructor (
        val fileName: String = "",
        val id: Long = 0,
        val keyType: String = "",
        val ratingsInfo: Rating = Rating(),
        val resolution: String = "",
        val subKey: String = "",
        val thumbnail: String = ""
)