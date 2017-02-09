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

package org.opensilk.video.tv.ui.landing;

import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.PresenterSelector;

import org.opensilk.common.core.dagger2.ScreenScope;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by drew on 3/16/16.
 */
@ScreenScope
public class MoviesAdapter extends ArrayObjectAdapter {

    @Inject
    public MoviesAdapter(@Named("cardPresenterSelector") PresenterSelector presenterSelector) {
        super(presenterSelector);
    }

}
