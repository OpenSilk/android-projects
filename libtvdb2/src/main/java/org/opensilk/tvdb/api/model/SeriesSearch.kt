package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class SeriesSearch(
        val aliases: List<String> = emptyList(),
        val banner: String = "",
        val firstAired: String = "",
        val id: Long,
        val network: String = "",
        val overview: String = "",
        val seriesName: String,
        val status: String = ""
)