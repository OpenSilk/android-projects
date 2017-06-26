/*
 * Copyright (c) 2016 OpenSilk Productions LLC.
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

package org.opensilk.video

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

import org.json.JSONArray
import org.json.JSONException
import org.opensilk.common.dagger.ForApplication

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by drew on 3/21/16.
 * Converted from java TODO make idomatic
 */
@Singleton
class VideoAppPreferences @Inject
constructor(@ForApplication context: Context) {

    internal val mPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun getBoolean(name: String, def: Boolean): Boolean {
        return mPrefs.getBoolean(name, def)
    }

    fun putBoolean(name: String, `val`: Boolean) {
        mPrefs.edit().putBoolean(name, `val`).apply()
    }

    fun getString(name: String, def: String?): String? {
        return mPrefs.getString(name, def)
    }

    fun putString(name: String, `val`: String) {
        mPrefs.edit().putString(name, `val`).apply()
    }

    fun getInt(name: String, def: Int): Int {
        return mPrefs.getInt(name, def)
    }

    fun putInt(name: String, `val`: Int) {
        mPrefs.edit().putInt(name, `val`).apply()
    }

    fun getLong(name: String, def: Long): Long {
        return mPrefs.getLong(name, def)
    }

    fun putLong(name: String, `val`: Long) {
        mPrefs.edit().putLong(name, `val`).apply()
    }

    fun getFloatArray(key: String): FloatArray {
        var array: FloatArray = FloatArray(0)
        val s = getString(key, null)
        if (s != null) {
            try {
                val json = JSONArray(s)
                array = FloatArray(json.length())
                for (i in array.indices)
                    array[i] = json.getDouble(i).toFloat()
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
        return array
    }

    fun putFloatArray(key: String, array: FloatArray) {
        try {
            val json = JSONArray()
            for (f in array)
                json.put(f.toDouble())
            putString(key, json.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }
}
