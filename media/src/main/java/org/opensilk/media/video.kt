package org.opensilk.media

import android.net.Uri

/**
 * Video classification
 */
interface VideoId: MediaId

/**
 * Video metadata
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

/**
 * Video ref, may have movie or episode id associated to it
 */
interface VideoRef: MediaRef {
    override val id: VideoId
    val tvEpisodeId: TvEpisodeId?
    val movieId: MovieId?
    val meta: VideoMeta
}

