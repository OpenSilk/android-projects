package org.opensilk.autumn

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

/**
 * Created by drew on 8/4/17.
 */
@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlist")
    fun getAll(): List<Playlist>

    @Query("SELECT * FROM playlist_asset where parentId = :playlistId ORDER BY pos")
    fun getAssets(playlistId: String): List<PlaylistAsset>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addPlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addPlaylistAsset(playlistAsset: PlaylistAsset)

}