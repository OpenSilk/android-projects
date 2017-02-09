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

import android.databinding.DataBindingUtil;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.opensilk.video.R;
import org.opensilk.video.databinding.PlaybackOverviewRowBinding;
import org.opensilk.video.tv.ui.common.OutlineUtil;

/**
 * Created by drew on 4/9/16.
 */
public class PlaybackOverviewRowPresenter extends RowPresenter {

    @Override
    public ViewHolder createRowViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        PlaybackOverviewRowBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.playback_overview_row, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindRowViewHolder(RowPresenter.ViewHolder viewHolder, Object item) {
        super.onBindRowViewHolder(viewHolder, item);
        ViewHolder vh = (ViewHolder) viewHolder;
        PlaybackOverviewRow info = (PlaybackOverviewRow) item;
        vh.getBinding().setItem(info.getItem());
        vh.onBind();
    }

    @Override
    public void onUnbindRowViewHolder(RowPresenter.ViewHolder viewHolder) {
        super.onUnbindRowViewHolder(viewHolder);
        ViewHolder vh = (ViewHolder) viewHolder;
        vh.getBinding().setItem(null);
        vh.onUnbind();
    }

    @Override
    public boolean isUsingDefaultSelectEffect() {
        return false;
    }

    public class ViewHolder extends RowPresenter.ViewHolder implements PlaybackOverviewRow.Listener {
        private PlaybackOverviewRowBinding binding;

        public ViewHolder(PlaybackOverviewRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            OutlineUtil.setOutline(binding.overview);
        }

        public PlaybackOverviewRowBinding getBinding() {
            return binding;
        }

        void onBind() {
            PlaybackOverviewRow row = (PlaybackOverviewRow) getRow();
            if (row != null) {
                row.addListener(this);
            }
        }

        void onUnbind() {
            PlaybackOverviewRow row = (PlaybackOverviewRow) getRow();
            if (row != null) {
                row.removeListener(this);
            }
        }

        @Override
        public void onItemChanged(PlaybackOverviewRow row) {
            onUnbindViewHolder(this);
            onBindRowViewHolder(this, row);
        }

    }

}
