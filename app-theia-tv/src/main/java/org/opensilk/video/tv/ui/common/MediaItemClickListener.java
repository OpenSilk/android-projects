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

package org.opensilk.video.tv.ui.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;

import org.opensilk.video.data.MediaMetaExtras;
import org.opensilk.video.tv.ui.details.DetailsActivity;
import org.opensilk.video.tv.ui.folders.FoldersActivity;
import org.opensilk.video.tv.ui.folders.FoldersListCardPresenter;
import org.opensilk.video.tv.ui.network.NetworkActivity;
import org.opensilk.video.util.Utils;

import timber.log.Timber;

/**
 * Created by drew on 4/8/16.
 */
public class MediaItemClickListener implements OnItemViewClickedListener {

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                              RowPresenter.ViewHolder rowViewHolder, Row row) {

        Context context = itemViewHolder.view.getContext();
        if (item instanceof MediaBrowser.MediaItem) {

            MediaBrowser.MediaItem mediaItem = (MediaBrowser.MediaItem) item;
            MediaMetaExtras metaExtras = MediaMetaExtras.from(mediaItem.getDescription().getExtras());

            if (metaExtras.isSpecial()) {
                Intent intent = null;
                switch (mediaItem.getMediaId()) {
                    case "special:network_folders":
                        intent = new Intent(context, NetworkActivity.class);
                        intent.putExtra(DetailsActivity.MEDIA_ITEM, mediaItem);
                        break;
                }
                if (intent != null) {
                    context.startActivity(intent);
                } else {
                    Timber.e("Unknown special item id=%s", mediaItem.getMediaId());
                }
            } else if (metaExtras.isDirectory()) {
                Intent intent = new Intent(context, FoldersActivity.class);
                intent.putExtra(DetailsActivity.MEDIA_ITEM, mediaItem);

                context.startActivity(intent);
            } else {
                Intent intent = new Intent(context, DetailsActivity.class);
                intent.putExtra(DetailsActivity.MEDIA_ITEM, mediaItem);

                Activity activity = Utils.findActivity(context);
                Bundle bundle = null;
                if (itemViewHolder.view instanceof ImageCardView) {
                    bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            activity,
                            ((ImageCardView) itemViewHolder.view).getMainImageView(),
                            DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                } else if (itemViewHolder instanceof FoldersListCardPresenter.ViewHolder) {
                    bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            activity,
                            ((FoldersListCardPresenter.ViewHolder) itemViewHolder).getBinding().icon,
                            DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                }
                activity.startActivity(intent, bundle);
            }

        } else {
            Timber.d("Unsupported object in adapter %s", item.getClass());
        }
    }
}
