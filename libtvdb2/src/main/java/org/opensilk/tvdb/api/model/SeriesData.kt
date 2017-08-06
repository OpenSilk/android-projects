package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class SeriesData(
        val data: Series,
        val errors: JSONErrors? = null
)