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

import android.content.Context;
import android.content.res.Resources;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.video.R;
import org.opensilk.video.data.MediaMetaExtras;

import javax.inject.Inject;

/**
 * Created by drew on 4/4/16.
 */
@ScreenScope
public class CardPresenter extends Presenter {

    private static int sSelectedBackgroundColor;
    private static int sDefaultBackgroundColor;

    private static void updateCardBackgroundColor(ImageCardView view, boolean selected) {
        int color = selected ? sSelectedBackgroundColor : sDefaultBackgroundColor;
        // Both background colors should be set because the view's background is temporarily visible
        // during animations.
        view.setBackgroundColor(color);
        view.findViewById(R.id.info_field).setBackgroundColor(color);
    }

    @Inject
    public CardPresenter() {

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
//        Timber.v("onCreateViewHolder");

        sDefaultBackgroundColor = parent.getResources().getColor(R.color.default_background);
        sSelectedBackgroundColor = parent.getResources().getColor(R.color.selected_background);

        ImageCardView cardView = new ImageCardView(parent.getContext()) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
            }
        };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        updateCardBackgroundColor(cardView, false);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
//        Timber.v("onBindViewHolder");

        final MediaBrowser.MediaItem mediaItem = (MediaBrowser.MediaItem) item;
        final MediaDescription description = mediaItem.getDescription();
        final MediaMetaExtras metaExtras = MediaMetaExtras.from(mediaItem);

        final ImageCardView cardView = (ImageCardView) viewHolder.view;
        final Context context = cardView.getContext();
        cardView.setTitleText(description.getTitle());
        cardView.setContentText(description.getSubtitle());

        final Resources resources = cardView.getResources();
        final int cardWidth = resources.getDimensionPixelSize(R.dimen.card_width);
        final int cardHeight = resources.getDimensionPixelSize(R.dimen.card_height);
        cardView.setMainImageDimensions(cardWidth, cardHeight);

        int iconResource;
        if (metaExtras.getIconResource() >= 0) {
            iconResource = metaExtras.getIconResource();
        } else if (mediaItem.isBrowsable()) {
            iconResource = R.drawable.folder_48dp;
        } else if (mediaItem.isPlayable()) {
            iconResource = R.drawable.movie_48dp;
        } else {
            iconResource = R.drawable.file_48dp;
        }

        if (description.getIconUri() != null) {
            RequestOptions options = new RequestOptions()
                    .fitCenter(cardView.getContext())
                    .fallback(iconResource);
            Glide.with(cardView.getContext())
                    .asDrawable()
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .load(description.getIconUri())
                    .into(cardView.getMainImageView());
        } else {
            cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE);
            cardView.setMainImage(context.getDrawable(iconResource));
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
//        Timber.v("onUnbindViewHolder");
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        // Remove references to images so that the garbage collector can free up memory
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }

}
