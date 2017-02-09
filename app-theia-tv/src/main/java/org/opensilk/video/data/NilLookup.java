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

package org.opensilk.video.data;

import android.media.browse.MediaBrowser;

import org.opensilk.common.dagger.ServiceScope;

import javax.inject.Inject;

import rx.Observable;

/**
 * Created by drew on 6/14/16.
 */
@ServiceScope
public class NilLookup extends LookupService {

    @Inject
    public NilLookup() {

    }

    @Override
    public Observable<MediaBrowser.MediaItem> lookup(MediaBrowser.MediaItem mediaItem) {
        return Observable.error(new Exception("Lookup not supported"));
    }
}
