package org.opensilk.video.telly

import dagger.Binds
import dagger.Module
import io.reactivex.Single
import org.opensilk.media.MediaRef
import org.opensilk.media.UpnpFolderId
import org.opensilk.media.loader.cds.UpnpBrowseLoader
import org.opensilk.video.upnpFolders
import org.opensilk.video.upnpVideo_folder_1_no_association
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
    val folders = upnpFolders()
    val item = upnpVideo_folder_1_no_association()

    override fun getDirectChildren(upnpFolderId: UpnpFolderId): Single<List<MediaRef>> {
        val list = ArrayList<MediaRef>()
        list.addAll(folders.filter { it.parentId == upnpFolderId })
        if (item.parentId == upnpFolderId) list.add(item)
        return Single.just(list)
    }
}

