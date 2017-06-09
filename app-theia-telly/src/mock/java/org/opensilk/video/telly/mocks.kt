package org.opensilk.video.telly

import android.media.browse.MediaBrowser
import dagger.Binds
import dagger.Module
import org.opensilk.common.dagger.ActivityScope
import rx.Observable
import javax.inject.Inject

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