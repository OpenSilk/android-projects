package org.opensilk.media.loader.cds

import io.reactivex.Maybe
import io.reactivex.Single
import org.opensilk.media.*

/**
 * Loader used by the UpnpFoldersLoader
 */
interface UpnpBrowseLoader {
    /**
     * Emits list of direct children, if there are no children (list size zero) completes
     */
    fun directChildren(upnpFolderId: UpnpContainerId, wantVideoItems: Boolean = false,
                       wantAudioItems: Boolean = false): Single<out List<MediaRef>>
}

