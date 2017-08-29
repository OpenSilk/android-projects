package org.opensilk.media

import android.net.Uri
import android.util.JsonReader
import android.util.JsonWriter

/**
 * Created by drew on 8/22/17.
 */
data class StorageVideoId(
        override val path: String,
        override val uuid: String,
        val parent: String
): StorageId, VideoId {
    override val json: String
        get() = writeJson(StorageVideoIdTransformer, this)
}

data class StorageVideoMeta(
        override val title: String,
        override val size: Long = 0,
        val lastMod: Long = 0,
        override val mimeType: String,
        override val mediaUri: Uri,
        override val duration: Long = 0,
        override val artworkUri: Uri = Uri.EMPTY,
        override val backdropUri: Uri = Uri.EMPTY,
        override val originalTitle: String = "",
        override val subtitle: String = ""
): StorageMeta, VideoMeta

data class StorageVideoRef(
        override val id: StorageVideoId,
        override val meta: StorageVideoMeta,
        override val movieId: MovieId? = null,
        override val tvEpisodeId: TvEpisodeId? = null,
        override val resumeInfo: VideoResumeInfo? = null
): StorageRef, VideoRef

internal object StorageVideoIdTransformer: MediaIdTransformer<StorageVideoId> {
    override val kind: String = STORAGE_VIDEO
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: StorageVideoId) {
        jw.name("path").value(item.path)
        jw.name("uuid").value(item.uuid)
        jw.name("pare").value(item.parent)
    }

    override fun read(jr: JsonReader, version: Int): StorageVideoId {
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
        return StorageVideoId(
                uuid = uuid,
                path = path,
                parent = pare
        )
    }
}