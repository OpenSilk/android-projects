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

package org.opensilk.music.data.ref

import android.net.Uri
import android.util.JsonReader
import android.util.JsonWriter
import java.io.StringReader
import java.io.StringWriter

/**
 * Created by drew on 6/20/16.
 */
open class SpecialRef internal constructor(
        private val kind: String
) : MediaRef {

    internal constructor(map: Map<String, String>): this(map["k"]!!)

    override val mediaId: String by lazy {
        val map = mapOf(
                Pair("k", kind)
        )
        buildMediaId(MEDIA_KIND_SPECIAL, map)
    }

    override val mediaUri: Uri by lazy {
        throw UnsupportedOperationException()
    }

    override val childrenUri: Uri by lazy {
        throw UnsupportedOperationException()
    }

}

const val SPECIAL_KIND_HEADER = "header"
const val SPECIAL_KIND_ACTION = "action"

class ActionRef(): SpecialRef(SPECIAL_KIND_ACTION)
class HeaderRef(): SpecialRef(SPECIAL_KIND_HEADER)
