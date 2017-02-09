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

import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.video.data.VideoDescInfo;

import javax.inject.Inject;

/**
 * Created by drew on 4/2/16.
 */
@ScreenScope
public class DetailsVideoDescPresenter extends AbstractDetailsDescriptionPresenter {

    @Inject
    public DetailsVideoDescPresenter() {
    }

    @Override
    protected void onBindDescription(ViewHolder vh, Object item) {
        VideoDescInfo info = (VideoDescInfo) item;
        vh.getTitle().setText(info.getTitle());
        vh.getSubtitle().setText(info.getSubtitle());
        vh.getBody().setText(info.getOverview());
    }
}
