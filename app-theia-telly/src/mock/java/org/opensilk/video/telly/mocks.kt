package org.opensilk.video.telly

import dagger.Binds
import dagger.Module
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import org.opensilk.media.MediaRef
import org.opensilk.media.UpnpFolderId
import org.opensilk.video.UpnpBrowseLoader
import org.opensilk.video.UpnpBrowseLoaderImpl
import javax.inject.Inject

/**
 * Created by drew on 8/10/17.
 */
@Module
abstract class MocksModule {
    @Binds
    abstract fun upnpBrowserLoader(impl: MockUpnpBrowseLoader): UpnpBrowseLoader
}

class MockUpnpBrowseLoader @Inject constructor(): UpnpBrowseLoader {
    val folders = testUpnpFolderMetas()
    val items = testUpnpVideoMetas()

    override fun getDirectChildren(upnpFolderId: UpnpFolderId): Single<List<MediaRef>> {
        val f = folders.filter { it.parentId == upnpFolderId }
        val i = items.filter { it.parentId == upnpFolderId }
        val list = ArrayList<MediaRef>()
        list.addAll(f)
        list.addAll(i)
        return Single.just(list)
    }
}

