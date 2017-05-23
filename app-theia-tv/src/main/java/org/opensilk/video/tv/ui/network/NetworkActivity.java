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

package org.opensilk.video.tv.ui.network;

import android.os.Bundle;

import org.opensilk.common.app.ScopedActivity;
import org.opensilk.common.dagger.FuncsKt;
import org.opensilk.common.dagger2.DaggerFuncsKt;
import org.opensilk.video.R;
import org.opensilk.video.VideoAppComponent;

import mortar.MortarScope;

/**
 * Created by drew on 4/10/16.
 */
public class NetworkActivity extends ScopedActivity {

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        VideoAppComponent parentComponent = FuncsKt.getDaggerComponent(getApplicationContext());
        NetworkActivityModule module = new NetworkActivityModule();
        NetworkActivityComponent component = NetworkActivityComponent.FACTORY.call(parentComponent, module);
        DaggerFuncsKt.withDaggerComponent(builder, component);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);
    }
}
