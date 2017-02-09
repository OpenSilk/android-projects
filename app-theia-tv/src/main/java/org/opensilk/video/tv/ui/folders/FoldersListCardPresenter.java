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

import android.databinding.DataBindingUtil;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.support.v17.leanback.widget.Presenter;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import org.opensilk.common.dagger.ScreenScope;
import org.opensilk.common.core.rx.RxUtils;
import org.opensilk.video.R;
import org.opensilk.video.data.DataService;
import org.opensilk.video.data.MediaItemUtil;
import org.opensilk.video.data.MediaMetaExtras;
import org.opensilk.video.data.VideoDescInfo;
import org.opensilk.video.data.VideoProgressInfo;
import org.opensilk.video.databinding.FoldersListCardBinding;

import javax.inject.Inject;

import rx.Subscription;
import timber.log.Timber;

/**
 * Created by drew on 3/22/16.
 */
@ScreenScope
public class FoldersListCardPresenter extends Presenter {

    final DataService mDataService;
    final long mCreatedAt = System.currentTimeMillis();

    @Inject
    public FoldersListCardPresenter(DataService mDataService) {
        this.mDataService = mDataService;
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        FoldersListCardBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.folders_list_card, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        ViewHolder vh = (ViewHolder) viewHolder;
        MediaBrowser.MediaItem mediaItem = (MediaBrowser.MediaItem) item;

        vh.setMediaItem(mediaItem);
        vh.registerChanges(mediaItem);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ViewHolder vh = (ViewHolder) viewHolder;
        Glide.with(vh.binding.icon.getContext()).clear(vh.binding.icon);
        vh.binding.icon.setImageDrawable(null);
        vh.unregisterChanges();
    }

    public class ViewHolder extends Presenter.ViewHolder {
        private FoldersListCardBinding binding;
        private Subscription subscriptions;

        public ViewHolder(FoldersListCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public FoldersListCardBinding getBinding() {
            return binding;
        }

        void registerChanges(MediaBrowser.MediaItem mediaItem) {
            RxUtils.unsubscribe(subscriptions);
            subscriptions = mDataService.getMediaItemOnChange(mediaItem, mCreatedAt)
                    .subscribe(item -> {
                        if (item == null) {
                            return;
                        }
                        MediaMetaExtras extras = MediaMetaExtras.from(item);
                        setMediaItem(item);
                    }, e -> {
                        Timber.w(e, "Progress changes for %s", MediaItemUtil.getMediaTitle(mediaItem));
                    });
        }

        void unregisterChanges() {
            RxUtils.unsubscribe(subscriptions);
            subscriptions = null;
        }

        void setMediaItem(MediaBrowser.MediaItem mediaItem) {
            VideoProgressInfo progressInfo = VideoProgressInfo.from(mediaItem);
            Timber.v("Binding(%s) %s", MediaItemUtil.getMediaTitle(mediaItem), progressInfo);
            binding.setProgressInfo(progressInfo);
            VideoDescInfo descInfo = VideoDescInfo.from(mediaItem);
            binding.setDesc(descInfo);

            setOrLoadIcon(mediaItem);
        }

        void setOrLoadIcon(MediaBrowser.MediaItem mediaItem) {
            MediaDescription description = mediaItem.getDescription();
            int iconResource;
            if (mediaItem.isBrowsable()) {
                iconResource = R.drawable.folder_48dp;
            } else if (mediaItem.isPlayable()) {
                iconResource = R.drawable.movie_48dp;
            } else {
                iconResource = R.drawable.file_48dp;
            }
            if (description.getIconUri() != null) {
                RequestOptions options = new RequestOptions()
                        .centerCrop(binding.icon.getContext())
                        .placeholder(iconResource);
                Glide.with(binding.icon.getContext())
                        .asDrawable()
                        .apply(options)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .load(description.getIconUri())
                        .into(binding.icon);
            } else {
                binding.icon.setImageResource(iconResource);
            }
        }
    }

}
