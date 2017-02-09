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

package org.opensilk.video.tv.ui.search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.opensilk.common.core.app.ScopedActivity;
import org.opensilk.common.dagger.DaggerService;
import org.opensilk.video.R;
import org.opensilk.video.VideoAppComponent;

import mortar.MortarScope;

/**
 * Created by drew on 4/14/16.
 */
public class SearchActivity extends ScopedActivity {

    public static void start(Context context) {
        context.startActivity(new Intent(context, SearchActivity.class));
    }

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        VideoAppComponent appComponent = DaggerService.getDaggerComponent(getApplicationContext());
        SearchActivityModule module = new SearchActivityModule();
        builder.withService(DaggerService.DAGGER_SERVICE,
                SearchActivityComponent.FACTORY.call(appComponent, module));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
    }

    @Override
    public boolean onSearchRequested() {
        SearchScreenFragment f = (SearchScreenFragment) getFragmentManager().findFragmentById(R.id.search_fragment);
        if (f != null) {
            f.startRecognition();
        }
        return f != null;
    }
}
