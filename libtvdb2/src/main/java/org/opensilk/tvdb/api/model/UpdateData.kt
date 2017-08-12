package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class UpdateData @JvmOverloads constructor (
        val data: List<Update> = emptyList(),
        val errors: JSONErrors? = null
)