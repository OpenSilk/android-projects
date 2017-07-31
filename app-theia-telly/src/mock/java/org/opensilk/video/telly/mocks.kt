package org.opensilk.video.telly

import android.media.browse.MediaBrowser
import android.net.Uri
import dagger.Binds
import dagger.Module
import org.opensilk.media.MediaMeta
import org.opensilk.media.MediaRef
import org.opensilk.media.MediaProviderClient
import org.opensilk.media._getMediaMeta
import org.opensilk.video.UpnpFoldersLoader
import org.opensilk.video.UpnpDevicesLoader
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
    abstract fun provideCDSDevicesLoader(mock: MockUpnpDevicesLoader): UpnpDevicesLoader
    @Binds
    abstract fun provideCDSBrowseLoader(mock: MockUpnpFoldersLoader): UpnpFoldersLoader
}

/**
 *
 */
class MockUpnpDevicesLoader
@Inject constructor(): UpnpDevicesLoader {
    override val observable: Observable<List<MediaBrowser.MediaItem>>
        get() = Observable.from(arrayOf(testUpnpDeviceItem(), testUpnpVideoItem())).toList()
}

/**
 *
 */
class MockUpnpFoldersLoader
@Inject constructor(): UpnpFoldersLoader {
    override fun observable(mediaId: String): Observable<List<MediaBrowser.MediaItem>>
            = Observable.just(testUpnpFolderItemList())
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

    override fun getMediaMeta(mediaRef: MediaRef): Single<MediaMeta> {
        return getMediaItem(mediaRef).map { it._getMediaMeta() }
    }

    override fun getMediaOverview(mediaRef: MediaRef): Single<String> {
        return Single.just("Doe remy epslon flon")
    }

    override fun getMediaArtworkUri(mediaRef: MediaRef): Single<Uri> {
        return Single.error(Exception())
    }
}