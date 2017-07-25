package org.opensilk.tvdb.api.model

/**
 * Created by drew on 7/25/17.
 */
data class JSONErrors(
    val invalidFilters: List<String> = emptyList(),
    val invalidLanguage: String = "",
    val invalidQueryParams: List<String> = emptyList()
)