package org.opensilk.common.dagger

import android.content.Context

/**
 * Created by drew on 5/19/17.
 */
const val DAGGER_SERVICE: String = "OPENSILK_DAGGER_SERVICE"

/**
 * Caller is required to know the type of the component for this context.
 *
 * @throws NoDaggerComponentException if there is no DaggerService attached to this context
 *
 * @return The Component associated with this context
 */

@Suppress("UNCHECKED_CAST")
fun <T> Context.getDaggerComponent() : T {
    return getSystemService(DAGGER_SERVICE) as? T ?:
            throw NoDaggerComponentException("No dagger component in given context")
}

class NoDaggerComponentException constructor(msg: String) : IllegalArgumentException(msg) {
    companion object {
        private val serialVersionUID = -3789316706938152733L
    }
}