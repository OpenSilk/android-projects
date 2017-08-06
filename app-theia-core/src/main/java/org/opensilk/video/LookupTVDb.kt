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
import io.reactivex.functions.Function
import io.reactivex.functions.Function4
import org.opensilk.media.MediaMeta
import org.opensilk.media.UPNP_VIDEO
import org.opensilk.media.newMediaRef
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
        private val mClient: DatabaseClient
) : LookupHandler {

    internal val mTokenObservable: Observable<Token> by lazy {
        //pull cached token
        mClient.getTvToken().toObservable()
                // make sure it is still valid
                .flatMap { token -> mApi.refreshToken(token) }
                //if above fails do fresh login
                .onErrorResumeNext(Function { mApi.login(mTVDbAuth) })
                .doOnNext { token -> mClient.setTvToken(token) }
                //only run this once
                .replay(1).autoConnect()
    }

    override fun lookupObservable(meta: MediaMeta): Observable<MediaMeta> {
        if (!meta.isVideo) {
            return Observable.error(IllegalMediaKindException())
        }
        val name = meta.lookupName
        val seasonNumber = meta.seasonNumber
        val episodeNumber = meta.episodeNumber

        //note this will likely emit multiple episodes, one for each matching
        //series, the first emission should be the best match
        val networkObservable = mTokenObservable.flatMap { token ->
            //get list of series matching name
            Observable.defer {
                LookupService.waitTurn()
                Timber.d("Searching name=$name")
                mApi.searchSeries(token, name)
            }
        }.flatMap { data ->
            Observable.fromIterable(data.data).take(1).doOnNext {
                Timber.d("Found series ${it.seriesName}")
            }
        }.flatMap { ss ->
            mClient.getTvEpisodes(ss.id).switchIfEmpty(fetchCompleteSeriesInfo(ss))
        }

        Timber.d("TV Lookup for ${meta.displayName} name=$name, s=$seasonNumber, e=$episodeNumber")

        return networkObservable
                //find our episode
                .filter { it.seasonNumber == seasonNumber && it.episodeNumber == episodeNumber }
                //no episodes is an error
                .switchIfEmpty(Observable.error(LookupException("Empty episode data")))
    }

    class SeriesEpisodesImages(val series: Series, val episodes: List<SeriesEpisode>,
                               val posters: List<SeriesImageQuery>, val fanart: List<SeriesImageQuery>)

    private fun fetchCompleteSeriesInfo(ss: SeriesSearch): Observable<MediaMeta> {
        //fetch full series metadata
        return Observable.defer {
            LookupService.waitTurn()
            mTokenObservable.flatMap { token ->
                Observable.zip<Series, List<SeriesEpisode>, List<SeriesImageQuery>,
                        List<SeriesImageQuery>, SeriesEpisodesImages>(
                        mApi.series(token, ss.id).map { it.data },
                        fetchPaginatedEpisodes(token, ss.id, 1).toList().toObservable(),
                        mApi.seriesImagesQuery(token, ss.id, "poster").map { it.data },
                        mApi.seriesImagesQuery(token, ss.id, "fanart").map { it.data },
                        Function4 { s, e, p, f -> SeriesEpisodesImages(s, e, p, f) }
                )
            }
        }.map { swi ->
            Timber.d("Fetched series ${swi.series.seriesName} ${swi.episodes.size} episodes")
            for (e in swi.episodes) {
                Timber.d("Episode ${e.episodeName} s${e.airedSeason}e${e.airedEpisodeNumber}")
            }
            //insert into database
            val uri = mClient.addTvSeries(swi.series, swi.posters.firstOrNull(), swi.fanart.firstOrNull())
            mClient.addTvEpisodes(swi.series.id, swi.episodes)
            mClient.addTvImages(swi.series.id, swi.posters)
            mClient.addTvImages(swi.series.id, swi.fanart)
            return@map uri
        }.flatMap { uri ->
            //pull episodes back from database
            mClient.getTvEpisodes(uri.lastPathSegment.toLong())
        }
    }

    private fun fetchPaginatedEpisodes(token: Token, seriesId: Long, page: Int): Observable<SeriesEpisode> {
        return mApi.seriesEpisodes(token, seriesId, page).flatMap { data ->
            val next = data.links?.next
            return@flatMap if (next == null) {
                Observable.fromIterable(data.data)
            } else {
                Observable.fromIterable(data.data)
                        .concatWith(fetchPaginatedEpisodes(token, seriesId, next))
            }
        }
    }
}
