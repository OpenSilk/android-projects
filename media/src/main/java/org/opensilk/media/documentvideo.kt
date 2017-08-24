package org.opensilk.media

import android.net.Uri

/**
 * Created by drew on 8/22/17.
 */
data class VideoDocumentMeta(
        override val mimeType: String,
        override val lastMod: Long = 0,
        override val flags: Long,

        override val duration: Long = 0,
        override val mediaUri: Uri,
        override val size: Long,
        override val title: String = "",
        override val subtitle: String = "",
        override val artworkUri: Uri = Uri.EMPTY,
        override val backdropUri: Uri = Uri.EMPTY,
        override val originalTitle: String = "",

        val summary: String = ""
): DocumentMeta, VideoMeta

data class VideoDocumentRef(
        override val id: DocumentId,
        override val tvEpisodeId: TvEpisodeId? = null,
        override val movieId: MovieId? = null,
        override val meta: VideoDocumentMeta
): DocumentRef, VideoRef