package org.opensilk.media

import android.net.Uri
import android.provider.DocumentsContract

data class DocVideoId(
        override val treeUri: Uri,
        override val documentId: String = if (isTreeUri(treeUri))
            DocumentsContract.getTreeDocumentId(treeUri) else
            DocumentsContract.getDocumentId(treeUri),
        override val parentId: String = documentId
): DocumentId, VideoId {
    override val json: String by lazy {
        writeJson(VideoDocumentIdTransformer, this)
    }
}

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
        override val id: VideoDocumentId,
        override val tvEpisodeId: TvEpisodeId? = null,
        override val movieId: MovieId? = null,
        override val meta: VideoDocumentMeta
): DocumentRef, VideoRef

internal object VideoDocumentIdTransformer: DocumentIdTransformer() {
    override val kind: String = DOCUMENT_VIDEO
}