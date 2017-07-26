package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
class SeriesImageQuery(
        val fileName: String,
        val id: Long,
        val keyType: String,
        val languageId: Long? = null,
        val ratingsInfo: Rating = Rating(),
        val resolution: String? = null,
        val subKey: String? = null,
        val thumbnail: String? = null
)