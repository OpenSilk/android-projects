package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class SeriesImageQueryData(
        val data: List<SeriesImageQuery>,
        val errors: JSONErrors? = null
)