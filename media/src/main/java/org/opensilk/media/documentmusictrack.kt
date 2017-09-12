package org.opensilk.media

import android.net.Uri

/**
 * Created by drew on 8/22/17.
 */
data class DocMusicTrackId(
        override val treeUri: Uri,
        override val parentId: String,
        override val documentId: String
): DocumentId, MusicTrackId {
    override val json: String by lazy {
        writeJson(DocMusicTrackIdTransformer, this)
    }
}

data class DocMusicTrackMeta(
        override val title: String,
        override val flags: Long,
        override val lastMod: Long,
        override val mimeType: String,
        override val artist: String = "",
        override val album: String = "",
        override val genre: String = "",
        override val trackNum: Int = 0,
        override val size: Long = 0,
        override val duration: Long = 0,
        override val bitrate: Long = 0,
        override val mediaUri: Uri,
        override val originalArtworkUri: Uri = Uri.EMPTY,
        override val artworkUri: Uri = Uri.EMPTY,
        override val backdropUri: Uri = Uri.EMPTY,
        override val originalTitle: String = "",
        override val albumArtist: String = "",
        override val discNumber: Int = 1,
        override val isCompilation: Boolean = false
): DocumentMeta, MusicTrackMeta

data class DocMusicTrackRef(
        override val id: DocMusicTrackId,
        override val meta: DocMusicTrackMeta
): DocumentRef, MusicTrackRef

internal object DocMusicTrackIdTransformer: DocumentIdTransformer() {
    override val kind: String = DOCUMENT_MUSIC_TRACK
}
