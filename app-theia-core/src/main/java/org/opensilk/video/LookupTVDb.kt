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

package org.opensilk.video

import io.reactivex.Observable
import io.reactivex.Single
import org.opensilk.media.TvEpisodeRef
import org.opensilk.media.TvSeriesId
import org.opensilk.media.database.MediaDAO
import org.opensilk.tvdb.api.TVDb
import org.opensilk.tvdb.api.model.*
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by drew on 4/11/16.
 */
class LookupTVDb
@Inject
constructor(
        private val mTVDbAuth: Auth,
        private val mApi: TVDb,
        private val mClient: MediaDAO,
        private val mAppClient: VideoAppDAO
) : LookupHandler {

    internal val mTokenObservable: Observable<Token> by lazy {
        //pull cached token
        mAppClient.getTvToken()
                // make sure it is still valid
                .flatMap { token -> mApi.refreshToken(token) }
                //if above fails do fresh login
                .onErrorResumeNext({ mApi.login(mTVDbAuth).retry(2) })
                .doOnSuccess { token -> mAppClient.setTvToken(token) }
                //only run this once
                .toObservable().replay(1).autoConnect()
    }

    override fun lookupObservable(lookup: LookupRequest): Observable<TvEpisodeRef> {
        val name = lookup.lookupName
        val seasonNumber = lookup.seasonNumber
        val episodeNumber = lookup.episodeNumber

        val networkObservable = mTokenObservable.flatMapSingle { token ->
            //get list of series matching name
            Single.defer {
                Timber.d("Searching name=$name")
                mApi.searchSeries(token, name).retry(1)
            }
        }.flatMap { data ->
            Observable.fromIterable(data.data).take(1).doOnNext {
                Timber.d("Found series ${it.seriesName}")
            }
        }.flatMap { ss ->
            mClient.getTvEpisodesForTvSeries(TvSeriesId(ss.id))
                    .switchIfEmpty(fetchCompleteSeriesInfo(ss))
        }

        Timber.d("TV Lookup name=$name, s=$seasonNumber, e=$episodeNumber")

        return networkObservable
                //find our episode
                .filter { it.meta.seasonNumber == seasonNumber && it.meta.episodeNumber == episodeNumber }
                //no episodes is an error
                .switchIfEmpty(Observable.error(LookupException("Empty episode data")))
    }

    private class SeriesEpisodesImages(val series: SeriesData,
                               val episodes: SeriesEpisodesList,
                               val posters: SeriesImageQueryData,
                               val fanart: SeriesImageQueryData,
                               val season: SeriesImageQueryData)

    private data class SeriesEpisodesList(val episodes: MutableList<SeriesEpisode> = ArrayList())

    private fun fetchCompleteSeriesInfo(ss: SeriesSearch): Observable<TvEpisodeRef> {
        //fetch full series metadata
        return Observable.defer {
            LookupService.waitTurn()
            mTokenObservable.flatMapSingle { token ->
                Single.zip(listOf(
                        mApi.series(token, ss.id),
                        fetchPaginatedEpisodes(token, ss.id, 1)
                                .collectInto(SeriesEpisodesList(), { (episodes), e -> episodes.add(e) }),
                        mApi.seriesImagesQuery(token, ss.id, "poster"),
                        mApi.seriesImagesQuery(token, ss.id, "fanart"),
                        mApi.seriesImagesQuery(token, ss.id, "season")),
                        { SeriesEpisodesImages(
                                it[0] as SeriesData,
                                it[1] as SeriesEpisodesList,
                                it[2] as SeriesImageQueryData,
                                it[3] as SeriesImageQueryData,
                                it[4] as SeriesImageQueryData) }
                )
            }
        }.map { swi ->
            Timber.d("Fetched series ${swi.series.data.seriesName} ${swi.episodes.episodes.size} episodes")
            //for (e in swi.episodes.data) {
            //    Timber.d("Episode ${e.episodeName} s${e.airedSeason}e${e.airedEpisodeNumber}")
            //}
            //insert into database
            val ref = swi.series.data.toTvSeriesRef(swi.posters.data.firstOrNull(), swi.fanart.data.firstOrNull())
            val episodes = swi.episodes.episodes.map { ep -> ep.toTvEpisodeRef(swi.series.data.id,
                    swi.season.data.firstOrNull { (it.subKey.toIntOrNull() ?: -1) == ep.airedSeason },
                    swi.fanart.data.firstOrNull()) }
            val posters = swi.posters.data.map { it.toTvImage(swi.series.data.id) }
            val bakcdrops = swi.posters.data.map { it.toTvImage(swi.series.data.id) }
            val seasons = swi.posters.data.map { it.toTvImage(swi.series.data.id) }
            mClient.addTvSeries(ref)
            mClient.addTvEpisodes(episodes)
            mClient.addTvImages(posters)
            mClient.addTvImages(bakcdrops)
            mClient.addTvImages(seasons)
            return@map ref.id
        }.flatMap { id ->
            //pull episodes back from database
            mClient.getTvEpisodesForTvSeries(id)
        }
    }

    private fun fetchPaginatedEpisodes(token: Token, seriesId: Long, page: Int): Observable<SeriesEpisode> {
        return mApi.seriesEpisodes(token, seriesId, page).flatMapObservable { data ->
            val next = data.links?.next
            return@flatMapObservable if (next == null) {
                Observable.fromIterable(data.data)
            } else {
                Observable.fromIterable(data.data)
                        .concatWith(fetchPaginatedEpisodes(token, seriesId, next))
            }
        }
    }
}
