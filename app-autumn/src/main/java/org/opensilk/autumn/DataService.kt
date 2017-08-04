package org.opensilk.autumn

import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by drew on 8/4/17.
 */
@Singleton
class DataService
@Inject constructor(
        private val mDatabase: PlaylistDao,
        private val mNetwork: NetworkApi
) {

    fun getPlaylists(): Single<List<Playlist>> {
        return getFromDatabase().flatMap { list ->
            return@flatMap if (list.isEmpty()) {
                getFromNetwork()
            } else {
                Single.just(list)
            }.subscribeOn(AppSchedulers.io)
        }
    }

    fun getFromNetwork(): Single<List<Playlist>> {
        return mNetwork.getPlaylists().flatMapObservable {
            Observable.fromIterable(it)
        }.map { pl ->
            //apply the parentId and pos
            val assets = pl.assets.mapIndexedTo(ArrayList<PlaylistAsset>()) {
                idx, asset -> asset.copy(pos = idx, parentId = pl.id) }
            return@map pl.copy(assets = assets)
        }.doOnNext { pl ->
            //insert into the playlist
            mDatabase.addPlaylist(pl)
            pl.assets.forEach { asset ->
                mDatabase.addPlaylistAsset(asset)
            }
        }.toList()
    }

    fun getFromDatabase(): Single<List<Playlist>> {
        return Single.fromCallable {
            //get the playlists
            mDatabase.getAll()
        }.flatMapObservable { list ->
            //shard
            Observable.fromIterable(list)
        }.flatMap { pl ->
            //inject the assets
            Observable.fromCallable {
                pl.copy(assets = mDatabase.getAssets(pl.id))
            }
        }.toList()
    }

}