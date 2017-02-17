/*
 * Copyright (c) 2016 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.common.dagger2

import android.content.Context

import mortar.MortarScope
import org.opensilk.common.mortar.HasScope

const val DAGGER_SERVICE: String = "OPENSILK_DAGGER_SERVICE"

/**
 * Extension fun to add dagger component to the scope
 */
fun MortarScope.Builder.withDaggerComponent(component: Any): MortarScope.Builder {
    return this.withService(DAGGER_SERVICE, component)
}

/**
 * Caller is required to know the type of the component for this context.
 *
 * @throws NoDaggerComponentException if there is no DaggerService attached to this context
 *
 * @return The Component associated with this context
 */
@Suppress("UNCHECKED_CAST")
@Throws(NoDaggerComponentException::class)
fun <T> getDaggerComponent(context: Context): T {
    return context.getSystemService(DAGGER_SERVICE) as? T ?: throw NoDaggerComponentException()
}

/**
 * Caller is required to know the type of the component for this scope.
 *
 * @throws NoDaggerComponentException if there is no DaggerService attached to this scope
 *
 * @return The Component associated with this scope
 */
@Suppress("UNCHECKED_CAST")
@Throws(NoDaggerComponentException::class)
fun <T> getDaggerComponent(scope: MortarScope): T {
    return if (scope.hasService(DAGGER_SERVICE)) {
        scope.getService<Any>(DAGGER_SERVICE) as T
    } else {
        throw NoDaggerComponentException(scope)
    }
}

fun <T> HasScope.daggerComponent(): T {
    return if (this.scope.hasService(DAGGER_SERVICE)) {
        scope.getService(DAGGER_SERVICE)
    } else {
        throw NoDaggerComponentException(this.scope)
    }
}

class NoDaggerComponentException : IllegalArgumentException {
    internal constructor() : super("No dagger component in given context")
    internal constructor(scope: MortarScope) : super("No dagger component found in scope ${scope.name}")
    companion object {
        private val serialVersionUID = -3789316706938152733L
    }
}