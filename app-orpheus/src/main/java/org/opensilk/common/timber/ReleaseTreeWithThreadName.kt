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

package org.opensilk.common.timber

/**
 * Stubs out v, d, and i logs, the optional lambda allows sending execption to crash
 * reporter server or whatever.
 *
 * Created by drew on 6/22/16.
 */
class ReleaseTreeWithThreadName(private val silentExceptionHandler: (Throwable) -> Unit = {}) : DebugTreeWithThreadName() {

    override fun v(message: String?, vararg args: Any) {
    }

    override fun v(t: Throwable, message: String?, vararg args: Any) {
    }

    override fun d(message: String?, vararg args: Any) {
    }

    override fun d(t: Throwable, message: String?, vararg args: Any) {
    }

    override fun i(message: String?, vararg args: Any) {
    }

    override fun i(t: Throwable, message: String?, vararg args: Any) {
    }

    override fun e(message: String?, vararg args: Any?) {
        super.e(message, *args)
    }

    override fun e(t: Throwable?, message: String?, vararg args: Any) {
        super.e(t, message, *args)
        if (t != null) {
            silentExceptionHandler(t)
        }
    }

}
