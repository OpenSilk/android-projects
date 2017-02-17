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
import android.os.Bundle;
import android.support.v17.leanback.widget.Presenter;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.opensilk.common.util.BundleHelper;
import org.opensilk.video.R;
import org.opensilk.video.databinding.SubtitleCardBinding;

/**
 * Created by drew on 4/29/16.
 */
public class SubtitleCardPresenter extends Presenter {

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        SubtitleCardBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.subtitle_card, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        ViewHolder vh = (ViewHolder) viewHolder;
        Bundle bundle = (Bundle) item;
        vh.binding.title.setText(BundleHelper.getString(bundle));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ViewHolder vh = (ViewHolder) viewHolder;
    }

    public static class ViewHolder extends Presenter.ViewHolder {
        final SubtitleCardBinding binding;
        public ViewHolder(SubtitleCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
