package org.opensilk.media

import android.content.Intent
import android.util.JsonReader
import android.util.JsonWriter

const val DOCUMENT_TREE_ID = "tree"
const val DOCUMENT_FILE_ID = "file"

interface DocDeviceId : MediaDeviceId {
    val id: String
}

interface DocDeviceMeta : MediaDeviceMeta {
    val intent: Intent
}

interface DocDeviceRef : MediaDeviceRef

object DocTreeDeviceId : DocDeviceId {
    override val id: String = DOCUMENT_TREE_ID
    override val json: String by lazy {
        writeJson(DocDeviceIdTransformer, this)
    }
}

object DocTreeDeviceMeta : DocDeviceMeta {
    override val title: String = "External directory"
    override val intent: Intent by lazy {
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    }
}

/**
 * Item representing access to directories in the SAF
 */
object DocTreeDeviceRef : DocDeviceRef {
    override val id = DocTreeDeviceId
    override val meta = DocTreeDeviceMeta
}

object DocFileDeviceId : DocDeviceId {
    override val id: String = DOCUMENT_FILE_ID
    override val json: String by lazy {
        writeJson(DocDeviceIdTransformer, this)
    }
}

object DocFileDeviceMeta : DocDeviceMeta {
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
object DocFileDeviceRef : DocDeviceRef {
    override val id = DocFileDeviceId
    override val meta = DocFileDeviceMeta
}


/**
 * transformer for document device ids
 */
object DocDeviceIdTransformer : MediaIdTransformer<DocDeviceId> {
    override val kind: String = DOCUMENT_DEVICE
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: DocDeviceId) {
        jw.name("id").value(item.id)
    }

    override fun read(jr: JsonReader, version: Int): DocDeviceId {
        var id = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "id" -> id = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return when (id) {
            DOCUMENT_TREE_ID -> DocTreeDeviceId
            DOCUMENT_FILE_ID -> DocFileDeviceId
            else -> TODO()
        }
    }
}

