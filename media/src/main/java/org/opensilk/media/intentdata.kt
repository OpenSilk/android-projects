package org.opensilk.media

import android.net.Uri
import android.util.JsonReader
import android.util.JsonWriter

/**
 * Created by drew on 9/2/17.
 */

data class IntentDataVideoId(
        val uri: Uri
): VideoId {
    override val json: String by lazy {
        writeJson(IntentDataVideoIdTransformer, this)
    }
}
data class IntentDataVideoMeta(
        override val artworkUri: Uri = Uri.EMPTY,
        override val title: String,
        override val backdropUri: Uri = Uri.EMPTY,
        override val duration: Long = 0L,
        override val mediaUri: Uri,
        override val subtitle: String = "",
        override val mimeType: String,
        override val originalTitle: String = "",
        override val size: Long = 0L
): VideoMeta

data class IntentDataVideoRef(
        override val id: VideoId,
        override val meta: VideoMeta,
        override val movieId: MovieId? = null,
        override val tvEpisodeId: TvEpisodeId? = null,
        override val resumeInfo: VideoResumeInfo? = null
): VideoRef

internal object IntentDataVideoIdTransformer: MediaIdTransformer<IntentDataVideoId> {
    override val kind: String = INTENT_DATA_VIDEO
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: IntentDataVideoId) {
        jw.name("uri").value(item.uri.toString())
    }

    override fun read(jr: JsonReader, version: Int): IntentDataVideoId {
        var uri = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "uri" -> uri = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return IntentDataVideoId(Uri.parse(uri))
    }
}