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

package org.opensilk.video;

import android.content.Context;

import org.opensilk.common.core.dagger2.ForApplication;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * A bit of a hack to allow using the {@link org.opensilk.video.data.VideosProviderClient}
 * from child scopes, inparticular the @ActivityScope, without exposing them from the
 * singleton scope (we just create another instance of this module). This is really just
 * being lazy.
 *
 * Created by drew on 4/3/16.
 */
@Module
public class ConfigModule {

    @Provides @Named("videosAuthority")
    public String provideVideosAuthority(@ForApplication Context context) {
        return context.getString(R.string.videos_authority);
    }

    @Provides @Named("tvdb_root")
    public String provideTvDbRoot() {
        return "https://thetvdb.com/";
    }

    @Provides @Named("tvdb_api_key")
    public String provideTvdbApikey() {
        return BuildConfig.TVDB_API_KEY;
    }

    @Provides @Named("tmdb_root")
    public String provideTMDBRoot() {
        return "https://api.themoviedb.org/3/";
    }

    @Provides @Named("tmdb_api_key")
    public String provideTMDBApiKey(){
        return BuildConfig.TMDB_API_KEY;
    }

}
