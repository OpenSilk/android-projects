package org.opensilk.media

import android.net.Uri
import android.util.JsonReader
import android.util.JsonWriter

/**
 * Created by drew on 9/8/17.
 */
data class StorageMusicTrackId(
        override val uuid: String,
        override val path: String,
        val parent: String
): StorageId, MusicTrackId {
    override val json: String by lazy {
        writeJson(StorageMusicTrackIdTransformer, this)
    }
}

data class StorageMusicTrackMeta(
        override val title: String,
        override val artist: String = "",
        override val album: String = "",
        override val genre: String = "",
        override val trackNum: Int = 0,
        override val size: Long = 0,
        override val duration: Long = 0,
        override val bitrate: Long = 0,
        override val mediaUri: Uri,
        override val mimeType: String,
        override val originalArtworkUri: Uri = Uri.EMPTY,
        override val artworkUri: Uri = Uri.EMPTY,
        override val backdropUri: Uri = Uri.EMPTY,
        override val originalTitle: String = "",
        override val albumArtist: String = "",
        override val discNumber: Int = 1,
        override val isCompilation: Boolean = false
): StorageMeta, MusicTrackMeta

data class StorageMusicTrackRef(
        override val id: StorageMusicTrackId,
        override val meta: StorageMusicTrackMeta
): StorageRef, MusicTrackRef

internal object StorageMusicTrackIdTransformer: MediaIdTransformer<StorageMusicTrackId> {
    override val kind: String = STORAGE_MUSIC_TRACK
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: StorageMusicTrackId) {
        jw.name("uuid").value(item.uuid)
        jw.name("path").value(item.path)
        jw.name("pare").value(item.parent)
    }

    override fun read(jr: JsonReader, version: Int): StorageMusicTrackId {
        var path = ""
        var uuid = ""
        var pare = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "path" -> path = jr.nextString()
                "uuid" -> uuid = jr.nextString()
                "pare" -> pare = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return StorageMusicTrackId(
                uuid = uuid,
                path = path,
                parent = pare
        )
    }
}