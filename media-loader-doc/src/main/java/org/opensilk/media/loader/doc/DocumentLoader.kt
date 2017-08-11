package org.opensilk.media.loader.doc

import io.reactivex.Maybe
import org.opensilk.media.DocumentId
import org.opensilk.media.DocumentRef

/**
 * Created by drew on 8/10/17.
 */
interface DocumentLoader {
    fun document(documentId: DocumentId): Maybe<DocumentRef>
    fun documents(documentId: DocumentId): Maybe<List<DocumentRef>>
}