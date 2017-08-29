package org.opensilk.media

import android.content.Intent
import android.util.JsonReader
import android.util.JsonWriter

const val DOCUMENT_TREE_ID = "tree"
const val DOCUMENT_FILE_ID = "file"

interface DocumentDeviceId: MediaDeviceId {
    val id: String
}

interface DocumentDeviceMeta: MediaDeviceMeta {
    val intent: Intent
}

interface DocumentDeviceRef: MediaDeviceRef

object DocumentTreeDeviceId: DocumentDeviceId {
    override val id: String = DOCUMENT_TREE_ID
    override val json: String by lazy {
        writeJson(DocumentDeviceIdTransformer, this)
    }
}

object DocumentTreeDeviceMeta: DocumentDeviceMeta {
    override val title: String = "External directory"
    override val intent: Intent by lazy {
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    }
}

/**
 * Item representing access to directories in the SAF
 */
object DocumentTreeDeviceRef: DocumentDeviceRef {
    override val id = DocumentTreeDeviceId
    override val meta = DocumentTreeDeviceMeta
}

object DocumentFileDeviceId: DocumentDeviceId {
    override val id: String = DOCUMENT_FILE_ID
    override val json: String by lazy {
        writeJson(DocumentDeviceIdTransformer, this)
    }
}

object DocumentFileDeviceMeta: DocumentDeviceMeta {
    override val title: String = "External file"
    override val intent: Intent by lazy {
        Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("video/*")
    }
}

/**
 * Item representing access to files in the SAF
 */
object DocumentFileDeviceRef: DocumentDeviceRef {
    override val id = DocumentFileDeviceId
    override val meta = DocumentFileDeviceMeta
}


/**
 * transformer for document device ids
 */
object DocumentDeviceIdTransformer: MediaIdTransformer<DocumentDeviceId> {
    override val kind: String = DOCUMENT_DEVICE
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: DocumentDeviceId) {
        jw.name("id").value(item.id)
    }

    override fun read(jr: JsonReader, version: Int): DocumentDeviceId {
        var id = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "id" -> id = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return when (id) {
            DOCUMENT_TREE_ID -> DocumentTreeDeviceId
            DOCUMENT_FILE_ID -> DocumentFileDeviceId
            else -> TODO()
        }
    }
}

