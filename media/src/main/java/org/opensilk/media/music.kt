package org.opensilk.media

import android.net.Uri

/**
 * Created by drew on 9/6/17.
 */
interface MusicId: AudioId

interface MusicMeta: AudioMeta {
    val album: String
    val albumArtist: String
    val artist: String
    val bitrate: Long
    val trackNum: Int
    val isCompilation: Boolean
    val discNumber: Int
    val duration: Long
    val genre: String
    val mimeType: String
    val mediaUri: Uri
    val originalTitle: String
    val originalArtworkUri: Uri
    val artworkUri: Uri
    val backdropUri: Uri
}

interface MusicRef: AudioRef {
    override val id: MusicId
    override val meta: MusicMeta
}

