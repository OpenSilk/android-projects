/*
 * Copyright (c) 2016 OpenSilk Productions LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.video.data

import android.media.MediaDescription
import android.media.browse.MediaBrowser

import org.opensilk.common.dagger.ServiceScope
import org.opensilk.tvdb.api.TVDb
import org.opensilk.tvdb.api.model.AllZipped
import org.opensilk.tvdb.api.model.Banner
import org.opensilk.tvdb.api.model.Episode
import org.opensilk.tvdb.api.model.Series
import org.opensilk.video.VideoAppPreferences
import org.opensilk.video.util.Utils

import javax.inject.Inject
import javax.inject.Named

import rx.Observable
import rx.functions.Action1
import rx.functions.Func1
import timber.log.Timber
import java.util.*

/**
 * Created by drew on 4/11/16.
 */
@ServiceScope
class TVDbLookup
@Inject
constructor(
        @Named("tvdb_api_key") internal val mTVDbApiKey: String,
        internal val mApi: TVDb,
        internal val mClient: VideosProviderClient,
        internal val mPreferences: VideoAppPreferences
) : LookupService() {

    internal val mSeenSeries: MutableMap<Long, AllZipped> = LinkedHashMap()
    internal val mSeriesAssociations: MutableMap<String, Long> = LinkedHashMap()

    internal val mUpdateObservable: Observable<Boolean> by lazy {
        /*return*/ Observable.defer<Boolean> {
            LookupService.waitTurn()
            val lastUpdate = mPreferences.getLong("tvdb_lastupdate", 0)
            if (lastUpdate > 0) {
                return@defer mApi.updatesObservable("all", lastUpdate).flatMap { updates ->
                    if (updates.series != null && updates.series.size > 0) {
                        //fetch all the updates and return true
                        return@flatMap Observable.mergeDelayError<AllZipped>(Observable.from<Long>(updates.series)
                                .filter({ seriesId -> mClient.tvdb().getSeries(seriesId!!) != null })
                                .map({ seriesId -> fetchAllZipped(seriesId!!).retry(1) })
                        ).doOnNext({ allZipped -> Timber.d("Updated series=%s", allZipped.series.seriesName) })
                                //ignore returned allseries only notify if there were any updates
                                .isEmpty.map({ empty -> !empty })
                                //only update if successfully fetched all series
                                .doOnCompleted({ mPreferences.putLong("tvdb_lastupdate", updates.time!!) })
                                //if failed skip update and wait for next run
                                .onExceptionResumeNext(Observable.just(false))
                    } else {
                        //no updates, return false;
                        return@flatMap Observable.just<Boolean>(false)
                    }
                }
            } else {
                //get sentinel and return true
                return@defer mApi.updatesObservable("none")
                        .doOnNext({ updates -> mPreferences.putLong("tvdb_lastupdate", updates.time!!) })
                        .map({ updates -> true })
            }
        }.replay(1).autoConnect()
    }

    override fun lookup(mediaItem: MediaBrowser.MediaItem): rx.Observable<MediaBrowser.MediaItem> {
        val description = mediaItem.description
        val metaExtras = MediaMetaExtras.from(description)
        val mediaTitle = MediaDescriptionUtil.getMediaTitle(description)
        val query = Utils.extractSeriesName(mediaTitle)
        if (query.isNullOrBlank()) {
            return rx.Observable.error<MediaBrowser.MediaItem>(Exception("Failed to extract series name from " + mediaTitle))
        }
        query!!
        val seasonNumber = Utils.extractSeasonNumber(mediaTitle)
        val episodeNumber = Utils.extractEpisodeNumber(mediaTitle)
        if (seasonNumber < 0 || episodeNumber < 0) {
            return rx.Observable.error<MediaBrowser.MediaItem>(Exception("Failed to extract season and episode from " + mediaTitle))
        }

        val cacheObservable = rx.Observable.create<MediaBrowser.MediaItem> { subscriber ->
            //Timber.d("lookup(%s)", mediaTitle);
            val series_id: Long = mSeriesAssociations[query] ?: mClient.tvdb().getSeriesAssociation(query)
            if (series_id != -1L) {
                mSeriesAssociations[query] = series_id
                val allZipped: AllZipped = mSeenSeries[series_id] ?: buildAllZipped(series_id)
                mSeenSeries.put(series_id, allZipped)
                if (!subscriber.isUnsubscribed) {
                    subscriber.onNext(buildMediaItem(mediaItem, allZipped, seasonNumber, episodeNumber))
                    subscriber.onCompleted()
                }
                return@create
            }
            if (!subscriber.isUnsubscribed) {
                subscriber.onError(Exception("Missed cache q=" + query))
            }
        }

        val networkObservable = rx.Observable.defer {
            LookupService.waitTurn()
            Timber.d("lookup(%s)->[series=%s]", mediaTitle, query)
            mApi.getSeriesObservable(query, "en").flatMap({ seriesList ->
                val series = seriesList.getSeries()
                if (series != null && !series.isEmpty()) {
                    val s = series[0]
                    return@flatMap fetchAllZipped(s.id!!)
                            .doOnNext({ allZipped ->
                                //TODO just do this up there ^ ??
                                mClient.tvdb().setSeriesAssociation(query, allZipped.series.id!!)
                                mSeriesAssociations.put(query, allZipped.series.id)
                            })
                } else {
                    return@flatMap Observable.error<AllZipped>(Exception("no results"))
                }
            }).map({ allZipped ->
                return@map buildMediaItem(mediaItem, allZipped, seasonNumber, episodeNumber)
            })
        }

        return mUpdateObservable.flatMap { wasUpdated ->
            if (!metaExtras.isIndexed || wasUpdated!!) {
                return@flatMap cacheObservable.onExceptionResumeNext(networkObservable)
            } else {
                return@flatMap Observable.empty<MediaBrowser.MediaItem>()
            }
        }
    }

    internal fun buildAllZipped(series_id: Long): AllZipped {
        val series = mClient.tvdb().getSeries(series_id)
        val episodes = mClient.tvdb().getEpisodes(series_id)
        val banners = mClient.tvdb().getBanners(series_id)
        return AllZipped.Builder()
                    .setSeries(series)
                    .setEpisodes(episodes)
                    .setBanners(banners)
                    .build()
    }

    internal fun fetchAllZipped(seriesId: Long): Observable<AllZipped> {
        return Observable.defer {
            LookupService.waitTurn()
            return@defer mApi.allZippedObservable(mTVDbApiKey, seriesId, "en")
        }.doOnNext { allZipped ->
            mClient.tvdb().insertAllZipped(allZipped)
            mSeenSeries.put(allZipped.series.id, allZipped)
        }
    }

    internal fun buildMediaItem(
            mediaItem: MediaBrowser.MediaItem, allZipped: AllZipped,
            seasonNumber: Int, episodeNumber: Int
    ): MediaBrowser.MediaItem? {
        val description = mediaItem.description
        val metaExtras = MediaMetaExtras.from(description)
        val builder = MediaDescriptionUtil.newBuilder(description)

        val series = allZipped.series
        metaExtras.seriesId = series.id!!
        metaExtras.isIndexed = true

        if (series.posterPath.isNotBlank()) {
            builder.setIconUri(mClient.tvdb().makeBannerUri(series.posterPath))
        }

        val episode = findEpisode(allZipped.episodes, seasonNumber, episodeNumber)
        if (episode != null) {
            metaExtras.episodeId = episode.id!!
            builder.setTitle(episode.episodeName)
            builder.setSubtitle(mClient.tvdb().makeSubtitle(series.seriesName,
                    episode.seasonNumber!!, episode.episodeNumber!!))
        }
        val banner = findBanner(allZipped.banners, seasonNumber)
        if (banner != null) {
            builder.setIconUri(mClient.tvdb().makeBannerUri(banner.bannerPath))
        }
        val backdrop = findBackdrop(allZipped.banners)
        if (backdrop != null) {
            metaExtras.backdropUri = mClient.tvdb().makeBannerUri(backdrop.bannerPath)
        }
        builder.setExtras(metaExtras.bundle)
        val newMediaItem = MediaBrowser.MediaItem(builder.build(), mediaItem.flags)
        //        Timber.d("lookup(%s)->buildMediaItem(series=%s)", mediaTitle, series.getSeriesName());
        //            mClient.insertMedia(newMediaItem);
        return newMediaItem
        //            mDataService.notifyChange(newMediaItem);
    }

    internal fun findEpisode(episodes: List<Episode>, seasonNumber: Int, episodeNumber: Int): Episode? {
        episodes.forEach { episode ->
            if (episode.seasonNumber == seasonNumber && episode.episodeNumber == episodeNumber) {
                return episode
            }
        }
        return null
    }

    internal fun findBanner(banners: List<Banner>, seasonNumber: Int): Banner? {
        var bestMatch: Banner? = null
        for (banner in banners) {
            if (!banner.isSeason) {
                continue
            }
            if (banner.season == seasonNumber) {
                if (bestMatch == null) {
                    bestMatch = banner
                    continue
                }
                if (bestMatch.rating < banner.rating) {
                    bestMatch = banner
                }
            }
        }
        return bestMatch
    }

    internal fun findBackdrop(banners: List<Banner>): Banner? {
        var bestMatch: Banner? = null
        for (banner in banners) {
            if (!banner.isFanart) {
                continue
            }
            if (bestMatch == null) {
                bestMatch = banner
                continue
            }
            if (banner.bannerType2 == "1920x1080" && bestMatch.bannerType2 != "1920x1080") {
                bestMatch = banner
                continue
            }
            if (bestMatch.rating < banner.rating) {
                bestMatch = banner
            }
        }
        return bestMatch
    }

}
