package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class SeriesEpisode @JvmOverloads constructor (
        val airedEpisodeNumber: Int = 0,
        val airedSeason: Int = 0,
        val episodeName: String = "",
        val firstAired: String = "",
        val id: Long = 0,
        val overview: String = ""
)