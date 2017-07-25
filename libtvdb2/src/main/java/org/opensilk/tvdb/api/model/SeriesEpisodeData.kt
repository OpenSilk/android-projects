package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class SeriesEpisodeData(
        val data: List<SeriesEpisode>,
        val errors: JSONErrors? = null,
        val links: Links = Links()
)