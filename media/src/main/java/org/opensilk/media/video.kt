package org.opensilk.media

import android.net.Uri

/**
 * Created by drew on 8/22/17.
 */
interface VideoMeta {
    val title: String
    val subtitle: String
    val artworkUri: Uri
    val backdropUri: Uri
    val mediaUri: Uri
    val mimeType: String
    val duration: Long
    val size: Long
    val originalTitle: String
}

interface VideoRef: MediaRef {
    val tvEpisodeId: TvEpisodeId?
    val movieId: MovieId?
    val meta: VideoMeta
}

