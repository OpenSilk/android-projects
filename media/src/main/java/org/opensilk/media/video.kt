package org.opensilk.media

import android.net.Uri

/**
 * Video classification
 */
interface VideoId: MediaId

/**
 * Video metadata
 */
interface VideoMeta: MediaMeta {
    val subtitle: String
    val artworkUri: Uri
    val backdropUri: Uri
    val mediaUri: Uri
    val mimeType: String
    val duration: Long
    val size: Long
    val originalTitle: String
}

data class VideoResumeInfo(
        val lastPosition: Long = 0,
        val lastCompletion: Int = 0,
        val lastPlayed: Long = 0)

/**
 * Video ref, may have movie or episode id associated to it
 */
interface VideoRef: MediaRef {
    override val id: VideoId
    val tvEpisodeId: TvEpisodeId?
    val movieId: MovieId?
    override val meta: VideoMeta
    val resumeInfo: VideoResumeInfo?
}

