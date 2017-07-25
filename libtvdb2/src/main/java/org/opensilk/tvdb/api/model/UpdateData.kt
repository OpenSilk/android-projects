package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class UpdateData(
        val data: List<Update>,
        val errors: JSONErrors? = null
)