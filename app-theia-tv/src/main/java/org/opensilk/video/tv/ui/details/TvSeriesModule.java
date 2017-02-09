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

import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.video.tv.ui.common.CardPresenterModule;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 3/19/16.
 */
@Module(
        includes = CardPresenterModule.class
)
public class TvSeriesModule {

    @Provides @ScreenScope
    public OnActionClickedListener getActionsClickListener() {
        return new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {

            }
        };
    }

    @Provides @ScreenScope @Named("additionalRowsAdapter")
    public ObjectAdapter getRowsAdapter(@Named("seriesRowAdapter") ArrayObjectAdapter seriesRowAdapter) {
        return seriesRowAdapter;
    }

    @Provides @ScreenScope @Named("seriesRowAdapter")
    public ArrayObjectAdapter provideTvRowsAdapter(){
        ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
        presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        return new ArrayObjectAdapter(presenterSelector);
    }

    @Provides @ScreenScope
    public DetailsScreenExtender provideExtender(TvSeriesExtender extender){
        return extender;
    }

}
