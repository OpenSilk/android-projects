package org.opensilk.video.telly

import android.media.browse.MediaBrowser
import dagger.Binds
import dagger.Module
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.media.MediaRef
import org.opensilk.media.playback.MediaProviderClient
import rx.Observable
import rx.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by drew on 5/30/17.
 */
@Module
abstract class MockUpnpLoadersModule {
    @Binds
    abstract fun provideCDSDevicesLoader(mock: MockCDSDevicesLoader): CDSDevicesLoader
    @Binds
    abstract fun provideCDSBrowseLoader(mock: MockCDSBrowseLoader): CDSBrowseLoader
}

/**
 *
 */
@ActivityScope
class MockCDSDevicesLoader
@Inject constructor(): CDSDevicesLoader {
    override val observable: Observable<MediaBrowser.MediaItem>
        get() = Observable.from(arrayOf(testUpnpDeviceItem(), testUpnpVideoItem()))
}

/**
 *
 */
@ActivityScope
class MockCDSBrowseLoader
@Inject constructor(): CDSBrowseLoader {
    override val observable: Observable<MediaBrowser.MediaItem>
        get() = Observable.from(testUpnpFolderItemList())
}


@Module
abstract class MockMediaProviderClientModule {
    @Binds abstract fun providesMediaProviderClient(impl: MockMediaProviderClient): MediaProviderClient
}

@Singleton
class MockMediaProviderClient
@Inject
constructor(): MediaProviderClient {
    override fun getMediaItem(mediaRef: MediaRef): Single<MediaBrowser.MediaItem> {
        return Single.just(testUpnpVideoItem())
    }
}