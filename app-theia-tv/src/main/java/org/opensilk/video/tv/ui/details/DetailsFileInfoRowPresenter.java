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

import android.databinding.DataBindingUtil;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.opensilk.common.dagger.ScreenScope;
import org.opensilk.video.R;
import org.opensilk.video.databinding.DetailsFileInfoRowBinding;

import javax.inject.Inject;

/**
 * Created by drew on 3/21/16.
 */
@ScreenScope
public class DetailsFileInfoRowPresenter extends RowPresenter {

    @Inject
    public DetailsFileInfoRowPresenter() {
    }

    @Override
    protected ViewHolder createRowViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        DetailsFileInfoRowBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.details_file_info_row, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder vh, Object item) {
//        Timber.d("onBindRowViewHolder");
        super.onBindRowViewHolder(vh, item);
        ViewHolder viewHolder = (ViewHolder) vh;
        DetailsFileInfoRow row = (DetailsFileInfoRow) item;
        viewHolder.getBinding().setInfo(row.getItem());
        viewHolder.onBind();
    }

    @Override
    protected void onUnbindRowViewHolder(RowPresenter.ViewHolder vh) {
//        Timber.d("onUnbindRowViewHolder");
        super.onUnbindRowViewHolder(vh);
        ViewHolder viewHolder = (ViewHolder) vh;
        viewHolder.getBinding().setInfo(null);
        viewHolder.onUnbind();
    }

    public class ViewHolder extends RowPresenter.ViewHolder implements DetailsFileInfoRow.Listener  {

        private DetailsFileInfoRowBinding binding;

        public ViewHolder(DetailsFileInfoRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
//            OutlineUtil.setOutline(binding.infoCard);
        }

        public DetailsFileInfoRowBinding getBinding() {
            return binding;
        }

        void onBind() {
            DetailsFileInfoRow row = (DetailsFileInfoRow) getRow();
            if (row != null) {
                row.addListener(this);
            }
        }

        void onUnbind() {
            DetailsFileInfoRow row = (DetailsFileInfoRow) getRow();
            if (row != null) {
                row.removeListener(this);
            }
        }

        @Override
        public void onItemChanged(DetailsFileInfoRow fileInfoRow) {
            onUnbindRowViewHolder(this);
            onBindRowViewHolder(this, fileInfoRow);
        }
    }
}
