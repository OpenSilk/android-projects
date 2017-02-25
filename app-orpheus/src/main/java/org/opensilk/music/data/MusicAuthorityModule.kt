package org.opensilk.music.data

import android.content.Context
import dagger.Module
import dagger.Provides
import org.opensilk.common.dagger.ForApplication
import org.opensilk.music.R
import javax.inject.Named

/**
 * Created by drew on 6/26/16.
 */
@Module
class MusicAuthorityModule {

    @Provides @Named("music_authority")
    fun provideMusicAuthority(@ForApplication context: Context): String {
        return context.getString(R.string.music_provider)
    }

}