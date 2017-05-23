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

package org.opensilk.video.tv.ui.playback;

import android.content.res.Resources;
import android.media.MediaDescription;
import android.media.session.MediaSession;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import org.opensilk.video.R;

/**
 * Created by drew on 4/9/16.
 */
public class QueuePresenter extends Presenter {

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        ImageCardView imageCardView = new ImageCardView(parent.getContext());
        imageCardView.setFocusable(true);
        imageCardView.setFocusableInTouchMode(true);
        return new ViewHolder(imageCardView);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        final MediaSession.QueueItem queueItem = (MediaSession.QueueItem) item;
        final MediaDescription description = queueItem.getDescription();

        final ImageCardView cardView = (ImageCardView) viewHolder.view;
        cardView.setTitleText(description.getTitle());
        cardView.setContentText(description.getSubtitle());

        final Resources resources = cardView.getResources();
        final int cardWidth = resources.getDimensionPixelSize(R.dimen.card_width);
        final int cardHeight = resources.getDimensionPixelSize(R.dimen.card_height);
        cardView.setMainImageDimensions(cardWidth, cardHeight);

        if (description.getIconUri() != null) {
            RequestOptions options = new RequestOptions()
                    .fitCenter()
                    .fallback(R.drawable.movie_48dp);
            Glide.with(cardView.getContext())
                    .asDrawable()
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .load(description.getIconUri())
                    .into(cardView.getMainImageView());
        } else {
            cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE);
            cardView.getMainImageView().setImageResource(R.drawable.movie_48dp);
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        final ImageCardView imageCardView = (ImageCardView) viewHolder.view;
        imageCardView.setMainImage(null);
        imageCardView.setBadgeImage(null);
    }
}
