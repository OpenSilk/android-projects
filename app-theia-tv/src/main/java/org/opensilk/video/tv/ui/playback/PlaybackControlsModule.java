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

import android.content.Context;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.support.v17.leanback.app.PlaybackOverlayFragment;

import org.opensilk.common.dagger.ForActivity;
import org.opensilk.common.dagger.ScreenScope;
import org.opensilk.video.data.DataService;
import org.opensilk.video.data.VideoDescInfo;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * Created by drew on 3/22/16.
 */
@Module
public class PlaybackControlsModule {
    final Context context;
    final PlaybackOverlayFragment fragment;

    public PlaybackControlsModule(Context context, PlaybackOverlayFragment fragment) {
        this.context = context;
        this.fragment = fragment;
    }

    @Provides @ForActivity
    public Context provideContext() {
        return context;
    }

    @Provides
    public PlaybackOverlayFragment provideFragment() {
        return fragment;
    }

    @Provides @ScreenScope @Named("itemChangeSubject")
    public Subject<MediaDescription, MediaDescription> provideOverviewSubject() {
        return PublishSubject.create();
    }

    @Provides @ScreenScope @Named("itemChange")
    public rx.Observable<MediaBrowser.MediaItem> provideMediaItemObservable(
            DataService dataService,
            @Named("itemChangeSubject") Subject<MediaDescription, MediaDescription> overviewSubject
    ) {
        return overviewSubject.flatMap(desc -> dataService.getMediaItemSingle(desc).toObservable());
    }

    @Provides @ScreenScope @Named("itemOverview")
    public rx.Observable<VideoDescInfo> provideVideoDescChanges(
            @Named("itemChange") rx.Observable<MediaBrowser.MediaItem> itemObservable,
            DataService dataService
    ) {
        return itemObservable.flatMap(dataService::getVideoDescription);
    }

}
