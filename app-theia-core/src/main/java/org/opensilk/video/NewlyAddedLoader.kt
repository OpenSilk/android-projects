package org.opensilk.video

import android.media.browse.MediaBrowser
import io.reactivex.Observable
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
                .map { true }
                .startWith(true)
                //we accept any change and can easily be flooded
                .throttleFirst(5, TimeUnit.SECONDS)
                .switchMapSingle {
                    mDatabaseClient.getRecentUpnpVideos().map {
                        it.toMediaItem()
                    }.toList().subscribeOn(AppSchedulers.diskIo)
                }
    }
}