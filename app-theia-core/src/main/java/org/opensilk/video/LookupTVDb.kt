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

        val cacheObservable = mClient.getTvSeriesAssociation(name)
                //pull all episodes for series
                .flatMapObservable { mClient.getTvEpisodes(it) }
                .switchIfEmpty(Observable.error(LookupException("Cache returned nothing")))


        //note this will likely emit multiple episodes, one for each matching
        //series, the first emission should be the best match
        val networkObservable = mTokenObservable.flatMap { token ->
            //get list of series matching name
            Observable.defer {
                LookupService.waitTurn()
                mApi.searchSeries(token, name)
            }
        }.flatMap { data ->
            //only take first couple series
            Observable.fromIterable(data.data).take(3)
        }.flatMap { ss ->
            //fetch full series metadata
            Observable.defer {
                LookupService.waitTurn()
                mTokenObservable.flatMap { token ->
                    Observable.zip<SeriesData, SeriesEpisodeData, SeriesImageQueryData,
                            SeriesImageQueryData, SeriesEpisodesImages>(
                            mApi.series(token, ss.id),
                            mApi.seriesEpisodes(token, ss.id),
                            mApi.seriesImagesQuery(token, ss.id, "poster"),
                            mApi.seriesImagesQuery(token, ss.id, "fanart"),
                            Function4 { s, e, p, f -> SeriesEpisodesImages(s.data, e.data, p.data, f.data) }
                    )
                }
            }
        }.map { swi ->
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

        Timber.d("TV Lookup for ${meta.displayName} name=$name, s=$seasonNumber, e=$episodeNumber")

        return cacheObservable.onErrorResumeNext(networkObservable)
                //find our episode
                .filter { it.seasonNumber == seasonNumber && it.episodeNumber == episodeNumber }
                //no episodes is an error
                .switchIfEmpty(Observable.error(LookupException("Empty episode data")))
    }

    class SeriesEpisodesImages(val series: Series, val episodes: List<SeriesEpisode>,
                               val posters: List<SeriesImageQuery>, val fanart: List<SeriesImageQuery>)

}
