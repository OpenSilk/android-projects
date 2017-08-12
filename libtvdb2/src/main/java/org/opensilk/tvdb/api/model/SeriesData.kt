package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class SeriesData @JvmOverloads constructor (
        val data: Series = Series(),
        val errors: JSONErrors? = null
)