package org.opensilk.video.telly

import android.media.browse.MediaBrowser
import dagger.Binds
import dagger.Module
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.media.MediaRef
import org.opensilk.media.playback.MediaProviderClient
import org.opensilk.video.CDSBrowseLoader
import org.opensilk.video.CDSDevicesLoader
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
class MockCDSDevicesLoader
@Inject constructor(): CDSDevicesLoader {
    override val observable: Observable<List<MediaBrowser.MediaItem>>
        get() = Observable.from(arrayOf(testUpnpDeviceItem(), testUpnpVideoItem())).toList()
}

/**
 *
 */
class MockCDSBrowseLoader
@Inject constructor(): CDSBrowseLoader {
    override fun observable(mediaId: String): Observable<MediaBrowser.MediaItem>
            = Observable.from(testUpnpFolderItemList())
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