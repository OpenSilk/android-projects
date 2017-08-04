package org.opensilk.autumn

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey

/**
 * Created by drew on 8/4/17.
 */
@Entity(tableName = "playlist_asset",
        foreignKeys = arrayOf(ForeignKey(entity = Playlist::class,
                parentColumns = arrayOf("id"), childColumns = arrayOf("parentId"))),
        indices = arrayOf(Index(value = "parentId")))
data class PlaylistAsset(
        @PrimaryKey var id: String,
        var parentId: String,
        var url: String,
        var timeOfDay: String,
        var accessibilityLabel: String,
        var type: String,
        var pos: Int)