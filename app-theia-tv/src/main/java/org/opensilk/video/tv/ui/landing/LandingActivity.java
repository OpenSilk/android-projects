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

package org.opensilk.video.tv.ui.landing;

import android.os.Bundle;
import android.widget.Toast;

import org.opensilk.common.core.app.ScopedActivity;
import org.opensilk.common.dagger.DaggerService;
import org.opensilk.video.R;
import org.opensilk.video.VideoApp;
import org.opensilk.video.VideoAppComponent;
import org.opensilk.video.tv.ui.search.SearchActivity;

import mortar.MortarScope;

/**
 * Created by drew on 3/15/16.
 */
public class LandingActivity extends ScopedActivity {

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        VideoAppComponent parentComponent = DaggerService.getDaggerComponent(getApplicationContext());
        LandingActivityModule module = new LandingActivityModule();
        LandingActivityComponent component = LandingActivityComponent.FACTORY.call(parentComponent, module);
        builder.withService(DaggerService.DAGGER_SERVICE, component);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onSearchRequested() {
        SearchActivity.start(this);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!VideoApp.hasLookupKeys()) {
            Toast.makeText(this, "No API keys!! Lookup will fail.", Toast.LENGTH_LONG).show();
        }
    }
}
