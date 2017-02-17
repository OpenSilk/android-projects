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

package org.opensilk.video.tv.ui.details;

import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.os.Bundle;

import org.opensilk.common.app.ScopedActivity;
import org.opensilk.common.core.dagger2.DaggerFuncsKt;
import org.opensilk.video.R;
import org.opensilk.video.VideoAppComponent;
import org.opensilk.video.data.MediaItemUtil;

import mortar.MortarScope;
import timber.log.Timber;

/**
 * Created by drew on 3/15/16.
 */
public class DetailsActivity extends ScopedActivity {

    public static final String SHARED_ELEMENT_NAME = "hero";
    public static final String MEDIA_ITEM = "media_item";

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        VideoAppComponent appComponent = DaggerFuncsKt.getDaggerComponent(getApplicationContext());
        DetailsActivityModule activityModule = new DetailsActivityModule(getMediaItem());
        DetailsActivityComponent activityComponent = DetailsActivityComponent.FACTORY.call(appComponent, activityModule);
        DaggerFuncsKt.withDaggerComponent(builder, activityComponent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
    }

    private MediaBrowser.MediaItem getMediaItem() {
        return getIntent().getParcelableExtra(MEDIA_ITEM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 203) {
            if (resultCode == RESULT_FIRST_USER + 12) {
                MediaBrowser.MediaItem mediaItem = data.getParcelableExtra(MEDIA_ITEM);
                if (mediaItem != null) {
                    //Relaunch ourselves with the new mediaItem
                    startActivity(new Intent(this, DetailsActivity.class)
                            .putExtra(MEDIA_ITEM, mediaItem)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                }
            }
        }
    }
}
