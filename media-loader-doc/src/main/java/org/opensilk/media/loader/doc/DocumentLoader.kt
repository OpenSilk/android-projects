package org.opensilk.media.loader.doc

import io.reactivex.Maybe
import io.reactivex.Single
import org.opensilk.media.DocDirectoryId
import org.opensilk.media.DocumentId
import org.opensilk.media.DocumentRef

/**
 * Created by drew on 8/10/17.
 */
interface DocumentLoader {
    fun document(documentId: DocumentId): Maybe<DocumentRef>
    fun directChildren(documentId: DocDirectoryId, wantVideoItems: Boolean = false,
                       wantAudioItems: Boolean = false): Single<out List<DocumentRef>>
}