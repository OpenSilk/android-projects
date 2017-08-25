package org.opensilk.media.loader.storage

import android.content.Context
import android.os.Environment
import android.webkit.MimeTypeMap
import io.reactivex.Single
import org.opensilk.dagger2.ForApp
import org.opensilk.media.*
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Created by drew on 8/17/17.
 */
class StorageLoaderImpl @Inject constructor(
        @ForApp private val mContext: Context
): StorageLoader {

    override fun directChildren(parentId: StorageContainerId, wantVideoItems: Boolean,
                                wantAudioItems: Boolean): Single<List<StorageRef>> = when {
        mContext.hasAccessActivity() -> TODO()
        mContext.canReadPrimaryStorage() -> {
            Single.create { s ->
                val directory = File(parentId.path)
                if (!directory.isDirectory || !directory.exists() || !directory.canRead()) {
                    s.onError(Exception("Unable to access directory ${directory.path}"))
                } else {
                    val children = directory.listFiles() ?: emptyArray()
                    val list = ArrayList<StorageRef>()
                    children.mapNotNullTo(list, { f ->
                        //Timber.d(f.path)
                        when {
                            f.isHidden -> null
                            f.isDirectory -> f.toDirectoryRef(parentId.uuid)
                            f.isVideoFile() && wantVideoItems -> f.toVideoRef(parentId.uuid)
                            else -> null
                        }
                    })
                    s.onSuccess(list)
                }
            }
        }
        else -> Single.just(emptyList())
    }
}

