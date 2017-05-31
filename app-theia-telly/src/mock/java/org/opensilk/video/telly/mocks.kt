package org.opensilk.video.telly

import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri
import dagger.Binds
import dagger.Module
import org.fourthline.cling.model.meta.RemoteDeviceIdentity
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.media.MediaMeta
import org.opensilk.media._setMediaMeta
import rx.Observable
import javax.inject.Inject

/**
 * Created by drew on 5/30/17.
 */

@Module
abstract class MockUpnpLoadersModule {
    @Binds
    abstract fun provideCDSDevicesLoader(mock: MockCDSDevicesLoader): CDSDevicesLoader
}

@ActivityScope
class MockCDSDevicesLoader
@Inject constructor(): CDSDevicesLoader {
    override val observable: Observable<MediaBrowser.MediaItem> by lazy {
            val id = "mockupnpservice-1"
            val label = "Mock CDService"
            val subtitle = "Made by mock"
            val mediaExtras = MediaMeta()
            val builder = MediaDescription.Builder()
                    .setTitle(label)
                    .setSubtitle(subtitle)
                    ._mediaRef(MediaRef(UPNP_DEVICE, id))
                    ._setMediaMeta(mediaExtras)
            Observable.just(MediaBrowser.MediaItem(builder.build(), MediaBrowser.MediaItem.FLAG_BROWSABLE))
        }
}