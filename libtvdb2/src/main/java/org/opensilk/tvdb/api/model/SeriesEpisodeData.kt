package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class SeriesEpisodeData @JvmOverloads constructor (
        val data: List<SeriesEpisode> = emptyList(),
        val errors: JSONErrors? = null,
        val links: Links? = null
)