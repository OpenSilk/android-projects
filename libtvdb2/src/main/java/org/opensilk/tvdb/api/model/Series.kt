package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class Series (
        val added: String = "",
        val airsDaysOfWeek: String = "",
        val airsTime: String = "",
        val aliases: List<String> = emptyList(),
        val banner: String = "",
        val firstAired: String = "",
        val genre: List<String> = emptyList(),
        val id: Long,
        val imdbId: String = "",
        val lastUpdated: Long,
        val network: String = "",
        val networkId: String = "",
        val overview: String = "",
        val rating: String = "",
        val runtime: String = "",
        val seriesId: Long = 0L,
        val seriesName: String,
        val siteRating: Float = 0F,
        val siteRatingCount: Long = 0L,
        val status: String = "",
        val zap2itId: String = ""
)
