package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class SeriesEpisode(
        val absoluteNumber: Int? = null,
        val airedEpisodeNumber: Int,
        val airedSeason: Int,
        val airedSeasonId: Long = 0,
        val dvdEpisodeNumber: Int? = null,
        val dvdSeason: Int? = null,
        val episodeName: String,
        val firstAired: String = "",
        val id: Long,
        val lastUpdated: Long = 0,
        val overview: String? = null
)