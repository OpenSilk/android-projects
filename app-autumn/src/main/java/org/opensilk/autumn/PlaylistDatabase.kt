package org.opensilk.autumn

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

/**
 * Created by drew on 8/4/17.
 */
@Database(entities = arrayOf(Playlist::class, PlaylistAsset::class), version = 1, exportSchema = false)
abstract class PlaylistDatabase : RoomDatabase() {
    abstract fun playlistDoa(): PlaylistDao
}