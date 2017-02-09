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

import android.media.MediaDescription;
import android.net.Uri;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;

import org.opensilk.video.data.VideoDescInfo;

import javax.inject.Named;

import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 3/16/16.
 */
public interface DetailsScreenComponent {
    MediaDescription mediaDescription();
    @Named("iconImage") rx.Observable<Uri> iconImageUriObservable();
    @Named("backgroundImage") rx.Observable<Uri> backgroundImageUriObservable();
    DetailsOverviewRow detailsOverviewRow();
    FullWidthDetailsOverviewRowPresenter detailsOverviewPresenter();
    @Named("rowsAdapter") ObjectAdapter rowsAdapter();
    @Named("screenSubscriptions") CompositeSubscription subscriptions();
    @Named("detailsOverview") rx.Observable<VideoDescInfo> detailsOverviewChanges();
    DetailsScreenExtender extender();
}
