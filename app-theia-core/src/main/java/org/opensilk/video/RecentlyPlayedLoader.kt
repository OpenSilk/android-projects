package org.opensilk.video

import io.reactivex.Observable
import io.reactivex.Single
import org.opensilk.media.VideoRef
import org.opensilk.media.database.DeviceChange
import org.opensilk.media.database.MediaDAO
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Loader for recents in home screen
 */
class RecentlyPlayedLoader
@Inject constructor(
        private val mDatabaseClient: MediaDAO
) {

    fun recentlyPlayed(): Observable<List<VideoRef>> {
        return mDatabaseClient.changesObservable
                //comes straight from db, delay longer
                .sample(3, TimeUnit.SECONDS, AppSchedulers.background)
                .startWith(DeviceChange())
                .switchMapSingle { _ ->
                    recents.subscribeOn(AppSchedulers.diskIo)
                }
    }

    private val recents: Single<List<VideoRef>> by lazy {
        mDatabaseClient.getRecentlyPlayedVideos().toList()
    }
}