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

package org.opensilk.video;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.opensilk.common.dagger.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 3/21/16.
 */
@Singleton
public class VideoAppPreferences {

    final SharedPreferences mPrefs;

    @Inject
    public VideoAppPreferences(@ForApplication Context context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean getBoolean(String name, boolean def) {
        return mPrefs.getBoolean(name, def);
    }

    public void putBoolean(String name, boolean val) {
        mPrefs.edit().putBoolean(name, val).apply();
    }

    public String getString(String name, String def) {
        return mPrefs.getString(name, def);
    }

    public void putString(String name, String val) {
        mPrefs.edit().putString(name, val).apply();
    }

    public int getInt(String name, int def) {
        return mPrefs.getInt(name, def);
    }

    public void putInt(String name, int val) {
        mPrefs.edit().putInt(name, val).apply();
    }

    public long getLong(String name, long def) {
        return mPrefs.getLong(name, def);
    }

    public void putLong(String name, long val) {
        mPrefs.edit().putLong(name, val).apply();
    }

    public float[] getFloatArray(String key) {
        float[] array = null;
        String s = getString(key, null);
        if (s != null) {
            try {
                JSONArray json = new JSONArray(s);
                array = new float[json.length()];
                for (int i = 0; i < array.length; i++)
                    array[i] = (float) json.getDouble(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return array;
    }

    public void putFloatArray(String key, float[] array) {
        try {
            JSONArray json = new JSONArray();
            for (float f : array)
                json.put(f);
            putString(key, json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
