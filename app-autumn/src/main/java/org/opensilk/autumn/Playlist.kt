package org.opensilk.autumn

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey

/**
 * Created by drew on 8/4/17.
 */
@Entity(tableName = "playlist")
data class Playlist @JvmOverloads constructor(
        @PrimaryKey var id: String,
        @Ignore var assets: List<PlaylistAsset> = emptyList())