package org.opensilk.media.loader.storage

import io.reactivex.Single
import org.opensilk.media.StorageContainerId
import org.opensilk.media.StorageRef

/**
 * Created by drew on 8/24/17.
 */
interface StorageLoader {
    fun directChildren(parentId: StorageContainerId, wantVideoItems: Boolean = false,
                       wantAudioItems: Boolean = false): Single<List<StorageRef>>
}