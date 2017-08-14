package org.opensilk.video.telly

import dagger.Binds
import dagger.Module
import io.reactivex.Maybe
import io.reactivex.Single
import org.opensilk.media.DocumentId
import org.opensilk.media.DocumentRef
import org.opensilk.media.MediaRef
import org.opensilk.media.UpnpContainerId
import org.opensilk.media.loader.cds.UpnpBrowseLoader
import org.opensilk.media.loader.doc.DocumentLoader
import org.opensilk.media.testdata.upnpFolders
import org.opensilk.media.testdata.upnpVideo_folder_1_no_association
import javax.inject.Inject

/**
 * Created by drew on 8/10/17.
 */
@Module
abstract class MocksModule {
    @Binds
    abstract fun upnpBrowserLoader(impl: MockUpnpBrowseLoader): UpnpBrowseLoader
    @Binds
    abstract fun documentLoader(impl: MockDocumentsLoader): DocumentLoader
}

class MockUpnpBrowseLoader @Inject constructor(): UpnpBrowseLoader {
    val folders = upnpFolders()
    val item = upnpVideo_folder_1_no_association()

    override fun directChildren(upnpFolderId: UpnpContainerId, wantVideoItems: Boolean, wantAudioItems: Boolean): Single<out List<MediaRef>> {
        val list = ArrayList<MediaRef>()
        list.addAll(folders.filter { it.id.parentId == upnpFolderId.containerId })
        if (item.id.parentId == upnpFolderId.containerId) list.add(item)
        return Single.just(list)
    }
}

class MockDocumentsLoader @Inject constructor(): DocumentLoader {
    override fun document(documentId: DocumentId): Maybe<DocumentRef> {
        TODO("not implemented")
    }

    override fun directChildren(documentId: DocumentId, wantVideoItems: Boolean, wantAudioItems: Boolean): Single<out List<DocumentRef>> {
        TODO("not implemented")
    }
}

