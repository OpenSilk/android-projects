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

import org.opensilk.common.app.BaseApp;
import org.opensilk.common.dagger2.DaggerFuncsKt;

import mortar.MortarScope;

/**
 * Created by drew on 3/15/16.
 */
public class VideoApp extends BaseApp {

    @Override
    public void onCreate() {
        super.onCreate();
        setupTimber(true, null);

        //start job scheduler
//        sendBroadcast(new Intent(this, BootCompleteReceiver.class));
    }

    @Override
    protected Object getRootComponent() {
        return VideoAppComponent.FACTORY.call(this);
    }

    @Override
    protected void onBuildRootScope(MortarScope.Builder builder) {
    }

    public static VideoAppPreferences getPreferences(Context context) {
        return DaggerFuncsKt.<VideoAppComponent>getDaggerComponent(context.getApplicationContext()).preferences();
    }

    public static boolean hasTMDBKey() {
        return !"NOKEY".equals(BuildConfig.TMDB_API_KEY);
    }

    public static boolean hasTVDBKey() {
        return !"NOKEY".equals(BuildConfig.TVDB_API_KEY);
    }

    public static boolean hasLookupKeys() {
        return hasTMDBKey() && hasTVDBKey();
    }

}
