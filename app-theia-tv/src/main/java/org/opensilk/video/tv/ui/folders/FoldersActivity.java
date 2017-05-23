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

package org.opensilk.video.tv.ui.folders;

import android.media.browse.MediaBrowser;
import android.os.Bundle;

import org.opensilk.common.app.ScopedActivity;
import org.opensilk.common.dagger.FuncsKt;
import org.opensilk.common.dagger2.DaggerFuncsKt;
import org.opensilk.video.R;
import org.opensilk.video.VideoAppComponent;
import org.opensilk.video.tv.ui.details.DetailsActivity;

import mortar.MortarScope;

/**
 * Created by drew on 3/22/16.
 */
public class FoldersActivity extends ScopedActivity {

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        VideoAppComponent appComponent = FuncsKt.getDaggerComponent(getApplicationContext());
        FoldersActivityModule activityModule = new FoldersActivityModule(getMediaItem());
        FoldersActivityComponent activityComponent = FoldersActivityComponent.FACTORY.call(appComponent, activityModule);
        DaggerFuncsKt.withDaggerComponent(builder, activityComponent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folders);
    }



    MediaBrowser.MediaItem getMediaItem() {
        return getIntent().getParcelableExtra(DetailsActivity.MEDIA_ITEM);
    }
}
