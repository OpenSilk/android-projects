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

import org.opensilk.media.MediaMeta
import org.opensilk.media.UPNP_VIDEO
import org.opensilk.media.newMediaRef
import org.opensilk.tvdb.api.TVDb
import org.opensilk.tvdb.api.model.*
import rx.Observable
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
                .onErrorResumeNext { mApi.login(mTVDbAuth) }
                .doOnNext { token -> mClient.setTvToken(token) }
                //only run this once
                .replay(1).autoConnect()
    }

    override fun lookupObservable(meta: MediaMeta): Observable<MediaMeta> {
        val ref = newMediaRef(meta.mediaId)
        if (ref.kind != UPNP_VIDEO) {
            return Observable.error(IllegalMediaKindException())
        }
        val name = meta.extras.getString(LOOKUP_NAME, "")
        val seasonNumber = meta.extras.getInt(LOOKUP_SEASON_NUM, 0)
        val episodeNumber = meta.extras.getInt(LOOKUP_EPISODE_NUM, 0)

        val cacheObservable = mClient.getTvSeriesAssociation(name)
                //pull all episodes for series
                .flatMapObservable { mClient.getTvEpisodes(it) }


        //note this will likely emit multiple episodes, one for each matching
        //series, the first emission should be the best match
        val networkObservable = mTokenObservable.flatMap { token ->
            //get list of series matching name
            Observable.defer {
                LookupService.waitTurn()
                mApi.searchSeries(token, name)
            }
        }.flatMap { data ->
            //only take first five series
            Observable.from(data.data).take(5)
        }.flatMap { ss ->
            //fetch full series metadata
            Observable.defer {
                LookupService.waitTurn()
                mTokenObservable.flatMap { token ->
                    Observable.zip(
                            mApi.series(token, ss.id),
                            mApi.seriesEpisodes(token, ss.id),
                            mApi.seriesImagesQuery(token, ss.id, "poster"),
                            mApi.seriesImagesQuery(token, ss.id, "fanart"),
                            { s, e, p, f -> SeriesEpisodesImages(s.data, e.data, p.data, f.data) }
                    )
                }
            }
        }.map { swi ->
            //insert into database
            val uri = mClient.addTvSeries(swi.series)
            mClient.addTvEpisodes(swi.series.id, swi.episodes)
            mClient.addTvImages(swi.series.id, swi.posters)
            mClient.addTvImages(swi.series.id, swi.fanart)
            return@map uri
        }.flatMap { uri ->
            //pull episodes back from database
            mClient.getTvEpisodes(uri.lastPathSegment.toLong())
        }

        return cacheObservable.onErrorResumeNext(networkObservable)
                //find our episode
                .filter { it.seasonNumber == seasonNumber && it.episodeNumber == episodeNumber }
                //no episodes is an error
                .switchIfEmpty(Observable.error(LookupException()))
    }

    class SeriesEpisodesImages(val series: Series, val episodes: List<SeriesEpisode>,
                               val posters: List<SeriesImageQuery>, val fanart: List<SeriesImageQuery>)

}
