package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class SeriesEpisode(
        val airedEpisodeNumber: Int,
        val airedSeason: Int,
        val episodeName: String,
        val firstAired: String = "",
        val id: Long,
        val overview: String? = null
)