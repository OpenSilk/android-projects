package org.opensilk.video

import android.media.browse.MediaBrowser
import io.reactivex.Observable
import io.reactivex.Single
import org.opensilk.media.toMediaItem
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Loader for newly added row in home screen
 */
class NewlyAddedLoader
@Inject constructor(
        private val mDatabaseClient: DatabaseClient
) {
    val observable: Observable<List<MediaBrowser.MediaItem>> by lazy {
        mDatabaseClient.changesObservable
                //we accept any change and can easily be flooded
                .throttleFirst(5, TimeUnit.SECONDS)
                .map { true }
                .startWith(true)
                .switchMapSingle { doDatabase }
    }

    private val doDatabase: Single<List<MediaBrowser.MediaItem>> by lazy {
        mDatabaseClient.getRecentUpnpVideos().map { it.toMediaItem() }
                .toList().subscribeOn(AppSchedulers.diskIo)
    }
}