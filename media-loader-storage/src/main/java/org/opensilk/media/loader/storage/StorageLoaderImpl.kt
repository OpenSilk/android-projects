package org.opensilk.media.loader.storage

import android.content.Context
import android.graphics.Path
import android.os.Build
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

    private data class Opts(val wantAudioItems: Boolean, val wantVideoItems: Boolean)

    override fun directChildren(parentId: StorageContainerId, wantVideoItems: Boolean,
                                wantAudioItems: Boolean): Single<List<StorageRef>> = when {
        Build.VERSION.SDK_INT >= 24 -> directChildrenApi24(parentId, Opts(wantAudioItems, wantVideoItems))
        else -> directChildrenApi21(parentId, Opts(wantAudioItems, wantVideoItems))
    }

    private fun directChildrenApi21(parentId: StorageContainerId, opts: Opts): Single<List<StorageRef>> = when {
        mContext.canReadPrimaryStorage() -> directChildrenReadValidated(parentId, opts)
        else -> Single.just(emptyList())
    }

    private fun directChildrenApi24(parentId: StorageContainerId, opts: Opts): Single<List<StorageRef>> = when {
        mContext.hasAccessActivity() -> TODO()
        mContext.canReadPrimaryStorage() -> directChildrenReadValidated(parentId, opts)
        else -> Single.just(emptyList())
    }

    private fun directChildrenReadValidated(parentId: StorageContainerId, opts: Opts): Single<List<StorageRef>> {
        return Single.create { s ->
            if (s.isDisposed) {
                return@create
            }
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
                        f.isVideoFile() && opts.wantVideoItems -> f.toVideoRef(parentId.uuid)
                        else -> null
                    }
                })
                s.onSuccess(list)
            }
        }
    }
}

