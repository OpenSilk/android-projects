package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class SeriesImageQueryData @JvmOverloads constructor (
        val data: List<SeriesImageQuery> = emptyList(),
        val errors: JSONErrors? = null
)